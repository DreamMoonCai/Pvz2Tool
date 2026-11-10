package io.github.dreammooncai.pvz2tool.view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource

/**
 * **折叠横条（向左展开）** —— 与 [PvzCollapsiblePanel] 同一套视觉，但展开方式是「抽屉式」：
 *
 * ```
 * 收起：[文字] [▶]                  文字段常驻 + 按钮段；文字传 null 时只剩按钮
 * 展开：[文字] │ [内容 ……] │ [◀]     三段同行，只有中间内容段受开关控制
 * ```
 *
 * 分隔：内容段与按钮段左侧各画一条实色竖线 + 轻微压暗，避免「融成一条」看不出分段。
 *
 * 实现要点（改之前先看，踩过三次坑）：
 * 1. 🔴 根节点必须是 `Box(fillMaxWidth) { Row(width = barWidth) }`：
 *    Row 若直接 `fillMaxWidth` 会被父约束撑到满宽（`width()` 顶不住 `minWidth`），
 *    宽度动画失效，行内布局时还会把相邻格子挤没。
 * 2. 🔴 行内使用（和格子并排）时靠 [onBarWidthChange] 把实际宽度报给调用方，
 *    调用方据此算相邻格子宽度；不要在调用方用 `Modifier.onSizeChanged` 测（测到的是 Box 的满宽）。
 * 3. 🔴 内容段内部是**普通布局**（`fillMaxSize` + 居左），不要再用 `requiredWidth(满宽)+align(CenterEnd)`
 *    做抽屉 —— 那会让内容段左边界恒为负偏移，内容整体左移并被文字段压住（用户说的「漂移」）。
 * 4. 🔴 不用 `BoxWithConstraints` 取父宽（SubcomposeLayout 在弹窗 intrinsic 探测下会崩）。
 * 5. 音效与折叠面板一致（`ui.sounds.collapsiblePanelPress/Release`）。
 *
 * @param title 可选文字（最左段，**常驻显示**）。null / 空串 = 没有这一段。
 * @param expandedState 外部受控展开状态（跨 `key()` 重组存活）；为空时组件内部 remember。
 * @param expandWidth 展开后的宽度。null = 撑满父容器；行内使用时传固定值。
 * @param onBarWidthChange 横条实际宽度变化回调（行内布局用它算相邻格子宽度）。
 * @param trailingContent 中间内容段（横向）—— 受开关控制，收起时不占位。
 */
@Composable
fun PvzSlideBar(
    title: String? = null,
    modifier: Modifier = Modifier,
    isExpandedInit: Boolean = false,
    expandedState: MutableState<Boolean>? = null,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
    barHeight: Dp = 42.dp,
    /** 第三段按钮宽度（默认与 42dp 高度同宽成方正块；内容段较高的场景建议单独调小） */
    buttonWidth: Dp = 42.dp,
    /** 展开后的宽度。null = 撑满父容器；行内使用（和别的格子并排）时传固定值 */
    expandWidth: Dp? = null,
    /** 横条实际宽度变化回调（行内布局要靠它算相邻格子的宽度） */
    onBarWidthChange: ((Dp) -> Unit)? = null,
    /** 收起态宽度。null = 由「文字段自然宽 + 按钮」决定；行内布局时由调用方按文字长度算好传进来，两边保持一致 */
    collapsedWidth: Dp? = null,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val internalExpanded = remember { mutableStateOf(isExpandedInit) }
    var isExpanded by (expandedState ?: internalExpanded)

    val density = LocalDensity.current
    var barWidthPx by remember { mutableIntStateOf(0) }
    val barWidth: Dp = with(density) { barWidthPx.toDp() }

    // 收起态宽度由「文字段自然宽 + 按钮段」决定（wrapContentWidth，文字多长就多宽）；
    // 展开态才用固定宽度。这样不依赖测量文字，避免「文字宽 ↔ 行宽」互相影响的死循环。
    val widthModifier = when {
        !isExpanded && collapsedWidth != null -> Modifier.width(collapsedWidth)
        !isExpanded -> Modifier.wrapContentWidth()
        expandWidth != null -> Modifier.width(expandWidth)
        else -> Modifier.fillMaxWidth()
    }

    if (onBarWidthChange != null) {
        SideEffect { onBarWidthChange.invoke(barWidth) }
    }

    val shape = RoundedCornerShape(8.dp)
    val background = Brush.verticalGradient(
        listOf(theme.headerGradientStart, theme.headerGradientEnd)
    )
    // 分段观感：内容段/按钮段压暗一点，配合左侧竖线才看得出「一格一格」
    val segmentShade = Color.Black.copy(alpha = 0.07f)

    val toggle: () -> Unit = { isExpanded = !isExpanded }
    val headerInteraction = if (InitializePvz2.isConfigReady()) {
        rememberSoundInteractionSource(
            InitializePvz2.config.ui.sounds.collapsiblePanelPress,
            InitializePvz2.config.ui.sounds.collapsiblePanelRelease
        )
    } else {
        remember { MutableInteractionSource() }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    alignment = Alignment.CenterEnd
                )
                .then(widthModifier)
                .onSizeChanged { barWidthPx = it.width }
                .height(barHeight)
                .clip(shape)
                .border(width = 2.dp, color = theme.borderColor, shape = shape)
                .background(background),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ---------- 第一段：文字（常驻，宽度实测） ----------
            if (!title.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 140.dp)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PvzRichText(
                        text = title,
                        defaultStyle = PvzTextWhiteStyle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            // ---------- 第二段：内容（受开关控制，收起时整段不组合） ----------
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clipToBounds()
                        .background(segmentShade)
                        .drawBehind { drawSegmentDivider(theme) }
                ) {
                    // 内容就是普通布局：从段左边界排起（居左）
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 6.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = trailingContent
                    )
                }
            }

            // ---------- 第三段：展开 / 收起按钮（只有这一段能开关） ----------
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .background(segmentShade)
                    .drawBehind { drawSegmentDivider(theme) }
                    // 文字段/内容段点了不收起（内容段里的格子要留给自己点）
                    .clickable(interactionSource = headerInteraction, indication = null) { toggle() },
                contentAlignment = Alignment.Center
            ) {
                SlideBarChevron(
                    pointRight = !isExpanded,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

/** 分段竖线（上下留 14% 边距，避免顶到边框像表格） */
private fun DrawScope.drawSegmentDivider(theme: PvzCollapsiblePanelTheme) {
    drawLine(
        color = theme.borderColor.copy(alpha = 0.85f),
        start = Offset(0f, size.height * 0.14f),
        end = Offset(0f, size.height * 0.86f),
        strokeWidth = (size.height * 0.045f).coerceAtLeast(1.5f)
    )
}

/**
 * 按钮里的三角形方向箭头（自绘，避免 Material 线性图标与 PVZ 厚描边风格冲突）。
 *
 * 收起时指向**右**（展开），展开后指向**左**（收回）—— 与需求草图一致。
 */
@Composable
private fun SlideBarChevron(
    pointRight: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (pointRight) 0f else 180f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "slide_bar_chevron"
    )

    Box(
        modifier = modifier
            .requiredSize(13.dp)
            .graphicsLayer { rotationZ = rotation }
            .drawBehind {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.32f, h * 0.12f)
                    lineTo(w * 0.78f, h * 0.5f)
                    lineTo(w * 0.32f, h * 0.88f)
                    close()
                }
                drawPath(path = path, color = color)
            }
    )
}
