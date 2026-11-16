package io.github.dreammooncai.pvz2tool.pop.save

import android.content.Context
import android.util.Log
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.pop.rton.RTON
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * `pp.dat` 多存档（存档槽）支持。
 *
 * ## 文件结构（解码成 JSON 后）
 * ```
 * {
 *   "version": 1,
 *   "objects": [                        // ← 存档槽数组：新版一个 pp.dat 可放 5 个存档
 *     { "uid": "1.0.b44f1804", "objclass": "PlayerInfo", "objdata": { "n": "用户名", ... } },
 *     { "uid": "1.1.73dca7e8", "objclass": "PlayerInfo", "objdata": { "n": "用户名", ... } }
 *   ],
 *   "@rton_encrypted": true
 * }
 * ```
 * 用户名在各槽的 `objdata.n`。
 *
 * ## 实测样本（assets/pvz2tool/saves/save_multi2、save_multi5）
 * - `uid` 格式为 `<大版本>.<槽位号>.<hash>`，如 `1.0.xxx` / `1.1.xxx` / `1.2.xxx`，
 *   中间的槽位号与 `objects` 下标一致（0 基）。
 * - `save_multi2`：objects 长度 2（牢黑特黑 / TEST1）。
 * - `save_multi5`：objects 长度 5（百合本工页 / 零氪时空再旅 / 4阶鸣人 / 植批地球人 / 3阶响晴），
 *   其中只有「4阶鸣人」有实际游戏进度（`gs` 签名、`l=egypt1`、`gg=2`），其余为空壳（md=-1）。
 *
 * ## 🔴 为什么必须有「选中槽位」
 * `pp.dat` 里**没有**任何「当前玩的是第几个存档」的字段（root 只有 `version` / `objects`，
 * `objdata` 里也没有槽位标记），游戏内选存档的状态不落在这个文件里。
 * 因此工具侧只能自己记住「用户在工具里看/改的是哪个槽」——
 * 之前的代码把索引写死成 `objects[0]`，导致多存档时**永远只显示、只修改第一个**，
 * 玩家在游戏里玩的第 2~5 个存档在这里完全看不见。
 *
 * 规范用法：**所有读写 pp.dat 的地方都走本对象**，不要再自己 `objects[0]`。
 */
object PpDatSaves {

    const val FILE_NAME = "pp.dat"

    private const val TAG = "PpDatSaves"
    private const val PREFS = "pvz2tool_ppdat"
    private const val KEY_PREFIX = "slot:"

    /** 一个存档槽（只取工具需要的字段，其余原样保留在 JSON 里） */
    data class Slot(
        /** 在 objects 数组中的下标（0 基） */
        val index: Int,
        val uid: String?,
        /** `objdata.n`，即存档用户名；可能为空 */
        val name: String?
    ) {
        /** 给用户看的槽位名（1 基，缺名时退化） */
        fun displayName(): String = name?.takeIf { it.isNotBlank() } ?: "存档${index + 1}"
    }

    // ======================== 定位文件 ========================

    /**
     * 当前选中版本的游玩存档目录（配置里 `saves` 栏目的 targetPath）。
     * 配置里找不到 `saves` 栏目时返回 null。
     */
    fun resolveGameSaveDir(): File? {
        val section = InitializePvz2.config.sections.find { it.id == "saves" } ?: return null
        return runCatching { section.resolveTargetDirectory() }.getOrNull()
    }

    /** 游戏存档目录下的 pp.dat（不判断存在性） */
    fun resolveSaveFile(): File? = resolveGameSaveDir()?.let { File(it, FILE_NAME) }

    // ======================== 读 ========================

    /**
     * 解析出所有存档槽。文件不存在 / 解析失败 / 没有 PlayerInfo 时返回空列表。
     *
     * @param cacheDir 解码用的临时目录（传 `context.cacheDir`）
     */
    suspend fun readSlots(ppDat: File, cacheDir: File): List<Slot> {
        val json = decode(ppDat, cacheDir) ?: return emptyList()
        return parseSlots(json)
    }

    /** 从已解码的 JSON 里解析槽位列表（纯函数） */
    fun parseSlots(json: JsonObject): List<Slot> {
        val objects = runCatching { json.getValue("objects").jsonArray }.getOrNull() ?: return emptyList()
        return objects.mapIndexedNotNull { index, element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapIndexedNotNull null
            // 只认玩家存档对象；其它 objclass（若有）跳过但**不改变原下标语义**
            val objclass = runCatching { obj["objclass"]?.jsonPrimitive?.content }.getOrNull()
            if (objclass != null && objclass != "PlayerInfo") return@mapIndexedNotNull null
            val objdata = runCatching { obj.getValue("objdata").jsonObject }.getOrNull()
            Slot(
                index = index,
                uid = runCatching { obj["uid"]?.jsonPrimitive?.content }.getOrNull(),
                name = runCatching { objdata?.get("n")?.jsonPrimitive?.content }.getOrNull()
            )
        }
    }

    /** 读取指定槽的用户名；槽不存在 / 无 `n` 字段返回 null */
    suspend fun readName(ppDat: File, cacheDir: File, index: Int): String? {
        val json = decode(ppDat, cacheDir) ?: return null
        val obj = runCatching { json.getValue("objects").jsonArray[index].jsonObject }.getOrNull() ?: return null
        return runCatching { obj.getValue("objdata").jsonObject.getValue("n").jsonPrimitive.content }.getOrNull()
    }

    // ======================== 写 ========================

    /**
     * 把新用户名写入**指定槽**（其它槽与其余字段原样保留），并重新编码回 RTON。
     *
     * 流程与旧实现一致：解码 → 改 JSON → 写临时 JSON → 编码临时 RTON → 校验非空 → 备份原文件 → 覆盖。
     *
     * @return 是否成功
     */
    suspend fun writeName(ppDat: File, cacheDir: File, index: Int, newName: String): Boolean {
        if (!ppDat.exists() || ppDat.length() == 0L) {
            Log.e(TAG, "pp.dat 不存在或为空")
            return false
        }
        val json = decode(ppDat, cacheDir) ?: return false
        val objects = runCatching { json.getValue("objects").jsonArray }.getOrNull()?.toMutableList()
            ?: return false
        if (index !in objects.indices) {
            Log.e(TAG, "槽位越界：index=$index, size=${objects.size}")
            return false
        }
        val target = runCatching { objects[index].jsonObject }.getOrNull() ?: return false
        val objData = runCatching { target.getValue("objdata").jsonObject }.getOrNull() ?: return false

        // 只改 n（用户名），objdata 其它字段与对象其它键原样保留
        val newObjData = JsonObject(objData.toMutableMap().apply { put("n", JsonPrimitive(newName)) })
        val newTarget = JsonObject(target.toMutableMap().apply { put("objdata", newObjData) })
        objects[index] = newTarget
        val newJson = JsonObject(json.toMutableMap().apply { put("objects", JsonArray(objects)) })

        val tempJson = File(cacheDir, "ppdat_json_${System.currentTimeMillis()}.json")
        val tempRton = File(cacheDir, "ppdat_rton_${System.currentTimeMillis()}.dat")
        var backup: File? = null
        return try {
            tempJson.writeText(newJson.toString())
            RTON.encodeAuto(tempJson.absolutePath, tempRton.absolutePath)
            if (!tempRton.exists() || tempRton.length() == 0L) {
                Log.e(TAG, "生成的 RTON 文件无效")
                return false
            }
            // 安全起见先备份再覆盖；成功后删备份
            backup = File(ppDat.parentFile, "$FILE_NAME.backup_${System.currentTimeMillis()}")
            ppDat.copyTo(backup, overwrite = true)
            tempRton.copyTo(ppDat, overwrite = true)
            backup.delete()
            backup = null
            Log.d(TAG, "槽位 $index 用户名已改为：$newName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "写入用户名失败", e)
            // 覆盖失败时把备份还原回去，避免存档被写坏
            backup?.takeIf { it.exists() }?.let { b ->
                runCatching { b.copyTo(ppDat, overwrite = true) }
            }
            false
        } finally {
            tempJson.delete()
            tempRton.delete()
            backup?.delete()
        }
    }

    // ======================== 选中槽位（工具侧持久化）========================

    /**
     * 当前选中的槽位下标（已按 slotCount 钳制）。**只在多槽时才有意义**，单槽恒为 0。
     *
     * 按「存档目录绝对路径」分别记忆：不同版本 / 不同存档目录各自独立。
     */
    fun selectedIndex(context: Context, saveDir: File, slotCount: Int): Int {
        if (slotCount <= 1) return 0
        val saved = prefs(context).getInt(KEY_PREFIX + saveDir.absolutePath, 0)
        return saved.coerceIn(0, slotCount - 1)
    }

    /** 记住用户选的槽位（下次打开同一存档目录仍然生效） */
    fun setSelectedIndex(context: Context, saveDir: File, index: Int) {
        prefs(context).edit().putInt(KEY_PREFIX + saveDir.absolutePath, index).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ======================== 高层封装：直接面向「当前游戏的 pp.dat + 工具选中槽」========================

    // 槽位解析结果缓存（pp.dat 可能几百 KB，脚本里连续调用别反复解码）
    private var cacheKey: Pair<String, Long>? = null
    private var cacheSlots: List<Slot> = emptyList()

    /**
     * 当前游戏存档的全部槽位（带缓存，按「路径 + 最后修改时间」失效）。
     * 文件不存在 / 解析失败返回空列表。
     */
    suspend fun readSlotsOfCurrentSave(context: Context): List<Slot> {
        val saveDir = resolveGameSaveDir() ?: return emptyList()
        val ppDat = File(saveDir, FILE_NAME)
        if (!ppDat.exists()) return emptyList()
        val key = ppDat.absolutePath to ppDat.lastModified()
        cacheKey?.let { if (it == key) return cacheSlots }
        val slots = readSlots(ppDat, context.cacheDir)
        cacheKey = key
        cacheSlots = slots
        return slots
    }

    /** 工具侧当前选中的槽位在 [readSlotsOfCurrentSave] 列表中的**位置**（0 基，已钳制） */
    fun selectedPosition(context: Context, slotCount: Int): Int {
        val saveDir = resolveGameSaveDir() ?: return 0
        return selectedIndex(context, saveDir, slotCount)
    }

    /** 记住选中槽位（位置，0 基） */
    fun setSelectedPosition(context: Context, position: Int) {
        val saveDir = resolveGameSaveDir() ?: return
        setSelectedIndex(context, saveDir, position)
    }

    /** 当前选中槽的用户名（欢迎语 / 存档信息用） */
    suspend fun readSelectedName(context: Context): String? {
        val slots = readSlotsOfCurrentSave(context)
        if (slots.isEmpty()) return null
        return slots.getOrNull(selectedPosition(context, slots.size))?.name
    }

    /** 把新用户名写入当前选中槽 */
    suspend fun writeSelectedName(context: Context, newName: String): Boolean {
        val saveDir = resolveGameSaveDir() ?: return false
        val slots = readSlotsOfCurrentSave(context)
        if (slots.isEmpty()) return false
        val slot = slots.getOrNull(selectedPosition(context, slots.size)) ?: return false
        val ok = writeName(File(saveDir, FILE_NAME), context.cacheDir, slot.index, newName)
        if (ok) cacheKey = null // 写完让缓存失效，下次重新解析
        return ok
    }

    // ======================== 内部：RTON <-> JSON ========================

    /** 解码 pp.dat 为 JSON 对象；失败返回 null（临时文件必定清理） */
    private suspend fun decode(ppDat: File, cacheDir: File): JsonObject? {
        if (!ppDat.exists() || ppDat.length() == 0L) return null
        val tmp = File(cacheDir, "ppdat_dec_${System.currentTimeMillis()}.json")
        return try {
            RTON.decodeAuto(ppDat.absolutePath, tmp.absolutePath)
            Json.parseToJsonElement(tmp.readText()).jsonObject
        } catch (e: Exception) {
            Log.e(TAG, "解码 pp.dat 失败", e)
            null
        } finally {
            tmp.delete()
        }
    }
}
