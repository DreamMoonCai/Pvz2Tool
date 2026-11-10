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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalContext
import io.github.dreammooncai.pvz2tool.ui.dialog.rememberAssetExtractor
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionComponent
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionState
import io.github.dreammooncai.pvz2tool.ui.dialog.JsChoiceItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsDialogsHost
import io.github.dreammooncai.pvz2tool.ui.dialog.JsMoreOptions
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzSlideBar
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
 * 页签 2：新增的**两个 API 弹窗**
 *
 * 1. **多选弹窗增强**：标题栏右上角「全选 / 搜索」+ 底部「数量 ±」，
 *    对应手写需求里「复选支持全选、搜索、数量输入」的那套；
 * 2. **数量弹窗**：`ui.number(...)` 的独立用法。
 *
 * 这里直接调用 `JsUiManager.showXxx(...)`——与 JS 调 `ui.multiSelect(...)` 走的是同一条链路，
 * 所以屏幕上看到的就是脚本弹出来的效果。
 */
@Composable
private fun UiComponentShowcasePanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ========== ① 多选弹窗（全选 / 搜索 / 数量） ==========
        ShowcaseLabel("① 多选弹窗：右上角「全选 / 搜索」+ 数量「±」")
        Text(
            text = "ui.multiSelect('选择功能', items, {selectAll:true, searchable:true, quantity:{min:1,max:99,default:3,suffix:'个'}})\n" +
                    "• 右上角「全选」：对当前**过滤后**的条目一键全选 / 取消全选\n" +
                    "• 右上角「搜索」：按名称过滤条目（勾选状态不受过滤影响）\n" +
                    "• 数量行：「±」步进、长按连续增减、「Min」「Max」直达边界\n" +
                    "• 拖动排序会**持久化**（同 orderKey 下次打开沿用）；列表行尾「≡」把手按住即拖\n" +
                    "• 确定返回 {values:[...], count:n}",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        ShowcaseActionButton("弹出多选弹窗（ui.multiSelect）") {
            scope.launch {
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
                    orderKey = "demo_list",
                    forceMaxForm = true
                ).await()
            }
        }

        Spacer(Modifier.height(6.dp))

        // ========== ①-b 网格布局（带图标，长按图标拖动） ==========
        ShowcaseLabel("①-b 多选弹窗 · 网格布局（长按图标拖动排序）")
        Text(
            text = "条目 ≥ 8 且带 icon 时自动切网格；长按任一格子拖动即可换位（网格没有把手）。\n" +
                    "配合 selectAll / searchable 使用：先搜索再全选，只作用于过滤结果。",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        ShowcaseActionButton("弹出多选弹窗（网格布局）") {
            scope.launch {
                JsUiManager.showMultiSelect(
                    ownerContext = null,
                    title = "选择要启用的功能（网格）",
                    items = DEMO_GRID_CHOICES,
                    defaultValues = listOf("sun"),
                    columns = 3,
                    selectAllEnabled = true,
                    searchEnabled = true,
                    reorderable = true,
                    orderKey = "demo_grid",
                    forceMaxForm = true
                ).await()
            }
        }

        Spacer(Modifier.height(6.dp))

        // ========== ①-c 网格 + 「更多」折叠 ==========
        ShowcaseLabel("①-c 网格 + 「更多」折叠（单行超过 5 个自动收起）")
        Text(
            text = "ui.multiSelect('...', items, { columns:5, more:{text:'更多', limit:5, visible:4} })\n" +
                    "• 首屏只显示前 limit 个（默认 5），其余收进三段式横条：[文字] [计数] [按钮]\n" +
                    "• 点开后横滚区一次可见 visible 个，超出左右滑动\n" +
                    "• text 不传 → 两段式（只有计数 + 按钮）；条目数 ≤ limit 时自动平铺不折叠",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        ShowcaseActionButton("弹出多选弹窗（网格 + 更多折叠）") {
            scope.launch {
                JsUiManager.showMultiSelect(
                    ownerContext = null,
                    title = "全部功能（12 项）",
                    items = DEMO_MORE_CHOICES,
                    defaultValues = listOf("sun"),
                    columns = 5,
                    selectAllEnabled = true,
                    searchEnabled = true,
                    more = JsMoreOptions(text = "更多", limit = 5, visible = 4),
                    orderKey = "demo_more",
                    forceMaxForm = true
                ).await()
            }
        }

        Spacer(Modifier.height(6.dp))

        // ========== ② 滑块弹窗（同样带 ±/«» 步进） ==========
        ShowcaseLabel("② 滑块弹窗：ui.slider(title, options)")
        Text(
            text = "滚动条下方同样有一排「« − 数值 + »」：单击 ± 步进 step、长按连续增减、" +
                    "「«」「»」直达最小 / 最大。\n不需要这排按钮就传 { stepButtons:false }。",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        ShowcaseActionButton("弹出滑块弹窗（ui.slider）") {
            scope.launch {
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

        Spacer(Modifier.height(6.dp))

        // ========== ③ 数量弹窗 ==========
        ShowcaseLabel("③ 数量弹窗：ui.number(title, options)")
        Text(
            text = "ui.number('购买数量', {min:1, max:99, step:1, suffix:'个'}) 弹窗，点确定返回数值（取消返回 null）。",
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

        // ========== ④ 折叠横条栏目（向左展开） ==========
        ShowcaseLabel("④ 折叠横条：点击按钮向左展开（左中右三段）")
        Text(
            text = "收起时右侧只剩一颗方按钮；点击后横条**向左生长**，按钮箭头翻转成「收起」。\n" +
                    "• 展开后是**同一行的左中右三段**：`[文字] [内容] [按钮]`\n" +
                    "• 文字可省略（传 null 即两段式）；内容横向排在中间段，超出部分自己滚\n" +
                    "• 视觉与主界面栏目同源（主题渐变 + 2dp 深色描边 + 8dp 圆角）",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        PvzSlideBar(
            title = "常用功能",
            theme = PvzCollapsiblePanelTheme.BROWN,
            trailingContent = {
                SlideDemoChip("加速")
                SlideDemoChip("静音")
                SlideDemoChip("自动收集")
                SlideDemoChip("无限金币")
            }
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "同上，但 title = null：只有「内容 + 按钮」两段，适合窄屏或纯操作条。",
            color = Color(0xFF8A9199),
            fontSize = 12.sp
        )
        PvzSlideBar(
            title = null,
            theme = PvzCollapsiblePanelTheme.GREEN,
            trailingContent = {
                SlideDemoChip("阳光 +100")
                SlideDemoChip("冷却 -5s")
            }
        )
    }
}

/** 折叠横条演示用的内容槽小胶囊 */
@Composable
private fun SlideDemoChip(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp)
    }
}

/** 多选弹窗演示数据：10 条纯文字条目，便于体验搜索过滤 */
private val DEMO_CHOICES = listOf(
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

/** 网格布局演示数据：9 条带图标（≥8 且带 icon 才会进网格），循环复用已有图标资源 */
private val DEMO_GRID_CHOICES = listOf(
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

/** 「更多」折叠演示数据：12 条带图标（超过 limit=5，会收起 7 条） */
private val DEMO_MORE_CHOICES = DEMO_GRID_CHOICES + listOf(
    JsChoiceItem(name = "昼夜切换", value = "daynight", icon = "new_version_icon.png"),
    JsChoiceItem(name = "无尽模式", value = "endless", icon = "old_version_icon.png"),
    JsChoiceItem(name = "成就全解锁", value = "achievement", icon = "auto_collect.png")
)

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
