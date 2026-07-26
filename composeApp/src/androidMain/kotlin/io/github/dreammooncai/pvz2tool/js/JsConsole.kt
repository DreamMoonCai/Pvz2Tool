package io.github.dreammooncai.pvz2tool.js

import io.github.alexzhirkevich.keight.Console
import io.github.alexzhirkevich.keight.js.SyntaxError

object JsConsole: Console {
    override fun verbose(message: Any?) {
        JsLogger.appendLog(JsLogLevel.INFO, formatConsoleArgs(message))
    }
    override fun info(message: Any?) {
        JsLogger.appendLog(JsLogLevel.INFO, "ℹ ${formatConsoleArgs(message)}")
    }
    override fun debug(message: Any?) {
        JsLogger.appendLog(JsLogLevel.INFO, "[dbg] ${formatConsoleArgs(message)}")
    }
    override fun warn(message: Any?) {
        JsLogger.appendLog(JsLogLevel.WARN, "⚠ ${formatConsoleArgs(message)}")
    }
    fun success(message: Any?) {
        JsLogger.appendLog(JsLogLevel.SUCCESS, "✓ ${formatConsoleArgs(message)}")
    }
    override fun error(message: Any?) {
        val msg = if (message is Throwable) {
            message.stackTraceToString()
        } else {
            formatConsoleArgs(message)
        }
        JsLogger.appendLog(JsLogLevel.ERROR, "✖ $msg")
    }

    fun warn(message: Any?,err: Throwable) {
        if (err is SyntaxError)
            warn("$message $err${if (err.cause != null) "\n${err.cause}" else ""} \n${err.stackTraceToString()}")
        else
            warn("$message $err${if (err.cause != null) "\n${err.cause}" else ""}")
    }

    fun error(message: Any?,err: Throwable) {
        if (err is SyntaxError)
            error("$message $err${if (err.cause != null) "\n${err.cause}" else ""} \n${err.stackTraceToString()}")
        else
            error("$message $err${if (err.cause != null) "\n${err.cause}" else ""}")
    }
}

private fun formatConsoleArgs(message: Any?): String {
    return when (message) {
        is List<*> -> message.joinToString(" ") { it?.toString() ?: "null" }
        null -> "null"
        else -> message.toString()
    }
}