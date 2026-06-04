package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.pop.plugin.crypt.Pvz2NumberCrypt
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * PVZ 数据类型枚举
 */
enum class PvzDataType(
    val jsonFile: String,
    vararg chineseNames: String
) {
    Plants("plants.json", "植物"),
    Zombies("zombies.json", "僵尸"),
    GameFeatures("game_features.json", "强化道具"),
    Worlds("worlds.json", "世界"),
    LevelModules("level_modules.json", "关卡模块"),
    GridItems("grid_items.json", "障碍物"),
    Projectiles("projectiles.json", "子弹"),
    ToolPackets("tool_packets.json", "传送带"),
    Properties("properties.json", "挂件"),
    Resources("resources.json", "资源"),
    Avatars("avatars.json", "头像"),
    Artifacts("artifacts.json", "神器"),
    Statuses("statuses.json", "状态"),
    Powerups("powerups.json", "金手指"),
    Genes("genes.json", "基因"),
    Gacha("gacha.json", "藏品"),
    PlantFamilies("plant_families.json", "植物家族");

    /**
     * 获取所有访问名称（首字母小写的英文名 + 中文别名）
     */
    val allNames: List<String> = listOf(name.first().lowercase() + name.substring(1)) + chineseNames.toList()
}

/**
 * PVZ 数据属性中文别名枚举
 */
enum class PvzPropertyChinese(
    vararg val aliases: String
) {
    Id("编号","ID"),
    Name("昵称"),
    Code("代号", "代码"),
    Members("成员"),
    Order("序号"),
    ShardId("碎片编号","碎片ID","碎片id"),
    AvatarId("装扮编号","装扮ID","装扮id"),
    AvatarShardId("装扮碎片编号","装扮碎片ID","装扮碎片id"),
    Avatars("装扮"),
    Section("组别");

    /**
     * 获取所有访问名称（小写英文名 + 中文别名）
     */
    val allNames: List<String> = listOf(name.first().lowercase() + name.substring(1)) + aliases.toList()

    companion object {
        private val aliasesMap: Map<String, PvzPropertyChinese> = entries.flatMap { entry ->
            // 中文别名 + 首字母小写的英文名
            val lowerName = entry.name.first().lowercaseChar().toString() + entry.name.substring(1)
            (listOf(lowerName) + entry.aliases.toList()).map { alias -> alias to entry }
        }.associate { it.first to it.second }

        fun fromKey(key: String): PvzPropertyChinese? = aliasesMap[key]
    }
}

/**
 * PVZ 游戏数据全局对象
 *
 * 支持的数据类型（与 pvz.js 保持一致）：
 * - plants: 植物
 * - zombies: 僵尸
 * - gameFeatures: 强化道具
 * - worlds: 世界
 * - levelModules: 关卡模块
 * - gridItems: 障碍物
 * - projectiles: 子弹
 * - toolPackets: 传送带
 * - properties: 挂件
 * - resources: 资源
 * - avatars: 头像
 * - artifacts: 神器
 * - statuses: 状态
 * - powerups: 金手指
 * - genes: 基因
 * - gacha: 藏品
 * - plantFamilies: 植物家族（家族 + 属性）
 */
object JsPvz {

    // 延迟加载的数据缓存
    private val dataCache = mutableMapOf<PvzDataType, JsonElement?>()

    /**
     * 从 assets 读取 JSON 文件
     */
    private fun loadJsonFromAssets(type: PvzDataType): JsonElement? {
        return try {
            val inputStream = InitializePvz2.context.assets.open("pvz2tool/pvz/${type.jsonFile}")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            Json.parseToJsonElement(jsonStr)
        } catch (e: Exception) {
            JsConsole.error("加载 JSON 失败: ${type.jsonFile} - $e")
            null
        }
    }

    /**
     * 获取数据（带缓存）
     */
    private fun getData(type: PvzDataType): JsonElement? {
        if (!dataCache.containsKey(type)) {
            dataCache[type] = loadJsonFromAssets(type)
        }
        return dataCache[type]
    }

    /**
     * 构建 PVZ 主对象
     */
    val js: JsObject = Object("pvz") {
        listOf("encrypt".js, "加密".js).func("value") { args ->
            val n = toNumber(args[0]).toLong()
            Pvz2NumberCrypt.encrypt(n).js
        }
        listOf("decrypt".js, "解密".js).func("value") { args ->
            val n = toNumber(args[0]).toLong()
            Pvz2NumberCrypt.decrypt(n).js
        }
        listOf("saves".js, "存档".js) eq Object("saves") {
            listOf("load".js, "加载".js).func {
                runCatching {
                    val load = PvzToolJsEngine.getJSEngine().compile("rton.load").invoke() as JSFunction
                    load.invoke(listOf("${JsFileResolver.GAME_SAVES}/pp.dat".js), this)
                }.getOrElse { error("存档读取失败或存档压根不存在啊？至少拥有存档才能使用此功能...") }
            }
        }

        // 遍历所有数据类型
        PvzDataType.entries.forEach { type ->
            val datas = runCatching { getData(type)?.jsonObject }.getOrNull() ?: return@forEach
            type.allNames.map { it.js } eq Object {
                listOf("all".js,"全部".js).js eq datas.map { (_, value) ->
                    val value = runCatching { value.jsonObject }.getOrNull() ?: return@forEach
                    val property = Object {
                        buildProperty(value)
                    }
                    listOfNotNull(
                        value["code"]?.jsonPrimitive?.content?.js,
                        value["name"]?.jsonPrimitive?.content?.js
                    ) eq property
                    property
                }.js
            }
        }
    }

    private fun ObjectScope.buildProperty(element: JsonObject) {
        element.forEach { (key, element) ->
            val propChinese = PvzPropertyChinese.fromKey(key)
            val names = propChinese?.allNames?.map { it.js } ?: listOf(key.js)
            names eq if (element !is JsonObject)
                JsProperty {
                    PvzToolJsEngine.parse(Json.encodeToString(element))
                }
            else Object {
                buildProperty(element)
            }
        }
    }

    /**
     * 清除缓存（用于重新加载）
     */
    fun clearCache() {
        dataCache.clear()
    }
}
