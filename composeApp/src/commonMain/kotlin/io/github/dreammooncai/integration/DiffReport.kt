package io.github.dreammooncai.integration

/**
 * 工具箱集成器 —— 差异合并的结果模型。
 *
 * 合并策略统一为 **append-only（纯增量）**：
 * - 源里目标没有的字段/栏目/项/资源 → [DiffOp.ADD]（追加，保留源注释）
 * - 目标已存在（无论是否被用户改过）  → [DiffOp.SKIP]（跳过，绝不覆盖）
 * - 目标独有（源里没有，用户自定义）  → [DiffOp.KEEP]（保留，绝不删除）
 */
enum class DiffOp { ADD, SKIP, KEEP, CONFLICT }

/**
 * 单条差异记录。
 * @param op      操作类型
 * @param kind    实体类型：section / version / item / field / file / topkey
 * @param path    稳定路径，如 `sections/audio_settings/items/bgm_volume`
 * @param summary 人类可读摘要
 */
data class DiffEntry(
    val op: DiffOp,
    val kind: String,
    val path: String,
    val summary: String
)

