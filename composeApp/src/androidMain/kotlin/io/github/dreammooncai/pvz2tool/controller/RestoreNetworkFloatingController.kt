package io.github.dreammooncai.pvz2tool.controller

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.petterp.floatingx.assist.FxAnimation
import com.petterp.floatingx.assist.helper.FxAppHelper
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.compose.FxComposeLifecycleOwner
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.service.LocalVpnService
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.any
import kotlin.time.Duration.Companion.seconds

private class FxComposeViewLifecycleImpl : IFxViewLifecycle {
    private var lifecycleOwner: FxComposeLifecycleOwner? = null

    override fun attach(view: View) {
        // 如果存在viewLifecycle,没有必要开启这些
        if (lifecycleOwner != null || view.findViewTreeLifecycleOwner() != null) return
        lifecycleOwner = FxComposeLifecycleOwner()
        lifecycleOwner?.attachToDecorView(view)
        lifecycleOwner?.onCreate()
        lifecycleOwner?.onStart()

        view.viewTreeObserver.addOnGlobalLayoutListener { // 递归关闭所有父布局的裁剪
            var parent: ViewParent? = view.parent
            while (parent is ViewGroup) {
                parent.clipChildren = false
                parent.clipToPadding = false // 强制父布局重绘，确保属性生效
                parent.invalidate()
                parent = parent.parent
            } // 同时关闭当前View自身的裁剪
            (view as? ViewGroup)?.apply {
                clipChildren = false
                clipToPadding = false
            }
        }
    }

    override fun detached(view: View) {
        lifecycleOwner?.onStop()
        lifecycleOwner?.onDestroy()
        lifecycleOwner?.detachFromDecorView(view)
        lifecycleOwner = null
    }

    override fun windowsVisibility(visibility: Int) {
        if (visibility == VISIBLE) {
            lifecycleOwner?.onResume()
        } else {
            lifecycleOwner?.onPause()
        }
    }
}

private lateinit var control: IFxScopeControl

object RestoreNetworkFloatingController {
    fun showFloatingControl(activity: Activity) {
        control = FxScopeHelper.build {
            addViewLifecycle(FxComposeViewLifecycleImpl())
            setEnableEdgeAdsorption(false)
            setLayoutView(ComposeView(activity).apply {
                setContent {
                    Pvz2ToolTheme {
                        Column(Modifier.padding(20.dp).wrapContentSize()) {
                            Pvz2RestoreNetworkFloating()
                        }
                    }
                }
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            })
        }.toControl(activity)
        control.show()
    }
}

@Composable
private fun Pvz2RestoreNetworkFloating() {
    AnimatedFloatingBounce(
        modifier = Modifier.size(100.dp, 60.dp),
        onDismiss = {
            LocalVpnService.stopVpn(InitializePvz2.context)
            control.cancel()
        }
    ) { exitAnimated ->
        PvzGreenButton("恢复网络", modifier = Modifier.size(100.dp,60.dp)) {
            exitAnimated()
        }
    }
}

/**
 * 悬浮窗专用：弹跳入场 + 缩小淡出退场 动画容器
 * @param onDismiss 退场动画结束后回调（执行关闭悬浮窗/业务逻辑）
 */
@Composable
fun <T> AnimatedFloatingBounce(
    modifier: Modifier = Modifier,
    onDismiss: suspend () -> Unit,
    content: @Composable (exitAnimated: () -> Unit) -> T
) {
    // 1. 初始设为false（隐藏状态），触发入场动画的「0→1」变化
    var show by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // 2. 组件初始化后，自动切换为true，触发入场动画
    LaunchedEffect(Unit) {
        show = true
    }

    // 入场弹跳关键帧（和你原View动画完全一致）
    val bounceInSpec = remember {
        keyframes {
            durationMillis = 800
            0f at 0
            0.95f at 80
            0.85f at 240
            0.95f at 400
            0.9f at 560
            1f at 800
        }
    }

    // 缩放动画：入场用弹跳，退场用线性
    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = if (isExiting) tween(400) else bounceInSpec,
        label = "floating_scale"
    )

    // 透明度动画：和缩放时长同步
    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(if (isExiting) 400 else 800),
        label = "floating_alpha"
    )

    // 退场动画结束后执行业务逻辑
    LaunchedEffect(scale) {
        if (!show && scale == 0f) {
            delay(50) // 兜底延迟，确保动画完全结束
            onDismiss()
        }
    }

    // 应用动画到内容
    Box(
        modifier = modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            alpha = alpha
        )
    ) {
        content {
            isExiting = true
            show = false
        }
    }
}