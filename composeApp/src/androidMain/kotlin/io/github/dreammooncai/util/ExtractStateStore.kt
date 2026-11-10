package io.github.dreammooncai.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 解压状态记录：保存「上次处理某个文件时，**源侧**的内容指纹」。
 *
 * ## 为什么需要它
 * 判断「目标文件是否已是最新、可以跳过解压」时，传统做法是比较**源与目标的文件长度**或**时间戳**，
 * 但这两者在「SMF 压缩优化」之后都失效：
 *
 * 1. **长度不在同一域**：APK 内的 `.rsb.smf` 是 `SMF_MAGIC(int32) + 原始解压长度(int32) + zlib` 压缩容器，
 *    文件字节数远小于解压后的 RSB；而本地工作目录里存的是**解压后的明文**。拿压缩容器长度比明文长度，
 *    两者天然不等 —— 该判据既可能「每次都误判为变化」（全量重解压），也可能被时间戳短路掩盖成「永不重解压」。
 * 2. **时间戳不可靠**：APK 内条目的 `ZipEntry.time` 由打包工具写入（aapt2/ARSCLib 通常沿用源文件 mtime），
 *    APK 内容更新后该时间**未必变新**；而本地文件的 mtime 是「上次解压/上次 JS 写回」的时刻。
 *    一旦出现「源时间 ≤ 本地时间」，就会被永久跳过 —— 这正是「APK 内 SMF 更新了，进游戏却不重新解压」的根因。
 * 3. **还需要保护本地改动**：JS 侧 `data.*` 写回的 SMF（`JsSmfDataManager.packSmf`）落在本地，
 *    其 mtime 是「刚刚」。如果只看长度，改动会导致长度不等 → 每次进游戏都被 APK 覆盖，用户改动丢失。
 *
 * ## 判据
 * 记录的是**源侧指纹**（内容长度 + 源时间戳），与本地文件无关：
 * - 指纹未变 → 说明 **APK 侧内容没变** → 保持本地（不覆盖用户/JS 的改动）；
 * - 指纹变了 → 源更新了 → 必须重新解压。
 *
 * 记录存放在应用私有目录（SharedPreferences），不需要 SAF 写权限，也不受用户清理目标目录影响
 * （目标文件不存在时会直接判为需要解压，记录残留无害）。
 */
object ExtractStateStore {

    private const val PREF_NAME = "pvz2tool_extract_state"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences =
        prefs ?: context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }

    /** 读取某条记录上次保存的源侧指纹；无记录返回 null。 */
    fun fingerprint(context: Context, key: String): String? =
        runCatching { prefs(context).getString(key, null) }.getOrNull()

    /** 保存源侧指纹（解压成功后调用）。 */
    fun putFingerprint(context: Context, key: String, fingerprint: String) {
        runCatching { prefs(context).edit().putString(key, fingerprint).apply() }
    }

    /** 清空全部记录（重置数据包 / 清缓存时调用）。 */
    fun clear(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
    }
}
