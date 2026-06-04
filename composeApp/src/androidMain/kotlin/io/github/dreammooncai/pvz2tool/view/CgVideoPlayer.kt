package io.github.dreammooncai.pvz2tool.view

import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// 定义解码方案枚举
private enum class DecoderScheme {
    EXO_PLAYER,
    MEDIA_PLAYER,
    POSTER
}

/**
 * CG 视频播放器（修复版：包含完整超时逻辑）
 */
@OptIn(UnstableApi::class)
@Composable
fun CgVideoPlayer(
    videoPath: String,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onVideoEnd: (() -> Unit)? = null,
    posterImagePath: String? = null,
    loadTimeoutMillis: Long = 5000L
) {
    // 核心状态管理
    var currentScheme by remember { mutableStateOf(DecoderScheme.EXO_PLAYER) }
    var videoUri: Uri? by remember { mutableStateOf(null) }
    var isManuallyStopped by remember { mutableStateOf(false) }

    // 【修复】将 isReady 提升回父组件，用于超时判断
    var isCurrentSchemeReady by remember { mutableStateOf(false) }

    // 背景音乐控制
    DisposableEffect(Unit) {
        val wasMusicOn = InitializePvz2.isBgMusicOn
        if (wasMusicOn) InitializePvz2.isBgMusicOn = false
        onDispose {
            if (wasMusicOn) InitializePvz2.initBgMusicOn()
        }
    }

    // 加载资源 URI
    LaunchedEffect(Unit) {
        videoUri = AssetExtractorHolder.open(videoPath)
        if (videoUri == null) {
            currentScheme = DecoderScheme.POSTER
        }
    }

    // 【修复】重置状态：每当切换解码方案时，重置准备状态
    LaunchedEffect(currentScheme) {
        isCurrentSchemeReady = false
    }

    // 【修复】真正的超时与降级逻辑
    LaunchedEffect(currentScheme, videoUri) {
        if (currentScheme == DecoderScheme.POSTER || videoUri == null) return@LaunchedEffect

        // 等待超时
        delay(loadTimeoutMillis.milliseconds)

        // 如果还没准备好，且不是用户手动停止的，则降级
        if (!isCurrentSchemeReady && !isManuallyStopped) {
            currentScheme = when (currentScheme) {
                DecoderScheme.EXO_PLAYER -> DecoderScheme.MEDIA_PLAYER
                DecoderScheme.MEDIA_PLAYER -> DecoderScheme.POSTER
                DecoderScheme.POSTER -> DecoderScheme.POSTER // 保持
            }
        }
    }

    // 海报模式兜底
    if (currentScheme == DecoderScheme.POSTER) {
        PosterFallback(
            posterPath = posterImagePath,
            onSkip = {
                if (!isManuallyStopped) {
                    isManuallyStopped = true
                    onVideoEnd?.invoke() ?: onSkip()
                }
            },
            modifier = modifier
        )
        return
    }

    // 视频播放容器（共有 UI 逻辑）
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 方案 0: ExoPlayer
        if (currentScheme == DecoderScheme.EXO_PLAYER && videoUri != null) {
            ExoPlayerDelegate(
                uri = videoUri!!,
                onReady = { isCurrentSchemeReady = true }, // 通知父组件准备好了
                onError = { if (!isManuallyStopped) currentScheme = DecoderScheme.MEDIA_PLAYER },
                onEnd = { if (!isManuallyStopped) onVideoEnd?.invoke() ?: onSkip() },
                onSkip = { isManuallyStopped = true; onVideoEnd?.invoke() ?: onSkip() },
                modifier = Modifier.matchParentSize()
            )
        }

        // 方案 1: MediaPlayer
        if (currentScheme == DecoderScheme.MEDIA_PLAYER && videoUri != null) {
            MediaPlayerDelegate(
                uri = videoUri!!,
                onReady = { isCurrentSchemeReady = true }, // 通知父组件准备好了
                onError = { if (!isManuallyStopped) currentScheme = DecoderScheme.POSTER },
                onEnd = { if (!isManuallyStopped) onVideoEnd?.invoke() ?: onSkip() },
                onSkip = { isManuallyStopped = true; onVideoEnd?.invoke() ?: onSkip() },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

// ----------------------------------------------------------------------
// 子组件：ExoPlayer 委托
// ----------------------------------------------------------------------
@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerDelegate(
    uri: Uri,
    onReady: () -> Unit, // 新增：准备完成回调
    onError: () -> Unit,
    onEnd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var showSkipButton by remember { mutableStateOf(false) }

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isReady = true
                    onReady() // 触发父组件回调
                }
                if (state == Player.STATE_ENDED) onEnd()
                // 原逻辑：如果进入 IDLE 且还没 Ready，视为错误
                if (state == Player.STATE_IDLE && !isReady) onError()
            }
            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                onError()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        if (showSkipButton) showSkipButton = false else if (isReady) showSkipButton = true
    }) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }

        SkipButtonVisibility(visible = showSkipButton && isReady, onSkip = onSkip)
    }
}

// ----------------------------------------------------------------------
// 子组件：MediaPlayer 委托
// ----------------------------------------------------------------------
@Composable
private fun MediaPlayerDelegate(
    uri: Uri,
    onReady: () -> Unit, // 新增：准备完成回调
    onError: () -> Unit,
    onEnd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var showSkipButton by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    Box(modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        if (showSkipButton) showSkipButton = false else if (isReady) showSkipButton = true
    }) {

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                val mp = MediaPlayer()
                                mediaPlayer = mp

                                // 数据源设置逻辑
                                if (uri.scheme == "file" && uri.path?.startsWith("/android_asset/") == true) {
                                    val assetPath = uri.path!!.removePrefix("/android_asset/")
                                    val afd = context.assets.openFd(assetPath)
                                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                    afd.close()
                                } else {
                                    mp.setDataSource(context, uri)
                                }

                                mp.setDisplay(holder)
                                mp.setOnPreparedListener {
                                    isReady = true
                                    onReady() // 触发父组件回调
                                    it.start()
                                }
                                mp.setOnCompletionListener { onEnd() }
                                mp.setOnErrorListener { _, _, _ -> onError(); true }
                                mp.prepareAsync()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onError()
                            }
                        }
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaPlayer?.release()
                            mediaPlayer = null
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }

        SkipButtonVisibility(visible = showSkipButton && isReady, onSkip = {
            mediaPlayer?.stop()
            onSkip()
        })
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }
}

// ----------------------------------------------------------------------
// 子组件：海报兜底
// ----------------------------------------------------------------------
@Composable
private fun PosterFallback(
    posterPath: String?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(3000.milliseconds)
        onSkip()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip
            )
    ) {
        if (posterPath != null) {
            AsyncImageFromAssets(
                "images/${posterPath}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        SkipButtonVisibility(visible = true, onSkip = onSkip)
    }
}

// ----------------------------------------------------------------------
// 公共 UI 组件：跳过按钮显示层
// ----------------------------------------------------------------------
@Composable
private fun BoxScope.SkipButtonVisibility(visible: Boolean, onSkip: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)),
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        SkipButton(text = "跳过", onClick = onSkip)
    }
}

// ----------------------------------------------------------------------
// 公共 UI 组件：跳过按钮
// ----------------------------------------------------------------------
@OptIn(UnstableApi::class)
@Composable
private fun SkipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val underlineAlpha by animateFloatAsState(
        targetValue = if (isPressed || isHovered) 1f else 0f,
        animationSpec = tween(150),
        label = "underline_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = if (isPressed) 0.9f else 0.6f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (isPressed) 0.7f else 1f
        }) {
            PvzRichText(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                defaultStyle = PvzTextStyle(Color.White),
                modifier = Modifier
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp)
                .background(Color.White.copy(alpha = underlineAlpha))
                .size(width = (text.length * 7).dp, height = 2.dp)
        )
    }
}