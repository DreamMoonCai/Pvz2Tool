package io.github.dreammooncai.pvz2tool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.ui.PvzNumberInput
import io.github.dreammooncai.pvz2tool.ui.dialog.JsDialogsHost
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanel
import kotlinx.coroutines.launch

/**
 * 演示状态：由 [ApiShowcaseTest] / [UiShowcaseTest] 在主线程更新，驱动界面切换与步骤提示。
 */
object ApiShowcaseState {
    var step: String by mutableStateOf("正在准备演示环境…")

    /** 0 = JS API 调用效果；1 = 新增 UI 组件预览 */
    var tab: Int by mutableStateOf(0)
}

/**
 * **演示用 Activity（仅 debug 变体，不打进 release 包）**。
 *
 * 两个页签：
 * 1. **JS API 调用效果**：实时日志面板 + [JsDialogsHost]（`video.play` 的全屏播放器挂在这里）；
 * 2. **新增 UI 组件**：点按钮直接调用**正式 API** `ui.number(...)` 弹出 PVZ 风格数量弹窗，
 *    可以手动点 ± / 长按体验。
 *
 * 运行：
 * ```
 * ./gradlew :composeApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=io.github.dreammooncai.pvz2tool.UiShowcaseTest
 * ```
 */
class ApiShowcaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitializePvz2.context = applicationContext
        // 演示需要资源层（video/opening.mp4 在 APK assets 下）能正常解析
        runCatching { InitializePvz2.initConfig() }
            .onFailure { ApiShowcaseState.step = "配置加载失败：${it.message}" }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E1116))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "Pvz2Tool · 演示",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ShowcaseTab("① JS API 调用效果", ApiShowcaseState.tab == 0) { ApiShowcaseState.tab = 0 }
                            Spacer(Modifier.width(8.dp))
                            ShowcaseTab("② 新增 UI 组件", ApiShowcaseState.tab == 1) { ApiShowcaseState.tab = 1 }
                        }
                        Spacer(Modifier.height(12.dp))

                        if (ApiShowcaseState.tab == 0) {
                            JsApiShowcasePanel(Modifier.weight(1f))
                        } else {
                            UiComponentShowcasePanel(Modifier.weight(1f))
                        }
                    }

                    // 🔴 视频弹窗宿主 + 所有 JS 弹窗（含 ui.number）都挂在这里
                    JsDialogsHost()
                }
            }
        }
    }
}

/** 页签按钮 */
@Composable
private fun ShowcaseTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF1F7A4D) else Color(0xFF1B2027))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF8A9199),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** 页签 1：JS API 调用效果（步骤提示 + 实时日志） */
@Composable
private fun JsApiShowcasePanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = ApiShowcaseState.step,
            color = Color(0xFF58D68D),
            fontSize = 15.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "—— 实时日志（log API 写入的就是这份）——",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))

        val logs by JsLogger.logs.collectAsState()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { entry ->
                Text(
                    text = "[${entry.timestamp}] ${entry.level.name} ${entry.message}",
                    color = when (entry.level.name) {
                        "ERROR" -> Color(0xFFE74C3C)
                        "WARN" -> Color(0xFFF1C40F)
                        "SUCCESS" -> Color(0xFF2ECC71)
                        else -> Color(0xFFBDC3C7)
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * 页签 2：新增 UI 组件的**两种使用形态**
 *
 * 1. **API 弹窗**：点按钮调用正式 API `ui.number(...)`，弹出 PVZ 风格数量弹窗；
 * 2. **主页栏目条目**：用真实的 [PvzCollapsiblePanel] 承载 `NUMBER` 条目与 `INFO` 限高条目，
 *    与主界面栏目里的渲染完全一致。
 */
@Composable
private fun UiComponentShowcasePanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var quantity by remember { mutableStateOf(3) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ========== ① API 弹窗形态 ==========
        ShowcaseLabel("① API 弹窗：ui.number(title, options)")
        Text(
            text = "脚本里一句 ui.number('购买数量', {min:1, max:99, step:1, suffix:'个'}) " +
                    "即可弹出，点确定返回所选数值（取消返回 null）。\n" +
                    "• ± 单击步进一次，长按连续增减\n" +
                    "• « / » 一键跳到最小 / 最大值（到边界自动禁用）",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        ShowcaseActionButton("弹出数量弹窗（ui.number）") {
            scope.launch {
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

        Spacer(Modifier.height(6.dp))

        // ========== ② 主页栏目条目形态 ==========
        ShowcaseLabel("② 主页功能栏目里的效果")
        Text(
            text = "下面是真实栏目面板（PvzCollapsiblePanel）= 主界面同一套渲染：\n" +
                    "NUMBER 数量条目 + INFO 限高滚动条目。",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )

        PvzCollapsiblePanel(
            title = "功能栏目示例",
            isExpandedInit = true
        ) { _ ->
            // —— NUMBER 条目（与主界面 SectionType.NUMBER 分支同构）——
            Text(
                text = "购买数量",
                color = Color(0xFFE8E8E8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "1~99，长按 ± 可连续增减",
                color = Color(0xFF9AA0A6),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            PvzNumberInput(
                value = quantity,
                onValueChange = { quantity = it },
                min = 1,
                max = 99,
                step = 1,
                suffix = "个"
            )
            Text(
                text = "当前值 = $quantity",
                color = Color(0xFF9AA0A6),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                thickness = 0.5.dp,
                color = Color(0x33FFFFFF)
            )

            // —— INFO 条目（maxLines = 5，超出滚动）——
            Text(
                text = "运行状态",
                color = Color(0xFFE8E8E8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "maxLines = 5，超出可在卡片内滚动",
                color = Color(0xFF9AA0A6),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (18 * 5).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFC9D8B6))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = INFO_DEMO_TEXT,
                    color = Color(0xFF1B2027),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private val INFO_DEMO_TEXT = buildString {
    appendLine("这是 INFO 类型信息卡的演示内容。")
    repeat(8) { i ->
        appendLine("第 ${i + 1} 行：INFO 条目通常用来展示 JS 动态计算出来的状态，")
        appendLine("例如当前版本、解压进度、VPN 状态、存档信息等。")
    }
    append("（内容结束）")
}

/** 演示用按钮 */
@Composable
private fun ShowcaseActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F7A4D))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFD5DBE1),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}
