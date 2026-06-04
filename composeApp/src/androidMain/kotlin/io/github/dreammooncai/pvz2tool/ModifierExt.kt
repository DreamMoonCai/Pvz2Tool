package io.github.dreammooncai.pvz2tool

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dreammooncai.pvz2tool.controller.SoundController

/**
 * 绘制“顶部圆角 + 仅上/左/右三边边框”的修饰符
 *
 * @param width 边框宽度
 * @param color 边框颜色
 * @param topCornerRadius 顶部圆角大小
 */
fun Modifier.topRoundedBorder(
    width: Dp,
    color: Color,
    topCornerRadius: Dp
): Modifier = this.then(
    Modifier.drawWithContent {
        // 先绘制原本的内容
        drawContent()

        val strokeWidth = width.toPx()
        val cornerRadiusPx = topCornerRadius.toPx()

        // 手动绘制 Path：只画上半部分边框
        drawPath(
            path = Path().apply {
                // 1. 移动到左下角
                moveTo(0f, size.height)
                // 2. 画直线到左上角圆弧起点
                lineTo(0f, cornerRadiusPx)
                // 3. 画左上角圆弧
                quadraticTo(0f, 0f, cornerRadiusPx, 0f) // 4. 画直线到右上角圆弧起点
                lineTo(size.width - cornerRadiusPx, 0f)
                // 5. 画右上角圆弧
                quadraticTo(size.width, 0f, size.width, cornerRadiusPx) // 6. 画直线到右下角
                lineTo(size.width, size.height)
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
)
/**
 * 绘制“顶部圆角 + 仅下/左/右三边边框”的修饰符
 *
 * @param width 边框宽度
 * @param color 边框颜色
 * @param bottomCornerRadius 顶部圆角大小
 */
fun Modifier.bottomRoundedBorder(
    width: Dp,
    color: Color,
    bottomCornerRadius: Dp
): Modifier = this.then(
    Modifier.drawWithContent {
        // 1. 先绘制原本的内容
        drawContent()

        val strokeWidth = width.toPx()
        val cornerRadiusPx = bottomCornerRadius.toPx()

        // 2. 手动绘制 Path
        drawPath(
            path = Path().apply {
                // 起点：左上角
                moveTo(0f, 0f)

                // 直线向下：左侧边 -> 左下角圆弧起点
                lineTo(0f, size.height - cornerRadiusPx)

                // 左下角圆角：贝塞尔曲线
                // 控制点 (0, height), 终点 (cornerRadius, height)
                quadraticTo(
                    x1 = 0f,
                    y1 = size.height,
                    x2 = cornerRadiusPx,
                    y2 = size.height
                )

                // 直线向右：底部边 -> 右下角圆弧起点
                lineTo(size.width - cornerRadiusPx, size.height)

                // 右下角圆角：贝塞尔曲线
                // 控制点 (width, height), 终点 (width, height - cornerRadius)
                quadraticTo(
                    x1 = size.width,
                    y1 = size.height,
                    x2 = size.width,
                    y2 = size.height - cornerRadiusPx
                )

                // 直线向上：右侧边 -> 右上角
                lineTo(size.width, 0f)

                // 注意：这里不调用 close()，保持顶部开口
            },
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
)

@Composable
fun rememberSoundInteractionSource(pressSound: String? = null, releaseSound: String? = null): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { type ->
            when (type) {
                is PressInteraction.Release,is PressInteraction.Cancel -> releaseSound?.let {
                    SoundController.playSoundFromAssets(releaseSound)
                }
                is PressInteraction.Press -> pressSound?.let { pressSound ->
                    SoundController.playSoundFromAssets(pressSound)
                }
            }
        }
    }
    return interactionSource
}