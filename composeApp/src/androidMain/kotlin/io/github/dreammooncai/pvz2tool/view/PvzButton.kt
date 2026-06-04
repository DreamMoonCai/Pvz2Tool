package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import kotlinx.coroutines.flow.any

@Composable
fun PvzButton(
    text: String,
    icon: ImageVector?,
    normalGradient: Brush,
    pressedGradient: Brush,
    modifier: Modifier = Modifier,
    highlightColor: Color, // 新增：允许自定义光团颜色
    onClick: () -> Unit
) {
    val interactionSource = rememberSoundInteractionSource(
        InitializePvz2.config.ui.sounds.buttonClickPress,
        InitializePvz2.config.ui.sounds.buttonClickRelease
    )
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentBrush = if (isPressed) pressedGradient else normalGradient

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(brush = currentBrush)
            .padding(2.dp)
            .border(
                BorderStroke(3.dp, Color(0xFFFDFDFD)), shape = RoundedCornerShape(6.dp)
            )
            .padding(0.3.dp)
            .border(
                BorderStroke(0.5.dp, Color(0xFF78A52B)), shape = RoundedCornerShape(6.dp)
            )
            .drawWithContent {
                drawContent()
                if (!isPressed) {
                    drawTopLeftHighlight(highlightColor) // 传入光团颜色
                }
            }
            .clickable(
                interactionSource = interactionSource, indication = ripple(), role = Role.Button, onClick = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon, contentDescription = text, modifier = Modifier.size(24.dp), tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            PvzRichText(text, fontSize = 16.sp,defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null))
        }
    }
}

// 紫色版本（保留，并显式传入紫色光团）
@Composable
fun PvzPurpleButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = PvzButton(
    text = text,
    icon = icon,
    normalGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        colors = listOf(Color(0xFF683EBB), Color(0xFF683EBD))
    ),
    pressedGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        colors = listOf(Color(0xFF7040C2), Color(0xFFD77FFB))
    ),
    highlightColor = Color(0xFFF1AEFB), // 紫色光团
    modifier = modifier,
    onClick = onClick
)

// 橙色版本（调整按下渐变顺序：上暗下亮）
@Composable
fun PvzOrangeButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = PvzButton(
    text = text,
    icon = icon,
    normalGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 正常状态：保持平稳，两个颜色相近
        colors = listOf(Color(0xFFDF901A), Color(0xFFA1510D))
    ),
    pressedGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 按下状态：上暗下亮（参考紫色逻辑）
        colors = listOf(Color(0xFFA1510D), Color(0xFFDF901A))
    ),
    highlightColor = Color(0xFFFFF8E1),
    modifier = modifier,
    onClick = onClick
)

// 红色版本（调整按下渐变顺序：上暗下亮）
@Composable
fun PvzRedButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = PvzButton(
    text = text,
    icon = icon,
    normalGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 正常状态：平稳
        colors = listOf(Color(0xFFC62828), Color(0xFFB71C1C))
    ),
    pressedGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 按下状态：上暗下亮
        colors = listOf(Color(0xFFB71C1C), Color(0xFFEF5350))
    ),
    highlightColor = Color(0xFFFFEBEE),
    modifier = modifier,
    onClick = onClick
)

// 绿色版本（新增）
@Composable
fun PvzGreenButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = PvzButton(
    text = text,
    icon = icon,
    normalGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 正常状态：经典 PVZ 绿
        colors = listOf(Color(0xFF689F38), Color(0xFF558B2F))
    ),
    pressedGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 按下状态：上暗下亮，更有活力的绿
        colors = listOf(Color(0xFF558B2F), Color(0xFF9CCC65))
    ),
    highlightColor = Color(0xFFF1F8E9), // 淡绿光团
    modifier = modifier,
    onClick = onClick
)

@Composable
fun PvzBlueButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) = PvzButton(
    text = text,
    icon = icon,
    normalGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 正常状态：平稳的PVZ风格蓝色，两个颜色相近
        colors = listOf(Color(0xFF1976D2), Color(0xFF1565C0))
    ),
    pressedGradient = Brush.linearGradient(
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
        // 按下状态：上暗下亮，符合统一的交互逻辑
        colors = listOf(Color(0xFF1565C0), Color(0xFF64B5F6))
    ),
    highlightColor = Color(0xFFE3F2FD), // 淡蓝光团，匹配整体视觉风格
    modifier = modifier,
    onClick = onClick
)

// 升级 drawTopLeftHighlight，支持传入颜色参数
private fun DrawScope.drawTopLeftHighlight(highlightColor: Color) {
    val ovalRect = Rect(11.1f, 14.3f, 50.4f, 44.5f)

    drawOval(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFFFFFFF), // 核心永远是纯白最亮
                0.4f to highlightColor.copy(alpha = 0.7f), // 使用传入的颜色
                0.7f to highlightColor.copy(alpha = 0.5f), // 使用传入的颜色
                1.0f to Color.Transparent,
            ),
            center = ovalRect.center,
            radius = 39.3f / 2f
        ),
        topLeft = ovalRect.topLeft,
        size = Size(39.3f, 30.2f)
    )
}