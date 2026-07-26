package io.github.dreammooncai.pvz2tool.js.code

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull

/**
 * 浏览器操作全局对象：`browser`。
 *
 * 调用系统浏览器（或具备 `ACTION_VIEW` 能力的其它应用）打开指定链接。
 * 内部使用 `Intent.ACTION_VIEW` + `FLAG_ACTIVITY_NEW_TASK` 启动，
 * 与复合文本普通链接的跳转行为一致。
 *
 * 用法：
 * ```js
 * // 用系统浏览器打开网页（支持 http/https/ftp/mailto/tel/file 等协议）
 * browser.open("https://github.com");
 * browser.打开("https://www.bing.com");
 *
 * // 未带协议时自动补全 https://
 * browser.open("github.com");
 *
 * // 配合其它 API：打开接口返回的跳转地址
 * let resp = http.get("https://api.example.com/redirect");
 * if (resp && resp.url) browser.openLink(resp.url);
 * ```
 *
 * 注意：若设备上没有可处理该协议的应用（如孤立的 `tel:` 在无电话设备），
 * 启动会静默失败（不抛异常、不影响后续脚本）。
 */
object JsBrowser {

    private val context: Context
        get() = InitializePvz2.context

    /**
     * 规整链接：若未携带任何协议（不含 `://`），则默认补全 `https://`，
     * 以便 `Uri.parse` 能正确识别为网络地址。
     */
    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    /**
     * 使用系统浏览器（或具备 ACTION_VIEW 处理能力的应用）打开链接。
     *
     * @param url 目标地址；支持完整协议（http/https/ftp/mailto/tel/file 等），
     *            未带协议时自动补全 `https://`
     * @return `undefined`
     */
    private fun launchBrowser(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(url))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    val js = Object("browser") {
        // 在系统浏览器中打开链接：browser.open(url) -> void
        listOf("open".js, "打开".js, "打开链接".js, "openLink".js).func(
            FunctionParam("url")
        ) { args ->
            val url = args.getOrNull(0).orNull?.let { toString(it) } ?: ""
            if (url.isNotBlank()) {
                launchBrowser(url)
            }
            Undefined
        }
    }
}
