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

    /**
     * android 框架里的某个 style 资源 id（包 id = 0x01）。
     * 测试只关心「这是一条指向框架包的引用」，具体是哪个 style 不影响任何断言。
     */
    private val ANDROID_FRAMEWORK_STYLE_ID = 0x01030007

    private fun buildTargetApk(out: File, sourceDexFirst: ByteArray, gameActivityThemeId: Int? = null) {
        // 以本应用 APK 为基底：其 0x66 包已正确挂接到 TableBlock，
        // 仅把包 id 改名为 0x7F、包名改为游戏包名，即可得到结构合法的 0x7F 目标。
        val module = ApkModule.loadApkFile(sourceApk)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.target.game")

        module.setManifest(newGameManifest(module, pkg))

        // 目标只保留 1 个 dex：用源首个 dex 的副本并改写末字节，使其与源 dex 内容可区分
        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        val targetDex = sourceDexFirst.copyOf()
            .apply { this[lastIndex] = (this[lastIndex].toInt() xor 0xFF).toByte() }
        module.add(ByteInputSource(targetDex, "classes.dex"))
        module.writeApk(out)

        if (gameActivityThemeId != null) injectGameActivityTheme(out, gameActivityThemeId)
    }

    /**
     * 造一份「游戏」manifest。
     *
     * 🔴 必须用 `AndroidManifestBlock()` 而不是 `AndroidManifestBlock.empty()`：
     * `empty()` 会先建一个占位根元素 `<x/>`（`AndroidManifest.EMPTY_MANIFEST_TAG`），
     * 随后 `parse()` 把真正的 `<manifest>` **追加成第二个顶层元素**。
     * ARSCLib 的所有查询 API（`getActivities` / `getApplicationElement` / XMLPath）
     * 只认第一个文档元素，于是全部返回空——合并器根本定位不到游戏 Activity，
     * 测试会「假绿」（弱断言全过，实际什么都没改）。
     */
    private fun newGameManifest(module: ApkModule, pkg: com.reandroid.arsc.chunk.PackageBlock): AndroidManifestBlock {
        val manifest = AndroidManifestBlock()
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
        // 兜底：若上游行为变化又冒出占位根元素，直接剔掉，保证 <manifest> 是唯一顶层元素
        manifest.removeElementsIf { it.name != "manifest" }
        check(manifest.documentElement.name == "manifest") {
            "目标 manifest 根元素应为 <manifest>，实际 <${manifest.documentElement.name}>"
        }
        return manifest
    }

    /**
     * 给已写出的目标 APK 的游戏 MainActivity 补一条 `android:theme` 资源引用。
     *
     * 为什么不在上面的 XML 文本里写 `android:theme="@android:style/xxx"`：
     *  1. `@android:` 框架引用在未 `addExternalFramework` 时解析不出资源 id，属性会被静默丢弃；
     *  2. `AndroidManifestBlock.parse()` 之后的内存树用 `getActivities()`（XMLPath 查询）查不到任何元素，
     *     必须先落盘再重新加载才可查询。
     * 所以改成「写出 → 重新加载 → 按资源 id 直接写属性 → 回写」，与生产代码操作真实 APK 的路径一致。
     */
    private fun injectGameActivityTheme(apk: File, themeId: Int) {
        val module = ApkModule.loadApkFile(apk)
        val act = module.androidManifest.getActivities(true).asSequence()
            .firstOrNull { it.attrValue("name")?.endsWith("MainActivity") == true }
            ?: error("目标 APK 内找不到 MainActivity，无法注入 theme")
        val attr = act.newAttribute()
        // 不用 encodeAttributeName：它需要注册 android framework 才能把 android:theme 解析成 0x01010000
        attr.setNamespace("http://schemas.android.com/apk/res/android", "android")
        attr.setName("theme", 0x01010000)
        attr.setValueAsResourceId(themeId)

        val tmp = File(apk.parentFile, apk.name + ".tmp")
        module.writeApk(tmp)
        module.close()
        check(apk.delete() && tmp.renameTo(apk)) { "回写目标 APK 失败：${apk.absolutePath}" }
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

    /**
     * 回归测试：沉浸式主题开关在**更新模式**下必须双向生效。
     *
     * 场景：游戏 MainActivity 原本带一条指向 android 框架包（0x01）的 `android:theme` 引用。
     *  1. 首次集成 useImmersiveTheme=true  → 主题被换成工具箱 0x66 主题，且描述文件记录原主题 id
     *  2. 更新模式 useImmersiveTheme=false → 主题必须**还原**为原来的 @android 主题
     *
     * 修复前：关闭开关是空操作（`if (useImmersiveTheme) applyGameActivityTheme(...)`），
     * 已注入的沉浸式主题永远留在产物里，等于更新模式下改不了这个选项。
     */
    @Test
    fun `更新模式关闭沉浸式主题应还原游戏原主题`() {
        val src = requireSource()
        val srcDexFirst = readDex(ApkModule.loadApkFile(src), 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target_theme.apk")
        buildTargetApk(target, srcDexFirst, gameActivityThemeId = ANDROID_FRAMEWORK_STYLE_ID)

        // 目标原主题 id（用于最终比对）
        val originalThemeId = ApkModule.loadApkFile(target).androidManifest
            .gameActivityThemeAttr()!!.getData()
        assertTrue(originalThemeId ushr 24 == 0x01,
            "原主题应为 android 框架资源（0x01 包），实际 0x%08x".format(originalThemeId))

        // ---- 第 1 步：首次集成，开启沉浸式主题 ----
        val on = File(work, "out_theme_on.apk")
        ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, on, useImmersiveTheme = true)

        val onModule = ApkModule.loadApkFile(on)
        // 游戏 Activity 上不能出现两条 android:theme（ARSCLib 的 removeAttributesWithName 只删无 id 的属性，
        // 曾导致新旧主题并存、系统取第一条 → 沉浸式主题看起来没生效）
        assertEquals(1, onModule.androidManifest.gameActivityThemeAttrs().size,
            "游戏 Activity 只能有一条 android:theme 属性")
        val onTheme = onModule.androidManifest.gameActivityThemeAttr()
        // 判据用「与工具箱启动 Activity 同一个主题」而不是硬编码 0x66：
        // 本测试的目标包是工具箱包的克隆（同名资源），@pkg:style 会解析到 0x7F 上。
        val toolboxTheme = onModule.androidManifest.activityThemeAttr(OUR_LAUNCH_ACTIVITY)?.getData()
        assertTrue(onTheme != null && toolboxTheme != null && onTheme.getData() == toolboxTheme,
            "开启后游戏 Activity 主题应等于工具箱沉浸式主题 " +
                "${toolboxTheme?.let { "0x%08x".format(it) }}，实际 ${onTheme?.getData()?.let { "0x%08x".format(it) }}")
        assertTrue(onTheme.getData() != originalThemeId, "开启后主题不应还是游戏原主题")
        val onInfo = onModule.integratorInfo()
        assertEquals("0x%08x".format(originalThemeId), onInfo["originalGameTheme"],
            "描述文件应记录游戏原主题 id，供关闭时还原")
        assertEquals("com.target.game.MainActivity", onInfo["gameActivity"],
            "描述文件应记录游戏主 Activity 名，供更新模式在 LAUNCHER 被剥离后仍能定位")

        // ---- 第 2 步：更新模式，关闭沉浸式主题 ----
        val off = File(work, "out_theme_off.apk")
        val srcDexCount = ApkModule.loadApkFile(src).listDexFiles().size
        ToolboxApkMerger.apply(
            src, on, DexStrategy.INSERT_BEFORE, off,
            updateMode = true, dexStart = 0, dexEnd = srcDexCount - 1,
            insertMode = DexStrategy.INSERT_BEFORE, useImmersiveTheme = false
        )

        val offModule = ApkModule.loadApkFile(off)
        assertEquals(1, offModule.androidManifest.gameActivityThemeAttrs().size,
            "还原后游戏 Activity 只能有一条 android:theme 属性")
        val offTheme = offModule.androidManifest.gameActivityThemeAttr()
        assertTrue(offTheme != null, "关闭后游戏 Activity 应还原出 theme 属性（原本就有）")
        assertEquals(originalThemeId, offTheme.getData(),
            "关闭后应还原为游戏原主题 0x%08x，实际 0x%08x".format(originalThemeId, offTheme.getData()))

        println("[IMMERSIVE THEME toggle] OK -> ${off.absolutePath}")
    }

    /**
     * 回归测试：游戏主 Activity **原本没有** `android:theme` 时，开启沉浸式主题开关仍应注入工具箱主题。
     *
     * 对应需求「无论 Activity 是否有自己的主题都替换成我们的」的「无自带主题」分支：
     *  1. 首次集成 useImmersiveTheme=true  → 即便 Activity 原本无 theme，也加一条指向工具箱 Theme.DreamPvzApp
     *  2. 更新模式 useImmersiveTheme=false → 把注入的主题移除，回到「无 theme」的原状
     */
    @Test
    fun `开启沉浸式主题即使游戏Activity无自带主题也注入工具箱主题`() {
        val src = requireSource()
        val srcDexFirst = readDex(ApkModule.loadApkFile(src), 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        // 注意：不传 gameActivityThemeId → 目标 MainActivity 不带任何 android:theme
        val target = File(work, "target_theme_none.apk")
        buildTargetApk(target, srcDexFirst) // gameActivityThemeId 默认 null

        // 前置：目标 Activity 确实无 theme
        val preModule = ApkModule.loadApkFile(target)
        assertEquals(0, preModule.androidManifest.gameActivityThemeAttrs().size,
            "用例前提：目标 MainActivity 应无 android:theme")

        // ---- 第 1 步：开启沉浸式主题 ----
        val on = File(work, "out_theme_none_on.apk")
        ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, on, useImmersiveTheme = true)

        val onModule = ApkModule.loadApkFile(on)
        assertEquals(1, onModule.androidManifest.gameActivityThemeAttrs().size,
            "无自带主题时开启开关，应注入且只注入一条 android:theme")
        val onTheme = onModule.androidManifest.gameActivityThemeAttr()
        val toolboxTheme = onModule.androidManifest.activityThemeAttr(OUR_LAUNCH_ACTIVITY)?.getData()
        assertTrue(onTheme != null && toolboxTheme != null && onTheme.getData() == toolboxTheme,
            "注入的主题应等于工具箱沉浸式主题")
        // 无原主题 → 描述文件 originalGameTheme 应为空，供关闭时恢复成「无 theme」
        assertEquals("", onModule.integratorInfo()["originalGameTheme"],
            "无原主题时 originalGameTheme 应为空串")
        assertEquals("com.target.game.MainActivity", onModule.integratorInfo()["gameActivity"])

        // ---- 第 2 步：更新模式关闭沉浸式主题 → 回到「无 theme」 ----
        val off = File(work, "out_theme_none_off.apk")
        val srcDexCount = ApkModule.loadApkFile(src).listDexFiles().size
        ToolboxApkMerger.apply(
            src, on, DexStrategy.INSERT_BEFORE, off,
            updateMode = true, dexStart = 0, dexEnd = srcDexCount - 1,
            insertMode = DexStrategy.INSERT_BEFORE, useImmersiveTheme = false
        )
        val offModule = ApkModule.loadApkFile(off)
        assertEquals(0, offModule.androidManifest.gameActivityThemeAttrs().size,
            "关闭后应移除注入的主题，恢复「无 android:theme」原状")

        println("[IMMERSIVE THEME no-pretheme] OK -> ${off.absolutePath}")
    }

    /**
     * 回归测试：更新模式重复集成时，LAUNCHER_BLOCK_XML 注入的组件 / uses-library 不得重复累积。
     *
     * 修复前：[removePreviousToolboxEntries] 的移除白名单漏掉
     *  `androidx.startup.InitializationProvider`、
     *  `androidx.profileinstaller.ProfileInstallReceiver`、
     *  `com.kdroid.androidcontextprovider.ContextInitProvider`
     *  以及 uses-library `androidx.window.extensions` / `androidx.window.sidecar`，
     *  → 每次更新都往 manifest 追加一份，导致这些条目成倍增长。
     */
    @Test
    fun `更新模式重复集成注入组件不得重复累积`() {
        val src = requireSource()
        val srcDexFirst = readDex(ApkModule.loadApkFile(src), 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target_dup.apk")
        buildTargetApk(target, srcDexFirst)

        fun counts(apk: File): Map<String, Int> {
            val m = ApkModule.loadApkFile(apk).androidManifest
            val bag = mutableMapOf<String, Int>()
            m.listApplicationElementsByTag("provider").forEach { el ->
                el.attrValue("name")?.let { bag["provider:$it"] = (bag["provider:$it"] ?: 0) + 1 }
            }
            m.listApplicationElementsByTag("receiver").forEach { el ->
                el.attrValue("name")?.let { bag["receiver:$it"] = (bag["receiver:$it"] ?: 0) + 1 }
            }
            m.applicationElement?.getElements()?.forEach { child ->
                if (child.name == "uses-library") {
                    child.attrValue("name")?.let { bag["lib:$it"] = (bag["lib:$it"] ?: 0) + 1 }
                }
            }
            m.getActivities(true).forEach { act ->
                act.attrValue("name")?.let { bag["activity:$it"] = (bag["activity:$it"] ?: 0) + 1 }
            }
            return bag
        }

        val injected = listOf(
            "provider:androidx.core.content.FileProvider",
            "provider:com.petterp.floatingx.assist.FxContentProvider",
            "provider:androidx.startup.InitializationProvider",
            "provider:com.kdroid.androidcontextprovider.ContextInitProvider",
            "receiver:androidx.profileinstaller.ProfileInstallReceiver",
            "lib:androidx.window.extensions",
            "lib:androidx.window.sidecar",
            "activity:io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity",
        )

        // 第 1 次：首次集成（append 模式）
        val out1 = File(work, "out_dup_1.apk")
        ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, out1)
        val c1 = counts(out1)
        injected.forEach { assertEquals(1, c1[it] ?: 0, "首次集成后 $it 应为 1 条，实际 ${c1[it]}") }

        // 第 2 次：更新模式重复集成
        val out2 = File(work, "out_dup_2.apk")
        val srcDexCount = ApkModule.loadApkFile(src).listDexFiles().size
        ToolboxApkMerger.apply(
            src, out1, DexStrategy.INSERT_BEFORE, out2,
            updateMode = true, dexStart = 0, dexEnd = srcDexCount - 1,
            insertMode = DexStrategy.INSERT_BEFORE
        )
        val c2 = counts(out2)
        injected.forEach {
            assertEquals(1, c2[it] ?: 0, "更新模式重复集成后 $it 应仍为 1 条（不可累积），实际 ${c2[it]}")
        }

        println("[UPDATE no-dup] OK -> ${out2.absolutePath}")
    }

    /**
     * 回归测试：游戏主 Activity 位于【子包】（Activity 全限定名与 APK package 不同，
     * 如真实游戏 com.popcap.pvz2mgtz 的 com.popcap.pvz2cmhd.SexyAppActivity）时，
     * 开启沉浸式主题开关仍应正确把该游戏 Activity 的主题替换为工具箱主题。
     *
     * 此前所有用例都用同包 Activity（com.target.game.MainActivity），从未覆盖「子包 Activity 名」，
     * 而这是真实游戏 APK 的常态结构，正是用户报告「游戏 Activity 主题改不掉」的高风险路径。
     */
    @Test
    fun `开启沉浸式主题对子包游戏Activity也生效`() {
        val src = requireSource()
        val srcDexFirst = readDex(ApkModule.loadApkFile(src), 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target_subpkg.apk")
        buildSubPackageTarget(target, srcDexFirst, ANDROID_FRAMEWORK_STYLE_ID)

        // 前置：游戏 Activity 当前是框架主题（模拟游戏原主题）
        val preModule = ApkModule.loadApkFile(target)
        val originalThemeId = preModule.androidManifest
            .activityThemeAttr("com.popcap.pvz2cmhd.SexyAppActivity")?.getData()
            ?: error("前置：子包游戏 Activity 应有框架主题")
        assertTrue((originalThemeId ushr 24) and 0xFF == 0x01,
            "前置：原主题应属框架包 0x01，实际 0x%08x".format(originalThemeId))

        // 开启沉浸式主题（首次集成）
        val on = File(work, "out_subpkg_on.apk")
        ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, on, useImmersiveTheme = true)

        val onModule = ApkModule.loadApkFile(on)
        val onTheme = onModule.androidManifest.activityThemeAttr("com.popcap.pvz2cmhd.SexyAppActivity")
        val toolboxTheme = onModule.androidManifest.activityThemeAttr(OUR_LAUNCH_ACTIVITY)?.getData()
        assertTrue(onTheme != null && toolboxTheme != null && onTheme.getData() == toolboxTheme,
            "子包游戏 Activity 主题应等于工具箱沉浸式主题 " +
                "${toolboxTheme?.let { "0x%08x".format(it) }}，实际 ${onTheme?.getData()?.let { "0x%08x".format(it) }}")
        assertTrue(onTheme.getData() != originalThemeId, "开启后主题不应还是游戏原主题")

        println("[IMMERSIVE THEME subpkg] OK -> ${on.absolutePath}")
    }

    /**
     * 复现用户真实 bug：目标 APK 是「上次合并的产物」，其 manifest 里已经有一个
     * Pvz2InitializeActivity（工具箱启动 Activity），且它**也携带了 bejeweledblitz 深链 scheme**，
     * 并且排位在真正的游戏主 Activity 之前。
     *
     * 这种情况下 findGameActivity 的「优先级1：首个带 bejeweledblitz scheme 的 Activity」会命中
     * 工具箱自己的 Pvz2InitializeActivity，把「游戏主 Activity」误判成工具箱自身 → 沉浸式主题
     * 打到错误的 Activity 上，真正的游戏主 Activity 主题纹丝不动。
     *
     * 修复后：优先级1 必须跳过工具箱自身 Activity（OUR_ACTIVITY_NAMES），才能正确命中游戏主 Activity。
     * 本用例断言：合并后【游戏主 Activity】的主题确实变成了工具箱沉浸式主题（0x66 包引用），
     * 且等于新注入的 Pvz2InitializeActivity 主题。
     */
    @Test
    fun `沉浸式主题在工具箱Activity也带bejeweledblitz时只作用于游戏Activity`() {
        val src = requireSource()
        val srcDexFirst = readDex(ApkModule.loadApkFile(src), 0)

        val work = File(moduleDir, "build/tmp/integrator-test")
        work.mkdirs()
        val target = File(work, "target_stale_merged.apk")
        buildStaleMergedTarget(target, srcDexFirst, ANDROID_FRAMEWORK_STYLE_ID)

        // 前置：游戏主 Activity 当前是框架主题（模拟游戏原主题）
        val preModule = ApkModule.loadApkFile(target)
        val originalThemeId = preModule.androidManifest
            .activityThemeAttr("com.popcap.pvz2cmhd.SexyAppActivity")?.getData()
            ?: error("前置：游戏主 Activity 应有框架主题")

        // 首合并不清理旧条目（复刻用户「对已是合并产物的 APK 再次合并」的场景）
        val on = File(work, "out_stale_merged_on.apk")
        ToolboxApkMerger.apply(src, target, DexStrategy.INSERT_BEFORE, on, useImmersiveTheme = true)

        val onModule = ApkModule.loadApkFile(on)
        // 关键断言：游戏主 Activity 必须被换成工具箱沉浸式主题，而非保持原框架主题。
        // 注：本用例的 target 是由源 APK 改名（0x66→0x7f）构造的，其 0x7f 包也含 Theme.DreamPvzApp，
        // 解析 @OUR_PKG_NAME:style/Theme.DreamPvzApp 时会被「当前包 0x7f」抢先命中，得到 0x7f11012f；
        // 真实游戏 APK 的 0x7f 包不含该资源，会正确落到工具箱 0x66 包（见真实目标测试 0x6611012f）。
        // 所以这里只校验「确被换成沉浸式主题（引用型且≠原框架主题）」+「与工具箱启动 Activity 主题一致」。
        val gameTheme = onModule.androidManifest.activityThemeAttr("com.popcap.pvz2cmhd.SexyAppActivity")
            ?: error("合并后游戏主 Activity 应带 theme")
        val gameThemeId = gameTheme.getData()
        assertTrue((gameThemeId ushr 24) and 0xFF != 0x01,
            "游戏主 Activity 主题不应还是框架包 0x01，实际 0x%08x".format(gameThemeId))
        assertTrue(gameThemeId != originalThemeId,
            "合并后游戏主 Activity 主题不应还是游戏原主题 0x%08x".format(originalThemeId))

        // 且应等于新注入的（末尾那个）Pvz2InitializeActivity 主题
        val newToolboxTheme = onModule.androidManifest.lastPvz2InitializeActivityTheme()
            ?: error("应存在新注入的 Pvz2InitializeActivity")
        assertEquals(newToolboxTheme, gameThemeId,
            "游戏主 Activity 主题应等于工具箱启动 Activity 主题")
    }

    /** 造一份「已是合并产物」的 target：manifest 里工具箱 Pvz2InitializeActivity（带 bejeweledblitz）排在游戏 Activity 之前 */
    private fun buildStaleMergedTarget(out: File, sourceDexFirst: ByteArray, gameActivityThemeId: Int) {
        val module = ApkModule.loadApkFile(sourceApk)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.popcap.pvz2mgtz")
        module.setManifest(newStaleMergedManifest(module))
        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        val targetDex = sourceDexFirst.copyOf()
            .apply { this[lastIndex] = (this[lastIndex].toInt() xor 0xFF).toByte() }
        module.add(ByteInputSource(targetDex, "classes.dex"))
        module.writeApk(out)
        injectGameActivityThemeByName(out, gameActivityThemeId, "com.popcap.pvz2cmhd.SexyAppActivity")
    }

    /** 同 newSubPackageManifest，但额外在游戏 Activity 之前加入一个「带 bejeweledblitz scheme 的工具箱 Pvz2InitializeActivity」，模拟已是合并产物的 APK */
    private fun newStaleMergedManifest(module: ApkModule): AndroidManifestBlock {
        val manifest = AndroidManifestBlock()
        manifest.setPackageBlock(module.tableBlock.listPackages().first { it.id == 0x7F })
        manifest.setApkFile(module)
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.popcap.pvz2mgtz"
                android:versionCode="1"
                android:versionName="1.0"
                android:minSdkVersion="21"
                android:targetSdkVersion="30">
                <application android:label="GameApp">
                    <activity android:name="io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.VIEW" />
                            <data android:scheme="com.sexyactioncool.bejeweledblitz" />
                            <category android:name="android.intent.category.DEFAULT" />
                            <category android:name="android.intent.category.BROWSABLE" />
                        </intent-filter>
                    </activity>
                    <activity android:name="com.popcap.pvz2cmhd.SexyAppActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                        <intent-filter>
                            <action android:name="android.intent.action.VIEW" />
                            <data android:scheme="com.sexyactioncool.bejeweledblitz" />
                            <category android:name="android.intent.category.DEFAULT" />
                            <category android:name="android.intent.category.BROWSABLE" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
        val parser = KXmlParser()
        parser.setInput(StringReader(xml))
        manifest.parse(parser)
        manifest.removeElementsIf { it.name != "manifest" }
        check(manifest.documentElement.name == "manifest") {
            "stale-merged 目标 manifest 根元素应为 <manifest>，实际 <${manifest.documentElement.name}>"
        }
        return manifest
    }

    /** 取【最后一个】Pvz2InitializeActivity 的 theme id（appendLauncherBlock 追加在末尾，即本次新注入的那个） */
    private fun AndroidManifestBlock.lastPvz2InitializeActivityTheme(): Int? {
        var result: Int? = null
        getActivities(true).asSequence().forEach { act ->
            if (act.attrValue("name") == OUR_LAUNCH_ACTIVITY) {
                act.getAttributes().asSequence()
                    .firstOrNull { it.getName() == "theme" }
                    ?.let { result = it.getData() }
            }
        }
        return result
    }

    /** 造一份「子包游戏」target：APK 包名 com.popcap.pvz2mgtz，游戏 Activity 在子包 com.popcap.pvz2cmhd 下 */
    private fun buildSubPackageTarget(out: File, sourceDexFirst: ByteArray, gameActivityThemeId: Int) {
        val module = ApkModule.loadApkFile(sourceApk)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.popcap.pvz2mgtz")
        module.setManifest(newSubPackageManifest(module))
        module.listDexFiles().forEach { module.removeInputSource(it.name) }
        val targetDex = sourceDexFirst.copyOf()
            .apply { this[lastIndex] = (this[lastIndex].toInt() xor 0xFF).toByte() }
        module.add(ByteInputSource(targetDex, "classes.dex"))
        module.writeApk(out)
        injectGameActivityThemeByName(out, gameActivityThemeId, "com.popcap.pvz2cmhd.SexyAppActivity")
    }

    /** 与 newGameManifest 同款，但游戏 Activity 用子包全限定名 + bejeweledblitz scheme，模拟真实游戏 */
    private fun newSubPackageManifest(module: ApkModule): AndroidManifestBlock {
        val manifest = AndroidManifestBlock()
        manifest.setPackageBlock(module.tableBlock.listPackages().first { it.id == 0x7F })
        manifest.setApkFile(module)
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.popcap.pvz2mgtz"
                android:versionCode="1"
                android:versionName="1.0"
                android:minSdkVersion="21"
                android:targetSdkVersion="30">
                <application android:label="GameApp">
                    <activity android:name="com.popcap.pvz2cmhd.SexyAppActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                        <intent-filter>
                            <action android:name="android.intent.action.VIEW" />
                            <data android:scheme="com.sexyactioncool.bejeweledblitz" />
                            <category android:name="android.intent.category.DEFAULT" />
                            <category android:name="android.intent.category.BROWSABLE" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
        val parser = KXmlParser()
        parser.setInput(StringReader(xml))
        manifest.parse(parser)
        manifest.removeElementsIf { it.name != "manifest" }
        check(manifest.documentElement.name == "manifest") {
            "子包目标 manifest 根元素应为 <manifest>，实际 <${manifest.documentElement.name}>"
        }
        return manifest
    }

    private fun injectGameActivityThemeByName(apk: File, themeId: Int, activityName: String) {
        val module = ApkModule.loadApkFile(apk)
        val act = module.androidManifest.getActivities(true).asSequence()
            .firstOrNull { it.attrValue("name") == activityName }
            ?: error("目标 APK 内找不到 $activityName，无法注入 theme")
        val attr = act.newAttribute()
        attr.setNamespace("http://schemas.android.com/apk/res/android", "android")
        attr.setName("theme", 0x01010000)
        attr.setValueAsResourceId(themeId)
        val tmp = File(apk.parentFile, apk.name + ".tmp")
        module.writeApk(tmp)
        module.close()
        check(apk.delete() && tmp.renameTo(apk)) { "回写目标 APK 失败：${apk.absolutePath}" }
    }

    /**
     * 仅本地复现的诊断：加载真实游戏 APK，列出所有带 bejeweledblitz scheme 的 Activity，
     * 复刻 findGameActivity 优先级1（首个带该 scheme 的 Activity）会命中的是哪个。
     * 若命中者不是真正的游戏主 Activity，则沉浸式主题会被打到错误 Activity 上。
     */
    @Test
    fun `诊断真实APK游戏Activity定位`() {
        val realApk = File("/Users/macbookpro/Downloads/植物大战僵尸2迷宫拓展版.apk")
        if (!realApk.exists()) return  // 仅本地复现，CI 跳过
        val module = ApkModule.loadApkFile(realApk)
        val tm = module.androidManifest
        val schemeActs = tm.getActivities(true).asSequence().filter { act ->
            act.getElements().asSequence().any { f ->
                f.name == "intent-filter" && f.getElements().asSequence().any { d ->
                    d.name == "data" && d.attrValue("scheme") == "com.sexyactioncool.bejeweledblitz"
                }
            }
        }.mapNotNull { it.attrValue("name") }.toList()
        println("[DIAG] 带 bejeweledblitz scheme 的 Activity 列表: $schemeActs")
        println("[DIAG] findGameActivity 优先级1 会命中: ${schemeActs.firstOrNull()}")
        // 真正的游戏主 Activity 主题
        val real = tm.activityThemeAttr("com.popcap.pvz2cmhd.SexyAppActivity")?.getData()
        val init = tm.activityThemeAttr(OUR_LAUNCH_ACTIVITY)?.getData()
        println("[DIAG] SexyAppActivity theme=0x%08x, Pvz2InitializeActivity theme=0x%08x".format(real ?: 0, init ?: 0))
    }

    /** 工具箱注入的启动 Activity 全限定名（其主题即 Theme.DreamPvzApp） */
    private val OUR_LAUNCH_ACTIVITY = "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity"

    /** 取指定 Activity 上全部 android:theme 属性（用于检测重复注入） */
    private fun AndroidManifestBlock.activityThemeAttrs(activityName: String): List<ResXmlAttribute> {
        val act = getActivities(true).asSequence()
            .firstOrNull { it.attrValue("name") == activityName } ?: return emptyList()
        return act.getAttributes().asSequence().filter { it.getName() == "theme" }.toList()
    }

    private fun AndroidManifestBlock.activityThemeAttr(activityName: String): ResXmlAttribute? =
        activityThemeAttrs(activityName).firstOrNull()

    private fun AndroidManifestBlock.gameActivityThemeAttrs(): List<ResXmlAttribute> =
        activityThemeAttrs("com.target.game.MainActivity")

    /** 取游戏 MainActivity 上的 android:theme 属性（找不到返回 null） */
    private fun AndroidManifestBlock.gameActivityThemeAttr(): ResXmlAttribute? =
        gameActivityThemeAttrs().firstOrNull()

    /** 读取产物 APK 内的集成描述文件为 key→value */
    private fun ApkModule.integratorInfo(): Map<String, String> =
        getInputSource("assets/pvz2tool/integrator_info.txt").openStream().readBytes()
            .toString(Charsets.UTF_8).lineSequence()
            .mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
            }.toMap()

    private fun buildTargetApkWithDexes(out: File, baseDex: ByteArray, dexCount: Int) {
        val module = ApkModule.loadApkFile(sourceApk)
        val table = module.tableBlock
        val pkg = table.listPackages().first { it.id == 0x66 }
        pkg.setId(0x7F)
        pkg.setName("com.target.game")

        module.setManifest(newGameManifest(module, pkg))

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
