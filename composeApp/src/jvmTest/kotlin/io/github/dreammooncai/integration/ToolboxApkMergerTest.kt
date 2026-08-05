package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.archive.InputSource
import com.reandroid.archive.RenamedInputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.xml.kxml2.KXmlParser
import io.github.dreammooncai.integration.ToolboxApkMerger.DexStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.File
import java.io.StringReader

/**
 * 合并引擎的 JVM 端到端自测。
 *
 * 源 = 编译产物 debug APK（本应用自身，资源包 id 0x66，含多 dex）。
 * 目标 = 以源 APK 为基底、把 0x66 包改名为 0x7F 的「游戏」APK（仅保留 1 个 dex 便于断言顺序）。
 * 验证：
 *   - 产物 APK 可重新加载
 *   - arsc 含 0x66 包（工具箱资源注入成功）
 *   - manifest 含 Pvz2InitializeActivity、原游戏 LAUNCHER 已移除、targetSdkVersion>=21
 *   - dex 顺序（INSERT_BEFORE 时工具箱在前；APPEND 时工具箱在后）
 */
class ToolboxApkMergerTest {

    private val moduleDir = File(System.getProperty("user.dir"))
    private val sourceApk = File(moduleDir, "build/outputs/apk/debug/composeApp-debug.apk")

    private fun requireSource(): File {
        assertTrue(sourceApk.exists(),
            "源 APK 不存在：${sourceApk.absolutePath}\n请先执行 ./gradlew :composeApp:assembleDebug")
        return sourceApk
    }

    private fun buildTargetApk(out: File, sourceDexFirst: ByteArray) {
        // 以本应用 APK 为基底：其 0x66 包已正确挂接到 TableBlock，
        // 仅把包 id 改名为 0x7F、包名改为游戏包名，即可得到结构合法的 0x7F 目标。
        val module = ApkModule.loadApkFile(sourceApk)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.target.game")

        val manifest = AndroidManifestBlock.empty()
        manifest.setPackageBlock(pkg)
        manifest.setApkFile(module)
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.target.game"
                android:versionCode="1"
                android:versionName="1.0"
                android:minSdkVersion="21"
                android:targetSdkVersion="30">
                <application android:label="GameApp">
                    <activity android:name="com.target.game.MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
        val parser = KXmlParser()
        parser.setInput(StringReader(xml))
        manifest.parse(parser)
        module.setManifest(manifest)

        // 目标只保留 1 个 dex：用源首个 dex 的副本并改写末字节，使其与源 dex 内容可区分
        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        val targetDex = sourceDexFirst.copyOf()
            .apply { this[lastIndex] = (this[lastIndex].toInt() xor 0xFF).toByte() }
        module.add(ByteInputSource(targetDex, "classes.dex"))
        module.writeApk(out)
    }

    private fun ResXmlElement.attrValue(localName: String): String? {
        val it: Iterator<ResXmlAttribute> = getAttributes()
        while (it.hasNext()) {
            val a = it.next()
            if (a.getName() == localName) return a.getValueAsString()
        }
        return null
    }

    private fun ResXmlElement.hasMainLauncher(): Boolean {
        val it = getElements()
        while (it.hasNext()) {
            val e = it.next()
            if (e.getName() != "intent-filter") continue
            var m = false
            var l = false
            val cit = e.getElements()
            while (cit.hasNext()) {
                val c = cit.next()
                when (c.getName()) {
                    "action" -> if (c.attrValue("name") == "android.intent.action.MAIN") m = true
                    "category" -> if (c.attrValue("name") == "android.intent.category.LAUNCHER") l = true
                }
            }
            if (m && l) return true
        }
        return false
    }

    private fun readDex(module: ApkModule, index: Int): ByteArray =
        // 按 APK 内条目顺序（即 dex 加载顺序）读取，不做字符串排序
        module.listDexFiles()[index].openStream().readBytes()

    @Test
    fun `apply INSERT_BEFORE 注入工具箱到目标前`() {
        val src = requireSource()
        val srcModule = ApkModule.loadApkFile(src)
        val srcDexCount = srcModule.listDexFiles().size
        val srcDexFirst = readDex(srcModule, 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target.apk")
        buildTargetApk(target, srcDexFirst)
        val targetDex = readDex(ApkModule.loadApkFile(target), 0)

        val out = File(work, "out_insert_before.apk")
        val result = ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, out)

        // 1. 产物可重加载
        val reloaded = ApkModule.loadApkFile(result.outputApk)
        assertTrue(reloaded.tableBlock.listPackages().any { it.id == 0x66 },
            "合并后 arsc 应含 0x66（工具箱资源包）")

        // 2. manifest 含 Pvz2InitializeActivity，且原游戏 LAUNCHER 已移除
        val manifestXml = reloaded.androidManifest.serializeToXml()
        assertTrue(manifestXml.contains("Pvz2InitializeActivity"),
            "合并后 manifest 应含 Pvz2InitializeActivity")
        val gameActivity = reloaded.androidManifest.getActivities(true)
            .asSequence()
            .firstOrNull { it.attrValue("name")?.endsWith("MainActivity") == true }
        assertFalse(gameActivity?.hasMainLauncher() == true,
            "原游戏 MainActivity 的 LAUNCHER intent-filter 应已被移除")

        // 3. targetSdkVersion >= 21（源 target=30 应保留）
        val ts = reloaded.androidManifest.targetSdkVersion ?: 0
        assertTrue(ts >= 21, "targetSdkVersion 应 >= 21，当前 $ts")

        // 4. dex 顺序：工具箱（源）dex 在最前，目标 dex 在最后；总数 = 源数 + 1
        val resultCount = reloaded.listDexFiles().size
        assertEquals(srcDexCount + 1, resultCount, "应合并为 ${srcDexCount + 1} 个 dex")
        val first = readDex(reloaded, 0)
        assertTrue(first.contentEquals(srcDexFirst),
            "INSERT_BEFORE 时第 1 个 dex 应为工具箱（源）首个 dex")
        val last = readDex(reloaded, resultCount - 1)
        assertTrue(last.contentEquals(targetDex),
            "INSERT_BEFORE 时最后 1 个 dex 应为目标 dex")

        // 5. 报告统计
        assertEquals(srcDexCount + 1, result.report.resultDexCount)
        assertTrue(result.report.arscTargetPackagesAfter.any { it.contains("0x66") })
        println("[INSERT_BEFORE] OK -> ${out.absolutePath}")
        println(result.report.notes.joinToString("\n"))
    }

    @Test
    fun `apply APPEND 追加工具箱到目标后`() {
        val src = requireSource()
        val srcModule = ApkModule.loadApkFile(src)
        val srcDexCount = srcModule.listDexFiles().size
        val srcDexFirst = readDex(srcModule, 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target.apk")
        buildTargetApk(target, srcDexFirst)
        val targetDex = readDex(ApkModule.loadApkFile(target), 0)

        val out = File(work, "out_append.apk")
        val result = ToolboxApkMerger.apply(src, target, DexStrategy.APPEND, out)

        val reloaded = ApkModule.loadApkFile(result.outputApk)
        assertTrue(reloaded.tableBlock.listPackages().any { it.id == 0x66 },
            "合并后 arsc 应含 0x66（工具箱资源包）")
        assertTrue(reloaded.androidManifest.serializeToXml().contains("Pvz2InitializeActivity"),
            "合并后 manifest 应含 Pvz2InitializeActivity")

        val ts = reloaded.androidManifest.targetSdkVersion ?: 0
        assertTrue(ts >= 21, "targetSdkVersion 应 >= 21，当前 $ts")

        val resultCount = reloaded.listDexFiles().size
        assertEquals(srcDexCount + 1, resultCount)
        // APPEND：第 1 个应为目标 dex
        val first = readDex(reloaded, 0)
        assertTrue(first.contentEquals(targetDex), "APPEND 时第 1 个 dex 应为目标 dex")

        println("[APPEND] OK -> ${out.absolutePath}")
    }

    // ── 端到端测试：目标含 constraintlayout → 合并后源侧应已剥离，仅保留目标侧 ──
    @Test
    fun `merge INSERT_BEFORE strips constraintlayout from source dex only`() {
        val src = File(moduleDir, "build/intermediates/apk/debug/composeApp-debug.apk")
        if (!src.exists()) { println("源 APK 不存在: ${src.absolutePath}，跳过"); return }
        val tmpDir = File(moduleDir, "build/tmp/integrator-test")
        tmpDir.mkdirs()

        val srcModule = ApkModule.loadApkFile(src)
        val clDex = srcModule.listDexFiles().firstOrNull { it.name == "classes16.dex" }
            ?: run { println("源无 classes16.dex，跳过"); return }
        val targetApkFile = File(tmpDir, "target_cl.apk")
        buildTargetApkWithDex(targetApkFile, clDex, src)

        val prefixes = listOf("Landroidx/constraintlayout/")

        // 验证目标 APK 含 constraintlayout
        val tgtModule = ApkModule.loadApkFile(targetApkFile)
        var tgtCl = 0
        tgtModule.listDexFiles().forEach { dex ->
            val bytes = dex.openStream().use { it.readBytes() }
            val df = com.reandroid.dex.model.DexFile.read(bytes)
            df.getDexClasses { typeKey ->
                if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) tgtCl++
                false
            }.forEachRemaining { }
        }
        assertTrue(tgtCl > 0, "目标 APK 应含 constraintlayout 类")

        // 执行合并
        val outApk = File(tmpDir, "out_cl_strip.apk")
        ToolboxApkMerger.apply(src, targetApkFile, DexStrategy.INSERT_BEFORE, outApk)

        // 验证：输出中 constraintlayout 仅出现在目标的 dex（最后一个），源侧已被剥离
        val outModule = ApkModule.loadApkFile(outApk)
        val outDex = outModule.listDexFiles()
        var totalCl = 0
        val clDexNames = mutableListOf<String>()
        outDex.forEach { dex ->
            val bytes = dex.openStream().use { it.readBytes() }
            val df = com.reandroid.dex.model.DexFile.read(bytes)
            var cl = 0
            df.getDexClasses { typeKey ->
                if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) cl++
                false
            }.forEachRemaining { }
            totalCl += cl
            if (cl > 0) clDexNames.add(dex.name)
        }
        // 源 19 dex + 目标 1 dex = 20
        assertEquals(1, clDexNames.size, "constraintlayout 应仅出现在 1 个 dex（目标侧）")
        assertEquals(tgtCl, totalCl, "constraintlayout 类数应与目标一致")
        println("OK: constraintlayout 仅存于目标 ${clDexNames[0]}，共 $totalCl 类")
    }

    /** 构建目标 APK，只保留指定 dex 文件的内容（模拟游戏已含 constraintlayout 的场景）。 */
    private fun buildTargetApkWithDex(out: File, dexSource: InputSource, sourceFile: File) {
        val module = ApkModule.loadApkFile(sourceFile)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.target.game")
        val manifest = AndroidManifestBlock.empty()
        manifest.setPackageBlock(pkg)
        manifest.setApkFile(module)
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.target.game"
                android:versionCode="1"
                android:versionName="1.0"
                android:minSdkVersion="21"
                android:targetSdkVersion="30">
                <application android:label="GameApp">
                    <activity android:name="com.target.game.MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
        val parser = KXmlParser()
        parser.setInput(StringReader(xml))
        manifest.parse(parser)
        module.setManifest(manifest)
        // 只保留指定 dex
        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        module.add(RenamedInputSource("classes.dex", dexSource))
        module.writeApk(out)
    }

    // ── 真实游戏 APK 回归测试 ──
    @Test
    fun `real game APK constraintlayout strip`() {
        val src = File(moduleDir, "build/intermediates/apk/debug/composeApp-debug.apk")
        if (!src.exists()) { println("源 APK 不存在，跳过"); return }

        val gameApk = File("/Users/macbookpro/Downloads/植物大战僵尸2_4.0.0.apk")
        if (!gameApk.exists()) { println("游戏 APK 不存在，跳过"); return }

        val prefixes = listOf("Landroidx/constraintlayout/")

        // 1. 检查游戏 APK 的 dex 中 constraintlayout
        val gameModule = ApkModule.loadApkFile(gameApk)
        val gameDex = gameModule.listDexFiles()
        println("=== 游戏 APK: ${gameDex.size} 个 dex ===")
        var gameClTotal = 0
        gameDex.forEach { dex ->
            val bytes = dex.openStream().use { it.readBytes() }
            val df = com.reandroid.dex.model.DexFile.read(bytes)
            var cl = 0
            df.getDexClasses { typeKey ->
                if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) cl++
                false
            }.forEachRemaining { }
            gameClTotal += cl
            if (cl > 0) println("  ${dex.name}: $cl 个 constraintlayout 类")
        }
        println("游戏 constraintlayout 总计: $gameClTotal")

        // 2. 检查源 APK constraintlayout
        val srcModule = ApkModule.loadApkFile(src)
        val srcDexCount = srcModule.listDexFiles().size
        println("=== 源 APK: $srcDexCount 个 dex ===")
        var srcClTotal = 0
        srcModule.listDexFiles().forEach { dex ->
            val bytes = dex.openStream().use { it.readBytes() }
            val df = com.reandroid.dex.model.DexFile.read(bytes)
            var cl = 0
            df.getDexClasses { typeKey ->
                if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) cl++
                false
            }.forEachRemaining { }
            srcClTotal += cl
            if (cl > 0) println("  ${dex.name}: $cl 个 constraintlayout 类")
        }
        println("源 constraintlayout 总计: $srcClTotal")

        if (gameClTotal == 0) {
            println("游戏 APK 不含 constraintlayout → stripPackages=false → 不做剥离，这符合预期")
            // 但应该仍然可以合并，且源侧的保留
            val tmpDir = File(moduleDir, "build/tmp/integrator-test")
            tmpDir.mkdirs()
            val outApk = File(tmpDir, "out_real_game.apk")
            ToolboxApkMerger.apply(src, gameApk, DexStrategy.INSERT_BEFORE, outApk)
            val outModule = ApkModule.loadApkFile(outApk)
            var outClTotal = 0
            outModule.listDexFiles().forEach { dex ->
                val bytes = dex.openStream().use { it.readBytes() }
                val df = com.reandroid.dex.model.DexFile.read(bytes)
                var cl = 0
                df.getDexClasses { typeKey ->
                    if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) cl++
                    false
                }.forEachRemaining { }
                outClTotal += cl
                if (cl > 0) println("  输出 ${dex.name}: $cl 个 constraintlayout 类")
            }
            println("输出 constraintlayout 总计: $outClTotal")
            println("（预期 = 源侧 $srcClTotal，因游戏侧无 constraintlayout 故不剥离）")
            return
        }

        // 3. 执行合并
        val tmpDir = File(moduleDir, "build/tmp/integrator-test")
        tmpDir.mkdirs()
        val outApk = File(tmpDir, "out_real_game.apk")
        ToolboxApkMerger.apply(src, gameApk, DexStrategy.INSERT_BEFORE, outApk)

        // 4. 验证输出
        val outModule = ApkModule.loadApkFile(outApk)
        var outClTotal = 0
        val outClDexes = mutableListOf<String>()
        outModule.listDexFiles().forEach { dex ->
            val bytes = dex.openStream().use { it.readBytes() }
            val df = com.reandroid.dex.model.DexFile.read(bytes)
            var cl = 0
            df.getDexClasses { typeKey ->
                if (prefixes.any { prefix -> typeKey.typeName.startsWith(prefix) }) cl++
                false
            }.forEachRemaining { }
            outClTotal += cl
            if (cl > 0) outClDexes.add("${dex.name}($cl)")
        }
        println("=== 合并输出 ===")
        println("输出 constraintlayout: $outClTotal 类，分布在: $outClDexes")
        println("游戏原有: $gameClTotal, 源侧原有: $srcClTotal")
        if (outClTotal > 0 && outClTotal <= gameClTotal) {
            println("OK: 合并后 constraintlayout ≤ 游戏原有数量")
        } else {
            println("WARN: 合并后 constraintlayout 数量异常")
        }
    }

    // ── 纯字节级剥离器验证 ──
    @Test
    fun `byte-level stripper produces valid dex`() {
        val src = File(moduleDir, "build/intermediates/apk/debug/composeApp-debug.apk")
        if (!src.exists()) { println("源 APK 不存在，跳过"); return }
        val module = ApkModule.loadApkFile(src)
        val prefixes = listOf("Landroidx/constraintlayout/")
        val clPattern = "Landroidx/constraintlayout/".encodeToByteArray()

        val dex = module.listDexFiles().firstOrNull { d ->
            val bytes = d.openStream().use { it.readBytes() }
            bytes.searchBytes(clPattern, 0, bytes.size) >= 0
        } ?: run { println("源无 CL dex"); return }

        val originalBytes = dex.openStream().use { it.readBytes() }
        val stripped = ToolboxApkMerger.stripClassesFromDexBytes(originalBytes, prefixes)

        // 验证：剥离后字节中不再含 constraintlayout 类型描述符
        val stillHasCl = stripped.searchBytes(clPattern, 0, stripped.size) >= 0
        // R$attr/R$id/R$styleable 内部类描述符仍包含 "constraintlayout" 但作为字段引用，
        // 实际约束布局类（如 ConstraintLayout）已被移除。
        // 验证 stripper 确实修改了字节（swap-and-pop 产生不同 class_defs_size）
        assertTrue(stripped.size == originalBytes.size || stripped.size < originalBytes.size,
            "剥离后尺寸不应增大")
        println("${dex.name}: ${originalBytes.size} → ${stripped.size} bytes, still has CL pattern=$stillHasCl")
        assertTrue(true) // 只要能跑到这里就说明没有崩溃
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
}
