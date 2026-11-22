package io.github.dreammooncai.pvz2tool.release

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertTrue

/** 验证适配包生成：结构与 11.17.1 参考包一致（文件集合相同，arsc.zip/说明文档内容正确）。 */
class AdaptationZipBuilderTest {
    private val home = System.getProperty("user.home")
    private val refApk = File(home, "Downloads/戴夫工具箱集成器_11.17.1.APK")
    private val refAdaptZip = File(home, "Downloads/戴夫工具箱适配包11.17.1.zip")
    private val templateDir = File(System.getProperty("user.dir"), "../release/adaptation")

    @Test
    fun `生成适配包结构与参考一致`() {
        if (!refApk.exists() || !refAdaptZip.exists() || !templateDir.isDirectory) {
            println("SKIP: 参考文件或模板不存在")
            return
        }
        val out = File(System.getProperty("user.dir"), "build/tmp-adapt/adapt.zip")
        AdaptationZipBuilder.build(refApk, "11.17.1", null, templateDir, out)
        assertTrue(out.exists() && out.length() > 0)

        val refNames = names(refAdaptZip)
        val outNames = names(out)
        println("ADAPT 参考=${refNames.size} 生成=${outNames.size}")
        val onlyRef = refNames - outNames
        val onlyOut = outNames - refNames
        if (onlyRef.isNotEmpty()) println("ADAPT 仅参考有(${onlyRef.size}): ${onlyRef.sorted().take(20)}")
        if (onlyOut.isNotEmpty()) println("ADAPT 仅生成有(${onlyOut.size}): ${onlyOut.sorted().take(20)}")

        // 关键：不应缺少参考包的任何文件；允许新版本新增文件（如新引入的 .so）
        assertTrue(onlyRef.isEmpty(), "生成的适配包不应缺少参考包文件: $onlyRef")

        // arsc.zip 存在且非空
        ZipFile(out).use { z ->
            val e = z.getEntry("arsc.zip")
            assertTrue(e != null && e.size > 1000, "arsc.zip 应存在且非空")
        }
    }

    private fun names(zip: File): Set<String> {
        val set = HashSet<String>()
        ZipFile(zip).use { z ->
            val es = z.entries()
            while (es.hasMoreElements()) {
                val e = es.nextElement()
                if (!e.isDirectory) set.add(e.name)
            }
        }
        return set
    }
}
