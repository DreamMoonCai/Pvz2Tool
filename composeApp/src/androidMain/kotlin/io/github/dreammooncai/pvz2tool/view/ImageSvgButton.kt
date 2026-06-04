package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import io.github.dreammooncai.pvz2tool.controller.SoundController

@Composable
fun ImageSvgButton(
    imageVector: ImageVector,
    imageVectorPress: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    pressSound: String? = null,
    releaseSound: String? = null,
    onClick: () -> Unit,
) {
    var isButtonPressed by remember { mutableStateOf(false) }

    Image(
        imageVector = if (isButtonPressed) imageVectorPress else imageVector,
        contentDescription = contentDescription,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // 1. 手指按下：改变状态并播放按下音效
                        isButtonPressed = true
                        pressSound?.let { SoundController.playSoundFromAssets(it) }

                        // 等待手指释放
                        tryAwaitRelease()

                        // 2. 手指释放：改变状态并播放释放音效
                        isButtonPressed = false
                        releaseSound?.let { SoundController.playSoundFromAssets(it) }
                    },
                    onTap = { onClick() }
                )
            }
    )
}