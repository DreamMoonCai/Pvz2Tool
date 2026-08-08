package io.github.dreammooncai.pvz2tool.js.code

import android.os.Process
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.RestartPhoenixActivity
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
 * - 退出采用 `ContextUtil.getCurrentActivity()?.finishAffinity()` 结束当前任务栈，再终止进程。
 * - 重启采用「真正冷重启」，机制参照 JakeWharton/ProcessPhoenix：由 [RestartPhoenixActivity]
 *   （运行在独立的 `:phoenix` 进程）拉起 LAUNCHER Activity 并带到前台，随后立即 `killProcess` /
 *   `exitProcess` 终止**主进程**；`:phoenix` 进程独立存活、不受主进程被杀影响，新 Activity 在全新主进程中
 *   创建，全局状态归零。
 * - 之所以用「独立进程 Activity」而非「独立进程 Service 直接 startActivity」或「精确闹钟」：
 *   实测 targetSdk 36 / 本 ROM 上，后台 Service（含前台 Service + `MODE_BACKGROUND_ACTIVITY_START_ALLOWED`）
 *   拉起的 Activity 只能进入后台任务栈、无法带到前台（用户停在桌面）；而 `setAlarmClock` 等精确闹钟又要求
 *   `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` 权限（本 ROM 未授予/未自动授予）。独立进程的 **Activity**
 *   被系统拉起瞬间即为前台/可见组件，由它再 `startActivity` 属前台上下文，目标 Activity 可靠带到前台——
 *   这是 ProcessPhoenix 在海量 App 上验证可靠的核心。
 * - 之所以必须杀进程（而非仅 finish Activity）：若只 finish 当前 Activity 而保留进程，游戏
 *   Activity 会在「未清零」的旧进程状态中被二次启动，触发其内部的 Activity 恢复逻辑失败，
 *   表现为黑屏随后闪退；同时 Pvz2Tool 的全局单例（InitializePvz2.*、各 Controller、
 *   ActivityLifecycleCallbacks、mGLView 引用等）也会残留，再次进入游戏时重复注册监听、
 *   重复调用游戏私有 GL 方法。杀进程后由 AMS 在全新进程中创建入口，游戏干净冷启动。
 * - 所有对 Activity / 任务的变更均切到 `Dispatchers.Main`（协程主线程上下文）执行，避免跨线程操作窗口。
 * - 退出时直接 `finishAffinity` 后立即 `killProcess` / `exitProcess` 终止进程。
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
     * 采用「真正冷重启」，机制参照 JakeWharton/ProcessPhoenix：
     * - 由 [RestartPhoenixActivity]（运行在独立的 `:phoenix` 进程）在自身 `onCreate` 中构造
     *   直接指向 LAUNCHER 的 `PendingIntent`（`NEW_TASK|CLEAR_TASK`，可选
     *   [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]）并 `startActivity`；
     * - 该 `:phoenix` Activity 被系统拉起时即为前台/可见组件，其 `startActivity` 属前台上下文，
     *   目标 LAUNCHER Activity 能可靠**带到前台**；随后 `:phoenix` 进程自尽；
     * - 主进程在触发后已被 [RestartPhoenixActivity.triggerRebirth] 杀掉，新主进程由 AMS 创建，全局状态归零。
     * - 此方案不依赖精确闹钟权限，也不依赖后台启动豁免，是本 ROM 上唯一验证可靠的冷重启路径。
     *
     * @param autoEnterGame 为 true 时重启后由入口 Activity 自动触发「进入游戏」逻辑。
     */
    private suspend fun restartApp(autoEnterGame: Boolean) {
        withContext(Dispatchers.Main) {
            val ctx = InitializePvz2.context.applicationContext
            // 启动 :phoenix 独立进程 Activity 并立即杀主进程；真正的拉起/带前台在 :phoenix 进程内完成。
            RestartPhoenixActivity.triggerRebirth(ctx, autoEnterGame)
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
