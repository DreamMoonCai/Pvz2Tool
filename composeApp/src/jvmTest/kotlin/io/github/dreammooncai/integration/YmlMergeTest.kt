package io.github.dreammooncai.integration

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证「目标 yml 深度合并到内置模板之上」的核心语义：
 * 目标已有字段用目标的；目标缺失字段用内置模板默认补齐；嵌套对象递归合并。
 * 复刻 ToolboxIntegratorScreen 中 deepMergeYaml / mergeDreamYml 的算法，针对 kaml 0.104 API 做端到端验证。
 */
private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false, encodeDefaults = false, multiLineStringStyle = MultiLineStringStyle.Literal))

@Serializable
private data class SampleConfig(
    val gameActivity: String,
    val smfDirectory: String = "files/",
    val versionsTheme: String = "BROWN",
    val ui: SampleUi = SampleUi(),
    val newFeatureFlag: Boolean = false,
    val newFeatureText: String = "default-from-template",
)

@Serializable
private data class SampleUi(
    val enterGame: String = "进入游戏",
    val greeting: String = "欢迎",
)

private fun deepMergeYaml(base: YamlNode, override: YamlNode): YamlNode {
    if (base is YamlMap && override is YamlMap) {
        val merged = LinkedHashMap<String, YamlNode>()
        val keyScalar = LinkedHashMap<String, YamlScalar>()
        for ((k, v) in base.entries) {
            merged[k.content] = v
            keyScalar[k.content] = k
        }
        for ((k, v) in override.entries) {
            val key = k.content
            val existing = merged[key]
            merged[key] = if (existing != null) deepMergeYaml(existing, v) else v
            keyScalar[key] = k
        }
        val result = LinkedHashMap<YamlScalar, YamlNode>()
        for ((key, node) in merged) result[keyScalar[key]!!] = node
        return YamlMap(result, base.path)
    }
    return override
}

private fun mergeYml(baseRaw: String, overrideRaw: String): SampleConfig {
    val baseNode = yaml.parseToYamlNode(baseRaw)
    val overrideNode = yaml.parseToYamlNode(overrideRaw)
    val merged = deepMergeYaml(baseNode, overrideNode)
    return yaml.decodeFromYamlNode(SampleConfig.serializer(), merged)
}

class YmlMergeTest {

    @Test
    fun `缺失字段取模板默认，已有字段取目标值`() {
        // 内置模板（base）：含全部字段，包括新版新增的 newFeature*
        val base = """
            gameActivity: com.popcap.pvz2. MainActivity
            smfDirectory: files/
            versionsTheme: GREEN
            ui:
              enterGame: 进入游戏
              greeting: 欢迎光临
            newFeatureFlag: true
            newFeatureText: template-default-text
        """.trimIndent()

        // 目标旧 yml（override）：缺 newFeature*（旧版没有这两个字段），改了 smfDirectory 和 ui.greeting
        val target = """
            gameActivity: com.popcap.pvz2. MainActivity
            smfDirectory: myfiles/
            ui:
              greeting: 老用户专属欢迎
        """.trimIndent()

        val merged = mergeYml(base, target)

        // 目标已有的字段 → 用目标的
        assertEquals("myfiles/", merged.smfDirectory)
        assertEquals("老用户专属欢迎", merged.ui.greeting)
        // 目标缺失的字段 → 用内置模板默认补齐
        assertEquals(true, merged.newFeatureFlag)
        assertEquals("template-default-text", merged.newFeatureText)
        // 嵌套对象里目标没改的字段 → 用模板默认
        assertEquals("进入游戏", merged.ui.enterGame)
        // 目标没动、模板改过的顶层字段 → 用模板
        assertEquals("GREEN", merged.versionsTheme)
    }

    @Test
    fun `目标仅含极少字段时其余全部取模板默认`() {
        val base = """
            gameActivity: com.popcap.pvz2. MainActivity
            newFeatureText: template-default-text
        """.trimIndent()
        // 目标只有一个字段，其余（含新增字段）都应来自模板
        val target = "gameActivity: com.popcap.pvz2. MainActivity"
        val merged = mergeYml(base, target)
        assertEquals("template-default-text", merged.newFeatureText)
        assertEquals("files/", merged.smfDirectory) // 模板缺省默认值
        assertEquals("BROWN", merged.versionsTheme) // 模板缺省默认值
    }
}
