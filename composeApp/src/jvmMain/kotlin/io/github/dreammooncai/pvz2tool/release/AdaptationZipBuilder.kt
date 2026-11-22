package io.github.dreammooncai.pvz2tool.release

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 生成「戴夫工具箱适配包」zip。
 *
 * 适配包 = Release APK 解包后的内容，做以下确定性变换（规则从 11.17.1 参考包反推）：
 *   删除：AndroidManifest.xml、arsclib.properties、assets/dexopt 目录、frameworks 目录、
 *         assets/pvz2tool/素材 目录、PvZ2中文版代码*.txt、parse_pvz_data.py、*.DS_Store、baseline.prof*
 *   新增：arsc.zip（由 [ArscZipExporter] 从新 APK 导出）、local.yml、4 个中文说明 txt
 *   更新：更新说明.txt（顶部插入当前版本段落）
 *   assets/pvz2tool 下的 dream.yml / js_documentation.md 等以 APK 内（即源码最新）为准。
 */
object AdaptationZipBuilder {

    private val DELETE_EXACT = setOf(
        "AndroidManifest.xml", "arsclib.properties", "local.properties", "resources.arsc"
    )
    private val DELETE_PATH_PREFIX = listOf(
        "assets/dexopt/", "frameworks/", "assets/pvz2tool/素材/"
    )
    private val DELETE_NAME_CONTAINS = listOf(
        "PvZ2中文版代码", "parse_pvz_data.py", ".DS_Store", "baseline.prof"
    )

    private val TEMPLATE_TXT = listOf(
        "使用说明.txt", "增加内容.txt", "关于旧版pvz的sdk升级与界面自定义.txt"
    )

    /**
     * @param apk        Release APK
     * @param version    版本号（如 11.22.0）
     * @param notesFile  当前版本说明文件（纯编号条目，每行一条；可为 null 表示不插入新段落）
     * @param templateDir 模板目录（含 local.yml / 更新说明.txt / 3 个固定 txt）
     * @param outZip     输出适配包
     */
    fun build(
        apk: File,
        version: String,
        notesFile: File?,
        templateDir: File,
        outZip: File
    ) {
        require(apk.exists()) { "APK 不存在: $apk" }
        require(templateDir.isDirectory) { "模板目录不存在: $templateDir" }

        // 1) 导出 arsc.zip 到临时文件
        val arscZip = File.createTempFile("arsc_", ".zip")
        try {
            ArscZipExporter.exportFromApk(apk, arscZip)
            val arscBytes = arscZip.readBytes()

            // 2) 组装模板文本
            val templates = HashMap<String, ByteArray>()
            for (name in TEMPLATE_TXT) {
                val f = File(templateDir, name)
                require(f.exists()) { "模板缺失: $f" }
                templates[name] = f.readBytes()
            }
            val localYml = File(templateDir, "local.yml")
            require(localYml.exists()) { "模板缺失: $localYml" }

            val updateLog = buildUpdateLog(
                File(templateDir, "更新说明.txt"), version, notesFile
            )

            outZip.parentFile?.mkdirs()
            ZipOutputStream(outZip.outputStream().buffered()).use { zos ->
                // 3) 复制 APK 内容（过滤）
                ZipFile(apk).use { zf ->
                    val es = zf.entries()
                    while (es.hasMoreElements()) {
                        val e = es.nextElement()
                        val name = e.name
                        if (shouldDelete(name)) continue
                        val outEntry = ZipEntry(name)
                        zos.putNextEntry(outEntry)
                        zf.getInputStream(e).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                // 4) 新增资源
                put(zos, "arsc.zip", arscBytes)
                put(zos, "local.yml", localYml.readBytes())
                for ((name, bytes) in templates) put(zos, name, bytes)
                put(zos, "更新说明.txt", updateLog.toByteArray(Charsets.UTF_8))
            }
        } finally {
            arscZip.delete()
        }
    }

    /** 在更新说明基线顶部插入当前版本段落（若该版本已存在则跳过，保证幂等）。 */
    private fun buildUpdateLog(baseline: File, version: String, notesFile: File?): String {
        val base = if (baseline.exists()) baseline.readText(Charsets.UTF_8) else ""
        if (notesFile == null || !notesFile.exists()) return base
        if (base.contains("更新说明-$version")) return base // 幂等
        val notes = notesFile.readText(Charsets.UTF_8).trim().lines()
            .filter { it.isNotBlank() }
            .mapIndexed { i, line ->
                // 若用户已自带编号则保留，否则补 1. 2. ...
                if (line.matches(Regex("^\\d+[.、].*"))) line else "${i + 1}.$line"
            }
        val section = buildString {
            append("更新说明-$version\n")
            notes.forEach { append(it).append('\n') }
            append('\n')
        }
        return section + base
    }

    private fun shouldDelete(name: String): Boolean {
        if (name in DELETE_EXACT) return true
        if (DELETE_PATH_PREFIX.any { name.startsWith(it) }) return true
        if (DELETE_NAME_CONTAINS.any { name.contains(it) }) return true
        // 目录条目中的根目录不需要，但保留子目录条目无害；仅过滤显式文件
        return false
    }

    private fun put(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(bytes)
        zos.closeEntry()
    }
}
