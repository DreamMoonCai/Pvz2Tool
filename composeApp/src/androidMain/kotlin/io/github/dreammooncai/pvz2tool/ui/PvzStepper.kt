package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 小数步进的**纯逻辑**（滑块用；数量输入走整数版 [stepNumber]）。
 *
 * 为什么单独抽出来：
 * - 滑块是 `Double`，直接 `+0.1` 累积会出现 `0.30000000000000004` 这类浮点尾巴，
 *   界面显示与实际值都会难看，必须按 [decimals] 四舍五入；
 * - 用 `step * n` 而不是反复累加，可避免长按连续步进时误差放大。
 *
 * @param value 当前值
 * @param delta 增量（可为负），单位是「步数 × step」的最终位移
 * @param min 允许最小值（若传入反向区间会自动纠正）
 * @param max 允许最大值
 * @param decimals 保留小数位（0~6）
 */
fun stepSlider(
    value: Double,
    delta: Double,
    min: Double,
    max: Double,
    decimals: Int = 2
): Double {
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
    val clamped = (value + delta).coerceIn(lo, hi)
    return roundToDecimals(clamped, decimals)
}

/**
 * 按 [decimals] 位小数四舍五入（0 位时返回整数 Double）。
 *
 * ⚠️ 取整用 Kotlin 的 `roundToLong()`（等价 `Math.round`），语义是「.5 向正无穷」：
 * `2.5 → 3`、`-2.5 → -2`。滑块取值范围通常非负，这个差别不影响；负数场景需按此预期。
 */
fun roundToDecimals(value: Double, decimals: Int): Double {
    val digits = decimals.coerceIn(0, 6)
    if (digits == 0) return value.roundToLong().toDouble()
    val factor = 10.0.pow(digits)
    return (value * factor).roundToLong() / factor
}

/** 滑块/数量的配色集合：按栏目主题派生，`theme == null` 时用 PVZ 默认绿框黄底。 */
data class PvzStepColors(
    val container: Color,
    val outerBorder: Color,
    val innerBorder: Color,
    val valueBox: Color,
    val valueText: Color,
    val disabledText: Color,
    val stepBrush: Brush,
    val stepDisabledBrush: Brush,
    val stepBorder: Color,
    val stepBorderDisabled: Color,
)

/** 外框深绿 */
private val PvzFrameDark = Color(0xFF344702)

/** 内框亮绿 */
private val PvzFrameLight = Color(0xFF8ED229)

/** 卡片浅黄底 */
private val PvzFill = Color(0xFFF3EEB9)

/** 数值区凹槽 */
private val PvzFillSunken = Color(0xFFE7E1A4)

/** 深橄榄绿文字 */
private val PvzInk = Color(0xFF33420A)

/** 禁用态 */
private val PvzInkDisabled = Color(0xFF9AA37B)

/** ± 按钮绿渐变 */
private val PvzButtonTop = Color(0xFF9BE00B)
private val PvzButtonBottom = Color(0xFF7CBE1C)

/**
 * 按主题派生 [PvzStepColors]：栏目内传 `section.theme`（深色凹槽 + 栏目头渐变按钮），
 * 弹窗内传 `null`（PVZ 绿框黄底）。
 */
fun pvzStepColors(theme: PvzCollapsiblePanelTheme?): PvzStepColors = if (theme == null) {
    PvzStepColors(
        container = PvzFill,
        outerBorder = PvzFrameDark,
        innerBorder = PvzFrameLight,
        valueBox = PvzFillSunken,
        valueText = PvzInk,
        disabledText = PvzInkDisabled,
        stepBrush = Brush.verticalGradient(listOf(PvzButtonTop, PvzButtonBottom)),
        stepDisabledBrush = Brush.verticalGradient(listOf(Color(0xFFCFCBA0), Color(0xFFC2BE94))),
        stepBorder = PvzFrameDark,
        stepBorderDisabled = Color(0xFF8E8B68),
    )
} else {
    PvzStepColors(
        container = theme.sliderInactiveColor,
        outerBorder = theme.headerGradientStart,
        innerBorder = theme.sliderActiveColor,
        valueBox = theme.sliderInactiveColor.copy(alpha = 0.55f),
        valueText = Color.White,
        disabledText = Color.White.copy(alpha = 0.38f),
        stepBrush = Brush.verticalGradient(listOf(theme.headerGradientStart, theme.headerGradientEnd)),
        stepDisabledBrush = Brush.verticalGradient(
            listOf(
                theme.sliderInactiveColor.copy(alpha = 0.8f),
                theme.sliderInactiveColor.copy(alpha = 0.8f)
            )
        ),
        stepBorder = theme.headerGradientEnd,
        stepBorderDisabled = theme.sliderActiveColor.copy(alpha = 0.35f),
    )
}

/**
 * ± 步进按钮：单击步进一次；**按住 400ms 后每 80ms 连续步进**，松手即停。
 *
 * 🔴 长按循环依赖 `collectIsPressedAsState` 的按压态：
 * `LaunchedEffect(pressed)` 在松手时自动取消，因此**不需要**手动清理任务，
 * 也不会出现「松手还在跑」的泄漏。
 *
 * @param width 按钮宽度（滑块旁用窄一些，数量条用宽一些）
 */
@Composable
fun PvzStepButton(
    symbol: String,
    enabled: Boolean,
    colors: PvzStepColors,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 50.dp,
    fontSize: Int = 22,
    /** 固定高度；为 null 时撑满父容器（数量条里用作 fillMaxHeight） */
    height: Dp? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        delay(400)
        while (true) {
            onStep()
            delay(80)
        }
    }

    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
            .width(width)
            .clip(shape)
            .background(if (enabled) colors.stepBrush else colors.stepDisabledBrush)
            // 按下时压暗，给出明确的点击反馈
            .background(if (pressed && enabled) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
            .border(2.dp, if (enabled) colors.stepBorder else colors.stepBorderDisabled, shape)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onStep() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (enabled) Color.White else colors.disabledText,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 「跳到最小值 / 最大值」按钮。
 *
 * 视觉上与 ± 步进按钮**完全同族**（同样的绿色渐变填充 + 2dp 深色描边 + 白色粗体字），只是更窄、字号更小。
 *
 * 符号选型：**用文字 `Min` / `Max`**（用户选定的表现形式）。
 * - `«` / `»` 法文引号字形不带「到头」语义、在厚描边控件里也不协调；
 * - 实心双三角（`◀◀` / `▶▶`）又容易读成「快退 / 快进」；
 * - `Min` / `Max` 直接点明「跳到下/上界」，且与 `−` `+` 的符号形态区分明显，不需要额外图标资源。
 *
 * @param label 按钮文字（`Min` / `Max`）
 */
@Composable
fun PvzJumpButton(
    label: String,
    enabled: Boolean,
    colors: PvzStepColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
    /** 固定高度；为 null 时撑满父容器 */
    height: Dp? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
            .width(width)
            .clip(shape)
            .background(if (enabled) colors.stepBrush else colors.stepDisabledBrush)
            .background(if (pressed && enabled) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
            .border(2.dp, if (enabled) colors.stepBorder else colors.stepBorderDisabled, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else colors.disabledText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
