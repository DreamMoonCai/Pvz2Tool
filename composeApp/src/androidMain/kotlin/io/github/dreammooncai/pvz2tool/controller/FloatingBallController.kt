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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.compose.enableComposeSupport
import com.petterp.floatingx.listener.IFxConfigStorage
import com.petterp.floatingx.listener.control.IFxScopeControl
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.icon.CloseFrame
import io.github.dreammooncai.pvz2tool.icon.CloseFramePress
import io.github.dreammooncai.pvz2tool.FloatingWindowItem
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.JsRichTextRefresher
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.rememberJsVisibility
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzDialogCard
import io.github.dreammooncai.pvz2tool.ui.main.RenderColoredButton
import io.github.dreammooncai.pvz2tool.view.ImageSvgButton
import io.github.dreammooncai.pvz2tool.view.PvzBlueButton
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzOrangeButton
import io.github.dreammooncai.pvz2tool.view.PvzPurpleButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.milliseconds

// ============================== 常量定义 ==============================
private object Constants {
    val BALL_SIZE: Dp = 52.dp
    val CARD_WIDTH: Dp = 210.dp
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
                    JsFileResolver.resolvePlaceholders(InitializePvz2.config.ui.assets.floatingBallIcon).let { if (it.startsWith("/")) it else "images/$it" },
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
        CardView()
    }

    @Composable
    private fun CardView() {
        val hostActivity = LocalActivity.current
        val scope = rememberCoroutineScope()
        val floatingConfig = InitializePvz2.config.ui.floatingWindow
        val items = floatingConfig.items

        // 逐项求值 isShowFromJs / isShowFromJsPath（两者皆空视为始终显示）。求值订阅 JsRichTextRefresher，
        // 因此任意脚本执行后按钮可实时显隐（如 VPN 授权完成、画面设置开关切换）。
        // 注意：composable 调用必须在 if 之外、按固定顺序执行，故用 forEach 收集而非 filter 内联判断。
        val visibleItems = ArrayList<FloatingWindowItem>(items.size)
        items.forEach { item ->
            if (rememberJsVisibility(item.isShowFromJs, item.isShowFromJsPath)) visibleItems += item
        }

        // 关闭走与 GameDisplayFloatingController 一致的【全屏】二次确认弹窗：
        // 复用 GeneralFloatingDialogController（独立 FloatingX 全屏遮罩层），覆盖整屏而非仅卡片范围。
        Column(
            modifier = Modifier.width(Constants.CARD_WIDTH),
            horizontalAlignment = Alignment.End
        ) {
            ImageSvgButton(
                imageVector = Pvz2Icon.CloseFrame,
                imageVectorPress = Pvz2Icon.CloseFramePress,
                contentDescription = InitializePvz2.config.ui.dialog.deleteSaveDesc,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp, 36.dp),
                onClick = {
                    if (hostActivity != null) {
                        GeneralFloatingDialogController.showDialog(hostActivity) {
                            CommonConfirmDialog(
                                title = InitializePvz2.config.ui.settings.floatingExitConfirmTitle,
                                message = InitializePvz2.config.ui.settings.floatingExitConfirmMessage,
                                cancelText = InitializePvz2.config.ui.dialog.cancel,
                                confirmText = InitializePvz2.config.ui.settings.floatingExitConfirmButtonText,
                                onConfirm = {
                                    GeneralFloatingDialogController.dismissDialog()
                                    dismiss()
                                }
                            )
                        }
                    } else {
                        dismiss()
                    }
                },
                pressSound = InitializePvz2.config.ui.sounds.buttonSettingsPress,
                releaseSound = InitializePvz2.config.ui.sounds.buttonSettingsRelease
            )

            PvzDialogCard(
                title = null,
                modifier = Modifier.width(Constants.CARD_WIDTH)
            ) {
                if (visibleItems.isEmpty()) {
                    // 区分「压根没配」与「配了但全被 isShowFromJs 隐藏」，便于排查配置问题
                    PvzRichText(
                        text = if (items.isEmpty()) floatingConfig.emptyTip else floatingConfig.allHiddenTip,
                        fontSize = 12.sp,
                        defaultStyle = PvzTextOliveStyle,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        visibleItems.forEach { item ->
                            FloatingActionButton(item = item, scope = scope)
                        }
                    }
                }
            }
        }
    }

    /**
     * 单个悬浮窗功能按钮：渲染为整宽彩色按钮，点击执行其 JS 脚本（jsScript 优先，jsPath 兜底）。
     * 按钮颜色由 item.buttonColor 决定（blue/red/green/orange/purple）。
     */
    @Composable
    private fun FloatingActionButton(item: FloatingWindowItem, scope: CoroutineScope) {
        val label = item.displayName
        val resolvedColor = (item.buttonColor ?: "blue").lowercase()
        val modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        val onClick = {
            val script = item.jsScript?.takeIf { it.isNotBlank() }
                ?: item.jsPath?.let { path ->
                    AssetExtractorHolder.openInputStream(JsFileResolver.resolvePlaceholders(path))
                        ?.use { it.bufferedReader().readText() }
                }
            if (script != null) {
                scope.launch {
                    runCatching { PvzToolJsEngine.executeScript(script) }
                    // 通知所有 {{js:...}} 复合文本重算，使按钮文案（如「断开网络」↔「恢复网络」）
                    // 能随本次脚本改变的状态即时更新。
                    JsRichTextRefresher.refresh()
                }
            }
        }
        RenderColoredButton(resolvedColor,text = label, modifier = modifier, onClick = onClick)
    }
}