package io.github.dreammooncai.pvz2tool.js.code

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.func

/**
 * 剪切板操作全局对象：`clipboard`。
 *
 * 基于 Android [ClipboardManager]（通过 [InitializePvz2.context] 获取），
 * 支持复制文本到系统剪切板、读取剪切板当前文本内容、清空剪切板。
 *
 * 用法：
 * ```js
 * // 复制指定字符串到剪切板
 * clipboard.copy("这是要复制的内容");
 * clipboard.复制("中文也行");
 *
 * // 读取剪切板当前文本（无内容时返回 undefined）
 * let text = clipboard.read();
 * if (text !== undefined) console.log("剪切板内容:", text);
 *
 * // 清空剪切板
 * clipboard.clear();
 * ```
 *
 * 注意：Android 10（API 29）及以上，应用仅能在自身处于前台（有焦点）时读取剪切板，
 * 本工具的 JS 运行于前台界面，因此读取可正常工作。
 */
object JsClipboard {

    private val context: Context
        get() = InitializePvz2.context

    private fun clipboardManager(): ClipboardManager? {
        return runCatching {
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        }.getOrNull()
    }

    val js = Object("clipboard") {
        // 复制文本到系统剪切板：clipboard.copy(text) -> void
        listOf("copy".js, "复制".js, "写入".js).func(
            FunctionParam("text")
        ) { args ->
            val text = args.getOrNull(0)?.let { toString(it) } ?: ""
            clipboardManager()?.setPrimaryClip(ClipData.newPlainText("pvz2tool", text))
            Undefined
        }

        // 读取剪切板当前文本：clipboard.read() -> string | undefined（无内容/失败时）
        listOf("read".js, "读取".js, "粘贴".js).func {
            val text = clipboardManager()?.primaryClip?.let { clip ->
                if (clip.itemCount <= 0) return@let null
                runCatching {
                    clip.getItemAt(0).let { item ->
                        item.text?.toString() ?: item.coerceToText(context)?.toString()
                    }
                }.getOrNull()
            }
            if (text == null) Undefined else text.js
        }

        // 清空剪切板：clipboard.clear() -> void
        listOf("clear".js, "清空".js).func {
            clipboardManager()?.setPrimaryClip(ClipData.newPlainText("", ""))
            Undefined
        }
    }
}
