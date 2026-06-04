package io.github.dreammooncai.pvz2tool.js.code

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.contains
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.func

/**
 * JS 持久化存储 API，封装 SharedPreferences。
 *
 * 支持的数据类型：
 * - String: 直接存储
 * - Number: 转为 Double 存储
 * - Boolean: 转为 String 存储
 * - Object/Array: JSON 序列化后存储
 *
 * JS 使用示例：
 * ```javascript
 * // 存储数据
 * storage.set("username", "张三");
 * storage.set("level", 99);
 * storage.set("settings", { theme: "dark", sound: true });
 *
 * // 读取数据
 * let name = storage.get("username");  // "张三"
 * let lvl = storage.get("level");      // 99
 * let settings = storage.get("settings");  // { theme: "dark", sound: true }
 *
 * // 删除数据
 * storage.delete("username");
 *
 * // 检查 key 是否存在
 * let exists = storage.has("username");  // false
 *
 * // 清空所有数据
 * storage.clear();
 *
 * // 获取所有 key
 * let keys = storage.keys();  // ["level", "settings"]
 *
 * // 获取所有数据
 * let all = storage.getAll();  // { level: 99, settings: { theme: "dark", sound: true } }
 * ```
 */
class JsStorage(private val name: String = "") {

    companion object {
        private const val DEFAULT_NAME = "pvz2tool_js_storage"
    }

    private val prefs by lazy {
        SharedPreferencesSettings( InitializePvz2.context.getSharedPreferences("${DEFAULT_NAME}_$name", Context.MODE_PRIVATE))
    }

    val js = buildStorageContext()

    /**
     * 获取所有 key
     */
    fun keys(): Set<String> = prefs.keys

    /**
     * 检查 key 是否存在
     */
    fun has(key: String): Boolean = prefs.contains(key)

    /**
     * 获取值
     */
    suspend fun get(key: String): JsAny? = PvzToolJsEngine.parse(prefs.getStringOrNull(key))

    /**
     * 设置值
     */
    suspend fun set(key: String, value: JsAny?) {
        if (value == null) {
            prefs.remove(key)
            return
        }
        prefs.putString(key, PvzToolJsEngine.stringify(value))
    }

    /**
     * 删除指定的 key
     */
    fun delete(key: String) {
        prefs.remove(key)
    }

    /**
     * 清空所有数据
     */
    fun clear() {
        prefs.clear()
    }

    /**
     * 获取所有数据
     */
    suspend fun getAll(): List<JsAny> = keys().mapNotNull { get(it) }
}

/**
 * 构建 storage 对象供 JS 使用
 */
private fun JsStorage.buildStorageContext(): JsObject {
    return Object {
        // get(key) - 获取值
        listOf("get".js, "获取".js).func("key") { args ->
            val key = toString(args[0])
            get(key)
        }

        // set(key, value) - 设置值
        listOf("set".js, "设置".js).func("key", "value") { args ->
            val key = toString(args[0])
            val value = args.getOrNull(1)
            set(key, value)
            Undefined
        }

        // delete(key) - 删除值
        listOf("delete".js, "删除".js).func("key") { args ->
            val key = toString(args[0])
            delete(key)
            Undefined
        }

        // has(key) - 检查 key 是否存在
        listOf("has".js, "有".js).func("key") { args ->
            val key = toString(args[0])
            has(key).js
            Undefined
        }

        // clear() - 清空所有数据
        listOf("clear".js, "清空".js).func { _ ->
            clear()
            Undefined
        }

        // keys() - 获取所有 key
        listOf("keys".js, "键列表".js).func { _ ->
            keys().map { it.js }.js
        }

        // getAll() - 获取所有数据
        listOf("getAll".js, "获取全部".js).func { _ ->
            getAll().js
        }
    }
}