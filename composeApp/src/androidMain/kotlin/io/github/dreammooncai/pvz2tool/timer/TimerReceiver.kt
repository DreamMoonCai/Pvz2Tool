package io.github.dreammooncai.pvz2tool.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 闹钟触发后启动 [TimerService] 执行 JS。 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra("timer_id") ?: return
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            putExtra("timer_id", timerId)
        }
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
