package io.github.dreammooncai.pvz2tool.js.code

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.util.ContextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

/**
 * 应用进程控制全局对象：`app`。
 *
 * 提供三类进程级操作：
 * - 重启应用（`restart`）：终止当前进程并由系统重新拉起入口 Activity（真正冷重启）。
 * - 重启并进入游戏（`restartGame`）：同上，但重启后自动触发「进入游戏」逻辑。
 * - 退出应用（`exit`）：结束所有 Activity 并终止进程。
 *
 * 实现说明：
 * - 通过 `ContextUtil.getCurrentActivity()` 获取当前前台 Activity（拿不到则回退到全局 Context）。
 * - 重启采用「真正冷重启」：以 `AlarmManager` 在极短延迟后由**系统进程**派发 LAUNCHER Intent
 *   （附加 `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`，以及可选的
 *   [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]），随后立即 `killProcess` / `exitProcess`
 *   终止当前进程。新 Activity 在全新进程中创建，所有全局状态归零。
 * - 之所以必须杀进程（而非仅 finish Activity）：若只 finish 当前 Activity 而保留进程，游戏
 *   Activity 会在「未清零」的旧进程状态中被二次启动，触发其内部的 Activity 恢复逻辑失败，
 *   表现为黑屏随后闪退；同时 Pvz2Tool 的全局单例（InitializePvz2.*、各 Controller、
 *   ActivityLifecycleCallbacks、mGLView 引用等）也会残留，再次进入游戏时重复注册监听、
 *   重复调用游戏私有 GL 方法。杀进程后由 AMS 在全新进程中创建入口，游戏干净冷启动。
 * - 之所以用 `AlarmManager` 而非直接 `startActivity` 后杀进程：单进程应用直接 `startActivity`
 *   会在同一进程内创建新 Activity，随即 kill 会连新 Activity 一起杀掉。改由系统进程延迟派发
 *   启动 Intent，可确保旧进程退出后新实例在全新进程中拉起。
 * - 所有对 Activity / 任务的变更均切到 `Dispatchers.Main`（协程主线程上下文）执行，避免跨线程操作窗口。
 * - 退出时直接 `finishAffinity` 后立即 `killProcess` / `exitProcess` 终止进程。
 * - 任何异常均静默吞掉（`runCatching`），不影响脚本后续执行。
 *
 * 用法：
 * ```js
 * // 重启应用（重新打开主界面）
 * app.restart();
 * app.重启();
 *
 * // 重启并自动进入游戏
 * app.restartGame();
 * app.重启游戏();
 *
 * // 退出应用
 * app.exit();
 * app.退出();
 * ```
 */
object JsApp {

    /**
     * 退出应用：结束当前 Activity 任务栈并终止进程。
     */
    private suspend fun exitApp() {
        withContext(Dispatchers.Main) {
            runCatching { ContextUtil.getCurrentActivity()?.finishAffinity() }
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    /**
     * 重启应用（可选自动进入游戏）。
     *
     * 采用「真正冷重启」：通过系统 [AlarmManager] 在极短延迟后由系统进程拉起
     * LAUNCHER Intent（附带 [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]），随后立即
     * [Process.killProcess] / [exitProcess] 终止当前进程。
     *
     * @param autoEnterGame 为 true 时在重启后的 LAUNCHER Intent 中附带
     *        [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]，由入口 Activity 在启动后
     *        自动触发「进入游戏」逻辑。
     */
    private suspend fun restartApp(autoEnterGame: Boolean) {
        withContext(Dispatchers.Main) {
            runCatching {
                val ctx = InitializePvz2.context.applicationContext
                val intent = (ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                    ?: Intent(ctx, Pvz2InitializeActivity::class.java)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    if (autoEnterGame) {
                        putExtra(Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME, true)
                    }
                }
                val pendingIntent = PendingIntent.getActivity(
                    ctx,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                // 极短延迟后由系统进程拉起入口 Activity，随后终止当前进程
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 80,
                    pendingIntent
                )
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
        }
    }

    val js = Object("app") {
        // 重启应用（冷重启，重新打开主界面）
        listOf("restart".js, "重启".js, "重启应用".js, "重启APP".js).func {
            restartApp(false)
            null
        }

        // 重启并自动进入游戏
        listOf("restartGame".js, "重启游戏".js).func {
            restartApp(true)
            null
        }

        // 退出应用
        listOf("exit".js, "退出".js, "退出应用".js, "退出APP".js).func {
            exitApp()
            null
        }
    }
}
