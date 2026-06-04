package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.JsSmfDataManager
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File

/**
 * 从版本级 SMF 缓存构建 JS `data` 对象。
 *
 * JS 访问结构（以 smfList = ["dynamic"] 为例）：
 * ```js
 * let npcs = data.dynamic.packages.npcs.load()
 * npcs["field"] = "value"
 * npcs.save()
 * ```
 *
 * **load/save 对接规则**：
 * - `entry.load()` 内部调 `rton.load(extractedPath, modifiedMirrorPath)`
 *   - 第一个参数：extracted 目录中的绝对路径（读取源）
 *   - 第二个参数：modified 目录中的镜像绝对路径（save() 不传参时的默认写入目标）
 * - `rton.load` 已经处理了 RTON→JSON 解码、save() 注入等所有逻辑
 * - 绝对路径走 `JsFileAccess.resolveInputOrThrow` 的快速路径，不经过 SAF/DocumentFile
 *
 * **通用二进制文件**：
 * - `buildBinaryFileObject()` 构建的文件对象提供 `path`、`readBytes()`、`writeBytes()`、`readText()`、`writeText()` 方法
 * - 读取优先使用 modified 目录中的文件（如果存在），否则使用 extracted 目录
 * - 写入目标为 modified 目录中的镜像路径
 *
 * modified 镜像路径计算：
 *   extracted 路径中的 `.../extracted/...` 替换为 `.../modified/...`
 */
class JsSmfDataBuilder(
    private val manager: JsSmfDataManager
) {

    /**
     * 构建顶层 `data` 对象，key 为 smf 基名（如 "dynamic"）。
     */
    fun buildFromExtractedDir(): JsObject {
        return Object("data") {
            manager.caches.forEach { (smfKey, cache) ->
                // Rsb.unpack 在 extractedDir 下会生成与 smfFileName 同名的子目录
                val smfSubDir = cache.extractedDir
                if (smfSubDir.exists() && smfSubDir.isDirectory) {
                    (if (smfKey.endsWith(".obb")) listOf(smfKey.js,"obb".js) else listOf(smfKey.js)) eq buildDirObject(smfSubDir, cache.extractedDir, cache.modifiedDir)
                }
            }
        }
    }

    /**
     * 递归构建目录的 JS 对象。
     *
     * @param dir           当前遍历的目录
     * @param extractedRoot 该 SMF 的 extractedDir（用于计算 modifiedRoot 镜像路径）
     * @param modifiedRoot  该 SMF 的 modifiedDir
     */
    private fun buildDirObject(dir: File, extractedRoot: File, modifiedRoot: File): JsObject {
        return Object(dir.name.lowercase()) {
            dir.listFiles()?.sortedBy { it.name }?.forEach { entry ->
                when {
                    entry.isDirectory -> {
                        listOf(entry.name.js,entry.name.lowercase().js) eq buildDirObject(entry, extractedRoot, modifiedRoot)
                    }
                    entry.isFile -> {
                        val key = listOf(entry.nameWithoutExtension.js, entry.nameWithoutExtension.lowercase().js)
                        val ext = entry.extension.lowercase()
                        if (ext == "rton" || ext == "json") {
                            key eq buildRtonFileObject(entry, extractedRoot, modifiedRoot)
                        } else {
                            // 其他文件类型：使用通用二进制文件对象，提供 readBytes/writeBytes/readText/writeText 方法
                            key eq buildBinaryFileObject(entry, extractedRoot, modifiedRoot)
                        }
                    }
                }
            }
        }
    }

    /**
     * 构建 RTON/JSON 文件对象，暴露 `path` 属性和 `load()` 方法。
     *
     * `load()` 委托给引擎全局的 `rton.load(inputPath, outputPath)`：
     * - inputPath  = extracted 目录中的绝对路径（实际读取）
     * - outputPath = modified 目录中的绝对镜像路径（save() 默认写入目标）
     *
     * 若 modified 镜像文件已存在（上次执行留下的），则以 modified 文件作为读取源，
     * 这样多次运行可以累加修改。
     */
    private fun buildRtonFileObject(
        extractedFile: File,
        extractedRoot: File,
        modifiedRoot: File
    ): JsObject {
        // 计算 modified 目录中的镜像路径
        val modifiedFile = File(
            extractedFile.absolutePath.replace(
                extractedRoot.absolutePath,
                modifiedRoot.absolutePath
            )
        )

        // 若 modified 文件已存在（上次已修改），以它作为读取源，避免覆盖历史修改
        val readSource = if (modifiedFile.exists()) modifiedFile else extractedFile

        return Object(extractedFile.nameWithoutExtension.lowercase()) {
            listOf("path".js,"路径".js) eq extractedFile.absolutePath.js

            listOf("readPath".js, "读取路径".js) eq readSource.absolutePath.js

            listOf("modifiedPath".js, "修改路径".js) eq modifiedFile.absolutePath.js

            listOf("load".js,"加载".js).func {
                // 委托给引擎全局的 rton.load(inputPath, outputPath)
                // inputPath  = readSource 绝对路径（支持绝对路径快速通道）
                // outputPath = modifiedFile 绝对路径（save() 默认写入目标）
                val rtonLoad = PvzToolJsEngine.getJSEngine()
                    .compile("rton.load")
                    .invoke() as JSFunction

                rtonLoad.invoke(
                    listOf(
                        readSource.absolutePath.js,
                        modifiedFile.absolutePath.js
                    ),
                    this
                )
            }
        }
    }

    /**
     * 构建通用二进制文件对象，暴露 `path` 属性和字节/文本读写方法。
     *
     * **读取逻辑**：优先读取 modified 目录中的文件（如果存在），否则读取 extracted 目录
     * **写入逻辑**：写入目标为 modified 目录中的镜像路径
     *
     * JS 使用示例：
     * ```js
     * let bytes = data.dynamic.packages.myfile.readBytes()
     * data.dynamic.packages.myfile.writeBytes(bytes)
     *
     * let text = data.dynamic.packages.myfile.readText()
     * data.dynamic.packages.myfile.writeText("hello world")
     * ```
     *
     * @param extractedFile extracted 目录中的文件
     * @param extractedRoot 该文件的 extracted 根目录
     * @param modifiedRoot 该文件的 modified 根目录
     */
    fun buildBinaryFileObject(
        extractedFile: File,
        extractedRoot: File,
        modifiedRoot: File
    ): JsObject {
        // 计算 modified 目录中的镜像路径
        val modifiedFile = File(
            extractedFile.absolutePath.replace(
                extractedRoot.absolutePath,
                modifiedRoot.absolutePath
            )
        )

        // 读取源：优先 modified（如果存在），否则 extracted
        val readSource = if (modifiedFile.exists()) modifiedFile else extractedFile

        return Object(extractedFile.nameWithoutExtension.lowercase()) {
            // 文件绝对路径（extracted 目录中的路径）
            listOf("path".js, "路径".js) eq extractedFile.absolutePath.js

            listOf("readPath".js, "读取路径".js) eq readSource.absolutePath.js

            // modified 目录中的镜像路径（写入目标）
            listOf("modifiedPath".js, "修改路径".js) eq modifiedFile.absolutePath.js

            // 读取字节数组
            listOf("readBytes".js, "读字节".js).func {
                if (!readSource.exists()) {
                    throw IllegalStateException("文件不存在: ${readSource.absolutePath}")
                }
                readSource.readBytes().js
            }

            // 写入字节数组
            listOf("writeBytes".js, "写字节".js).func("bytes") { args ->
                modifiedFile.parentFile?.mkdirs()
                val bytes = args.getOrNull(0)?.toKotlin(this@func) as? List<*> ?: throw IllegalArgumentException("需要传入字节数组")
                modifiedFile.writeBytes(bytes.map { (it as? Number)?.toByte() ?: 0 }.toByteArray())
                Undefined
            }

            // 读取文本（UTF-8）
            listOf("readText".js, "读文本".js).func {
                if (!readSource.exists()) {
                    throw IllegalStateException("文件不存在: ${readSource.absolutePath}")
                }
                readSource.readText().js
            }

            // 写入文本（UTF-8）
            listOf("writeText".js, "写文本".js).func("text") { args ->
                modifiedFile.parentFile?.mkdirs()
                val text = args.getOrNull(0)?.let { toString(it) } ?: throw IllegalArgumentException("需要传入字节数组")
                modifiedFile.writeText(text)
                Undefined
            }
        }
    }
}