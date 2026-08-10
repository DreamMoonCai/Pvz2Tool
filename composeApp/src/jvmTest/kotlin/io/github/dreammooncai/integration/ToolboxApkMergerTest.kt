package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
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

    /**
     * 回归测试：更新模式删除「旧工具箱 DEX 区间」时，必须按数字序号排序，
     * 不能按字典序。否则 classes10.dex 会排在 classes2.dex 前面，
     * 导致删除窗口错位（误删尾部 classes10/11/12、保留前中部 classes3/4/5）。
     *
     * 复现用户现场：目标含 12 个 DEX，更新时删除前 5 个（索引 0..4），
     * 正确结果应为保留 classes6..classes12（数字序），且 classes10/11/12 不得被误删。
     */
    @Test
    fun `apply 更新模式按数字序删除DEX区间而非字典序`() {
        val src = requireSource()
        val srcModule = ApkModule.loadApkFile(src)
        val srcDexCount = srcModule.listDexFiles().size
        val srcDexFirst = readDex(srcModule, 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target_multidex.apk")
        // 造一个含 12 个 DEX 的目标：每个 DEX 末尾附近写入唯一标记字节，便于事后识别。
        // 标记值 = DEX 数字序号（classes.dex->1, classes2.dex->2, ... classes12.dex->12）。
        buildTargetApkWithDexes(target, srcDexFirst, 12)

        val out = File(work, "out_update_multidex.apk")
        val result = ToolboxApkMerger.apply(
            src, target, DexStrategy.INSERT_BEFORE, out,
            updateMode = true, dexStart = 0, dexEnd = 4, insertMode = DexStrategy.INSERT_BEFORE
        )

        val reloaded = ApkModule.loadApkFile(result.outputApk)
        val allDex = reloaded.listDexFiles()
        // 总数 = 源 DEX 数 + 剩余目标 DEX 数(12-5=7)
        assertEquals(srcDexCount + 7, allDex.size, "应合并为 ${srcDexCount + 7} 个 dex")

        val markerOffset = 2000
        fun markerOf(index: Int): Int {
            val b = allDex[index].openStream().readBytes()
            return b[markerOffset.coerceAtMost(b.lastIndex)].toInt() and 0xFF
        }
        // 产物前半是源（工具箱）DEX，后半是残留目标 DEX，按数字序应为标记 6..12
        val tailMarkers = (srcDexCount until allDex.size).map { markerOf(it) }
        assertEquals(
            List(7) { it + 6 }, tailMarkers,
            "数字序删除后应保留标记 6..12 的目标 DEX；修复前字典序会误删 10/11/12 并保留 3/4/5"
        )
        // 关键：classes10/11/12（标记 10/11/12）必须仍存在
        assertTrue(tailMarkers.containsAll(listOf(10, 11, 12)), "classes10/11/12 不得被字典序误删")
        // 被删的旧工具箱 DEX（标记 1..5）不得出现在残留目标中
        assertTrue(tailMarkers.none { it in 1..5 }, "旧工具箱 DEX（标记 1..5）应已被删除")

        println("[UPDATE multidex] OK -> ${out.absolutePath}")
    }

    private fun buildTargetApkWithDexes(out: File, baseDex: ByteArray, dexCount: Int) {
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

        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        for (i in 0 until dexCount) {
            val copy = baseDex.copyOf()
            val off = 2000.coerceAtMost(copy.lastIndex)
            copy[off] = (i + 1).toByte()
            val name = if (i == 0) "classes.dex" else "classes${i + 1}.dex"
            module.add(ByteInputSource(copy, name))
        }
        module.writeApk(out)
    }

}
