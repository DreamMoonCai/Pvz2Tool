package io.github.dreammooncai.pvz2tool.js

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JsLogEntry(
    val level: JsLogLevel,
    val message: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)