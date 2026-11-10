package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.js.JsRichTextRefresher
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.ui.dialog.LocalUiFrame
import io.github.dreammooncai.pvz2tool.ui.dialog.UiFrame
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionState
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.style.TextOverflow
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.code.JsExecutionContext
import io.github.dreammooncai.pvz2tool.js.code.LocalJsExecutionContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

// --------------- JS 执行辅助方法 ---------------

/**
 * 在给定上下文中执行 JS 表达式（支持内联表达式和 .js 文件）
 * 使用 PvzToolJsEngine.executeScript 带上下文重载，使 JS 中可访问 this.当前
 */
private suspend fun executeJsExprWithContext(
    expr: String,
    context: JsExecutionContext,
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    uiFrame: UiFrame? = null,
): String {
    // 统一执行入口（内联表达式或 .js 文件），返回 JS 引擎给出的字符串结果
    val runScript: suspend (String) -> String? = { code ->
        runCatching {
            PvzToolJsEngine.executeScript(
                script = code,
                section = context.section,
                item = context.item,
                version = context.version,
                sectionStates = context.sectionStates,
                isRichText = true,
                updateSectionState = context.updateSectionState,
                text = text,
                onTextChange = onTextChange,
                uiFrame = uiFrame,
            )
        }.getOrNull()
    }
    return try {
        val raw = if (expr.endsWith(".js", ignoreCase = true)) {
            val jsFile = if (expr.startsWith("/")) {
                runCatching { File(expr).inputStream() }.getOrNull()
            } else runCatching { AssetExtractorHolder.openInputStream("js/${expr.removePrefix("js/")}") }.getOrNull()
            // 只有 .js 文件未找到时才降级为内联表达式执行；
            // 文件已找到并执行过则直接采纳其结果（即便为 null），不能因结果为 null 而回退，
            // 否则会把 expr 这个路径字符串当 JS 源码编译，抛出误导性的 SyntaxError。
            if (jsFile != null) {
                jsFile.use { f -> runScript(f.bufferedReader().readText()) }
            } else {
                runScript(expr)
            }
        } else {
            runScript(expr)
        }
        val rendered = raw?.takeIf { it.isNotBlank() } ?: "{{js:$expr}}"
        // 表达式求值为对象时，引擎默认渲染成无意义的 "[object Object]"；
        // 改用 JSON.stringify 给出可读表示（仅对内联表达式，避免把 .js 文件名当表达式求值）。
        if (rendered == "[object Object]" && !expr.endsWith(".js", ignoreCase = true)) {
            runScript("JSON.stringify($expr)")?.takeIf { it.isNotBlank() && it != "null" && it != "undefined" }
                ?: rendered
        } else {
            rendered
        }
    } catch (e: Exception) {
        "{{js:$expr}}"
    }
}

/**
 * 无上下文执行 JS 表达式（降级方案，用于无法提供上下文的场景）
 * 使用 PvzToolJsEngine.executeScript(script: String) 无参重载
 */
private suspend fun executeJsExprNoContext(expr: String): String {
    val runScript: suspend (String) -> String? = { code ->
        runCatching { PvzToolJsEngine.executeScript(code, source = "复合文本") }.getOrNull()
    }
    return try {
        val raw = if (expr.endsWith(".js", ignoreCase = true)) {
            val jsFile = if (expr.startsWith("/")) {
                runCatching { File(expr).inputStream() }.getOrNull()
            } else runCatching { AssetExtractorHolder.openInputStream("js/${expr.removePrefix("js/")}") }.getOrNull()
            // 同 executeJsExprWithContext：只有文件未找到才降级为内联表达式，
            // 执行结果为 null 属正常，不得回退去编译路径字符串。
            if (jsFile != null) {
                jsFile.use { f -> runScript(f.bufferedReader().readText()) }
            } else {
                runScript(expr)
            }
        } else {
            runScript(expr)
        }
        val rendered = raw?.takeIf { it.isNotBlank() } ?: "{{js:$expr}}"
        if (rendered == "[object Object]" && !expr.endsWith(".js", ignoreCase = true)) {
            runScript("JSON.stringify($expr)")?.takeIf { it.isNotBlank() && it != "null" && it != "undefined" }
                ?: rendered
        } else {
            rendered
        }
    } catch (e: Exception) {
        "{{js:$expr}}"
    }
}

/**
 * 批量求值文本中出现的所有 {{js:...}} 表达式。
 * 有上下文时走带上下文重载（JS 可访问 this.当前），否则降级为无上下文执行。
 * 单个表达式失败时回退为原始标签文本，不影响其余表达式。
 */
private suspend fun evaluateJsExpressions(
    expressions: List<String>,
    context: JsExecutionContext?,
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    uiFrame: UiFrame? = null,
): Map<String, String> {
    val results = mutableMapOf<String, String>()
    for (expr in expressions) {
        results[expr] = try {
            val result = if (context != null) {
                executeJsExprWithContext(expr, context, text, onTextChange, uiFrame)
            } else {
                executeJsExprNoContext(expr)
            }
            result.ifBlank { "{{js:$expr}}" }
        } catch (e: Exception) {
            "{{js:$expr}}"
        }
    }
    return results
}

// --------------- 复合文本链接点击：执行 JS / 跳转浏览器 ---------------

/**
 * 链接目标分类
 * - [JS]：需要作为 JS 直接执行（内联代码，或以 .js 结尾的文件）
 * - [BROWSER]：普通链接，使用浏览器打开
 */
private enum class LinkTarget { JS, BROWSER }

/**
 * 判断链接目标类型：
 * 1. 以 .js 结尾（忽略 ?查询 / #片段）→ 视为 JS 文件（网络 / 绝对本地 / 工具箱相对路径）
 * 2. 带有明确协议（http/https/ftp/mailto/tel/file/...）→ 浏览器打开
 * 3. 其余（无协议、又非 .js 文件）→ 视为内联 JS 代码
 */
private fun classifyLink(url: String): LinkTarget {
    val trimmed = url.trim()
    val pathOnly = trimmed.substringBefore('?').substringBefore('#')
    if (pathOnly.endsWith(".js", ignoreCase = true)) return LinkTarget.JS
    if (hasUrlScheme(trimmed)) return LinkTarget.BROWSER
    return LinkTarget.JS
}

private fun hasUrlScheme(s: String): Boolean {
    val lower = s.lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("ftp://") || lower.startsWith("ftps://") ||
            lower.startsWith("mailto:") || lower.startsWith("tel:") ||
            lower.startsWith("file://") || s.contains("://")
}

/**
 * 加载 .js 文件内容，支持三种来源：
 * - 网络链接（http/https）：下载后读取
 * - 绝对本地路径（以 / 开头）：直接读取文件
 * - 工具箱相对路径（其余）：从 js/ 目录读取（与 {{js:...}} 行为一致）
 * 读取失败时返回 null。
 */
private suspend fun loadJsFileContent(url: String): String? = withContext(Dispatchers.IO) {
    val trimmed = url.trim()
    runCatching {
        when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> {
                val conn = URL(trimmed).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.inputStream.bufferedReader().readText()
            }
            trimmed.startsWith("/") -> {
                File(trimmed).inputStream().bufferedReader().readText()
            }
            else -> {
                // 工具箱相对路径：从 js/ 目录读取
                AssetExtractorHolder.openInputStream("js/${trimmed.removePrefix("js/")}")?.bufferedReader()?.readText()
            }
        }
    }.getOrNull()
}

/**
 * 执行链接点击：JS 目标直接执行（内联代码 / .js 文件），否则打开浏览器。
 */
private fun handleLinkClick(
    context: Context,
    url: String,
    jsContext: JsExecutionContext?,
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    uiFrame: UiFrame? = null,
) {
    if (classifyLink(url) == LinkTarget.BROWSER) {
        openBrowser(context, url)
    } else {
        // 🔴 必须用与弹窗组合生命周期解耦的稳定作用域（JsUiManager.uiScope）：
        // 链接里的 JS（如 {{link|当前.backUI}}）可能直接关闭所在弹窗，组合离场后
        // rememberCoroutineScope 会被 forget，仍在 executeScript（keight jsFn.call 会挂起）上
        // 挂起的协程 resume 时即抛 ForgottenCoroutineScopeException。这与弹窗按钮回调同源，故统一走 uiScope。
        JsUiManager.uiScope.launch {
            runCatching { executeJsFromLink(url, jsContext, text, onTextChange, uiFrame) }
            // 链接点击属于用户交互，脚本执行完后刷新所有 {{js:...}}
            // （executeJsFromLink 内部按 isRichText = true 调用，不会自动触发刷新）
            JsRichTextRefresher.refresh()
        }
    }
}

/**
 * 按照链接规则执行 JS：
 * - 以 .js 结尾 → 加载文件内容后执行
 * - 否则 → 作为内联 JS 代码直接执行
 */
private suspend fun executeJsFromLink(
    url: String,
    jsContext: JsExecutionContext?,
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    uiFrame: UiFrame? = null,
) {
    val trimmed = url.trim()
    val isJsFile = trimmed.substringBefore('?').substringBefore('#')
        .endsWith(".js", ignoreCase = true)
    val code = if (isJsFile) {
        loadJsFileContent(trimmed) ?: return
    } else {
        trimmed
    }
    if (jsContext != null) {
        PvzToolJsEngine.executeScript(
            script = code,
            section = jsContext.section,
            item = jsContext.item,
            version = jsContext.version,
            sectionStates = jsContext.sectionStates,
            isRichText = true,
            updateSectionState = jsContext.updateSectionState,
            text = text,
            onTextChange = onTextChange,
            uiFrame = uiFrame,
        )
    } else {
        // 弹窗内复合文本：若没有显式 jsContext（弹窗 Composable 未提供 LocalJsExecutionContext），
        // 回退到所在帧的 ownerContext（由 JsUiManager.pushFrame 填充）。
        // 这样链接里的 当前.backUI / 当前.xxx 才能正确解析；否则 当前 未定义、点击会抛错并被静默吞掉，
        // 表现就是"点了没反应"。
        val ownerCtx = uiFrame?.ownerContext
        if (ownerCtx != null) {
            // 🔴 修复：复合文本链接里的 当前.backUI 必须作用到「链接所在的那一帧」(uiFrame = 当前弹窗帧)，
            // 而非 ownerContext 上残留的旧 uiFrame。该旧值由 JsUiManager.pushFrame 在「callbackStack 非空」
            // （即本帧由某个祖先回调里打开）时**故意不覆盖**而来，恒比本帧浅一层（指向打开本帧的父帧）。
            // 若直接用它，resolveBackTarget 会把链接的 backUI 判到父帧上，逐层返回时整层跳过
            // （现象：four_level 第 4 层链接直接触发「第二层收到」、return_data 第 2 层链接直接触发根的 onResult）。
            // 此处临时把 ownerContext.uiFrame 覆盖为本帧并在执行后还原，既让链接 backUI 命中本帧，
            // 又不污染该 ownerContext 上其它链接 / 回调（同一 ownerContext 可能被多个帧共享）。
            val prevUiFrame = ownerCtx.uiFrame
            try {
                ownerCtx.uiFrame = uiFrame
                PvzToolJsEngine.executeScript(
                    ownerCtx,
                    script = code,
                    isRichText = true,
                )
            } finally {
                ownerCtx.uiFrame = prevUiFrame
            }
        } else {
            PvzToolJsEngine.executeScript(code, source = "复合文本")
        }
    }
}

/**
 * 使用系统浏览器打开普通链接
 */
private fun openBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

// 1. 简化文字样式配置：移除固定的 blur 和 offset，只保留核心颜色
data class PvzTextStyle(
    val color: Color,
    val shadowColor: Color? = null
)

// 辅助函数：根据字体大小动态计算模糊半径 (16sp -> 4f 为基准)
private fun calculateBlurRadius(fontSize: TextUnit): Float {
    return (fontSize.value / 16f) * 4f
}

@Composable
fun PvzText(
    text: String,
    style: PvzTextStyle,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = style.color,
        style = if (style.shadowColor != null) LocalTextStyle.current.copy(
            shadow = Shadow(
                color = style.shadowColor,
                blurRadius = calculateBlurRadius(fontSize),
                offset = Offset(2f, 2f)
            ),
            fontWeight = fontWeight
        ) else LocalTextStyle.current
    )
}

// ---------------- 预定义样式区域 ----------------

val PvzTextWhiteStyle = PvzTextStyle(
    color = Color.White,
    shadowColor = Color(0xFF252D22)
)

val PvzTextGoldStyle = PvzTextStyle(
    color = Color(0xFFFCB501),
    shadowColor = Color(0xFF493601)
)

val PvzTextGreenStyle = PvzTextStyle(
    color = Color(0xFF21A800),
    shadowColor = Color(0xFF022D00)
)

val PvzTextPurpleStyle = PvzTextStyle(
    color = Color(0xFFD77FFB),
    shadowColor = Color(0xFF3E1050)
)

val PvzTextRedStyle = PvzTextStyle(
    color = Color(0xFFFF5252),
    shadowColor = Color(0xFF5D0000)
)

val PvzTextGrayStyle = PvzTextStyle(
    color = Color(0xFFB0BEC5),
    shadowColor = Color(0xFF37474F)
)

val PvzTextOliveStyle = PvzTextStyle(
    color = Color(0xFF423F00),
    shadowColor = Color(0xFF141200)
)

// 图标标签数据类
private data class IconTag(
    val id: String,
    val path: String,
    val width: TextUnit,
    val height: TextUnit,
    val fullMatch: String,
    val x: Float? = null,   // dp：相对文本起点(首个文字处)的水平偏移；非空即进入浮层模式
    val y: Float? = null,   // dp：相对文本起点的垂直偏移
    val z: Float = 0f       // z-index：>0 在文字之上，<0 在文字之下，图标之间也可分层
) {
    val isOverlay: Boolean get() = x != null || y != null
}

// ---------------- 富文本区域 ----------------

private val DefaultPvzTagStyles = mapOf(
    "green" to PvzTextGreenStyle.copy(shadowColor = null),
    "purple" to PvzTextPurpleStyle.copy(shadowColor = null),
    "red" to PvzTextRedStyle.copy(shadowColor = null),
    "gold" to PvzTextGoldStyle.copy(shadowColor = null),
    "gray" to PvzTextGrayStyle.copy(shadowColor = null),
    "white" to PvzTextWhiteStyle.copy(shadowColor = null),
    "olive" to PvzTextOliveStyle.copy(shadowColor = null),

    // 扩展命名色：供复合文本 {{color:内容}} 直接使用（无阴影）
    "black" to PvzTextStyle(Color.Black),
    "grey" to PvzTextStyle(Color(0xFFB0BEC5)),
    "blue" to PvzTextStyle(Color(0xFF2196F3)),
    "yellow" to PvzTextStyle(Color(0xFFFFEB3B)),
    "orange" to PvzTextStyle(Color(0xFFFF9800)),
    "cyan" to PvzTextStyle(Color(0xFF00BCD4)),
    "pink" to PvzTextStyle(Color(0xFFE91E63)),

    "green-shadow" to PvzTextGreenStyle,
    "purple-shadow" to PvzTextPurpleStyle,
    "red-shadow" to PvzTextRedStyle,
    "gold-shadow" to PvzTextGoldStyle,
    "gray-shadow" to PvzTextGrayStyle,
    "white-shadow" to PvzTextWhiteStyle,
    "olive-shadow" to PvzTextOliveStyle,
)

/**
 * 图标标签解析结果（路径/尺寸/可选坐标与层级）
 */
private data class ParsedIcon(
    val path: String,
    val width: TextUnit? = null,
    val height: TextUnit? = null,
    val x: Float? = null,   // dp，相对文本起点水平偏移
    val y: Float? = null,   // dp，相对文本起点垂直偏移
    val z: Float? = null    // z-index，控制覆盖层级
)

/**
 * 解析图标标签内容，提取路径、宽度、高度以及可选的坐标(x/y, 单位 dp)与层级(z)。
 * 支持两种格式：
 * 1. 带参数：width=80|height=80|x=10|y=-6|z=2:auto_collect.png
 * 2. 无参数：auto_collect.png
 * 只要出现 x= 或 y= 即进入浮层模式（不再占位，按文本起点偏移覆盖）。
 */
private fun parseIconTagContent(content: String, fontSize: TextUnit): ParsedIcon {
    // 判断是否为带参数格式：":" 前面包含 "="（参数特征）
    val lastColonIndex = content.lastIndexOf(":")
    val hasParamsBeforeColon = lastColonIndex > 0 &&
            content.substring(0, lastColonIndex).contains("=")

    if (hasParamsBeforeColon) {
        // 带参数：width=80|height=80|x=10|y=-6|z=2:auto_collect.png
        val paramsPart = content.substring(0, lastColonIndex)
        val path = content.substring(lastColonIndex + 1)
        var width: Float? = null
        var height: Float? = null
        var x: Float? = null
        var y: Float? = null
        var z: Float? = null
        paramsPart.split("|").forEach { part ->
            val trimmed = part.trim()
            when {
                trimmed.startsWith("width=") -> width = trimmed.substringAfter("=").toFloatOrNull()
                trimmed.startsWith("height=") -> height = trimmed.substringAfter("=").toFloatOrNull()
                trimmed.startsWith("x=") -> x = trimmed.substringAfter("=").toFloatOrNull()
                trimmed.startsWith("y=") -> y = trimmed.substringAfter("=").toFloatOrNull()
                trimmed.startsWith("z=") -> z = trimmed.substringAfter("=").toFloatOrNull()
            }
        }
        return ParsedIcon(
            path = path,
            width = width?.sp,
            height = height?.sp,
            x = x,
            y = y,
            z = z
        )
    } else {
        // 无参数：auto_collect.png
        return ParsedIcon(path = content.trim())
    }
}

/**
 * 从原始文本和 JS 缓存结果中解析所有图标标签
 * 支持两种格式：
 * 1. 带参数：{{icon|width=80|height=80:auto_collect.png}}
 * 2. 无参数：{{icon:auto_collect.png}}
 */
private val iconRegex = "\\{\\{icon(:|\\|)([^}]+)\\}\\}".toRegex()

private fun parseIconTags(text: String, jsCache: Map<String, String>, fontSize: TextUnit): List<IconTag> {
    val tags = mutableListOf<IconTag>()
    var globalIndex = 0

    fun addTag(fullMatch: String, p: ParsedIcon) {
        val isOverlay = p.x != null || p.y != null
        tags.add(
            IconTag(
                id = "icon_${globalIndex}_${p.path}",
                path = p.path,
                width = p.width ?: (fontSize * 1.2f),
                height = p.height ?: (fontSize * 1.2f),
                fullMatch = fullMatch,
                x = p.x,
                y = p.y,
                z = p.z ?: if (isOverlay) 1f else 0f
            )
        )
        globalIndex++
    }

    // 从原始 text 中解析图标标签
    iconRegex.findAll(text).forEach { match ->
        addTag(match.value, parseIconTagContent(match.groupValues[2], fontSize))
    }

    // 从 JS 返回结果中解析图标标签（去重）
    jsCache.values.forEach { result ->
        iconRegex.findAll(result).forEach { iconMatch ->
            val fullMatch = iconMatch.value
            if (tags.none { it.fullMatch == fullMatch }) {
                addTag(fullMatch, parseIconTagContent(iconMatch.groupValues[2], fontSize))
            }
        }
    }

    return tags
}

@Composable
fun PvzRichText(
    text: String,
    modifier: Modifier = Modifier,
    defaultStyle: PvzTextStyle = PvzTextGoldStyle,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    jsContext: JsExecutionContext? = null,
) {
    val blurRadius = calculateBlurRadius(fontSize)
    val fixedOffset = Offset(2f, 2f)

    // 让复合文本自身持有可变文本：this.text = "..." 才能触发重组重渲染
    var textState by remember(text) { mutableStateOf(text) }
    val onTextChange: (String) -> Unit = { textState = it }

    // 合并显式参数和 CompositionLocal 中的上下文
    val effectiveContext = jsContext ?: LocalJsExecutionContext.current

    // 当前对话框帧（若此复合文本位于某个弹窗内，则为其对应的 UiFrame；
    // 用于使弹窗内 {{js:}} 可调用 当前.backUI 返回上一层）。非弹窗内为 null。
    val uiFrame = LocalUiFrame.current

    // 链接点击处理：JS 目标执行 JS，普通链接打开浏览器
    val context = LocalContext.current
    val linkListener: LinkInteractionListener = linkListener@ { annotation ->
        val linkUrl = (annotation as? LinkAnnotation.Url)?.url ?: return@linkListener
        handleLinkClick(context, linkUrl, effectiveContext, textState, onTextChange, uiFrame)
    }

    // 1. 从 text 中找出所有 {{js:...}} 表达式
    val jsExpressions = remember(textState) {
        "\\{\\{js:([^}]+)\\}\\}".toRegex().findAll(textState).map { it.groupValues[1] }.toList()
    }

    // 2. 异步执行 JS 并缓存结果（使用 produceState 支持 suspend）
    //    订阅 JsRichTextRefresher.revision：任何用户交互触发的 JS（BUTTON / CHECKBOX / SLIDER /
    //    悬浮窗按钮 / 链接点击）执行完毕后都会自增该版本号，这里随之重新求值，
    //    使 "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}" 这类文本能跟随状态实时更新。
    val jsCache by produceState<Map<String, String>>(emptyMap(), jsExpressions, effectiveContext) {
        if (jsExpressions.isEmpty()) {
            value = emptyMap()
            return@produceState
        }
        var isFirstEmission = true
        // collectLatest：连续刷新信号会取消上一轮（含下面的 delay），天然合并成一次求值
        JsRichTextRefresher.revision.collectLatest {
            // 首帧立即求值；后续刷新做一个极短防抖，合并同一批次的多次通知
            if (!isFirstEmission) delay(60.milliseconds)
            isFirstEmission = false
            value = evaluateJsExpressions(jsExpressions, effectiveContext, textState, onTextChange, uiFrame)
        }
    }

    // 3. 解析所有图标标签（包括原始文本和 JS 返回结果中的）
    val allIconTags = remember(textState, jsCache, fontSize) {
        parseIconTags(textState, jsCache, fontSize)
    }

    // 4. 动态构建 inlineContent 映射表（仅非浮层图标需要占位；浮层图标走覆盖渲染）
    val dynamicInlineContent = allIconTags.filter { !it.isOverlay }.associate { tag ->
        tag.id to InlineTextContent(
            Placeholder(
                width = tag.width,
                height = tag.height,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) { _ ->
            val imagePath = if (tag.path.startsWith("/")) tag.path else "images/${tag.path}"
            AsyncImageFromAssets(
                imagePath,
                contentDescription = tag.path,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 5. 构建富文本
    val annotatedString = buildAnnotatedString {
        val defaultSpanStyle = SpanStyle(
            color = defaultStyle.color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            shadow = if (defaultStyle.shadowColor != null) Shadow(defaultStyle.shadowColor, fixedOffset, blurRadius) else null
        )

        pushStyle(defaultSpanStyle)
        parseRichText(this, textState, jsCache, allIconTags, fixedOffset, blurRadius, defaultStyle, linkListener)
        pop()
    }

    // 6. 浮层图标（带 x/y 坐标）：不占文本空间，按「文本起点(首个文字处) + (x,y)dp」浮于文字之上/之下，z 控制层级
    val density = LocalDensity.current
    val overlayIcons = allIconTags.filter { it.isOverlay }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    if (overlayIcons.isEmpty()) {
        // 无浮层图标：渲染行为与原版完全一致（不引入 Box 包裹，避免改变文本布局/对齐/换行）
        Text(
            text = annotatedString,
            modifier = modifier,
            lineHeight = lineHeight,
            style = LocalTextStyle.current.copy(fontSize = fontSize),
            textAlign = textAlign,
            maxLines = maxLines,
            inlineContent = dynamicInlineContent,
            onTextLayout = { textLayoutResult = it },
            overflow = overflow,
        )
    } else {
        Box(modifier = modifier) {
            // 浮层文本：按内容自适应宽度，由外层 Box 的 modifier 决定整体尺寸，
            // 覆盖层坐标以文本起点(0,0)为锚点，与 Box 左上角对齐。
            Text(
                text = annotatedString,
                modifier = Modifier,
                lineHeight = lineHeight,
                style = LocalTextStyle.current.copy(fontSize = fontSize),
                textAlign = textAlign,
                maxLines = maxLines,
                inlineContent = dynamicInlineContent,
                onTextLayout = { textLayoutResult = it },
                overflow = overflow,
            )

            textLayoutResult?.let { result ->
                // 锚点：整段文本起点（首个文字处），换行不重算
                val anchorLeft: Float
                val anchorTop: Float
                if (result.layoutInput.text.length > 0) {
                    val b = result.getBoundingBox(0)
                    anchorLeft = b.left
                    anchorTop = b.top
                } else {
                    anchorLeft = 0f
                    anchorTop = 0f
                }
                overlayIcons.forEach { icon ->
                    val px = (anchorLeft + (icon.x ?: 0f) * density.density).roundToInt()
                    val py = (anchorTop + (icon.y ?: 0f) * density.density).roundToInt()
                    val wDp = with(density) { icon.width.toDp() }
                    val hDp = with(density) { icon.height.toDp() }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(icon.z)
                    ) {
                        val imagePath = if (icon.path.startsWith("/")) icon.path else "images/${icon.path}"
                        AsyncImageFromAssets(
                            imagePath,
                            contentDescription = icon.path,
                            modifier = Modifier
                                .offset { IntOffset(px, py) }
                                .size(wDp, hDp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 递归解析函数：解析文本中的 {{...}} 标签
 * JS 执行结果通过 jsCache 获取，递归解析 JS 返回文本时不使用缓存（避免无限递归）
 */
private fun parseRichText(
    builder: AnnotatedString.Builder,
    src: String,
    jsCache: Map<String, String>,
    allIconTags: List<IconTag>,
    fixedOffset: Offset,
    blurRadius: Float,
    defaultStyle: PvzTextStyle,
    linkListener: LinkInteractionListener
) {
    var currentIndex = 0
    while (currentIndex < src.length) {
        val start = src.indexOf("{{", currentIndex)
        if (start == -1) {
            builder.append(src.substring(currentIndex))
            break
        }
        builder.append(src.substring(currentIndex, start))

        val end = src.indexOf("}}", start + 2)
        if (end == -1) {
            builder.append(src.substring(start))
            break
        }

        val inner = src.substring(start + 2, end)

        if (inner.contains("|")) {
            val tagName = inner.substringBefore("|").trim()
            val remainder = inner.substringAfter("|")
            val url = remainder.substringBeforeLast(":").trim()
            val displayContent = remainder.substringAfterLast(":")

            if (tagName.startsWith("link")) {
                val styleSuffix = tagName.removePrefix("link-")
                val targetStyle = DefaultPvzTagStyles[styleSuffix] ?: PvzTextStyle(Color(0xFF64B5F6), Color.Black)

                builder.pushLink(LinkAnnotation.Url(url, linkInteractionListener = linkListener))
                builder.withStyle(SpanStyle(
                    color = targetStyle.color,
                    textDecoration = TextDecoration.Underline,
                    shadow = if (targetStyle.shadowColor != null) Shadow(targetStyle.shadowColor, fixedOffset, blurRadius) else null
                )) {
                    builder.append(displayContent)
                }
                builder.pop()
            } else if (tagName == "icon") {
                // 新格式：{{icon|width=xx|height=xx|x=..|y=..|z=..:path}}
                val fullMatch = "{{$inner}}"
                val iconTag = allIconTags.find { it.fullMatch == fullMatch }
                when {
                    iconTag == null -> builder.append("{{$inner}}")
                    iconTag.isOverlay -> { /* 浮层图标：不在文本内占位，交由 Box 覆盖渲染 */ }
                    else -> builder.appendInlineContent(id = iconTag.id, alternateText = "[${iconTag.path}]")
                }
            }
        } else if (inner.contains(":")) {
            val tagName = inner.substringBefore(":").trim()
            val displayContent = inner.substringAfter(":")

            if (tagName == "js") {
                // 使用缓存的 JS 执行结果
                val result = jsCache[displayContent]
                if (result != null) {
                    // 递归解析，对 JS 返回结果不使用缓存（避免无限递归）
                    parseRichText(builder, result, emptyMap(), allIconTags, fixedOffset, blurRadius, defaultStyle, linkListener)
                } else {
                    builder.append("{{$inner}}")
                }
            } else if (tagName == "icon") {
                // 在 allIconTags 中查找（包括 JS 返回结果中的图标）
                val iconTag = allIconTags.find { it.fullMatch == "{{$tagName:$displayContent}}" }
                when {
                    iconTag == null -> builder.append("{{$inner}}")
                    iconTag.isOverlay -> { /* 浮层图标：不在文本内占位 */ }
                    else -> builder.appendInlineContent(id = iconTag.id, alternateText = "[${iconTag.path}]")
                }
            } else {
                val targetStyle = parseHexColorTag(tagName) ?: (DefaultPvzTagStyles[tagName] ?: defaultStyle)
                builder.withStyle(SpanStyle(
                    color = targetStyle.color,
                    shadow = if (targetStyle.shadowColor != null) Shadow(targetStyle.shadowColor, fixedOffset, blurRadius) else null
                )) {
                    builder.append(displayContent)
                }
            }
        } else {
            builder.append("{{$inner}}")
        }

        currentIndex = end + 2
    }
}

/**
 * 解析形如 #RGB / #RRGGBB / #AARRGGBB 的颜色标签名，返回对应样式；非十六进制返回 null。
 * 使复合文本可直接书写 {{#FF0000:内容}} 进行着色。
 */
private fun parseHexColorTag(tag: String): PvzTextStyle? {
    if (!tag.startsWith("#")) return null
    val hex = tag.removePrefix("#")
    val argb = when (hex.length) {
        3 -> "FF" + hex.map { "$it$it" }.joinToString("")
        6 -> "FF$hex"
        8 -> hex
        else -> return null
    }
    val color = runCatching { Color(argb.toLong(16)) }.getOrNull() ?: return null
    return PvzTextStyle(color)
}

/**
 * 工具方法：将 PvzRichText 格式的字符串转换为纯文本
 * 去除 {{tag:内容}} 标记，只保留内容
 */
fun String.stripPvzRichTags(): String {
    val sb = StringBuilder()
    var currentIndex = 0

    while (currentIndex < this.length) {
        val start = this.indexOf("{{", currentIndex)
        if (start == -1) {
            sb.append(this.substring(currentIndex))
            break
        }
        sb.append(this.substring(currentIndex, start))
        val end = this.indexOf("}}", start + 2)
        if (end == -1) {
            sb.append(this.substring(start))
            break
        }

        val inner = this.substring(start + 2, end)
        when {
            // 处理带参数的 link 或 icon（新格式 {{icon|params:path}}）
            inner.contains("|") -> {
                val tagName = inner.substringBefore("|").trim()
                if (tagName == "icon") {
                    // 图标标签不输出路径文字
                } else {
                    val displayContent = inner.substringAfterLast(":")
                    sb.append(displayContent)
                }
            }
            // 处理普通颜色标签，排除 icon
            inner.contains(":") -> {
                val tagName = inner.substringBefore(":").trim()
                val displayContent = inner.substringAfter(":")
                if (tagName != "icon") {
                    sb.append(displayContent)
                }
            }
        }
        currentIndex = end + 2
    }
    return sb.toString()
}
