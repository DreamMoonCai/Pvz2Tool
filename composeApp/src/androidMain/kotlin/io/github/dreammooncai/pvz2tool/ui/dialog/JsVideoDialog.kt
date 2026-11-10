package io.github.dreammooncai.pvz2tool.ui.dialog

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.view.CgVideoPlayer
import io.github.dreammooncai.pvz2tool.view.VideoPlaybackControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局视频播放管理器，供 JS 的 `video.*` API 使用。
 *
 * ## 与 CG 完全一致的播放链路
 * 画面承载在 Compose 弹窗 [JsVideoDialog] 上（与 `ui.alert` / `ui.popup` 同一套弹窗栈，**不新起 Activity**），
 * 弹窗内部直接复用游戏内 CG 播放器 [CgVideoPlayer]，因此行为与 CG 一致：
 * - ExoPlayer → MediaPlayer → 海报 三级自动降级（5 秒未就绪即降级）；
 * - 加载中显示 loading 动画、右上角「跳过」按钮；
 * - 前后台切换自动暂停/恢复；
 * - 全屏黑底、画面按比例适配。
 *
 * JS 侧仍能暂停 / 继续 / 跳转 / 调音量：`CgVideoPlayer` 通过 [VideoPlaybackControl]
 * 把当前解码方案的控制句柄交出来（见 [bindControl]）。
 *
 * ## 全局唯一
 * 与 `PvzMusicController` 同思路：任何时刻只允许一个视频实例，新 [play] 会替换上一段。
 */
object JsVideoManager {

    /** 视频弹窗状态。 */
    data class VideoState(
        val isVisible: Boolean = false,
        /** 交给 [CgVideoPlayer] 的原始路径（它内部走资源层：URL > 绝对路径 > 本地工作目录 > 目标 APK > APK assets） */
        val path: String? = null,
        /** 海报兜底图路径（可选） */
        val posterPath: String? = null,
        val loop: Boolean = false,
        val volume: Float = 1f,
        /** 起始位置（毫秒），播放器就绪后跳转 */
        val startMs: Long = 0L,
        /** 每次 play 递增：让弹窗重建播放器（同一地址重复播放也能从头开始） */
        val session: Long = 0L
    )

    private val _state = MutableStateFlow(VideoState())
    val state: StateFlow<VideoState> = _state.asStateFlow()

    /** 当前解码方案的控制句柄（由 [CgVideoPlayer] 就绪时回调注入，主线程）。 */
    @Volatile
    private var control: VideoPlaybackControl? = null

    /** 播放器就绪前先记下的参数，绑定控制句柄时补上。 */
    private var pendingVolume: Float = 1f
    private var pendingLoop: Boolean = false
    private var pendingStartMs: Long = 0L

    /** 由弹窗内的 [CgVideoPlayer] 调用：注入/注销控制句柄。 */
    internal fun bindControl(playbackControl: VideoPlaybackControl?) {
        control = playbackControl
        val c = playbackControl ?: return
        runCatching {
            c.setVolume(pendingVolume)
            c.setLoop(pendingLoop)
            if (pendingStartMs > 0L) c.seekTo(pendingStartMs)
        }
    }

    // ======================== 路径处理 ========================

    /**
     * 规整视频路径，使其能被 [CgVideoPlayer] 的资源层正确解析。
     *
     * `AssetExtractorHolder.open()` 支持 `http(s)://`、`/` 绝对路径、以及相对路径（本地工作目录 → 目标 APK → APK assets），
     * 但不认识 `file://` / `content://`。这里做两件事：
     * 1. `file://` 还原成绝对路径（`/` 开头交给资源层）；
     * 2. 相对路径若在资源层找不到，再试 `video/` 前缀（约定俗成的存放目录）。
     */
    fun resolveVideoPath(fileName: String): String {
        val resolved = JsFileResolver.resolvePlaceholders(fileName).trim()
        if (resolved.isEmpty()) return resolved
        if (resolved.startsWith("file://")) {
            return runCatching { Uri.parse(resolved).path ?: resolved }.getOrDefault(resolved)
        }
        // http(s)/content 与绝对路径：原样交给播放器
        if (resolved.startsWith("http://") || resolved.startsWith("https://") ||
            resolved.startsWith("content://") || resolved.startsWith("/")
        ) return resolved

        // 相对路径：资源层找不到时尝试 video/ 前缀
        val hasAsIs = AssetExtractorHolder.open(resolved) != null
        if (hasAsIs) return resolved
        val withPrefix = "video/$resolved"
        return if (AssetExtractorHolder.open(withPrefix) != null) withPrefix else resolved
    }

    // ======================== 播放控制（主线程） ========================

    /**
     * 开始播放（替换上一段）。
     *
     * @param path 已规整的路径（见 [resolveVideoPath]）
     * @param loop 是否循环
     * @param volume 音量 0~1
     * @param posterPath 海报兜底图
     * @param startMs 起始位置（毫秒）
     */
    fun play(
        path: String,
        loop: Boolean = false,
        volume: Float = 1f,
        posterPath: String? = null,
        startMs: Long = 0L
    ) {
        control = null
        pendingVolume = volume.coerceIn(0f, 1f)
        pendingLoop = loop
        pendingStartMs = startMs.coerceAtLeast(0L)
        _state.value = _state.value.copy(
            isVisible = true,
            path = path,
            posterPath = posterPath,
            loop = loop,
            volume = pendingVolume,
            startMs = pendingStartMs,
            session = _state.value.session + 1
        )
    }

    /** 重播当前视频（循环模式播完后用）。 */
    private fun replay() {
        _state.value = _state.value.copy(session = _state.value.session + 1)
    }

    /** 暂停（画面与弹窗保留）。 */
    fun pause() {
        runCatching { control?.pause() }
    }

    /** 继续播放。 */
    fun resume() {
        runCatching { control?.resume() }
    }

    /** 停止播放并关闭弹窗。 */
    fun stop() {
        control = null
        _state.value = _state.value.copy(isVisible = false, path = null, posterPath = null)
    }

    /** 跳转到指定位置（毫秒）。 */
    fun seekTo(positionMs: Long) {
        runCatching { control?.seekTo(positionMs.coerceAtLeast(0L)) }
    }

    /** 设置音量（0~1）。 */
    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        pendingVolume = v
        _state.value = _state.value.copy(volume = v)
        runCatching { control?.setVolume(v) }
    }

    /** 是否正在播放。 */
    fun isPlaying(): Boolean = runCatching { control?.isPlaying() == true }.getOrDefault(false)

    /** 总时长（毫秒），未知为 0。 */
    fun duration(): Long = runCatching { control?.durationMs() ?: 0L }.getOrDefault(0L)

    /** 当前播放位置（毫秒）。 */
    fun position(): Long = runCatching { control?.positionMs() ?: 0L }.getOrDefault(0L)

    /** 弹窗是否正在显示（播放器可能仍在准备中）。 */
    fun isVisible(): Boolean = _state.value.isVisible

    /** 播放结束（非循环）时的收尾：关闭弹窗。 */
    internal fun onPlaybackEnded() {
        if (_state.value.loop) replay() else stop()
    }
}

/**
 * JS 视频弹窗宿主。挂在 `JsDialogsHost()` 上，与其它 JS 弹窗共用同一套弹窗机制，
 * 内部直接复用 [CgVideoPlayer]，播放行为与游戏内 CG 完全一致。
 *
 * 🔴 这里刻意**不用** M3 的 `BasicAlertDialog`（其它 JS 弹窗用的那个）：
 * 它内部会给内容套上 `DialogMinWidth=280dp` / `DialogMaxWidth=560dp` 的尺寸约束，
 * 即便 `usePlatformDefaultWidth = false`，视频也只会显示在中间一小块里（「视频没有全屏」）。
 * 改用无该约束的 [Dialog]，配合 `usePlatformDefaultWidth = false` +
 * `decorFitsSystemWindows = false`（窗口延伸进状态栏/导航栏后面），才是真正的全屏。
 */
@Composable
fun JsVideoDialog() {
    val state by JsVideoManager.state.collectAsState()
    val path = state.path
    if (!state.isVisible || path == null) return

    Dialog(
        onDismissRequest = { JsVideoManager.stop() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 按 session 重建：同一地址再次播放时也要从头开始
            key(state.session) {
                CgVideoPlayer(
                    videoPath = path,
                    onSkip = { JsVideoManager.stop() },
                    onVideoEnd = { JsVideoManager.onPlaybackEnded() },
                    posterImagePath = state.posterPath,
                    onControlChanged = { JsVideoManager.bindControl(it) }
                )
            }
        }
    }
}
