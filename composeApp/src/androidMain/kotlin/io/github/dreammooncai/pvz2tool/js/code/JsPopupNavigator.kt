package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.ui.dialog.JsPopupItem
import io.github.dreammooncai.pvz2tool.ui.dialog.JsPopupPage
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupNavigator
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup

class JsPopupNavigator(
    private val navigator: PvzPopupNavigator
) {
    val js = Object("nav") {
        listOf("push".js, "进入子页面".js, "pushPage".js).func(FunctionParam("page")) { a ->
            val pageRaw = a.getOrNull(0).orNull as? JsObject
            if (pageRaw != null) {
                val page = parsePopupPage(pageRaw)
                navigator.navigate(SubPopup(page.title, page))
            }
            null
        }
        listOf("pop".js, "返回".js, "back".js).func { _ ->
            navigator.pop()
            null
        }
        listOf("close".js, "关闭".js).func { _ ->
            JsUiManager.hidePopup()
            null
        }
    }

    companion object {
        private suspend fun ScriptRuntime.parsePopupPage(raw: JsObject): JsPopupPage {
            val title = toString((raw.get("title".js, this).orNull ?: raw.get("标题".js, this).orNull ?: "".js))
            val itemsRaw = raw.get("items".js, this).orNull?.toKotlin(this) as? List<*> ?: emptyList<Any>()
            val items = parsePopupItems(itemsRaw)
            val bottomText = raw.get("bottomText".js, this).orNull?.let { toString(it) }
                ?: raw.get("底部文本".js, this).orNull?.let { toString(it) }
            return JsPopupPage(title = title, items = items, bottomText = bottomText)
        }

        context(runtime: ScriptRuntime)
        suspend fun parsePopupItems(raw: List<*>): List<JsPopupItem> {
            return with(runtime) {
                raw.mapNotNull { el ->
                    when (el) {
                        is JsObject -> {
                            val type = toString((el.get("type".js, this).orNull ?: el.get("类型".js, this).orNull ?: "".js))
                            val title = toString((el.get("title".js, this).orNull ?: el.get("标题".js, this).orNull ?: "".js))
                            val value = (el.get("value".js, this).orNull ?: el.get("值".js, this).orNull)
                                ?.let { it.toKotlin(this) as? Boolean } ?: false
                            val text = toString((el.get("text".js, this).orNull ?: el.get("文本".js, this).orNull ?: "".js))
                            val onChangeJs = el.get("onChange".js, this).orNull as? JSFunction
                            val onClickJs = el.get("onClick".js, this).orNull as? JSFunction
                            val onChange: (suspend (JsAny?) -> Unit)? =
                                onChangeJs?.let { fn -> { v -> runCatching { fn.invoke(listOf(v), this) } } }
                            val onClick: (suspend (JsAny?) -> Unit)? =
                                onClickJs?.let { fn -> { v -> runCatching { fn.invoke(listOf(v), this) } } }
                            // 类型推断：未给 type 时按 text(含边距) 渲染；button 类型不再支持
                            val effectiveType = when {
                                type == "switch" || type == "arrow" || type == "text" || type == "spacer" -> type
                                text.isNotBlank() -> "text"
                                else -> "text"
                            }
                            JsPopupItem(
                                type = effectiveType,
                                title = title,
                                value = value,
                                text = text,
                                bare = false,
                                onChange = onChange,
                                onClick = onClick
                            )
                        }
                        is String -> JsPopupItem(type = "text", text = el, bare = true)
                        else -> null
                    }
                }
            }
        }
    }
}