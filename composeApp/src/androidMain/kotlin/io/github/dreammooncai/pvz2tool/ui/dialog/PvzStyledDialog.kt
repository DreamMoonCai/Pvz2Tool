package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import io.github.dreammooncai.pvz2tool.view.PerfectAdaptiveLayout
import io.github.dreammooncai.pvz2tool.view.PvzProgressBar
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * PVZ 风格弹窗卡片外壳：奶黄渐变底 + 三层绿框 + 绿色标题栏。
 * [PvzStyledDialog] 与悬浮窗确认框（CommonConfirmDialog）共用，保证风格一致、单一来源。
 */
@Composable
fun PvzDialogCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    /**
     * 标题栏右侧的操作插槽（如多选弹窗的「全选 / 搜索」按钮）。
     * 传了内容时标题改为居中布局 + 右侧浮层，按钮不挤占标题。
     */
    headerTrailingContent: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFFF3EEB9), Color(0xFFF2EDBB))),
                RoundedCornerShape(15.dp)
            )
            .border(3.dp, Color(0xFF344702), RoundedCornerShape(15.dp))
            .padding(2.dp)
            .border(5.dp, Color(0xFF8ED229), RoundedCornerShape(15.dp))
            .padding(0.5.dp)
            .border(1.dp, Color(0xFF78A52B), RoundedCornerShape(15.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = if (title != null) 10.dp else 8.dp)
        ) {
            if (title != null) {
                // 标题栏：标题**优先居中**；只有居中会压到右侧按钮时，才向左让出「差的那部分」。
                // 用 onSizeChanged 量三个宽度来算位移（不能用 BoxWithConstraints：
                // 它是 SubcomposeLayout，在本弹窗的 intrinsic 测量探测下会崩）。
                var barWidthPx by remember { mutableIntStateOf(0) }
                var titleWidthPx by remember { mutableIntStateOf(0) }
                var trailingWidthPx by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val gapPx = with(density) { 8.dp.toPx() }

                val centeredLeft = (barWidthPx - titleWidthPx) / 2f
                val rightLimit = barWidthPx - trailingWidthPx - gapPx
                // 居中时标题右缘超出按钮左缘多少 → 需要左移多少（且不超过「贴左」）
                val overflow = centeredLeft + titleWidthPx - rightLimit
                val shiftPx = overflow.coerceIn(0f, centeredLeft.coerceAtLeast(0f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color(0xFF88CD23), Color(0xFF97DC02))))
                        .padding(vertical = 15.dp)
                        .onSizeChanged { barWidthPx = it.width },
                    contentAlignment = Alignment.Center
                ) {
                    PvzRichText(
                        title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null),
                        modifier = Modifier
                            .onSizeChanged { titleWidthPx = it.width }
                            .offset { IntOffset(-shiftPx.roundToInt(), 0) }
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .onSizeChanged { trailingWidthPx = it.width },
                        verticalAlignment = Alignment.CenterVertically,
                        content = headerTrailingContent
                    )
                }
                Box(modifier = Modifier.height(20.dp))
            }
            content()
        }
    }
}

/**
 * PVZ风格通用弹窗框架（支持滚动 + 底部固定内容，和Popup行为一致）
 * @param isVisible 是否显示弹窗
 * @param titleText 弹窗标题
 * @param onDismissRequest 关闭弹窗的回调
 * @param dismissible 点击外部是否可关闭（默认false）
 * @param content 中间可滚动内容区域
 * @param bottomContent 底部固定内容（按钮区域）
 */
@Composable
fun PvzStyledDialog(
    isVisible: Boolean,
    titleText: String,
    onDismissRequest: () -> Unit,
    dismissible: Boolean = false,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    forceMaxForm: Boolean = false,
    /** 标题栏右侧操作插槽（如多选弹窗的「全选 / 搜索」） */
    headerTrailingContent: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable (ColumnScope.() -> Unit) = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = dismissible,
                usePlatformDefaultWidth = false
            )
        ) {
            PvzDialogCard(
                title = titleText,
                modifier = Modifier.fillMaxWidth(0.5f),
                headerTrailingContent = headerTrailingContent
            ) {
                    // 核心：和 PvzPopupContent 完全一致的 探测+滚动显示 逻辑
                    PerfectAdaptiveLayout(
                        height = 0.dp,
                        heightRange = 0.dp .. 250.dp,
                        forceMaxForm = forceMaxForm,
                        // 探测层：无滚动、无fillMaxHeight，测量真实内容高度
                        probeContent = {
                            Column(
                                modifier,
                                verticalArrangement = verticalArrangement,
                                horizontalAlignment = horizontalAlignment
                            ) {
                                content()
                            }
                        },
                        // 显示层
                        displayContent = {
                            if (forceMaxForm) {
                                // 最高形态：内容区固定上限高度，滚动交由内部 Lazy 容器自身处理，
                                // 外层不再包 verticalScroll（否则 verticalScroll 会把无限高度约束传给
                                // Lazy 容器，导致其退化成全量测量，失去懒加载意义）。
                                // 仍 fillMaxHeight 以便内部 Lazy 容器拿到固定视口高度（250dp），正常懒加载+滚动。
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = verticalArrangement,
                                    horizontalAlignment = horizontalAlignment
                                ) {
                                    content()
                                }
                            } else {
                                // 自适应模式：固定高度 + verticalScroll 真正滚动
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .verticalScroll(scrollState),
                                    verticalArrangement = verticalArrangement,
                                    horizontalAlignment = horizontalAlignment
                                ) {
                                    content()
                                }
                            }
                        },
                        // 底部固定栏
                        bottomContent = {
                            Column(
                                modifier,
                                verticalArrangement = verticalArrangement,
                                horizontalAlignment = horizontalAlignment
                            ) {
                                bottomContent()
                            }
                        }
                    )
            }
        }
    }
}

/**
 * 全屏加载指示（半透明遮罩 + 居中 loading 图 + 图下方文字）。
 *
 * 与 [PvzStyledDialog] 不同，本组件不包裹绿框对话框，而是以全屏 Dialog 形式覆盖
 * 整个屏幕：背景为半透明黑色阴影遮罩，loading 图居中（默认 184.dp），
 * 文字显示在图片下方。可复用于任意需要全屏加载提示的场景（非 JS 专属）。
 *
 * @param isVisible   是否显示
 * @param title       主标题（显示在图片下方，可空）
 * @param message     说明文字（显示在标题下方，可空）
 * @param dismissible 点击遮罩或取消按钮是否可关闭（默认 false）
 * @param cancelText  取消按钮文字（仅 dismissible 时显示）
 * @param cancelColor 取消按钮自定义底色（Color?，为 null 用主题红）
 * @param onDismiss   关闭前回调（suspend，仅 dismissible 时点击遮罩/取消触发）
 * @param onClose     实际关闭回调（由宿主提供，例如 JsUiManager.hideLoading）
 */
@Composable
fun PvzLoadingDialog(
    isVisible: Boolean,
    title: String = "",
    message: String = "",
    dismissible: Boolean = false,
    cancelText: String = "取消",
    cancelColor: Color? = null,
    onDismiss: (suspend () -> Unit)? = null,
    onClose: suspend () -> Unit,
) {
    if (!isVisible) return
    val handleDismiss: () -> Unit = {
        if (dismissible) {
            // 🔴 用与弹窗生命周期解耦的稳定作用域（JsUiManager.uiScope）：
            // onDismiss 是 JS 注册的 onCancel（用户 JS），若其执行期间弹窗被关（scope 被 forget），
            // 仍在挂起（keight jsFn.call）的协程 resume 即抛 ForgottenCoroutineScopeException；统一走 uiScope 规避。
            JsUiManager.uiScope.launch {
                onDismiss?.invoke()
                onClose()
            }
        }
    }
    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = dismissible,
            decorFitsSystemWindows = false
        )
    ) {
        // 使背景真正铺满整屏（含异形屏区域）。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = dismissible, onClick = handleDismiss),
            contentAlignment = Alignment.Center
        ) {
            // 内层：在安全区(safeDrawing)内居中，避开刘海与底部手势条。
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImageFromAssets(
                        "images/loading.gif",
                        modifier = Modifier.size(184.dp),
                        contentDescription = null
                    )
                    if (title.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PvzRichText(
                            title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null)
                        )
                    }
                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PvzRichText(
                            message,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null)
                        )
                    }
                    if (dismissible) {
                        Spacer(modifier = Modifier.height(20.dp))
                        PvzRedButton(
                            text = cancelText,
                            backgroundColor = cancelColor,
                            modifier = Modifier
                                .width(160.dp)
                                .height(46.dp),
                            onClick = handleDismiss
                        )
                    }
                }
            }
        }
    }
}

/**
 * 全屏进度弹窗：半透明阴影遮罩铺满整屏（含刘海/小白条），内容在安全区内布局。
 * 进度条永远固定在底部；上半区垂直水平居中——
 * 若处于 indeterminate（带加载器），则 loading 动图居中、文字在图之下（与普通加载器一致）；
 * 否则文字直接居中。
 * 与 JsUiManager 解耦：onComplete 用于正常完成/点外部关闭，onCancel 用于“取消”标签点击。
 */
@Composable
fun PvzProgressDialog(
    isVisible: Boolean,
    title: String = "",
    message: String = "",
    progress: Float = 0f,
    isIndeterminate: Boolean = false,
    dismissible: Boolean = false,
    showCancel: Boolean = false,
    onComplete: () -> Unit = {},
    onCancel: (suspend () -> Unit)? = null,
) {
    if (!isVisible) return
    val handleDismiss: () -> Unit = {
        if (dismissible) {
            onComplete()
        }
    }
    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = dismissible,
            decorFitsSystemWindows = false
        )
    ) {
        // 外层：全屏半透明遮罩（decorFitsSystemWindows=false 让窗口铺到刘海/小白条之下）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = dismissible, onClick = handleDismiss)
        ) {
            // 内层：在安全区内布局，上半区居中对齐，进度条永远在底部
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // 上半区：占满剩余空间并垂直水平居中
                // 带加载器时动图居中、文字在图之下（与普通加载器一致）；无加载器时文字居中
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isIndeterminate) {
                            AsyncImageFromAssets(
                                "images/loading.gif",
                                modifier = Modifier.size(154.dp),
                                contentDescription = null
                            )
                        }
                        if (title.isNotEmpty()) {
                            PvzRichText(
                                title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        val displayText = "${message.ifEmpty { "处理中..." }} (${(progress * 100).toInt()}%)"
                        PvzRichText(
                            displayText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null)
                        )
                    }
                }
                // 底部：进度条（永远在底部，固定宽度居中）
                PvzProgressBar(
                    progress = progress,
                    label = if (progress >= 1f) "完成" else if (showCancel) "取消" else null,
                    modifier = Modifier.width(300.dp).align(Alignment.BottomCenter).padding(bottom = 40.dp),
                    onLabelClick = {
                        if (progress >= 1f) {
                            onComplete()
                        } else if (showCancel) {
                            // 🔴 用与弹窗生命周期解耦的稳定作用域（JsUiManager.uiScope）：
                            // onCancel 最终走到 JsUiManager.cancelProgress()，它会先 isVisible=false 关弹窗、
                            // 再 runCatching { progressCancelHandler?.invoke() } 跑 JS 注册的 options.onCancel（用户 JS）。
                            // 用户 JS 在 scope.launch（进度弹窗的 rememberCoroutineScope）上挂起时弹窗已离场、scope 被 forget，
                            // resume 即抛 ForgottenCoroutineScopeException。改用 uiScope 彻底规避。
                            JsUiManager.uiScope.launch { onCancel?.invoke() }
                        }
                    }
                )
            }
        }
    }
}