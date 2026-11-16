package io.github.dreammooncai.pvz2tool.ui.dialog

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.ui.PvzInput
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzSimpleCardGreen
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * 复合文本工具（共享实现）
 *
 * 同一份 UI / 拼接逻辑被两处复用，**不要再各写一份**：
 *  - 工具箱集成器右上角入口（[io.github.dreammooncai.pvz2tool.ui.integration.ToolboxIntegratorScreen]）
 *  - JS API `ui.compositeText(options)`（[JsCompositeTextDialog]）
 *
 * 复合文本语法细节见 `assets/pvz2tool/js_documentation.md` →「复合文本语法」章节。
 * 记忆要点：`{{...}}` **不支持嵌套**；`{{link|JS:显示文字}}` 按**最后一个** `:` 切分，
 * 因此 JS 代码里可以有冒号、显示文字里不能有冒号。
 */

// ── PVZ 主题色（与集成器同一套绿系，避免两处色值漂移）──────────
private val CtGreen = Color(0xFF689F38)        // PVZ 主绿（边框）
private val CtGreenSurface = Color(0xFFE8F5D0) // 浅绿面
private val CtGreenBright = Color(0xFF8ED229)  // 高亮绿（选中态）
private val CtCardBg = Color(0xFFD4E8A0)       // 预览/结果卡片底
private val CtCardText = Color(0xFF33691E)     // 卡片深绿文字
private val CtOliveStyle = PvzTextOliveStyle.copy(shadowColor = null)

/** 复合文本类型 */
enum class CompositeTextType(val label: String, val mainLabel: String) {
    PLAIN("普通文字", "文字内容"),
    COLOR("颜色文本", "文字内容"),
    JS("JS表达式", "JS表达式"),
    ICON("图标", "图标路径"),
    LINK("链接", "显示文字");

    companion object {
        /** 兼容 JS 传入的各种写法（英文名大小写不敏感 / 中文标签 / 常见别名）；无法识别返回 null */
        fun fromKey(key: String?): CompositeTextType? {
            val k = key?.trim()?.lowercase() ?: return null
            if (k.isEmpty()) return null
            entries.forEach { t ->
                if (t.name.lowercase() == k || t.label == key.trim()) return t
            }
            return when (k) {
                "text", "普通", "文字", "普通文字" -> PLAIN
                "color", "colour", "颜色", "颜色文本" -> COLOR
                "js", "表达式", "js表达式", "js脚本" -> JS
                "icon", "图标", "图片" -> ICON
                "link", "链接", "超链接" -> LINK
                else -> null
            }
        }
    }
}

/** 颜色选取模式 */
enum class CompositeTextColorMode(val label: String) {
    PRESET("预设色"),
    CUSTOM("自定义色");

    companion object {
        fun fromKey(key: String?): CompositeTextColorMode? {
            val k = key?.trim()?.lowercase() ?: return null
            if (k.isEmpty()) return null
            entries.forEach { m ->
                if (m.name.lowercase() == k || m.label == key.trim()) return m
            }
            return when (k) {
                "preset", "预设", "预设色", "named", "命名色" -> PRESET
                "custom", "hex", "自定义", "自定义色", "十六进制" -> CUSTOM
                else -> null
            }
        }
    }
}

/**
 * 复合文本工具的入参 / 当前状态。
 *
 * 全部字段均为「字符串 + 枚举」（不做数字转换），因为 UI 里就是文本框：
 * 留空表示不追加该参数，解析器会走默认行为（如图标宽高留空 → 按字号自适应）。
 */
data class CompositeTextParams(
    val type: CompositeTextType = CompositeTextType.PLAIN,
    val text: String = "",
    val colorMode: CompositeTextColorMode = CompositeTextColorMode.PRESET,
    val presetColor: String = CompositeTextBuilder.presetColors.first(),
    val customHex: String = "#FF0000",
    val iconWidth: String = "",
    val iconHeight: String = "",
    val iconX: String = "",
    val iconY: String = "",
    val iconZ: String = "",
    val linkUrl: String = "",
    val linkStyle: String = ""
)

/**
 * 复合文本拼接器（纯函数，无 Compose / 无 Android 依赖，可单测）。
 */
object CompositeTextBuilder {

    /** 预设色（与 PvzText.DefaultPvzTagStyles 命名一致，含 -shadow 变体） */
    val presetColors: List<String> = listOf(
        "green", "purple", "red", "gold", "gray", "white", "olive",
        "black", "grey", "blue", "yellow", "orange", "cyan", "pink",
        "green-shadow", "purple-shadow", "red-shadow", "gold-shadow",
        "gray-shadow", "white-shadow", "olive-shadow"
    )

    /** 链接样式后缀（对应 link-<style>） */
    val linkStyles: List<String> = listOf(
        "", "green", "purple", "red", "gold", "gray", "white", "olive", "blue", "cyan", "pink"
    )

    /**
     * 根据所选类型与参数拼接出最终的复合文本标记。
     * - 普通文字：原样返回
     * - 颜色文本：`{{color:内容}}` 或 `{{#RRGGBB:内容}}`
     * - JS表达式：`{{js:表达式}}`
     * - 图标：`{{icon:path}}`（宽高留空则不写，由字号自适应）
     *   或 `{{icon|width=w|height=h:path}}`（可选 x/y/z：带 x 或 y 即进入浮层模式，不占位、按文本起点偏移覆盖）
     * - 链接：`{{link:url:显示}}` 或 `{{link-green:url:显示}}`
     *   （url 可为网址——点击用浏览器打开，或 JS 代码/.js 文件——点击执行）
     */
    fun build(params: CompositeTextParams): String = with(params) {
        when (type) {
            CompositeTextType.PLAIN -> text
            CompositeTextType.COLOR -> {
                val c = if (colorMode == CompositeTextColorMode.PRESET) presetColor else customHex.trim()
                if (c.isBlank()) text else "{{$c:$text}}"
            }
            CompositeTextType.JS -> if (text.isBlank()) "" else "{{js:$text}}"
            CompositeTextType.ICON -> {
                // 仅当用户填写才追加对应参数：宽/高留空即省略（解析器按 fontSize*1.2f 自适应）；
                // x 或 y 任一存在即触发浮层模式；z 控制层级（>0 在文字之上、<0 之下）。
                val parts = mutableListOf<String>()
                if (iconWidth.isNotBlank()) parts += "width=$iconWidth"
                if (iconHeight.isNotBlank()) parts += "height=$iconHeight"
                if (iconX.isNotBlank()) parts += "x=$iconX"
                if (iconY.isNotBlank()) parts += "y=$iconY"
                if (iconZ.isNotBlank()) parts += "z=$iconZ"
                if (parts.isEmpty()) "{{icon:$text}}"
                else "{{icon|${parts.joinToString("|")}:$text}}"
            }
            CompositeTextType.LINK -> {
                val tag = "link" + if (linkStyle.isNotBlank()) "-$linkStyle" else ""
                "{{$tag|$linkUrl:$text}}"
            }
        }
    }
}

/** 类型选择器（分段按钮） */
@Composable
private fun CtTypeSelector(selected: CompositeTextType, onSelect: (CompositeTextType) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompositeTextType.entries.forEach { t ->
            val sel = t == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) CtGreenBright else CtGreenSurface)
                    .border(1.dp, CtGreen, RoundedCornerShape(8.dp))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                PvzRichText(
                    t.label,
                    defaultStyle = if (sel) PvzTextWhiteStyle.copy(shadowColor = null) else CtOliveStyle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** 下拉选择器（点击展开内联列表，避免引入额外依赖） */
@Composable
private fun CtSpinner(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        PvzRichText(label, defaultStyle = CtOliveStyle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CtGreenSurface)
                .border(1.dp, CtGreen, RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                PvzRichText(
                    if (selected.isBlank()) "（默认/无）" else selected,
                    defaultStyle = CtOliveStyle, fontSize = 13.sp
                )
                PvzRichText("▼", defaultStyle = CtOliveStyle, fontSize = 12.sp)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, CtGreen, RoundedCornerShape(8.dp))
            ) {
                options.forEach { opt ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(opt); expanded = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        PvzRichText(
                            if (opt.isBlank()) "（默认/无）" else opt,
                            defaultStyle = CtOliveStyle, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/** 单行输入（本文件自用，标题即浮动标签） */
@Composable
private fun CtInput(
    value: String,
    placeholder: String,
    label: String,
    multiline: Boolean = false,
    onValue: (String) -> Unit
) {
    PvzInput(
        value = value,
        onValueChange = onValue,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        label = label,
        multiline = multiline,
        theme = PvzCollapsiblePanelTheme.GREEN
    )
}

/**
 * 复合文本工具弹窗：输入文字 → 选择类型 → 按类型显示更多自定义参数 → 实时预览 → 复制。
 *
 * @param dialogKey `remember` 的复位键：**每次弹窗必须传一个唯一值**（JS 侧传 deferred）。
 *                  嵌套弹窗时父弹窗会离开组合、`remember` 丢失，靠它决定重新组合时是「恢复」还是「重置」。
 * @param params 初始参数（JS 可预填；集成器入口用默认值）
 * @param copyOnConfirm 点确认时是否复制到剪贴板
 * @param onCancel 取消 / 点外部 / 返回键
 * @param onConfirm 确认，回传最终生成的复合文本
 */
@Composable
fun CompositeTextToolDialog(
    dialogKey: Any?,
    params: CompositeTextParams = CompositeTextParams(),
    title: String = "复合文本工具",
    confirmText: String = "复制",
    cancelText: String = "取消",
    dismissible: Boolean = true,
    copyOnConfirm: Boolean = true,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var ctType by remember(dialogKey) { mutableStateOf(params.type) }
    var ctText by remember(dialogKey) { mutableStateOf(params.text) }
    var ctColorMode by remember(dialogKey) { mutableStateOf(params.colorMode) }
    var ctPresetColor by remember(dialogKey) { mutableStateOf(params.presetColor) }
    var ctCustomHex by remember(dialogKey) { mutableStateOf(params.customHex) }
    var ctIconWidth by remember(dialogKey) { mutableStateOf(params.iconWidth) }
    var ctIconHeight by remember(dialogKey) { mutableStateOf(params.iconHeight) }
    var ctIconX by remember(dialogKey) { mutableStateOf(params.iconX) }
    var ctIconY by remember(dialogKey) { mutableStateOf(params.iconY) }
    var ctIconZ by remember(dialogKey) { mutableStateOf(params.iconZ) }
    var ctLinkUrl by remember(dialogKey) { mutableStateOf(params.linkUrl) }
    var ctLinkStyle by remember(dialogKey) { mutableStateOf(params.linkStyle) }

    val generated = CompositeTextBuilder.build(
        CompositeTextParams(
            type = ctType,
            text = ctText,
            colorMode = ctColorMode,
            presetColor = ctPresetColor,
            customHex = ctCustomHex,
            iconWidth = ctIconWidth,
            iconHeight = ctIconHeight,
            iconX = ctIconX,
            iconY = ctIconY,
            iconZ = ctIconZ,
            linkUrl = ctLinkUrl,
            linkStyle = ctLinkStyle
        )
    )

    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PvzStyledDialog(
        isVisible = true,
        titleText = title,
        onDismissRequest = onCancel,
        dismissible = dismissible,
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PvzRedButton(
                    text = cancelText,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    onClick = onCancel
                )
                PvzGreenButton(
                    text = confirmText,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    onClick = {
                        if (copyOnConfirm) {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("composite_text", generated))
                                )
                            }
                            Toast.makeText(context, "已复制：$generated", Toast.LENGTH_SHORT).show()
                        }
                        onConfirm(generated)
                    }
                )
            }
        }
    ) {
        // 内容整体加边距（包住全部，避免类型以下顶格）
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 文字内容
            CtInput(
                value = ctText,
                placeholder = "在此输入${ctType.mainLabel}",
                label = ctType.mainLabel,
                multiline = true
            ) { ctText = it }

            Spacer(Modifier.height(12.dp))

            // 类型选择
            PvzRichText("类型", defaultStyle = CtOliveStyle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            CtTypeSelector(ctType) { ctType = it }

            Spacer(Modifier.height(12.dp))

            // 类型相关自定义参数
            when (ctType) {
                CompositeTextType.COLOR -> {
                    // 颜色模式切换
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompositeTextColorMode.entries.forEach { m ->
                            val sel = m == ctColorMode
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) CtGreenBright else CtGreenSurface)
                                    .border(1.dp, CtGreen, RoundedCornerShape(8.dp))
                                    .clickable { ctColorMode = m }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                PvzRichText(
                                    m.label,
                                    defaultStyle = if (sel) PvzTextWhiteStyle.copy(shadowColor = null) else CtOliveStyle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (ctColorMode == CompositeTextColorMode.PRESET) {
                        CtSpinner("预设色", CompositeTextBuilder.presetColors, ctPresetColor) { ctPresetColor = it }
                    } else {
                        CtInput(
                            value = ctCustomHex,
                            placeholder = "#FF0000",
                            label = "十六进制色（如 #FF0000 / #AARRGGBB）"
                        ) { ctCustomHex = it }
                    }
                }
                CompositeTextType.ICON -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            CtInput(ctIconWidth, "留空自适应", "宽度(留空自适应)") { ctIconWidth = it }
                        }
                        Column(Modifier.weight(1f)) {
                            CtInput(ctIconHeight, "留空自适应", "高度(留空自适应)") { ctIconHeight = it }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            CtInput(ctIconX, "如 10", "X偏移(dp,可选)") { ctIconX = it }
                        }
                        Column(Modifier.weight(1f)) {
                            CtInput(ctIconY, "-6", "Y偏移(dp,可选)") { ctIconY = it }
                        }
                        Column(Modifier.weight(1f)) {
                            CtInput(ctIconZ, "2", "层级z(可选)") { ctIconZ = it }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    PvzRichText("提示：图标相对 images/ 目录，如 auto_collect.png", defaultStyle = CtOliveStyle, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    PvzRichText(
                        "填写 X 或 Y 即进入浮层模式：图标不再占位，从整段文本起点按偏移覆盖其他文字；z>0 在文字之上、<0 在下、可分层。",
                        defaultStyle = CtOliveStyle, fontSize = 12.sp
                    )
                }
                CompositeTextType.LINK -> {
                    CtInput(ctLinkUrl, "https://... 或 JS代码", "链接地址 / JS代码(点击执行)") { ctLinkUrl = it }
                    Spacer(Modifier.height(8.dp))
                    CtSpinner("链接样式(可选)", CompositeTextBuilder.linkStyles, ctLinkStyle) { ctLinkStyle = it }
                    Spacer(Modifier.height(4.dp))
                    PvzRichText(
                        "提示：地址可为网址（点击用浏览器打开），也可填 JS 代码或 .js 文件（点击即执行，相当于点击式 JS）。",
                        defaultStyle = CtOliveStyle, fontSize = 12.sp
                    )
                }
                else -> {
                    PvzRichText(
                        if (ctType == CompositeTextType.JS) "提示：表达式将在运行时求值，如 vpn.isActive() ? '开' : '关'"
                        else "无需额外参数",
                        defaultStyle = CtOliveStyle, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 实时预览（图标两侧加示例文字，便于查看图标在文字中的内联效果；
            // 若使用 X/Y 坐标，需有周围文字作为锚点，否则图标因无文本起点可能不显示）
            PvzRichText("预览", defaultStyle = CtOliveStyle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            PvzSimpleCardGreen(borderColor = CtCardBg, backgroundColor = CtCardBg) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PvzRichText(
                        if (generated.isBlank()) "（输出为空）" else "示例文字 $generated 示例文字",
                        defaultStyle = CtOliveStyle,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PvzRichText(
                "提示：使用 X/Y 坐标时，图标漂浮在文字之上/下且不占位，需要周围有文字提供起点锚点；若整段只有图标本身，坐标模式可能无法定位而不显示。",
                defaultStyle = CtOliveStyle, fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            // 生成的原始文本
            PvzRichText("生成结果", defaultStyle = CtOliveStyle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            PvzSimpleCardGreen(borderColor = CtCardBg, backgroundColor = CtCardBg) {
                Text(
                    generated.ifBlank { "（输出为空）" },
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = CtCardText,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// ======================== JS 弹窗宿主 ========================

/**
 * JS 复合文本工具弹窗状态（`ui.compositeText`）。
 *
 * `deferred` 承载返回值：确认 → 生成的复合文本；取消 / 点外部 → null。
 */
data class JsCompositeTextState(
    val isVisible: Boolean = false,
    val title: String = "复合文本工具",
    val params: CompositeTextParams = CompositeTextParams(),
    val confirmText: String = "复制",
    val cancelText: String = "取消",
    val copyOnConfirm: Boolean = true,
    val dismissible: Boolean = true,
    val onResult: (suspend (JsAny?) -> Unit)? = null,
    val onClose: (suspend (JsAny?) -> Unit)? = null,
    val deferred: CompletableDeferred<String?>? = null
)

/**
 * JS 复合文本工具弹窗宿主（挂在 [JsDialogsHost]）。
 *
 * 与其它 JS 弹窗共用同一套弹窗栈：`立即关闭` 走 [JsUiManager.dismissSelfWithCallback]
 * 而不是直接 `hideXxx`，避免误关回调中新开的子弹窗。
 */
@Composable
fun JsCompositeTextDialog() {
    val state by JsUiManager.compositeTextState.collectAsState()
    if (!state.isVisible) return

    CompositionLocalProvider(LocalUiFrame provides JsUiManager.topFrame()) {
        val scope = rememberCoroutineScope()
        // 🔴 先取快照：dismissSelfWithCallback 会触发 sync() 把 _compositeTextState 写成空态，
        // 若协程内继续读 state 就会拿到空对象 → deferred 不补全 / 定位不到本帧 → 弹窗关不掉。
        val self = state
        CompositeTextToolDialog(
            dialogKey = self.deferred,
            params = self.params,
            title = self.title,
            confirmText = self.confirmText,
            cancelText = self.cancelText,
            dismissible = self.dismissible,
            copyOnConfirm = self.copyOnConfirm,
            onCancel = {
                scope.launch {
                    JsUiManager.dismissSelfWithCallback(self, null) { self.onClose?.invoke(null) }
                }
            },
            onConfirm = { generated ->
                scope.launch {
                    JsUiManager.dismissSelfWithCallback(self, generated) {
                        self.onResult?.invoke(generated.js)
                    }
                }
            }
        )
    }
}
