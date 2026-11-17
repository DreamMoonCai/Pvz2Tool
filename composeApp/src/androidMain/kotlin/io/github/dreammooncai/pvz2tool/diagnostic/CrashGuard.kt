package io.github.dreammooncai.pvz2tool.diagnostic

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import io.github.dreammooncai.pvz2tool.Pvz2ErrorScreenState
import io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity
import io.github.dreammooncai.pvz2tool.RestartPhoenixActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * 全局崩溃拦截：把「直接闪退」换成「自动重启并显示错误页」。
 *
 * ### 为什么必须是「重启」而不是「原地弹窗」
 * 未捕获异常抛出后，主线程的 `Looper.loop()` 已经退出，**主线程彻底死亡**：
 * 任何后续的 UI（Compose 重组、弹窗、Toast）都不会再被调度，进程只会挂在那里等 ANR。
 * 因此「原地显示错误页」在物理上不可能。可行做法是：
 * 1. 崩溃线程里把完整堆栈落盘（SharedPreferences，`commit()` 同步写，保证进程被 kill 前落盘）；
 * 2. 借 [RestartPhoenixActivity]（`:phoenix` 独立进程）把主界面带回前台——
 *    独立进程的 Activity 创建即处于前台，由它 `startActivity` 不受 Android 10+ 后台启动限制，
 *    比 AlarmManager 预约更可靠（后者可能被 BAL 拦掉）；
 * 3. 新进程启动后 [consumePendingCrash] 读到堆栈 → 直接渲染 [io.github.dreammooncai.pvz2tool.Pvz2ErrorScreen]。
 *
 * ### 只对本进程内 Java 层异常有效
 * native crash（SIGSEGV/abort，如游戏 .so 崩溃）、ANR、被系统 OOM 杀，都不会走这里。
 *
 * ### 防崩溃循环
 * 60 秒内连续崩溃超过 [MAX_RELAUNCH] 次就不再自动重启（否则会变成「崩溃→错误页→继续→崩溃」的无限循环，
 * 用户连错误页都点不了）。此时仍会落盘，用户下次手动打开应用能看到最后一次崩溃信息。
 */
object CrashGuard {

    private const val TAG = "CrashGuard"

    private const val PREFS = "pvz2tool_crash_guard"

    /** 待展示崩溃的落盘时间戳 */
    private const val KEY_TIME = "pending_time"

    /** 待展示崩溃的完整堆栈文本 */
    private const val KEY_STACK = "pending_stack"

    /** 崩溃时前台页面类名（便于定位） */
    private const val KEY_SCREEN = "pending_screen"

    /** 崩溃循环检测：上次崩溃时间 */
    private const val KEY_LOOP_TIME = "loop_time"

    /** 崩溃循环检测：窗口内崩溃次数 */
    private const val KEY_LOOP_COUNT = "loop_count"

    /** 崩溃循环窗口 */
    private const val LOOP_WINDOW_MS = 60_000L

    /** 窗口内最多自动重启次数 */
    private const val MAX_RELAUNCH = 3

    /** 落盘多久内算「本次崩溃」并直接展示（超过则认为用户已放弃，静默丢弃，避免下次打开误弹旧崩溃） */
    private const val PENDING_TTL_MS = 5 * 60_000L

    private val installed = AtomicBoolean(false)

    private val handling = AtomicBoolean(false)

    private var appContext: Context? = null

    private var foreground: Activity? = null

    /**
     * 安装全局拦截（幂等）。越早调用越好：目前挂在 [Pvz2InitializeActivity.onCreate]、
     * [io.github.dreammooncai.pvz2tool.ToolboxIntegratorActivity] 的最开头，以及
     * `InitializePvz2.ensureInitialized`（覆盖 TimerReceiver/TimerService 这类无 Activity 的入口），
     * 因此这些入口之后的所有代码（含 Compose 组合/测量/布局/绘制）都在保护范围内。
     *
     * @param context 任意 Context（Activity / Service / Receiver / Application 均可）；
     *                用 `applicationContext` 注册生命周期回调来记录前台页面。
     *
     * 不会侵占别人的 handler：链式调用安装前那一个，游戏/第三方崩溃上报依然收得到。
     */
    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return

        val app = (context.applicationContext as? Application) ?: (context as? Application)
        appContext = context.applicationContext ?: context

        if (app != null) {
            runCatching {
                app.registerActivityLifecycleCallbacks(
                    object : Application.ActivityLifecycleCallbacks {
                        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                        override fun onActivityStarted(activity: Activity) {
                            foreground = activity
                        }

                        override fun onActivityResumed(activity: Activity) {
                            foreground = activity
                        }

                        override fun onActivityPaused(activity: Activity) {}

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            if (foreground === activity) foreground = null
                        }
                    }
                )
            }.onFailure { Log.w(TAG, "注册生命周期回调失败（只影响崩溃信息里的前台页面名）", it) }
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        runCatching {
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                onUncaught(thread, error, previous)
            }
        }.onFailure { Log.e(TAG, "安装全局崩溃拦截失败", it) }
    }

    /**
     * 取出「上次被拦截的崩溃」并清空落盘记录。
     *
     * @return 需要展示的错误页状态；没有待展示崩溃（或已超过 [PENDING_TTL_MS]）时返回 null。
     */
    fun consumePendingCrash(context: Context): Pvz2ErrorScreenState? {
        val sp = runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }.getOrNull() ?: return null

        val stack = sp.getString(KEY_STACK, null) ?: return null
        val time = sp.getLong(KEY_TIME, 0L)
        val screen = sp.getString(KEY_SCREEN, null)

        // 一次性：读完即清，避免「继续使用」后再打开又弹一次
        runCatching {
            sp.edit().remove(KEY_STACK).remove(KEY_TIME).remove(KEY_SCREEN)
                // 崩溃页已经成功显示出来了 → 重置崩溃循环计数，
                // 用户后续再遇到崩溃仍能正常进入错误页（真正拉不起来时计数会一直累加并被 [MAX_RELAUNCH] 拦住）
                .remove(KEY_LOOP_COUNT).remove(KEY_LOOP_TIME)
                .commit()
        }

        if (stack.isBlank()) return null
        if (System.currentTimeMillis() - time > PENDING_TTL_MS) return null

        return Pvz2ErrorScreenState(
            message = "程序崩溃（已拦截）",
            throwable = null,
            typeName = "UncaughtException",
            stackText = if (screen.isNullOrBlank()) stack else "$stack\n前台页面：$screen",
            hint = "崩溃已被拦截，并自动重启到本页面，没有直接闪退。\n" +
                "点「复制错误信息」把下方堆栈发给开发者定位；点「继续使用」返回工具箱（本次崩溃前的界面状态已丢失）。",
            recoverable = true
        )
    }

    // ======================== 内部实现 ========================

    private fun onUncaught(
        thread: Thread,
        error: Throwable,
        previous: Thread.UncaughtExceptionHandler?
    ) {
        val ctx = appContext
        // 已处理过一次（崩溃过程中又崩）/ 尚未安装成功 → 交回原 handler，不改变系统行为
        if (ctx == null || !handling.compareAndSet(false, true)) {
            previous?.uncaughtException(thread, error) ?: hardKill()
            return
        }

        Log.e(TAG, "拦截到未捕获异常，准备重启到错误页", error)

        try {
            persistPending(ctx, buildReport(ctx, thread, error))
            if (canRelaunch(ctx)) {
                // 先把崩溃现场完整打到 logcat（后面自己 kill，不会再有系统的 FATAL 段）
                logFatal(thread, error)
                // 走 :phoenix 独立进程中转：新进程的 Activity 属于前台组件，
                // 由它拉起主界面不受后台启动限制；本函数内部会杀掉当前（已死）进程。
                runCatching {
                    RestartPhoenixActivity.triggerRebirth(ctx, autoEnterGame = false)
                }.onFailure { e ->
                    Log.e(TAG, ":phoenix 中转失败，退化为直接启动入口 Activity", e)
                    runCatching {
                        ctx.startActivity(
                            Intent(ctx, Pvz2InitializeActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                    }
                }
            } else {
                Log.w(TAG, "短时间内崩溃次数过多，放弃自动重启（避免崩溃循环）")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "崩溃信息落盘失败", t)
        }

        // 正常路径上 triggerRebirth 已经把进程杀了，走不到这里；
        // 兜底（不重启 / 启动失败）时交回安装前的 handler：系统会打印 FATAL 并结束进程。
        previous?.uncaughtException(thread, error) ?: hardKill()
    }

    /** 自己结束进程时补一条 FATAL 日志，保证 logcat 仍能按 FATAL 抓到这个崩溃 */
    private fun logFatal(thread: Thread, error: Throwable) {
        runCatching {
            Log.e(TAG, "FATAL EXCEPTION (intercepted) [${thread.name}] ${error.message}", error)
        }
    }

    /** 拼装可读的崩溃报告（含线程、前台页面、设备、应用版本、异常链） */
    private fun buildReport(ctx: Context, thread: Thread, error: Throwable): String = buildString {
        appendLine("=========== 崩溃报告 ===========")
        appendLine("时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine("线程：${thread.name}${if (thread === android.os.Looper.getMainLooper().thread) "（主线程）" else ""}")
        appendLine("前台页面：${foreground?.let { it::class.java.name } ?: "无（可能已销毁）"}")
        appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("应用：${ctx.packageName} v${appVersion(ctx)}")
        appendLine()
        appendLine(error.toString())
        append(error.stackTraceToString())

        var cause = error.cause
        var depth = 0
        while (cause != null && depth < 5) {
            appendLine()
            appendLine("Caused by: $cause")
            append(cause.stackTraceToString())
            cause = cause.cause
            depth++
        }
    }

    private fun appVersion(ctx: Context): String = runCatching {
        @Suppress("DEPRECATION")
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
    }.getOrNull() ?: "?"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun persistPending(ctx: Context, report: String) {
        prefs(ctx).edit()
            .putLong(KEY_TIME, System.currentTimeMillis())
            .putString(KEY_STACK, report)
            .putString(KEY_SCREEN, foreground?.let { it::class.java.name })
            .commit() // 必须同步落盘：紧接着进程就会被结束
    }

    /** 崩溃循环检测：窗口内累计次数 + 是否仍允许自动重启 */
    private fun canRelaunch(ctx: Context): Boolean {
        val sp = prefs(ctx)
        val now = System.currentTimeMillis()
        val last = sp.getLong(KEY_LOOP_TIME, 0L)
        val count = if (now - last < LOOP_WINDOW_MS) sp.getInt(KEY_LOOP_COUNT, 0) + 1 else 1
        sp.edit().putLong(KEY_LOOP_TIME, now).putInt(KEY_LOOP_COUNT, count).commit()
        return count <= MAX_RELAUNCH
    }

    private fun hardKill() {
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}
