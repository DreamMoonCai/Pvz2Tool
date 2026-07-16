package io.github.dreammooncai.pvz2tool.controller

import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayer
import androidx.core.net.toUri
import eu.iamkonstantin.kotlin.gadulka.isPlaying
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder

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

    /**
     * 播放音效，支持多种路径格式：
     * - URL（http/https）→ 直接播放
     * - 绝对路径（/ 开头）→ 本地文件
     * - 占位符（$WORK_DIR/... 等）→ 展开后按类型分发
     * - 相对路径 → 本地工作目录 sound/ 优先，assets/pvz2tool/sound/ 兜底
     */
    fun playSoundFromAssets(fileName: String, volume: Float = globalSfxVolume) {
        // 展开占位符
        val resolved = JsFileResolver.resolvePlaceholders(fileName)

        // URL 直接使用
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            playSound(resolved, volume)
            return
        }

        // 绝对路径：直接使用本地文件系统
        if (resolved.startsWith("/")) {
            playSound(android.net.Uri.fromFile(java.io.File(resolved)).toString(), volume)
            return
        }

        // 相对路径：本地工作目录 sound/ 优先，assets/pvz2tool/sound/ 兜底
        val soundPath = "sound/$resolved"
        val uri = AssetExtractorHolder.open(soundPath)
        if (uri != null) {
            playSound(uri.toString(), volume)
            return
        }

        // 最终兜底：直接拼 assets 路径
        val assetUri = "file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/$soundPath".toUri().toString()
        playSound(assetUri, volume)
    }
}

fun GadulkaPlayer.playSoundFromAssets(fileName: String) {
    val resolved = JsFileResolver.resolvePlaceholders(fileName)
    when {
        resolved.startsWith("http://") || resolved.startsWith("https://") -> play(resolved)
        resolved.startsWith("/") -> play(android.net.Uri.fromFile(java.io.File(resolved)).toString())
        else -> {
            val soundPath = "sound/$resolved"
            val uri = AssetExtractorHolder.open(soundPath)
            if (uri != null) play(uri.toString())
            else play("file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/$soundPath".toUri().toString())
        }
    }
}