package io.github.dreammooncai.pvz2tool.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 定时脚本执行前台 Service。
 *
 * 由 [TimerReceiver] 通过 `startForegroundService` 拉起。关键点：**`onStartCommand` 必须
 * 立即调用 `startForeground()` 弹出常驻通知**——否则系统会抛
 * `ForegroundServiceDidNotStartInTimeException` 并杀进程（这正是之前崩溃的根因）。
 *
 * 前台 Service 没有 `goAsync()` 那样的保活窗口限制，可支撑**任意时长**的脚本，
 * 因此对「每日本就跑很久」的定时任务（批量处理/下载/长联网）也比接收器内 goAsync 更稳妥。
 * 执行完脚本后在 `finally` 中 `stopSelf()`，常驻通知随即消失。
 */
class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getStringExtra("timer_id") ?: return START_NOT_STICKY
        // 必须先提升为前台（弹通知），再跑脚本，顺序不能反，否则超时崩溃。
        startForeground(NOTIF_ID, buildNotification(timerId))
        scope.launch {
            try {
                execute(this@TimerService, timerId)
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(timerId: String): android.app.Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pvz2Tool 定时任务")
            .setContentText("正在执行定时器：$timerId")
            .setSmallIcon(applicationInfo.icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "定时任务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "执行定时 JS 任务时的前台通知"
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "pvz2tool_timer"
        private const val NOTIF_ID = 0x5411

        /**
         * 执行指定定时器对应的 JS 脚本，并在 finally 中重新调度所有定时器（续期）。
         * 由 [TimerService] 在前台 Service 内调用；被杀后由闹钟唤醒亦是此路径。
         */
        suspend fun execute(context: Context, timerId: String) {
            try {
                // 被杀后的进程里没有 Activity，InitializePvz2 尚未初始化，脚本访问 context.assets 会崩溃；
                // 这里用 Receiver/Service 的 applicationContext 补初始化。
                InitializePvz2.ensureInitialized(context)
                val timers = TimerManager.loadTimers(context)
                val def = timers.find { it.id == timerId } ?: return
                val script = def.jsScript ?: def.jsPath?.let { loadScript(context, it) } ?: return
                PvzToolGlobals.currentTimerId = timerId
                PvzToolJsEngine.executeScript(script, source = "定时器:$timerId")
            } finally {
                try { TimerManager.rescheduleAll(context) } catch (_: Exception) {}
            }
        }

        private fun loadScript(context: Context, jsPath: String): String? {
            val resolved = JsFileResolver.resolvePlaceholders(jsPath)
            return AssetExtractorHolder.openInputStream(resolved)?.bufferedReader()?.readText()
        }
    }
}
