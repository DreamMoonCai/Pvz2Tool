package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func

/**
 * 文件操作对象，支持占位符路径解析和读写操作（同步，不需要 await）。
 *
 * JS 使用示例：
 * ```js
 * // 方式1：使用 resolve() 链式调用
 * let bytes = file.resolve($SMF + "/packages/icon.bin").readBytes()
 * file.resolve($SMF + "/packages/output.bin").writeBytes(bytes)
 *
 * // 方式2：使用简写方法
 * let text = file.readText($ITEM + "/config.txt")
 * file.writeText($ITEM + "/config.txt", text)
 *
 * // 方式3：创建文件对象后多次操作
 * let fileObj = file.resolve($SMF + "/data.bin")
 * let bytes1 = fileObj.readBytes()
 * fileObj.writeBytes(modifiedBytes)
 * ```
 */
class JsFile(
    private val access: JsFileAccess
) {
    val js = Object("file") {
        // file.resolve(placeholderPath) → 文件对象
        listOf("resolve".js, "解析".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            buildFileObject(placeholderPath)
        }

        // file.readBytes(placeholderPath) - 简写方式
        listOf("readBytes".js, "读字节".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInputOrThrow(placeholderPath, ctx).use { inputFile ->
                inputFile.file.readBytes().js
            }
        }

        // file.readText(placeholderPath) - 简写方式
        listOf("readText".js, "读文本".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInputOrThrow(placeholderPath, ctx).use { inputFile ->
                inputFile.file.readText().js
            }
        }

        // file.writeBytes(placeholderPath, bytes) - 简写方式
        listOf("writeBytes".js, "写字节".js).func("placeholderPath", "bytes") { args ->
            val placeholderPath = toString(args[0])
            val bytes =
                args.getOrNull(1)?.toKotlin(this@func) as? List<*> ?: throw IllegalArgumentException("需要传入字节数组")
            val ctx = InitializePvz2.context
            val outputHandle = access.resolveOutputOrThrow(placeholderPath, ctx)
            try {
                outputHandle.targetFile.writeBytes(bytes.map { (it as? Number)?.toByte() ?: 0 }.toByteArray())
                outputHandle.commit()
            } catch (e: Exception) {
                JsConsole.error("file.writeBytes 失败:",e)
                throw e
            }
            Undefined
        }

        // file.writeText(placeholderPath, text) - 简写方式
        listOf("writeText".js, "写文本".js).func("placeholderPath", "text") { args ->
            val placeholderPath = toString(args[0])
            val text = toString(args[1])
            val ctx = InitializePvz2.context
            val outputHandle = access.resolveOutputOrThrow(placeholderPath, ctx)
            try {
                outputHandle.targetFile.writeText(text)
                outputHandle.commit()
            } catch (e: Exception) {
                JsConsole.error("file.writeText 失败:",e)
                throw e
            }
            Undefined
        }

        // file.copy(fromPath, toPath) - 复制文件（使用 extract 解压管线）
        listOf("copy".js, "复制".js, "copyTo".js,"复制到".js).func("fromPath", "toPath") { args ->
            val fromPath = toString(args[0])
            val toPath = toString(args[1])
            val ctx = InitializePvz2.context

            // 源路径 → internalPath（支持降级）
            val internalPath = access.resolver.resolveToInternalPath(fromPath, ctx)
                ?: throw IllegalArgumentException("无法将源路径转换为内部路径: $fromPath")

            // 目标路径 → 解析并获取目录
            val targetFile = access.resolveOutputOrThrow(toPath, ctx).targetFile
            val isFile = targetFile.isFile || targetFile.extension.isNotEmpty()
            val targetDir = when {
                targetFile.isDirectory || targetFile.extension.isEmpty() -> targetFile.also { it.mkdirs() }
                else -> targetFile.parentFile?.also { it.mkdirs() } ?: throw IllegalArgumentException("无法获取目标目录: $toPath")
            }

            JsUiManager.extract(
                sourcePaths = listOf(internalPath),
                targetDir = targetDir.absolutePath,
                sectionName = "复制文件"
            ).await()
            if (isFile) {
                val fileName = internalPath.substringAfterLast("/")
                if (targetFile.name != fileName) {
                    targetFile.deleteRecursively()
                    targetDir.resolve(fileName).renameTo(targetFile)
                }
            }
            Undefined
        }

        // file.delete(path) - 删除文件
        listOf("delete".js, "删除".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            val inputFile = access.resolveInput(placeholderPath, ctx)
            (inputFile?.file?.delete() ?: false).js
        }

        // file.exists(path) - 检查文件是否存在
        listOf("exists".js, "存在".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            val inputFile = access.resolveInput(placeholderPath, ctx)
            (inputFile != null && inputFile.file.exists()).js
        }

        listOf("list".js, "列表".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            listDir(placeholderPath)
        }

        // ========== 文件属性简写（传路径，返回属性值）==========

        // file.size(path) - 文件大小（字节）
        listOf("size".js, "大小".js, "length".js, "长度".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInput(placeholderPath, ctx)?.use { it.file.length().toDouble().js }
                ?: 0.0.js
        }

        // file.isDirectory(path) - 是否为目录
        listOf("isDirectory".js, "是目录".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInput(placeholderPath, ctx)?.use { it.file.isDirectory.js }
                ?: false.js
        }

        // file.isFile(path) - 是否为文件
        listOf("isFile".js, "是文件".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInput(placeholderPath, ctx)?.use { it.file.isFile.js }
                ?: false.js
        }

        // file.lastModified(path) - 最后修改时间（Unix 毫秒时间戳）
        listOf("lastModified".js, "修改时间".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInput(placeholderPath, ctx)?.use { it.file.lastModified().toDouble().js }
                ?: 0.0.js
        }

        // file.extension(path) - 扩展名（不含点），无扩展名返回空字符串
        listOf("extension".js, "扩展名".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val name = placeholderPath.trimEnd('/').substringAfterLast('/')
            val ext = name.substringAfterLast('.', "")
            ext.js
        }

        // file.parent(path) - 父目录路径，无父目录返回 null
        listOf("parent".js, "父目录".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            access.resolveInput(placeholderPath, ctx)?.use { inputFile ->
                inputFile.file.parentFile?.absolutePath?.js
            }
        }

        // ========== 文件操作简写 ==========

        // file.mkdir(path) - 创建目录（含父目录）
        listOf("mkdir".js, "创建目录".js, "mkdirs".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            val result = access.resolveInput(placeholderPath, ctx)?.use { it.file.mkdirs().js }
                ?: run {
                    val outputHandle = access.resolveOutputOrThrow(placeholderPath, ctx)
                    val r = outputHandle.targetFile.mkdirs()
                    outputHandle.commit()
                    r.js
                }
            result
        }

        // file.rename(fromPath, toPath) - 重命名/移动文件或目录
        listOf("rename".js, "重命名".js, "renameTo".js, "移动到".js).func("fromPath", "toPath") { args ->
            val fromPath = toString(args[0])
            val toPath = toString(args[1])
            val ctx = InitializePvz2.context
            val targetAbs = access.resolveOutputOrThrow(toPath, ctx).targetFile
            targetAbs.deleteRecursively()
            val result = access.resolveInput(fromPath, ctx)?.file?.renameTo(targetAbs) ?: false
            result.js
        }

        // file.appendText(path, text) - 追加文本（UTF-8），不存在则创建
        listOf("appendText".js, "追加文本".js).func("placeholderPath", "text") { args ->
            val placeholderPath = toString(args[0])
            val text = toString(args[1])
            val ctx = InitializePvz2.context
            val outputHandle = access.resolveOutputOrThrow(placeholderPath, ctx)
            outputHandle.targetFile.appendText(text)
            outputHandle.commit()
            Undefined
        }
    }

    /**
     * 根据路径是文件还是目录，构建不同的文件对象。
     * 文件对象提供读写 API（readBytes/readText/writeBytes/writeText 等），
     * 目录对象提供列表/创建 API（list/mkdir 等）。
     */
    private fun buildFileObject(placeholderPath: String): JsObject {
        val context = InitializePvz2.context
        val resolvedPath = access.normalizePath(placeholderPath)
        val inputFile = access.resolveInput(resolvedPath, context) ?: return Object("file-invalid") {
            listOf("name".js, "文件名".js) eq resolvedPath.trimEnd('/').substringAfterLast('/').js
            listOf("normalizePath".js, "规范路径".js) eq resolvedPath.js
            listOf("extension".js, "扩展名".js) eq resolvedPath
                .substringAfterLast('.').let { ext ->
                    if (ext == resolvedPath || ext.isEmpty()) "".js else ext.js
                }
        }

        val file = inputFile.file
        return if (file.isDirectory) {
            Object("dir") { buildDirObject(resolvedPath, inputFile, context) }
        } else if (file.isFile) {
            Object("file") { buildFileOnlyObject(resolvedPath, inputFile, context) }
        } else {
            Object("file-not-exist") {
                buildDirObject(resolvedPath, inputFile, context)
                buildFileOnlyObject(resolvedPath, inputFile, context)
            }
        }
    }

    /**
     * 构建目录对象：提供列表、创建等目录操作，不提供文件读/写 API。
     */
    private fun ObjectScope.buildDirObject(
        resolvedPath: String,
        inputFile: JsFileAccess.InputFile,
        context: android.content.Context
    ) {
        val file = inputFile.file
        val fileName = resolvedPath.trimEnd('/').substringAfterLast('/')
        val fileExtension = fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it != fileName } ?: ""
        val displayPath = if (inputFile.isCache) resolvedPath else file.absolutePath
        // ========== 通用属性 ==========
        listOf("name".js, "文件名".js) eq fileName.js
        listOf("normalizePath".js, "规范路径".js) eq resolvedPath.js
        listOf("extension".js, "扩展名".js) eq fileExtension.js
        listOf("path".js, "路径".js) eq displayPath.js
        listOf("internalPath".js, "内部路径".js) eq (access.resolver.resolveToInternalPath(resolvedPath, InitializePvz2.context) ?: "").js
        listOf("size".js, "大小".js) eq JsProperty { file.length().toDouble().js }
        listOf("isDirectory".js, "是目录".js) eq true.js
        listOf("isFile".js, "是文件".js) eq false.js
        listOf("lastModified".js, "修改时间".js) eq JsProperty { file.lastModified().toDouble().js }
        listOf("parent".js, "父目录".js) eq JsProperty { file.parentFile?.absolutePath?.js }

        // ========== 通用操作 ==========
        listOf("exists".js, "存在".js).func { file.exists().js }
        listOf("delete".js, "删除".js).func { file.deleteRecursively().js }

        listOf("rename".js, "重命名".js, "renameTo".js, "移动到".js).func("newPath") { args ->
            val newPath = toString(args[0])
            val targetAbs = access.resolveOutputOrThrow(newPath, context).targetFile
            targetAbs.deleteRecursively()
            file.renameTo(targetAbs).js
        }

        // ========== 目录专属操作 ==========
        listOf("list".js, "列表".js).func {
            listDir(resolvedPath)
        }

        listOf("mkdir".js, "创建目录".js, "mkdirs".js).func {
            access.resolveInput(resolvedPath, context)?.use { inputFile ->
                inputFile.file.mkdirs().js
            } ?: run {
                val outputHandle = access.resolveOutputOrThrow(resolvedPath, context)
                val result = outputHandle.targetFile.mkdirs()
                outputHandle.commit()
                result.js
            }
        }
    }

    /**
     * 构建文件对象（非目录）：提供读/写 API，不提供目录专属 API。
     */
    private fun ObjectScope.buildFileOnlyObject(
        resolvedPath: String,
        inputFile: JsFileAccess.InputFile,
        context: android.content.Context
    ) {
        val file = inputFile.file
        val fileName = resolvedPath.trimEnd('/').substringAfterLast('/')
        val fileExtension = fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it != fileName } ?: ""
        val displayPath = if (inputFile.isCache) resolvedPath else file.absolutePath
        // ========== 通用属性 ==========
        listOf("name".js, "文件名".js) eq fileName.js
        listOf("normalizePath".js, "规范路径".js) eq resolvedPath.js
        listOf("extension".js, "扩展名".js) eq fileExtension.js
        listOf("path".js, "路径".js) eq displayPath.js
        listOf("internalPath".js, "内部路径".js) eq (access.resolver.resolveToInternalPath(resolvedPath, InitializePvz2.context) ?: "").js
        listOf("size".js, "大小".js) eq JsProperty { file.length().toDouble().js }
        listOf("isDirectory".js, "是目录".js) eq false.js
        listOf("isFile".js, "是文件".js) eq true.js
        listOf("lastModified".js, "修改时间".js) eq JsProperty { file.lastModified().toDouble().js }
        listOf("parent".js, "父目录".js) eq JsProperty { file.parentFile?.absolutePath?.js }

        // ========== 通用操作 ==========
        listOf("exists".js, "存在".js).func { file.exists().js }
        listOf("delete".js, "删除".js).func { file.deleteRecursively().js }

        listOf("rename".js, "重命名".js, "renameTo".js, "移动到".js).func("newPath") { args ->
            val newPath = toString(args[0])
            val targetAbs = access.resolveOutputOrThrow(newPath, context).targetFile
            targetAbs.deleteRecursively()
            file.renameTo(targetAbs).js
        }

        // ========== 文件专属操作 ==========
        listOf("readBytes".js, "读字节".js).func {
            file.readBytes().js
        }

        listOf("readText".js, "读文本".js).func {
            file.readText().js
        }

        listOf("writeBytes".js, "写字节".js).func("bytes") { args ->
            val outputHandle = access.resolveOutputOrThrow(resolvedPath, context)
            try {
                val bytes = args.getOrNull(0)?.toKotlin(this@func) as? List<*>
                    ?: throw IllegalArgumentException("需要传入字节数组")
                outputHandle.targetFile.writeBytes(bytes.map { (it as? Number)?.toByte() ?: 0 }.toByteArray())
                outputHandle.commit()
            } catch (e: Exception) {
                JsConsole.error("file.writeBytes 失败:", e)
                throw e
            }
            Undefined
        }

        listOf("writeText".js, "写文本".js).func("text") { args ->
            val outputHandle = access.resolveOutputOrThrow(resolvedPath, context)
            try {
                val text = toString(args[1])
                outputHandle.targetFile.writeText(text)
                outputHandle.commit()
            } catch (e: Exception) {
                JsConsole.error("file.writeText 失败:", e)
                throw e
            }
            Undefined
        }

        // fileObj.copy(toPath) - 复制当前文件（使用 extract 解压管线）
        listOf("copy".js, "复制".js, "copyTo".js, "复制到".js).func("toPath") { args ->
            val toPath = toString(args[0])

            // 当前文件路径 → internalPath（支持降级）
            val internalPath = access.resolver.resolveToInternalPath(resolvedPath, InitializePvz2.context)
                ?: throw IllegalArgumentException("无法将路径转换为内部路径: $resolvedPath")

            // 目标路径 → 解析并获取目录
            val targetFile = access.resolveOutputOrThrow(toPath, InitializePvz2.context).targetFile
            val isFile = targetFile.isFile || targetFile.extension.isNotEmpty()
            val targetDir = when {
                targetFile.isDirectory || targetFile.extension.isEmpty() -> targetFile.also { it.mkdirs() }
                else -> targetFile.parentFile?.also { it.mkdirs() } ?: throw IllegalArgumentException("无法获取目标目录: $toPath")
            }

            JsUiManager.extract(
                sourcePaths = listOf(internalPath),
                targetDir = targetDir.absolutePath,
                sectionName = "复制文件"
            ).await()
            if (isFile) {
                val fileName = internalPath.substringAfterLast("/")
                if (targetFile.name != fileName) {
                    targetFile.deleteRecursively()
                    targetDir.resolve(fileName).renameTo(targetFile)
                }
            }
            Undefined
        }

        // fileObj.appendText(text) - 追加文本
        listOf("appendText".js, "追加文本".js).func("text") { args ->
            val text = toString(args[0])
            val outputHandle = access.resolveOutputOrThrow(resolvedPath, context)
            outputHandle.targetFile.appendText(text)
            outputHandle.commit()
            Undefined
        }
    }

    /**
     * 列出 [dirPlaceholderPath] 指向的目录下所有直接子文件/子目录，
     * 每项包装为 [buildFileObject] 对象，返回 JS 数组。
     *
     * 使用 [JsFileAccess.listDirectory] 获取子项名列表（不拷贝到缓存），
     * 然后为每个子项构建 "$dirPlaceholderPath/$childName" 的 [buildFileObject]。
     */
    private fun listDir(dirPlaceholderPath: String) =
        access.listDirectory(dirPlaceholderPath, InitializePvz2.context)
            ?.map { childName ->
                // 拼接父目录占位符路径和子项名，构建子项的文件对象
                val childPath = "$dirPlaceholderPath/$childName"
                buildFileObject(childPath)
            }?.js ?: emptyList<JsObject>().js

}