package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme
import kotlinx.coroutines.delay

/**
 * 数量步进的核心逻辑：**纯函数**，抽出来便于在 JVM 单测里覆盖边界（不必上设备）。
 *
 * - 结果恒被钳制在 `[min, max]`；即使外部误传 `min > max` 也会自动纠正顺序；
 * - `delta` 可为负（减）。
 *
 * @see PvzNumberInput
 */
fun stepNumber(value: Int, delta: Int, min: Int, max: Int): Int {
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
    return (value.toLong() + delta.toLong()).coerceIn(lo.toLong(), hi.toLong()).toInt()
}

// ======================== 默认（弹窗）配色：PVZ 绿框黄底 ========================
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
 * 数量输入组件：`[«] [−]  123  [+] [»]`
 *
 * 需求来源（手写需求图）：
 * - 数量输入框要能用 **+ / −** 控制；
 * - **长按 + / − 可连续增减**；
 * - **最小值 / 最大值可自定义**，并提供**一键跳到最小 / 最大**的按钮（`«` / `»`）。
 *
 * ## 两种外观
 * - **栏目条目**：传 [theme]（`section.theme`），配色跟随栏目主题（与同栏目 `PvzInput` 一致：
 *   深色凹槽 + 栏目头渐变按钮），不会在棕色/蓝色栏目里突兀地出现一块绿色。
 * - **JS 弹窗 / 独立使用**：不传 [theme]，使用 PVZ 默认的「绿框黄底」。
 *
 * ## 实现要点
 * - **不修改 `PvzInput.kt`**（该文件有「一律不改」的硬约束），本组件为独立实现。
 * - 🔴 **组件内部维护一份「最新值」**（`internalValue`），长按循环每次都基于它计算：
 *   若直接用外部传入的 `value` 闭包，长按期间捕获的始终是**按下那一刻的旧值**，
 *   结果是「按住只会加一次、不持续变化」。
 * - 值变化时一次性回写外部状态，不产生中间态 → 不会每次步进都重建整个栏目。
 * - 长按：按住 400ms 后以 80ms 间隔连续步进，松手立即停止。
 *
 * @param value 当前值（外部受控）
 * @param onValueChange 值变化回调，传入值已按 [min]/[max] 钳制
 * @param min 最小值（默认不限制）
 * @param max 最大值（默认不限制）
 * @param step 每次步进量（≤0 时按 1 处理）
 * @param suffix 单位后缀，如 `"个"`
 * @param showMinMaxButtons 是否显示「跳到最小值 `«` / 跳到最大值 `»`」按钮
 * @param theme 栏目主题；为 null 时使用 PVZ 默认绿框黄底
 */
@Composable
fun PvzNumberInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    step: Int = 1,
    suffix: String? = null,
    enabled: Boolean = true,
    showMinMaxButtons: Boolean = true,
    theme: PvzCollapsiblePanelTheme? = null,
) {
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
    val safeStep = if (step <= 0) 1 else step
    val outerShape = RoundedCornerShape(12.dp)
    val innerShape = RoundedCornerShape(9.dp)

    // —— 按主题派生配色：有 theme 走栏目主题（与同栏 PvzInput 观感一致），无 theme 走 PVZ 默认 ——
    val containerColor = theme?.sliderInactiveColor ?: PvzFill
    val outerBorderColor = theme?.headerGradientStart ?: PvzFrameDark
    val innerBorderColor = theme?.sliderActiveColor ?: PvzFrameLight
    val valueBoxColor = theme?.sliderInactiveColor?.copy(alpha = 0.55f) ?: PvzFillSunken
    val valueTextColor = if (theme != null) Color.White else PvzInk
    val disabledText = if (theme != null) Color.White.copy(alpha = 0.38f) else PvzInkDisabled
    val stepBrush = if (theme != null) {
        Brush.verticalGradient(listOf(theme.headerGradientStart, theme.headerGradientEnd))
    } else {
        Brush.verticalGradient(listOf(PvzButtonTop, PvzButtonBottom))
    }
    val stepDisabledBrush = if (theme != null) {
        Brush.verticalGradient(listOf(containerColor.copy(alpha = 0.8f), containerColor.copy(alpha = 0.8f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCFCBA0), Color(0xFFC2BE94)))
    }
    val stepBorderColor = theme?.headerGradientEnd ?: PvzFrameDark
    val stepBorderDisabled = if (theme != null) theme.sliderActiveColor.copy(alpha = 0.35f) else Color(0xFF8E8B68)
    val jumpTextColor = theme?.sliderActiveColor ?: PvzInk
    val jumpBgColor = if (theme != null) theme.sliderActiveColor.copy(alpha = 0.18f) else PvzFillSunken
    val jumpBorderColor = theme?.sliderActiveColor ?: PvzFrameLight

    // 🔴 内部持有的「最新值」：长按连续步进必须基于它，而不是外部 value 的旧闭包，
    // 否则长按期间每次计算都用按下瞬间的值 → 表现为「按住毫无反应 / 只跳一次」。
    var internalValue by remember { mutableStateOf(value.coerceIn(lo, hi)) }
    // 外部值变化（如 JS 写入、重置）时同步回来
    LaunchedEffect(value) { internalValue = value.coerceIn(lo, hi) }

    fun commit(next: Int) {
        val clamped = next.coerceIn(lo, hi)
        if (clamped != internalValue) {
            internalValue = clamped
            onValueChange(clamped)
        }
    }

    fun stepBy(delta: Int) = commit(stepNumber(internalValue, delta, lo, hi))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(outerShape)
            .background(containerColor)
            // 双层描边：外框用栏目头色 / 内框用强调色，层次与栏目一致
            .border(2.dp, outerBorderColor, outerShape)
            .padding(3.dp)
            .border(1.dp, innerBorderColor, innerShape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMinMaxButtons) {
            // « 一键跳到最小值
            JumpButton(
                symbol = "«",
                enabled = enabled && internalValue > lo,
                textColor = jumpTextColor,
                backgroundColor = jumpBgColor,
                borderColor = jumpBorderColor,
                disabledTextColor = disabledText,
                onClick = { commit(lo) }
            )
        }

        StepButton(
            symbol = "−",
            enabled = enabled && internalValue > lo,
            enabledBrush = stepBrush,
            disabledBrush = stepDisabledBrush,
            borderColor = stepBorderColor,
            disabledBorderColor = stepBorderDisabled,
            disabledTextColor = disabledText,
            onStep = { stepBy(-safeStep) }
        )

        // 数值凹槽
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
                .clip(innerShape)
                .background(valueBoxColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (suffix.isNullOrBlank()) internalValue.toString() else "$internalValue$suffix",
                color = valueTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        StepButton(
            symbol = "+",
            enabled = enabled && internalValue < hi,
            enabledBrush = stepBrush,
            disabledBrush = stepDisabledBrush,
            borderColor = stepBorderColor,
            disabledBorderColor = stepBorderDisabled,
            disabledTextColor = disabledText,
            onStep = { stepBy(safeStep) }
        )

        if (showMinMaxButtons) {
            // » 一键跳到最大值
            JumpButton(
                symbol = "»",
                enabled = enabled && internalValue < hi,
                textColor = jumpTextColor,
                backgroundColor = jumpBgColor,
                borderColor = jumpBorderColor,
                disabledTextColor = disabledText,
                onClick = { commit(hi) }
            )
        }
    }
}

/**
 * ± 步进按钮：填充色由调用方按主题给出（栏目主题 → 栏目头渐变；默认 → PVZ 绿渐变）。
 * 单击步进一次；按住 400ms 后每 80ms 连续步进，松手即停。
 */
@Composable
private fun StepButton(
    symbol: String,
    enabled: Boolean,
    enabledBrush: Brush,
    disabledBrush: Brush,
    borderColor: Color,
    disabledBorderColor: Color,
    disabledTextColor: Color,
    onStep: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 长按连续步进：effect 在松手（pressed=false）时自动取消
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
        modifier = Modifier
            .fillMaxHeight()
            .width(50.dp)
            .clip(shape)
            .background(if (enabled) enabledBrush else disabledBrush)
            // 按下时压暗，给出明确的点击反馈
            .background(if (pressed && enabled) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
            .border(2.dp, if (enabled) borderColor else disabledBorderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) { onStep() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (enabled) Color.White else disabledTextColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 「跳到最小值 / 最大值」按钮（`«` / `»`）：轻量按钮，点击一次直达边界。
 */
@Composable
private fun JumpButton(
    symbol: String,
    enabled: Boolean,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    disabledTextColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(34.dp)
            .clip(shape)
            .background(if (enabled) backgroundColor else Color.Transparent)
            .border(1.5.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (enabled) textColor else disabledTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
