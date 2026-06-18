package io.github.dreammooncai.pvz2tool.controller

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petterp.floatingx.assist.FxDisplayMode
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.compose.enableComposeSupport
import com.petterp.floatingx.listener.control.IFxScopeControl
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolTheme
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextGreenStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

//region ===================== 通用弹窗控制器（全局管理浮窗弹窗，无业务耦合） =====================
/**
 * 通用悬浮弹窗控制器
 * 支持：任意自定义Compose内容、统一显示/隐藏、单例防重复创建
 */
object GeneralFloatingDialogController {
    val isShow: StateFlow<Boolean> = MutableStateFlow(false)

    private var control: IFxScopeControl? = null
    // 弹窗关闭回调（每次弹窗独立赋值，避免全局冲突）
    private var onDialogDismiss: (() -> Unit)? = null

    /**
     * 显示通用悬浮弹窗
     * @param activity 宿主Activity
     * @param dialogContent 弹窗Compose内容（任意自定义组件）
     * @param onDismiss 弹窗完全关闭后的回调（可选）
     */
    fun showDialog(
        activity: Activity,
        onDismiss: (() -> Unit)? = null,
        dialogContent: @Composable () -> Unit,
    ) {
        onDialogDismiss = onDismiss
        // 已初始化则直接显示，避免重复创建
        if (control != null) {
            (isShow as MutableStateFlow<Boolean>).value = true
            control?.show()
            return
        }

        // 初始化 FloatingX 容器 + Compose 视图
        val composeView = ComposeView(activity).apply {
            setContent {
                Pvz2ToolTheme {
                    dialogContent()
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
        (isShow as MutableStateFlow<Boolean>).value = true
    }

    // 调用示例：弹出退出确认弹窗
    fun showExitConfirm(activity: Activity, onExit: () -> Unit) {
        showDialog(
            activity = activity
        ) { ExitConfirmDialog(onExit) }
    }

    /**
     * 关闭弹窗（统一入口）
     */
    fun dismissDialog() {
        (isShow as MutableStateFlow<Boolean>).value = false
        control?.cancel()
        control = null
        // 执行外部关闭回调并清空
        onDialogDismiss?.invoke()
        onDialogDismiss = null
    }
}
//endregion

//region ===================== 第一层：底层通用弹窗壳（核心基础组件，所有弹窗都基于它） =====================
/**
 * 【底层通用弹窗壳】
 * 能力：全局半透明遮罩 + 入场/退场动画 + 遮罩点击关闭 + 内容插槽
 * 任何自定义弹窗、确认框、提示框都嵌套此组件
 * @param content 弹窗自定义内容插槽
 * @param onDismiss 关闭弹窗回调
 * @param maskClickable 遮罩是否允许点击关闭（默认开启）
 * @param animEnterDuration 入场动画时长
 * @param animExitDuration 退场动画时长
 */
@Composable
fun BaseFloatingDialog(
    content: @Composable () -> Unit,
    onDismiss: () -> Unit = GeneralFloatingDialogController::dismissDialog,
    maskClickable: Boolean = true,
    animEnterDuration: Int = 350,
    animExitDuration: Int = 200
) {
    var showAnim by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // 缩放动画
    val scaleAnim by animateFloatAsState(
        targetValue = if (showAnim) 1f else 0.8f,
        animationSpec = tween(if (isExiting) animExitDuration else animEnterDuration),
        label = "dialog_scale"
    )
    // 透明度动画
    val alphaAnim by animateFloatAsState(
        targetValue = if (showAnim) 1f else 0f,
        animationSpec = tween(if (isExiting) animExitDuration else animEnterDuration),
        label = "dialog_alpha"
    )

    // 入场动画：组件加载完成后播放
    LaunchedEffect(Unit) {
        showAnim = true
    }

    // 退场动画：动画结束后真正关闭弹窗
    LaunchedEffect(showAnim) {
        if (!showAnim && isExiting) {
            delay(animExitDuration.toLong().milliseconds)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 半透明遮罩层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alphaAnim * 0.5f)
                .background(Color.Black.copy(alpha = 0.5f))
                .then(
                    if (maskClickable) {
                        Modifier.clickable {
                            isExiting = true
                            showAnim = false
                        }
                    } else Modifier
                )
        )

        // 弹窗内容容器（动画包裹）
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scaleAnim,
                    scaleY = scaleAnim,
                    alpha = alphaAnim
                )
        ) {
            content()
        }
    }
}
//endregion

//region ===================== 第二层：通用确认弹窗模板（标准双按钮确认框） =====================
/**
 * 【通用确认弹窗模板】
 * 基于 BaseFloatingDialog 封装标准样式：标题 + 描述 + 取消/确认双按钮
 * 可直接复用，也可在此基础上扩展样式
 */
@Composable
fun CommonConfirmDialog(
    title: String,
    message: String,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit = GeneralFloatingDialogController::dismissDialog,
    onConfirm: () -> Unit,
    maskClickable: Boolean = true
) {
    BaseFloatingDialog(
        content = {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .background(
                        color = Color(0xFF2E2E2E),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 标题
                PvzRichText(
                    text = title,
                    defaultStyle = PvzTextGreenStyle,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                // 描述文案
                PvzRichText(
                    text = message,
                    defaultStyle = PvzTextWhiteStyle,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // 按钮行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PvzCancelButton(
                        text = cancelText,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    )
                    PvzGreenButton(
                        text = confirmText,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        onConfirm()
                    }
                }
            }
        },
        maskClickable = maskClickable
    )
}
//endregion

//region ===================== 第三层：业务专属弹窗（退出确认弹窗，通用模板的业务变体） =====================
/**
 * 【业务弹窗：退出确认弹窗】
 * 纯业务层，仅填充业务文案、回调，完全复用通用确认框模板
 * 后续新增业务弹窗，参照此写法即可
 */
@Composable
fun ExitConfirmDialog(onExit: () -> Unit) {
    CommonConfirmDialog(
        title = InitializePvz2.config.ui.settings.exitConfirmTitle,
        message = InitializePvz2.config.ui.settings.exitConfirmMessage,
        cancelText = InitializePvz2.config.ui.dialog.cancel,
        confirmText = InitializePvz2.config.ui.settings.exitConfirmButtonText,
        onConfirm = onExit
    )
}
//endregion

//region ===================== 公共按钮组件（全局复用） =====================
@Composable
private fun PvzCancelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = Color(0xFF555555),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        PvzRichText(
            text = text,
            defaultStyle = PvzTextWhiteStyle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
//endregion