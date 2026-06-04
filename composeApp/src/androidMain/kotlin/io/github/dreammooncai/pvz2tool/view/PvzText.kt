package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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

// 1. 简化文字样式配置：移除固定的 blur 和 offset，只保留核心颜色
data class PvzTextStyle(
    val color: Color,
    val shadowColor: Color? = null
)

// 辅助函数：根据字体大小动态计算模糊半径 (16sp -> 5f 为基准)
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
                blurRadius = calculateBlurRadius(fontSize),      // 动态计算
                offset = Offset(2f, 2f)       // 固定偏移
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
) {
    val blurRadius = calculateBlurRadius(fontSize)
    val fixedOffset = Offset(2f, 2f)

    // 1. 预解析：使用正则提取文本中所有的图标路径
    // 使用 remember 避免每次重组都去跑正则匹配
    val iconPaths = remember(text) {
        val regex = "\\{\\{icon:([^}]+)\\}\\}".toRegex()
        regex.findAll(text).map { it.groupValues[1] }.toSet()
    }

    // 2. 动态构建 inlineContent 映射表
    val dynamicInlineContent = iconPaths.associateWith { path ->
        InlineTextContent(
            Placeholder(
                width = fontSize * 1.2f,  // 图标稍微比文字大一点，看起来更协调
                height = fontSize * 1.2f, placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) { _ -> // 这里的参数是 alternateText，我们不需要使用它
            AsyncImageFromAssets(
                "images/$path",
                contentDescription = path,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 3. 构建富文本 (这里的逻辑与上一版保持一致)
    val annotatedString = buildAnnotatedString {
        val defaultSpanStyle = SpanStyle(
            color = defaultStyle.color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            shadow = if (defaultStyle.shadowColor != null) Shadow(defaultStyle.shadowColor, fixedOffset,blurRadius = blurRadius) else null
        )

        pushStyle(defaultSpanStyle)

        var currentIndex = 0
        while (currentIndex < text.length) {
            val start = text.indexOf("{{", currentIndex)
            if (start == -1) {
                append(text.substring(currentIndex))
                break
            }
            append(text.substring(currentIndex, start))

            val end = text.indexOf("}}", start + 2)
            if (end == -1) {
                append(text.substring(start))
                break
            }

            // 【核心改进】先提取 {{ }} 内部的所有内容
            val inner = text.substring(start + 2, end)

            // 解析结构：{{ 标签名 | 参数 : 显示内容 }}
            // 如果带 '|'，说明是 link 这种带参数的
            if (inner.contains("|")) {
                val tagName = inner.substringBefore("|").trim()
                val remainder = inner.substringAfter("|")
                // URL 和文本的分割点应该是最后一个冒号，避免被 https:// 干扰
                val url = remainder.substringBeforeLast(":").trim()
                val displayContent = remainder.substringAfterLast(":")

                if (tagName.startsWith("link")) {
                    val styleSuffix = tagName.removePrefix("link-")
                    val targetStyle = DefaultPvzTagStyles[styleSuffix] ?: PvzTextStyle(Color(0xFF64B5F6), Color.Black)

                    pushLink(LinkAnnotation.Url(url))
                    withStyle(SpanStyle(
                        color = targetStyle.color,
                        textDecoration = TextDecoration.Underline,
                        shadow = if (targetStyle.shadowColor != null) Shadow(targetStyle.shadowColor, fixedOffset, blurRadius) else null
                    )) {
                        append(displayContent)
                    }
                    pop()
                }
            } else if (inner.contains(":")) {
                // 普通标签：{{ 标签名 : 内容 }}
                val tagName = inner.substringBefore(":").trim()
                val displayContent = inner.substringAfter(":")

                if (tagName == "icon") {
                    appendInlineContent(id = displayContent, alternateText = "[$displayContent]")
                } else {
                    val targetStyle = DefaultPvzTagStyles[tagName] ?: defaultStyle
                    withStyle(SpanStyle(
                        color = targetStyle.color,
                        shadow = if (targetStyle.shadowColor != null) Shadow(targetStyle.shadowColor, fixedOffset, blurRadius) else null
                    )) {
                        append(displayContent)
                    }
                }
            } else {
                // 格式不合法的直接原样显示
                append("{{$inner}}")
            }

            currentIndex = end + 2
        }
        pop()
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        lineHeight = lineHeight,
        style = LocalTextStyle.current.copy(fontSize = fontSize),
        textAlign = textAlign,
        maxLines = maxLines,
        inlineContent = dynamicInlineContent // 传入我们动态生成的图片映射
    )
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
            // 处理带参数的 link
            inner.contains("|") -> {
                val displayContent = inner.substringAfterLast(":")
                sb.append(displayContent)
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