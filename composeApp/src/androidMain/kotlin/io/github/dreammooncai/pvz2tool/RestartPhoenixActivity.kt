package io.github.dreammooncai.pvz2tool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity
import kotlin.system.exitProcess

/**
 * 冷重启专用「独立进程透明 Activity」，机制参照 JakeWharton/ProcessPhoenix。
 *
 * 为什么必须用 **Activity** 而不是 **Service**：
 * 在 targetSdk 36 / 本 ROM 上，后台 Service（即便是前台 Service）直接 `startActivity` 或经
 * `PendingIntent + MODE_BACKGROUND_ACTIVITY_START_ALLOWED` 拉起的 Activity，都只能被创建在
 * 后台任务栈、无法带到前台（Android 10+ 后台启动 Activity 限制，BAL）。用户始终停在桌面。
 * 而独立进程的 **Activity** 在被系统拉起的那一刻即为前台/可见组件，由它再 `startActivity`
 * 属于前台上下文，目标 Activity 能可靠带到前台——这正是 ProcessPhoenix 在海量 App 上可靠工作的核心。
 *
 * 流程：
 * 1. 主进程调用 [triggerRebirth] → 启动本 Activity（`:phoenix` 进程）+ 立即杀掉主进程；
 * 2. 本 Activity 在 `:phoenix` 进程 `onCreate` 中构造 LAUNCHER Intent 并 `startActivity`
 *    （`NEW_TASK|CLEAR_TASK`，可选 [Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME]）；
 * 3. 随后 `finish` 并 `exit` 自身进程。新主进程由 AMS 创建，全局单例/状态全部归零，游戏干净冷启动。
 *
 * 该方案不依赖任何精确闹钟权限，也不依赖后台启动豁免，是 Android 上最可靠的「杀进程后自动带回前台」实现。
 */
class RestartPhoenixActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val autoEnterGame = intent.getBooleanExtra(EXTRA_AUTO_ENTER_GAME, false)
        val ctx = applicationContext
        // 直接构造 LAUNCHER Intent（入口 Activity 已注册，无需新增组件）。
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?: Intent(ctx, Pvz2InitializeActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (autoEnterGame) {
            launch.putExtra(Pvz2InitializeActivity.EXTRA_AUTO_ENTER_GAME, true)
        }
        // 当前 :phoenix Activity 处于前台/可见，此次启动属前台上下文，目标 Activity 会带到前台。
        startActivity(launch)
        finish()
        // 自杀 :phoenix 进程（新主进程已由上面的 startActivity 拉起）。
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    companion object {
        const val EXTRA_AUTO_ENTER_GAME =
            "io.github.dreammooncai.pvz2tool.RestartPhoenixActivity.AUTO_ENTER_GAME"

        /**
         * 触发冷重启：启动 [RestartPhoenixActivity]（`:phoenix` 独立进程）后立即杀掉当前（主）进程。
         *
         * @param autoEnterGame 为 true 时重启后由入口 Activity 自动触发「进入游戏」。
         */
        fun triggerRebirth(context: Context, autoEnterGame: Boolean) {
            val intent = Intent(context, RestartPhoenixActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_AUTO_ENTER_GAME, autoEnterGame)
            }
            // startActivity 通过 binder 同步交付给 AMS，随后杀主进程不影响 :phoenix 进程被拉起。
            context.startActivity(intent)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
}
