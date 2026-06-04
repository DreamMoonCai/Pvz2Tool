package io.github.dreammooncai.pvz2tool.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.icon.Gear

/**
 * 功能栏主题。
 * 决定面板头部、分隔线、内容区背景以及文本颜色。
 */
enum class PvzCollapsiblePanelTheme(
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val dividerAlpha: Float = 0.4f,
    val contentBackgroundColor: Color = Color(0xFFF3EEB9),
    /** 主文本颜色（功能名称等） */
    val primaryTextColor: Color = Color(0xFF5D4037),
    /** 次要文本颜色（描述等） */
    val secondaryTextColor: Color = Color(0xFF8D6E63),
    /** Slider 填充色（选中部分） */
    val sliderActiveColor: Color = Color(0xFF8ED229),
    /** Slider 轨道背景色（未选中部分） */
    val sliderInactiveColor: Color = Color(0xFF4A3010)
) {
    /** 默认棕色主题（RADIO/CHECKBOX/DESCRIPTION 栏目） */
    BROWN(
        headerGradientStart = Color(0xFFDF901A),
        headerGradientEnd   = Color(0xFFA1510D),
        borderColor         = Color(0xFFA1510D),
        dividerColor        = Color(0xFFA1510D),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFF3EEB9),
        primaryTextColor    = Color(0xFF5D4037),
        secondaryTextColor  = Color(0xFF8D6E63),
        sliderActiveColor   = Color(0xFF8ED229),
        sliderInactiveColor = Color(0xFF4A3010)
    ),

    /** JS 蓝色主题（BUTTON 栏目） */
    BLUE(
        headerGradientStart = Color(0xFF1976D2),
        headerGradientEnd   = Color(0xFF1565C0),
        borderColor         = Color(0xFF1565C0),
        dividerColor        = Color(0xFF1565C0),
        dividerAlpha        = 0.5f,
        contentBackgroundColor = Color(0xFFF3EEB9),
        primaryTextColor    = Color(0xFF1565C0),
        secondaryTextColor  = Color(0xFF5C8BC0),
        sliderActiveColor   = Color(0xFF64B5F6),
        sliderInactiveColor = Color(0xFF1565C0)
    ),

    /** JS 日志专用深蓝色主题 */
    BLUE_BACKGROUND(
        headerGradientStart = Color(0xFF1E5FA8),
        headerGradientEnd   = Color(0xFF0D3A72),
        borderColor         = Color(0xFF1A3A5C),
        dividerColor        = Color(0xFF1A3A5C),
        dividerAlpha        = 0.6f,
        contentBackgroundColor = Color(0xFF0B1929),
        primaryTextColor    = Color(0xFFCCE4FF),
        secondaryTextColor  = Color(0xFF99C4E8),
        sliderActiveColor   = Color(0xFF69FF47),
        sliderInactiveColor = Color(0xFF1B5E20)
    ),

    /** 绿色自然主题（草坪/植物相关） */
    GREEN(
        headerGradientStart = Color(0xFF558B2F),
        headerGradientEnd   = Color(0xFF33691E),
        borderColor         = Color(0xFF33691E),
        dividerColor        = Color(0xFF33691E),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFE8F5E9),
        primaryTextColor    = Color(0xFF1B5E20),
        secondaryTextColor  = Color(0xFF558B2F),
        sliderActiveColor   = Color(0xFF8ED229),
        sliderInactiveColor = Color(0xFF33691E)
    ),

    /** 深绿黑色背景主题（终端/命令行风格） */
    GREEN_BACKGROUND(
        headerGradientStart = Color(0xFF2E7D32),
        headerGradientEnd   = Color(0xFF1B5E20),
        borderColor         = Color(0xFF1B5E20),
        dividerColor        = Color(0xFF1B5E20),
        dividerAlpha        = 0.6f,
        contentBackgroundColor = Color(0xFF0A1F0A),
        primaryTextColor    = Color(0xFF69FF47),
        secondaryTextColor  = Color(0xFF57CB3A),
        sliderActiveColor   = Color(0xFFAEEA00),
        sliderInactiveColor = Color(0xFF1B5E20)
    ),

    /** 红色主题（警告/错误/危险操作） */
    RED(
        headerGradientStart = Color(0xFFE53935),
        headerGradientEnd   = Color(0xFFC62828),
        borderColor         = Color(0xFFC62828),
        dividerColor        = Color(0xFFC62828),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFFFF3F3),
        primaryTextColor    = Color(0xFFB71C1C),
        secondaryTextColor  = Color(0xFFE57373),
        sliderActiveColor   = Color(0xFFEF5350),
        sliderInactiveColor = Color(0xFF8B0000) // 深暗红色，更像"暗色"轨道
    ),

    /** 紫色主题（高级/魔法/特殊功能） */
    PURPLE(
        headerGradientStart = Color(0xFF7B1FA2),
        headerGradientEnd   = Color(0xFF4A148C),
        borderColor         = Color(0xFF4A148C),
        dividerColor        = Color(0xFF4A148C),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFF3E5F5),
        primaryTextColor    = Color(0xFF4A148C),
        secondaryTextColor  = Color(0xFF9C27B0),
        sliderActiveColor   = Color(0xFFBA68C8),
        sliderInactiveColor = Color(0xFF7B1FA2)
    ),

    /** 深紫背景主题（魔法/暗夜风格） */
    PURPLE_BACKGROUND(
        headerGradientStart = Color(0xFF6A1B9A),
        headerGradientEnd   = Color(0xFF38006B),
        borderColor         = Color(0xFF38006B),
        dividerColor        = Color(0xFF38006B),
        dividerAlpha        = 0.6f,
        contentBackgroundColor = Color(0xFF120520),
        primaryTextColor    = Color(0xFFE1BEE7),
        secondaryTextColor  = Color(0xFFCE93D8),
        sliderActiveColor   = Color(0xFFE040FB),
        sliderInactiveColor = Color(0xFF7B1FA2)
    ),

    /** 橙色主题（进度/成就/能量） */
    ORANGE(
        headerGradientStart = Color(0xFFF57C00),
        headerGradientEnd   = Color(0xFFE65100),
        borderColor         = Color(0xFFE65100),
        dividerColor        = Color(0xFFE65100),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFFFF8E1),
        primaryTextColor    = Color(0xFFBF360C),
        secondaryTextColor  = Color(0xFFFF8F00),
        sliderActiveColor   = Color(0xFFFFB74D),
        sliderInactiveColor = Color(0xFFE65100)
    ),

    /** 青色主题（信息/网络/连接） */
    TEAL(
        headerGradientStart = Color(0xFF00838F),
        headerGradientEnd   = Color(0xFF006064),
        borderColor         = Color(0xFF006064),
        dividerColor        = Color(0xFF006064),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFE0F7FA),
        primaryTextColor    = Color(0xFF004D40),
        secondaryTextColor  = Color(0xFF00897B),
        sliderActiveColor   = Color(0xFF4DB6AC),
        sliderInactiveColor = Color(0xFF00695C)
    ),

    /** 深青背景主题（水下/科技感） */
    TEAL_BACKGROUND(
        headerGradientStart = Color(0xFF00838F),
        headerGradientEnd   = Color(0xFF00363A),
        borderColor         = Color(0xFF004D40),
        dividerColor        = Color(0xFF004D40),
        dividerAlpha        = 0.6f,
        contentBackgroundColor = Color(0xFF001B1C),
        primaryTextColor    = Color(0xFF80DEEA),
        secondaryTextColor  = Color(0xFF4DD0E1),
        sliderActiveColor   = Color(0xFF64FFDA),
        sliderInactiveColor = Color(0xFF00695C)
    ),

    /** 金色主题（VIP/奖励/成就） */
    GOLD(
        headerGradientStart = Color(0xFFFFCA28),
        headerGradientEnd   = Color(0xFFF9A825),
        borderColor         = Color(0xFFF57F17),
        dividerColor        = Color(0xFFF57F17),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFFFFDE7),
        primaryTextColor    = Color(0xFF7B4F00),
        secondaryTextColor  = Color(0xFFB8860B),
        sliderActiveColor   = Color(0xFFFFD54F),
        sliderInactiveColor = Color(0xFFFF8F00)
    ),

    /** 暗灰主题（禁用/系统/底层） */
    GRAY(
        headerGradientStart = Color(0xFF757575),
        headerGradientEnd   = Color(0xFF424242),
        borderColor         = Color(0xFF424242),
        dividerColor        = Color(0xFF424242),
        dividerAlpha        = 0.4f,
        contentBackgroundColor = Color(0xFFF5F5F5),
        primaryTextColor    = Color(0xFF212121),
        secondaryTextColor  = Color(0xFF616161),
        sliderActiveColor   = Color(0xFFBDBDBD),
        sliderInactiveColor = Color(0xFF616161)
    ),

    /** 深灰背景主题（暗色模式/代码编辑器） */
    GRAY_BACKGROUND(
        headerGradientStart = Color(0xFF546E7A),
        headerGradientEnd   = Color(0xFF263238),
        borderColor         = Color(0xFF263238),
        dividerColor        = Color(0xFF263238),
        dividerAlpha        = 0.6f,
        contentBackgroundColor = Color(0xFF1C2833),
        primaryTextColor    = Color(0xFFECEFF1),
        secondaryTextColor  = Color(0xFF90A4AE),
        sliderActiveColor   = Color(0xFFE0E0E0),
        sliderInactiveColor = Color(0xFF424242)
    );
}

/**
 * 折叠面板组件。
 * 提供可折叠的内容区域，带有主题化的头部和动画效果。
 *
 * @param title 面板标题
 * @param modifier 修饰符
 * @param isExpandedInit 初始展开状态
 * @param theme 主题样式，默认使用棕色主题
 * @param headerTrailingContent 标题栏右侧可自定义的内容（如清空按钮）
 * @param content 面板内容
 */
@Composable
fun PvzCollapsiblePanel(
    title: String,
    modifier: Modifier = Modifier,
    isExpandedInit: Boolean = false,
    /** 外部受控展开状态。非空时由外部管理（跨 key() 重组存活），为空时组件内部自行 remember。 */
    expandedState: MutableState<Boolean>? = null,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.BROWN,
    headerTrailingContent: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(theme: PvzCollapsiblePanelTheme) -> Unit
) {
    // 优先使用外部受控状态；否则用内部 remember（保持原有行为）
    val internalExpanded = remember { mutableStateOf(isExpandedInit) }
    val expandedStateResolved = expandedState ?: internalExpanded
    var isExpanded by expandedStateResolved

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 370f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "gear_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(
                width = 2.dp, color = theme.borderColor, shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(theme.headerGradientStart, theme.headerGradientEnd)
                        ), cornerRadius = CornerRadius(
                            if (isExpanded) 8.dp.toPx() else 8.dp.toPx(), if (isExpanded) 0.dp.toPx() else 8.dp.toPx()
                        )
                    )
                }
                .clickable(
                    interactionSource = rememberSoundInteractionSource(
                        InitializePvz2.config.ui.sounds.collapsiblePanelPress,
                        InitializePvz2.config.ui.sounds.collapsiblePanelRelease
                    )
                ) {
                    isExpanded = !isExpanded
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PvzRichText(title,defaultStyle = PvzTextWhiteStyle, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            
            // 右侧自定义内容
            headerTrailingContent()
            
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(20.dp)
            ) {
                Image(Pvz2Icon.Gear,
                    contentDescription = "展开按钮",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.contentBackgroundColor)
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    content(theme)
                }
            }
        }
    }
}
