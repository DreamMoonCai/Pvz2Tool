package io.github.dreammooncai.integration

import kotlin.math.min

/**
 * dream.yml 的 **文本拼接式 append-only 合并引擎**。
 *
 * 设计要点（已在真实 dream.yml 上用 Python 原型验证）：
 * - 用 kaml 之外的纯文本层做 **差异检测**（建 id 索引、比较字段 key），
 *   避免重序列化丢失用户手改的注释与格式。
 * - 写回时以 **目标原文件为底**，把源里新增条目的 **原始文本块**（含注释）拼接进去。
 * - section / version / item 均以 `id` 为锚点；item 内字段以字段名为锚点。
 * - 策略：源有目标无 → 追加；目标已有（无论是否被改）→ 跳过；目标独有 → 保留。
 *
 * 缩进约定（与项目 dream.yml 一致）：顶层 0，list item `- id:` 在 2/6，
 * section 字段在 4，item 字段在 8。注释/空行不终止 block。
 */
object YamlTextMerger {

    private const val INDENT = 2

    private fun lineIndent(line: String): Int = line.length - line.takeWhile { it == ' ' }.length

    private fun isIgnorable(line: String): Boolean {
        val s = line.trim()
        return s.isEmpty() || s.startsWith("#")
    }

    /** 返回 block 的独占结束行号（下一个 indent<=indent 的非忽略行，或 EOF）。 */
    private fun blockEnd(lines: List<String>, start: Int, indent: Int): Int {
        var i = start + 1
        val n = lines.size
        while (i < n) {
            val l = lines[i]
            if (isIgnorable(l)) { i++; continue }
            if (lineIndent(l) <= indent) break
            i++
        }
        return i
    }

    private fun findTopKey(lines: List<String>, key: String): Int {
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("$key:") && lineIndent(lines[i]) == 0) return i
        }
        return -1
    }

    /** 在 [headerLine] 指向的列表（如 sections:/items:/versions:）中按 id 找一个 list item。 */
    private fun findListItem(lines: List<String>, headerLine: Int, itemId: String, itemIndent: Int): IntRange? {
        var i = headerLine + 1
        val n = lines.size
        while (i < n) {
            val l = lines[i]
            if (isIgnorable(l)) { i++; continue }
            val ind = lineIndent(l)
            if (ind < itemIndent) break
            if (ind == itemIndent && l.trim().startsWith("- id:")) {
                val m = """- id:\s*["']?([^"']+)["']?""".toRegex().find(l.trim())
                if (m != null && m.groupValues[1] == itemId) {
                    return i until blockEnd(lines, i, itemIndent)
                }
            }
            i++
        }
        return null
    }

    private fun idsOfList(lines: List<String>, headerLine: Int, itemIndent: Int): List<String> {
        val out = mutableListOf<String>()
        var i = headerLine + 1
        val n = lines.size
        while (i < n) {
            val l = lines[i]
            if (isIgnorable(l)) { i++; continue }
            val ind = lineIndent(l)
            if (ind < itemIndent) break
            if (ind == itemIndent && l.trim().startsWith("- id:")) {
                val m = """- id:\s*["']?([^"']+)["']?""".toRegex().find(l.trim())
                if (m != null) out.add(m.groupValues[1])
            }
            i++
        }
        return out
    }

    /** 返回某列表最后一个 item 之后的独占结束位置（用于在其后追加新 item）。 */
    private fun lastItemEnd(lines: List<String>, headerLine: Int, itemIndent: Int): Int {
        var i = headerLine + 1
        val n = lines.size
        var cur = headerLine + 1
        while (i < n) {
            val l = lines[i]
            if (isIgnorable(l)) { i++; continue }
            val ind = lineIndent(l)
            if (ind == itemIndent && l.trim().startsWith("- id:")) {
                cur = blockEnd(lines, i, itemIndent)
                i = cur
                continue
            }
            if (ind < itemIndent && l.trim().isNotEmpty()) break
            i++
        }
        return cur
    }

    /** 收集某个 item block 内、位于 fieldIndent 的字段 key 集合。 */
    private fun fieldKeysOfItem(lines: List<String>, itemRange: IntRange, fieldIndent: Int): List<String> {
        val out = mutableListOf<String>()
        for (i in itemRange) {
            val l = lines[i]
            if (isIgnorable(l)) continue
            if (lineIndent(l) == fieldIndent) {
                val m = """^([A-Za-z_][\w-]*)\s*:""".toRegex().find(l.trim())
                if (m != null) out.add(m.groupValues[1])
            }
        }
        return out
    }

    /** 从源 item block 中抽取某个字段（可能是块标量 `key: |`）的完整文本行。 */
    private fun extractFieldBlock(lines: List<String>, itemRange: IntRange, key: String, fieldIndent: Int): List<String> {
        for (i in itemRange) {
            val l = lines[i]
            if (isIgnorable(l)) continue
            if (lineIndent(l) == fieldIndent && l.trim().startsWith("$key:")) {
                // 块标量：值行缩进 > fieldIndent
                var j = i + 1
                val end = itemRange.last + 1
                while (j < end) {
                    val nl = lines[j]
                    if (isIgnorable(nl)) { j++; continue }
                    if (lineIndent(nl) <= fieldIndent) break
                    j++
                }
                return lines.subList(i, j)
            }
        }
        return emptyList()
    }

    private fun topKeys(lines: List<String>): Set<String> {
        val out = mutableSetOf<String>()
        for (l in lines) {
            if (lineIndent(l) == 0 && !isIgnorable(l)) {
                val m = """^([A-Za-z_][\w-]*)\s*:""".toRegex().find(l.trim())
                if (m != null) out.add(m.groupValues[1])
            }
        }
        return out
    }

    /**
     * 合并 dream.yml。
     * @return 合并后的完整文本，以及差异条目列表（用于预览）。
     */
    fun merge(sourceText: String, targetText: String): Pair<String, List<DiffEntry>> {
        // 目标为空：整文件为新增
        if (targetText.trim().isEmpty()) {
            val entries = scanAllAsAdd(sourceText)
            return sourceText to entries
        }

        val src = sourceText.split("\n")
        val out = targetText.split("\n").toMutableList()
        val entries = mutableListOf<DiffEntry>()
        var addN = 0; var skipN = 0; var keepN = 0

        // ---------- 顶层 key ----------
        val srcTop = topKeys(src)
        val tgtTop = topKeys(out)
        val newTop = srcTop - tgtTop
        val secHeader = findTopKey(src, "sections")
        val verHeader = findTopKey(src, "versions")

        // 先处理列表型（sections/versions），再处理其余顶层 key，避免把列表型 key 当成普通顶层 key 重复
        val listTopKeys = setOf("sections", "versions")

        // ---------- versions ----------
        if (verHeader >= 0) {
            val srcVer = idsOfList(src, verHeader, 2)
            val tgtVer = if (findTopKey(out, "versions") >= 0) idsOfList(out, findTopKey(out, "versions"), 2) else emptyList()
            val newVers = srcVer.filter { it !in tgtVer }
            val keepVers = tgtVer.filter { it !in srcVer }
            // 追加新 version
            if (newVers.isNotEmpty()) {
                val lastEnd = if (findTopKey(out, "versions") >= 0) lastItemEnd(out, findTopKey(out, "versions"), 2) else out.size
                val blocks = newVers.mapNotNull { id -> findListItem(src, verHeader, id, 2)?.let { src.subList(it.first, it.last + 1) } }
                insertAfter(out, lastEnd, blocks)
                newVers.forEach { entries.add(DiffEntry(DiffOp.ADD, "version", "versions/$it", "新增版本 $it")) ; addN++ }
            }
            tgtVer.filter { it in srcVer }.forEach { entries.add(DiffEntry(DiffOp.SKIP, "version", "versions/$it", "版本已存在，跳过")) ; skipN++ }
            keepVers.forEach { entries.add(DiffEntry(DiffOp.KEEP, "version", "versions/$it", "目标独有版本，保留")) ; keepN++ }
        }

        // ---------- sections ----------
        if (secHeader >= 0) {
            val srcSec = idsOfList(src, secHeader, 2)
            val tgtSecHeader = findTopKey(out, "sections")
            val tgtSec = if (tgtSecHeader >= 0) idsOfList(out, tgtSecHeader, 2) else emptyList()
            val newSec = srcSec.filter { it !in tgtSec }
            val keepSec = tgtSec.filter { it !in srcSec }

            // 已在两侧的 section：处理其内部 item 与字段
            for (sid in srcSec.filter { it in tgtSec }) {
                val sRange = findListItem(src, secHeader, sid, 2)!!
                val tRange = findListItem(out, tgtSecHeader, sid, 2)!!
                // 找 items: 子头（indent 4）
                val sItemsHeader = subHeader(src, sRange, "items", 4)
                val tItemsHeader = subHeader(out, tRange, "items", 4)
                if (sItemsHeader != null && tItemsHeader != null) {
                    val srcItems = idsOfList(src, sItemsHeader, 6)
                    val tgtItems = idsOfList(out, tItemsHeader, 6)
                    val newItems = srcItems.filter { it !in tgtItems }
                    // 追加新 item
                    if (newItems.isNotEmpty()) {
                        val lastEnd = lastItemEnd(out, tItemsHeader, 6)
                        val blocks = newItems.mapNotNull { id -> findListItem(src, sItemsHeader, id, 6)?.let { src.subList(it.first, it.last + 1) } }
                        insertAfter(out, lastEnd, blocks)
                        newItems.forEach { entries.add(DiffEntry(DiffOp.ADD, "item", "sections/$sid/items/$it", "栏目 $sid 新增项 $it")) ; addN++ }
                    }
                    tgtItems.filter { it in srcItems }.forEach { entries.add(DiffEntry(DiffOp.SKIP, "item", "sections/$sid/items/$it", "项已存在，跳过")) ; skipN++ }
                    // 已在两侧 item：处理字段级新增
                    for (iid in srcItems.filter { it in tgtItems }) {
                        val siRange = findListItem(src, sItemsHeader, iid, 6)!!
                        val tiRange = findListItem(out, tItemsHeader, iid, 6)!!
                        val sFields = fieldKeysOfItem(src, siRange, 8)
                        val tFields = fieldKeysOfItem(out, tiRange, 8)
                        val newFields = sFields.filter { it !in tFields }
                        if (newFields.isNotEmpty()) {
                            val itemEnd = blockEnd(out, tiRange.first, 6)
                            val blocks = newFields.mapNotNull { key -> extractFieldBlock(src, siRange, key, 8).takeIf { it.isNotEmpty() } }
                            if (blocks.isNotEmpty()) insertBefore(out, itemEnd, blocks)
                            newFields.forEach { entries.add(DiffEntry(DiffOp.ADD, "field", "sections/$sid/items/$iid/$it", "项 $iid 新增字段 $it")) ; addN++ }
                        }
                        tFields.filter { it in sFields }.forEach { entries.add(DiffEntry(DiffOp.SKIP, "field", "sections/$sid/items/$iid/$it", "字段已存在，跳过覆盖")) ; skipN++ }
                    }
                }
                entries.add(DiffEntry(DiffOp.SKIP, "section", "sections/$sid", "栏目已存在，跳过（仅追加其内部新增项/字段）")) ; skipN++
            }

            // 追加新 section
            if (newSec.isNotEmpty()) {
                val lastEnd = if (tgtSecHeader >= 0) lastItemEnd(out, tgtSecHeader, 2) else out.size
                val blocks = newSec.mapNotNull { id -> findListItem(src, secHeader, id, 2)?.let { src.subList(it.first, it.last + 1) } }
                insertAfter(out, lastEnd, blocks)
                newSec.forEach { entries.add(DiffEntry(DiffOp.ADD, "section", "sections/$it", "新增栏目 $it（含其全部项）")) ; addN++ }
            }
            keepSec.forEach { entries.add(DiffEntry(DiffOp.KEEP, "section", "sections/$it", "目标独有栏目，保留")) ; keepN++ }
        }

        // ---------- 其余顶层 key（非列表型） ----------
        for (key in newTop) {
            if (key in listTopKeys) continue
            val sIdx = findTopKey(src, key)
            if (sIdx < 0) continue
            val block = src.subList(sIdx, blockEnd(src, sIdx, 0))
            out.add("")
            out.addAll(block)
            out.add("")
            entries.add(DiffEntry(DiffOp.ADD, "topkey", key, "新增顶层配置 $key"))
            addN++
        }
        // 已存在的顶层 key：聚合跳过
        (tgtTop intersect srcTop).filter { it !in listTopKeys }.forEach { skipN++ }
        if (skipN > 0 && (tgtTop intersect srcTop).any { it !in listTopKeys }) {
            entries.add(DiffEntry(DiffOp.SKIP, "topkey", "*", "其余已存在顶层配置（如 gameActivity/smfDirectory 等）跳过覆盖"))
        }

        // 目标独有顶层 key：保留
        (tgtTop - srcTop).filter { it !in listTopKeys }.forEach {
            entries.add(DiffEntry(DiffOp.KEEP, "topkey", it, "目标独有顶层配置 $it，保留"))
            keepN++
        }

        val merged = out.joinToString("\n")
        return merged to entries
    }

    /** 在 header 指向的 block 内查找某个子 key（如 items:）的行号。 */
    private fun subHeader(lines: List<String>, parentRange: IntRange, key: String, indent: Int): Int? {
        for (i in parentRange) {
            if (lines[i].trim().startsWith("$key:") && lineIndent(lines[i]) == indent) return i
        }
        return null
    }

    private fun insertAfter(out: MutableList<String>, at: Int, blocks: List<List<String>>) {
        if (blocks.isEmpty()) return
        val ins = mutableListOf<String>()
        if (out.getOrNull(at - 1)?.trim()?.isNotEmpty() == true) ins.add("")
        for (b in blocks) { ins.addAll(b); ins.add("") }
        out.addAll(min(at, out.size), ins)
    }

    private fun insertBefore(out: MutableList<String>, at: Int, blocks: List<List<String>>) {
        if (blocks.isEmpty()) return
        val ins = mutableListOf<String>()
        if (out.getOrNull(at - 1)?.trim()?.isNotEmpty() == true) ins.add("")
        for (b in blocks) ins.addAll(b)
        out.addAll(min(at, out.size), ins)
    }

    /** 目标为空时，把源里所有容器登记为 ADD。 */
    private fun scanAllAsAdd(src: String): List<DiffEntry> {
        val lines = src.split("\n")
        val entries = mutableListOf<DiffEntry>()
        val secHeader = findTopKey(lines, "sections")
        val verHeader = findTopKey(lines, "versions")
        if (verHeader >= 0) idsOfList(lines, verHeader, 2).forEach { entries.add(DiffEntry(DiffOp.ADD, "version", "versions/$it", "新增版本 $it")) }
        if (secHeader >= 0) {
            for (sid in idsOfList(lines, secHeader, 2)) {
                entries.add(DiffEntry(DiffOp.ADD, "section", "sections/$sid", "新增栏目 $sid"))
                val sRange = findListItem(lines, secHeader, sid, 2) ?: continue
                val itemsHeader = subHeader(lines, sRange, "items", 4) ?: continue
                for (iid in idsOfList(lines, itemsHeader, 6)) {
                    entries.add(DiffEntry(DiffOp.ADD, "item", "sections/$sid/items/$iid", "新增项 $iid"))
                }
            }
        }
        return entries
    }
}
