package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 栏目条目「自定义排序」的纯逻辑。
 *
 * 配置里的 `items` 是**默认顺序**，用户拖动后把顺序存进 `DynamicSectionState.itemOrder`。
 * 两者可能不同步（脚本动态增删条目、用户切换配置版本），所以合并规则必须容错：
 * - 已记录顺序里的条目按记录排；
 * - **新增**（记录里没有）的条目按配置里的原始相对位置排在后面，不能被吞掉；
 * - 记录里已不存在（配置删掉了）的条目直接忽略。
 *
 * 这些边界在真机上很难复现（要同时满足「拖过顺序 + 配置改动」），抽成纯函数在 JVM 覆盖。
 */
object ItemOrder {

    /**
     * 把配置顺序 [configured] 按已保存顺序 [saved] 重排。
     *
     * @param configured 配置里的条目 id（默认顺序）
     * @param saved 用户拖动后保存的顺序（可为空 = 用默认顺序）
     */
    fun applyOrder(configured: List<String>, saved: List<String>): List<String> {
        if (saved.isEmpty()) return configured
        val configuredSet = configured.toHashSet()
        val result = ArrayList<String>(configured.size)
        // 1) 先按保存顺序放「仍然存在」的条目
        saved.forEach { id -> if (id in configuredSet && id !in result) result.add(id) }
        // 2) 剩下的（新增条目 / 保存顺序里没有的）按配置顺序追加
        configured.forEach { id -> if (id !in result) result.add(id) }
        return result
    }

    /**
     * 把 [from] 移动到 [to] 的位置（拖拽落点语义：插到 `to` 所在位置，其余顺延）。
     *
     * - 任一 id 不在 [order] 中时原样返回（避免把列表改坏）；
     * - `from == to` 时原样返回。
     */
    /**
     * 拖动时命中判定：给定「被拖格子的当前中心点」与各格子的布局矩形，返回应换位的目标 id。
     *
     * 用**被拖格子的中心**而不是手指坐标：
     * 长按后手指常略偏格子中心，用指针命中会在换位后反复来回跳（拖影抖动）。
     */
    fun findDropTarget(
        dragCenter: Offset,
        selfId: String,
        order: List<String>,
        bounds: Map<String, Rect>,
    ): String? = order.firstOrNull { other ->
        other != selfId && bounds[other]?.contains(dragCenter) == true
    }

    fun move(order: List<String>, from: String, to: String): List<String> {
        val fromIndex = order.indexOf(from)
        val toIndex = order.indexOf(to)
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return order
        val mutable = order.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable
    }
}
