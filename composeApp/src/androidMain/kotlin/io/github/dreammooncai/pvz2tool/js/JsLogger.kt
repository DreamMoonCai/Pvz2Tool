package io.github.dreammooncai.pvz2tool.js

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.plus

private const val JS_LOG_MAX_LINES = 200

object JsLogger {
    // ======================== 日志 ========================
    private val _logs = MutableStateFlow<List<JsLogEntry>>(emptyList())
    /** 供 UI 订阅的日志流 */
    val logs: StateFlow<List<JsLogEntry>> = _logs.asStateFlow()

    /** 追加一条日志（线程安全，超过上限自动截断头部） */
    fun appendLog(level: JsLogLevel, message: String) {
        Log.d("JsLog", "[${level.name}] $message")
        _logs.update { current ->
            val entry = JsLogEntry(level, message)
            if (current.size >= JS_LOG_MAX_LINES) {
                current.drop(current.size - JS_LOG_MAX_LINES + 1) + entry
            } else {
                current + entry
            }
        }
    }

    /** 清空日志 */
    fun clearLogs() {
        _logs.value = emptyList()
    }
}