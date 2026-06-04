package io.github.dreammooncai.pvz2tool.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.icon.ArrowLeft
import io.github.dreammooncai.pvz2tool.icon.ArrowLeftPress
import io.github.dreammooncai.pvz2tool.icon.ArrowRight
import io.github.dreammooncai.pvz2tool.icon.ArrowRightPress
import io.github.dreammooncai.pvz2tool.icon.CloseTwo
import io.github.dreammooncai.pvz2tool.icon.CloseTwoPress
import io.github.dreammooncai.pvz2tool.icon.Hook
import io.github.dreammooncai.pvz2tool.icon.HookSelect
import io.github.dreammooncai.pvz2tool.icon.Pvz2Icon
import io.github.dreammooncai.pvz2tool.rememberSoundInteractionSource
import io.github.dreammooncai.pvz2tool.view.ImageSvgButton
import io.github.dreammooncai.pvz2tool.view.PerfectAdaptiveLayout
import io.github.dreammooncai.pvz2tool.view.PvzCard
import io.github.dreammooncai.pvz2tool.view.PvzCardColors
import io.github.dreammooncai.pvz2tool.view.PvzCardGreen
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import io.github.dreammooncai.pvz2tool.view.PvzTextWhiteStyle

@Composable
fun PvzPopupContent(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    bottomContent: @Composable (ColumnScope.() -> Unit) = {},
    isInternalCard: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Box(modifier = modifier.padding(25.dp)) {
        PvzCardGreen(
            title = title,
            fontSize = 26.sp,
            modifier = Modifier.heightIn(max = 400.dp),
            headerLeadingContent = {
                if (showBackButton) ImageSvgButton(
                    Pvz2Icon.ArrowLeft,
                    Pvz2Icon.ArrowLeftPress,
                    "返回",
                    Modifier.padding(horizontal = 10.dp).size(45.dp),
                    InitializePvz2.config.ui.sounds.buttonXClosePress,
                    InitializePvz2.config.ui.sounds.buttonXCloseRelease,
                    onClick = onBack
                )
            }) {

            PerfectAdaptiveLayout(
                modifier = Modifier.padding(10.dp, 0.dp),
                // 探测版 Content：不带 fillMaxHeight，用来查真实高度
                probeContent = {
                    val scrollState = rememberScrollState()
                    val probeModifier = Modifier
                        .padding(0.dp, 15.dp)
                        .verticalScroll(scrollState)

                    if (isInternalCard) {
                        PvzCard(
                            null,
                            PvzCardColors(
                                outerBackgroundTop = Color(0xFFC9B994),
                                outerBackgroundBottom = Color(0xFFCABB97),
                                border1Color = Color(0xFF7AAD08),
                                border2Color = Color(0xFFFFFFFF),
                                border3Color = Color(0xFF9E927C),
                                headerBackgroundTop = Color.Transparent,
                                headerBackgroundBottom = Color.Transparent,
                                headerTextStyle = PvzTextWhiteStyle
                            ),
                            Modifier,
                            probeModifier,
                            content = content
                        )
                    } else {
                        Column(probeModifier) {
                            content()
                        }
                    }
                },
                // 显示版 Content：带 fillMaxHeight，用来实际展示
                displayContent = {
                    val scrollState = rememberScrollState()
                    val displayModifier = Modifier
                        .padding(0.dp, 15.dp)
                        .fillMaxHeight() // 【关键】强制填充满分配的高度
                        .verticalScroll(scrollState)

                    if (isInternalCard) {
                        PvzCard(
                            null,
                            PvzCardColors(
                                outerBackgroundTop = Color(0xFFC9B994),
                                outerBackgroundBottom = Color(0xFFCABB97),
                                border1Color = Color(0xFF7AAD08),
                                border2Color = Color(0xFFFFFFFF),
                                border3Color = Color(0xFF9E927C),
                                headerBackgroundTop = Color.Transparent,
                                headerBackgroundBottom = Color.Transparent,
                                headerTextStyle = PvzTextWhiteStyle
                            ),
                            Modifier,
                            displayModifier,
                            content = content
                        )
                    } else {
                        Column(displayModifier) {
                            content()
                        }
                    }
                },
                bottomContent = bottomContent
            )
        }

        // 关闭按钮
        if (!showBackButton) {
            ImageSvgButton(
                Pvz2Icon.CloseTwo, Pvz2Icon.CloseTwoPress, "关闭",
                Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(45.dp),
                InitializePvz2.config.ui.sounds.buttonXClosePress,
                InitializePvz2.config.ui.sounds.buttonXCloseRelease,
                onClick = onClose
            )
        }
    }
}

@Composable
fun PvzPopupText(
    text: String,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 5.dp).then(modifier), horizontalArrangement, verticalAlignment) {
        PvzRichText(text, defaultStyle = PvzTextStyle(Color(0xFF423F00), null), fontSize = 20.sp)
    }
}

@Composable
fun PvzPopupRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable (RowScope.() -> Unit)
) {
    Row(Modifier.fillMaxWidth().padding(20.dp).then(modifier), horizontalArrangement, verticalAlignment,content)
}

@Composable
fun PvzPopupItem(title: String,isSpacer: Boolean = true, content: @Composable (RowScope.() -> Unit)) {
    PvzPopupRow {
        PvzRichText(
            title,
            defaultStyle = PvzTextStyle(Color(0xFF423F00), null),
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )

        content()
    }
    if (isSpacer) Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFAA9A5F)))
}


@Composable
fun PvzPopupItemArrow(title: String,isSpacer: Boolean = true,  onClick: () -> Unit) {
    PvzPopupItem(title,isSpacer) {
        ImageSvgButton(
            Pvz2Icon.ArrowRight,
            Pvz2Icon.ArrowRightPress,
            "前往", Modifier.size(25.dp),
            InitializePvz2.config.ui.sounds.switchClickPress,
            InitializePvz2.config.ui.sounds.switchClickRelease,
            onClick = onClick
        )
    }
}

@Composable
fun PvzPopupItemSwitch(
    title: String,
    selected: Boolean,
    isSpacer: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    PvzPopupItem(title,isSpacer) {
        Image(
            imageVector = if (selected) Pvz2Icon.HookSelect else Pvz2Icon.Hook,
            contentDescription = if (selected) "已选中" else "未选中",
            modifier = Modifier
                .size(25.dp)
                .clickable(
                    interactionSource = rememberSoundInteractionSource(
                        InitializePvz2.config.ui.sounds.switchClickPress,
                        InitializePvz2.config.ui.sounds.switchClickRelease
                    )
                ) {
                    SoundController.playSoundFromAssets(InitializePvz2.config.ui.sounds.switchClick)
                    onCheckedChange(!selected)
                }
        )
    }
}

@Preview
@Composable
fun PvzPopupContentPreview() {
    PvzPopupContent("公告", bottomContent = {
        PvzPopupText("版本号:6666",horizontalArrangement = Arrangement.Center)
    }) {
        PvzPopupItemArrow("音乐") {

        }
        PvzPopupItemSwitch("开关",true) {

        }
    }
}