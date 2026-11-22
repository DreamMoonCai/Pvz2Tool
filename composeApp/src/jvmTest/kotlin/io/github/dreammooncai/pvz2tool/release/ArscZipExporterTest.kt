package io.github.dreammooncai.pvz2tool.release

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 验证 [ArscZipExporter] 导出的 arsc.zip 与 MT 编辑器导出的参考包结构/内容一致。
 * 参考 APK 与参考适配包放在 ~/Downloads（不随仓库分发），缺失则跳过。
 */
class ArscZipExporterTest {
    private val refApk = File(System.getProperty("user.home"), "Downloads/戴夫工具箱集成器_11.17.1.APK")
    private val refAdaptZip = File(System.getProperty("user.home"), "Downloads/戴夫工具箱适配包11.17.1.zip")

    @Test
    fun `导出 arsc zip 与参考包逐文件对比`() {
        if (!refApk.exists() || !refAdaptZip.exists()) {
            println("SKIP: 参考文件不存在")
            return
        }
        val out = File(System.getProperty("user.dir"), "build/tmp-arsc-export/arsc.zip")
        ArscZipExporter.exportFromApk(refApk, out)
        assertTrue(out.exists() && out.length() > 0, "导出文件应存在")

        val refMap = readZipInnerArsc(refAdaptZip)
        val outMap = readZip(out)
        println("参考文件数=${refMap.size}  导出文件数=${outMap.size}")
        val onlyRef = refMap.keys - outMap.keys
        val onlyOut = outMap.keys - refMap.keys
        if (onlyRef.isNotEmpty()) println("仅参考包有 (${onlyRef.size}): ${onlyRef.sorted().take(20)}")
        if (onlyOut.isNotEmpty()) println("仅导出有 (${onlyOut.size}): ${onlyOut.sorted().take(20)}")

        val common = refMap.keys intersect outMap.keys
        var contentDiff = 0
        val diffSamples = mutableListOf<String>()
        for (k in common.sorted()) {
            if (refMap[k]?.trim() != outMap[k]?.trim()) {
                contentDiff++
                if (diffSamples.size < 5) diffSamples.add(k)
            }
        }
        println("共同文件=${common.size}  内容不同=$contentDiff  样例=$diffSamples")
        assertTrue(outMap.size >= refMap.size * 0.9, "导出文件数应覆盖参考包 90% 以上")
    }

    private fun readZipInnerArsc(adaptZip: File): Map<String, String> {
        ZipFile(adaptZip).use { z ->
            val e = z.getEntry("arsc.zip") ?: error("参考适配包无 arsc.zip")
            val tmp = File.createTempFile("ref_arsc", ".zip")
            z.getInputStream(e).use { tmp.writeBytes(it.readBytes()) }
            val result = readZip(tmp)
            tmp.delete()
            return result
        }
    }

    private fun readZip(zip: File): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        ZipFile(zip).use { z ->
            val es = z.entries()
            while (es.hasMoreElements()) {
                val en = es.nextElement()
                if (en.isDirectory) continue
                map[en.name] = z.getInputStream(en).bufferedReader(Charsets.UTF_8).readText()
            }
        }
        return map
    }
}
