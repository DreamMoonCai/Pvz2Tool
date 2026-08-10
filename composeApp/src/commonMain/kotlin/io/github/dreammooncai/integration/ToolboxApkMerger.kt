package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.archive.FileInputSource
import com.reandroid.archive.RenamedInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.xml.kxml2.KXmlParser
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern

/**
 * 工具箱 APK 集成引擎（纯二进制手术，无需 aapt2）。
 *
 * 关键前提：本应用自身 APK 的资源包 id 为 0x66（build.gradle 中 --package-id 0x66），
 * 目标游戏 APK 通常是 0x7F，二者零冲突，因此 arsc 合并只需把 0x66 包整体搬移 +
 * 全局字符串池合并（TableBlock.merge 已正确处理），资源引用天然无碰撞。
 *
 * 流水线（对应 README「内置启动器适配流程」）：
 *  1. assets/pvz2tool、kotlin、org、META-INF、根文件 —— 整目录/逐文件合并（dream.yml 走 append-only 差异）
 *  2. arsc —— 删除目标原 0x66 包，加入当前版本最新 0x66 包
 *  3. dex —— 追加到目标 / 或插入到目标所有 dex 之前（可选）
 *  4. AndroidManifest.xml —— 删游戏原 LAUNCHER、追加启动器组件块、targetSdkVersion≥21
 *  5. res —— 覆盖替换（同名覆盖、新增添加，保留游戏自有 res）
 *  6. lib —— 按 ABI 对应合并（armeabi 与 armeabi-v7a 不可同时保留）
 *
 * 目标形态：仅 APK。最终产出未签名 APK，由 MT 管理器签名安装。
 */
object ToolboxApkMerger {

    private const val OUR_PKG_ID = 0x66
    private const val OUR_PKG_NAME = "io.github.dreammooncai.pvz2tool"

    // manifest 重解析时遇到 ARSCLib 内嵌框架表不认识的属性名（如 API24+ 的 directBootAware），
    // 用「剥离-重解析」循环处理；这里限制最大轮次，避免极端情况死循环。
    private const val MAX_UNKNOWN_ATTR_PASSES = 64
    private val UNKNOWN_ATTR_RE = Regex("Unknown attribute name '([^']+)'")

    enum class DexStrategy { INSERT_BEFORE, APPEND }

    /** 选择目标 APK 后的检测结果 */
    data class TargetDetection(
        val gameActivity: String,
        /** 目标 APK 是否自带 kotlin（用于决定 dex 合并默认策略） */
        val hasKotlin: Boolean
    )

    /**
     * 集成描述文件（assets/pvz2tool/integrator_info.txt）解析结果。
     * 每次集成/更新都会在产物 APK 内写入该文件，记录：
     *  - 本次工具箱版本（用于下次更新时识别「旧版工具箱 DEX」所在范围）
     *  - 工具箱 DEX 在最终 APK 中的索引区间 [dexStart, dexEnd]（含端点）
     *  - insertMode：新工具箱 DEX 相对「剩余目标 DEX」的插入位置（before=之前 / after=之后）
     * 下次以「更新模式」集成时，依据该文件删除旧范围并替换，保证可反复迭代更新。
     */
    data class IntegratorInfo(
        val version: String,
        val dexStart: Int,
        val dexEnd: Int,
        val insertMode: String,   // "before" | "after"
        val includeExamples: Boolean = true,
        val simplifiedLaunch: Boolean = false,
        /** 是否把游戏主 Activity 的主题设为工具箱沉浸式主题（@OUR_PKG_NAME:style/Theme.DreamPvzApp） */
        val useImmersiveTheme: Boolean = true
    )

    /** 从目标 APK 读取集成描述文件（无则返回 null）。 */
    fun detectIntegratorInfo(targetApk: File): IntegratorInfo? = runCatching {
        ApkModule.loadApkFile(targetApk).use { readIntegratorInfo(it) }
    }.getOrNull()

    /** 从已加载的 ApkModule 读取集成描述文件（无则返回 null）。 */
    private fun readIntegratorInfo(target: ApkModule): IntegratorInfo? {
        val raw = target.getInputSource("assets/pvz2tool/integrator_info.txt")?.openStream()?.readBytes()
            ?: return null
        val map = raw.toString(Charsets.UTF_8).lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }.toMap()
        val version = map["version"] ?: return null
        val dexStart = map["dexStart"]?.toIntOrNull() ?: return null
        val dexEnd = map["dexEnd"]?.toIntOrNull() ?: return null
        val insertMode = map["insertMode"] ?: "before"
        val includeExamples = map["includeExamples"]?.toBooleanStrictOrNull() ?: true
        val simplifiedLaunch = map["simplifiedLaunch"]?.toBooleanStrictOrNull() ?: false
        val useImmersiveTheme = map["useImmersiveTheme"]?.toBooleanStrictOrNull() ?: false
        return IntegratorInfo(version, dexStart, dexEnd, insertMode, includeExamples, simplifiedLaunch, useImmersiveTheme)
    }

    data class IntegrateReport(
        val sourceLabel: String,
        val targetLabel: String,
        val targetPackage: String,
        val dexStrategy: DexStrategy,
        val sourceDexCount: Int,
        val targetDexCount: Int,
        val resultDexCount: Int,
        val arscTargetPackagesAfter: List<String>,
        val manifestChanges: List<String>,
        val resAdded: Int,
        val resOverwritten: Int,
        val assetsAdded: Int,
        val libAbis: List<String>,
        val dreamYmlAdded: Int,
        val dreamYmlSkipped: Int,
        /** 即将注入的追加文件（APK 内完整路径：assets/pvz2tool/... 或 res/...） */
        val extraFiles: List<String>,
        /** 被用户排除（不打包）的 SMF/资源条目，相对 assets/pvz2tool/ 的路径 */
        val smfExcluded: List<String>,
        /** 被用户从「目标 APK」中删除的原始条目（APK 内完整路径），受保护条目已过滤 */
        val targetRemoved: List<String>,
        val notes: List<String>,
        /**
         * 合并过程中产生的「需用户介入」的告警（与 notes 的中性说明区分，UI 用醒目样式渲染）。
         * 保留为扩展点：当前合并管线不产生任何告警，恒为空列表。
         * （历史来源为 dex 剥离失败提示，该子系统已于 2026-08-09 整体移除。）
         */
        val warnings: List<String> = emptyList()
    )

    /**
     * 目标 APK 内不允许被用户删除的条目：删掉会直接破坏 APK 结构或资源表引用。
     * - manifest / 资源表 / dex：APK 骨架
     * - res/：条目被 resources.arsc 引用，删除会造成引用悬空 → 运行时崩溃
     * - assets/pvz2tool/：工具箱自身资源域，由 SMF「排除」功能单独管理
     */
    fun isProtectedTargetEntry(name: String): Boolean {
        val n = name.trimStart('/')
        return n == "AndroidManifest.xml" ||
            n == "resources.arsc" ||
            Regex("^classes\\d*\\.dex$").matches(n) ||
            n.startsWith("res/") ||
            n.startsWith("META-INF/") ||
            n.startsWith("assets/pvz2tool/")
    }

    data class MergeResult(
        val outputApk: File,
        val report: IntegrateReport
    )

    // ---- 公开入口 ----

    /** 从目标 APK 的 manifest 中检测游戏入口 Activity（全限定名） */
    fun detectGameActivity(targetApk: File): String = ApkModule.loadApkFile(targetApk).use { target ->
        val targetPackage = target.androidManifest.packageName ?: ""
        findGameActivity(target, targetPackage)
    }

    /** 一站式检测：游戏入口 Activity + 是否含 kotlin（决定 dex 合并默认策略） */
    fun detectTarget(targetApk: File): TargetDetection = ApkModule.loadApkFile(targetApk).use { target ->
        val targetPackage = target.androidManifest.packageName ?: ""
        val gameActivity = findGameActivity(target, targetPackage)
        val hasKotlin = target.listInputSources().any { ins ->
            val n = ins.name
            n.startsWith("kotlin/") ||
                    n.startsWith("assets/kotlin/") ||
                    n.endsWith(".kotlin_module")
        }
        TargetDetection(gameActivity, hasKotlin)
    }

    /** 获取目标 APK 的包名 */
    fun detectPackageName(targetApk: File): String = ApkModule.loadApkFile(targetApk).use { it.androidManifest.packageName ?: "" }

    fun preview(
        sourceApk: File,
        targetApk: File,
        dexStrategy: DexStrategy,
        /** 额外资源：APK路径 → 本地文件（同 apply，用于预览即将注入的文件清单） */
        extraResources: Map<String, File> = emptyMap(),
        /** 额外 res 资源：res 下相对路径 → 本地文件（同 apply） */
        extraResResources: Map<String, File> = emptyMap(),
        /** 用户排除（不打包）的 SMF/资源条目，相对 assets/pvz2tool/ 的路径集合 */
        excludedSmfAssets: Set<String> = emptySet(),
        /** 用户勾选「选择后删除」的目标 APK 原始条目（APK 内完整路径），打包时从产物中移除 */
        removedTargetEntries: Set<String> = emptySet(),
        /** 集成描述文件版本号（预览仅用于报告展示） */
        version: String = "?",
        /** 更新模式开关 */
        updateMode: Boolean = false,
        /** 更新模式：旧版工具箱 DEX 起始索引 */
        dexStart: Int = 0,
        /** 更新模式：旧版工具箱 DEX 结束索引（<0 表示覆盖到本版本所有 DEX） */
        dexEnd: Int = -1,
        /** 实际写入的 dream.yml 文本（向导生成的完整配置）；为空则回退为源 APK 的 dream.yml。用于报告差异统计 */
        overrideDreamYml: String? = null,
        /** 更新模式：保留目标 APK 现有 res 条目（源 APK 不覆盖），如 bg_fill_image */
        preserveTargetResEntries: Set<String> = emptySet(),
        /** 保留目标已有 assets/pvz2tool 文件 */
        preserveTargetAssets: Boolean = false,
        /** 附加目标未包含的源文件 */
        appendUnreferenced: Boolean = true,
        /** 是否把游戏主 Activity 的主题设为工具箱沉浸式主题 */
        useImmersiveTheme: Boolean = false
    ): IntegrateReport {
        val source = ApkModule.loadApkFile(sourceApk)
        val target = ApkModule.loadApkFile(targetApk)
        return try {
            computeReport(source, target, dexStrategy, sourceApk.name, targetApk.name, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries,
                updateMode, dexStart, dexEnd, overrideDreamYml = overrideDreamYml,
                preserveTargetAssets = preserveTargetAssets, appendUnreferenced = appendUnreferenced,
                useImmersiveTheme = useImmersiveTheme)
        } finally {
            source.close()
            target.close()
        }
    }

    fun apply(
        sourceApk: File,
        targetApk: File,
        dexStrategy: DexStrategy,
        outApk: File,
        includeExamples: Boolean = true,
        /** 如果提供，直接使用该 YAML 内容替代文本合并（如集成器向导生成的完整配置） */
        overrideDreamYml: String? = null,
        /** 额外资源：APK路径 → 本地文件（集成器文件选择器选中的文件，注入 assets/pvz2tool/） */
        extraResources: Map<String, File> = emptyMap(),
        /** 额外 res 资源：res 下相对路径（如 mipmap-hdpi-v4/bg_fill_image.jpg）→ 本地文件（注入 APK 的 res/ 目录，用于替换编译资源） */
        extraResResources: Map<String, File> = emptyMap(),
        /** 用户排除（不打包）的 SMF/资源条目，相对 assets/pvz2tool/ 的路径集合 */
        excludedSmfAssets: Set<String> = emptySet(),
        /** 用户勾选「选择后删除」的目标 APK 原始条目（APK 内完整路径），打包时从产物中移除 */
        removedTargetEntries: Set<String> = emptySet(),
        /** 集成描述文件版本号（当前工具箱版本，运行时传入 BuildConfig.VERSION_NAME） */
        version: String = "?",
        /** 更新模式开关 */
        updateMode: Boolean = false,
        /** 更新模式：旧版工具箱 DEX 起始索引 */
        dexStart: Int = 0,
        /** 更新模式：旧版工具箱 DEX 结束索引（<0 表示覆盖到本版本所有 DEX） */
        dexEnd: Int = -1,
        /** 更新模式：新工具箱 DEX 插入策略 */
        insertMode: DexStrategy = DexStrategy.INSERT_BEFORE,
        /** 更新模式：保留目标 APK 现有 res 条目（源 APK 不覆盖），如 bg_fill_image */
        preserveTargetResEntries: Set<String> = emptySet(),
        /** 简易模式开关（写入描述文件，供下次更新识别） */
        simplifiedLaunch: Boolean = false,
        preserveTargetAssets: Boolean = false,
        appendUnreferenced: Boolean = true,
        /** 是否把游戏主 Activity 的主题设为工具箱沉浸式主题 */
        useImmersiveTheme: Boolean = false
    ): MergeResult {
        val source = ApkModule.loadApkFile(sourceApk)
        val target = ApkModule.loadApkFile(targetApk)
        val report: IntegrateReport
        source.use { source ->
            report = computeReport(source, target, dexStrategy, sourceApk.name, targetApk.name, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries, overrideDreamYml = overrideDreamYml,
                useImmersiveTheme = useImmersiveTheme)
            doMerge(source, target, dexStrategy, report.targetPackage, includeExamples, overrideDreamYml, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries,
                version, updateMode, dexStart, dexEnd, insertMode, simplifiedLaunch = simplifiedLaunch, preserveTargetAssets = preserveTargetAssets, appendUnreferenced = appendUnreferenced,
                useImmersiveTheme = useImmersiveTheme)
        }
        target.use {
            outApk.parentFile?.mkdirs()
            target.writeApk(outApk)
        }
        return MergeResult(outApk, report)
    }

    // ---- 报告（只读，不写回） ----

    private fun computeReport(
        source: ApkModule,
        target: ApkModule,
        dexStrategy: DexStrategy,
        sourceLabel: String,
        targetLabel: String,
        extraResources: Map<String, File> = emptyMap(),
        extraResResources: Map<String, File> = emptyMap(),
        excludedSmfAssets: Set<String> = emptySet(),
        removedTargetEntries: Set<String> = emptySet(),
        /** 更新模式：报告中的 resultDexCount 需扣除被替换的旧工具箱 DEX 数 */
        updateMode: Boolean = false,
        dexStart: Int = 0,
        dexEnd: Int = -1,
        /** 实际写入的 dream.yml 文本（向导生成的完整配置）；为空则回退为源 APK 的 dream.yml。用于报告差异统计 */
        overrideDreamYml: String? = null,
        preserveTargetAssets: Boolean = false,
        appendUnreferenced: Boolean = true,
        /** 是否把游戏主 Activity 的主题设为工具箱沉浸式主题（仅影响报告说明，实际改动在 doMerge） */
        useImmersiveTheme: Boolean = false
    ): IntegrateReport {
        val srcDex = source.listDexFiles().size
        val tgtDex = target.listDexFiles().size
        val resAdded: Int
        val resOverwritten: Int
        run {
            var add = 0
            var over = 0
            source.listInputSources().filter { it.name.startsWith("res/") }.forEach { ins ->
                if (target.containsFile(ins.name)) over++ else add++
            }
            resAdded = add
            resOverwritten = over
        }
        // SMF/资源排除：相对 assets/pvz2tool/ 的路径
        val smfExcluded = source.listInputSources()
            .filter { it.name.startsWith("assets/pvz2tool/") }
            .map { it.name.removePrefix("assets/pvz2tool/").trimStart('/') }
            .filter { it in excludedSmfAssets }
        // 与 mergeAssetsPvz2tool 同步：首次集成时忽略 appendUnreferenced=false 的限制
        val firstIntegrate = target.listInputSources().none { it.name.startsWith("assets/pvz2tool/") }
        val effectiveAppendUnreferenced = appendUnreferenced || firstIntegrate
        var assetsAdded = 0
        source.listInputSources().filter { it.name.startsWith("assets/pvz2tool/") }.forEach { ins ->
            val rel = ins.name.removePrefix("assets/pvz2tool/").trimStart('/')
            if (rel !in excludedSmfAssets && !target.containsFile(ins.name)) {
                // 与 mergeAssetsPvz2tool 使用相同的跳过逻辑
                if (preserveTargetAssets && target.getInputSource(ins.name) != null) {
                    val always = ins.name.endsWith("/config_documentation.md") || ins.name.endsWith("/js_documentation.md")
                        || ins.name == "assets/pvz2tool/dream.yml" || ins.name == "assets/pvz2tool/integrator_info.txt"
                    if (!always) return@forEach
                }
                if (!effectiveAppendUnreferenced && target.getInputSource(ins.name) == null) return@forEach
                assetsAdded++
            }
        }
        // dream.yml 差异统计：基于「实际写入内容（overrideDreamYml）」vs「目标已有 dream.yml」
        val (dreamAdd, dreamSkip) = run {
            val writeText = overrideDreamYml
                ?: source.getInputSource("assets/pvz2tool/dream.yml")?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
            val targetText = target.getInputSource("assets/pvz2tool/dream.yml")?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
            if (writeText != null) countDreamYmlDiff(writeText, targetText) else 0 to 0
        }
        val targetPkg = target.androidManifest.packageName ?: ""
        val manifestChanges = buildList {
            add("删除游戏原 LAUNCHER intent-filter")
            add("追加启动器组件块（Pvz2InitializeActivity / FileProvider / VPN / 各类 Provider / Receiver / uses-library）")
            val ts = target.androidManifest.targetSdkVersion
            if (ts == null || ts < 21) add("targetSdkVersion 调整至 21（当前 ${ts ?: "未设置"}）")
            else add("targetSdkVersion 保持 $ts（已 ≥21）")
            if (useImmersiveTheme) add("游戏主 Activity 主题设为工具箱沉浸式主题（@$OUR_PKG_NAME:style/Theme.DreamPvzApp）")
        }
        val arscAfter = (target.tableBlock.listPackages() + source.tableBlock.listPackages())
            .distinctBy { it.id }
            .map { "0x%02X:%s".format(it.id, it.name) }
        val libAbis = computeLibAbis(source, target)
        val extraFiles = extraResources.keys.map { "assets/pvz2tool/$it" } +
            extraResResources.keys.map { "res/$it" }
        // 目标 APK 内被用户勾选删除的条目：只统计真实存在且非受保护的
        val targetRemoved = removedTargetEntries
            .filter { !isProtectedTargetEntry(it) && target.containsFile(it) }
            .sorted()
        return IntegrateReport(
            sourceLabel = sourceLabel,
            targetLabel = targetLabel,
            targetPackage = targetPkg,
            dexStrategy = dexStrategy,
            sourceDexCount = srcDex,
            targetDexCount = tgtDex,
            resultDexCount = if (updateMode) {
                val removed = if (dexEnd >= dexStart) (dexEnd - dexStart + 1) else 0
                (tgtDex - removed + srcDex).coerceAtLeast(0)
            } else srcDex + tgtDex,
            arscTargetPackagesAfter = arscAfter,
            manifestChanges = manifestChanges,
            resAdded = resAdded,
            resOverwritten = resOverwritten,
            assetsAdded = assetsAdded,
            libAbis = libAbis,
            dreamYmlAdded = dreamAdd,
            dreamYmlSkipped = dreamSkip,
            extraFiles = extraFiles,
            smfExcluded = smfExcluded,
            targetRemoved = targetRemoved,
            notes = buildList {
                add("合并策略：dex=${dexStrategy.name}，arsc 搬移 0x66 包，manifest/targetSdk/资源/lib 全量合并。")
                if (targetRemoved.isNotEmpty()) add("将从目标 APK 中删除 ${targetRemoved.size} 个原始条目（「选择后删除」）。")
                add("产物为未签名 APK，请用 MT 管理器签名后安装。")
            }
        )
    }

    // ---- dream.yml 轻量差异统计（仅 key 级集合差，不做文本写回） ----

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

    /** 在 [headerLine] 指向的列表（如 sections:/versions:/items:）中按 id 找一个 list item。 */
    private fun findListItem(lines: List<String>, headerLine: Int, itemId: String, itemIndent: Int): IntRange? {
        if (headerLine < 0) return null
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
        if (headerLine < 0) return emptyList()
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

    /** 在 header 指向的 block 内查找某个子 key（如 items:）的行号。 */
    private fun subHeader(lines: List<String>, parentRange: IntRange, key: String, indent: Int): Int? {
        for (i in parentRange) {
            if (lines[i].trim().startsWith("$key:") && lineIndent(lines[i]) == indent) return i
        }
        return null
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
     * 统计实际写入的 dream.yml（writeText）相对目标已有 dream.yml（targetText）的差异条目数。
     * 仅做 key 级集合差（sections/versions 顶层 id，及 section 内 items 的 id，及其余顶层 key），
     * 不做文本写回。返回 Pair(ADD 数, SKIP 数)：
     * - ADD = 目标没有的新增栏目/版本/项/顶层配置（将被追加或覆盖式写入）
     * - SKIP = 目标已存在（将被覆盖式保留，不删除）
     */
    private fun countDreamYmlDiff(writeText: String, targetText: String?): Pair<Int, Int> {
        val w = writeText.split("\n")
        val t = targetText?.split("\n") ?: emptyList()
        var add = 0
        var skip = 0

        // ---------- versions ----------
        val wVer = idsOfList(w, findTopKey(w, "versions"), 2)
        val tVer = if (t.isNotEmpty()) idsOfList(t, findTopKey(t, "versions"), 2) else emptyList()
        add += (wVer - tVer).size
        skip += (wVer intersect tVer).size

        // ---------- sections + 其内部 items ----------
        val wSecHeader = findTopKey(w, "sections")
        if (wSecHeader >= 0) {
            val wSecs = idsOfList(w, wSecHeader, 2)
            val tSecHeader = if (t.isNotEmpty()) findTopKey(t, "sections") else -1
            for (sid in wSecs) {
                val wRange = findListItem(w, wSecHeader, sid, 2) ?: continue
                val tRange = if (tSecHeader >= 0) findListItem(t, tSecHeader, sid, 2) else null
                if (tRange == null) {
                    add++ // 整个 section 新增
                    val wItemsHeader = subHeader(w, wRange, "items", 4)
                    if (wItemsHeader != null) add += idsOfList(w, wItemsHeader, 6).size
                } else {
                    skip++ // section 已存在（覆盖式保留）
                    val wItemsHeader = subHeader(w, wRange, "items", 4)
                    val tItemsHeader = subHeader(t, tRange, "items", 4)
                    if (wItemsHeader != null && tItemsHeader != null) {
                        val wItems = idsOfList(w, wItemsHeader, 6)
                        val tItems = idsOfList(t, tItemsHeader, 6)
                        add += (wItems - tItems).size
                        skip += (wItems intersect tItems).size
                    }
                }
            }
        }

        // ---------- 其余顶层 key（gameActivity/smfDirectory 等） ----------
        val ignore = setOf("sections", "versions")
        val wTop = topKeys(w) - ignore
        val tTop = if (t.isNotEmpty()) topKeys(t) - ignore else emptySet()
        add += (wTop - tTop).size
        skip += (wTop intersect tTop).size

        return add to skip
    }

    // ---- 实际合并（修改 target 模块） ----

    private fun doMerge(source: ApkModule, target: ApkModule, dexStrategy: DexStrategy, targetPackage: String, includeExamples: Boolean, overrideDreamYml: String?, extraResources: Map<String, File>, extraResResources: Map<String, File> = emptyMap(), excludedSmfAssets: Set<String> = emptySet(), removedTargetEntries: Set<String> = emptySet(),
                  /** 集成描述文件写入用：当前工具箱版本（运行时传入 BuildConfig.VERSION_NAME） */
                  version: String = "?",
                  /** 更新模式：删除目标 APK 中旧版工具箱 DEX 区间并替换，而非普通插入 */
                  updateMode: Boolean = false,
                  /** 更新模式：旧版工具箱 DEX 起始索引（含） */
                  dexStart: Int = 0,
                  /** 更新模式：旧版工具箱 DEX 结束索引（含）；<0 表示覆盖到本版本所有 DEX */
                  dexEnd: Int = -1,
                  /** 更新模式：新工具箱 DEX 插入到剩余目标 DEX 之前/之后 */
                  insertMode: DexStrategy = DexStrategy.INSERT_BEFORE,
                  /** 更新模式：保留目标 APK 现有 res 条目（源 APK 不覆盖），如 bg_fill_image。键为 res/ 下的完整路径 */
                  preserveTargetResEntries: Set<String> = emptySet(),
                  /** 简易模式开关（写入描述文件，供下次更新识别） */
                  simplifiedLaunch: Boolean = false,
                  preserveTargetAssets: Boolean = false,
                  appendUnreferenced: Boolean = true,
                  /** 是否把游戏主 Activity 的主题设为工具箱沉浸式主题 */
                  useImmersiveTheme: Boolean = false) {
        // 1. 「选择后删除」：先移除目标 APK 中被用户勾选的原始条目。
        //    放在所有注入之前 —— 若某条目既被勾选删除又被重新注入，则以注入为准（用户明确选择的资源保留）。
        //    受保护条目（manifest/arsc/dex/res/META-INF/assets/pvz2tool）在此静默跳过，UI 侧也已拦截。
        removedTargetEntries.forEach { name ->
            if (!isProtectedTargetEntry(name)) target.removeInputSource(name)
        }

        // 2. arsc：移除目标原 0x66 包，加入源 0x66 包（全局字符串池一并合并）
        val tTable: TableBlock = target.tableBlock
        tTable.listPackages().firstOrNull { it.id == OUR_PKG_ID }?.let { tTable.removePackage(it) }
        tTable.merge(source.tableBlock)

        // 4. manifest
        val tm: AndroidManifestBlock = target.androidManifest
        removeGameLauncher(tm)
        // 更新模式：先清除旧版工具箱的 manifest 条目（避免重复/冲突）
        if (updateMode) removePreviousToolboxEntries(tm, targetPackage)
        appendLauncherBlock(tm, source, target, targetPackage)
        // 可选：把游戏主 Activity 的主题设为工具箱沉浸式主题（需在 appendLauncherBlock 之后，
        // 此时 0x66 资源表已 merge 且 addExternalFramework 已登记，@package:style 引用才能正确编码）。
        if (useImmersiveTheme) applyGameActivityTheme(tm, target, targetPackage)
        // appendLauncherBlock 在原地修改 tm（目标原本的 manifest 对象，根元素 <manifest> 不变），
        // 因此 targetSdkVersion 直接作用在 tm 上；最后 setManifest 固化（覆盖 getter 可能返回副本的情况）。
        val ts = tm.targetSdkVersion
        if (ts == null || ts < 21) tm.targetSdkVersion = 21
        target.setManifest(tm)

        // 3. dex
        val toolboxRange: Pair<Int, Int> = if (updateMode) {
            // 更新模式：删除旧版工具箱 DEX 区间；结束索引缺省时覆盖到本版本所有 DEX
            val end = if (dexEnd < dexStart) dexStart + source.listDexFiles().size - 1 else dexEnd
            replaceToolboxDex(source, target, dexStart, end, insertMode)
        } else {
            renumberDex(source, target, dexStrategy)
        }

        // 5. res 覆盖替换
        // 更新模式：res 全部覆盖替换为新版工具箱资源（含游戏自有资源），仅保留用户自定义背景图 bg_fill_image；
        // 用户若通过向导显式重选背景图，则由下方 extraResResources 注入覆盖。（首次集成则按 preserveTargetResEntries 处理。）
        val resSkip = (if (updateMode) {
            val userRes = extraResResources.keys.map { "res/$it" }.toSet()
            source.listInputSources().filter { ins ->
                ins.name.startsWith("res/") && ins.name.contains("bg_fill_image") && ins.name !in userRes
            }.map { it.name }.toSet()
        } else preserveTargetResEntries).toMutableSet()
        // 始终排除工具箱自身的图标资源（ic_launcher），避免覆盖目标 APK 的桌面图标
        source.listInputSources().filter {
            it.name.startsWith("res/") && it.name.contains("ic_launcher")
        }.forEach { resSkip.add(it.name) }
        copyDir(source, target, "res/", skip = resSkip)

        // 1. 其余资源合并
        copyDir(source, target, "kotlin/")
        copyDir(source, target, "org/")
        copyDir(source, target, "META-INF/")
        copyFile(source, target, "DebugProbesKt.bin")
        copyFile(source, target, "kotlin-tooling-metadata.json")
        mergeAssetsPvz2tool(source, target, targetPackage, includeExamples, overrideDreamYml, excludedSmfAssets, preserveTargetAssets, appendUnreferenced)

        // 注入向导中选择的额外文件到 APK
        // 关键：用 FileInputSource 从磁盘按需分块读，避免 readBytes() 把整文件读进内存（大体积 smf 会直接 OOM）
        for ((apkPath, localFile) in extraResources) {
            val fullPath = "assets/pvz2tool/$apkPath"
            target.removeInputSource(fullPath)
            target.add(FileInputSource(localFile, fullPath))
        }

        // 注入向导中选择的额外 res 资源（如 @mipmap/bg_fill_image）到 APK 的 res/ 目录
        // 必须在 copyDir(source, target, "res/") 之后执行，以覆盖工具自带资源
        for ((apkPath, localFile) in extraResResources) {
            val fullPath = "res/$apkPath"
            target.removeInputSource(fullPath)
            target.add(FileInputSource(localFile, fullPath))
        }

        // 6. lib 按 ABI 合并
        mergeLib(source, target)

        // 7. 写入集成描述文件（记录版本 + 工具箱 DEX 所在范围，供下次更新模式识别旧区间）
        val infoMode = if (updateMode) insertMode else dexStrategy
        val infoInsert = if (infoMode == DexStrategy.INSERT_BEFORE) "before" else "after"
        val infoText = buildString {
            appendLine("version=$version")
            appendLine("dexStart=${toolboxRange.first}")
            appendLine("dexEnd=${toolboxRange.second}")
            appendLine("insertMode=$infoInsert")
            appendLine("includeExamples=$includeExamples")
            appendLine("simplifiedLaunch=$simplifiedLaunch")
            appendLine("useImmersiveTheme=$useImmersiveTheme")
        }
        target.removeInputSource("assets/pvz2tool/integrator_info.txt")
        target.add(ByteInputSource(infoText.toByteArray(Charsets.UTF_8), "assets/pvz2tool/integrator_info.txt"))
    }

    // ---- arsc / 包 ----

    // ---- dex ----

    /**
     * 普通（首次集成）DEX 合并：把工具箱 DEX 按策略插入/追加到目标。
     * 返回工具箱 DEX 在最终 APK 中的索引区间 [start, end]（含端点），用于写入描述文件。
     */
    private fun renumberDex(
        source: ApkModule,
        target: ApkModule,
        dexStrategy: DexStrategy
    ): Pair<Int, Int> {
        val srcDex = source.listDexFiles().sortedBy { dexFileIndex(it.name) }
        val tgtDex = target.listDexFiles().sortedBy { dexFileIndex(it.name) }
        val ordered = if (dexStrategy == DexStrategy.INSERT_BEFORE) srcDex + tgtDex else tgtDex + srcDex
        target.listDexFiles().forEach { target.removeInputSource(it.name) }

        ordered.forEachIndexed { idx, dex ->
            target.add(RenamedInputSource(dexName(idx), dex))
        }
        // 工具箱 DEX 在最终 APK 中的索引区间 [start, end]（含端点），用于写入描述文件。
        val start = if (dexStrategy == DexStrategy.INSERT_BEFORE) 0 else tgtDex.size
        val end = (start + srcDex.size - 1).coerceAtLeast(start)
        return start to end
    }

    /**
     * 更新模式 DEX 合并：删除目标 APK 中旧版工具箱 DEX 所在区间 [dexStart, dexEnd]，
     * 将剩余目标 DEX 顺序重排后，把新工具箱 DEX 按 insertMode 插入到其之前或之后。
     * 返回新工具箱 DEX 在最终 APK 中的索引区间 [start, end]（含端点），用于更新描述文件。
     */
    private fun replaceToolboxDex(
        source: ApkModule,
        target: ApkModule,
        dexStart: Int,
        dexEnd: Int,
        insertMode: DexStrategy
    ): Pair<Int, Int> {
        val srcDex = source.listDexFiles().sortedBy { dexFileIndex(it.name) }
        val tgtDex = target.listDexFiles().sortedBy { dexFileIndex(it.name) }
        val count = tgtDex.size
        if (count == 0) {
            return renumberDex(source, target, insertMode)
        }
        val start = dexStart.coerceIn(0, count - 1)
        val end = dexEnd.coerceIn(start, count - 1)
        // 1. 删除旧版工具箱 DEX 区间
        tgtDex.filterIndexed { i, _ -> i in start..end }.forEach { target.removeInputSource(it.name) }
        // 2. 顺序重排剩余目标 DEX
        val remaining = target.listDexFiles().sortedBy { dexFileIndex(it.name) }
        val ordered = if (insertMode == DexStrategy.INSERT_BEFORE) srcDex + remaining else remaining + srcDex
        target.listDexFiles().forEach { target.removeInputSource(it.name) }
        // 3. 重新编号写入
        ordered.forEachIndexed { idx, dex ->
            target.add(RenamedInputSource(dexName(idx), dex))
        }
        // 4. 新工具箱 DEX 区间
        val srcCount = srcDex.size
        val remainingCount = remaining.size
        val newStart = if (insertMode == DexStrategy.INSERT_BEFORE) 0 else remainingCount
        val newEnd = (newStart + srcCount - 1).coerceAtLeast(newStart)
        return newStart to newEnd
    }


    private fun dexName(index: Int): String =
        if (index == 0) "classes.dex" else "classes${index + 1}.dex"

    /** DEX 文件按数字序号排序（而非字典序，否则 classes10.dex 会排在 classes2.dex 前面）。
     *  classes.dex -> 1, classes2.dex -> 2, classes10.dex -> 10；无法解析的排到最后。 */
    private fun dexFileIndex(name: String): Int {
        val num = name.removePrefix("classes").removeSuffix(".dex")
        if (num.isEmpty()) return 1 // classes.dex（无数字后缀）排在最前
        return num.toIntOrNull() ?: Int.MAX_VALUE
    }

    // ---- manifest ----

    /** 更新模式：移除目标 APK 中旧版工具箱已注入的 manifest 条目，避免 appendLauncherBlock 追加时重复。 */
    private fun removePreviousToolboxEntries(tm: AndroidManifestBlock, targetPackage: String) {
        val toRemove = mutableListOf<ResXmlElement>()
        val toolboxClasses = setOf(
            "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity",
            "io.github.dreammooncai.pvz2tool.service.LocalVpnService",
            "io.github.dreammooncai.pvz2tool.timer.TimerService",
            "io.github.dreammooncai.pvz2tool.timer.TimerReceiver",
            "io.github.dreammooncai.pvz2tool.RestartPhoenixActivity",
            "com.petterp.floatingx.assist.FxContentProvider",
        )
        val app = tm.applicationElement ?: return

        // 移除旧版 Activity
        tm.getActivities(true).forEach { act ->
            val name = act.attr("name") ?: ""
            if (name == "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity"
                || name == "io.github.dreammooncai.pvz2tool.RestartPhoenixActivity") toRemove.add(act)
        }

        // 移除旧版 Service / Receiver / Provider（均在 <application> 下）
        for (tag in listOf("service", "receiver", "provider")) {
            tm.listApplicationElementsByTag(tag).forEach { el ->
                val name = el.attr("name") ?: ""
                if (name in toolboxClasses) {
                    toRemove.add(el)
                } else if (tag == "provider" && name == "androidx.core.content.FileProvider") {
                    val authority = el.attr("authorities") ?: ""
                    if (authority.startsWith(targetPackage)) toRemove.add(el)
                }
            }
        }

        // 移除 uses-library（引用 OUR_PKG_NAME）
        app.getElements().forEach { child ->
            if (child.name == "uses-library") {
                val libName = child.attr("name") ?: ""
                if (libName.startsWith(OUR_PKG_NAME)) toRemove.add(child)
            }
        }

        toRemove.forEach { it.removeSelf() }
    }

    private fun removeGameLauncher(tm: AndroidManifestBlock) {
        val toRemove = mutableListOf<ResXmlElement>()
        tm.getActivities(true).forEach { act ->
            act.getElements().forEach { child ->
                if (child.name == "intent-filter" && child.hasMainLauncher()) {
                    toRemove.add(child)
                }
            }
        }
        toRemove.forEach { it.removeSelf() }
    }

    private fun ResXmlElement.hasMainLauncher(): Boolean {
        var hasMain = false
        var hasLauncher = false
        getElements().forEach { e ->
            when (e.name) {
                "action" -> if (e.attr("name") == "android.intent.action.MAIN") hasMain = true
                "category" -> if (e.attr("name") == "android.intent.category.LAUNCHER") hasLauncher = true
            }
        }
        return hasMain && hasLauncher
    }

    private fun appendLauncherBlock(tm: AndroidManifestBlock, source: ApkModule, target: ApkModule, targetPackage: String) {
        // 关键修复：不再用 AndroidManifestBlock.empty() + 整体重解析。
        // 旧做法会生成「多余的 <x> 根元素」把真正的 <manifest> 包成 <x><manifest>...</manifest></x>，
        // 导致 MT 管理器反编译只读到根 <x> 渲染成 <x />、内容全丢（aapt 能勉强解故不易察觉）。
        // 正确做法：直接在【目标原本的】 <application> 元素上原地 parse 追加我们的组件块，
        // 根元素 <manifest> 始终是目标原本正确的那一个，字符串池/命名空间也原样保留。
        // 注入块里的资源引用用「%PKG66%」占位，运行时替换为我们的真实包名（@io.github.dreammooncai.pvz2tool:xml/file_paths）。
        // ARSCLib 文本解析阶段只从「框架包」解析具名资源引用（自身主表不查），所以必须把我们的 0x66 资源表
        // 登记为 target 的外部框架，解析时按全限定包名编码成正确 id；外部框架不会作为普通条目写进产物 APK。
        // 兜底：ARSCLib 1.4.0 内嵌框架属性表不含 API24+ 的属性（如 android:directBootAware），
        // 解析期遇到未知属性名会抛 IOException("Unknown attribute name 'X'")，用「剥离-重解析」循环处理。
        target.addExternalFramework(source.tableBlock)
        val block = LAUNCHER_BLOCK_XML
            .replace("%PKG66%", OUR_PKG_NAME)
            .replace("%PKG%", targetPackage)
        // 用 <application> 包裹组件，parse 进现有 application 元素（parse 会把内层组件追加为其子节点）。
        // 必须显式声明 android 命名空间，否则 KXmlParser 解析 android: 前缀属性会抛 XmlPullParserException（未定义前缀）。
        var injected = "<application xmlns:android=\"http://schemas.android.com/apk/res/android\">\n$block\n</application>"
        val app = tm.getOrCreateApplicationElement()

        var lastError: IOException? = null
        repeat(MAX_UNKNOWN_ATTR_PASSES) {
            // ResXmlElement.parse 非原子：parseInnerNodes 里 createForEvent 先 newElement()→add() 空元素到 this，
            // 再 node.parse(parser) 填充；若某子元素属性解析抛 IOException（如 directBootAware），
            // 已 add 的前序组件不会回滚。剥离-重解析循环会反复 add 前序组件 → 重复 + 后续丢失。
            // 故每轮 parse 前快照现有子节点，失败时删除新增（含失败点的空壳），保证每轮从干净状态开始。
            val snapshot = snapshotElements(app)
            val parser = KXmlParser()
            parser.setInput(StringReader(injected))
            try {
                // ResXmlElement.parse 要求解析器已停在待解析元素的 START_TAG（setInput 后停在 START_DOCUMENT），
                // 故先 nextTag() 推进到 <application> 起始标签，parse 再消费它并追加内层组件为子节点。
                parser.nextTag()
                app.parse(parser)
                return
            } catch (e: IOException) {
                // 回滚本轮 parse 已追加的子节点（含失败点的空壳元素）
                rollbackNewElements(app, snapshot)
                val attr = UNKNOWN_ATTR_RE.find(e.message ?: "")?.groupValues?.get(1)
                if (attr == null) throw e // 非「未知属性名」错误，直接抛出
                val before = injected
                // 剥除 android:attr="..."（属性值用双引号，manifest 由 aapt2 生成均如此）
                injected = injected.replace(Regex("""\s*${Pattern.quote(attr)}="[^"]*""""), "")
                if (injected == before) throw e // 模式未命中，避免死循环
                lastError = e
            }
        }
        throw lastError ?: IOException("Too many unknown attributes in manifest, stripped $MAX_UNKNOWN_ATTR_PASSES passes")
    }

    // 手动迭代收集子元素（getElements() 返回 java.util.Iterator，commonMain 下 toList/asSequence 不可用）
    private fun snapshotElements(e: ResXmlElement): List<ResXmlElement> {
        val list = mutableListOf<ResXmlElement>()
        val it = e.getElements()
        while (it.hasNext()) list.add(it.next())
        return list
    }

    private fun rollbackNewElements(e: ResXmlElement, snapshot: List<ResXmlElement>) {
        val toRemove = mutableListOf<ResXmlElement>()
        val it = e.getElements()
        while (it.hasNext()) {
            val n = it.next()
            if (snapshot.none { o -> o === n }) toRemove.add(n)
        }
        toRemove.forEach { it.removeSelf() }
    }

    /**
     * 在主合并前，从目标 APK 的 manifest 中查询 gameActivity：
     * 1. 优先：含 `<data android:scheme="com.sexyactioncool.bejeweledblitz" />` 的 Activity
     * 2. 回退：含 MAIN+LAUNCHER 的启动 Activity
     * 返回全限定 Activity 名称（如 "com.popcap.pvz2cmhd.SexyAppActivity"），找不到返回空串。
     */
    private fun findGameActivity(target: ApkModule, targetPackage: String): String {
        val tm = target.androidManifest

        // 优先级 1：查找含 data scheme="com.sexyactioncool.bejeweledblitz" 的 Activity
        val activities = tm.getActivities(true)
        val it1: Iterator<ResXmlElement> = activities.iterator()
        while (it1.hasNext()) {
            val act = it1.next()
            val children: Iterator<ResXmlElement> = act.getElements().iterator()
            while (children.hasNext()) {
                val child = children.next()
                if (child.name == "intent-filter") {
                    val dataNodes: Iterator<ResXmlElement> = child.getElements().iterator()
                    while (dataNodes.hasNext()) {
                        val d = dataNodes.next()
                        if (d.name == "data" && d.attr("scheme") == "com.sexyactioncool.bejeweledblitz") {
                            val name = act.attr("name") ?: continue
                            return sanitizeActivityName(name, targetPackage)
                        }
                    }
                }
            }
        }

        // 优先级 2：回退到 MAIN+LAUNCHER 启动 Activity
        val it2: Iterator<ResXmlElement> = tm.getActivities(true).iterator()
        while (it2.hasNext()) {
            val act = it2.next()
            val children: Iterator<ResXmlElement> = act.getElements().iterator()
            while (children.hasNext()) {
                val child = children.next()
                if (child.name == "intent-filter") {
                    var hasMain = false
                    var hasLauncher = false
                    val inner: Iterator<ResXmlElement> = child.getElements().iterator()
                    while (inner.hasNext()) {
                        val e = inner.next()
                        when (e.name) {
                            "action" -> if (e.attr("name") == "android.intent.action.MAIN") hasMain = true
                            "category" -> if (e.attr("name") == "android.intent.category.LAUNCHER") hasLauncher = true
                        }
                    }
                    if (hasMain && hasLauncher) {
                        val name = act.attr("name") ?: return ""
                        return sanitizeActivityName(name, targetPackage)
                    }
                }
            }
        }

        return ""
    }

    /** 把 manifest 中的 Activity 名称补全为全限定名 */
    private fun sanitizeActivityName(name: String, targetPackage: String): String {
        return when {
            name.startsWith(".") -> targetPackage + name
            '.' !in name -> "$targetPackage.$name"
            else -> name
        }
    }

    /**
     * 可选：把游戏主 Activity 的 `android:theme` 设为工具箱沉浸式主题，
     * 让进入游戏时复用工具箱的全屏沉浸样式（与 launcher 块的 Pvz2InitializeActivity 同源）。
     *
     * 通过 ARSCLib 的 [ResXmlAttribute.encode] 写入——与 [appendLauncherBlock] 解析注入块底层
     * 用的是同一套机制，能正确把 `@package:style/Theme.DreamPvzApp` 编码成资源引用。
     * 调用方需保证此时 0x66 资源表已 merge 且已 addExternalFramework（即在本函数之前调过 appendLauncherBlock）。
     *
     * 幂等：更新模式下重复调用只会覆盖同名 theme 属性，不会重复追加。
     */
    private fun applyGameActivityTheme(tm: AndroidManifestBlock, target: ApkModule, targetPackage: String) {
        val gameAct = findGameActivity(target, targetPackage)
        if (gameAct.isEmpty()) return
        val activities = tm.getActivities(true)
        val it: Iterator<ResXmlElement> = activities.iterator()
        while (it.hasNext()) {
            val act = it.next()
            val name = act.attr("name") ?: continue
            if (sanitizeActivityName(name, targetPackage) == gameAct) {
                // 先移除已有 theme（无论是否我们写入），再写入沉浸式主题，保证幂等
                act.removeAttributesWithName("theme")
                val attr: ResXmlAttribute = act.newAttribute()
                attr.encode(
                    "http://schemas.android.com/apk/res/android",
                    "android",
                    "theme",
                    "@$OUR_PKG_NAME:style/Theme.DreamPvzApp",
                    false
                )
                return
            }
        }
    }

    // ---- 通用拷贝（流式：直接复用 source 的 InputSource，writeApk 时从 zip 流式读取，零内存拷贝） ----

    private fun copyDir(source: ApkModule, target: ApkModule, prefix: String, skip: Set<String> = emptySet()) {
        source.listInputSources().filter { it.name.startsWith(prefix) && it.name !in skip }.forEach { ins ->
            target.removeInputSource(ins.name)
            target.add(ins)
        }
    }

    private fun copyFile(source: ApkModule, target: ApkModule, name: String) {
        val ins = source.getInputSource(name) ?: return
        target.removeInputSource(name)
        target.add(ins)
    }

    private fun mergeAssetsPvz2tool(source: ApkModule, target: ApkModule, targetPackage: String, includeExamples: Boolean, overrideDreamYml: String?, excludedSmfAssets: Set<String> = emptySet(),
                                  /** 保留目标已有文件：目标已存在的 assets/pvz2tool 文件不覆盖（文档 md 除外） */
                                  preserveTargetAssets: Boolean = false,
                                  /** 附加目标未包含的源文件：源有目标没有的文件是否写入 */
                                  appendUnreferenced: Boolean = true) {
        val prefix = "assets/pvz2tool/"
        val alwaysExclude = setOf(
            "assets/pvz2tool/parse_pvz_data.py",
            "assets/pvz2tool/PvZ2中文版代码(至v3.9.2).txt",
        )
        val dirExcludePrefixes = mutableListOf("assets/pvz2tool/素材/")
        if (!includeExamples) dirExcludePrefixes.add("assets/pvz2tool/example/")

        // 首次集成（目标 APK 尚未含任何 assets/pvz2tool 内容）：必须完整写入工具箱资源，
        // 否则非更新模式下目标「没有的」文件会被 appendUnreferenced=false 全跳过（pvz2tool 内容几乎为空）。
        val firstIntegrate = target.listInputSources().none { it.name.startsWith(prefix) }
        val effectiveAppendUnreferenced = appendUnreferenced || firstIntegrate

        source.listInputSources().filter { ins ->
            ins.name.startsWith(prefix) &&
            ins.name !in alwaysExclude &&
            dirExcludePrefixes.none { ins.name.startsWith(it) } &&
            // 用户排除的 SMF/资源条目（相对 assets/pvz2tool/ 的路径）
            ins.name.removePrefix(prefix).trimStart('/') !in excludedSmfAssets
        }.forEach { ins ->
            val rel = ins.name
            // 保留目标资源：目标已有的文件不覆盖（以下三类无条件覆盖：文档 md、dream.yml、integrator_info.txt）
            val alwaysOverwrite = rel.endsWith("/config_documentation.md") || rel.endsWith("/js_documentation.md")
                || rel == "assets/pvz2tool/dream.yml" || rel == "assets/pvz2tool/integrator_info.txt"
            if (preserveTargetAssets && target.getInputSource(rel) != null && !alwaysOverwrite) return@forEach
            // 开关关：源有目标没有的新文件也不写入（首次集成时强制覆盖此限制，保证完整集成）
            if (!effectiveAppendUnreferenced && target.getInputSource(rel) == null) return@forEach
            // 更新模式同样采用「来源/向导」的 assets/pvz2tool 内容：用户自定义资源应走向导的额外文件注入通道，
            // 新版工具箱的脚本/媒体/图标等默认覆盖目标 APK，确保新功能与修复能落地；dream.yml 已由 overrideDreamYml 处理。
            if (rel == "assets/pvz2tool/dream.yml") {
                // 优先使用外部提供的完整 YAML（集成器向导生成的配置）
                val finalText: String
                if (overrideDreamYml != null) {
                    // 集成器向导已基于「目标 APK 的 dream.yml 作为默认值源」生成完整配置，直接采用（已是合并结果）
                    finalText = overrideDreamYml
                } else {
                    val tgtBytes = target.getInputSource(rel)?.openStream()?.readBytes()
                    finalText = if (tgtBytes != null) {
                        // 目标 APK 已内置上次集成产物：其 dream.yml 即为已合并内容，直接沿用，不再做文本 merge
                        tgtBytes.toString(Charsets.UTF_8)
                    } else {
                        // 首次内置工具箱：动态确定 gameActivity
                        val srcText = ins.openStream().readBytes().toString(Charsets.UTF_8)
                        val ga = findGameActivity(target, targetPackage)
                        val updated = if (ga.isNotEmpty()) {
                            srcText.replace(Regex("^gameActivity:\\s*.+", RegexOption.MULTILINE), "gameActivity: $ga")
                        } else srcText
                        if (includeExamples) updated else stripExampleSections(updated)
                    }
                }
                target.removeInputSource(rel)
                target.add(ByteInputSource(finalText.toByteArray(Charsets.UTF_8), rel))
            } else {
                // 流式复用：writeApk 时从 source APK 的 zip 流式读取
                target.removeInputSource(rel)
                target.add(ins)
            }
        }
    }

    /**
     * 从 YAML 文本中剥离 id 以 "example_" 开头的栏目（section）。
     *
     * 算法：逐行解析 YAML 缩进。栏目在 sections: 下以 indent=2 的 `- id: "..."` 起始；
     * 检测到 `- id: "example_*"` 则进入跳过模式，直至遇到 indent≤1 的非空行（下一顶级键）
     * 或 indent=2 的非 example_ 栏目头（下一普通栏目）为止。
     */
    internal fun stripExampleSections(yamlText: String): String {
        val lines = yamlText.lines()
        val result = mutableListOf<String>()
        var inExample = false

        for (line in lines) {
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length

            if (inExample) {
                // 退出条件：indent≤1 的非空行（顶级键如 ui:），
                // 或 indent=2 的非 example_ 栏目头
                val exitByTopLevel = indent <= 1 && trimmed.isNotEmpty()
                val exitByNextSection = indent == 2 && trimmed.startsWith("- ") &&
                    !trimmed.startsWith("- id: \"example_")
                if (exitByTopLevel || exitByNextSection) {
                    inExample = false
                } else {
                    continue // 仍在示例栏目内，跳过
                }
            }

            // 检测示例栏目起始：indent=2 且 id 以 "example_" 开头
            if (!inExample && indent == 2 && trimmed.startsWith("- id: \"example_")) {
                inExample = true
                continue
            }

            result.add(line)
        }
        return result.joinToString("\n")
    }

    // ---- lib ----

    private fun computeLibAbis(source: ApkModule, target: ApkModule): List<String> {
        // 只替换目标已有架构的 lib，不引入目标没有的新 ABI → 合并后 ABI 集合 = 目标原 ABI 集合
        return target.libAbis().toList()
    }

    private fun mergeLib(source: ApkModule, target: ApkModule) {
        // 只替换目标已有架构的 .so：目标没有的 ABI 不引入（避免给游戏塞入它不支持的架构）。
        // 同名 .so 覆盖（工具箱的 libpvz2tool.so 等替换目标同名库），目标独有 .so 保留不动。
        // 流式复用：writeApk 时从 source APK 的 zip 流式读取 .so，不全量加载到内存。
        val tgtAbis = target.libAbis()
        source.listInputSources().filter { ins ->
            ins.name.startsWith("lib/") &&
                ins.name.substringAfter("lib/").contains("/") &&
                ins.name.substringAfter("lib/").substringBefore("/") in tgtAbis
        }.forEach { ins ->
            target.removeInputSource(ins.name)
            target.add(ins)
        }
    }

    private fun ApkModule.libAbis(): Set<String> =
        listInputSources()
            .filter { it.name.startsWith("lib/") && it.name.substringAfter("lib/").contains("/") }
            .map { it.name.substringAfter("lib/").substringBefore("/") }
            .toSet()

    // ---- ResXmlElement 小工具 ----

    private val ResXmlElement.name: String get() = getName() ?: ""

    private fun ResXmlElement.attr(localName: String): String? {
        val it: Iterator<ResXmlAttribute> = getAttributes()
        while (it.hasNext()) {
            val a = it.next()
            if (a.getName() == localName) return a.getValueAsString()
        }
        return null
    }

    // ---- 启动器组件块（README 4.2，游戏包名占位符 %PKG%，资源引用用 @app66: 全限定） ----

    private val LAUNCHER_BLOCK_XML = """
    <provider
        android:name="androidx.core.content.FileProvider"
        android:exported="false"
        android:authorities="%PKG%.fileprovider"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@%PKG66%:xml/file_paths" />
    </provider>

    <activity
        android:theme="@%PKG66%:style/Theme.DreamPvzApp"
        android:name="io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity"
        android:screenOrientation="sensorLandscape"
        android:configChanges="15840"
        android:windowSoftInputMode="stateAlwaysHidden">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
        <meta-data
            android:name="android.max_aspect"
            android:value="2.5" />
        <intent-filter android:label="导入 PVZ2 存档">
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:mimeType="application/x-pvz2saves" />
            <data android:scheme="content" />
        </intent-filter>
        <intent-filter android:label="导入 PVZ2 存档">
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="file" />
            <data android:host="*" />
            <data android:pathPattern=".*\.pvz2saves" />
        </intent-filter>
        <intent-filter android:label="导入 PVZ2 存档">
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="content" />
            <data android:scheme="file" />
            <data android:mimeType="*/*" />
            <data android:host="*" />
            <data android:pathPattern=".*\.pvz2saves" />
        </intent-filter>
        <intent-filter android:label="导入 PVZ2 存档">
            <action android:name="android.intent.action.SEND" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="application/x-pvz2saves" />
            <data android:mimeType="application/zip" />
        </intent-filter>
    </activity>

    <service
        android:name="io.github.dreammooncai.pvz2tool.service.LocalVpnService"
        android:permission="android.permission.BIND_VPN_SERVICE"
        android:exported="true">
        <intent-filter>
            <action android:name="android.net.VpnService" />
        </intent-filter>
    </service>

    <service
        android:name="io.github.dreammooncai.pvz2tool.timer.TimerService"
        android:exported="false"
        android:foregroundServiceType="dataSync" />

    <receiver
        android:name="io.github.dreammooncai.pvz2tool.timer.TimerReceiver"
        android:exported="false" />

    <!-- 冷重启专用：独立 :phoenix 进程透明 Activity（参照 ProcessPhoenix）。主进程被杀后仍存活，
         在 :phoenix 进程内拉起入口 Activity 到前台。主题指向源包 0x66 的 Pvz2ToolPhoenixTheme。 -->
    <activity
        android:name="io.github.dreammooncai.pvz2tool.RestartPhoenixActivity"
        android:process=":phoenix"
        android:excludeFromRecents="true"
        android:theme="@%PKG66%:style/Pvz2ToolPhoenixTheme"
        android:exported="false" />

    <provider
        android:name="com.petterp.floatingx.assist.FxContentProvider"
        android:exported="false"
        android:multiprocess="true"
        android:authorities="%PKG%.fx.provider" />

    <provider
        android:name="androidx.startup.InitializationProvider"
        android:exported="false"
        android:authorities="%PKG%.androidx-startup">
        <meta-data
            android:name="androidx.emoji2.text.EmojiCompatInitializer"
            android:value="androidx.startup" />
        <meta-data
            android:name="androidx.lifecycle.ProcessLifecycleInitializer"
            android:value="androidx.startup" />
        <meta-data
            android:name="androidx.profileinstaller.ProfileInstallerInitializer"
            android:value="androidx.startup" />
    </provider>

    <uses-library
        android:name="androidx.window.extensions"
        android:required="false" />
    <uses-library
        android:name="androidx.window.sidecar"
        android:required="false" />

    <receiver
        android:name="androidx.profileinstaller.ProfileInstallReceiver"
        android:permission="android.permission.DUMP"
        android:enabled="true"
        android:exported="true"
        android:directBootAware="false">
        <intent-filter>
            <action android:name="androidx.profileinstaller.action.INSTALL_PROFILE" />
        </intent-filter>
        <intent-filter>
            <action android:name="androidx.profileinstaller.action.SKIP_FILE" />
        </intent-filter>
        <intent-filter>
            <action android:name="androidx.profileinstaller.action.SAVE_PROFILE" />
        </intent-filter>
        <intent-filter>
            <action android:name="androidx.profileinstaller.action.BENCHMARK_OPERATION" />
        </intent-filter>
    </receiver>

    <provider
        android:name="com.kdroid.androidcontextprovider.ContextInitProvider"
        android:exported="false"
        android:authorities="%PKG%.kmploginitprovider" />
""".trimIndent()
}
