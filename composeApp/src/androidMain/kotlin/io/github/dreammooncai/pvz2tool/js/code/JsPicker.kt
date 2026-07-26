package io.github.dreammooncai.pvz2tool.js.code

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import kotlin.text.Charsets

/**
 * 文件选择器全局对象：`picker`。
 *
 * 用法：
 * ```js
 * // 选择一个目录
 * let dir = picker.directory();
 * if (dir) console.log(dir.name, dir.path);
 *
 * // 选择一个文件（可指定 MIME 类型）
 * let f = picker.file({ mimeType: "image/..." });
 * if (f) {
 *   let text = f.readText();
 *   f.writeText("hello");
 * }
 *
 * // 选择多个文件
 * let files = picker.files({ mimeType: "application/json" });
 * files.forEach(f => console.log(f.name));
 * ```
 *
 * 选择结果是「文件对象」，API 与 [io.github.dreammooncai.pvz2tool.js.code.JsFile] 构建的对象一致
 * （name / path / size / isDirectory / isFile / lastModified / readBytes / readText / writeBytes /
 * writeText / delete / rename / list / copy / appendText 等），
 * 区别是底层基于 SAF 的 [DocumentFile]（content URI），而非本地文件路径。
 */
object JsPicker {

    val js = Object("picker") {
        // 选择目录：picker.directory(options?) -> 文件对象 | undefined（取消时）
        listOf("directory".js, "选择目录".js, "选择文件夹".js).func(
            FunctionParam("options")
        ) { args ->
            pickSingle(FilePickerManager.PickerMode.DIRECTORY, args.getOrNull(0).orNull)
        }

        // 选择单个文件：picker.file(options?) -> 文件对象 | undefined（取消时）
        listOf("file".js, "选择文件".js).func(
            FunctionParam("options")
        ) { args ->
            pickSingle(FilePickerManager.PickerMode.FILE, args.getOrNull(0).orNull)
        }

        // 选择多个文件：picker.files(options?) -> 文件对象数组（取消时为空数组）
        listOf("files".js, "选择多个文件".js).func(
            FunctionParam("options")
        ) { args ->
            pickMultiple(args.getOrNull(0).orNull)
        }
    }

    /**
     * 选择单个目标（目录或文件），返回单个文件对象；取消时返回 [Undefined]。
     */
    private suspend fun ScriptRuntime.pickSingle(
        mode: FilePickerManager.PickerMode,
        options: JsAny?
    ): JsAny? {
        val mimeType = parseMimeType(options)
        val fpm = InitializePvz2.filePickerManager ?: return Undefined
        val docs = fpm.pick(mode, mimeType).await()
        val doc = docs.firstOrNull()
        return if (doc != null) buildPickedFile(doc) else Undefined
    }

    /**
     * 选择多个文件，返回文件对象数组；取消时返回空数组。
     */
    private suspend fun ScriptRuntime.pickMultiple(options: JsAny?): JsAny? {
        val mimeType = parseMimeType(options)
        val fpm = InitializePvz2.filePickerManager ?: return emptyList<JsAny>().js
        val docs = fpm.pick(FilePickerManager.PickerMode.FILES, mimeType).await()
        val list = docs.filterNotNull().map { buildPickedFile(it) }
        return list.js
    }

    /**
     * 从 options 对象中解析 mimeType 字段。
     */
    private suspend fun ScriptRuntime.parseMimeType(options: JsAny?): String {
        val obj = options?.orNull ?: return "*/*"
        return obj.get("mimeType".js, this)?.orNull
            ?.let { toString(it) }
            ?.takeIf { it.isNotBlank() } ?: "*/*"
    }

    /**
     * 将选中的 [DocumentFile] 包装为与 `file` 对象同款的 JS 文件对象。
     */
    private fun buildPickedFile(docFile: DocumentFile): JsObject {
        val context: Context = InitializePvz2.context
        val uri: Uri = docFile.uri
        val uriStr = uri.toString()
        val name = docFile.name ?: ""
        val isDir = docFile.isDirectory
        val isFileType = docFile.isFile
        val extension = name.substringAfterLast('.', "")
            .takeIf { it.isNotEmpty() && it != name } ?: ""

        return Object("file") {
            // ========== 通用属性 ==========
            listOf("name".js, "文件名".js) eq name.js
            // 内容 URI：既是访问地址，也作为 path / normalizePath
            listOf("uri".js, "地址".js) eq uriStr.js
            listOf("path".js, "路径".js) eq uriStr.js
            listOf("normalizePath".js, "规范路径".js) eq uriStr.js
            listOf("extension".js, "扩展名".js) eq extension.js
            listOf("size".js, "大小".js) eq JsProperty { docFile.length().toDouble().js }
            listOf("isDirectory".js, "是目录".js) eq isDir.js
            listOf("isFile".js, "是文件".js) eq isFileType.js
            listOf("lastModified".js, "修改时间".js) eq JsProperty { docFile.lastModified().toDouble().js }
            // 内容 URI 无法获取父目录
            listOf("parent".js, "父目录".js) eq JsProperty { docFile.parentFile?.let { buildPickedFile(it) } }

            // ========== 通用操作 ==========
            listOf("exists".js, "存在".js).func { docFile.exists().js }
            listOf("delete".js, "删除".js).func { docFile.delete().js }

            listOf("rename".js, "重命名".js, "renameTo".js).func("newName") { args ->
                val newName = toString(args[0])
                docFile.renameTo(newName).js
            }

            if (isDir) {
                // ========== 目录专属操作 ==========
                listOf("list".js, "列表".js).func {
                    docFile.listFiles().map { buildPickedFile(it) }.js
                }
                // 远程 tree URI 无法就地创建目录
                listOf("mkdir".js, "创建目录".js, "mkdirs".js).func { false.js }
            } else {
                // ========== 文件专属操作 ==========
                listOf("readBytes".js, "读字节".js).func {
                    try {
                        JsFileResolver.readFromDocumentFile(docFile, context).js
                    } catch (e: Exception) {
                        JsConsole.error("picker.readBytes 失败:", e)
                        Undefined
                    }
                }

                listOf("readText".js, "读文本".js).func {
                    try {
                        val bytes = JsFileResolver.readFromDocumentFile(docFile, context)
                        String(bytes, Charsets.UTF_8).js
                    } catch (e: Exception) {
                        JsConsole.error("picker.readText 失败:", e)
                        Undefined
                    }
                }

                listOf("writeBytes".js, "写字节".js).func("bytes") { args ->
                    val bytes = args.getOrNull(0).orNull?.toKotlin(this) as? List<*>
                        ?: throw IllegalArgumentException("需要传入字节数组")
                    JsFileResolver.writeToDocumentFile(
                        docFile,
                        bytes.map { (it as? Number)?.toByte() ?: 0 }.toByteArray(),
                        context
                    )
                    Undefined
                }

                listOf("writeText".js, "写文本".js).func("text") { args ->
                    val text = toString(args[0])
                    JsFileResolver.writeToDocumentFile(
                        docFile,
                        text.toByteArray(Charsets.UTF_8),
                        context
                    )
                    Undefined
                }

                listOf("appendText".js, "追加文本".js).func("text") { args ->
                    val text = toString(args[0])
                    val existing = try {
                        JsFileResolver.readFromDocumentFile(docFile, context)
                    } catch (_: Exception) {
                        ByteArray(0)
                    }
                    val merged = existing + text.toByteArray(Charsets.UTF_8)
                    JsFileResolver.writeToDocumentFile(docFile, merged, context)
                    Undefined
                }
            }

            // ========== 复制：把选中的文件写入到本地 / 占位符路径 ==========
            listOf("copy".js, "复制".js, "copyTo".js, "复制到".js).func("toPath") { args ->
                val toPath = toString(args[0])
                val bytes = try {
                    JsFileResolver.readFromDocumentFile(docFile, context)
                } catch (e: Exception) {
                    throw IllegalArgumentException("无法读取源文件: ${e.message}")
                }
                val outputHandle = JsFileAccess(JsFileResolver).resolveOutputOrThrow(toPath, context)
                try {
                    outputHandle.targetFile.writeBytes(bytes)
                    outputHandle.commit()
                } catch (e: Exception) {
                    outputHandle.cancel()
                    throw e
                }
                Undefined
            }
        }
    }
}
