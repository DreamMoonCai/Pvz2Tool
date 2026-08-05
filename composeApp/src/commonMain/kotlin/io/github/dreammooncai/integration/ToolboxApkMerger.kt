package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.archive.InputSource
import com.reandroid.archive.RenamedInputSource
import com.reandroid.dex.model.DexFile
import com.reandroid.arsc.chunk.PackageBlock
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.xml.kxml2.KXmlParser
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
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
        val notes: List<String>
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
        removedTargetEntries: Set<String> = emptySet()
    ): IntegrateReport {
        val source = ApkModule.loadApkFile(sourceApk)
        val target = ApkModule.loadApkFile(targetApk)
        return try {
            computeReport(source, target, dexStrategy, sourceApk.name, targetApk.name, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries)
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
        removedTargetEntries: Set<String> = emptySet()
    ): MergeResult {
        val source = ApkModule.loadApkFile(sourceApk)
        val target = ApkModule.loadApkFile(targetApk)
        val report: IntegrateReport
        source.use { source ->
            report = computeReport(source, target, dexStrategy, sourceApk.name, targetApk.name, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries)
            doMerge(source, target, dexStrategy, report.targetPackage, includeExamples, overrideDreamYml, extraResources, extraResResources, excludedSmfAssets, removedTargetEntries)
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
        removedTargetEntries: Set<String> = emptySet()
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
        var assetsAdded = 0
        source.listInputSources().filter { it.name.startsWith("assets/pvz2tool/") }.forEach { ins ->
            val rel = ins.name.removePrefix("assets/pvz2tool/").trimStart('/')
            if (rel !in excludedSmfAssets && !target.containsFile(ins.name)) assetsAdded++
        }
        // dream.yml 差异统计
        val (dreamAdd, dreamSkip) = run {
            val s = source.getInputSource("assets/pvz2tool/dream.yml")?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
            val t = target.getInputSource("assets/pvz2tool/dream.yml")?.openStream()?.readBytes()?.toString(Charsets.UTF_8)
            if (s != null && t != null) {
                val (_, entries) = YamlTextMerger.merge(s, t)
                val add = entries.count { it.op == DiffOp.ADD }
                val skip = entries.count { it.op == DiffOp.SKIP }
                add to skip
            } else if (s != null) {
                1 to 0
            } else 0 to 0
        }
        val targetPkg = target.androidManifest.packageName ?: ""
        val manifestChanges = buildList {
            add("删除游戏原 LAUNCHER intent-filter")
            add("追加启动器组件块（Pvz2InitializeActivity / FileProvider / VPN / 各类 Provider / Receiver / uses-library）")
            val ts = target.androidManifest.targetSdkVersion
            if (ts == null || ts < 21) add("targetSdkVersion 调整至 21（当前 ${ts ?: "未设置"}）")
            else add("targetSdkVersion 保持 $ts（已 ≥21）")
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
            resultDexCount = srcDex + tgtDex,
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

    // ---- 实际合并（修改 target 模块） ----

    private fun doMerge(source: ApkModule, target: ApkModule, dexStrategy: DexStrategy, targetPackage: String, includeExamples: Boolean, overrideDreamYml: String?, extraResources: Map<String, File>, extraResResources: Map<String, File> = emptyMap(), excludedSmfAssets: Set<String> = emptySet(), removedTargetEntries: Set<String> = emptySet()) {
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
        appendLauncherBlock(tm, source, target, targetPackage)
        // appendLauncherBlock 在原地修改 tm（目标原本的 manifest 对象，根元素 <manifest> 不变），
        // 因此 targetSdkVersion 直接作用在 tm 上；最后 setManifest 固化（覆盖 getter 可能返回副本的情况）。
        val ts = tm.targetSdkVersion
        if (ts == null || ts < 21) tm.targetSdkVersion = 21
        target.setManifest(tm)

        // 3. dex
        renumberDex(source, target, dexStrategy)

        // 5. res 覆盖替换
        copyDir(source, target, "res/")

        // 1. 其余资源合并
        copyDir(source, target, "kotlin/")
        copyDir(source, target, "org/")
        copyDir(source, target, "META-INF/")
        copyFile(source, target, "DebugProbesKt.bin")
        copyFile(source, target, "kotlin-tooling-metadata.json")
        mergeAssetsPvz2tool(source, target, targetPackage, includeExamples, overrideDreamYml, excludedSmfAssets)

        // 注入向导中选择的额外文件到 APK
        for ((apkPath, localFile) in extraResources) {
            val fullPath = "assets/pvz2tool/$apkPath"
            target.removeInputSource(fullPath)
            target.add(ByteInputSource(localFile.readBytes(), fullPath))
        }

        // 注入向导中选择的额外 res 资源（如 @mipmap/bg_fill_image）到 APK 的 res/ 目录
        // 必须在 copyDir(source, target, "res/") 之后执行，以覆盖工具自带资源
        for ((apkPath, localFile) in extraResResources) {
            val fullPath = "res/$apkPath"
            target.removeInputSource(fullPath)
            target.add(ByteInputSource(localFile.readBytes(), fullPath))
        }

        // 6. lib 按 ABI 合并
        mergeLib(source, target)
    }

    // ---- arsc / 包 ----

    // ---- dex ----

    /**
     * 目标 APK 原 DEX 已自带、需要被剔除出工具箱 DEX 的包前缀（类型描述符形式，含末尾 '/'）。
     * 典型场景：游戏 APK 自身已内置 androidx.constraintlayout，而工具箱 DEX 也打包了一份，
     * 插入到最前时两份同名类会冲突/被游戏版本覆盖，因此工具箱侧主动剥离、改由游戏侧提供。
     */
    private val STRIP_DEX_PACKAGE_PREFIXES = listOf("Landroidx/constraintlayout/")

    private fun renumberDex(source: ApkModule, target: ApkModule, dexStrategy: DexStrategy) {
        val srcDex = source.listDexFiles().sortedBy { it.name }
        val tgtDex = target.listDexFiles().sortedBy { it.name }
        val ordered = if (dexStrategy == DexStrategy.INSERT_BEFORE) srcDex + tgtDex else tgtDex + srcDex
        val stripPackages = dexStrategy == DexStrategy.INSERT_BEFORE &&
            tgtDex.any { dex -> dexContainsAnyPackage(dex) }
        target.listDexFiles().forEach { target.removeInputSource(it.name) }

        val tempDir = File(System.getProperty("java.io.tmpdir"), "dex_strip").apply { mkdirs() }
        ordered.forEachIndexed { idx, dex ->
            val isSourceDex = srcDex.any { it === dex }
            val renamed = if (stripPackages && isSourceDex && dexContainsAnyPackage(dex)) {
                val stripped = runCatching { stripPackagesToFile(dex, tempDir) }
                    .onFailure { it.printStackTrace() }
                if (stripped.isSuccess)
                    com.reandroid.archive.FileInputSource(stripped.getOrThrow(), dexName(idx))
                else
                    RenamedInputSource(dexName(idx), dex)
            } else {
                RenamedInputSource(dexName(idx), dex)
            }
            target.add(renamed)
        }
    }

    /** 目标原 DEX 是否包含任一需剔除包下的类。
     *  流式扫描 dex 内容中是否出现类型描述符前缀（如 "Landroidx/constraintlayout/"），
     *  不将整个 dex 加载到内存中，适配 Android 受限堆。 */
    private fun dexContainsAnyPackage(dex: InputSource): Boolean {
        return runCatching {
            dex.openStream().use { stream ->
                val target = STRIP_DEX_PACKAGE_PREFIXES.first().encodeToByteArray()
                val buf = ByteArray(target.size * 2)
                var pos = 0
                var bytesRead: Int
                while (stream.read(buf, pos, buf.size - pos).also { bytesRead = it } > 0) {
                    val end = pos + bytesRead
                    // 在已读缓冲区中搜索
                    if (buf.searchBytes(target, 0, end) >= 0) return@runCatching true
                    // 保留末尾 target.size-1 字节作为滚动窗口（防止模式跨块边界）
                    if (end > target.size) {
                        val keep = target.size - 1
                        System.arraycopy(buf, end - keep, buf, 0, keep)
                        pos = keep
                    } else {
                        pos = end
                    }
                }
                false
            }
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

    private fun ByteArray.searchBytes(pattern: ByteArray, from: Int, to: Int): Int {
        if (pattern.isEmpty() || pattern.size > (to - from)) return -1
        val last = to - pattern.size
        outer@ for (i in from..last) {
            for (j in pattern.indices) {
                if (this[i + j] != pattern[j]) continue@outer
            }
            return i
        }
        return -1
    }

    /** 从工具箱 DEX 中剔除需剥离包下的类定义。
     *  纯字节级操作，不动用 ARSCLib DexFile（其内部构建完整对象树，Android 上 OOM）。
     *  解析 dex header → 遍历 type_ids→string_ids 标记匹配前缀 → 移除对应 class_def → 更新 header。 */
    private fun stripPackagesToFile(dex: InputSource, tempDir: File): File {
        val bytes = dex.openStream().use { it.readBytes() }
        val stripped = stripClassesFromDexBytes(bytes, STRIP_DEX_PACKAGE_PREFIXES)
        val outFile = File(tempDir, "out_${System.nanoTime()}.dex")
        try {
            outFile.writeBytes(stripped)
            return outFile
        } catch (e: Exception) {
            outFile.delete()
            throw e
        }
    }

    /** 纯字节级 dex 类剥离：移除所有 type descriptor 以 prefixes 中任一开头的 class_def。
     *  swap-and-pop 原地移除，不动 data 区避免内部偏移量失效。完全不依赖 ARSCLib。 */
    internal fun stripClassesFromDexBytes(bytes: ByteArray, prefixes: List<String>): ByteArray {
        if (prefixes.isEmpty()) return bytes

        fun readU32(off: Int): Int {
            var v = 0
            for (i in 0..3) v = v or ((bytes[off + i].toInt() and 0xFF) shl (i * 8))
            return v
        }
        fun writeU32(off: Int, v: Int) {
            for (i in 0..3) bytes[off + i] = (v shr (i * 8) and 0xFF).toByte()
        }

        val strsSize = readU32(0x38); val strsOff = readU32(0x3C)
        val typsSize = readU32(0x40); val typsOff = readU32(0x44)
        val clDefSize = readU32(0x60); val clDefOff = readU32(0x64)

        // 1. 读取 type descriptor 字符串
        val stringCache = mutableMapOf<Int, String>()
        fun getString(idx: Int): String = stringCache.getOrPut(idx) {
            val off = readU32(strsOff + idx * 4)
            var p = off; while ((bytes[p].toInt() and 0x80) != 0) p++; p++
            val s = p; while (bytes[p] != 0.toByte()) p++
            bytes.decodeToString(s, p)
        }

        // 2. 标记要移除的 type_id
        var hasMatch = false
        val removeType = BooleanArray(typsSize)
        for (i in 0 until typsSize) {
            if (prefixes.any { getString(readU32(typsOff + i * 4)).startsWith(it) }) {
                removeType[i] = true; hasMatch = true
            }
        }
        if (!hasMatch) return bytes

        // 3. swap-and-pop 原地移除
        var tail = clDefSize - 1
        for (i in 0..tail) {
            val classIdx = readU32(clDefOff + i * 32)
            if (removeType[classIdx]) {
                // 用尾部非移除 entry 覆盖当前
                while (tail > i && removeType[readU32(clDefOff + tail * 32)]) tail--
                if (tail > i) {
                    System.arraycopy(bytes, clDefOff + tail * 32, bytes, clDefOff + i * 32, 32)
                    tail--
                }
            }
        }
        val kept = tail + 1
        if (kept == clDefSize) return bytes

        // 4. 更新 header
        writeU32(0x60, kept)

        // 5. 更新 map_list 中 TYPE_CLASS_DEF_ITEM 的 size
        val mapOff = readU32(0x34)
        val mapSize = readU32(mapOff)
        var pos = mapOff + 4
        for (m in 0 until mapSize) {
            val type = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
            if (type == 0x0006) { // TYPE_CLASS_DEF_ITEM
                writeU32(pos + 6, kept)
            }
            pos += 12
        }
        return bytes
    }

    private fun dexName(index: Int): String =
        if (index == 0) "classes.dex" else "classes${index + 1}.dex"

    // ---- manifest ----

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

    // ---- 通用拷贝（流式：直接复用 source 的 InputSource，writeApk 时从 zip 流式读取，零内存拷贝） ----

    private fun copyDir(source: ApkModule, target: ApkModule, prefix: String) {
        source.listInputSources().filter { it.name.startsWith(prefix) }.forEach { ins ->
            target.removeInputSource(ins.name)
            target.add(ins)
        }
    }

    private fun copyFile(source: ApkModule, target: ApkModule, name: String) {
        val ins = source.getInputSource(name) ?: return
        target.removeInputSource(name)
        target.add(ins)
    }

    private fun mergeAssetsPvz2tool(source: ApkModule, target: ApkModule, targetPackage: String, includeExamples: Boolean, overrideDreamYml: String?, excludedSmfAssets: Set<String> = emptySet()) {
        val prefix = "assets/pvz2tool/"
        val alwaysExclude = setOf(
            "assets/pvz2tool/parse_pvz_data.py",
            "assets/pvz2tool/PvZ2中文版代码(至v3.9.2).txt",
        )
        val dirExcludePrefixes = mutableListOf("assets/pvz2tool/素材/")
        if (!includeExamples) dirExcludePrefixes.add("assets/pvz2tool/example/")

        source.listInputSources().filter { ins ->
            ins.name.startsWith(prefix) &&
            ins.name !in alwaysExclude &&
            dirExcludePrefixes.none { ins.name.startsWith(it) } &&
            // 用户排除的 SMF/资源条目（相对 assets/pvz2tool/ 的路径）
            ins.name.removePrefix(prefix).trimStart('/') !in excludedSmfAssets
        }.forEach { ins ->
            val rel = ins.name
            if (rel == "assets/pvz2tool/dream.yml") {
                // 优先使用外部提供的完整 YAML（集成器向导生成的配置）
                val finalText: String
                if (overrideDreamYml != null) {
                    finalText = overrideDreamYml
                } else {
                    val srcText = ins.openStream().readBytes().toString(Charsets.UTF_8)
                    val tgtBytes = target.getInputSource(rel)?.openStream()?.readBytes()
                    finalText = if (tgtBytes != null) {
                        // 目标已有 dream.yml：差异合并（保留目标既有 gameActivity）
                        YamlTextMerger.merge(srcText, tgtBytes.toString(Charsets.UTF_8)).first
                    } else {
                        // 首次内置工具箱：动态确定 gameActivity
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
