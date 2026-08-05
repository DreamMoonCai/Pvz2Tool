package io.github.dreammooncai.pvz2tool.js

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.view.JsExecutionContext
import io.github.dreammooncai.pvz2tool.view.LocalJsExecutionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/** 连续信号合并延迟（毫秒），与复合文本重算保持一致 */
private const val VISIBILITY_DEBOUNCE_MS = 60L

/**
 * 基于 JS 表达式的「可见性」求值工具。
 *
 * ## 用途
 * 让配置项（如悬浮窗按钮 `isShowFromJs`）能用一行 JS 表达式描述「什么情况下才显示我」，
 * 把原本写死在 Kotlin 里的显示条件（例如 `LocalVpnService.prepareVpn(context) == null`）
 * 下放到 yml 配置，由 JS 全局 API 提供判定能力：
 *
 * ```yaml
 * - id: vpn_toggle
 *   isShowFromJs: "vpn.isPrepared()"              # VPN 已授权才显示断网按钮
 * - id: game_display
 *   isShowFromJs: "ui.isCustomGameDisplayEnabled()" # 开启自定义画面才显示画面设置
 * ```
 *
 * ## 动态重算
 * 订阅 [JsRichTextRefresher.revision]：任何用户交互触发的 JS 脚本执行完毕后都会广播一次，
 * 此处随之重新求值，因此按钮可以随运行时状态（授权、开关、存档等）实时显隐，
 * 与 `{{js:...}}` 动态文案的刷新时机完全一致。
 *
 * ## 防循环
 * 求值优先走 [PvzToolJsEngine.executeScript] 的**带上下文重载**（当 [LocalJsExecutionContext] 提供上下文时，
 * JS 可访问 `this.当前`）；无上下文时降级为无上下文重载。两种重载都不会触发
 * [JsRichTextRefresher.refresh]，所以不存在「求值 → 刷新 → 再求值」死循环。
 */
object JsVisibility {

    /**
     * 按 JS 语义把脚本返回值解析为布尔：
     * 空串 / `false` / `0` / `null` / `undefined` / `NaN` 视为 false，其余为 true。
     *
     * 注：JS 执行异常时 [PvzToolJsEngine.executeScript] 返回空串，会被判为 false（保守隐藏）。
     */
    fun parseBoolean(raw: String): Boolean {
        val v = raw.trim()
        if (v.isEmpty()) return false
        return when (v.lowercase()) {
            "false", "0", "null", "undefined", "nan" -> false
            else -> true
        }
    }

    /**
     * 执行一次可见性表达式，异常一律视为不可见。
     *
     * 分两阶段以保证结果稳定：
     * 1. 优先包一层 `String(!!(表达式))` —— 在 JS 侧先做真值转换再转字符串，
     *    返回值必定是 `"true"` / `"false"`，规避不同引擎对布尔 `toString()` 的表示差异。
     * 2. 若包装后语法不成立（例如表达式其实是多语句脚本），回退为原样执行 + 真值解析。
     */
    suspend fun evaluate(expression: String, context: JsExecutionContext? = null): Boolean {
        val expr = expression.trim().trimEnd(';').trim()
        if (expr.isEmpty()) return false
        // 有上下文时走带上下文重载（JS 可访问 this.当前），否则走无上下文降级重载
        val runner: suspend (String) -> String = { script ->
            if (context != null) {
                // 可见性求值本身就在 JsRichTextRefresher.revision 的 collectLatest 订阅内重算，
                // 必须 isRichText = true 抑制末尾的 refresh()，否则会触发「求值→刷新→再求值」死循环。
                PvzToolJsEngine.executeScript(
                    script = script,
                    section = context.section,
                    item = context.item,
                    version = context.version,
                    sectionStates = context.sectionStates,
                    isRichText = true,
                    updateSectionState = context.updateSectionState,
                    source = "可见性求值"
                )
            } else {
                PvzToolJsEngine.executeScript(script, source = "可见性求值")
            }
        }
        runCatching { runner("String(!!($expr))") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return parseBoolean(it) }
        return runCatching { parseBoolean(runner(expression)) }.getOrDefault(false)
    }

    /**
     * 读取路径指向的可见性脚本内容。
     *
     * 路径解析与其它 JS 路径一致：先做占位符展开（`$WORK_DIR` / `$JS_DIR` 等），
     * 再按「绝对路径 > 本地工作目录 > APK Assets」优先级查找。
     * 读不到（文件缺失 / IO 异常）返回 null。
     *
     * 注：刻意不做内容缓存 —— 可见性脚本通常只有一行，且缓存会让用户热改脚本文件后不生效。
     */
    suspend fun readScript(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            AssetExtractorHolder.openInputStream(JsFileResolver.resolvePlaceholders(path))
                ?.use { it.bufferedReader().readText() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * 解析并求值可见性条件：`expression` 优先，为空时回退到 `path` 指向的脚本文件。
     *
     * @return 两者都为空 → true（无条件显示）；有条件但求值失败/异常 → false（保守隐藏）。
     */
    suspend fun evaluate(expression: String?, path: String?, context: JsExecutionContext? = null): Boolean {
        expression?.takeIf { it.isNotBlank() }?.let { return evaluate(it, context) }
        // 两者皆空 = 未配置条件 → 无条件显示
        val scriptPath = path?.takeIf { it.isNotBlank() } ?: return true
        // 配了路径却读不到文件：视为条件不成立（隐藏），避免静默展示不可用功能
        return readScript(scriptPath)?.let { evaluate(it, context) } ?: false
    }
}

/**
 * 求值 JS 可见性条件，并在 [JsRichTextRefresher] 广播时自动重算。
 *
 * 与 `{{js:...}}` 动态文案共用同一条刷新通道：任意 BUTTON / CHECKBOX / SLIDER / INPUT
 * 交互执行脚本后都会重新判定，因此配置项可以随运行时状态实时显隐。
 *
 * @param expression 可见性表达式（裸表达式，不需要 `{{js:}}` 包裹）
 * @param expressionPath 可见性脚本文件路径；仅当 [expression] 为空时生效
 * @return 是否应当显示。两者皆空 → 恒 true 且完全不接触 JS 引擎；
 *         有条件时求值完成前返回 false，避免「先闪现再消失」。
 */
@Composable
fun rememberJsVisibility(expression: String?, expressionPath: String? = null): Boolean {
    // 在组合体内捕获当前 JS 执行上下文（Provider 提供或默认版本上下文），供可见性求值优先使用带上下文重载
    val jsContext = LocalJsExecutionContext.current
    val expr = expression?.takeIf { it.isNotBlank() }
    val path = expressionPath?.takeIf { it.isNotBlank() }
    // 未配置任何条件：走快路径，不建 produceState、不订阅信号
    if (expr == null && path == null) return true
    // 起始 false：未求值完成前先不渲染，避免不该显示的项闪现一帧
    val visible by produceState(false, expr, path, jsContext) {
        var isFirstEmission = true
        JsRichTextRefresher.revision.collectLatest {
            if (!isFirstEmission) delay(VISIBILITY_DEBOUNCE_MS)
            isFirstEmission = false
            value = JsVisibility.evaluate(expr, path, jsContext)
        }
    }
    return visible
}
