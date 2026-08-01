package io.github.dreammooncai.pvz2tool.js

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 复合文本（{{js:...}}）重算信号中心。
 *
 * ## 背景
 * 复合文本里的 `{{js:表达式}}` 只在「表达式内容 / JS 执行上下文」变化时才会重新求值。
 * 但像下面这种写法，文本内容与上下文都没变，仅仅是 JS 侧的运行时状态变了：
 *
 * ```yaml
 * - id: vpn_toggle
 *   name: "{{js:vpn.isActive() ? '恢复网络' : '断开网络'}}"
 *   jsScript: "vpn.isActive() ? vpn.restore() : vpn.disconnect();"
 * ```
 *
 * 点击按钮后 `vpn.isActive()` 的结果已经翻转，但 UI 上的 `name` 不会跟着变。
 *
 * ## 机制
 * 任何**用户交互触发**的 JS 脚本（BUTTON 点击 / CHECKBOX 勾选 / SLIDER 拖动结束 /
 * 悬浮窗按钮 / 复合文本链接点击）执行完毕后调用 [refresh]，
 * [revision] 自增一次；所有含 `{{js:...}}` 的 [io.github.dreammooncai.pvz2tool.view.PvzRichText]
 * 会订阅该信号并重新求值，从而让 `name` / `desc` / `buttonText` 等文本立即刷新。
 *
 * ## 防循环
 * 复合文本自身求值时使用 `isRichText = true`，**不会**触发 [refresh]，因此不存在
 * 「求值 → 刷新 → 再求值」的死循环。
 *
 * ## 防抖
 * 订阅侧使用 `collectLatest + 短延迟` 合并连续信号（例如首屏多个 DESCRIPTION 依次执行 JS），
 * 避免同一帧内重复求值。
 */
object JsRichTextRefresher {

    private val _revision = MutableStateFlow(0)

    /** 复合文本重算版本号，每次 [refresh] 自增。订阅方据此重新求值 `{{js:...}}`。 */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * 通知所有含 `{{js:...}}` 的复合文本重新计算。
     *
     * 线程安全，可在任意线程 / 协程调度器上调用。
     */
    fun refresh() {
        _revision.update { it + 1 }
    }
}
