package io.github.dreammooncai.pvz2tool.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.view.PvzCollapsiblePanelTheme

/**
 * M3 内部常量 `OutlinedTextFieldInnerPadding`（private，Material 规范固定值 4dp）。
 *
 * label 上浮后 `Modifier.outlineCutout` 会用 `ClipOp.Difference` 从**描边和容器底色**里
 * 一起挖掉一块矩形，宽度写死为 `labelWidth + 2 * 这个值`，挖掉的地方露出组件外的页面背景。
 */
private val LABEL_CUTOUT_INNER_PADDING = 4.dp

/**
 * 项目统一文本输入框（Material [OutlinedTextField] + 主题配色）。
 *
 * 样式要点（都是踩过坑的）：
 * 1. 🔴 label 上浮后描边缺口两侧会各露出 4dp 页面背景。
 *    **加 padding 或给 label 挂 `background` 都填不满**——两者都会被计入 `labelWidth`，
 *    缺口按 `labelWidth + 8dp` 同步变宽，差值恒定追不上；挂 `background` 还会因为
 *    圆角+尺寸对不上，变成一颗浮在描边上的色块，很难看。
 *    正解：用 [Modifier.drawBehind] 画一块**比自身左右各宽 4dp** 的实色底。
 *    `drawBehind` 的绘制不裁剪到自身边界，且**不参与测量**，
 *    所以「画得更宽」不会让 `labelWidth` 变大，缺口宽度纹丝不动，正好被补严。
 *    同一块绘制里再补一道描边，使输入框外框在标签处「环抱」标签，看起来连续不断开：
 *    - 悬浮但未聚焦 → 描边色同输入框外框（[PvzCollapsiblePanelTheme.headerGradientStart]），用 `clipRect` 只画**上半截**（中间剪断、下半截不要）；
 *    - 聚焦 → 只画上半截高亮描边（[PvzCollapsiblePanelTheme.sliderActiveColor]）并与顶部外框对齐作聚焦卡扣；
 *    - 未悬浮（空值且未聚焦）→ 标签落在框内、M3 不挖缺口，直接跳过绘制。
 * 2. 容器底色仍由 [OutlinedTextFieldDefaults.colors] 提供。
 *    绝不能用 `Modifier.background()` 铺在组件最外层——那是整块矩形，
 *    会连 label 上浮占据的顶部外圈一起刷色，露出一坨凸起的色块。
 * 3. 配色全部来自传入的 [theme]，与项目其它卡片同源，不新造色值。
 *
 * @param placeholder 示例/提示值（如「如 5000」），只在聚焦且内容为空时显示；
 *              同时作为**默认标题**——[label] 缺省时，浮动标签即显示 placeholder，保证标签不为空。
 * @param label 字段名；缺省时回退用 [placeholder]。
 * @param theme 主题，决定输入框底色([PvzCollapsiblePanelTheme.sliderInactiveColor])、
 *              常态描边([PvzCollapsiblePanelTheme.headerGradientStart])、聚焦高亮([PvzCollapsiblePanelTheme.sliderActiveColor])。
 * @param multiline 是否多行（默认 false = 单行）。
 * @param maxLines 多行时最大行数（默认无限制）。
 */
@Composable
fun PvzInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    theme: PvzCollapsiblePanelTheme = PvzCollapsiblePanelTheme.GREEN,
    label: String? = null,
    multiline: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true
) {
    val fieldLabel = label ?: placeholder
    // 主题色：所有主题的 sliderInactiveColor 均为深底，白字恒成立
    val fieldBg = theme.sliderInactiveColor
    val fieldBorder = theme.headerGradientStart
    val fieldAccent = theme.sliderActiveColor

    // 跟踪焦点，用于决定描边画法；标签「悬浮（上浮）」= 聚焦 或 已有内容
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isFloating = isFocused || value.isNotEmpty()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        interactionSource = interactionSource,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        label = if (fieldLabel.isNotEmpty()) {
            {
                Text(
                    text = fieldLabel,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 精确补上描边缺口：左右各外扩 4dp（= M3 挖缺口时多留的量），刚好填平，
                // 既不残留空隙、也不会盖住两端还该保留的描边。
                // 同时按焦点状态补描边，让输入框外框在标签处「环抱」标签，视觉连续不断开。
                modifier = Modifier.drawBehind {
                    // 未悬浮（空值且未聚焦）时标签落在输入框内部，M3 不挖缺口，
                    // 此时既无缺口可补、也无需描边，直接退出避免无谓绘制。
                    if (!isFloating) return@drawBehind

                    val ext = LABEL_CUTOUT_INNER_PADDING.toPx()   // 4.dp
                    val r = 5.dp.toPx()
                    val w = size.width + ext * 2
                    // 向下多画一截：缺口高度按 label 外层 Box（有 16dp 最小高度）算，
                    // 比文字本身高，不补会在下沿留一条细缝。
                    // 向下溢出全落在容器内部（同色）不可见；向上绝不能溢出——那已是组件之外。
                    val h = size.height
                    val strokeW = 1.dp.toPx()   // 与 M3 OutlinedTextField 外框线宽一致

                    // 1) 容器底色补缺口（与输入框同色，盖住 M3 挖掉的页面背景）
                    drawRoundRect(
                        color = fieldBg,
                        topLeft = Offset(-ext, 0f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(r, r)
                    )
                    // 底部两角补回方角：那里在描边线**以下**、属于容器内部，
                    // 圆掉会在缺口下沿露出两个页面背景小三角；补成方角后同色不可见。
                    drawRect(
                        color = fieldBg,
                        topLeft = Offset(-ext, h - r),
                        size = Size(w, r)
                    )

                    // 2) 描边：让输入框外框在标签处连续环抱
                    if (isFocused) {
                        drawRoundRect(
                            color = fieldAccent,
                            topLeft = Offset(-ext, 0F),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = strokeW)
                        )
                    } else {
                        // 常态（悬浮但未聚焦）：描边只画上半截，从中间剪断、下半截不要，与外框顶部对齐
                        clipRect(
                            left = -ext - strokeW,
                            top = -strokeW,
                            right = w + strokeW,
                            bottom = h / 2f
                        ) {
                            drawRoundRect(
                                color = fieldBorder,
                                topLeft = Offset(-ext, 0F),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(r, r),
                                style = Stroke(width = strokeW)
                            )
                        }
                    }
                }
            )
            }
        } else null,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 13.sp,
                maxLines = if (multiline) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        singleLine = !multiline,
        maxLines = if (multiline) maxLines else 1,
        shape = RoundedCornerShape(12.dp),
        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
        colors = OutlinedTextFieldDefaults.colors(
            // 容器：主题深底，聚焦与否都一致，避免闪色
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            disabledContainerColor = fieldBg,
            // 描边：常态偏暗（主题 header），聚焦亮（主题 accent）
            focusedBorderColor = fieldAccent,
            unfocusedBorderColor = fieldBorder,
            // 正文：白字白光标（与项目其它绿底输入框一致）
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            // 标签：常态半透明白，聚焦时主题 accent 高亮
            focusedLabelColor = fieldAccent,
            unfocusedLabelColor = Color(0xB3FFFFFF),
            // 占位示例值：更淡，明显区别于已输入内容
            focusedPlaceholderColor = Color(0x80FFFFFF),
            unfocusedPlaceholderColor = Color(0x80FFFFFF),
            // 禁用态
            disabledTextColor = Color(0x80FFFFFF),
            disabledBorderColor = fieldBorder.copy(alpha = 0.5f),
            disabledLabelColor = Color(0x80FFFFFF),
            disabledPlaceholderColor = Color(0x80FFFFFF)
        )
    )
}
