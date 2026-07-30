package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    forceMaxForm: Boolean = false,
    probeContent: @Composable () -> Unit,
    displayContent: @Composable () -> Unit,
    bottomContent: @Composable (ColumnScope.() -> Unit)
) {
    val density = LocalDensity.current

    // 常量定义
    val maxContentHeightPx = with(density) { heightRange.endInclusive.roundToPx() }      // 最高 250
    val initialMinContentHeightPx = with(density) { height.roundToPx() } // 初始最低 150
    val absoluteMinContentHeightPx = with(density) { heightRange.start.roundToPx() }  // 最终底线 60

    // 探测层高度缓存：maxIntrinsicHeight 是全量测量（整棵内容树），开销最大，
    // 且结果只与 maxWidth 有关，与“可用高度”无关。键盘升降等仅高度变化的场景下
    // maxWidth 不变，可复用缓存避免每帧重测。
    // 仅当已知内容已超出上限(maxContentHeightPx)时才跳过重测——此时精确值无意义
    // （会被钳制、显示层本就滚动），因此复用上限值完全等价；未超出时仍每帧重测，
    // 既保持小内容/内容增删时的正确性，又规避了缓存陈旧导致的布局错乱。
    // 若 forceMaxForm=true，则直接以最高形态（上限高度）展示，连缓存都无需查询、探测层
    // 完全不测量，彻底省去 maxIntrinsicHeight 这一最贵的计算。
    // 注意：使用非 State 的普通对象承载缓存，避免在 measure 阶段写 State 触发重组。
    val probeCache = remember { ProbeHeightCache() }

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
        // —— 但以下情况可跳过重测：
        //   a) forceMaxForm=true：要求以最高形态展示，直接取上限值，无需任何探测计算；
        //   b) 已确认内容远超上限且 maxWidth 未变：精确值无意义（会被钳制、显示层本就滚动），
        //      直接复用上限值，完全等价。
        // 其余情况（未超限）仍每帧重测，开销低且保证内容增删/缩小时的正确性。
        val realContentHeight: Int = when {
            forceMaxForm -> maxContentHeightPx
            probeCache.width == maxWidth && probeCache.height >= maxContentHeightPx -> maxContentHeightPx
            else -> probeMeasurable.maxIntrinsicHeight(maxWidth).also {
                probeCache.width = maxWidth
                probeCache.height = it
            }
        }

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

/**
 * 探测层自然高度的缓存载体。
 * 故意用普通对象（而非 State）承载，避免在 Layout 的 measure 阶段写入 State
 * 而触发重组/循环重测。缓存按 maxWidth 维度复用：当 maxWidth 变化时（如旋转）
 * 自动失效重测，保证正确性。
 */
private class ProbeHeightCache {
    var width: Int = -1
    var height: Int = -1
}