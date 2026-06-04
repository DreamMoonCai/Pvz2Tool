package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PvzCardColors(
    val outerBackgroundTop: Color,
    val outerBackgroundBottom: Color,
    val border1Color: Color, // 最外层深色边框
    val border2Color: Color, // 中间主色边框
    val border3Color: Color, // 最内层细线边框
    val headerBackgroundTop: Color,
    val headerBackgroundBottom: Color,
    val headerTextStyle: PvzTextStyle,
)

@Composable
fun PvzCard(
    title: String?,
    colors: PvzCardColors, // 默认使用绿色
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier.padding(10.dp, 15.dp),
    fontSize: TextUnit = 18.sp,
    headerLeadingContent: @Composable BoxScope.() -> Unit = {},
    headerTrailingContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.outerBackgroundTop, colors.outerBackgroundBottom
                        )
                    ), RoundedCornerShape(15.dp)
                )
                .border(
                    BorderStroke(3.dp, colors.border1Color), shape = RoundedCornerShape(15.dp)
                )
                .padding(2.dp)
                .border(
                    BorderStroke(5.dp, colors.border2Color), shape = RoundedCornerShape(15.dp)
                )
                .padding(0.5.dp)
                .border(
                    BorderStroke(1.dp, colors.border3Color), shape = RoundedCornerShape(15.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题区域
            if (title != null) {
                Box( // 【核心】改用 Box
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.headerBackgroundTop,
                                    colors.headerBackgroundBottom
                                )
                            )
                        )
                        .padding(top = 2.5.dp, start = 2.5.dp, end = 2.5.dp)
                        .border(
                            BorderStroke(2.5.dp, colors.border3Color),
                            shape = RoundedCornerShape(2.5.dp)
                        )
                        .padding(vertical = 15.dp), // 上下 padding 放在这里
                    contentAlignment = Alignment.Center // 【核心】Box 默认居中
                ) {
                    // 1. 左侧按钮 (不影响布局，只是覆盖在左上角)
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart),
                        content = headerLeadingContent
                    )

                    // 2. 中间标题 (永远在正中心)
                    PvzRichText(
                        title,
                        defaultStyle = PvzTextStyle(colors.headerTextStyle.color, colors.headerTextStyle.shadowColor),
                        fontSize = fontSize,
                        lineHeight = TextUnit.Unspecified
                    )

                    // 3. 右侧按钮 (不影响布局，只是覆盖在右上角)
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        content = headerTrailingContent
                    )
                }
            }

            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(contentModifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
fun PvzCardGreen(
    title: String?,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier.padding(10.dp, 15.dp),
    fontSize: TextUnit = 18.sp,
    headerLeadingContent: @Composable BoxScope.() -> Unit = {},
    headerTrailingContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) = PvzCard(title, PvzCardColors(
    outerBackgroundTop = Color(0xFFF3EEB9),
    outerBackgroundBottom = Color(0xFFF2EDBB),
    border1Color = Color(0xFF344702),
    border2Color = Color(0xFF8ED229),
    border3Color = Color(0xFF78A52B),
    headerBackgroundTop = Color(0xFF88CD23),
    headerBackgroundBottom = Color(0xFF97DC02),
    headerTextStyle = PvzTextWhiteStyle
),modifier,  contentModifier,fontSize,headerLeadingContent,headerTrailingContent,content)

@Composable
fun PvzCardNewYear(
    title: String?,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier.padding(10.dp, 15.dp),
    fontSize: TextUnit = 18.sp,
    headerLeadingContent: @Composable BoxScope.() -> Unit = {},
    headerTrailingContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) = PvzCard(title, PvzCardColors(
    outerBackgroundTop = Color(0xFFF3EEB9),
    outerBackgroundBottom = Color(0xFFF2EDBB),
    border1Color = Color(0xFFB71C1C),
    border2Color = Color(0xFFFFD700),
    border3Color = Color(0xFFFFAB00),
    headerBackgroundTop = Color(0xFFEF5350),
    headerBackgroundBottom = Color(0xFFE53935),
    headerTextStyle = PvzTextGoldStyle
),modifier,  contentModifier,fontSize,headerLeadingContent,headerTrailingContent,content)

/**
 * 简单卡片基础组件
 * @param borderColor 边框颜色
 * @param backgroundColor 背景色
 */
@Composable
fun PvzSimpleCard(
    borderColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    // 计算渐变顶部颜色（更亮）
    val backgroundTopColor = Color(
        red = (backgroundColor.red * 1.2f).coerceIn(0f, 1f),
        green = (backgroundColor.green * 1.2f).coerceIn(0f, 1f),
        blue = (backgroundColor.blue * 1.2f).coerceIn(0f, 1f)
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(backgroundTopColor, backgroundColor)
                    ),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(2.dp)
                .border(
                    BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp)
                ),
            content = content
        )
    }
}

/**
 * 棕色卡片
 */
@Composable
fun PvzSimpleCardBrown(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF8B5A2B),
    backgroundColor: Color = Color(0xFF4A3010),
    content: @Composable BoxScope.() -> Unit = {}
) = PvzSimpleCard(borderColor, backgroundColor, modifier, content)

/**
 * 绿色卡片
 */
@Composable
fun PvzSimpleCardGreen(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF8ED229),
    backgroundColor: Color = Color(0xFF4A6A00),
    content: @Composable BoxScope.() -> Unit = {}
) = PvzSimpleCard(borderColor, backgroundColor, modifier, content)