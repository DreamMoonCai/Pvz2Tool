package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dreammooncai.pvz2tool.view.PerfectAdaptiveLayout
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle

/**
 * PVZ风格通用弹窗框架（支持滚动 + 底部固定内容，和Popup行为一致）
 * @param isVisible 是否显示弹窗
 * @param titleText 弹窗标题
 * @param onDismissRequest 关闭弹窗的回调
 * @param dismissible 点击外部是否可关闭（默认false）
 * @param content 中间可滚动内容区域
 * @param bottomContent 底部固定内容（按钮区域）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzStyledDialog(
    isVisible: Boolean,
    titleText: String,
    onDismissRequest: () -> Unit,
    dismissible: Boolean = false,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    bottomContent: @Composable (ColumnScope.() -> Unit) = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(dismissOnClickOutside = dismissible)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
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
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    // 标题栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(colors = listOf(Color(0xFF88CD23), Color(0xFF97DC02)))
                            )
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PvzRichText(
                            titleText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            defaultStyle = PvzTextWhiteStyle.copy(shadowColor = null)
                        )
                    }

                    Box(modifier = Modifier.height(20.dp))

                    // 核心：和 PvzPopupContent 完全一致的 探测+滚动显示 逻辑
                    PerfectAdaptiveLayout(
                        height = 0.dp,
                        heightRange = 0.dp .. 250.dp,
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
                        // 显示层：带固定高度 + verticalScroll 真正滚动
                        displayContent = {
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
}