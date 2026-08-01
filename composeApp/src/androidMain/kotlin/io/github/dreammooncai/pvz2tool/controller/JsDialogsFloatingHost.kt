package io.github.dreammooncai.pvz2tool.controller

import android.app.Activity
import androidx.compose.ui.platform.ComposeView
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.compose.enableComposeSupport
import com.petterp.floatingx.listener.control.IFxScopeControl
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.ui.dialog.JsDialogsHost

/**
 * JS 弹窗常驻遮罩宿主。
 *
 * 问题背景：10 个 JS 驱动的弹窗（alert/confirm/popup...）原本只挂在主界面
 * [Pvz2MainScreen] 的 Composition 里。悬浮窗启用、进入游戏后，主界面被游戏窗口
 * 挡在后面，那些 `BasicAlertDialog` 渲染在后台窗口里不可见；悬浮窗自身的
 * Composition 又没有挂这些弹窗，于是游戏内由 JS 回调触发的弹窗「弹不出来」。
 *
 * 解决：本控制器持有一个常驻的 FloatingX 全屏遮罩（独立窗口，盖在游戏上方），
 * 内容为 [JsDialogsHost]——它持续观察 [io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager]
 * 的各 StateFlow。悬浮窗启用期间它就是 JS 弹窗的唯一宿主，弹窗以 `BasicAlertDialog`
 * 自开顶层 Dialog 窗口、全屏盖在游戏上。
 *
 * 空闲态（所有 JS 弹窗均不可见）时 [JsDialogsHost] 不渲染任何内容，Composition 为空，
 * 本遮罩窗口尺寸为 0、不拦截下层（游戏 / 主界面）的触摸。
 *
 * 生命周期：随悬浮窗一起创建/销毁——在 [FloatingBallController.showFloatingControl]
 * 末尾调用 [show]，在 [FloatingBallController.dismiss] 末尾调用 [dismiss]。
 */
object JsDialogsFloatingHost {
    private var control: IFxScopeControl? = null

    /** 是否处于常驻显示状态 */
    val isShow: Boolean get() = control != null

    /**
     * 显示 JS 弹窗常驻遮罩（幂等：已创建则直接返回）。
     * 应在悬浮窗实际创建后调用，以确保游戏内 JS 弹窗可正常弹出。
     */
    fun show(activity: Activity) {
        if (control != null) return
        val composeView = ComposeView(activity).apply {
            setContent {
                Pvz2ToolTheme {
                    JsDialogsHost()
                }
            }
        }
        control = FxScopeHelper.build {
            enableComposeSupport()
            setEnableEdgeAdsorption(false)
            setLayoutView(composeView)
            setDisplayMode(FxDisplayMode.ClickOnly)
        }.toControl(activity)
        control?.show()
    }

    /**
     * 关闭并销毁常驻遮罩（幂等）。在悬浮窗销毁时调用，避免后台残留空窗口。
     */
    fun dismiss() {
        control?.cancel()
        control = null
    }
}
