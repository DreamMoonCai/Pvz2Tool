package io.github.dreammooncai.integration

import com.reandroid.apk.ApkModule
import io.github.dreammooncai.integration.ToolboxApkMerger.DexStrategy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 回归测试：合并「manifest 的 ResXmlIDMap 覆盖整个字符串池」的游戏包（如 PVZ2 高清版）时，
 * ARSCLib 1.4.0 在 writeApk → refresh → sort 阶段会抛
 * `IllegalStateException: Resource id string item is already linked`，导致打包失败。
 * [ToolboxApkMerger] 在写盘前全量解绑 ResXmlString 的 ResXmlID 链接（unlinkAllManifestResXmlIds）修复此问题。
 *
 * 目标 APK 不随仓库分发；放在约定路径时执行真实验证，否则跳过（不算失败）。
 */
class ToolboxApkMergerLinkedIdTest {
    private val sourceApk = File(System.getProperty("user.dir"), "build/outputs/apk/debug/composeApp-debug.apk")
    private val targetApk = run {
        val env = System.getenv("INTEGRATOR_TARGET_APK")
        if (!env.isNullOrBlank()) File(env)
        else File(System.getProperty("user.home"), "Downloads/植物大战僵尸2高清版_1.7.7.apk.1")
    }

    @Test
    fun `合并带畸形 idMap 的 manifest 不抛 already linked 且产物可重载`() {
        if (!targetApk.exists() || !sourceApk.exists()) {
            println("SKIP: 目标 APK 或源 APK 不存在（${targetApk} / ${sourceApk}）")
            return
        }
        val work = File(System.getProperty("user.dir"), "build/tmp-linked-id-test")
        work.mkdirs()
        val out = File(work, "merged.apk")
        ToolboxApkMerger.apply(
            sourceApk = sourceApk,
            targetApk = targetApk,
            dexStrategy = DexStrategy.INSERT_BEFORE,
            outApk = out,
            includeExamples = false,
            useImmersiveTheme = true,
            version = "LINKED_ID_TEST"
        )
        assertTrue(out.exists() && out.length() > 0, "产物 APK 应存在且非空")

        // 关键：重新加载产物，确认 manifest 关键属性能解析（idMap 语义未被解绑破坏）
        val m = ApkModule.loadApkFile(out)
        try {
            val tm = m.androidManifest
            assertTrue(tm.getPackageName()?.startsWith("com.popcap.pvz2") == true,
                "产物包名应保持游戏包，实际 ${tm.getPackageName()}")
            var nameAttrs = 0
            val ai = tm.getActivities(true).iterator()
            while (ai.hasNext()) {
                val attrs = ai.next().getAttributes()
                while (attrs.hasNext()) {
                    if (attrs.next().getName() == "name") nameAttrs++
                }
            }
            assertTrue(nameAttrs > 0, "产物的 Activity 应能解析出 android:name（idMap 语义完好）")
        } finally {
            m.close()
        }
    }
}
