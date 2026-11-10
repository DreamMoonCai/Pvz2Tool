package io.github.dreammooncai.pvz2tool

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dreammooncai.pvz2tool.ui.dialog.JsChoiceItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **新增 API 弹窗的效果演示（只跑这一条，不涉及视频/音频）**
 *
 * 启动 [ApiShowcaseActivity] 后依次展示两个弹窗：
 * 1. **多选弹窗 · 列表布局**：标题栏「全选 / 搜索」+ 数量行 + 每行「≡」拖动把手（按下即拖）；
 * 1-b. **多选弹窗 · 网格布局**：带图标（≥8 条）自动切网格，**长按图标拖动**换位；
 * 2. **数量弹窗**：`ui.number` 独立用法。
 *
 * 运行（约 25 秒）：
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
            // ================= ① 多选弹窗（全选 / 搜索 / 数量） =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.tab = 1
                ApiShowcaseState.step =
                    "① 多选弹窗：右上角「全选 / 搜索」、数量「±」、长按条目可拖动排序"
            }
            instrumentation.waitForIdleSync()

            // 弹窗会 await 用户操作，放协程里挂起，主流程继续按时收尾
            val multiJob = launch(Dispatchers.Main) {
                runCatching {
                    JsUiManager.showMultiSelect(
                        ownerContext = null,
                        title = "选择要启用的功能",
                        items = DEMO_CHOICES,
                        defaultValues = listOf("sun"),
                        selectAllEnabled = true,
                        searchEnabled = true,
                        quantityEnabled = true,
                        quantityMin = 1,
                        quantityMax = 99,
                        quantityStep = 1,
                        quantityDefault = 3,
                        quantitySuffix = "个",
                        reorderable = true,
                        forceMaxForm = true
                    ).await()
                }
            }

            delay(12_000)
            JsUiManager.hideSelect()
            delay(600)
            multiJob.cancel()

            // ================= ①-b 多选弹窗（网格布局，长按图标拖动） =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.step = "①-b 网格布局：长按图标拖动换位（3 列）"
            }
            val gridJob = launch(Dispatchers.Main) {
                runCatching {
                    JsUiManager.showMultiSelect(
                        ownerContext = null,
                        title = "选择要启用的功能（网格）",
                        items = GRID_CHOICES,
                        defaultValues = listOf("sun"),
                        columns = 3,
                        selectAllEnabled = true,
                        searchEnabled = true,
                        reorderable = true,
                        forceMaxForm = true
                    ).await()
                }
            }
            delay(10_000)
            JsUiManager.hideSelect()
            delay(600)
            gridJob.cancel()

            // ================= ② 滑块弹窗（带 ± / « » 步进） =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.step = "② 滑块弹窗：ui.slider（滑块下方 ± / « » 步进，可手动点按/长按）"
            }
            val sliderJob = launch(Dispatchers.Main) {
                runCatching {
                    JsUiManager.showSlider(
                        ownerContext = null,
                        title = "音量调节",
                        min = 0.0,
                        max = 100.0,
                        step = 5.0,
                        default = 50.0,
                        unit = "%",
                        decimals = 0
                    ).await()
                }
            }
            delay(9_000)
            JsUiManager.hideSlider()
            delay(500)
            sliderJob.cancel()

            // ================= ② 数量弹窗（ui.number） =================
            instrumentation.runOnMainSync {
                ApiShowcaseState.step = "③ 数量弹窗：ui.number（± / 长按 / « »）"
            }
            val numberJob = launch(Dispatchers.Main) {
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

            delay(8_000)
            JsUiManager.hideNumber()
            delay(500)
            numberJob.cancel()
        } finally {
            instrumentation.runOnMainSync { runCatching { activity.finish() } }
        }
    }

    private companion object {
        /** 与演示页一致的条目：10 条纯文字，便于体验搜索过滤 */
        val DEMO_CHOICES = listOf(
            JsChoiceItem(name = "阳光无限", value = "sun"),
            JsChoiceItem(name = "冷却无CD", value = "cool"),
            JsChoiceItem(name = "自动收集", value = "auto"),
            JsChoiceItem(name = "自动种植", value = "plant"),
            JsChoiceItem(name = "加速战斗", value = "speed"),
            JsChoiceItem(name = "无限金币", value = "gold"),
            JsChoiceItem(name = "无限钻石", value = "diamond"),
            JsChoiceItem(name = "免内购", value = "iap"),
            JsChoiceItem(name = "关卡解锁", value = "level"),
            JsChoiceItem(name = "植物全解锁", value = "plants")
        )

        /** 网格布局演示：9 条带图标（≥8 且带 icon 才会进网格） */
        val GRID_CHOICES = listOf(
            JsChoiceItem(name = "阳光无限", value = "sun", icon = "auto_collect.png"),
            JsChoiceItem(name = "冷却无CD", value = "cool", icon = "save_icon.png"),
            JsChoiceItem(name = "自动收集", value = "auto", icon = "auto_collect.png"),
            JsChoiceItem(name = "自动种植", value = "plant", icon = "save_icon.png"),
            JsChoiceItem(name = "加速战斗", value = "speed", icon = "new_version_icon.png"),
            JsChoiceItem(name = "无限金币", value = "gold", icon = "old_version_icon.png"),
            JsChoiceItem(name = "无限钻石", value = "diamond", icon = "new_version_icon.png"),
            JsChoiceItem(name = "关卡解锁", value = "level", icon = "old_version_icon.png"),
            JsChoiceItem(name = "植物全解锁", value = "plants", icon = "ic_floating_dave.png")
        )
    }
}
