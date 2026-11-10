package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * 可**长按拖动排序**的条目容器，同时支持列表（[columns] = 1）与网格（[columns] > 1）。
 *
 * ## 交互
 * - 长按某格 → 抬起即可拖动；拖到别的格子上方 → 两者**换位**（实时预览）；
 * - 松手 → 通过 [onOrderChange] 回调把新顺序交给调用方持久化。
 *
 * ## 实现要点（都是踩过的坑）
 * 1. **用 `boundsInParent` 命中的不是手指位置，而是被拖格子的当前中心**。
 *    手指长按后往往略偏格子中心，用指针位置命中会在换位后产生抖动/来回跳。
 * 2. **换位后要补偿拖拽偏移**（`dragOffset -= 目标格 - 原格` 的左上角差）：
 *    否则格子布局被移到新位置、视觉上却还叠加着旧偏移 → 拖影「飞走」。
 * 3. **拖动结束后才回调**，拖动过程中只改本地顺序：避免每帧写持久化状态导致整棵栏目重组、卡顿。
 * 4. [enabled] 为 false（如搜索过滤中）时完全不接管手势，避免「过滤后的子集被拖乱」。
 *
 * @param ids 当前要展示的条目 id（顺序 = 初始顺序）
 * @param columns 每行列数；1 = 列表
 * @param onOrderChange 拖动结束后回调新顺序（仅本次展示的 id 子集）
 * @param content 单格内容；`dragging` 为 true 时表示该格正在被拖动
 */
@Composable
fun ReorderableItemGrid(
    ids: List<String>,
    columns: Int,
    onOrderChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemSpacing: Dp = 8.dp,
    content: @Composable (id: String, dragging: Boolean) -> Unit,
) {
    // 本地顺序：拖动过程只改它，松手才回调（见实现要点 3）
    var order by remember(ids) { mutableStateOf(ids) }
    // 外部顺序变化（清空搜索、脚本改顺序）时跟随
    LaunchedEffect(ids) { if (order.toSet() != ids.toSet()) order = ids }

    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val cols = columns.coerceAtLeast(1)
    val rows = order.chunked(cols)

    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { rowIndex, rowIds ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(itemSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                rowIds.forEach { id ->
                    CellScope(
                        id = id,
                        dragging = draggingId == id,
                        enabled = enabled,
                        onBoundsChanged = { rect -> bounds[id] = rect },
                        dragOffsetProvider = { if (draggingId == id) dragOffset else Offset.Zero },
                        onDragStart = {
                            draggingId = id
                            dragOffset = Offset.Zero
                        },
                        onDrag = { amount ->
                            dragOffset += amount
                            // 用「被拖格子当前中心」命中目标（见实现要点 1）
                            val selfRect = bounds[id]
                            if (selfRect != null) {
                                val targetId = ItemOrder.findDropTarget(
                                    dragCenter = selfRect.center + dragOffset,
                                    selfId = id,
                                    order = order,
                                    bounds = bounds
                                )
                                if (targetId != null) {
                                    val targetRect = bounds[targetId]
                                    if (targetRect != null) {
                                        order = ItemOrder.move(order, from = id, to = targetId)
                                        // 补偿偏移，避免拖影飞走（见实现要点 2）
                                        dragOffset -= (targetRect.topLeft - selfRect.topLeft)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            dragOffset = Offset.Zero
                            onOrderChange(order)
                        },
                        onDragCancel = {
                            draggingId = null
                            dragOffset = Offset.Zero
                            order = ids
                        }
                    ) { dragging ->
                        content(id, dragging)
                    }
                }
                // 末行不足 columns 时补空位，保证每格宽度一致
                repeat(cols - rowIds.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 单格包装：负责上报位置、叠加拖拽位移与层级，并只在本格内挂手势 */
@Composable
private fun RowScope.CellScope(
    id: String,
    dragging: Boolean,
    enabled: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    dragOffsetProvider: () -> Offset,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    content: @Composable (dragging: Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { onBoundsChanged(it.boundsInParent()) }
            .graphicsLayer {
                val offset = dragOffsetProvider()
                translationX = offset.x
                translationY = offset.y
                // 拖动中的格子放大一点并浮到最上层，明确「正在拖它」
                scaleX = if (dragging) 1.06f else 1f
                scaleY = if (dragging) 1.06f else 1f
                alpha = if (dragging) 0.92f else 1f
            }
            .zIndex(if (dragging) 1f else 0f)
            .pointerInput(id, enabled) {
                if (!enabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
    ) {
        content(dragging)
    }
}
