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
import io.github.dreammooncai.pvz2tool.ScheduleDef
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsLogger
import io.github.dreammooncai.pvz2tool.js.JsLogLevel
import io.github.dreammooncai.pvz2tool.ui.dialog.JsVideoManager
import io.github.dreammooncai.pvz2tool.js.code.JsDevice
import io.github.dreammooncai.pvz2tool.js.code.JsNative
import io.github.dreammooncai.pvz2tool.js.code.JsApp
import io.github.dreammooncai.pvz2tool.js.code.JsDex
import io.github.dreammooncai.pvz2tool.js.code.JsReflect
import io.github.dreammooncai.pvz2tool.js.code.JsBrowser
import io.github.dreammooncai.pvz2tool.js.code.JsToast
import io.github.dreammooncai.pvz2tool.js.code.JsThread
import io.github.dreammooncai.pvz2tool.NotificationPermission
import io.github.dreammooncai.pvz2tool.js.JsRichTextRefresher
import io.github.dreammooncai.util.ContextUtil
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.js.toBoolean
import io.github.dreammooncai.pvz2tool.js.toNumber
import io.github.dreammooncai.pvz2tool.timer.TimerManager
import io.github.dreammooncai.pvz2tool.pop.plugin.crypt.Pvz2NumberCrypt
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import androidx.core.app.NotificationCompat
import android.content.Context
import android.os.Build
import io.github.dreammooncai.pvz2tool.ui.dialog.JsActionItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsChoiceItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.ui.dialog.JsPopupItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsPopupPage
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupNavigator
import io.github.dreammooncai.pvz2tool.controller.PvzMusicController
import io.github.dreammooncai.util.getAssetLastModified
import io.github.dreammooncai.util.isAssetDirExist
import io.github.dreammooncai.util.isAssetFileExist
import io.github.dreammooncai.util.openUriInputStreamOrAssetNull
import android.app.Activity
import androidx.core.graphics.drawable.IconCompat
import io.github.dreammooncai.pvz2tool.controller.GameDisplayFloatingController
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine.getJsToolContext
import io.github.dreammooncai.pvz2tool.service.LocalVpnService
import io.github.dreammooncai.pvz2tool.ui.main.SettingsDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import android.util.Base64 as AndroidBase64

/**
 * 仅当该全局名尚未被 keight 暴露时才注入，避免覆盖引擎自带的同名实现（若有）。
 * keight 只内置最小全局集，绝大多数宿主环境函数（URI 编解码 / base64 / escape 等）都不存在，
 * 但用 setIfAbsent 注册可保证「无则补、有则不碰」的安全语义。
 */
private suspend fun ScriptRuntime.setIfAbsent(key: JsAny, value: JsAny, type: VariableType = VariableType.Global) {
    val existing = runCatching { get(key) }.getOrNull()
    if (existing == null || existing is Undefined) set(key, value, type)
}

/** JS atob：base64 -> 二进制字符串（每字符一字节，Latin-1），对齐浏览器语义。标准字母表优先，失败回退 URL-safe */
private fun jsAtob(encoded: String): String = runCatching {
    val clean = encoded.replace(Regex("\\s"), "")
    val bytes = runCatching { AndroidBase64.decode(clean, AndroidBase64.DEFAULT or AndroidBase64.NO_WRAP) }
        .getOrElse { AndroidBase64.decode(clean, AndroidBase64.URL_SAFE or AndroidBase64.NO_WRAP) }
    bytes.joinToString("") { (it.toInt() and 0xFF).toChar().toString() }
}.getOrDefault("")

/** JS btoa：二进制字符串 -> base64（超 255 的字符取低字节，对齐不可信输入兜底） */
private fun jsBtoa(data: String): String =
    AndroidBase64.encodeToString(data.map { (it.code and 0xFF).toByte() }.toByteArray(), AndroidBase64.NO_WRAP)

/** JS escape：仅 A-Za-z0-9 @*_+-./ 与空格保留（空格->%20），其余 Latin1->%XX、非 Latin1->%uXXXX */
private fun jsEscape(s: String): String = buildString {
    for (ch in s) {
        when {
            ch.isLetterOrDigit() || ch in "@*_+-./" -> append(ch)
            ch == ' ' -> append("%20")
            ch.code < 256 -> append("%${ch.code.toString(16).padStart(2, '0').uppercase()}")
            else -> append("%u${ch.code.toString(16).padStart(4, '0').uppercase()}")
        }
    }
}

/** JS unescape：反转义 %XX / %uXXXX（无法解析的 % 序列原样保留） */
private fun jsUnescape(s: String): String = buildString {
    var i = 0
    while (i < s.length) {
        val c = s[i]
        var consumed = 1
        if (c == '%' && i + 1 < s.length && s[i + 1] == 'u' && i + 6 <= s.length) {
            s.substring(i + 2, i + 6).toIntOrNull(16)?.let { ch -> append(ch.toChar()); consumed = 6 }
        }
        if (consumed == 1 && c == '%' && i + 3 <= s.length) {
            s.substring(i + 1, i + 3).toIntOrNull(16)?.let { ch -> append(ch.toChar()); consumed = 3 }
        }
        if (consumed == 1) append(c)
        i += consumed
    }
}

object PvzToolGlobals {

    /**
     * 将 options 中的 JS 函数字段绑定为 Kotlin 挂起回调（value 透传给 JS 函数作为第一个参数）。
     *
     * 🔴 必须等 JS 函数体**真正跑完**（尤其是 async 函数体里 await 之后的代码）：
     * 否则回调一返回，「当前回调帧」槽位就被还原成 null，
     * 回调内 await 之后才执行的 `当前.backUI(...)` 拿不到帧 → 「点确认只关弹窗、不返回数据」。
     *
     * 🔴 等待方式经实测确定（见 jvmTest/…/KeightScopeProbeTest）：
     * - 用 `call(...)` 而**不是** `invoke(...)`：对 async 函数体，invoke 只返回 JsPromiseWrapper 且**不等待**；
     * - `toKotlin()` 要**直接**调，不能先 `as? Callable` 再调（那样恒为 null）；
     * - 同步函数体 → toKotlin 得到完成值（非 Job），说明 invoke/call 返回时函数体已跑完，无需等待；
     *   异步函数体 → 得到 Job，join() 才会推进到 await 之后。
     *
     * 🔴 thisArg 必须传 globalThis，**不能传 null**：`invoke()` 的 this 是 globalThis，
     * 而 `call(null, ...)` 的 this 会是 null，两者不一致会让回调里 `this.xxx` 的写法静默失效。
     */
    private suspend fun bindJsCallback(
        options: JsObject?,
        key: String,
        runtime: ScriptRuntime
    ): (suspend (JsAny?) -> Unit)? {
        val jsFn = options?.get(key.js, runtime)?.orNull as? JSFunction ?: return null
        return { value ->
            runCatching {
                val ret = jsFn.invoke(listOf(value), runtime)
                val job = ret?.toKotlin(runtime) as? Job
                job?.join()
            }.onFailure { JsConsole.error("回调 $key 执行失败:", it) }
        }
    }

    val ui = Object("ui") { // 提示弹窗（单按钮）：ui.alert(title, message, options?) -> void
        listOf("alert".js, "提示".js).func(
            FunctionParam("title"), FunctionParam("message"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val message = messageToString(args[1], this)
            val options = args[2].orNull as? JsObject
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showAlert(getJsToolContext(),title, message, confirmText, confirmColor, dismissible, onConfirm, onResult).await()
            null
        }

        // 确认弹窗：ui.confirm(title, message, options?) -> boolean
        listOf("confirm".js, "确认".js).func(
            FunctionParam("title"), FunctionParam("message"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val message = messageToString(args[1], this)
            val options = args[2].orNull as? JsObject
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确认"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = options?.get("dismissible".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("可关闭".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showConfirm(getJsToolContext(),title, message, confirmText, cancelText, confirmColor, cancelColor, dismissible, onConfirm, onCancel, onResult).await().js
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
            val message = messageToString(args[1], this)
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
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showPrompt(getJsToolContext(),title, message, defaultValue, placeholder, confirmText, cancelText, confirmColor, cancelColor, dismissible, onConfirm, onCancel, onResult).await()?.js
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
                    null
                }

                // 关闭进度弹窗（正常完成）
                listOf("close".js, "关闭".js).func { _ ->
                    JsUiManager.closeProgress()
                    null
                }

                // 主动取消进度（效果同点击“取消”按钮）：隐藏弹窗并触发 onCancel
                listOf("cancel".js, "取消".js).func { _ ->
                    JsUiManager.cancelProgress()
                    null
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
            null
        }

        // 单项选择弹窗：ui.select(title, items, options?) -> string|null
        // items: 字符串数组 或 对象数组 [{name, icon?, value?, showIndex?, showIndexColor?}]
        // options: { columns?, cancelable?, showIndex?, showIndexColor?, confirmText?, cancelText?, confirmColor?, cancelColor?, forceMaxForm?, onCancel?, onSelect? }
        //   forceMaxForm: true 时以最高形态展示（内容区固定上限高度，跳过探测重测，适合内容很多的场景）
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
            val forceMaxForm = options?.get("forceMaxForm".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("最高形态".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showSelect(getJsToolContext(),title, items, columns, cancelable, showIndex, showIndexColor, confirmText, cancelText, confirmColor, cancelColor, forceMaxForm, onCancel, onSelect, onResult).await()?.js
        }

        // 多项选择弹窗：ui.multiSelect(title, items, options?) -> string[]
        // options: { defaultValues?, columns?, cancelable?, showIndex?, showIndexColor?, confirmText?, cancelText?, confirmColor?, cancelColor?, forceMaxForm?, onCancel?, onSelect? }
        //   forceMaxForm: true 时以最高形态展示（内容区固定上限高度，跳过探测重测，适合内容很多的场景）
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
            val forceMaxForm = options?.get("forceMaxForm".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("最高形态".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showMultiSelect(getJsToolContext(),title, items, defaultValues, columns, cancelable, showIndex, showIndexColor, confirmText, cancelText, confirmColor, cancelColor, forceMaxForm, onCancel, onSelect, onResult).await().map { it.js }.js
        }

        // 操作菜单弹窗：ui.actionSheet(title, actions, options?) -> string|null
        // actions: 字符串数组 或 对象数组 [{name, value?, danger?}]
        // options: { cancelable?, cancelText?, cancelColor?, forceMaxForm?, onCancel?, onSelect? }  点击某项返回其 value（或 name）；取消/点外部返回 null
        //   forceMaxForm: true 时以最高形态展示（内容区固定上限高度，跳过探测重测，适合内容很多的场景）
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
            val forceMaxForm = options?.get("forceMaxForm".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean }
                ?: options?.get("最高形态".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: false
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onSelect = bindJsCallback(options, "onSelect", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showActionSheet(getJsToolContext(),title, items, cancelable, cancelText, cancelColor, forceMaxForm, onCancel, onSelect, onResult).await()?.js
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
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showSlider(getJsToolContext(),title, min, max, step, default, unit, decimals, showValue, confirmText, cancelText, confirmColor, cancelColor, dismissible, onChange, onConfirm, onCancel, onResult).await()?.js
        }

        // 数量输入弹窗：ui.number(title, options?) -> number | null
        // 形态：[«] [−] 数值 [+] [»]，单击步进 / 长按连续增减 / «» 直达最小最大
        // options: { min?, max?, step?, default?, suffix?(或 unit?),
        //            showMinMaxButtons?(是否显示 «» 按钮，默认 true),
        //            confirmText?, cancelText?, confirmColor?, cancelColor?, dismissible?/可关闭?,
        //            onChange?, onConfirm?, onCancel?, onResult? }
        listOf("number".js, "数量".js, "数量输入".js).func(
            FunctionParam("title"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val options = args[1].orNull as? JsObject
            val min = options?.get("min".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 0
            val max = options?.get("max".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 100
            val step = options?.get("step".js, this)?.orNull?.let { toNumber(it).toInt() } ?: 1
            val default = options?.get("default".js, this)?.orNull?.let { toNumber(it).toInt() } ?: min
            val suffix = options?.get("suffix".js, this)?.orNull?.let { toString(it) }
                ?: options?.get("unit".js, this)?.orNull?.let { toString(it) } ?: ""
            // 布尔一律走字符串判定（toBoolean 的数值投影在不同场景下不可靠）
            val showMinMax = options?.get("showMinMaxButtons".js, this)?.orNull
                ?.let { toString(it).trim().lowercase() != "false" } ?: true
            val confirmText = options?.get("confirmText".js, this)?.orNull?.let { toString(it) } ?: "确定"
            val cancelText = options?.get("cancelText".js, this)?.orNull?.let { toString(it) } ?: "取消"
            val confirmColor = options?.get("confirmColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val cancelColor = options?.get("cancelColor".js, this)?.orNull?.let { toString(it) } ?: ""
            val dismissible = (options?.get("dismissible".js, this)?.orNull
                ?: options?.get("可关闭".js, this)?.orNull)
                ?.let { toString(it).trim().lowercase() == "true" } ?: false
            val onChange = bindJsCallback(options, "onChange", runtime)
            val onConfirm = bindJsCallback(options, "onConfirm", runtime)
            val onCancel = bindJsCallback(options, "onCancel", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showNumber(
                getJsToolContext(), title, min, max, step, default, suffix, showMinMax,
                confirmText, cancelText, confirmColor, cancelColor, dismissible,
                onChange, onConfirm, onCancel, onResult
            ).await()?.js
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
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showLoading(getJsToolContext(),title, message, dismissible, cancelText, cancelColor, onDismiss, onResult)
            Object("controller") {
                listOf("close".js, "关闭".js).func { _ ->
                    JsUiManager.hideLoading()
                    null
                }
                listOf("update".js, "更新".js).func(FunctionParam("message")) { updateArgs ->
                    val msg = updateArgs.getOrNull(0).orNull?.let { toString(it) } ?: ""
                    JsUiManager.updateLoading(msg)
                    null
                }
            }
        }

        // 弹出游戏画面设置浮窗（与悬浮球"画面设置"按钮同款全屏弹窗）：ui.showGameDisplay() / ui.弹出画面设置() / ui.画面设置()
        // 通过 ContextUtil.getCurrentActivity() 取当前前台 Activity（拿不到则回退到全局 Context，须为 Activity 才能弹窗）
        listOf("showGameDisplay".js, "弹出画面设置".js, "画面设置".js).func {
            withContext(Dispatchers.Main) {
                val activity = ContextUtil.getCurrentActivity() ?: (InitializePvz2.context as? Activity)
                if (activity != null) {
                    runCatching { GameDisplayFloatingController.show(activity) }
                }
            }
            null
        }

        // 「自定义游戏画面」开关是否已开启（决定画面设置功能可不可用）：
        // ui.isCustomGameDisplayEnabled() / ui.是否启用自定义画面() / ui.画面设置是否可用()
        // 常用于悬浮窗按钮的 isShowFromJs，未开启时自动隐藏「画面设置」按钮。
        listOf("isCustomGameDisplayEnabled".js, "是否启用自定义画面".js, "画面设置是否可用".js).func {
            runCatching { SettingsDialogState.isUseCustomGameDisplay }.getOrDefault(false).js
        }

        // 主动刷新所有复合文本（{{js:...}} 文本与 isShowFromJs 动态显隐）：
        // ui.refreshAll() / ui.刷新所有() / ui.刷新复合文本()
        // 当脚本改变了某个运行时状态、但本次交互未由系统自动触发刷新时（例如通过 ui.confirm 回调、
        // 定时器、网络回调等异步路径修改状态），可手动调用本方法让所有复合文本立即重算。
        // 内部仅广播一次重算信号，订阅侧（复合文本与动态显隐）会合并后统一刷新，不会死循环。
        listOf("refreshAll".js, "刷新所有".js, "刷新复合文本".js).func {
            JsRichTextRefresher.refresh()
            null
        }

        // 通用弹窗（设置风格，支持子页面）：ui.popup(title, items, options?) / ui.弹出(...) / ui.对话框(...)
        // items: 数组，每项可为 字符串 或 对象 { type:"switch"|"arrow"|"text"|"spacer", title, value, text, onChange, onClick }
        //   - 字符串项："普通文本" → 自动按 text 渲染（无额外内边距，bare）
        //   - 对象 type 省略且有文本 → 按 text 渲染（含边距，仿开关行仅文字）
        //   - switch: value 为初始开关状态；onChange(newValue) 在状态变化时回调
        //   - arrow: onClick(nav) 点击回调，nav 为导航对象：nav.push(page) 进入子页面、nav.pop() 返回、nav.close() 关闭
        //   - text: 纯文本（bare 无内边距 / 含边距两种，见上）；spacer: 间距
        // 子页面 page = { title, items, bottomText? }，结构与主页面一致（items 同上）
        // options.onClose: 弹窗关闭时回调（返回/关闭按钮/nav.close 均触发）
        listOf("popup".js, "弹出".js, "对话框".js, "showPopup".js).func(
            FunctionParam("title"), FunctionParam("items"), FunctionParam("options")
        ) { args ->
            val runtime = this
            val title = toString(args[0])
            val itemsRaw = args.getOrNull(1).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val options = args[2].orNull as? JsObject
            val items = JsPopupNavigator.parsePopupItems(itemsRaw)
            val onClose = bindJsCallback(options, "onClose", runtime)
            val onResult = bindJsCallback(options, "onResult", runtime)
            JsUiManager.showPopup(getJsToolContext(),title, items, onClose, onResult)
            null
        }
    }

    // ======================== VPN 控制 API ========================
    val vpn = Object("vpn") {
        // 断开网络（开启 VPN 拦截）：vpn.disconnect() / vpn.断网() / vpn.断开网络()
        listOf("disconnect".js, "断网".js, "断开网络".js).func {
            withContext(Dispatchers.Main) {
                runCatching { LocalVpnService.startVpn(InitializePvz2.context) }.onFailure {
                    LocalVpnService.stopVpn(InitializePvz2.context)
                }
            }
            null
        }

        // 恢复网络（关闭 VPN）：vpn.restore() / vpn.恢复() / vpn.恢复网络()
        listOf("restore".js, "恢复".js, "恢复网络".js).func {
            withContext(Dispatchers.Main) {
                runCatching { LocalVpnService.stopVpn(InitializePvz2.context) }
            }
            null
        }

        // 当前 VPN 是否处于激活状态（即是否处于断网状态）：vpn.isActive() / vpn.是否激活() / vpn.是否开启()
        listOf("isActive".js, "是否激活".js, "是否开启".js, "是否已开启".js).func {
            LocalVpnService.isVpnActive.value.js
        }

        // VPN 是否已获得系统授权（等价于 Kotlin 侧 LocalVpnService.prepareVpn(context) == null）：
        // vpn.isPrepared() / vpn.是否已授权() / vpn.已授权() / vpn.是否可用()
        // 返回 false 表示尚未授权，此时调用 disconnect() 不会真正断网（需先在系统弹窗中授权）。
        // 常用于悬浮窗按钮的 isShowFromJs，未授权时自动隐藏断网按钮。
        listOf("isPrepared".js, "是否已授权".js, "已授权".js, "是否可用".js).func {
            withContext(Dispatchers.Main) {
                val context = ContextUtil.getCurrentActivity() ?: InitializePvz2.context
                runCatching { LocalVpnService.prepareVpn(context) == null }.getOrDefault(false)
            }.js
        }
    }

    val notifications = Object("notifications") {
        listOf("show".js, "显示".js).func(
            FunctionParam("title"), FunctionParam("message"), FunctionParam("options")
        ) { args ->
            val title = toString(args[0]); val message = messageToString(args[1], this)
            val opts = args[2].orNull as? JsObject
            val ctx = InitializePvz2.context
            // Android 13+ 需要在 Manifest 声明并运行时授予 POST_NOTIFICATIONS，未授权时系统会静默丢弃通知。
            // 这里提前检测：未授权则不发送并输出 JS 控制台警告（不阻断 JS 调用），调用方拿到 null 即表示本次未发出。
            if (!NotificationPermission.isGranted(ctx)) {
                JsConsole.warn("未授予通知权限(POST_NOTIFICATIONS)，通知未发送（可在系统设置中开启通知）")
                return@func null
            }
            val cid = opts?.get("channelId".js, this)?.orNull?.let { toString(it) } ?: "pvz2tool_default"
            val cname = opts?.get("channelName".js, this)?.orNull?.let { toString(it) } ?: "工具箱通知"
            val icon = opts?.get("icon".js, this)?.orNull?.let { toString(it) }
            val auto = opts?.get("autoCancel".js, this)?.orNull?.let { it.toKotlin(this) as? Boolean } ?: true
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
                if (nm.getNotificationChannel(cid) == null)
                    nm.createNotificationChannel(NotificationChannel(cid, cname, AndroidNotificationManager.IMPORTANCE_DEFAULT))
            }
            val id = (System.currentTimeMillis() % 100000).toInt()
            // 通知 small icon：应用图标可能为 0（未在 <application> 声明 icon）
            // 兜底：生成 1px 白色 Bitmap 通过 IconCompat 传入，100% 可靠
            val smallIcon = ctx.applicationInfo.icon.takeIf { it != 0 }
            val b = NotificationCompat.Builder(ctx, cid)
                .setContentTitle(title).setContentText(message)
                .setAutoCancel(auto)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            if (smallIcon != null) {
                b.setSmallIcon(smallIcon)
            } else {
                b.setSmallIcon(android.R.drawable.ic_popup_reminder)
            }
            if (icon != null) {
                val iconRes = AssetExtractorHolder.openInputStream(icon)
                if (iconRes != null) b.setSmallIcon(IconCompat.createWithBitmap(android.graphics.BitmapFactory.decodeStream(iconRes)))
            }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager).notify(id, b.build())
            id.js
        }
        listOf("cancel".js, "取消".js).func(FunctionParam("id")) { args ->
            val id = args[0].orNull?.let { toNumber(it).toInt() } ?: return@func null
            (InitializePvz2.context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager).cancel(id)
            null
        }
        // 检测通知权限是否已授予（Android 13+ 需运行时授权；低于 13 恒为 true）
        // 用法：notifications.isGranted() / 通知.已授权()
        listOf("isGranted".js, "已授权".js).func { _ ->
            NotificationPermission.isGranted(InitializePvz2.context).js
        }
    }

    val timer = Object("timer") {
        listOf("schedule".js, "注册".js).func(
            FunctionParam("id"), FunctionParam("name"), FunctionParam("cron"), FunctionParam("script")
        ) { args ->
            val id = toString(args[0]); val name = toString(args[1])
            val cron = toString(args[2]); val script = toString(args[3])
            TimerManager.addOrUpdate(InitializePvz2.context,
                ScheduleDef(id = id, name = name, cron = cron, jsScript = script))
            id.js
        }
        listOf("list".js, "列表".js).func { _ ->
            val timers = TimerManager.loadTimers(InitializePvz2.context)
            PvzToolJsEngine.parse(Json.encodeToString(ListSerializer(ScheduleDef.serializer()), timers))
        }
        listOf("cancel".js, "取消".js).func(FunctionParam("id")) { args ->
            TimerManager.remove(InitializePvz2.context, toString(args[0])).js
        }
        listOf("cancelAll".js, "全部取消".js).func { _ ->
            TimerManager.cancelAll(InitializePvz2.context); null
        }
        listOf("nextTrigger".js, "下次".js).func { _ ->
            val id = currentTimerId
            if (id != null) for (t in TimerManager.loadTimers(InitializePvz2.context)) {
                if (t.id == id) { TimerManager.schedule(InitializePvz2.context, t); break }
            }
            null
        }
    }

    @Volatile var currentTimerId: String? = null

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

        // 获取资源信息：assets.info(path) -> { exists, isDirectory, isFile, size, lastModified }
        // 工作目录优先：本地覆盖 > APK Assets（含绝对路径）
        listOf("info".js, "信息".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val info = AssetExtractorHolder.resourceInfo(path)
            Object("info") {
                "exists".js eq info.exists.js
                "isDirectory".js eq info.isDirectory.js
                "isFile".js eq info.isFile.js
                "size".js eq info.size.js
                "lastModified".js eq info.lastModified.js
            }
        }

        // 读取资源为字符串：assets.read(path) -> string
        listOf("read".js, "读取".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                null
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                inputStream?.bufferedReader()?.use { it.readText() }?.js
            }
        }

        // 读取资源为字节数组：assets.readBytes(path) -> Uint8Array
        listOf("readBytes".js, "读取字节".js).func(FunctionParam("path")) { args ->
            val path = toString(args[0])
            val uri = AssetExtractorHolder.open(path)
            if (uri == null) {
                null
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    null
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
                null
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    null
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
                null
            } else {
                val inputStream = InitializePvz2.context.openUriInputStreamOrAssetNull(uri)
                if (inputStream == null) {
                    null
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
            PvzMusicController.getBgmCurrentVolume().js
        }

        // 设置背景音乐音量：audio.setBgmVolume(volume) -> void
        // volume: 0.0（静音）~ 1.0（最大）
        listOf("setBgmVolume".js, "设置背景音乐音量".js).func(FunctionParam("volume")) { args ->
            val volume = toNumber(args[0]).toFloat().coerceIn(0f, 1f)
            withContext(Dispatchers.Main) {
                PvzMusicController.setBgmVolume(volume)
                null
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
                null
            }
        }

        // 播放音乐：audio.playMusic(url, options?) -> void
        // url: 音频路径（支持 URL / 绝对路径 / 占位符 $WORK_DIR 等 / 相对 sound/ 或 assets/pvz2tool/sound/ 兜底）
        // options: { volume?:0~1, loop?:bool, fade?:ms(默认1000) }
        //   volume 缺省（不传）= 跟随 audio.setBgmVolume 的当前值，且播放期间改动实时生效；显式传值则固定该音量。
        // 全局唯一：新调用会先结束并释放上一段，绝不会叠加出多段音乐。
        // 播放期间背景音乐让位（渐隐暂停）；即使此时用户在设置里打开背景音乐、或从后台返回界面，
        // 背景音乐也会保持静默，直到本段音乐播放结束 / 被 stopMusic / 被新的 playMusic 取代后统一恢复。
        listOf("playMusic".js, "播放音乐".js).func(FunctionParam("url"), FunctionParam("options")) { args ->
            val url = toString(args[0])
            if (url.isBlank()) return@func Undefined
            val opts = args.getOrNull(1)?.orNull as? JsObject
            // 未指定 volume → null → 控制器按「跟随 setBgmVolume」处理
            val volume = opts?.get("volume".js, this)?.orNull?.let { toNumber(it).toFloat() }?.coerceIn(0f, 1f)
            val loop = opts?.get("loop".js, this)?.orNull?.toBoolean() ?: false
            val fade = opts?.get("fade".js, this)?.orNull?.let { toNumber(it).toLong() }?.coerceAtLeast(0) ?: 1000L
            val audioUrl = PvzMusicController.resolveAudioUri(url)
            withContext(Dispatchers.Main) {
                PvzMusicController.playMusic(audioUrl, loop, volume, fade)
                null
            }
        }

        // 停止当前音乐：audio.stopMusic() -> void（若其曾让背景音乐让位，则随之恢复）
        listOf("stopMusic".js, "停止音乐".js).func {
            withContext(Dispatchers.Main) {
                PvzMusicController.stopMusic()
                null
            }
        }

        // 查询音乐是否正在播放：audio.isMusicPlaying() -> bool
        listOf("isMusicPlaying".js, "是否正在播放音乐".js).func {
            PvzMusicController.isMusicPlaying().js
        }

        // 播放音效（不受背景音乐影响）：audio.playSound(url, volume?) -> void
        // 路径解析同 playMusic；volume 可选（0~1，默认跟随全局音效音量）
        listOf("playSound".js, "播放音效".js).func(FunctionParam("url"), FunctionParam("volume")) { args ->
            val url = toString(args[0])
            if (url.isBlank()) return@func Undefined
            val vol = args.getOrNull(1)?.orNull?.let { toNumber(it).toFloat() }?.coerceIn(0f, 1f)
            withContext(Dispatchers.Main) {
                if (vol != null) SoundController.playSoundFromAssets(url, vol)
                else SoundController.playSoundFromAssets(url)
                null
            }
        }
    }

    // ======================== 视频播放 API ========================
    /**
     * 视频播放。画面承载在 Compose 弹窗（`JsVideoDialog`）上，与 `ui.alert` / `ui.popup` 同一套弹窗栈，
     * 主界面与悬浮窗都能弹出，**不新起 Activity**；播放链路直接复用游戏内 CG 播放器
     * （ExoPlayer → MediaPlayer → 海报三级自动降级、加载动画、跳过按钮、后台自动暂停），
     * 全局唯一：新的 play 会替换上一段。
     */
    val video = Object("video") {
        // 播放视频：video.play(url, options?) -> void
        // url: 视频路径（支持 URL / 绝对路径 / 占位符 $WORK_DIR 等 / 资源层本地覆盖与 APK assets / video- 目录兜底）
        // options: { loop?:bool, volume?:0~1, poster?:string(海报兜底图), start?:ms(起始位置) }
        listOf("play".js, "播放".js).func(FunctionParam("url"), FunctionParam("options")) { args ->
            val url = toString(args[0])
            if (url.isBlank()) return@func Undefined
            val opts = args.getOrNull(1)?.orNull as? JsObject
            val loop = opts?.get("loop".js, this)?.orNull?.toBoolean() ?: false
            val volume = opts?.get("volume".js, this)?.orNull?.let { toNumber(it).toFloat() }?.coerceIn(0f, 1f) ?: 1f
            val poster = opts?.get("poster".js, this)?.orNull?.let { toString(it) }?.takeIf { it.isNotBlank() }
            val start = opts?.get("start".js, this)?.orNull?.let { toNumber(it).toLong() } ?: 0L
            val videoPath = JsVideoManager.resolveVideoPath(url)
            withContext(Dispatchers.Main) {
                JsVideoManager.play(videoPath, loop, volume, poster, start)
                null
            }
        }

        // 暂停播放（画面保留）：video.pause() -> void
        listOf("pause".js, "暂停".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.pause(); null }
        }

        // 继续播放：video.resume() -> void
        listOf("resume".js, "继续".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.resume(); null }
        }

        // 停止播放并关闭弹窗：video.stop() -> void
        listOf("stop".js, "停止".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.stop(); null }
        }

        // 跳转：video.seekTo(positionMs) -> void
        listOf("seekTo".js, "跳转".js).func(FunctionParam("position")) { args ->
            val ms = toNumber(args[0]).toLong().coerceAtLeast(0L)
            withContext(Dispatchers.Main) { JsVideoManager.seekTo(ms); null }
        }

        // 设置音量：video.setVolume(volume) -> void（0~1）
        listOf("setVolume".js, "设置音量".js).func(FunctionParam("volume")) { args ->
            val v = toNumber(args[0]).toFloat().coerceIn(0f, 1f)
            withContext(Dispatchers.Main) { JsVideoManager.setVolume(v); null }
        }

        // 是否正在播放：video.isPlaying() -> bool
        listOf("isPlaying".js, "是否播放中".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.isPlaying().js }
        }

        // 视频弹窗是否正在显示：video.isVisible() -> bool
        listOf("isVisible".js, "是否显示中".js).func {
            JsVideoManager.isVisible().js
        }

        // 总时长（毫秒，未知为 0）：video.getDuration() -> number
        listOf("getDuration".js, "获取时长".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.duration().js }
        }

        // 当前播放位置（毫秒）：video.getPosition() -> number
        listOf("getPosition".js, "获取位置".js).func {
            withContext(Dispatchers.Main) { JsVideoManager.position().js }
        }
    }

    // ======================== 日志 API ========================
    /**
     * 读取 / 写入工具箱的日志（即界面上「日志」面板显示的那份，最多保留 200 条）。
     * 可用于脚本内自检、把关键信息写进日志面板、或导出日志排查问题。
     */
    val log = Object("log") {
        // 获取日志数组：log.get(options?) -> [{ level, message, time }]
        // options: { limit?:number(取最近 N 条), level?:'info'|'warn'|'error'|'success'(按级别过滤) }
        listOf("get".js, "获取".js).func(FunctionParam("options")) { args ->
            val opts = args.getOrNull(0)?.orNull as? JsObject
            // 🔴 options 里的数字/布尔必须走 toNumber/toBoolean（context receiver），
            // 直接 `as? Number` / `as? Boolean` 拿到的是 keight 包装对象，参数会被静默忽略。
            val limit = opts?.get("limit".js, this)?.orNull?.let { toNumber(it).toInt() } ?: -1
            val level = opts?.get("level".js, this)?.orNull?.let { toString(it).trim().lowercase() }
                ?.takeIf { it.isNotEmpty() }
            var entries = JsLogger.logs.value
            if (!level.isNullOrEmpty()) {
                entries = entries.filter { it.level.name.lowercase() == level }
            }
            if (limit > 0) entries = entries.takeLast(limit)
            entries.map { entry ->
                Object("entry") {
                    "level".js eq entry.level.name.lowercase().js
                    "message".js eq entry.message.js
                    "time".js eq entry.timestamp.js
                } as JsAny
            }.js
        }

        // 获取日志纯文本（每行一条，便于写文件）：log.text(options?) -> string
        listOf("text".js, "文本".js).func(FunctionParam("options")) { args ->
            val opts = args.getOrNull(0)?.orNull as? JsObject
            val limit = opts?.get("limit".js, this)?.orNull?.let { toNumber(it).toInt() } ?: -1
            var entries = JsLogger.logs.value
            if (limit > 0) entries = entries.takeLast(limit)
            entries.joinToString("\n") { "[${it.timestamp}] ${it.level.name} ${it.message}" }.js
        }

        // 追加一条日志：log.add(message, level?) -> void
        // level: 'info'(默认) | 'warn' | 'error' | 'success'
        listOf("add".js, "添加".js, "write".js, "写入".js).func(
            FunctionParam("message"),
            FunctionParam("level")
        ) { args ->
            val message = toString(args[0])
            val levelName = args.getOrNull(1)?.orNull?.let { toString(it).trim().lowercase() }
                ?.takeIf { it.isNotEmpty() }
            val level = when (levelName) {
                "warn", "警告", "warning" -> JsLogLevel.WARN
                "error", "错误" -> JsLogLevel.ERROR
                "success", "成功" -> JsLogLevel.SUCCESS
                else -> JsLogLevel.INFO
            }
            JsLogger.appendLog(level, message)
            Undefined
        }

        // 清空日志：log.clear() -> void
        listOf("clear".js, "清空".js).func {
            JsLogger.clearLogs()
            Undefined
        }

        // 当前日志条数：log.count() -> number
        listOf("count".js, "数量".js).func {
            JsLogger.logs.value.size.js
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
        listOf("console".js, "控制台".js,"JsConsole".js).forEach { key ->
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
        listOf("video".js, "视频".js).forEach { key ->
            runtime.set(key, video, VariableType.Global)
        }
        listOf("log".js, "日志记录".js).forEach { key ->
            runtime.set(key, log, VariableType.Global)
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
        listOf("toast".js, "吐司".js).forEach { key ->
            runtime.set(key, JsToast.js, VariableType.Global)
        }
        listOf("thread".js, "协程".js, "线程".js).forEach { key ->
            runtime.set(key, JsThread.js, VariableType.Global)
        }
        listOf("app".js, "应用".js).forEach { key ->
            runtime.set(key, JsApp.js, VariableType.Global)
        }
        listOf("dex".js, "dex加载".js).forEach { key ->
            runtime.set(key, JsDex.js, VariableType.Global)
        }
        listOf("reflect".js, "反射".js).forEach { key ->
            runtime.set(key, JsReflect.js, VariableType.Global)
        }
        listOf("native".js, "原生".js, "so加载".js).forEach { key ->
            runtime.set(key, JsNative.js, VariableType.Global)
        }
        listOf("vpn".js, "虚拟专网".js, "VPN".js).forEach { key ->
            runtime.set(key, vpn, VariableType.Global)
        }
        listOf("notifications".js, "通知".js).forEach { key ->
            runtime.set(key, notifications, VariableType.Global)
        }
        listOf("timer".js, "定时器".js).forEach { key ->
            runtime.set(key, timer, VariableType.Global)
        }
        listOf("server".js, "本地服务器".js, "服务器".js).forEach { key ->
            runtime.set(key, JsServer.js, VariableType.Global)
        }
        // ======================== 标准 URI / base64 / escape 全局函数 ========================
        // keight 只内置最小全局集（连 JSON 都是手动注册的），标准 ECMAScript「宿主环境」函数从未暴露，
        // 脚本里用到 decodeURIComponent / atob / escape 等会直接 ReferenceError。
        // 这里补齐浏览器标准集，实现用 Java 标准库，语义对齐 JS：
        //   - Component 系列对空格编码为 %20、对 ! ~ * ' ( ) 等保留字符不编码（与浏览器一致），'+' 按字面量处理；
        //   - encodeURI 比 encodeURIComponent 再多保留一组 URI 保留字符（; / ? : @ & = + $ , #）。
        //   - atob/btoa 为 base64 编解码（二进制字符串，对齐浏览器语义）；escape/unescape 为遗留转义。
        // 解码/解析失败时返回原串或空串，比 JS 抛异常更宽容，适合工具箱脚本对不可信输入做兜底。
        // ★ 注册统一走 setIfAbsent：keight 若未来自带这些实现则不加覆盖；缺失则补齐，双向安全。
        val uriFns = Object("__uri") {
            listOf("decodeURIComponent".js, "解码URI组件".js).func(FunctionParam("encodedURIString")) { args ->
                val s = toString(args[0])
                runCatching { URLDecoder.decode(s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s).js
            }
            listOf("encodeURIComponent".js, "编码URI组件".js).func(FunctionParam("uriComponent")) { args ->
                val s = toString(args[0])
                URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20")
                    .replace("%21", "!").replace("%27", "'")
                    .replace("%28", "(").replace("%29", ")").replace("%7E", "~").js
            }
            listOf("decodeURI".js, "解码URI".js).func(FunctionParam("encodedURI")) { args ->
                val s = toString(args[0])
                runCatching { URLDecoder.decode(s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s).js
            }
            listOf("encodeURI".js, "编码URI".js).func(FunctionParam("uri")) { args ->
                val s = toString(args[0])
                URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20")
                    .replace("%21", "!").replace("%27", "'")
                    .replace("%28", "(").replace("%29", ")").replace("%7E", "~")
                    .replace("%3B", ";").replace("%2F", "/").replace("%3F", "?")
                    .replace("%3A", ":").replace("%40", "@").replace("%26", "&")
                    .replace("%3D", "=").replace("%2B", "+").replace("%24", "$")
                    .replace("%2C", ",").replace("%23", "#").js
            }
            listOf("atob".js).func(FunctionParam("encodedData")) { args -> jsAtob(toString(args[0])).js }
            listOf("btoa".js).func(FunctionParam("stringToEncode")) { args -> jsBtoa(toString(args[0])).js }
            listOf("escape".js).func(FunctionParam("str")) { args -> jsEscape(toString(args[0])).js }
            listOf("unescape".js).func(FunctionParam("str")) { args -> jsUnescape(toString(args[0])).js }
        }
        listOf(
            "decodeURIComponent", "解码URI组件",
            "encodeURIComponent", "编码URI组件",
            "decodeURI", "解码URI",
            "encodeURI", "编码URI",
            "atob", "btoa", "escape", "unescape"
        ).forEach { name ->
            uriFns.get(name.js, runtime)?.let { runtime.setIfAbsent(name.js, it) }
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
/**
 * 将 JS 侧传入的 message 转为展示字符串。
 * - 普通字符串原样返回；
 * - 数组（如 ui.confirm 传入 [文本, 箭头项对象]）逐元素转换：字符串保留，对象提取
 *   title/text/name/label 字段，避免出现无意义的 `[object Object]`。
 */
private suspend fun messageToString(arg: JsAny?, runtime: ScriptRuntime): String {
    val converted = runCatching { arg?.toKotlin(runtime) }.getOrNull()
    when (converted) {
        is List<*> -> {
            val text = converted.mapNotNull { elementToText(it) }
                .joinToString("\n")
                .replace(Regex("\\n{2,}"), "\n")
                .trim()
            return if (text.isBlank()) runtime.toString(arg) else text
        }
        is Map<*, *> -> {
            val t = elementTitle(converted)
            return t ?: runtime.toString(arg)
        }
    }
    return runtime.toString(arg)
}

private fun elementToText(el: Any?): String? = when (el) {
    is String -> el
    is Map<*, *> -> elementTitle(el)
    else -> el?.toString()?.takeIf { it != "[object Object]" && it.isNotBlank() }
}

private fun elementTitle(map: Map<*, *>): String? =
    listOf("title", "text", "name", "label")
        .firstNotNullOfOrNull { key -> (map[key] as? String)?.takeIf { it.isNotBlank() } }