package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.code.JsDevice
import io.github.dreammooncai.pvz2tool.js.code.JsBrowser
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.pop.plugin.crypt.Pvz2NumberCrypt
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.JsActionItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsChoiceItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.ui.music.BackgroundMusicState
import io.github.dreammooncai.util.getAssetLastModified
import io.github.dreammooncai.util.isAssetDirExist
import io.github.dreammooncai.util.isAssetFileExist
import io.github.dreammooncai.util.openUriInputStreamOrAssetNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.io.File
import android.util.Base64 as AndroidBase64

object PvzToolGlobals {

    // 持有 BackgroundMusicState 引用，供 audio 对象访问（由 Pvz2InitializeActivity 注入）
    var bgMusicState: BackgroundMusicState? = null

    /** 将 options 中的 JS 函数字段绑定为 Kotlin 挂起回调（value 透传给 JS 函数作为第一个参数）。 */
    private suspend fun bindJsCallback(
        options: JsObject?,
        key: String,
        runtime: ScriptRuntime
    ): (suspend (JsAny?) -> Unit)? {
        val jsFn = options?.get(key.js, runtime)?.orNull as? JSFunction ?: return null
        return { value -> runCatching { jsFn.invoke(listOf(value), runtime) } }
    }

    val ui = Object("ui") { // 提示弹窗（单按钮）：ui.alert(title, message, options?) -> void
        listOf("alert".js, "提示".js).func(
            FunctionParam("title"), FunctionParam("message"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val message = toString(args[1])
            val options = args[2].orNull as? JsObject
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            JsUiManager.showAlert(title, message, confirmText, confirmColor, dismissible, onConfirm).await()
            Undefined
        }

        // 确认弹窗：ui.confirm(title, message, options?) -> boolean
        listOf("confirm".js, "确认".js).func(
            FunctionParam("title"), FunctionParam("message"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val message = toString(args[1])
            val options = args[2].orNull as? JsObject
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确认"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            JsUiManager.showConfirm(title, message, confirmText, cancelText, confirmColor, cancelColor, dismissible, onConfirm, onCancel).await().js
        }

        // 输入弹窗：ui.prompt(title, message, defaultValue?, placeholder?, options?) -> string|null
        listOf("prompt".js, "输入".js).func(
            FunctionParam("title"),
            FunctionParam("message"),
            FunctionParam("defaultValue"),
            FunctionParam("placeholder"),
            FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val message = toString(args[1])
            val defaultValue = args.getOrNull(2).orNull?.let { toString(it) } ?: ""
            val placeholder = args.getOrNull(3).orNull?.let { toString(it) } ?: ""
            val options = args[4].orNull as? JsObject
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            JsUiManager.showPrompt(title, message, defaultValue, placeholder, confirmText, cancelText, confirmColor, cancelColor, dismissible, onConfirm, onCancel).await()?.js
        }

        // 进度弹窗：ui.progress(title, options?) -> progressController
        // options: { message?, indeterminate?, showCancel?, onCancel? }
        listOf("progress".js, "进度".js).func(
            FunctionParam("title"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val options = args[1].orNull

            // 解析 options 参数
            val message = options?.get("message".js, this)?.orNull?.let { toString(it) } ?: ""
            val indeterminate = options?.get("indeterminate".js, this)?.let { it.toKotlin(this) as? Boolean } ?: false
            val showCancel = options?.get("showCancel".js, this)?.let { it.toKotlin(this) as? Boolean }
                ?: true // 取消回调：用户点击“取消”时触发（供 JS 中断耗时任务）
            val onCancelJs = options?.get("onCancel".js, this)?.orNull as? JSFunction

            // 先显示进度弹窗
            JsUiManager.showProgress(title, message, indeterminate, showCancel)
            if (onCancelJs != null) {
                JsUiManager.setProgressCancelHandler {
                    runCatching { onCancelJs.invoke(emptyList(), runtime) }
                }
            }

            // 返回一个 JS 对象，包含 update / close / cancel / isCancelled 方法
            Object("controller") { // 更新进度：update(message?, progress?)
                // progress 是 0.0-1.0 的 Float
                listOf("update".js, "更新".js).func(
                    FunctionParam("message"), FunctionParam("progress")
                ) { updateArgs ->
                    val msg = updateArgs.getOrNull(0).orNull?.let { toString(it) }
                    val progress = updateArgs.getOrNull(1).orNull?.let { toNumber(it).toFloat() }
                    JsUiManager.updateProgress(msg, progress)
                    Undefined
                }

                // 关闭进度弹窗（正常完成）
                listOf("close".js, "关闭".js).func { _ ->
                    JsUiManager.closeProgress()
                    Undefined
                }

                // 主动取消进度（效果同点击“取消”按钮）：隐藏弹窗并触发 onCancel
                listOf("cancel".js, "取消".js).func { _ ->
                    JsUiManager.cancelProgress()
                    Undefined
                }

                // 是否已取消（供 JS 循环轮询，及时中断耗时任务）
                listOf("isCancelled".js, "是否已取消".js).func { _ ->
                    JsUiManager.isProgressCancelled().js
                }
            }
        }

        listOf("extract".js, "解压".js).func(
            FunctionParam("sourcePaths"), FunctionParam("targetDir"), FunctionParam("sectionName")
        ) { args ->
            val sourcePaths = args.getOrNull(0).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val targetDir = toString(args[1])
            val sectionName = args.getOrNull(2).orNull?.let { toString(it) } ?: ""
            JsUiManager.extract(
                sourcePaths = sourcePaths.mapNotNull { it?.toString() },
                targetDir = targetDir,
                sectionName = sectionName
            ).await()
            Undefined
        }

        // 单项选择弹窗：ui.select(title, items, options?) -> string|null
        // items: 字符串数组 或 对象数组 [{name, icon?, value?, showIndex?, showIndexColor?}]
        // options: { columns?, cancelable?, showIndex?, showIndexColor?, confirmText?, cancelText?, confirmColor?, cancelColor?, onCancel?, onSelect? }
        listOf("select".js, "选择".js).func(
            FunctionParam("title"), FunctionParam("items"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val itemsRaw = args.getOrNull(1).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val options = args[2].orNull as? JsObject
            val items = parseChoiceItems(itemsRaw)
            val columns = (options?.get("columns".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 4).coerceIn(2, 6)
            val cancelable = options?.get("cancelable".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val showIndex = options?.get("showIndex".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val showIndexColor = toString((options?.get("showIndexColor".js, this).orNull ?: options?.get("序号颜色".js, this).orNull ?: "black".js))
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            JsUiManager.showSelect(title, items, columns, cancelable, showIndex, showIndexColor, confirmText, cancelText, confirmColor, cancelColor, onCancel, onSelect).await()?.js
        }

        // 多项选择弹窗：ui.multiSelect(title, items, options?) -> string[]
        // options: { defaultValues?, columns?, cancelable?, showIndex?, showIndexColor?, confirmText?, cancelText?, confirmColor?, cancelColor?, onCancel?, onSelect? }
        listOf("multiSelect".js, "多选".js).func(
            FunctionParam("title"), FunctionParam("items"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val itemsRaw = args.getOrNull(1).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val options = args[2].orNull as? JsObject
            val items = parseChoiceItems(itemsRaw)
            val defaultValues = options?.get("defaultValues".js, this)?.let { it.toKotlin(this) as? List<*> }
                ?.mapNotNull { it?.toString() }
            val columns = (options?.get("columns".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 4).coerceIn(2, 6)
            val cancelable = options?.get("cancelable".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val showIndex = options?.get("showIndex".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val showIndexColor = toString((options?.get("showIndexColor".js, this).orNull ?: options?.get("序号颜色".js, this).orNull ?: "black".js))
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            JsUiManager.showMultiSelect(title, items, defaultValues, columns, cancelable, showIndex, showIndexColor, confirmText, cancelText, confirmColor, cancelColor, onCancel, onSelect).await().map { it.js }.js
        }

        // 操作菜单弹窗：ui.actionSheet(title, actions, options?) -> string|null
        // actions: 字符串数组 或 对象数组 [{name, value?, danger?}]
        // options: { cancelable?, cancelText?, cancelColor?, onCancel?, onSelect? }  点击某项返回其 value（或 name）；取消/点外部返回 null
        listOf("actionSheet".js, "操作菜单".js).func(
            FunctionParam("title"), FunctionParam("actions"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val itemsRaw = args.getOrNull(1).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val options = args[2].orNull as? JsObject
            val items = parseActionItems(itemsRaw)
            val cancelable = options?.get("cancelable".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: true
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            JsUiManager.showActionSheet(title, items, cancelable, cancelText, cancelColor, onCancel, onSelect).await()?.js
        }

        // 数值滑块弹窗：ui.slider(title, options?) -> number|null
        // options: { min?, max?, step?, default?, unit?, decimals?, showValue?, confirmText?, cancelText?, confirmColor?, cancelColor?, dismissible?, onChange?, onConfirm?, onCancel? }  点击"取消"返回 null
        listOf("slider".js, "滑块".js).func(
            FunctionParam("title"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val options = args[1].orNull as? JsObject
            val min = options?.get("min".js, this)?.orNull?.let { toNumber(it).toDouble() } ?: 0.0
            val max = options?.get("max".js, this)?.orNull?.let { toNumber(it).toDouble() } ?: 100.0
            val step = options?.get("step".js, this)?.orNull?.let { toNumber(it).toDouble() } ?: 1.0
            val default = options?.get("default".js, this)?.orNull?.let { toNumber(it).toDouble() } ?: min
            val unit = options?.get("unit".js, this)?.orNull?.let { toString(it) } ?: ""
            val decimals = options?.get("decimals".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 2
            val showValue = options?.get("showValue".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: true
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onChange = bindJsCallback(options, "onChange", runtime)
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            JsUiManager.showSlider(title, min, max, step, default, unit, decimals, showValue, confirmText, cancelText, confirmColor, cancelColor, dismissible, onChange, onConfirm, onCancel).await()?.js
        }

        // 加载指示弹窗：ui.loading(title, options?) -> controller{ close(), 关闭(), update(), 更新() }
        // options: { message?, dismissible?, cancelText?, cancelColor?, onDismiss? }  不阻塞 await，返回 controller 供手动关闭/实时更新文字
        listOf("loading".js, "加载".js).func(
            FunctionParam("title"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val options = args[1].orNull as? JsObject
            val message = options?.get("message".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val onDismiss = bindJsCallback(options, "onDismiss", runtime)
            JsUiManager.showLoading(title, message, dismissible, cancelText, cancelColor, onDismiss)
            Object("controller") {
                listOf("close".js, "关闭".js).func { _ ->
                    JsUiManager.hideLoading()
                    Undefined
                }
                listOf("update".js, "更新".js).func(FunctionParam("message")) { updateArgs ->
                    val msg = updateArgs.getOrNull(0).orNull?.let { toString(it) } ?: ""
                    JsUiManager.updateLoading(msg)
                    Undefined
                }
            }
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

            if (path.startsWith("/")) {
                // 绝对路径：使用本地文件系统
                val file = File(path)
                Object("info") {
                    "exists".js eq file.exists().js
                    "isDirectory".js eq file.isDirectory.js
                    "isFile".js eq file.isFile.js
                    "size".js eq (if (file.isFile) file.length() else -1L).js
                    "lastModified".js eq (if (file.exists()) file.lastModified() else 0L).js
                }
            } else {
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

    // ======================== 音频控制 API ========================
    val audio = Object("audio") {
        // 获取背景音乐音量：audio.getBgmVolume() -> number (0.0 ~ 1.0)
        listOf("getBgmVolume".js, "获取背景音乐音量".js).func {
            (bgMusicState?.currentVolume ?: 1.0f).js
        }

        // 设置背景音乐音量：audio.setBgmVolume(volume) -> void
        // volume: 0.0（静音）~ 1.0（最大）
        listOf("setBgmVolume".js, "设置背景音乐音量".js).func(FunctionParam("volume")) { args ->
            val volume = toNumber(args[0]).toFloat().coerceIn(0f, 1f)
            withContext(Dispatchers.Main) {
                bgMusicState?.setVolume(volume)
                Undefined
            }
        }

        // 获取音效音量：audio.getSfxVolume() -> number (0.0 ~ 1.0)
        listOf("getSfxVolume".js, "获取音效音量".js).func {
            SoundController.globalSfxVolume.js
        }

        // 设置音效音量：audio.setSfxVolume(volume) -> void
        // volume: 0.0（静音）~ 1.0（最大），同步更新所有已存在的音效播放器
        listOf("setSfxVolume".js, "设置音效音量".js).func(FunctionParam("volume")) { args ->
            val volume = toNumber(args[0]).toFloat().coerceIn(0f, 1f)
            withContext(Dispatchers.Main) {
                SoundController.globalSfxVolume = volume
                Undefined
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
        listOf("audio".js, "音频".js).forEach { key ->
            runtime.set(key, audio, VariableType.Global)
        }
        listOf("http".js, "网络".js).forEach { key ->
            runtime.set(key, JsHttp.js, VariableType.Global)
        }
        listOf("picker".js, "选择器".js).forEach { key ->
            runtime.set(key, JsPicker.js, VariableType.Global)
        }
        listOf("clipboard".js, "剪切板".js).forEach { key ->
            runtime.set(key, JsClipboard.js, VariableType.Global)
        }
        listOf("device".js, "设备".js).forEach { key ->
            runtime.set(key, JsProperty { JsDevice.js }, VariableType.Global)
        }
        listOf("browser".js, "浏览器".js).forEach { key ->
            runtime.set(key, JsBrowser.js, VariableType.Global)
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

/**
 * 将 JS 传入的 items 数组解析为 [JsChoiceItem] 列表。
 * 支持两种元素：
 * - 字符串 → 视为 name（value 同 name）
 * - 对象   → { name/名称, icon/图标?, value/值? }，value 缺省回退 name
 */
private suspend fun ScriptRuntime.parseChoiceItems(raw: List<*>): List<JsChoiceItem> {
    return raw.mapNotNull { el ->
        when (el) {
            is String -> JsChoiceItem(el, "", el)
            is Number -> JsChoiceItem(el.toString(), "", el.toString())
            is JsObject -> {
                val name = toString((el.get("name".js,this).orNull ?: el.get("名称".js,this).orNull ?: "".js))
                if (name.isBlank()) return@mapNotNull null
                val icon = toString((el.get("icon".js,this).orNull ?: el.get("图标".js,this).orNull ?: "".js))
                val value = toString((el.get("value".js,this).orNull ?: el.get("值".js,this).orNull ?: "".js))
                val itemShowIndex = (el.get("showIndex".js, this).orNull ?: el.get("显示序号".js, this).orNull)
                    ?.let { it.toKotlin(this) as? Boolean }
                val itemShowIndexColor = (el.get("showIndexColor".js, this).orNull ?: el.get("序号颜色".js, this).orNull)
                    ?.let { toString(it) }
                JsChoiceItem(name, icon, value.ifEmpty { name }, itemShowIndex, itemShowIndexColor)
            }
            is JsAny -> {
                val s = toString(el)
                JsChoiceItem(s, "", s)
            }
            else -> {
                val s = el?.toString() ?: return@mapNotNull null
                JsChoiceItem(s, "", s)
            }
        }
    }
}

private suspend fun ScriptRuntime.parseActionItems(raw: List<*>): List<JsActionItem> {
    return raw.mapNotNull { el ->
        when (el) {
            is String -> JsActionItem(el, el, false)
            is Number -> JsActionItem(el.toString(), el.toString(), false)
            is JsObject -> {
                val name = toString((el.get("name".js, this).orNull ?: el.get("名称".js, this).orNull ?: "".js))
                if (name.isBlank()) return@mapNotNull null
                val value = toString((el.get("value".js, this).orNull ?: el.get("值".js, this).orNull ?: "".js))
                val danger = (el.get("danger".js, this).orNull ?: el.get("危险".js, this).orNull)
                    ?.let { it.toKotlin(this) as? Boolean } ?: false
                JsActionItem(name, value.ifEmpty { name }, danger)
            }
            is JsAny -> {
                val s = toString(el)
                JsActionItem(s, s, false)
            }
            else -> {
                val s = el?.toString() ?: return@mapNotNull null
                JsActionItem(s, s, false)
            }
        }
    }
}