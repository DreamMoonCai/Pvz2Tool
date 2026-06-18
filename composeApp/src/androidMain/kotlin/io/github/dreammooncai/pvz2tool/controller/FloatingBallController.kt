package io.github.dreammooncai.pvz2tool.controller

import android.app.Activity
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.compose.enableComposeSupport
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.control.IFxScopeControl
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.service.LocalVpnService
import io.github.dreammooncai.pvz2tool.ui.main.SettingsDialogState
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.milliseconds

// ============================== 常量定义 ==============================
private object Constants {
    val BALL_SIZE: Dp = 52.dp
    val CARD_WIDTH: Dp = 210.dp
    val CARD_HEIGHT: Dp = 116.dp
    val AUTO_HIDE_DELAY = 5000.milliseconds // 悬浮球完全显示后自动收起时间
    const val ANIMATION_DURATION_IN = 600
    const val ANIMATION_DURATION_OUT = 400
}

// ============================== 全局状态管理 ==============================
private object GlobalState {
    var touchInterceptor: WeakReference<View>? = null
    var touchLock = false // 触摸事件锁，防止状态变化时的误判
    var isSwitchingState = false // 状态切换互斥锁，确保同一时间只有一个切换操作
    var ballControl: IFxScopeControl? = null
    var cardControl: IFxScopeControl? = null
    val isBallExpanded = MutableStateFlow(false)
}

object FloatingBallController {
    val isShow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    // ============================== 公共API ==============================
    fun showFloatingControl(activity: Activity) {
        if (GlobalState.ballControl != null && GlobalState.cardControl != null) return

        // 重置所有状态，防止之前的锁残留
        GlobalState.isSwitchingState = false
        GlobalState.touchLock = false
        GlobalState.isBallExpanded.value = false
        // 强制清理可能残留的拦截层
        forceRemoveTouchInterceptor()

        initBallControl(activity)
        initCardControl(activity)

        GlobalState.ballControl?.show()
        isShow.value = true
    }

    fun dismiss() {
        isShow.value = false
        GlobalState.isBallExpanded.value = false
        GlobalState.isSwitchingState = false
        GlobalState.touchLock = false
        // 强制清理拦截层
        forceRemoveTouchInterceptor()

        GlobalState.ballControl?.cancel()
        GlobalState.cardControl?.cancel()
        GlobalState.ballControl = null
        GlobalState.cardControl = null
    }

    // ============================== 初始化方法 ==============================
    private fun initBallControl(activity: Activity) {
        val ballView = ComposeView(activity).apply {
            id = View.generateViewId()
            setContent {
                Pvz2ToolTheme {
                    FloatingBallContent(activity)
                }
            }
        }

        GlobalState.ballControl = FxScopeHelper.build {
            enableComposeSupport()
            setEnableHalfHide(true)
            setAnimationImpl(FxAnimationImpl(Constants.ANIMATION_DURATION_IN.toLong(), Constants.ANIMATION_DURATION_OUT.toLong()))
            setSaveDirectionImpl(FxConfigStorageToSpImpl("RestoreNetworkBall"))
            setLayoutView(ballView)
        }.toControl(activity)
    }

    private fun initCardControl(activity: Activity) {
        val cardView = ComposeView(activity).apply {
            id = View.generateViewId()
            setContent {
                Pvz2ToolTheme {
                    CardContent()
                }
            }
        }

        GlobalState.cardControl = FxScopeHelper.build {
            enableComposeSupport()
            setEnableHalfHide(false) // 卡片永远禁用半隐藏
            setAnimationImpl(FxAnimationImpl(Constants.ANIMATION_DURATION_IN.toLong(), Constants.ANIMATION_DURATION_OUT.toLong()))
            // 卡片位置与悬浮球完全同步
            setSaveDirectionImpl(object : IFxConfigStorage {
                override fun getX(): Float = GlobalState.ballControl?.getX() ?: 0f
                override fun getY(): Float = GlobalState.ballControl?.getY() ?: 0f
                override fun update(x: Float, y: Float) = Unit
                override fun hasConfig(): Boolean = true
                override fun clear() {}
            })
            setLayoutView(cardView)
        }.toControl(activity)

        // 默认隐藏卡片
        GlobalState.cardControl?.hide()
    }

    // ============================== 状态切换方法 ==============================
    private fun switchToCard(activity: Activity) {
        // 前置状态检查
        if (GlobalState.isSwitchingState || GlobalState.cardControl?.isShow() == true) return

        withStateLock {
            // 强制清理任何可能残留的拦截层
            forceRemoveTouchInterceptor()

            // 同步位置并切换显示
            GlobalState.cardControl?.move(
                GlobalState.ballControl?.getX() ?: 0f,
                GlobalState.ballControl?.getY() ?: 0f,
                false
            )
            GlobalState.ballControl?.hide()
            GlobalState.cardControl?.show()

            // 延迟添加卡片的触摸拦截层，确保卡片完全显示
            if (GlobalState.cardControl?.isShow() == true) {
                addTouchInterceptor(activity) {
                     switchToBall()
                }
            }
        }
    }

    private fun switchToBall() {
        // 前置状态检查
        if (GlobalState.isSwitchingState || GlobalState.ballControl?.isShow() == true) return

        withStateLock {
            // 强制清理任何可能残留的拦截层
            forceRemoveTouchInterceptor()

            GlobalState.isBallExpanded.value = false

            // 同步位置并切换显示
            GlobalState.ballControl?.move(
                GlobalState.cardControl?.getX() ?: 0f,
                GlobalState.cardControl?.getY() ?: 0f,
                false
            )
            GlobalState.cardControl?.hide()
            GlobalState.ballControl?.configControl?.setEnableHalfHide(true, false)
            GlobalState.ballControl?.show()
        }
    }

    // ============================== 触摸拦截层管理（核心修复） ==============================
    private fun addTouchInterceptor(activity: Activity, onOutsideClick: () -> Unit) {
        // 先强制移除任何已存在的拦截层，确保不会有残留
        forceRemoveTouchInterceptor()

        val touchInterceptor = View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnTouchListener { view, event ->
                // 1. 双浮窗同时显示 = 动画过渡中，拦截点击
                val ballShowing = GlobalState.ballControl?.isShow() == true
                val cardShowing = GlobalState.cardControl?.isShow() == true
                if (ballShowing && cardShowing) {
                    return@setOnTouchListener false
                }

                // 2. 全局锁拦截
                if (GlobalState.touchLock || GlobalState.isSwitchingState) {
                    return@setOnTouchListener false
                }

                // ========== 修复 Lint：完整触摸流 + 调用 performClick() ==========
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 业务逻辑：点击外部关闭浮窗
                        val x = event.rawX.toInt()
                        val y = event.rawY.toInt()
                        if (!isTouchOnFloatingView(x, y)) {
                            forceRemoveTouchInterceptor()
                            onOutsideClick()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        // 抬起 = 有效点击，触发系统标准点击回调
                        view.performClick()
                    }
                }
                false
            }
        }

        GlobalState.touchInterceptor = WeakReference(touchInterceptor)

        activity.window.decorView.let { decorView ->
            (decorView as ViewGroup).addView(touchInterceptor)
        }
    }

    // 强制移除拦截层，无论什么状态都执行
    private fun forceRemoveTouchInterceptor() {
        GlobalState.touchInterceptor?.get()?.let { view ->
            try {
                (view.parent as? ViewGroup)?.removeView(view)
            } catch (e: Exception) {
                // 忽略移除失败的异常
            }
            GlobalState.touchInterceptor?.clear()
            GlobalState.touchInterceptor = null
        }
    }

    // ============================== 工具方法 ==============================
    private fun isTouchOnFloatingView(x: Int, y: Int): Boolean {
        // 检查卡片（优先检查，因为卡片显示时悬浮球是隐藏的）
        GlobalState.cardControl?.takeIf { it.isShow() }?.getManagerView()?.let { cardView ->
            val location = IntArray(2)
            cardView.getLocationOnScreen(location)
            val rect = Rect(
                location[0], location[1],
                location[0] + cardView.width, location[1] + cardView.height
            )
            if (rect.contains(x, y)) return true
        }

        // 检查悬浮球
        GlobalState.ballControl?.takeIf { it.isShow() }?.getManagerView()?.let { ballView ->
            val location = IntArray(2)
            ballView.getLocationOnScreen(location)
            val rect = Rect(
                location[0], location[1],
                location[0] + ballView.width, location[1] + ballView.height
            )
            if (rect.contains(x, y)) return true
        }

        return false
    }

    // 状态切换互斥锁：使用全局主线程Handler确保永远解锁
    private inline fun withStateLock(block: () -> Unit) {
        GlobalState.isSwitchingState = true
        GlobalState.touchLock = true

        block()

        GlobalState.touchLock = false
        GlobalState.isSwitchingState = false
    }

    // ============================== Compose组件 ==============================
    @Composable
    private fun FloatingBallContent(activity: Activity) {
        val isExpanded by GlobalState.isBallExpanded.collectAsState()
        val ballControlState = rememberUpdatedState(GlobalState.ballControl)

        // 控制半隐藏状态和触摸拦截层
        DisposableEffect(isExpanded) {
            if (isExpanded && !GlobalState.isSwitchingState) {
                if (GlobalState.isBallExpanded.value && !GlobalState.isSwitchingState && GlobalState.ballControl?.isShow() == true) {
                    ballControlState.value?.configControl?.setEnableHalfHide(false)
                    addTouchInterceptor(activity) {
                        GlobalState.isBallExpanded.value = false
                    }
                }
            } else {
                ballControlState.value?.configControl?.setEnableHalfHide(true)
                forceRemoveTouchInterceptor()
            }

            onDispose {
                forceRemoveTouchInterceptor()
            }
        }

        // 5秒自动收起计时器
        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                delay(Constants.AUTO_HIDE_DELAY)
                // 只有当仍然是展开状态且没有在切换状态时才自动收起
                if (GlobalState.isBallExpanded.value && !GlobalState.isSwitchingState && GlobalState.ballControl?.isShow() == true) {
                    GlobalState.isBallExpanded.value = false
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(Constants.ANIMATION_DURATION_IN)) +
                    scaleIn(tween(Constants.ANIMATION_DURATION_IN), initialScale = 0.5f),
            exit = fadeOut(tween(Constants.ANIMATION_DURATION_OUT)) +
                    scaleOut(tween(Constants.ANIMATION_DURATION_OUT), targetScale = 0.5f),
        ) {
            FloatingBall(
                onClick = {
                    // 只有当没有在切换状态时才响应点击
                    if (!GlobalState.isSwitchingState && !GlobalState.touchLock) {
                        if (isExpanded) {
                            switchToCard(activity)
                        } else {
                            GlobalState.isBallExpanded.value = true
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun FloatingBall(onClick: () -> Unit) {
        Surface(
            modifier = Modifier.size(Constants.BALL_SIZE),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            shadowElevation = 6.dp,
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImageFromAssets(
                    "images/${InitializePvz2.config.ui.assets.floatingBallIcon}",
                    contentDescription = "PVZ2 戴夫",
                    modifier = Modifier
                        .size(Constants.BALL_SIZE - 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    @Composable
    private fun CardContent() {
        CardView(
            onMinimize = {
                // 只有当没有在切换状态时才响应点击
                if (!GlobalState.isSwitchingState && !GlobalState.touchLock) {
                    switchToBall()
                }
            }
        )
    }

    @Composable
    private fun CardView(onMinimize: () -> Unit) {
        val isVpn by LocalVpnService.isVpnActive.collectAsState()
        val context = LocalContext.current

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xE6222222),
            shadowElevation = 10.dp,
            modifier = Modifier.size(Constants.CARD_WIDTH, Constants.CARD_HEIGHT)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotColor = if (isVpn) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "缩小",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { dismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (LocalVpnService.prepareVpn(context) == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        if (isVpn) {
                            PvzGreenButton("恢复网络", modifier = Modifier.size(150.dp, 44.dp)) {
                                LocalVpnService.stopVpn(InitializePvz2.context)
                            }
                        } else {
                            PvzGreenButton("断开网络", modifier = Modifier.size(150.dp, 44.dp)) {
                                runCatching {
                                    LocalVpnService.startVpn(InitializePvz2.context)
                                }.onFailure {
                                    LocalVpnService.stopVpn(InitializePvz2.context)
                                }
                            }
                        }
                    }
                }

                if (SettingsDialogState.isUseCustomGameDisplay) {
                    Spacer(Modifier.padding(top = 10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        val activity = LocalActivity.current ?: return@Box
                        PvzGreenButton("画面设置", modifier = Modifier.size(150.dp, 44.dp)) {
                            GameDisplayFloatingController.show(activity)
                        }
                    }
                }
            }
        }
    }
}