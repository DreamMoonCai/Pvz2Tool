package io.github.dreammooncai.pvz2tool.controller

import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayer
import androidx.core.net.toUri
import eu.iamkonstantin.kotlin.gadulka.isPlaying
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig

object SoundController {
    private val playerMap = mutableMapOf<String, GadulkaPlayer>()

    // 全局音效音量（0.0 ~ 1.0），设置时同步更新所有已存在的播放器
    var globalSfxVolume: Float
        get() = InitializePvz2.initialSfxMusicVolume
        set(value) {
            val v = value.coerceIn(0f, 1f)
            playerMap.values.forEach { it.setVolume(v) }
            InitializePvz2.saveSfxMusicVolume(v)
        }

    fun playSound(url: String?, volume: Float = globalSfxVolume) {
        if (url == null) return

        val player = playerMap.getOrPut(url) {
            GadulkaPlayer().also { player ->
                player.setVolume(volume)
            }
        }

        if (player.isPlaying()) return
        player.play(url)
    }

    fun playSoundFromAssets(fileName: String, volume: Float = globalSfxVolume) {
        // URL 直接使用
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            playSound(fileName, volume)
            return
        }
        // 构建Assets音频的Uri（和之前逻辑一致）
        val assetUri = "file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/sound/$fileName".toUri().toString()
        playSound(assetUri, volume)
    }
}

fun GadulkaPlayer.playSoundFromAssets(fileName: String) = play("file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/sound/$fileName".toUri().toString())