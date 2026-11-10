package io.github.dreammooncai.pvz2tool

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.dreammooncai.pvz2tool.ui.ItemOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 栏目条目自定义排序（拖动改位置）的纯逻辑单测。
 *
 * 覆盖两类真机上极难复现的边界：
 * 1. 「拖过顺序后又改了配置」——新增条目不能被吞、删掉的条目不能残留；
 * 2. 落点语义——把 A 拖到 B 的位置时，是「插入到 B 处、其余顺延」，不是两者交换。
 */
class ItemOrderTest {

    @Test
    fun emptySavedKeepsConfiguredOrder() {
        assertEquals(listOf("a", "b", "c"), ItemOrder.applyOrder(listOf("a", "b", "c"), emptyList()))
    }

    @Test
    fun savedOrderIsApplied() {
        assertEquals(listOf("c", "a", "b"), ItemOrder.applyOrder(listOf("a", "b", "c"), listOf("c", "a", "b")))
    }

    @Test
    fun newItemsAppendedInConfiguredOrder() {
        // 配置新增了 d/e，保存顺序里只有旧的 c/a/b
        val result = ItemOrder.applyOrder(
            configured = listOf("a", "b", "c", "d", "e"),
            saved = listOf("c", "a", "b")
        )
        assertEquals(listOf("c", "a", "b", "d", "e"), result)
    }

    @Test
    fun removedItemsAreDropped() {
        // 保存顺序里有已删除的 x，结果不应包含它
        val result = ItemOrder.applyOrder(
            configured = listOf("a", "b"),
            saved = listOf("x", "b", "a")
        )
        assertEquals(listOf("b", "a"), result)
    }

    @Test
    fun duplicatesInSavedDoNotDuplicateResult() {
        val result = ItemOrder.applyOrder(
            configured = listOf("a", "b"),
            saved = listOf("b", "b", "a")
        )
        assertEquals(listOf("b", "a"), result)
    }

    @Test
    fun moveInsertsAtTargetPosition() {
        assertEquals(
            listOf("b", "c", "a"),
            ItemOrder.move(listOf("a", "b", "c"), from = "a", to = "c")
        )
    }

    @Test
    fun moveBackwards() {
        assertEquals(
            listOf("c", "a", "b"),
            ItemOrder.move(listOf("a", "b", "c"), from = "c", to = "a")
        )
    }

    @Test
    fun moveWithUnknownIdIsNoOp() {
        val order = listOf("a", "b")
        assertEquals(order, ItemOrder.move(order, from = "zzz", to = "a"))
        assertEquals(order, ItemOrder.move(order, from = "a", to = "zzz"))
        assertEquals(order, ItemOrder.move(order, from = "a", to = "a"))
    }

    // ===== 拖动命中判定（换位目标）=====

    /** 3 列网格：每格 100x100，按 a/b/c 排 */
    private val gridBounds = mapOf(
        "a" to Rect(0f, 0f, 100f, 100f),
        "b" to Rect(100f, 0f, 200f, 100f),
        "c" to Rect(0f, 100f, 100f, 200f)
    )

    @Test
    fun dropTargetHitsCellContainingCenter() {
        assertEquals(
            "b",
            ItemOrder.findDropTarget(Offset(150f, 50f), "a", listOf("a", "b", "c"), gridBounds)
        )
    }

    @Test
    fun dropTargetSkipsSelf() {
        // 中心仍在 a 自己范围内 → 不应返回 a
        assertEquals(null, ItemOrder.findDropTarget(Offset(50f, 50f), "a", listOf("a", "b", "c"), gridBounds))
    }

    @Test
    fun dropTargetNullWhenOutsideAllCells() {
        assertEquals(
            null,
            ItemOrder.findDropTarget(Offset(999f, 999f), "a", listOf("a", "b", "c"), gridBounds)
        )
    }

    @Test
    fun dropTargetFollowsOrderPriority() {
        // 若两块矩形重叠，取顺序里更靠前的那块，保证结果稳定（不随机）
        val overlapping = mapOf(
            "a" to Rect(0f, 0f, 300f, 300f),
            "b" to Rect(0f, 0f, 300f, 300f)
        )
        assertEquals(
            "b",
            ItemOrder.findDropTarget(Offset(50f, 50f), "a", listOf("b", "a"), overlapping)
        )
    }
}
