package io.github.dreammooncai.pvz2tool.ui.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayer
import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayerState
import eu.iamkonstantin.kotlin.gadulka.rememberGadulkaLiveState
import io.github.dreammooncai.pvz2tool.InitializePvz2
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 背景音乐状态类，管理播放开关、循环、音量渐变等逻辑
 */
@Stable
class BackgroundMusicState(
    val player: GadulkaPlayer,
    // 是否开启背景音乐
    var isEnabled: Boolean = true,
    // 是否开启循环
    var isLoopEnabled: Boolean = true,
    // 目标音量（用于渐变）
    var targetVolume: Float = 1.0f,
    // 当前实际音量（渐变过程中实时更新）
    var currentVolume: Float = 1.0f
) {
    // 暂停音乐（带渐隐）
    suspend fun pauseWithFadeOut(fadeDuration: Long = 1000) {
        if (!isEnabled) return
        isEnabled = false
        val stepCount = (fadeDuration / 50).toInt() // 每50ms调整一次音量
        val volumeStep = currentVolume / stepCount
        repeat(stepCount) {
            currentVolume = maxOf(0f, currentVolume - volumeStep)
            player.setVolume(currentVolume)
            delay(50.milliseconds)
        }
        player.pause() // 音量降为0后暂停
    }

    // 恢复音乐（带渐入）
    suspend fun resumeWithFadeIn(fadeDuration: Long = 1000) {
        if (isEnabled) return
        isEnabled = true
        player.play() // 先播放（音量0）
        val stepCount = (fadeDuration / 50).toInt()
        val volumeStep = targetVolume / stepCount
        currentVolume = 0f
        player.setVolume(0f)
        repeat(stepCount) {
            currentVolume = minOf(targetVolume, currentVolume + volumeStep)
            player.setVolume(currentVolume)
            delay(50.milliseconds)
        }
    }

    // 直接设置音量（无渐变）
    fun setVolume(volume: Float) {
        targetVolume = volume
        currentVolume = volume
        player.setVolume(volume)
        InitializePvz2.saveBgMusicVolume(volume)
    }
}

/**
 * 记住背景音乐状态，绑定Compose生命周期，内置循环+音量渐变逻辑
 * @param audioUrl 背景音乐URL
 * @param initialVolume 初始音量（默认1.0）
 */
@Composable
fun rememberBackgroundMusicState(
    audioUrl: String,
    initialVolume: Float = 1.0f
): BackgroundMusicState {
    // 获取带实时状态的播放器
    val liveState = rememberGadulkaLiveState()
    val player = liveState.player

    // 初始化背景音乐状态
    val bgMusicState = remember {
        BackgroundMusicState(
            player = player,
            targetVolume = initialVolume,
            currentVolume = initialVolume
        )
    }

    // 记录上一次播放状态（用于循环判断）
    var lastPlayerState by remember { mutableStateOf<GadulkaPlayerState?>(null) }

    // 1. 初始播放音乐
    LaunchedEffect(player) {
        if (bgMusicState.isEnabled) {
            player.play(audioUrl)
            player.setVolume(initialVolume)
        }
    }

    // 2. 循环播放逻辑（监听播放状态变化）
    LaunchedEffect(liveState.state, bgMusicState.isLoopEnabled) {
        if (!bgMusicState.isLoopEnabled || !bgMusicState.isEnabled) return@LaunchedEffect

        val currentState = liveState.state
        // 播放完成（从播放/缓冲 → IDLE），重置并重新播放
        if (lastPlayerState in listOf(GadulkaPlayerState.PLAYING, GadulkaPlayerState.BUFFERING)
            && currentState == GadulkaPlayerState.IDLE
        ) {
            player.seekTo(0)
            player.play()
            // 恢复音量（防止渐变过程中播放完成）
            player.setVolume(bgMusicState.currentVolume)
        }
        lastPlayerState = currentState
    }

    // 3. 监听音量渐变目标变化（可选：外部直接修改targetVolume时触发渐变）
    LaunchedEffect(bgMusicState.targetVolume) {
        if (bgMusicState.currentVolume != bgMusicState.targetVolume) {
            val stepCount = 20 // 固定20步渐变
            val volumeStep = (bgMusicState.targetVolume - bgMusicState.currentVolume) / stepCount
            repeat(stepCount) {
                bgMusicState.currentVolume += volumeStep
                player.setVolume(bgMusicState.currentVolume)
                delay(50.milliseconds)
            }
        }
    }

    return bgMusicState
}