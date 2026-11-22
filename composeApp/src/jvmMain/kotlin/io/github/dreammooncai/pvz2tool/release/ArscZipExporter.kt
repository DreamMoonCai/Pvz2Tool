package io.github.dreammooncai.pvz2tool.release

import com.reandroid.apk.ApkModule
import com.reandroid.apk.ApkModuleXmlDecoder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 把 APK 的 resources.arsc 导出为 MT 管理器「Arsc 编辑器++」可识别的 arsc.zip 格式。
 *
 * 实现方式：ARSCLib 官方 [ApkModuleXmlDecoder.decodeResourceTable] 先把 APK 资源解码成标准
 * Android res 目录（权威内容），再把该目录重组为 MT 私有结构：
 *   <package>/package-info.xml
 *   <package>/<type>/type-info.xml
 *   <package>/<type>/<type>-<qualifier>.xml   （default config 无 -qualifier 后缀）
 *
 * 之所以不直接用 XmlCoder.VALUES_XML.decode/ decodePackage：在 KMP/JVM 下这些 API 对纯
 * table 的 decode 恒返回 0（只有被 decodeResFiles 记录过的 entry 才输出），而官方
 * decodeResourceTable 先解码 res 文件再 decode values，能完整产出。
 */
object ArscZipExporter {

    fun exportFromApk(apk: File, outZip: File, packageName: String = DEFAULT_PACKAGE) {
        val work = File(System.getProperty("java.io.tmpdir"), "pvz2tool_arsc_export_${System.nanoTime()}")
        val decodedRoot: File
        val mod = ApkModule.loadApkFile(apk)
        try {
            ApkModuleXmlDecoder(mod).decodeResourceTable(work)
            // 官方输出目录：work/resources/package_N/  ——取含 res/ 的那个 package 目录
            decodedRoot = File(work, "resources").listFiles()
                ?.firstOrNull { File(it, "res").isDirectory }
                ?: error("ARSCLib 解码产物中未找到资源目录")
            exportFromDecodedDir(decodedRoot, outZip, packageName)
        } finally {
            mod.close()
            work.deleteRecursively()
        }
    }

    /** 从官方解码出的 package 目录（含 res/）重组为 MT arsc.zip。 */
    fun exportFromDecodedDir(decodedRoot: File, outZip: File, packageName: String = DEFAULT_PACKAGE) {
        val resDir = File(decodedRoot, "res")
        require(resDir.isDirectory) { "缺少 res 目录: $resDir" }

        // 结果 "<package>/<type>" → 文件内的 <resources> 子元素集合
        val files = LinkedHashMap<String, StringBuilder>()

        // 1) values[-qualifier]/<语义>.xml  →  <type>/<type>[-qualifier].xml
        val valueDirs = resDir.listFiles()?.filter { it.isDirectory && (it.name == "values" || it.name.startsWith("values-")) } ?: emptyList()
        for (vd in valueDirs) {
            val qualifier = if (vd.name == "values") "" else vd.name.removePrefix("values-")
            for (f in vd.listFiles()?.filter { it.extension == "xml" } ?: emptyList()) {
                val type = semanticType(f.name) ?: continue // public.xml → null 跳过
                val body = extractResourcesBody(f.readText())
                if (body.isBlank()) continue
                val file = "$packageName/$type/$type${if (qualifier.isEmpty()) "" else "-$qualifier"}.xml"
                files.getOrPut(file) { StringBuilder() }.append(body)
            }
        }

        // 2) 文件型 type（res/<type>[-qual]/<name>.*）→ <type>/<type>[-qual].xml 里的 <path>
        //    注意资源文件可能是 .xml/.png/.9.png/.jpg/.ttf 等，MT 统一用 <path>，name 去掉扩展名。
        //    <path> 必须排在同 type 的 scalar 值之前（与 MT 参考包顺序一致）。
        val typeDirs = resDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith("values") } ?: emptyList()
        for (d in typeDirs) {
            val (type, qualifier) = splitTypeDir(d.name)
            val tag = FILE_TYPE_TAG[type] ?: continue
            val sb = StringBuilder()
            for (f in d.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()) {
                val name = f.name.substringBeforeLast('.') // .9.png → abc_xxx.9 → 需再去掉 .9
                    .removeSuffix(".9")
                val resPath = "res/${d.name}/${f.name}"
                sb.append("    <$tag name=\"${escape(name)}\">$resPath</$tag>\n")
            }
            if (sb.isNotBlank()) {
                val file = "$packageName/$type/$type${if (qualifier.isEmpty()) "" else "-$qualifier"}.xml"
                val target = files.getOrPut(file) { StringBuilder() }
                target.insert(0, sb) // <path> 置于 scalar 之前
            }
        }

        // 3) public.xml 解析 id↔name，生成每个 type 的 type-info.xml + package id
        val publicEntries = parsePublicXml(File(resDir, "values/public.xml"))
        val pkgId = publicEntries.map { it.second ushr 24 and 0xff }.firstOrNull() ?: 0x66
        val pkgXml = xmlHeader() +
            "<package id=\"0x%02x\" name=\"${escape(packageName)}\">\n".format(pkgId) +
            "    <library id=\"0x%02x\" name=\"${escape(packageName)}\" />\n".format(pkgId) +
            "</package>"

        outZip.parentFile?.mkdirs()
        ZipOutputStream(outZip.outputStream().buffered()).use { zos ->
            put(zos, "$packageName/package-info.xml", pkgXml)
            val byType = publicEntries.groupBy { it.first }
            for ((type, entries) in byType) {
                val sb = StringBuilder(xmlHeader())
                val typeId = entries.first().second ushr 16 and 0xff
                sb.append("<type id=\"0x%02x\">\n".format(typeId))
                for ((_, id, name) in entries.sortedBy { it.second }) {
                    sb.append("    <entry id=\"0x%08x\" name=\"${escape(name)}\" />\n".format(id))
                }
                sb.append("</type>")
                put(zos, "$packageName/$type/type-info.xml", sb.toString())
            }
            for ((path, sb) in files) {
                if (sb.isBlank()) continue
                put(zos, path, xmlHeader() + "<resources>\n" + sb + "</resources>")
            }
        }
    }

    /** 从官方语义 XML 提取 <resources> 内的直接子元素，统一为 MT 的 4 空格缩进。 */
    private fun extractResourcesBody(text: String): String {
        val start = text.indexOf("<resources")
        val openEnd = text.indexOf('>', start)
        val close = text.lastIndexOf("</resources>")
        if (start < 0 || openEnd < 0 || close < 0) return ""
        val inner = text.substring(openEnd + 1, close)
        val out = StringBuilder()
        var depth = 0
        val buf = StringBuilder()
        for (line in inner.lines()) {
            val t = line.trim()
            if (t.isEmpty()) continue
            if (depth == 0) buf.setLength(0)
            buf.append(t).append('\n')
            depth += countOpen(t) - countClose(t)
            if (depth <= 0) {
                val block = buf.toString().trimEnd('\n')
                val lines = block.lines()
                // 官方顶层缩进 2 空格、子级 4 空格；MT 顶层 4、子级 8 → 每行整体 +2 空格，
                // 保留相对缩进层级。
                for (bl in lines) out.append("  ").append(bl).append('\n')
                buf.setLength(0)
                depth = 0
            }
        }
        return out.toString()
    }

    private fun countOpen(s: String): Int = Regex("<[a-zA-Z][^>]*?(?<!/)>").findAll(s).count()
    private fun countClose(s: String): Int = Regex("</[a-zA-Z]").findAll(s).count()

    /** 解析 public.xml：返回 (type, resourceId, name)。 */
    private fun parsePublicXml(f: File): List<Triple<String, Int, String>> {
        if (!f.exists()) return emptyList()
        val result = mutableListOf<Triple<String, Int, String>>()
        val re = Regex("<public\\s+([^>]+)/>")
        for (m in re.findAll(f.readText())) {
            val attrs = m.groupValues[1]
            fun a(k: String) = Regex("$k=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1)
            val type = a("type") ?: continue
            val name = a("name") ?: continue
            val id = a("id")?.removePrefix("0x")?.toLongOrNull(16) ?: continue
            result.add(Triple(type, id.toInt(), name))
        }
        return result
    }

    private fun semanticType(fileName: String): String? = when (fileName) {
        "strings.xml" -> "string"; "arrays.xml" -> "array"; "plurals.xml" -> "plurals"
        "bools.xml" -> "bool"; "colors.xml" -> "color"; "dimens.xml" -> "dimen"
        "ids.xml" -> "id"; "integers.xml" -> "integer"; "styles.xml" -> "style"
        "attrs.xml" -> "attr"; "drawables.xml" -> "drawable"
        else -> null // public.xml 等
    }

    /** res 目录名 → (基础 type, qualifier)。如 drawable-hdpi → (drawable, hdpi)。 */
    private fun splitTypeDir(dirName: String): Pair<String, String> {
        for (t in FILE_TYPE_TAG.keys.sortedByDescending { it.length }) {
            if (dirName == t) return t to ""
            if (dirName.startsWith("$t-")) return t to dirName.removePrefix("$t-")
        }
        return dirName to ""
    }

    private fun put(zos: ZipOutputStream, path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun xmlHeader() = "<?xml version='1.0' encoding='utf-8' ?>\n"

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
            '"' -> append("&quot;"); '\'' -> append("&apos;"); else -> append(c)
        }
    }

    /** 文件型资源 type → MT 使用的 XML 标签。anim/drawable 等用 <path>，color 用 <color>，font 用 <string>。 */
    private val FILE_TYPE_TAG = mapOf(
        "anim" to "path", "animator" to "path", "layout" to "path", "interpolator" to "path",
        "xml" to "path", "mipmap" to "path", "drawable" to "path", "transition" to "path", "menu" to "path",
        "color" to "color", "font" to "string"
    )

    private const val DEFAULT_PACKAGE = "io.github.dreammooncai.pvz2tool"
}
