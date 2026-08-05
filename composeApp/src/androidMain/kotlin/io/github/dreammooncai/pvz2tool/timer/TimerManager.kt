package io.github.dreammooncai.pvz2tool.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.dreammooncai.pvz2tool.ScheduleDef
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

/**
 * 管理定时任务注册/取消/查询。
 */
object TimerManager {
    private const val PREFS = "pvz2tool_timers"
    private const val KEY_TIMERS = "timer_defs_json"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun loadTimers(context: Context): List<ScheduleDef> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TIMERS, null) ?: return emptyList()
        return try { json.decodeFromString<List<ScheduleDef>>(raw) } catch (_: Exception) { emptyList() }
    }

    private fun saveTimers(context: Context, timers: List<ScheduleDef>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_TIMERS, json.encodeToString(timers))
        }
    }

    /** 从 dream.yml 初始加载 schedules 并注册（仅在应用首次启动时调用）。 */
    fun initFromConfig(context: Context, schedules: List<ScheduleDef>) {
        val existing = loadTimers(context)
        val configIds = schedules.map { it.id }.toSet()
        // 移除旧定义中不再存在的
        val updated = schedules.toMutableList()
        for (t in existing) {
            if (t.id !in configIds) updated.add(t)
        }
        saveTimers(context, updated)
        for (t in updated) {
            if (t.enabled && t.cron.isNotBlank()) schedule(context, t)
        }
    }

    /** 注册单个定时器闹钟，基于 cron 解析首次触发时间。 */
    fun schedule(context: Context, def: ScheduleDef) {
        val millis = cronToNextMillis(def.cron) ?: return
        val intent = buildIntent(context, def.id)
        val pi = PendingIntent.getBroadcast(
            context, def.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31) {
            if (!am.canScheduleExactAlarms()) return
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (_: SecurityException) { }
    }

    /** 取消指定定时器的闹钟。 */
    fun cancel(context: Context, id: String) {
        val intent = buildIntent(context, id)
        val pi = PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        pi?.cancel()
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    /** 取消所有定时器闹钟。 */
    fun cancelAll(context: Context) {
        for (t in loadTimers(context)) cancel(context, t.id)
    }

    /** 添加/更新一个定时器（来自 JS API），持久化并注册闹钟。 */
    fun addOrUpdate(context: Context, def: ScheduleDef): ScheduleDef {
        val list = loadTimers(context).toMutableList()
        val idx = list.indexOfFirst { it.id == def.id }
        if (idx >= 0) list[idx] = def else list.add(def)
        saveTimers(context, list)
        cancel(context, def.id)
        if (def.enabled && def.cron.isNotBlank()) schedule(context, def)
        return def
    }

    /** 删除一个定时器（来自 JS API），持久化并取消闹钟。 */
    fun remove(context: Context, id: String): Boolean {
        val list = loadTimers(context).toMutableList()
        val removed = list.removeAll { it.id == id }
        if (removed) {
            saveTimers(context, list)
            cancel(context, id)
        }
        return removed
    }

    /** 重新调度所有已启用的定时器（应用启动 / 重启后调用）。 */
    fun rescheduleAll(context: Context) {
        for (t in loadTimers(context)) {
            if (t.enabled && t.cron.isNotBlank()) schedule(context, t)
        }
    }

    private fun buildIntent(context: Context, id: String): Intent {
        return Intent(context, TimerReceiver::class.java).apply {
            putExtra("timer_id", id)
        }
    }

    /**
     * Parses simple cron/interval descriptions.
     * Supports: "0 10 * * *", "every 30m", "every 2h", "every 1d"
     */
    fun cronToNextMillis(cron: String): Long? {
        if (cron.isBlank()) return null
        val now = System.currentTimeMillis()

        when {
            cron.startsWith("every ") -> {
                val parts = cron.removePrefix("every ").trim().split(" ")
                if (parts.size != 1) return null
                val spec = parts[0]
                val num = spec.filter { it.isDigit() }.toLongOrNull() ?: 1L
                val unit = when {
                    spec.endsWith("m") || spec.endsWith("min") -> TimeUnit.MINUTES
                    spec.endsWith("h") || spec.endsWith("hour") -> TimeUnit.HOURS
                    spec.endsWith("d") || spec.endsWith("day") -> TimeUnit.DAYS
                    else -> return null
                }
                return now + unit.toMillis(num)
            }
            // 五段式 cron: minute hour dayOfMonth month dayOfWeek
            cron.count { it == ' ' } >= 4 -> {
                val parts = cron.trim().split("\\s+".toRegex())
                if (parts.size < 5) return null
                // 简化 cron 解析：若 hour 非 *，设为今天该时间；若已过则 +1 天
                val hour = parts[1].toIntOrNull() ?: return null
                val minute = parts[0].toIntOrNull()?.let { if (it in 0..59) it else null } ?: return null

                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, minute)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                if (cal.timeInMillis <= now && parts[2] == "*") {
                    // 非指定日期 → +1 天
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                return cal.timeInMillis
            }
            else -> return null
        }
    }
}
