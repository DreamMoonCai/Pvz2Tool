package io.github.dreammooncai.pvz2tool

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.charleskorn.kaml.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.dreammooncai.pvz2tool.ui.main.Pvz2ScreenState
import io.github.dreammooncai.util.ContextUtil
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import kotlinx.serialization.json.Json
import java.io.File

@SuppressLint("StaticFieldLeak")
object InitializePvz2 {
    lateinit var context: Context

    var mPvz2MainScreenReloadKey by mutableIntStateOf(0)

    val settings: Settings by lazy {
        SharedPreferencesSettings(context.getSharedPreferences("pvz2tool", 0))
    }

    var errorScreenState by mutableStateOf<Pvz2ErrorScreenState?>(null)

    val versionName: Version by lazy {
        runCatching {
            runCatching {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            }.getOrNull()?.versionName?.toVersion()
        }.onFailure { e -> errorScreenState = Pvz2ErrorScreenState("版本号不规范",e) }.getOrNull() ?: Version.min
    }

    // 最终合并后的配置
    lateinit var config: Pvz2ToolConfig

    // 初始化配置（基础配置 + 本地配置合并）
    fun initConfig() {
        val appContext = context.applicationContext
        val yaml = Yaml()

        // 1. 读取APK内置的基础配置
        val baseConfigContent = appContext.assets.open("${Pvz2ToolConfig.PATH_NAME}/dream.yml").use { input ->
            input.reader().readText()
        }
        val baseYamlNode = yaml.parseToYamlNode(baseConfigContent)
        val baseConfig = yaml.decodeFromString(Pvz2ToolConfig.serializer(), baseConfigContent)

        // 2. 合并本地配置（若存在）
        val mergedYamlNode = tryMergeLocalConfig(yaml, baseConfig, baseYamlNode)

        // 3. 反序列化为最终配置
        config = yaml.decodeFromYamlNode(Pvz2ToolConfig.serializer(), mergedYamlNode)
    }

    /**
     * 尝试读取本地配置文件并与基础配置合并
     */
    private fun tryMergeLocalConfig(
        yaml: Yaml,
        baseConfig: Pvz2ToolConfig,
        baseYamlNode: YamlNode
    ): YamlNode {
        return try {
            val docFile = mLocalConfigDirUri?.let { DocumentFile.fromTreeUri(context,it) }
            val yml = docFile?.listFiles()?.find { it.name?.endsWith(".yml", ignoreCase = true) == true || it.name?.endsWith(".yaml", ignoreCase = true) == true }
            // 读取本地配置内容并解析为YamlNode
            val localConfigContent = if (yml != null) {
                context.contentResolver.openInputStream(yml.uri)?.use { input ->
                    input.reader().readText()
                } ?: return baseYamlNode
            } else {
                // 本地配置路径为空/文件不存在 → 返回基础配置
                val localFile = File(baseConfig.localConfigFile ?: return baseYamlNode)
                if (!localFile.exists()) return baseYamlNode
                localFile.readText()
            }
            val localYamlNode = yaml.parseToYamlNode(localConfigContent)

            // 递归合并节点
            mergeYamlNodes(baseYamlNode, localYamlNode)
        } catch (e: Exception) {
            e.printStackTrace()
            baseYamlNode // 合并失败则使用基础配置
        }
    }

    /**
     * 核心递归合并逻辑：
     * 1. sections列表 → 按ID匹配更新（支持addItems）
     * 2. YamlMap（对象）→ 按Key覆盖
     * 3. 其他类型 → 直接替换
     */
    private fun mergeYamlNodes(baseNode: YamlNode, localNode: YamlNode): YamlNode {
        // ========== 关键修正：基于YamlPathSegment准确识别sections列表 ==========
        // 判断逻辑：
        // 1. 当前节点是YamlList（列表）
        // 2. 路径中包含 MapElementKey(key = "sections") 这个segment
        val isSectionsList = baseNode is YamlList && localNode is YamlList &&
                baseNode.path.segments.any { segment ->
                    segment is YamlPathSegment.MapElementKey && segment.key == "sections"
                }

        if (isSectionsList) {
            // 触发sections专属合并逻辑（按ID匹配，保留其他栏目）
            return mergeSectionsSequence(baseNode, localNode)
        }

        // 常规处理：YamlMap（对象）按Key合并
        if (baseNode is YamlMap && localNode is YamlMap) {
            val baseStringKeyMap = baseNode.entries.entries.associateBy { it.key.content }
            val localStringKeyMap = localNode.entries.entries.associateBy { it.key.content }
            val mergedEntries = mutableMapOf<YamlScalar, YamlNode>()

            // 合并已有Key（本地覆盖基础）
            baseStringKeyMap.forEach { (stringKey, entry) ->
                val (baseScalarKey, baseValueNode) = entry
                val localEntry = localStringKeyMap[stringKey]
                mergedEntries[baseScalarKey] = localEntry?.let {
                    mergeYamlNodes(baseValueNode, it.value)
                } ?: baseValueNode
            }

            // 添加本地独有的Key
            localStringKeyMap.forEach { (stringKey, entry) ->
                if (!baseStringKeyMap.containsKey(stringKey)) {
                    mergedEntries[entry.key] = entry.value
                }
            }

            return YamlMap(mergedEntries, baseNode.path)
        }

        // 其他类型（Scalar/Null/TaggedNode）→ 本地节点直接替换基础节点
        return localNode
    }

    /**
     * 合并sections列表：按ID匹配更新，保留基础列表的其他项（功能管理/功能说明）
     */
    private fun mergeSectionsSequence(baseSeq: YamlList, localSeq: YamlList): YamlList {
        android.util.Log.d("Pvz2Config", "✅ 触发sections合并逻辑")
        android.util.Log.d("Pvz2Config", "基础sections数量：${baseSeq.items.size}")
        android.util.Log.d("Pvz2Config", "本地sections数量：${localSeq.items.size}")

        // 1. 基础sections转Map
        val baseSectionMap = mutableMapOf<String, YamlNode>()
        // 【关键修复】新增：专门保存基础配置原本就有的ID列表（用于后续判断是否为新增）
        val originalBaseIds = mutableListOf<String>()

        baseSeq.items.forEach { baseItemNode ->
            if (baseItemNode is YamlMap) {
                val idNode = baseItemNode.getScalar("id")
                idNode?.content?.let { sectionId ->
                    baseSectionMap[sectionId] = baseItemNode
                    originalBaseIds.add(sectionId) // 记录原始ID
                    android.util.Log.d("Pvz2Config", "基础sections包含ID：$sectionId")
                }
            }
        }

        // 2. 用本地sections更新Map
        localSeq.items.forEach { localItemNode ->
            if (localItemNode is YamlMap) {
                val idNode = localItemNode.getScalar("id")
                idNode?.content?.let { sectionId ->
                    android.util.Log.d("Pvz2Config", "本地sections处理ID：$sectionId")
                    if (baseSectionMap.containsKey(sectionId)) {
                        baseSectionMap[sectionId] = mergeSingleSection(
                            baseSectionMap[sectionId] as YamlMap,
                            localItemNode
                        )
                    } else {
                        baseSectionMap[sectionId] = localItemNode
                    }
                }
            }
        }

        // 3. 还原为列表
        val mergedItems = mutableListOf<YamlNode>()

        // 第一步：先按基础原有顺序添加
        originalBaseIds.forEach { baseId ->
            mergedItems.add(baseSectionMap[baseId]!!)
            android.util.Log.d("Pvz2Config", "保留基础栏目：$baseId")
        }

        // 第二步：添加本地新增项
        // 【关键修复】判断依据改为 !originalBaseIds.contains(id)
        localSeq.items.forEach { localItem ->
            val id = (localItem as? YamlMap)?.getScalar("id")?.content
            if (id != null && !originalBaseIds.contains(id)) {
                // 双重检查避免重复添加
                if (!mergedItems.any { (it as? YamlMap)?.getScalar("id")?.content == id }) {
                    mergedItems.add(localItem)
                    android.util.Log.d("Pvz2Config", "新增本地栏目：$id")
                }
            }
        }

        android.util.Log.d("Pvz2Config", "合并后sections总数：${mergedItems.size}")
        return YamlList(mergedItems, baseSeq.path)
    }

    /**
     * 合并单个section：核心处理addItems标识，控制items是追加（true）还是替换（false）
     */
    private fun mergeSingleSection(baseSection: YamlMap, localSection: YamlMap): YamlMap {
        val baseEntries = baseSection.entries.entries.associateBy { it.key.content }
        val localEntries = localSection.entries.entries.associateBy { it.key.content }
        val mergedEntries = mutableMapOf<YamlScalar, YamlNode>()

        // 读取addItems标识（默认true：追加模式）
        val addItems = runCatching {
            localSection.getScalar("addItems")?.toBoolean()
        }.getOrNull() ?: false

        android.util.Log.d("Pvz2Config", "addItems标识：$addItems")

        // 遍历基础section的所有字段
        baseEntries.forEach { (stringKey, entry) ->
            val (baseScalarKey, baseValueNode) = entry
            val localEntry = localEntries[stringKey]

            if (stringKey == "items" && localEntry != null) {
                mergedEntries[baseScalarKey] = if (addItems) {
                    // ========== 追加模式（默认） ==========
                    // 合并基础items + 本地items（保留原有存档，新增本地存档）
                    mergeItemsList(baseValueNode as YamlList, localEntry.value as YamlList)
                } else {
                    // ========== 替换模式 ==========
                    // 本地items直接替换基础items（清空原有存档，只保留本地）
                    localEntry.value
                }
            } else {
                // 其他字段：常规合并（本地覆盖基础，无则保留）
                mergedEntries[baseScalarKey] = localEntry?.let {
                    mergeYamlNodes(baseValueNode, it.value)
                } ?: baseValueNode
            }
        }

        // 添加本地section独有的字段（如addItems）
        localEntries.forEach { (stringKey, entry) ->
            if (!baseEntries.containsKey(stringKey)) {
                mergedEntries[entry.key] = entry.value
            }
        }

        return YamlMap(mergedEntries, baseSection.path)
    }

    /**
     * 专门合并items列表（追加模式）：保留基础items + 新增本地items（去重）
     */
    private fun mergeItemsList(baseItems: YamlList, localItems: YamlList): YamlList {
        // 1. 基础items转Map：id → YamlNode（去重，方便按ID判断）
        val baseItemsMap = mutableMapOf<String, YamlNode>()
        baseItems.items.forEach { item ->
            if (item is YamlMap) {
                val id = item.getScalar("id")?.content
                id?.let {
                    baseItemsMap[it] = item
                    android.util.Log.d("Pvz2Config", "基础存档item：$id") // 新增日志：打印基础存档ID
                }
            }
        }

        // 2. 新增/覆盖本地items（同ID覆盖，不同ID新增）
        localItems.items.forEach { item ->
            if (item is YamlMap) {
                val id = item.getScalar("id")?.content
                id?.let {
                    baseItemsMap[it] = item
                    android.util.Log.d("Pvz2Config", "本地存档item（追加/覆盖）：$id")
                }
            }
        }

        // 3. 转回YamlList（先保留基础原有顺序，再追加本地独有的ID）
        val mergedItems = mutableListOf<YamlNode>()

        // 第一步：按基础items的原有顺序添加（保留基础存档的顺序）
        baseItems.items.forEach { item ->
            val id = (item as? YamlMap)?.getScalar("id")?.content
            id?.let {
                mergedItems.add(baseItemsMap[it]!!)
            }
        }

        // 第二步：添加本地items中「基础没有的新ID」（解决新增存档不显示的核心）
        localItems.items.forEach { item ->
            val id = (item as? YamlMap)?.getScalar("id")?.content
            if (id != null && !baseItems.items.any { baseItem ->
                    (baseItem as? YamlMap)?.getScalar("id")?.content == id
                }) {
                // 只有基础中没有这个ID，才追加到列表末尾
                mergedItems.add(item)
                android.util.Log.d("Pvz2Config", "新增本地存档item到列表：$id")
            }
        }

        android.util.Log.d("Pvz2Config", "合并后存档items总数：${mergedItems.size}")
        return YamlList(mergedItems, baseItems.path)
    }

    // ======================== 原有工具逻辑 ========================
    val font by lazy {
        val typeface = Typeface.createFromAsset(context.assets, "${Pvz2ToolConfig.PATH_NAME}/pvz2font.ttf")
        FontFamily(typeface)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    var mPvz2ScreenState: Pvz2ScreenState
        get() {
            return json.decodeFromString(settings["pvz2toolScreenState", ""].ifEmpty {
                return Pvz2ScreenState()
            })
        }
        set(value) {
            settings["pvz2toolScreenState"] = json.encodeToString(value)
        }

    var mSfmVersion: Version
        get() = settings["pvz2toolSfmVersion", "0.0.0"].toVersion()
        set(value) {
            settings["pvz2toolSfmVersion"] = value.toString()
        }

    var mLocalConfigDirUri: Uri?
        get() {
            return settings["mLocalConfigDirUri", ""].ifEmpty { null }?.toUri()
        }
        set(value) {
            settings["mLocalConfigDirUri"] = value?.toString() ?: ""
        }

    var isBgMusicOn by mutableStateOf(false)

    fun initBgMusicOn() {
        if (::config.isInitialized)
            isBgMusicOn = settings["isBgMusicOn",config.ui.assets.isPlayBackgroundMusic]
    }

    fun saveBgMusicOn(state: Boolean) {
        isBgMusicOn = state
        settings["isBgMusicOn"] = state
    }

    // 初始化入口
    fun init(context: Context = ContextUtil.getCurrentActivity() ?: ContextUtil.context) {
        this.context = context
        runCatching {
            initConfig()
            initBgMusicOn()
        }.onFailure { e -> errorScreenState = Pvz2ErrorScreenState("初始化配置失败",e) }
    }

    fun hasVersionChanges() = versionName != mSfmVersion
}