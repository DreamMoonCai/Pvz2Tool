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
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.controller.SoundController
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder

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

/**
 * 与 [ImageSvgButton] 行为一致，但图标资源来自 [AssetExtractorHolder]（相对工作目录 / 绝对路径 / URL / APK Assets），
 * 而非 ImageVector。用于顶栏由 yml 动态配置的图标：正常态 / 按下态分别由 [normalPath] / [pressPath] 指定，
 * 点击执行 [onClick]（通常是执行 JS）。音效默认回退到设置按钮音效，可用 [pressSound] / [releaseSound] 覆盖。
 */
@Composable
fun ImageAssetButton(
    normalPath: String,
    pressPath: String? = null,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    pressSound: String? = null,
    releaseSound: String? = null,
    onClick: () -> Unit,
) {
    var isButtonPressed by remember { mutableStateOf(false) }

    AsyncImage(
        // 按下态优先用 pressPath，否则复用 normalPath；model 为 Uri，coil 内存缓存保证切换即时无闪烁
        model = AssetExtractorHolder.open(if (isButtonPressed) (pressPath ?: normalPath) else normalPath),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // 1. 手指按下：改变状态并播放按下音效
                        isButtonPressed = true
                        SoundController.playSoundFromAssets(
                            pressSound ?: InitializePvz2.config.ui.sounds.buttonSettingsPress
                        )

                        // 等待手指释放
                        tryAwaitRelease()

                        // 2. 手指释放：改变状态并播放释放音效
                        isButtonPressed = false
                        SoundController.playSoundFromAssets(
                            releaseSound ?: InitializePvz2.config.ui.sounds.buttonSettingsRelease
                        )
                    },
                    onTap = { onClick() }
                )
            }
    )
}