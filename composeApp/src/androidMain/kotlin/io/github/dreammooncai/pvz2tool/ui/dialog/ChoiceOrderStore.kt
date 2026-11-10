package io.github.dreammooncai.pvz2tool.ui.dialog

import android.content.Context
import io.github.dreammooncai.pvz2tool.InitializePvz2
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 多选弹窗「拖动排序」结果的持久化。
 *
 * 存 SharedPreferences（一个 JSON 字符串数组，按 key 分组）：
 * - 弹窗下次打开时用 `ItemOrder.applyOrder` 把保存顺序套回配置顺序上；
 * - 配置改动（增删条目）由 applyOrder 兜底：新条目追加、已删除条目忽略。
 *
 * 为什么不用 `JsStorage`（JS 侧 kv）：那是脚本可见的存储，把 UI 排序状态混进去
 * 会污染用户的 `storage.keys()`；UI 自己的偏好单独用一个 prefs 文件更干净。
 */
internal object ChoiceOrderStore {

    private const val PREFS_NAME = "pvz2tool_choice_order"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(String.serializer())

    private fun prefs() = InitializePvz2.context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读取某个弹窗保存的顺序（空 = 未保存过） */
    fun load(key: String): List<String> {
        val raw = prefs().getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    /** 保存拖动后的顺序 */
    fun save(key: String, values: List<String>) {
        runCatching {
            prefs().edit().putString(key, json.encodeToString(serializer, values)).apply()
        }
    }

    /** 清除某个弹窗的顺序（恢复配置默认顺序） */
    fun clear(key: String) {
        runCatching { prefs().edit().remove(key).apply() }
    }
}
