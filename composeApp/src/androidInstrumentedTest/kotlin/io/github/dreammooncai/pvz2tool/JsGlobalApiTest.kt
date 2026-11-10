package io.github.dreammooncai.pvz2tool

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.ui.dialog.JsVideoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * JS 全局 API 契约测试：`log`（日志读写）与 `video`（视频播放）。
 *
 * 目的不是「跑一遍完整脚本」，而是锁住**对外契约**：
 * 1. 全局对象与每个方法都被正确注册（`typeof` 检查，防止漏注册/改名）；
 * 2. `log` 的增 / 查 / 计数 / 清空闭环成立，且与 [JsLogger] 缓冲区一致；
 * 3. `video` 的状态机成立（play → 可见，stop → 不可见），且不依赖真实视频文件。
 *
 * 视频这里刻意只用**绝对路径**：相对路径会走资源层（需要完整的工作目录配置），
 * 而绝对路径分支不触碰配置，测试环境即可覆盖。
 */
@RunWith(AndroidJUnit4::class)
class JsGlobalApiTest {

    private lateinit var runtime: JSRuntime
    private lateinit var engine: JSEngine<JSRuntime>

    @Before
    fun setUp() = runBlocking {
        InitializePvz2.context = ApplicationProvider.getApplicationContext()
        JsLogger.clearLogs()
        JsVideoManager.stop()
        runtime = JSRuntime(
            context = Dispatchers.Default + SupervisorJob(),
            console = JsConsole
        )
        PvzToolGlobals.attached(runtime)
        engine = JSEngine(runtime)
    }

    /** 执行一段 JS 并取字符串结果；异常时返回 "<error:...>" 便于断言定位。 */
    private suspend fun eval(expr: String): String =
        runCatching { runtime.toString(engine.compile(expr).invoke()) }
            .getOrElse { "<error:${it.message}>" }

    private suspend fun evalNumber(expr: String): Double = eval(expr).toDouble()

    private suspend fun evalBoolean(expr: String): Boolean = eval(expr).toBooleanStrict()

    // ======================== 全局注册 ========================

    @Test
    fun globalsAreRegistered() = runBlocking {
        assertEquals("object", eval("typeof log"))
        assertEquals("object", eval("typeof video"))
        // 中文别名同样可用
        assertEquals("object", eval("typeof 日志记录"))
        assertEquals("object", eval("typeof 视频"))
    }

    @Test
    fun logApiMethodsExist() = runBlocking {
        listOf("get", "text", "add", "write", "clear", "count").forEach { name ->
            assertEquals("log.$name 应为函数", "function", eval("typeof log.$name"))
        }
    }

    @Test
    fun videoApiMethodsExist() = runBlocking {
        listOf(
            "play", "pause", "resume", "stop", "seekTo",
            "setVolume", "isPlaying", "isVisible", "getDuration", "getPosition"
        ).forEach { name ->
            assertEquals("video.$name 应为函数", "function", eval("typeof video.$name"))
        }
    }

    // ======================== log 行为 ========================

    @Test
    fun logAddThenCountAndClear() = runBlocking {
        assertEquals(0.0, evalNumber("log.count()"), 0.0)

        eval("log.add('hello from test')")
        eval("log.add('warn message', 'warn')")
        assertEquals(2.0, evalNumber("log.count()"), 0.0)
        // 与底层缓冲区保持一致
        assertEquals(2, JsLogger.logs.value.size)
        assertEquals("hello from test", JsLogger.logs.value[0].message)

        eval("log.clear()")
        assertEquals(0.0, evalNumber("log.count()"), 0.0)
        assertEquals(0, JsLogger.logs.value.size)
    }

    @Test
    fun logGetSupportsLimitAndLevel() = runBlocking {
        eval("log.add('a')")
        eval("log.add('b', 'error')")
        eval("log.add('c')")

        assertEquals("get() 应返回全部 3 条", 3.0, evalNumber("log.get().length"), 0.0)
        assertEquals("limit 应只取 1 条", 1.0, evalNumber("log.get({limit: 1}).length"), 0.0)
        assertEquals("add 的级别参数应被记录", "error", eval("log.get()[1].level"))
        assertEquals("level 过滤应只命中 1 条", 1.0, evalNumber("log.get({level: 'error'}).length"), 0.0)
        assertEquals("b", eval("log.get({level:'error'})[0].message"))
        assertEquals("error", eval("log.get({level:'error'})[0].level"))
    }

    @Test
    fun logTextIsLineJoined() = runBlocking {
        eval("log.add('line-1')")
        eval("log.add('line-2')")
        val text = eval("log.text()")
        assertEquals(2, text.lines().size)
        // 每行格式：[时间] 级别 内容
        assertEquals(true, text.lines().first().contains("line-1"))
        assertEquals(true, text.lines().first().contains("INFO"))
    }

    // ======================== video 状态机 ========================

    @Test
    fun videoStartsInvisible() = runBlocking {
        assertEquals(false, evalBoolean("video.isVisible()"))
        assertEquals(false, JsVideoManager.isVisible())
    }

    @Test
    fun videoPlayThenStopTogglesVisibility() = runBlocking {
        // 绝对路径：不触碰资源层与工作目录配置，仅验证状态机
        eval("video.play('/sdcard/pvz2tool_test_absent.mp4')")
        assertEquals(true, evalBoolean("video.isVisible()"))
        assertEquals(true, JsVideoManager.isVisible())

        eval("video.stop()")
        assertEquals(false, evalBoolean("video.isVisible()"))
        assertEquals(false, JsVideoManager.isVisible())
    }

    @Test
    fun videoControlCallsAreSafeWithoutPlayer() = runBlocking {
        // 没有播放器时，控制类 API 不应抛异常（内部全部 runCatching 兜底）
        eval("video.pause()")
        eval("video.resume()")
        eval("video.seekTo(1000)")
        eval("video.setVolume(0.5)")
        assertEquals(false, evalBoolean("video.isPlaying()"))
        assertEquals(0.0, evalNumber("video.getDuration()"), 0.0)
        assertEquals(0.0, evalNumber("video.getPosition()"), 0.0)
    }
}
