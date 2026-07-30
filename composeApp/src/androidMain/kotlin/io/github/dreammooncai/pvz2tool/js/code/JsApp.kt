package io.github.dreammooncai.pvz2tool.js.code

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
 * - 重启应用（`restart`）：退出当前进程并以 LAUNCHER Intent 冷重启，重新打开主界面。
 * - 重启并进入游戏（`restartGame`）：同上，但重启后自动触发「进入游戏」逻辑。
 * - 退出应用（`exit`）：结束所有 Activity 并终止进程。
 *
 * 实现说明：
 * - 通过 `ContextUtil.getCurrentActivity()` 获取当前前台 Activity（拿不到则回退到全局 Context）。
 * - 重启使用 `PackageManager.getLaunchIntentForPackage` 取得 LAUNCHER Intent，并附加
 *   `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`，确保是干净的冷重启。
 * - 所有对 Activity / 任务的变更均切到 `Dispatchers.Main`（协程主线程上下文）执行，避免跨线程操作窗口。
 * - 退出时直接 `finishAffinity` 后立即 `killProcess` / `exitProcess` 终止进程。
 * - 重启**不**杀进程：仅以 LAUNCHER Intent + `finishAffinity` 结束旧任务，新 Activity 在同一进程内
 *   重新创建，确保「重新打开」可靠（若在 startActivity 后杀进程，新 Activity 会被一并杀掉）。
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
     * 采用标准「冷重启」做法：以 LAUNCHER Intent + `NEW_TASK | CLEAR_TASK` 启动入口 Activity，
     * 并结束当前 Activity 任务栈。**不主动杀进程**——新 Activity 会在同一进程内重新创建，
     * 这样能可靠地「重新打开」（若在 startActivity 后杀进程，刚启动的新 Activity 会被一并杀掉）。
     *
     * @param autoEnterGame 为 true 时在重启后的 LAUNCHER Intent 中附带
     *        [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]，由入口 Activity 在启动后
     *        自动触发「进入游戏」逻辑。
     */
    private suspend fun restartApp(autoEnterGame: Boolean) {
        withContext(Dispatchers.Main) {
            runCatching {
                val activity = ContextUtil.getCurrentActivity()
                val ctx = InitializePvz2.context
                val intent = (ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                    ?: Intent(ctx, Pvz2InitializeActivity::class.java)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    if (autoEnterGame) {
                        putExtra(Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME, true)
                    }
                }
                (activity ?: ctx).startActivity(intent)
                activity?.finishAffinity()
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
