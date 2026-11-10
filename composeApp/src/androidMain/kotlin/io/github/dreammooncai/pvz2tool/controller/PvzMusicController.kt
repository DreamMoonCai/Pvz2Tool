package io.github.dreammooncai.pvz2tool.controller

import android.os.Looper
import androidx.core.net.toUri
import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayer
import eu.iamkonstantin.kotlin.gadulka.GadulkaPlayerState
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 全局音乐控制器（进程内单例）。
 *
 * ## 设计目标
 * **任何时刻只允许存在一个正在播放的背景音乐实例**，且 API 音乐（`audio.playMusic`）与背景音乐互斥。
 *
 * ## 为什么能根治「双重播放 / 反复播放多个背景音乐」
 * 1. **播放器实例唯一**：背景音乐的 [GadulkaPlayer] 由本控制器持有并复用，只创建一次。
 *    Activity 重建、组合重组、从后台返回都不会再 New 出第二个播放器 —— 这正是此前双重播放的根因
 *    （旧实现把播放器放在 `remember` 里，组合一重建就是新实例，旧实例还在响）。
 * 2. **状态收敛为单一幂等入口**：所有输入变化（用户开关 / 前后台 / 绑定地址 / API 音乐起止 / 音量）
 *    都只调用 [reconcile]，由它把「那唯一一个播放器」驱动到目标状态：
 *    ```
 *      shouldPlayBgm = bgmUrl != null && userEnabled && foreground && !musicActive
 *    ```
 *    [reconcile] 内部会先检查播放器**真实状态**：已在播放就只补音量、绝不重新 `play`，
 *    所以外部无论触发多少次都不可能叠加出第二段声音。
 * 3. **API 音乐让位**：`audio.playMusic` 会置 `musicActive = true` 并触发 [reconcile]，
 *    此时即使用户在设置里手动打开背景音乐、或从后台返回，`shouldPlayBgm` 依然为 false，
 *    背景音乐保持静默；音乐播放结束 / 被 `stopMusic` / 被新的 `playMusic` 取代后统一恢复。
 */
object PvzMusicController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()
    
    // ======================== 播放器实例（全局唯一） ========================
    private var bgmPlayer: GadulkaPlayer? = null
    private var musicPlayer: GadulkaPlayer? = null

    // ======================== 输入状态 ========================
    private var bgmUrl: String? = null
    /** 背景音乐地址是否已灌入当前播放器（用于区分「续播」与「需要重新 prepare」） */
    private var bgmLoadedUrl: String? = null
    private var userEnabled: Boolean = false
    private var foreground: Boolean = true
    /** 是否有 API 音乐正在占用声道（占用期间背景音乐必须让位） */
    private var musicActive: Boolean = false

    // ======================== 音量 / 渐变 ========================
    private var targetVolume: Float = 1f
    private var currentVolume: Float = 1f

    /** 背景音乐是否循环（默认开启） */
    var bgmLoopEnabled: Boolean = true

    /**
     * API 音乐音量覆写：null = 跟随全局 BGM 音量（[setBgmVolume]），非 null = 用户显式指定。
     * [setBgmVolume] 据此决定是否实时更新正在播放的 API 音乐。
     */
    private var musicVolumeOverride: Float? = null

    /** 音量渐变总时长（毫秒）。由 audio.playMusic 的 fade 选项写入。 */
    var fadeDurationMs: Long = 1000L

    // ======================== 任务 ========================
    private var fadeJob: Job? = null
    private var musicJob: Job? = null
    private var loopJob: Job? = null
    /** 每次 reconcile 递增；渐变任务每步都校验，过期即让位给新的 reconcile */
    private var generation: Int = 0

    // ======================== 对外状态查询 ========================

    /** 背景音乐渐变过程中的实时音量 */
    suspend fun getBgmCurrentVolume(): Float = mutex.withLock { currentVolume }

    /** 背景音乐目标音量 */
    suspend fun getBgmTargetVolume(): Float = mutex.withLock { targetVolume }

    /** API 音乐是否正在播放 */
    suspend fun isMusicPlaying(): Boolean = withContext(Dispatchers.Main) {
        val p = mutex.withLock { musicPlayer } ?: return@withContext false
        when (p.currentPlayerState()) {
            GadulkaPlayerState.PLAYING, GadulkaPlayerState.BUFFERING -> true
            else -> false
        }
    }

    /** 背景音乐是否正在播放（真实状态，非期望状态） */
    suspend fun isBgmPlaying(): Boolean = withContext(Dispatchers.Main) {
        val p = mutex.withLock { bgmPlayer } ?: return@withContext false
        when (p.currentPlayerState()) {
            GadulkaPlayerState.PLAYING, GadulkaPlayerState.BUFFERING -> true
            else -> false
        }
    }

    // ======================== 输入（供外部调用） ========================

    /** 绑定背景音乐地址；重复绑定同一地址是空操作（不会重新起播）。传 null 表示当前界面没有背景音乐。 */
    suspend fun bindBgmUrl(url: String?) {
        val changed = mutex.withLock {
            if (bgmUrl == url) false else { bgmUrl = url; true }
        }
        if (!changed) return
        startLoopWatchIfNeeded()
        reconcile()
    }

    /** 用户在设置里开关背景音乐 */
    suspend fun setBgmEnabled(enabled: Boolean) {
        val changed = mutex.withLock {
            if (userEnabled == enabled) false else { userEnabled = enabled; true }
        }
        if (changed) reconcile()
    }

    /** 应用前后台切换（后台不播放） */
    suspend fun setForeground(inForeground: Boolean) {
        val changed = mutex.withLock {
            if (foreground == inForeground) false else { foreground = inForeground; true }
        }
        if (changed) reconcile()
    }

    /**
     * 直接设置背景音乐音量（无渐变）。persist=false 时只初始化内存值，不写设置。
     *
     * 实时联动：正在播放的背景音乐立即生效；处于「跟随模式」（playMusic 未显式指定 volume）的
     * API 音乐也立即更新，显式指定过音量的 API 音乐不受影响。
     */
    suspend fun setBgmVolume(volume: Float, persist: Boolean = true) {
        val v = volume.coerceIn(0f, 1f)
        val followMusic = mutex.withLock {
            targetVolume = v
            currentVolume = v
            musicVolumeOverride == null
        }
        // GadulkaPlayer 必须在主线程访问；穿上 Main scope 派发（引擎脚本可能在工作线程调用）
        scope.launch {
            mutex.withLock { bgmPlayer }?.setVolume(v)
            if (followMusic) mutex.withLock { musicPlayer }?.setVolume(v)
        }
        if (persist) InitializePvz2.saveBgMusicVolume(v)
    }

    // ======================== 状态机核心 ========================

    private suspend fun shouldPlayBgm(): Boolean = mutex.withLock {
        bgmUrl != null && userEnabled && foreground && !musicActive
    }

    /**
     * 唯一驱动入口：把背景音乐播放器收敛到 [shouldPlayBgm] 期望的状态。
     * 幂等 —— 连续调用、并发调用都只会有一个渐变任务存活，且不会重复起播。
     */
    private suspend fun reconcile() {
        val gen = mutex.withLock { ++generation }
        fadeJob?.cancel()
        fadeJob = scope.launch {
            if (gen == mutex.withLock { generation }) {
                if (shouldPlayBgm()) startBgm(gen) else stopBgm(gen)
            }
        }
    }

    /** 取得背景音乐播放器；首次调用创建，之后一律复用。播放器若已被释放（异常路径）则重建。 */
    private suspend fun ensureBgmPlayer(): GadulkaPlayer = mutex.withLock {
        val cur = bgmPlayer
        if (cur?.currentPlayerState() != null) return@withLock cur
        GadulkaPlayer().also { bgmPlayer = it; bgmLoadedUrl = null }
    }

    private suspend fun startBgm(gen: Int) {
        val url = mutex.withLock { bgmUrl } ?: return
        val p = ensureBgmPlayer()
        startLoopWatchIfNeeded()

        val state = p.currentPlayerState()
        val loadedUrl = mutex.withLock { bgmLoadedUrl }
        when {
            // 已在播放/缓冲且加载的就是目标地址：只把音量补到目标值，绝不重新 play（否则会从头重开，听感上就是"又多了一段"）
            (state == GadulkaPlayerState.PLAYING || state == GadulkaPlayerState.BUFFERING) && loadedUrl == url -> {
                rampTo(p, gen, mutex.withLock { targetVolume })
            }
            // 已暂停且是同一首：从暂停处续播（前后台切换走这里，不会重头开始）
            state == GadulkaPlayerState.PAUSED && loadedUrl == url -> {
                p.setVolume(0f)
                mutex.withLock { currentVolume = 0f }
                p.play()
                rampTo(p, gen, mutex.withLock { targetVolume })
            }
            else -> {
                // 首次起播 / 换曲目 / 播放器刚创建：从 0 渐入。play(url) 内部会先 stop 再重设 mediaItem，天然幂等
                p.setVolume(0f)
                mutex.withLock { currentVolume = 0f }
                p.play(url)
                mutex.withLock { bgmLoadedUrl = url }
                rampTo(p, gen, mutex.withLock { targetVolume })
            }
        }
    }

    private suspend fun stopBgm(gen: Int) {
        val p = mutex.withLock { bgmPlayer } ?: return
        val state = p.currentPlayerState() ?: return // 已释放，无需处理
        when (state) {
            // 正在发声：先渐隐再暂停
            GadulkaPlayerState.PLAYING, GadulkaPlayerState.BUFFERING -> {
                rampTo(p, gen, 0f)
                if (gen == mutex.withLock { generation }) p.pause()
            }
            // 尚未发声（已暂停 / 刚 prepare 还在 IDLE 未起播）：直接压住，避免它随后自动起播造成"不该响的时候响了"
            else -> p.pause()
        }
    }

    /** 音量渐变；每一步都校验 generation，被新的 reconcile 接管时立即让位（保证只有一个渐变在跑）。 */
    private suspend fun rampTo(p: GadulkaPlayer, gen: Int, target: Float) {
        val from = mutex.withLock { currentVolume }
        if (from == target) {
            p.setVolume(target)
            mutex.withLock { currentVolume = target }
            return
        }
        val steps = (fadeDurationMs / 50L).toInt().coerceAtLeast(1)
        val step = (target - from) / steps
        for (i in 1..steps) {
            delay(50.milliseconds)
            if (gen != mutex.withLock { generation }) return
            val v = if (i == steps) target else (from + step * i).coerceIn(0f, 1f)
            mutex.withLock { currentVolume = v }
            p.setVolume(v)
        }
    }

    /** 背景音乐循环：播放结束（PLAYING/BUFFERING → IDLE）后回到开头重播 */
    private fun startLoopWatchIfNeeded() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            var playingSeen = false
            while (isActive) {
                delay(500.milliseconds)
                val p = mutex.withLock { bgmPlayer } ?: continue
                val st = p.currentPlayerState() ?: continue
                if (!bgmLoopEnabled || !shouldPlayBgm()) {
                    playingSeen = false
                    continue
                }
                if (st == GadulkaPlayerState.PLAYING || st == GadulkaPlayerState.BUFFERING) {
                    playingSeen = true
                } else if (playingSeen && st == GadulkaPlayerState.IDLE) {
                    playingSeen = false
                    p.seekTo(0)
                    p.play()
                    p.setVolume(mutex.withLock { currentVolume })
                }
            }
        }
    }

    // ======================== API 音乐（audio.playMusic / stopMusic） ========================

    /**
     * 只取消 API 音乐任务，**不释放播放器**（释放必须在主线程，见 [releaseMusicPlayer]）。
     *
     * 🔴 必须在 `musicJob = scope.launch { ... }` **之前**调用：job 体异步执行，
     * 待它运行时 `musicJob` 早已被赋值为该新 job，若在 job 内取消 `musicJob` 就是**取消自己**，
     * 于是 job 内第一个挂起点（delay）立即抛 CancellationException 走进 finally，
     * 把刚起播的播放器 release 掉 —— 表现为「调用后立刻跳 finally、听不到声音」。
     */
    private fun cancelMusicJob() {
        musicJob?.cancel()
        musicJob = null
    }

    /** 释放当前 API 音乐播放器并清空引用。GadulkaPlayer 必须在主线程释放。 */
    private suspend fun releaseMusicPlayer() {
        val p = mutex.withLock {
            musicPlayer.also {
                musicPlayer = null
                musicVolumeOverride = null
            }
        }
        if (p?.currentPlayerState() != null) p.release()
    }

    /**
     * 播放 API 音乐。全局唯一：新调用会先结束并释放上一段，绝不会叠加。
     * 播放期间背景音乐让位（渐隐暂停）；播放结束 / 被 [stopMusic] / 被新的 [playMusic] 取代后自动恢复。
     *
     * @param url 已解析的可播放地址（见 [resolveAudioUri]）
     * @param loop 是否循环
     * @param volume 音量 0~1；**null（缺省）= 跟随 [setBgmVolume] 的当前值，且播放期间改动实时生效**；
     *               显式传值则该段音乐固定为该音量，不随全局音量变化
     * @param fadeMs 背景音乐让位/恢复的渐变时长（毫秒）
     */
    fun playMusic(url: String, loop: Boolean = false, volume: Float? = null, fadeMs: Long = 1000) {
        fadeDurationMs = fadeMs.coerceAtLeast(0)
        // 所有 GadulkaPlayer 操作必须在主线程执行（引擎脚本运行在 Dispatchers.Default 工作线程，
        // 且 keight 的 suspend 桥接不会把 withContext(Dispatchers.Main) 真正切到主线程）。
        // 因此统一经由 scope（Dispatchers.Main）派发，确保 p.play/release 等都在主线程。
        //
        // 🔴 取消旧会话必须在 launch 之前同步完成；job 体内只允许释放播放器（见 cancelMusicJob 注释）。
        cancelMusicJob()
        musicJob = scope.launch {
            // 顺序很重要：先释放上一段的播放器，再标记占用，最后才创建新的播放器
            releaseMusicPlayer()
            mutex.withLock { musicActive = true }
            reconcile()

            // 本会话的 job 身份：finally 里据此判断「自己是否仍是当前会话」，
            // 被新会话取代时由取代方负责释放与恢复，避免重复释放、避免把 musicActive 误置 false
            // （否则会让背景音乐在新会话开始时被错误地短暂恢复再让位，产生闪动）。
            val self = coroutineContext[Job]!!
            val p = GadulkaPlayer()
            mutex.withLock {
                musicPlayer = p
                musicVolumeOverride = volume
            }
            // 未显式指定音量 → 跟随全局 BGM 音量（targetVolume）
            val effectiveVolume = (volume ?: mutex.withLock { targetVolume }).coerceIn(0f, 1f)
            p.setVolume(effectiveVolume)
            p.play(url)
            try {
                delay(400.milliseconds)
                val dur = p.currentDuration()
                val maxMs = (dur?.plus(20_000L)) ?: 600_000L
                do {
                    waitMusicEnd(p, maxMs)
                    if (loop && mutex.withLock { musicJob === self && p === musicPlayer }) {
                        p.seekTo(0)
                        p.play()
                        delay(400.milliseconds)
                    }
                } while (loop && mutex.withLock { p === musicPlayer })
            } finally {
                // 只有「自己仍是当前会话」才收尾：被取代时由取代方负责释放与恢复
                val mine = mutex.withLock {
                    if (musicJob === self && p === musicPlayer) {
                        musicPlayer = null
                        musicActive = false
                        true
                    } else false
                }
                if (mine) {
                    if (p.currentPlayerState() != null) p.release()
                    reconcile()
                }
            }
        }
    }

    /** 停止当前 API 音乐（若其曾让背景音乐让位，则随之恢复）。 */
    fun stopMusic() {
        // 同 playMusic：先同步取消任务（旧 job 的 finally 会因 musicJob 已易主而跳过收尾），
        // 释放与恢复统一交给下面这个 job，避免重复释放 / 双重 reconcile。
        cancelMusicJob()
        scope.launch {
            releaseMusicPlayer()
            val wasActive = mutex.withLock {
                if (musicActive) { musicActive = false; true } else false
            }
            if (wasActive) reconcile()
        }
    }

    /** 轮询直到音乐播放结束（进入 IDLE=STATE_ENDED），或播放器已被释放/被取代，或超时。 */
    private suspend fun waitMusicEnd(p: GadulkaPlayer, maxMs: Long) {
        var playingSeen = false
        val deadline = System.currentTimeMillis() + maxMs.coerceAtLeast(10_000L)
        while (System.currentTimeMillis() < deadline) {
            delay(300.milliseconds)
            if (mutex.withLock { p !== musicPlayer }) return
            val st = p.currentPlayerState() ?: return
            if (st == GadulkaPlayerState.PLAYING || st == GadulkaPlayerState.BUFFERING) playingSeen = true
            else if (playingSeen && st == GadulkaPlayerState.IDLE) return
        }
    }

    // ======================== 路径解析 ========================

    /**
     * 将音频路径解析为可播放 URI：
     * URL / 绝对路径 / 占位符（$WORK_DIR 等）/ 相对 sound/ 优先、assets/pvz2tool/sound/ 兜底。
     */
    fun resolveAudioUri(fileName: String): String {
        val resolved = JsFileResolver.resolvePlaceholders(fileName)
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) return resolved
        if (resolved.startsWith("/")) return android.net.Uri.fromFile(java.io.File(resolved)).toString()
        val soundPath = "sound/$resolved"
        val uri = AssetExtractorHolder.open(soundPath)
        if (uri != null) return uri.toString()
        return "file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/$soundPath".toUri().toString()
    }
}
