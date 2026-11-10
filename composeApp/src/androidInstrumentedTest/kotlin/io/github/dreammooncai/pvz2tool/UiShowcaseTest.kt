package io.github.dreammooncai.pvz2tool

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **新增 UI 组件的效果演示（只跑这一条，不涉及视频/音频）**
 *
 * 启动 [ApiShowcaseActivity] 后依次展示两个形态：
 * 1. **主页功能栏目里的效果**：真实 `PvzCollapsiblePanel` 承载 `NUMBER` 数量条目
 *    （`[«] [−] 数值 [+] [»]`）与 `INFO` 限高滚动条目；
 * 2. **API 弹窗效果**：直接调用 `ui.number(...)` 弹出 PVZ 风格数量弹窗，可手动点按体验。
 *
 * 运行（推荐，约 20 秒）：
 * ```
 * ./gradlew :composeApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=io.github.dreammooncai.pvz2tool.UiShowcaseTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class UiShowcaseTest {

    @Test
    fun showcaseNewUiComponents() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext

        val intent = Intent(targetContext, ApiShowcaseActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()

        try {
            // ================= ① 主页栏目里的条目形态 =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.tab = 1
                ApiShowcaseState.step = "① 主页栏目里的效果：NUMBER 数量条目 + INFO 限高滚动条目"
            }
            instrumentation.waitForIdleSync()
            delay(6_000)

            // ================= ② API 弹窗形态（ui.number） =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.step = "② API 弹窗效果：ui.number 数量弹窗（± / 长按 / « »）"
            }

            // 弹窗会 await 用户操作，放协程里挂起，主流程继续按时收尾
            val dialogJob = launch(Dispatchers.Main) {
                runCatching {
                    JsUiManager.showNumber(
                        ownerContext = null,
                        title = "购买数量",
                        min = 1,
                        max = 99,
                        step = 1,
                        default = 3,
                        suffix = "个"
                    ).await()
                }
            }

            delay(9_000)

            // 到点自动关闭（用户没点确定也不会卡住测试）
            JsUiManager.hideNumber()
            delay(500)
            dialogJob.cancel()
        } finally {
            instrumentation.runOnMainSync { runCatching { activity.finish() } }
        }
    }
}
