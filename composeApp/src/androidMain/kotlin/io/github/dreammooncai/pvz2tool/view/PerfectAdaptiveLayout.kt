package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==============================================
// 【完美版 Layout】双组件策略：探测+显示
// ==============================================
@Composable
fun PerfectAdaptiveLayout(
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    heightRange: ClosedRange<Dp> = 60.dp..250.dp,
    probeContent: @Composable () -> Unit,
    displayContent: @Composable () -> Unit,
    bottomContent: @Composable (ColumnScope.() -> Unit)
) {
    val density = LocalDensity.current

    // 常量定义
    val maxContentHeightPx = with(density) { heightRange.endInclusive.roundToPx() }      // 最高 250
    val initialMinContentHeightPx = with(density) { height.roundToPx() } // 初始最低 150
    val absoluteMinContentHeightPx = with(density) { heightRange.start.roundToPx() }  // 最终底线 60

    Layout(
        modifier = modifier,
        content = {
            // 1. 探测组件：alpha=0 不可见，只用来查 intrinsicHeight
            Box(modifier = Modifier.alpha(0f), contentAlignment = Alignment.TopCenter) {
                probeContent()
            }
            // 2. 显示组件：实际显示的内容
            displayContent()
            // 3. 底部组件
            Column(content = bottomContent)
        }
    ) { measurables, constraints ->
        check(measurables.size == 3) { "Need exactly three children: probe, display, bottom" }
        val probeMeasurable = measurables[0]
        val displayMeasurable = measurables[1]
        val bottomMeasurable = measurables[2]

        val maxWidth = constraints.maxWidth
        val parentMaxHeight = constraints.maxHeight

        // 1. 先测量 Bottom
        val bottomPlaceable = bottomMeasurable.measure(constraints.copy(minHeight = 0))
        val bottomHeight = bottomPlaceable.height

        // 2. 【核心】查询探测组件的真实自然高度
        // 探测组件不带 fillMaxHeight，所以它的 intrinsicHeight 是准确的
        val realContentHeight = probeMeasurable.maxIntrinsicHeight(maxWidth)

        // 3. 计算 Content 的理想高度（不考虑挤压）
        // 规则：内容少 -> 150；内容中等 -> 包裹；内容多 -> 250
        val idealContentHeight = realContentHeight.coerceIn(initialMinContentHeightPx, maxContentHeightPx)
        val totalIdealHeight = idealContentHeight + bottomHeight

        // 4. 决策最终高度
        val finalContentHeight: Int
        val finalLayoutHeight: Int

        if (parentMaxHeight == Constraints.Infinity || totalIdealHeight <= parentMaxHeight) {
            // 【情况 A】空间充足，或者还没触达 400dp 上限
            finalContentHeight = idealContentHeight
            finalLayoutHeight = totalIdealHeight
        } else {
            // 【情况 B】触达上限了，开始挤压！
            val remainingSpace = (parentMaxHeight - bottomHeight).coerceAtLeast(0)

            finalContentHeight = when {
                remainingSpace >= initialMinContentHeightPx -> remainingSpace
                remainingSpace > absoluteMinContentHeightPx -> remainingSpace
                else -> absoluteMinContentHeightPx
            }

            finalLayoutHeight = parentMaxHeight
        }

        // 5. 测量显示组件（唯一一次测量显示组件）
        val displayPlaceable = displayMeasurable.measure(
            Constraints.fixed(maxWidth, finalContentHeight)
        )

        // 6. 放置（探测组件不需要放置，因为 alpha=0 且我们没测它）
        layout(maxWidth, finalLayoutHeight) {
            displayPlaceable.placeRelative(0, 0)
            bottomPlaceable.placeRelative(0, finalLayoutHeight - bottomHeight)
        }
    }
}