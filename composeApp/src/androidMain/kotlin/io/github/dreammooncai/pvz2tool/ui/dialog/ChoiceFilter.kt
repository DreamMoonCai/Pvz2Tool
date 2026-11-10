package io.github.dreammooncai.pvz2tool.ui.dialog

/**
 * 多选弹窗「搜索 / 全选」的纯逻辑。
 *
 * 选中态是以**原始下标**为键的集合（`JsSelectState.items` 的下标），所以过滤后
 * 必须保留原始下标，否则「搜索到第 2 条并勾选」会错勾成别的条目。这类下标错位
 * 在真机上很难复现（要同时满足搜索+勾选+确定），因此抽成纯函数在 JVM 单测覆盖。
 */
object ChoiceFilter {

    /**
     * 按关键字过滤可选条目，返回 `原始下标 to 条目` 列表（顺序与 [items] 一致）。
     *
     * - 关键字先 `trim()`；全空白时原样返回（不过滤）。
     * - 命中规则：`name` 或 `value` 包含关键字（忽略大小写）。
     */
    fun filterWithIndex(items: List<JsChoiceItem>, query: String): List<Pair<Int, JsChoiceItem>> {
        val indexed = items.mapIndexed { index, item -> index to item }
        val q = query.trim()
        if (q.isEmpty()) return indexed
        return indexed.filter { (_, item) ->
            item.name.contains(q, ignoreCase = true) || item.value.contains(q, ignoreCase = true)
        }
    }

    /**
     * 全选 / 取消全选：只增删 [indices] 里的下标，集合里其它下标（如搜索过滤前已勾选的项）不受影响。
     */
    fun applySelectAll(selected: Set<Int>, indices: Collection<Int>, select: Boolean): Set<Int> =
        if (select) selected + indices else selected - indices.toSet()

    /**
     * 是否「已全选」：至少有一个候选下标，且全部已选中。
     * 用于让标题栏按钮在 全选 / 取消全选 之间切换。
     */
    fun isAllSelected(selected: Set<Int>, indices: Collection<Int>): Boolean =
        indices.isNotEmpty() && indices.all { it in selected }
}
