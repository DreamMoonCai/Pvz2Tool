package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.ui.main.DynamicSectionState
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder

// JS 执行上下文：携带当前渲染环境信息，使 {{js:...}} 可访问 this.当前
data class JsExecutionContext(
    val section: DynamicSection? = null,
    val item: SectionItem? = null,
    val version: VersionDef,
    val sectionStates: Map<String, DynamicSectionState> = emptyMap(),
    val updateSectionState: ((String, (DynamicSectionState) -> DynamicSectionState) -> Unit)? = null,
)

// 通过 CompositionLocal 向下传递 JS 执行上下文，避免每个 PvzRichText 调用点手动传参
val LocalJsExecutionContext = compositionLocalOf<JsExecutionContext?> { null }

// --------------- JS 执行辅助方法 ---------------

/**
 * 在给定上下文中执行 JS 表达式（支持内联表达式和 .js 文件）
 * 使用 PvzToolJsEngine.executeScript 带上下文重载，使 JS 中可访问 this.当前
 */
private suspend fun executeJsExprWithContext(expr: String, context: JsExecutionContext): String {
    return try {
        val result = if (expr.endsWith(".js")) {
            val jsFile = AssetExtractorHolder.openInputStream("js/$expr")
            jsFile?.use { jsFile ->
                val jsCode = jsFile.bufferedReader().readText()
                PvzToolJsEngine.executeScript(
                    script = jsCode,
                    section = context.section,
                    item = context.item,
                    version = context.version,
                    sectionStates = context.sectionStates,
                    updateSectionState = context.updateSectionState
                )
            } ?: // .js 文件未找到，降级为内联表达式执行
            PvzToolJsEngine.executeScript(
                script = expr,
                section = context.section,
                item = context.item,
                version = context.version,
                sectionStates = context.sectionStates,
                updateSectionState = context.updateSectionState
            )
        } else {
            // 内联表达式
            PvzToolJsEngine.executeScript(
                script = expr,
                section = context.section,
                item = context.item,
                version = context.version,
                sectionStates = context.sectionStates,
                updateSectionState = context.updateSectionState
            )
        }
        result.ifBlank { "{{js:$expr}}" }
    } catch (e: Exception) {
        "{{js:$expr}}"
    }
}

/**
 * 无上下文执行 JS 表达式（降级方案，用于无法提供上下文的场景）
 * 使用 PvzToolJsEngine.executeScript(script: String) 无参重载
 */
private suspend fun executeJsExprNoContext(expr: String): String {
    return try {
        val result = if (expr.endsWith(".js")) {
            val jsFile = AssetExtractorHolder.openInputStream("js/$expr")
            jsFile?.use { jsFile ->
                val jsCode = jsFile.bufferedReader().readText()
                PvzToolJsEngine.executeScript(jsCode)
            } ?: PvzToolJsEngine.executeScript(expr)
        } else {
            PvzToolJsEngine.executeScript(expr)
        }
        result.ifBlank { "{{js:$expr}}" }
    } catch (e: Exception) {
        "{{js:$expr}}"
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
    val fullMatch: String
)

// ---------------- 富文本区域 ----------------

private val DefaultPvzTagStyles = mapOf(
    "green" to PvzTextGreenStyle.copy(shadowColor = null),
    "purple" to PvzTextPurpleStyle.copy(shadowColor = null),
    "red" to PvzTextRedStyle.copy(shadowColor = null),
    "gold" to PvzTextGoldStyle.copy(shadowColor = null),
    "gray" to PvzTextGrayStyle.copy(shadowColor = null),
    "white" to PvzTextWhiteStyle.copy(shadowColor = null),
    "olive" to PvzTextOliveStyle.copy(shadowColor = null),

    "green-shadow" to PvzTextGreenStyle,
    "purple-shadow" to PvzTextPurpleStyle,
    "red-shadow" to PvzTextRedStyle,
    "gold-shadow" to PvzTextGoldStyle,
    "gray-shadow" to PvzTextGrayStyle,
    "white-shadow" to PvzTextWhiteStyle,
    "olive-shadow" to PvzTextOliveStyle,
)

/**
 * 解析图标标签内容，提取路径、宽度、高度
 * 支持两种格式：
 * 1. 新格式（参数在冒号前）：width=80|height=80:auto_collect.png
 * 2. 旧格式（参数在路径后）：auto_collect.png|width=80|height=80
 * 3. 无参数：auto_collect.png
 */
private fun parseIconTagContent(content: String, fontSize: TextUnit): Triple<String, TextUnit, TextUnit> {
    // 判断是否为新格式：包含 "|" 且包含 ":" 且 ":" 在最后一个 "|" 之后
    // 新格式特征：params:path，即 ":" 前面是参数（含 "="）
    val lastColonIndex = content.lastIndexOf(":")
    val hasParamsBeforeColon = lastColonIndex > 0 &&
            content.substring(0, lastColonIndex).contains("=")

    if (hasParamsBeforeColon) {
        // 新格式：width=80|height=80:auto_collect.png
        val paramsPart = content.substring(0, lastColonIndex)
        val path = content.substring(lastColonIndex + 1)
        var width: TextUnit? = null
        var height: TextUnit? = null
        paramsPart.split("|").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.startsWith("width=")) {
                width = trimmed.substringAfter("=").toFloatOrNull()?.sp
            } else if (trimmed.startsWith("height=")) {
                height = trimmed.substringAfter("=").toFloatOrNull()?.sp
            }
        }
        return Triple(path, width ?: (fontSize * 1.2f), height ?: (fontSize * 1.2f))
    } else {
        // 旧格式或无参数：auto_collect.png|width=80|height=80 或 auto_collect.png
        val parts = content.split("|").map { it.trim() }
        val path = parts[0]
        var width: TextUnit? = null
        var height: TextUnit? = null
        for (part in parts.drop(1)) {
            if (part.startsWith("width=")) {
                width = part.substringAfter("=").toFloatOrNull()?.sp
            } else if (part.startsWith("height=")) {
                height = part.substringAfter("=").toFloatOrNull()?.sp
            }
        }
        return Triple(path, width ?: (fontSize * 1.2f), height ?: (fontSize * 1.2f))
    }
}

/**
 * 从原始文本和 JS 缓存结果中解析所有图标标签
 * 支持两种格式：
 * 1. 新格式（参数在冒号前）：{{icon|width=80|height=80:auto_collect.png}}
 * 2. 旧格式（参数在路径后）：{{icon:auto_collect.png|width=80|height=80}} 或 {{icon:auto_collect.png}}
 */
private val iconRegex = "\\{\\{icon(:|\\|)([^}]+)\\}\\}".toRegex()

private fun parseIconTags(text: String, jsCache: Map<String, String>, fontSize: TextUnit): List<IconTag> {
    val tags = mutableListOf<IconTag>()
    var globalIndex = 0

    // 从原始 text 中解析图标标签
    iconRegex.findAll(text).forEach { match ->
        val content = match.groupValues[2]
        val (path, width, height) = parseIconTagContent(content, fontSize)
        tags.add(
            IconTag(
                id = "icon_${globalIndex}_$path",
                path = path,
                width = width,
                height = height,
                fullMatch = match.value
            )
        )
        globalIndex++
    }

    // 从 JS 返回结果中解析图标标签
    jsCache.values.forEach { result ->
        iconRegex.findAll(result).forEach { iconMatch ->
            val content = iconMatch.groupValues[2]
            val (iconPath, iconWidth, iconHeight) = parseIconTagContent(content, fontSize)
            val fullMatch = iconMatch.value
            // 去重：如果 fullMatch 已存在则跳过
            if (tags.none { it.fullMatch == fullMatch }) {
                tags.add(
                    IconTag(
                        id = "icon_${globalIndex}_$iconPath",
                        path = iconPath,
                        width = iconWidth,
                        height = iconHeight,
                        fullMatch = fullMatch
                    )
                )
                globalIndex++
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
    jsContext: JsExecutionContext? = null,
) {
    val blurRadius = calculateBlurRadius(fontSize)
    val fixedOffset = Offset(2f, 2f)

    // 合并显式参数和 CompositionLocal 中的上下文
    val effectiveContext = jsContext ?: LocalJsExecutionContext.current

    // 1. 从 text 中找出所有 {{js:...}} 表达式
    val jsExpressions = remember(text) {
        "\\{\\{js:([^}]+)\\}\\}".toRegex().findAll(text).map { it.groupValues[1] }.toList()
    }

    // 2. 异步执行 JS 并缓存结果（使用 produceState 支持 suspend）
    val jsCache by produceState(emptyMap(), jsExpressions, effectiveContext) {
        if (effectiveContext != null && jsExpressions.isNotEmpty()) {
            // 有上下文：使用带上下文的 executeScript，使 JS 中可访问 this.当前
            val results = mutableMapOf<String, String>()
            for (expr in jsExpressions) {
                try {
                    val result = executeJsExprWithContext(expr, effectiveContext)
                    results[expr] = result.ifBlank { "{{js:$expr}}" }
                } catch (e: Exception) {
                    results[expr] = "{{js:$expr}}"
                }
            }
            value = results
        } else if (jsExpressions.isNotEmpty()) {
            // 无上下文：降级为无参执行（兼容无上下文场景）
            val results = mutableMapOf<String, String>()
            for (expr in jsExpressions) {
                try {
                    val result = executeJsExprNoContext(expr)
                    results[expr] = result.ifBlank { "{{js:$expr}}" }
                } catch (e: Exception) {
                    results[expr] = "{{js:$expr}}"
                }
            }
            value = results
        } else {
            value = emptyMap()
        }
    }

    // 3. 解析所有图标标签（包括原始文本和 JS 返回结果中的）
    val allIconTags = remember(text, jsCache, fontSize) {
        parseIconTags(text, jsCache, fontSize)
    }

    // 4. 动态构建 inlineContent 映射表
    val dynamicInlineContent = allIconTags.associate { tag ->
        tag.id to InlineTextContent(
            Placeholder(
                width = tag.width,
                height = tag.height,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) { _ ->
            AsyncImageFromAssets(
                "images/${tag.path}",
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
        parseRichText(this, text, jsCache, allIconTags, fixedOffset, blurRadius, defaultStyle)
        pop()
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        lineHeight = lineHeight,
        style = LocalTextStyle.current.copy(fontSize = fontSize),
        textAlign = textAlign,
        maxLines = maxLines,
        inlineContent = dynamicInlineContent
    )
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
    defaultStyle: PvzTextStyle
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

                builder.pushLink(LinkAnnotation.Url(url))
                builder.withStyle(SpanStyle(
                    color = targetStyle.color,
                    textDecoration = TextDecoration.Underline,
                    shadow = if (targetStyle.shadowColor != null) Shadow(targetStyle.shadowColor, fixedOffset, blurRadius) else null
                )) {
                    builder.append(displayContent)
                }
                builder.pop()
            } else if (tagName == "icon") {
                // 新格式：{{icon|width=xx|height=xx:path}}
                val fullMatch = "{{$inner}}"
                val iconTag = allIconTags.find { it.fullMatch == fullMatch }
                if (iconTag != null) {
                    builder.appendInlineContent(id = iconTag.id, alternateText = "[${iconTag.path}]")
                } else {
                    builder.append("{{$inner}}")
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
                    parseRichText(builder, result, emptyMap(), allIconTags, fixedOffset, blurRadius, defaultStyle)
                } else {
                    builder.append("{{$inner}}")
                }
            } else if (tagName == "icon") {
                // 在 allIconTags 中查找（包括 JS 返回结果中的图标）
                val iconTag = allIconTags.find { it.fullMatch == "{{$tagName:$displayContent}}" }
                if (iconTag != null) {
                    builder.appendInlineContent(id = iconTag.id, alternateText = "[${iconTag.path}]")
                } else {
                    builder.append("{{$inner}}")
                }
            } else {
                val targetStyle = DefaultPvzTagStyles[tagName] ?: defaultStyle
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
