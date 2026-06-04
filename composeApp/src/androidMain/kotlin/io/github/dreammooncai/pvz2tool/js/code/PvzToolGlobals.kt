package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.pop.plugin.crypt.Pvz2NumberCrypt
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.util.getAssetLastModified
import io.github.dreammooncai.util.isAssetDirExist
import io.github.dreammooncai.util.isAssetFileExist
import io.github.dreammooncai.util.openUriInputStreamOrAssetNull
import java.nio.ByteBuffer
import android.util.Base64 as AndroidBase64

object PvzToolGlobals {

    val ui = Object("ui") {
        // 提示弹窗（单按钮）：ui.alert(title, message) -> void
        listOf("alert".js, "提示".js).func(
            FunctionParam("title"),
            FunctionParam("message")
        ) { args ->
            val title = toString(args[0])
            val message = toString(args[1])
            JsUiManager.showAlert(title, message).await()
            Undefined
        }

        // 确认弹窗：ui.confirm(title, message) -> boolean
        listOf("confirm".js, "确认".js).func(
            FunctionParam("title"),
            FunctionParam("message")
        ) { args ->
            val title = toString(args[0])
            val message = toString(args[1])
            JsUiManager.showConfirm(title, message).await().js
        }

        // 输入弹窗：ui.prompt(title, message, defaultValue?) -> string|null
        listOf("prompt".js, "输入".js).func(
            FunctionParam("title"),
            FunctionParam("message"),
            FunctionParam("defaultValue")
        ) { args ->
            val title = toString(args[0])
            val message = toString(args[1])
            val defaultValue = args.getOrNull(2)?.let { toString(it) } ?: ""
            JsUiManager.showPrompt(title, message, defaultValue).await()?.js
        }

        // 进度弹窗：ui.progress(title, options?) -> progressController
        // options: { message?, indeterminate?, showCancel? }
        listOf("progress".js, "进度".js).func(
            FunctionParam("title"),
            FunctionParam("options")
        ) { args ->
            val title = toString(args[0])
            val options = args[1].orNull

            // 解析 options 参数
            val message = options?.get("message".js, this)?.orNull?.let { toString(it) } ?: ""
            val indeterminate = options?.get("indeterminate".js, this)?.let { it.toKotlin(this) as? Boolean } ?: false
            val showCancel = options?.get("showCancel".js, this)?.let { it.toKotlin(this) as? Boolean } ?: true

            // 先显示进度弹窗
            JsUiManager.showProgress(title, message, indeterminate, showCancel)

            // 返回一个 JS 对象，包含 update 和 close 方法
            Object("controller") {
                // 更新进度：update(message?, progress?)
                // progress 是 0.0-1.0 的 Float
                listOf("update".js, "更新".js).func(
                    FunctionParam("message"),
                    FunctionParam("progress")
                ) { updateArgs ->
                    val msg = updateArgs.getOrNull(0)?.let { toString(it) }
                    val progress = updateArgs.getOrNull(1)?.let { toNumber(it).toFloat() }
                    JsUiManager.updateProgress(msg, progress)
                    Undefined
                }

                // 关闭进度弹窗
                listOf("close".js, "关闭".js).func { _ ->
                    JsUiManager.closeProgress()
                    Undefined
                }
            }
        }

        listOf("extract".js, "解压".js).func(
            FunctionParam("sourcePaths"),
            FunctionParam("targetDir"),
            FunctionParam("sectionName")
        ) { args ->
            val sourcePaths = args[0]?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val targetDir = toString(args[1])
            val sectionName = args.getOrNull(2).orNull?.let { toString(it) } ?: ""
            JsUiManager.extract(
                sourcePaths = sourcePaths.mapNotNull { it?.toString() },
                targetDir = targetDir,
                sectionName = sectionName
            ).await()
            Undefined
        }
    }

    val console = Object("console") {
        listOf("log".js, "日志".js).func(FunctionParam("msg", isVararg = true)) { args -> out(args, JsConsole::verbose) }
        listOf("info".js, "信息".js).func(FunctionParam("msg", isVararg = true)) { args -> out(args, JsConsole::info) }
        listOf("debug".js, "调试".js).func(FunctionParam("msg", isVararg = true)) { args -> out(args, JsConsole::debug) }
        listOf("warn".js, "警告".js).func(FunctionParam("msg", isVararg = true)) { args -> out(args, JsConsole::warn) }
        listOf("error".js, "错误".js).func(FunctionParam("msg", isVararg = true)) { args -> out(args, JsConsole::error) }
    }

    // ======================== 资源相关 API ========================
    val assets = Object("assets") {
        // 列出资源目录下的所有文件：assets.list(path) -> string[]
        listOf("list".js, "列表".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            AssetExtractorHolder.listResources(path).map { it.js }.js
        }

        // 列出 assets 目录下的所有文件（仅 assets，不含本地覆盖）：assets.listAssets(path) -> string[]
        listOf("listAssets".js, "列表Assets".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            AssetExtractorHolder.listAssetFiles(path).map { it.js }.js
        }

        // 检查资源是否存在：assets.exists(path) -> boolean
        listOf("exists".js, "存在".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            AssetExtractorHolder.exist(path).js
        }

        // 获取资源信息：assets.info(path) -> { exists, size, lastModified, isDirectory }
        listOf("info".js, "信息".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val normalizedPath = if (path.startsWith("${Pvz2ToolConfig.PATH_NAME}/")) path else "${Pvz2ToolConfig.PATH_NAME}/$path"
            val exists = AssetExtractorHolder.exist(path)
            val isDir = InitializePvz2.context.isAssetDirExist(normalizedPath)
            val isFile = InitializePvz2.context.isAssetFileExist(normalizedPath)

            Object("info") {
                "exists".js eq exists.js
                "isDirectory".js eq isDir.js
                "isFile".js eq isFile.js
                "size".js eq (if (isFile) {
                    try {
                        InitializePvz2.context.assets.openFd(normalizedPath).use { it.length }
                    } catch (e: Exception) {
                        -1L
                    }
                } else -1L).js
                "lastModified".js eq (if (isFile || isDir) {
                    InitializePvz2.context.getAssetLastModified(normalizedPath)
                } else 0L).js
            }
        }

        // 读取资源为字符串：assets.read(path) -> string
        listOf("read".js, "读取".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                Undefined
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                inputStream?.bufferedReader()?.use { it.readText() }?.js ?: Undefined
            }
        }

        // 读取资源为字节数组：assets.readBytes(path) -> Uint8Array
        listOf("readBytes".js, "读取字节".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                Undefined
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    Undefined
                } else {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    bytes.js
                }
            }
        }

        // 读取资源为 Base64：assets.readBase64(path) -> string
        listOf("readBase64".js, "读取Base64".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                Undefined
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    Undefined
                } else {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    AndroidBase64.encodeToString(bytes, AndroidBase64.NO_WRAP).js
                }
            }
        }

        // 读取资源为 ArrayBuffer（用于二进制数据）：assets.readArrayBuffer(path) -> ArrayBuffer
        listOf("readArrayBuffer".js, "读取ArrayBuffer".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                Undefined
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    Undefined
                } else {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    // 转换为 ArrayBuffer
                    val buffer = ByteBuffer.allocate(bytes.size)
                    buffer.put(bytes)
                    buffer.rewind()
                    buffer.array().js
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun ScriptRuntime.out(message : List<Any?>, out : (Any?) -> Unit) : JsAny {
        if (message.isEmpty() || (message[0] as List<*>).isEmpty()) {
            return Undefined
        }
        val args = message[0] as List<JsAny?>
        if (args.size == 1) {
            val obj = args[0]
            out(if (obj is JsObject) "$obj ${PvzToolJsEngine.stringify(obj,4)}" else args[0]?.toKotlin(this))
        } else {
            out(args.map { if (it is JsObject) "$it ${PvzToolJsEngine.stringify(it,4)}" else it?.toKotlin(this) })
        }
        return Undefined
    }

    suspend fun attached(runtime: ScriptRuntime) {
        listOf("console".js, "控制台".js).forEach { key ->
            runtime.set(key, console, VariableType.Global)
        }
        listOf("pvz".js, "植物大战僵尸".js).forEach { key ->
            runtime.set(key, JsPvz.js, VariableType.Global)
        }
        listOf("ui".js, "界面".js).forEach { key ->
            runtime.set(key, ui, VariableType.Global)
        }
        listOf("assets".js, "资源".js).forEach { key ->
            runtime.set(key, assets, VariableType.Global)
        }
        runtime.get("Number".js)?.get("prototype".js, runtime)?.let { it as? JsObject }?.let { prototype ->
            listOf("encrypt".js, "加密".js).forEach { key ->
                prototype.set(key, JsProperty {
                    val n = toNumber(thisRef).toLong()
                    Pvz2NumberCrypt.encrypt(n).js
                }, runtime)
            }
            listOf("decrypt".js, "解密".js).forEach { key ->
                prototype.set(key, JsProperty {
                    val n = toNumber(thisRef).toLong()
                    Pvz2NumberCrypt.decrypt(n).js
                }, runtime)
            }
        }
    }
}