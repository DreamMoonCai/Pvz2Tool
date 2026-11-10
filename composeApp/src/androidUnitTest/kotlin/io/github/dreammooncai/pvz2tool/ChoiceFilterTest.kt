package io.github.dreammooncai.pvz2tool

import io.github.dreammooncai.pvz2tool.ui.dialog.ChoiceFilter
import io.github.dreammooncai.pvz2tool.ui.dialog.JsChoiceItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 多选弹窗「搜索 / 全选」纯逻辑单测（JVM，秒级）。
 *
 * 重点覆盖两件在真机上极难复现的事：
 * 1. 过滤后**保留原始下标**——否则「搜索后勾选确定」会返回错的条目值；
 * 2. 全选只增删**当前可见**下标——否则「搜索后全选」会误改被过滤掉的条目。
 */
class ChoiceFilterTest {

    private val items = listOf(
        JsChoiceItem(name = "阳光无限", value = "sun"),
        JsChoiceItem(name = "冷却无CD", value = "cool"),
        JsChoiceItem(name = "自动收集", value = "auto"),
        JsChoiceItem(name = "SunFlower", value = "flower"),
    )

    @Test
    fun blankQueryKeepsAllItemsAndOriginalIndices() {
        val result = ChoiceFilter.filterWithIndex(items, "   ")
        assertEquals(listOf(0, 1, 2, 3), result.map { it.first })
        assertEquals(items, result.map { it.second })
    }

    @Test
    fun filterMatchesNameCaseInsensitively() {
        // "sun" 命中第 1 条中文名不含、value 含 sun → 同时命中第 4 条 SunFlower
        val result = ChoiceFilter.filterWithIndex(items, "sun")
        assertEquals(listOf(0, 3), result.map { it.first })
    }

    @Test
    fun filterMatchesChineseName() {
        val result = ChoiceFilter.filterWithIndex(items, "自动")
        assertEquals(listOf(2), result.map { it.first })
        assertEquals("auto", result.single().second.value)
    }

    @Test
    fun filterKeepsOriginalIndexWhenItemsShift() {
        // 过滤后剩下第 2 条（原下标 1）：必须仍是 1，而不是过滤后的 0
        val result = ChoiceFilter.filterWithIndex(items, "冷却")
        assertEquals(1, result.single().first)
    }

    @Test
    fun filterNoMatchReturnsEmpty() {
        assertTrue(ChoiceFilter.filterWithIndex(items, "不存在的名字").isEmpty())
    }

    @Test
    fun applySelectAllAddsOnlyGivenIndices() {
        val selected = setOf(0, 3) // 过滤前已勾选
        val result = ChoiceFilter.applySelectAll(selected, listOf(1, 2), select = true)
        assertEquals(setOf(0, 1, 2, 3), result)
    }

    @Test
    fun applySelectAllRemovesOnlyGivenIndices() {
        val selected = setOf(0, 1, 2, 3)
        val result = ChoiceFilter.applySelectAll(selected, listOf(1, 2), select = false)
        // 被过滤掉的 0、3 不受影响
        assertEquals(setOf(0, 3), result)
    }

    @Test
    fun isAllSelectedHandlesEmptyAndPartial() {
        assertFalse(
            ChoiceFilter.isAllSelected(setOf(0), emptyList()),
            "空候选不算全选（避免按钮恒显示「取消全选」）"
        )
        assertFalse(ChoiceFilter.isAllSelected(setOf(0), listOf(0, 1)))
        assertTrue(ChoiceFilter.isAllSelected(setOf(0, 1, 5), listOf(0, 1)))
    }
}
