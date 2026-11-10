package io.github.dreammooncai.pvz2tool.controller

import android.os.Looper
import androidx.core.net.toUri
import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayer
import eu.iamkonstantin.kotlin.gadulka.isPlaying
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object SoundController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val playerMap = mutableMapOf<String, GadulkaPlayer>()

    /**
     * 引擎脚本运行在 Dispatchers.Default 工作线程上，keight 的 suspend 桥接不会真正切到主线程，
     * 因此任何触碰 GadulkaPlayer 的操作都必须显式派发到 Dispatchers.Main。
     */
    private inline fun <T> runOnMain(crossinline block: () -> T): T =
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else runBlocking(Dispatchers.Main) { block() }

    // 全局音效音量（0.0 ~ 1.0），设置时同步更新所有已存在的播放器
    var globalSfxVolume: Float
        get() = InitializePvz2.initialSfxMusicVolume
        set(value) {
            val v = value.coerceIn(0f, 1f)
            // GadulkaPlayer 必须在主线程访问
            scope.launch { playerMap.values.forEach { it.setVolume(v) } }
            InitializePvz2.saveSfxMusicVolume(v)
        }

    /**
     * 播放音效。
     *
     * @param url 播放地址
     * @param volume 音量 0~1；**null（缺省）= 播放那一刻读取全局音效音量 [globalSfxVolume]（实时跟随）**；
     *               显式传值则固定该音量
     */
    fun playSound(url: String?, volume: Float? = null) {
        if (url == null) return
        // 播放器创建与播放必须在主线程；穿上 Main scope 派发（引擎脚本可能在工作线程调用）
        scope.launch {
            val player = playerMap.getOrPut(url) { GadulkaPlayer() }
            // 关键：在「播放时」才解析音量——volume==null 时读取当时的全局音效音量，
            // 而不是用调用入口的默认参数快照（否则调用与真正 play 之间改动的 setSfxVolume 会丢失）。
            // 复用池里已存在的播放器同样拿到最新音量，不沿用首次播放时的旧值。
            val effective = (volume ?: globalSfxVolume).coerceIn(0f, 1f)
            player.setVolume(effective)
            if (player.isPlaying()) return@launch
            player.play(url)
        }
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