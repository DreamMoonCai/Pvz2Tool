package io.github.dreammooncai.pvz2tool.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 前台 Service，在后台启动 JS 引擎执行定时脚本。
 */
class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getStringExtra("timer_id") ?: return START_NOT_STICKY
        scope.launch {
            try {
                val timers = TimerManager.loadTimers(this@TimerService)
                val def = timers.find { it.id == timerId } ?: return@launch
                val script = def.jsScript ?: def.jsPath?.let { loadScript(it) } ?: return@launch
                PvzToolGlobals.currentTimerId = timerId
                PvzToolJsEngine.executeScript(script, source = "定时器:$timerId")
            } finally {
                try { TimerManager.rescheduleAll(this@TimerService) } catch (_: Exception) {}
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadScript(jsPath: String): String? {
        val resolved = JsFileResolver.resolvePlaceholders(jsPath)
        return AssetExtractorHolder.openInputStream(resolved)?.bufferedReader()?.readText()
    }
}
