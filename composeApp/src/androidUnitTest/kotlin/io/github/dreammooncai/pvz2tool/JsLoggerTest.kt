package io.github.dreammooncai.pvz2tool

import io.github.dreammooncai.pvz2tool.js.JsLogLevel
import io.github.dreammooncai.pvz2tool.js.JsLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [JsLogger] 单元测试：JS `log.*` API 的底层日志缓冲区（**纯内存逻辑**）。
 *
 * `log.get` / `log.count` / `log.clear` 都由这里支撑，因此先保证缓冲区语义正确：
 * 追加、级别记录、超出上限只保留最近 N 条、清空。
 *
 * ⚠️ `JsLogger.appendLog` 内部会调 `android.util.Log.d`；本项目已开启
 * `testOptions.unitTests.isReturnDefaultValues`，JVM 单测下 Log 是空实现（返回默认值），
 * 因此这些用例可以脱离设备运行。
 *
 * 运行：./gradlew :composeApp:testDebugUnitTest --tests "*JsLoggerTest*"
 */
class JsLoggerTest {

    @BeforeTest
    fun clearBeforeEach() {
        JsLogger.clearLogs()
    }

    @Test
    fun appendIncreasesCount() {
        assertEquals(0, JsLogger.logs.value.size)
        JsLogger.appendLog(JsLogLevel.INFO, "第一条")
        JsLogger.appendLog(JsLogLevel.WARN, "第二条")
        assertEquals(2, JsLogger.logs.value.size)
        assertEquals("第一条", JsLogger.logs.value[0].message)
        assertEquals(JsLogLevel.WARN, JsLogger.logs.value[1].level)
    }

    @Test
    fun entriesKeepLevelMessageAndTimestamp() {
        JsLogger.appendLog(JsLogLevel.ERROR, "出错了")
        val entry = JsLogger.logs.value.single()
        assertEquals(JsLogLevel.ERROR, entry.level)
        assertEquals("出错了", entry.message)
        assertTrue(entry.timestamp.isNotBlank(), "时间戳应非空")
    }

    @Test
    fun clearEmptiesBuffer() {
        repeat(5) { JsLogger.appendLog(JsLogLevel.INFO, "第 $it 条") }
        JsLogger.clearLogs()
        assertTrue(JsLogger.logs.value.isEmpty())
    }

    @Test
    fun bufferKeepsOnlyMostRecentEntries() {
        // 上限 200：写入 260 条后应只保留最近 200 条
        repeat(260) { JsLogger.appendLog(JsLogLevel.INFO, "line-$it") }
        val logs = JsLogger.logs.value
        assertEquals(200, logs.size)
        assertEquals("line-60", logs.first().message)
        assertEquals("line-259", logs.last().message)
    }

    @Test
    fun levelFilterMatchesEnumNameLowerCase() {
        JsLogger.appendLog(JsLogLevel.INFO, "a")
        JsLogger.appendLog(JsLogLevel.ERROR, "b")
        JsLogger.appendLog(JsLogLevel.SUCCESS, "c")
        // JS 侧 log.get({level:'error'}) 用 name.lowercase() 比对，这里固化该契约
        val errors = JsLogger.logs.value.filter { it.level.name.lowercase() == "error" }
        assertEquals(1, errors.size)
        assertEquals("b", errors.single().message)
    }
}
