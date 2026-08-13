package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.value.ValueType
import io.github.dreammooncai.integration.ToolboxApkMerger.DexStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

/**
 * 真实目标 APK 离线合并验证（不依赖设备）。
 *
 * 目标 APK 路径取自环境变量 INTEGRATOR_TARGET_APK，缺省回退到 ~/Downloads/原版.apk。
 * 源 APK = 本应用 debug 产物（build/outputs/apk/debug/composeApp-debug.apk，资源包 id 0x66）。
 * 产物 = 未签名集成 APK（INTEGRATOR_OUT_APK 或 build/integrator-out/<目标名>_pvz2tool.apk）。
 *
 * 若目标 APK 不存在则 assumeTrue 跳过，避免污染常规 jvmTest。
 */
class ToolboxApkMergerRealTargetTest {

    private val moduleDir = File(System.getProperty("user.dir"))
    private val sourceApk = File(moduleDir, "build/outputs/apk/debug/composeApp-debug.apk")
    private val targetApk = run {
        val env = System.getenv("INTEGRATOR_TARGET_APK")
        if (!env.isNullOrBlank()) File(env)
        else File(System.getProperty("user.home"), "Downloads/原版.apk")
    }
    private val outApk = run {
        val env = System.getenv("INTEGRATOR_OUT_APK")
        if (!env.isNullOrBlank()) File(env)
        else File(moduleDir, "build/integrator-out/${targetApk.nameWithoutExtension}_pvz2tool.apk")
    }

    @Test
    fun `apply INSERT_BEFORE 用真实目标游戏 APK 产出未签名集成包`() {
        if (!sourceApk.exists()) {
            println("跳过：源 APK 不存在 ${sourceApk.absolutePath}，请先 ./gradlew :composeApp:assembleDebug")
            return
        }
        if (!targetApk.exists()) {
            println("跳过：目标 APK 不存在 ${targetApk.absolutePath}，请设置 INTEGRATOR_TARGET_APK 或放到 ~/Downloads/原版.apk")
            return
        }

        val srcModule = ApkModule.loadApkFile(sourceApk)
        val srcDexCount = srcModule.listDexFiles().size
        val tgtModule = ApkModule.loadApkFile(targetApk)
        val tgtDexCount = tgtModule.listDexFiles().size
        val tgtPackage = tgtModule.androidManifest.packageName ?: "?"

        println("[真实目标] 源 dex=$srcDexCount, 目标 dex=$tgtDexCount, 目标包名=$tgtPackage, 目标大小=${"%.1f".format(targetApk.length() / 1024f / 1024f)}MB")

        val result = ToolboxApkMerger.apply(sourceApk, targetApk, DexStrategy.INSERT_BEFORE, outApk)

        // 1. 产物可重加载、含 0x66 包
        val reloaded = ApkModule.loadApkFile(result.outputApk)
        assertTrue(reloaded.tableBlock.listPackages().any { it.id == 0x66 },
            "合并后 arsc 应含 0x66（工具箱资源包）")

        // 2. manifest 含 Pvz2InitializeActivity，且游戏 LAUNCHER 已移除。
        // 用元素树遍历（而非 serializeToXml）：重载后的 manifest 在未挂接 table 时 serializeToXml 会因
        // 引用属性无法解析成名称而 NPE，但产物二进制本身正确（MT/aapt 直接读二进制无此问题）。
        val table = reloaded.tableBlock

        // 2a. 直接遍历 <application> 子元素计数（抓「产物里真的有两个同名组件」的重复 bug）。
        // 不能用 getActivities + 集合去重：ARSCLib getActivities(true) 偶发把同一活动返回两次，
        // 集合去重会掩盖 appendLauncherBlock 剥离循环非原子(parseInnerNodes 先 add 再 parse、失败不回滚)
        // 导致的真实重复 —— 之前正是这个掩盖让「两个 Pvz2InitializeActivity」漏网。
        val app = reloaded.androidManifest.getOrCreateApplicationElement()
        val pvzInitCount = countChildActivity(app, table, "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity")
        println("[真实目标] Pvz2InitializeActivity 元素计数=$pvzInitCount")
        assertEquals(1, pvzInitCount,
            "Pvz2InitializeActivity 应恰好 1 个，实际 $pvzInitCount（剥离循环非原子会导致重复）")

        // 2b. 启动器活动名集合（容忍 getActivities 重复返回，但确保只有一种启动器活动名）
        val activities = reloaded.androidManifest.getActivities(true).asSequence().toList()
        val launcherNames = activities.filter { it.hasMainLauncher(table) }
            .map { it.androidName(table) }.toSet()
        println("[真实目标] MAIN+LAUNCHER 活动名集合=$launcherNames (activities.size=${activities.size})")
        assertEquals(setOf("io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity"), launcherNames,
            "合并后启动器应仅有 Pvz2InitializeActivity，实际 $launcherNames")

        // 2c. 注入的关键组件不能丢失（剥离循环失败时后半段会被静默丢弃，故逐个校验存在）
        val mustExist = listOf(
            "androidx.core.content.FileProvider",
            "io.github.dreammooncai.pvz2tool.service.LocalVpnService",
            "com.petterp.floatingx.assist.FxContentProvider",
            "androidx.startup.InitializationProvider",
            "androidx.profileinstaller.ProfileInstallReceiver",
            "com.kdroid.androidcontextprovider.ContextInitProvider"
        )
        val missing = mustExist.filter { !hasChildComponentNamed(app, table, it) }
        assertTrue(missing.isEmpty(), "以下注入组件在产物中丢失：$missing（剥离循环回滚失败）")

        // 2b. 我们注入的 0x66 引用（theme/label/resource）必须指向产物 0x66 包中真实存在的资源，
        // 否则运行时 app 找不到主题/标签/file_paths 会崩。
        val bad66 = mutableListOf<String>()
        reloaded.androidManifest.getManifestElement()?.walkRefs { a ->
            if (a.getValueType() == ValueType.REFERENCE) {
                val resid = a.getData()
                if ((resid ushr 24) and 0xFF == 0x66 && table.getResource(resid) == null) {
                    bad66.add("0x%08X".format(resid))
                }
            }
        }
        assertTrue(bad66.isEmpty(), "以下 0x66 引用在产物中不存在：$bad66")

        // 2c-2. 沉浸式主题：游戏主 Activity（带 bejeweledblitz scheme）的主题应等于工具箱启动 Activity 主题。
        // 这是用户报告「游戏 Activity 主题改不掉」的直接断言。
        val gameAct = reloaded.androidManifest.getActivities(true).asSequence().firstOrNull { act ->
            act.getElements().asSequence().any { f ->
                f.name == "intent-filter" && f.getElements().asSequence().any { d ->
                    d.name == "data" && d.refOrString("scheme", table).contains("bejeweledblitz", ignoreCase = true)
                }
            }
        }
        val gameTheme = gameAct?.getAttributeTheme(table)
        val toolboxTheme = reloaded.androidManifest.getActivities(true).asSequence()
            .firstOrNull { it.androidName(table) == "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity" }
            ?.getAttributeTheme(table)
        println("[真实目标] 游戏Activity主题=0x%08X, 工具箱启动Activity主题=0x%08X".format(gameTheme ?: 0, toolboxTheme ?: 0))
        assertTrue(gameTheme != null && toolboxTheme != null && gameTheme == toolboxTheme,
            "游戏主 Activity 主题应等于工具箱沉浸式主题（0x%08X），实际 0x%08X".format(toolboxTheme ?: 0, gameTheme ?: 0))

        // 3. targetSdkVersion >= 21
        val ts = reloaded.androidManifest.targetSdkVersion ?: 0
        assertTrue(ts >= 21, "targetSdkVersion 应 >= 21，当前 $ts")

        // 4. dex 总数 = 源 + 目标
        val resultCount = reloaded.listDexFiles().size
        assertEquals(srcDexCount + tgtDexCount, resultCount,
            "应合并为 ${srcDexCount + tgtDexCount} 个 dex（实际 $resultCount）")

        // 5. 报告统计合理
        assertTrue(result.report.arscTargetPackagesAfter.any { it.contains("0x66") })

        println("[真实目标] ✅ 合并完成")
        println("  输出 APK：${outApk.absolutePath}")
        println("  大小：${"%.1f".format(outApk.length() / 1024f / 1024f)}MB")
        println("  dex：$srcDexCount(源) + $tgtDexCount(目标) = $resultCount")
        println("  res 新增=${result.report.resAdded} 覆盖=${result.report.resOverwritten}")
        println("  assets 新增=${result.report.assetsAdded}")
        println("  lib ABI=${result.report.libAbis}")
        println("  dream.yml 新增=${result.report.dreamYmlAdded} 跳过=${result.report.dreamYmlSkipped}")
        println("  arsc 合并后包：${result.report.arscTargetPackagesAfter}")
        println("  👉 请用 MT 管理器对该 APK 签名后安装。")
    }

    /**
     * 真实目标「更新模式」沉浸式主题跟随验证（用户报告「游戏 Activity 主题改不掉」的权威复现）。
     *
     * 真实目标若已含 integrator_info.txt（即已是合并产物），按更新模式重合并：
     *   - 读取旧 dex 区间作为 dexStart/dexEnd
     *   - updateMode=true, useImmersiveTheme=true
     * 否则按首次集成（useImmersiveTheme=true）。
     * 断言：游戏主 Activity（带 bejeweledblitz scheme）的主题 == 工具箱启动 Activity 主题。
     */
    @Test
    fun `真实目标更新模式沉浸式主题跟随新工具箱`() {
        if (!sourceApk.exists()) { println("跳过：源 APK 不存在 ${sourceApk.absolutePath}"); return }
        if (!targetApk.exists()) { println("跳过：目标 APK 不存在 ${targetApk.absolutePath}"); return }

        val prev = ToolboxApkMerger.detectIntegratorInfo(targetApk)
        println("[真实目标-更新] prevInfo=$prev")
        val srcDexCount = ApkModule.loadApkFile(sourceApk).listDexFiles().size
        val out = File(moduleDir, "build/integrator-out/${targetApk.nameWithoutExtension}_update_pvz2tool.apk")

        if (prev != null) {
            ToolboxApkMerger.apply(
                sourceApk, targetApk, DexStrategy.INSERT_BEFORE, out,
                updateMode = true, dexStart = prev.dexStart, dexEnd = prev.dexEnd,
                insertMode = DexStrategy.INSERT_BEFORE, useImmersiveTheme = true
            )
        } else {
            ToolboxApkMerger.apply(sourceApk, targetApk, DexStrategy.INSERT_BEFORE, out, useImmersiveTheme = true)
        }

        val reloaded = ApkModule.loadApkFile(out)
        val table = reloaded.tableBlock
        // 复用与 findGameActivity 一致的「跳过工具箱自身 Activity」逻辑，避免命中旧版合并残留的、
        // 同样带 bejeweledblitz scheme 的 Pvz2InitializeActivity（否则会误把工具箱 Activity 当游戏主 Activity）。
        val toolboxNames = setOf(
            "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity",
            "io.github.dreammooncai.pvz2tool.RestartPhoenixActivity"
        )
        val gameAct = reloaded.androidManifest.getActivities(true).asSequence().firstOrNull { act ->
            val n = act.androidName(table)
            n !in toolboxNames && act.getElements().asSequence().any { f ->
                f.name == "intent-filter" && f.getElements().asSequence().any { d ->
                    d.name == "data" && d.refOrString("scheme", table).contains("bejeweledblitz", ignoreCase = true)
                }
            }
        }
        val gameTheme = gameAct?.getAttributeTheme(table)
        // 取【最后一个】Pvz2InitializeActivity 的主题：appendLauncherBlock 在 <application> 末尾追加，
        // 新注入的启动 Activity 才是本次合并写入的正确 0x66 资源 id；若目标 APK 已是旧版合并产物
        // （旧 Pvz2InitializeActivity 仍带 bejeweledblitz 排在前），取第一个会命中旧 id 导致「假绿」。
        val toolboxTheme = reloaded.androidManifest.getActivities(true).asSequence()
            .filter { it.androidName(table) == "io.github.dreammooncai.pvz2tool.Pvz2InitializeActivity" }
            .lastOrNull()
            ?.getAttributeTheme(table)
        println("[真实目标-更新] 游戏Activity主题=0x%08X, 工具箱启动Activity主题(末)=0x%08X".format(gameTheme ?: 0, toolboxTheme ?: 0))
        assertEquals(toolboxTheme, gameTheme,
            "更新模式后游戏主 Activity 主题应等于工具箱沉浸式主题（0x%08X），实际 0x%08X".format(toolboxTheme ?: 0, gameTheme ?: 0))
    }
}

// ---- 测试辅助 ----

private fun ResXmlElement.getAttributeTheme(table: TableBlock): Int? {
    val it: Iterator<ResXmlAttribute> = getAttributes()
    while (it.hasNext()) {
        val a = it.next()
        if (a.getName() == "theme" && a.getValueType() == ValueType.REFERENCE) return a.getData()
    }
    return null
}

private val ResXmlElement.name: String get() = getName() ?: ""

private fun ResXmlElement.androidName(table: TableBlock): String {
    val it: Iterator<ResXmlAttribute> = getAttributes()
    while (it.hasNext()) {
        val a = it.next()
        if (a.getName() == "name") {
            return if (a.getValueType() == ValueType.REFERENCE) {
                table.getResource(a.getData())?.name?.toString() ?: ""
            } else {
                a.getValueAsString() ?: ""
            }
        }
    }
    return ""
}

private fun ResXmlElement.refOrString(localName: String, table: TableBlock): String {
    val it: Iterator<ResXmlAttribute> = getAttributes()
    while (it.hasNext()) {
        val a = it.next()
        if (a.getName() == localName) {
            return if (a.getValueType() == ValueType.REFERENCE) {
                table.getResource(a.getData())?.name?.toString() ?: ""
            } else {
                a.getValueAsString() ?: ""
            }
        }
    }
    return ""
}

private fun ResXmlElement.hasMainLauncher(table: TableBlock): Boolean {
    // Android 启动器语义：必须在【同一个 intent-filter】内同时含 MAIN + LAUNCHER。
    // 不能跨 filter 累积（否则一个有 MAIN 无 LAUNCHER、另一个有 LAUNCHER 无 MAIN 的活动会被误判）。
    getElements().forEach { filter ->
        if (filter.name != "intent-filter") return@forEach
        var main = false
        var launcher = false
        filter.getElements().forEach { child ->
            when (child.name) {
                "action" -> if (child.refOrString("name", table).contains("main", ignoreCase = true)) main = true
                "category" -> if (child.refOrString("name", table).contains("launcher", ignoreCase = true)) launcher = true
            }
        }
        if (main && launcher) return true
    }
    return false
}

private fun ResXmlElement.walkRefs(block: (ResXmlAttribute) -> Unit) {
    val attrs: Iterator<ResXmlAttribute> = getAttributes()
    while (attrs.hasNext()) block(attrs.next())
    val children: Iterator<ResXmlElement> = getElements()
    while (children.hasNext()) children.next().walkRefs(block)
}

// 直接遍历 <application> 直接子元素，统计指定 android:name 的 activity 数量。
// 不走 getActivities（它可能重复返回/去重），直接数元素树 = 产物二进制真实情况。
private fun countChildActivity(app: ResXmlElement, table: TableBlock, activityName: String): Int {
    var count = 0
    val it = app.getElements()
    while (it.hasNext()) {
        val e = it.next()
        if (e.name == "activity" && e.androidName(table) == activityName) count++
    }
    return count
}

// 校验 <application> 直接子元素中是否存在指定 android:name 的组件（activity/service/provider/receiver）
private fun hasChildComponentNamed(app: ResXmlElement, table: TableBlock, componentName: String): Boolean {
    val it = app.getElements()
    while (it.hasNext()) {
        if (it.next().androidName(table) == componentName) return true
    }
    return false
}
