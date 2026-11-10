package io.github.dreammooncai.pvz2tool

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **JS API 调用效果演示（可直接用肉眼在模拟器上观察）**
 *
 * 与纯断言测试不同，本用例会启动 [ApiShowcaseActivity]（定义在 debug 变体源集，
 * 因此属于**被测应用进程**、可用 `startActivitySync` 启动，且不会打进 release 包），
 * 然后**真的调用** API：
 * - 屏幕上的日志面板会逐条刷新（`log.add`）；
 * - `video.play` 会弹出全屏播放器并真的开始播放内置视频，随后暂停 / 继续 / 跳转 / 关闭都能看到；
 * - 最后播放一次点击音效（`audio.playSound`）。
 *
 * 运行：
 * ```
 * ./gradlew :composeApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=io.github.dreammooncai.pvz2tool.ApiShowcaseTest
 * ```
 *
 * 说明：用例里的 [delay] 只是「让人看清楚」的停留时间，不影响断言结论。
 */
@RunWith(AndroidJUnit4::class)
class ApiShowcaseTest {

    @Test
    fun showcaseVideoLogAndAudioApis() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext

        // 启动演示界面（Activity 位于 debug 变体，与 instrumentation 同进程）
        val intent = Intent(targetContext, ApiShowcaseActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()

        try {
            // 与正式环境一致的引擎：挂载所有 PvzTool 全局对象
            val runtime = JSRuntime(
                context = Dispatchers.Default + SupervisorJob(),
                console = JsConsole
            )
            PvzToolGlobals.attached(runtime)
            val engine = JSEngine(runtime)

            suspend fun js(expr: String): String =
                runCatching { runtime.toString(engine.compile(expr).invoke()) }
                    .getOrElse { "<error:${it.message}>" }

            fun setStep(text: String) = instrumentation.runOnMainSync { ApiShowcaseState.step = text }

            JsLogger.clearLogs()

            // ================= ① 日志 API =================
            setStep("① 日志 API：log.add 写入三条不同级别日志")
            js("log.clear()")
            js("log.add('演示：这是一条普通日志')")
            js("log.add('演示：这是一条警告日志', 'warn')")
            js("log.add('演示：这是一条成功日志', 'success')")
            delay(2_500)

            val count = js("log.count()").toDouble()
            val successCount = js("log.get({level:'success'}).length").toDouble()
            val text = js("log.text({limit: 1})")
            assertEquals("log 应写入 3 条", 3.0, count, 0.0)
            assertEquals("level 过滤应命中 1 条", 1.0, successCount, 0.0)
            assertTrue("log.text 应包含最后一条内容", text.contains("成功日志"))

            // ================= ② 视频 API：真播 =================
            // video/opening.mp4 是随包内置的真实视频，走资源层解析后由 CG 播放器全屏播放
            setStep("② 视频 API：video.play 全屏播放（画面来自内置 video/opening.mp4）")
            js("video.play('video/opening.mp4', { volume: 0.8 })")
            delay(2_000)
            assertTrue("video.play 后弹窗应可见", js("video.isVisible()").toBooleanStrict())

            // 证明是真的在播放（而不是解码失败降级成海报/黑屏）：能读到片长与已播进度。
            // ⚠️ 这里必须在播放初期就取样：视频播完会自动关闭弹窗，人工点「跳过」也会，
            //    放到后面断言会误判成「没播」。
            val duration = js("video.getDuration()").toDouble()
            val position = js("video.getPosition()").toDouble()
            assertTrue("视频应已加载，getDuration > 0（否则说明降级成了海报）", duration > 0.0)
            assertTrue("视频应已产生播放进度，getPosition > 0", position > 0.0)

            delay(7_000)

            // ================= ③ 暂停 / 继续 / 跳转 =================
            setStep("③ 视频 API：video.pause 暂停（画面定格）")
            js("video.pause()")
            delay(2_500)

            setStep("④ 视频 API：video.resume 继续播放")
            js("video.resume()")
            delay(3_000)

            setStep("⑤ 视频 API：video.seekTo(1000) 跳到第 1 秒")
            js("video.seekTo(1000)")
            delay(2_500)

            // ================= ⑥ 停止关闭 =================
            setStep("⑥ 视频 API：video.stop 停止并关闭弹窗")
            js("video.stop()")
            delay(1_500)
            assertFalse("video.stop 后弹窗应关闭", js("video.isVisible()").toBooleanStrict())

            // ================= ⑦ 音效 API =================
            setStep("⑦ 音频 API：audio.playSound 播放点击音效")
            js("audio.playSound('sound/ui_switch_click.wav')")
            delay(2_000)

            // ================= ⑧ 跳过工具箱 API =================
            // 设置后下次冷启动直接进游戏（不再显示工具箱），App 更新/覆盖安装会自动失效
            setStep("⑧ app.skipToolboxNextLaunch(true)：下次冷启动直接进游戏")
            js("app.skipToolboxNextLaunch(true)")
            val skipAfterSet = InitializePvz2.skipToolboxNextLaunch
            println("[diag] skipToolbox after set: kotlin=$skipAfterSet js=${js("app.isSkipToolboxNextLaunch()")}")
            assertTrue("应已设置「下次启动跳过工具箱」（Kotlin 侧读取）", skipAfterSet)
            delay(1_500)
            // 演示完复原，避免影响后续手动启动调试
            js("app.skipToolboxNextLaunch(false)")
            assertFalse(
                "取消后应恢复为 false",
                InitializePvz2.skipToolboxNextLaunch
            )

            setStep("演示结束：全部 API 调用完成（可查看上方日志）")
            delay(1_500)
        } finally {
            instrumentation.runOnMainSync { runCatching { activity.finish() } }
        }
    }
}

