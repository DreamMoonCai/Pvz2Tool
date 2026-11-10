package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 可**拖动排序**的条目容器，同时支持列表（[columns] = 1）与网格（[columns] > 1）。
 *
 * ## 交互（需求约定）
 * - 列表：**长按**行尾「≡」把手后拖动；
 * - 网格：**长按**格子里的图标后拖动；
 * - 行体/空白处不响应拖动；拖到别的格子上方换位，松手回调新顺序。
 *
 * ## 关键设计（三处都踩过坑，别改回去）
 * 1. **手势挂容器，不挂格子**：换位会改变格子在 Column/Row 里的位置，Compose 会把该位置的节点
 *    重建，挂在格子上的触摸协程随之取消 → 一换位手势就断、顺序弹回。容器节点不因换位重建。
 * 2. **坐标统一用 root**：`boundsInParent` 在列表里是「相对各自那一行」的（每行都从 y=0 开始），
 *    和手指坐标（相对整个容器）不同空间 → 命中错乱、换位补偿把格子甩出屏幕。
 * 3. **把手范围由内容上报**：`content` 第三个参数贴到把手/图标上，容器据此判断「按住这块才算起拖」，
 *    不去猜行内 padding。
 * 4. **布局用普通 Column+Row，不要自定义 Layout**：弹窗走 `PerfectAdaptiveLayout`，
 *    会向子树请求 intrinsic 高度，自定义 MeasurePolicy 量不准会让内容溢出卡片。
 *
 * @param ids 要展示的条目 id（顺序 = 初始顺序）
 * @param columns 每行列数；1 = 列表
 * @param onOrderChange 拖动结束后回调新顺序
 * @param content 单格内容；`dragging` 表示正在被拖，第三个参数必须贴到把手/图标上
 */
@Composable
fun ReorderableItemGrid(
    ids: List<String>,
    columns: Int,
    onOrderChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemSpacing: Dp = 8.dp,
    content: @Composable (id: String, dragging: Boolean, handleModifier: Modifier) -> Unit,
) {
    var order by remember { mutableStateOf(ids) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    val handleBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // ⚠️ 只在「条目集合变了」时跟随外部（首次/搜索过滤）。不要按顺序比较：
    // 松手后父级会把新顺序回传，若这里再按新 ids 重置，会和拖动结果打架（表现为闪回）。
    LaunchedEffect(ids) {
        if (order.size != ids.size || order.toSet() != ids.toSet()) order = ids
    }

    val cols = columns.coerceAtLeast(1)

    // 防御：`order` 可能还是「上一个弹窗/上一次过滤」的 id 列表（remember 跨弹窗复用），
    // 先按当前 ids 过滤、再把新出现的补上——否则会拿旧下标去索引新列表，直接越界崩溃。
    val renderOrder = run {
        val current = ids.toHashSet()
        val kept = order.filter { it in current }
        if (kept.size == ids.size) kept else kept + ids.filter { it !in kept.toHashSet() }
    }

    fun hitTestCell(position: Offset): String? =
        order.firstOrNull { bounds[it]?.contains(position) == true }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerCoords = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    // 🔴 用 Initial 通道：先于子级（行的 clickable / 外层滚动）拿到事件，
                    // 否则长按后子级已经吃掉 MOVE，容器这边「只变大一格都不动」。
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val root = containerCoords?.localToRoot(down.position) ?: down.position
                    val pressedId = hitTestCell(root) ?: return@awaitEachGesture
                    // 必须按在把手/图标上；按行体空白不响应（也不消费，留给点击）
                    if (handleBounds[pressedId]?.contains(root) != true) return@awaitEachGesture
                    down.consume()

                    // —— 等长按（期间若移动超过 slop 说明是滑动，放弃） ——
                    val timeout = viewConfiguration.longPressTimeoutMillis
                    val longPressed = withTimeoutOrNull(timeout) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull false
                            if (!change.pressed) return@withTimeoutOrNull false
                            if ((change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) return@withTimeoutOrNull false
                            change.consume()
                        }
                        @Suppress("UNREACHABLE_CODE") false
                    } == null
                    if (!longPressed) return@awaitEachGesture

                    draggingId = pressedId
                    dragOffset = Offset.Zero
                    // —— 跟随手指，直到抬手 ——
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        dragOffset += change.positionChange()
                        change.consume()
                        val selfRect = bounds[pressedId]
                        if (selfRect != null) {
                            val targetId = ItemOrder.findDropTarget(
                                dragCenter = selfRect.center + dragOffset,
                                selfId = pressedId,
                                order = order,
                                bounds = bounds
                            )
                            val targetRect = targetId?.let { bounds[it] }
                            if (targetId != null && targetRect != null) {
                                order = ItemOrder.move(order, from = pressedId, to = targetId)
                                dragOffset -= (targetRect.topLeft - selfRect.topLeft)
                            }
                        }
                    }
                    draggingId = null
                    dragOffset = Offset.Zero
                    onOrderChange(order)
                }
            }
    ) {
        renderOrder.chunked(cols).forEachIndexed { rowIndex, rowIds ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(itemSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                rowIds.forEach { id ->
                    key(id) {
                        Cell(
                            id = id,
                            dragging = draggingId == id,
                            dragOffset = { if (draggingId == id) dragOffset else Offset.Zero },
                            onBoundsChanged = { bounds[id] = it },
                            modifier = Modifier.weight(1f)
                        ) { dragging ->
                            content(
                                id,
                                dragging,
                                Modifier.onGloballyPositioned { coords ->
                                    handleBounds[id] = coords.boundsInRoot()
                                }
                            )
                        }
                    }
                }
                repeat(cols - rowIds.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 单格：上报位置、叠加拖拽/换位动画、拖动时提升层级。
 *
 * ⚠️ 这里**不挂拖动/长按手势**——手势统一在容器上处理（见文件头说明）。
 */
@Composable
private fun Cell(
    id: String,
    dragging: Boolean,
    dragOffset: () -> Offset,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (dragging: Boolean) -> Unit,
) {
    // 换位后布局瞬时变化：记上一帧位置，变化时以「旧位置 - 新位置」为起始偏移弹回 0，
    // 视觉上从原位滑过去（普通 Column 没有 animateItemPlacement）。
    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var lastTopLeft by remember { mutableStateOf<Offset?>(null) }
    var wasDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val rect = coords.boundsInRoot()
                onBoundsChanged(rect)
                val prev = lastTopLeft
                if (wasDragging && !dragging) {
                    lastTopLeft = rect.topLeft
                    wasDragging = dragging
                    return@onGloballyPositioned
                }
                wasDragging = dragging
                if (prev != null && !dragging && prev != rect.topLeft) {
                    scope.launch {
                        animOffset.snapTo(prev - rect.topLeft)
                        animOffset.animateTo(
                            targetValue = Offset.Zero,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f)
                        )
                    }
                }
                lastTopLeft = rect.topLeft
            }
            .graphicsLayer {
                val drag = dragOffset()
                translationX = drag.x + animOffset.value.x
                translationY = drag.y + animOffset.value.y
                scaleX = if (dragging) 1.06f else 1f
                scaleY = if (dragging) 1.06f else 1f
                alpha = if (dragging) 0.94f else 1f
            }
            .zIndex(if (dragging) 1f else 0f)
    ) {
        content(dragging)
    }
}
