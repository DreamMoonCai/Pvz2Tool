package io.github.dreammooncai.pvz2tool.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟触发后的入口接收器（仅用于定时 JS 任务）：
 * 由 [TimerManager] 通过 [android.app.AlarmManager] 定时派发的广播触发，拉起前台
 * [TimerService] 执行 JS。
 *
 * [TimerService] 会在 `onStartCommand` 内立即 `startForeground()`，从而既能跑任意时长的脚本
 * （不受接收器 `goAsync()` 保活窗口限制，适合每日批量/长任务），又不会触发
 * `ForegroundServiceDidNotStartInTimeException`。
 *
 * 兜底：Android 12+ 下若应用处于后台（如被杀后由闹钟唤醒），`startForegroundService` 可能被系统
 * 拒绝。此时退化为在接收器内通过 `goAsync()` 保活直接执行脚本（适合较短脚本）。
 *
 * 注：应用冷重启（app.restart / app.restartGame）已不再经由此接收器 —— 改为由
 * [io.github.dreammooncai.pvz2tool.RestartPhoenixActivity]（独立 :phoenix 进程）负责拉起入口 Activity。
 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra("timer_id") ?: return
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            putExtra("timer_id", timerId)
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // 后台启动前台 Service 受限：在接收器内直接执行（goAsync 延长接收器生命周期）。
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    TimerService.execute(context, timerId)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
