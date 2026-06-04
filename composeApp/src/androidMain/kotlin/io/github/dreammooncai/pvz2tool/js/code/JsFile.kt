package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
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
            val inputFile = access.resolveInputOrThrow(placeholderPath, ctx)
            try {
                inputFile.file.readBytes().js
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
            }
        }

        // file.readText(placeholderPath) - 简写方式
        listOf("readText".js, "读文本".js).func("placeholderPath") { args ->
            val placeholderPath = toString(args[0])
            val ctx = InitializePvz2.context
            val inputFile = access.resolveInputOrThrow(placeholderPath, ctx)
            try {
                inputFile.file.readText().js
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
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

        // file.copy(fromPath, toPath) - 复制文件
        listOf("copy".js, "复制".js).func("fromPath", "toPath") { args ->
            val fromPath = toString(args[0])
            val toPath = toString(args[1])
            val ctx = InitializePvz2.context
            val inputFile = access.resolveInputOrThrow(fromPath, ctx)
            val outputHandle = access.resolveOutputOrThrow(toPath, ctx)
            try {
                inputFile.file.copyTo(outputHandle.targetFile, overwrite = true)
                outputHandle.commit()
            } finally {
                if (inputFile.isCache) inputFile.file.delete()
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
    }

    /**
     * 构建文件对象，提供读写方法。
     * 每次操作时重新解析占位符路径。
     */
    private fun buildFileObject(placeholderPath: String): JsObject {
        val context = InitializePvz2.context
        val resolvedPath = access.resolver.resolveToPath(placeholderPath, context) ?: ""

        return Object("file") {
            // 解析后的绝对路径
            listOf("path".js, "路径".js) eq resolvedPath.js

            // 读取字节数组
            listOf("readBytes".js, "读字节".js).func {
                val inputFile = access.resolveInputOrThrow(placeholderPath, context)
                try {
                    inputFile.file.readBytes().js
                } finally {
                    if (inputFile.isCache) inputFile.file.delete()
                }
            }

            // 写入字节数组
            listOf("writeBytes".js, "写字节".js).func("bytes") { args ->
                val outputHandle = access.resolveOutputOrThrow(placeholderPath, context)
                try {
                    val bytes = args.getOrNull(0)?.toKotlin(this@func) as? List<*>
                        ?: throw IllegalArgumentException("需要传入字节数组")
                    outputHandle.targetFile.writeBytes(bytes.map { (it as? Number)?.toByte() ?: 0 }.toByteArray())
                    outputHandle.commit()
                } catch (e: Exception) {
                    JsConsole.error("file.writeBytes 失败:",e)
                    throw e
                }
                Undefined
            }

            // 读取文本（UTF-8）
            listOf("readText".js, "读文本".js).func {
                val inputFile = access.resolveInputOrThrow(placeholderPath, context)
                try {
                    inputFile.file.readText().js
                } finally {
                    if (inputFile.isCache) inputFile.file.delete()
                }
            }

            // 写入文本（UTF-8）
            listOf("writeText".js, "写文本".js).func("text") { args ->
                val outputHandle = access.resolveOutputOrThrow(placeholderPath, context)
                try {
                    val text = toString(args[1])
                    outputHandle.targetFile.writeText(text)
                    outputHandle.commit()
                } catch (e: Exception) {
                    JsConsole.error("file.writeText 失败:",e)
                    throw e
                }
                Undefined
            }

            // 复制文件到目标路径
            listOf("copyTo".js, "复制到".js).func("toPath") { args ->
                val toPath = toString(args[0])
                val inputFile = access.resolveInputOrThrow(placeholderPath, context)
                val outputHandle = access.resolveOutputOrThrow(toPath, context)
                try {
                    inputFile.file.copyTo(outputHandle.targetFile, overwrite = true)
                    outputHandle.commit()
                } finally {
                    if (inputFile.isCache) inputFile.file.delete()
                }
                Undefined
            }

            // 删除此文件
            listOf("delete".js, "删除".js).func {
                val inputFile = access.resolveInput(placeholderPath, context)
                (inputFile?.file?.delete() ?: false).js
            }

            // 检查文件是否存在
            listOf("exists".js, "存在".js).func {
                val inputFile = access.resolveInput(placeholderPath, context)
                (inputFile != null && inputFile.file.exists()).js
            }
        }
    }

}