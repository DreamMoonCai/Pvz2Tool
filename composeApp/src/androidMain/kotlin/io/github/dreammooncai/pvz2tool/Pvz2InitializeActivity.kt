package io.github.dreammooncai.pvz2tool

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.view.KeyEvent
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.controller.GameDisplayFloatingController
import io.github.dreammooncai.pvz2tool.controller.FloatingBallController
import io.github.dreammooncai.pvz2tool.js.JsFileResolver
import io.github.dreammooncai.pvz2tool.js.JsSmfDataManager
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.code.JsDex
import io.github.dreammooncai.pvz2tool.js.code.JsPvz
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.ui.main.*
import io.github.dreammooncai.pvz2tool.ui.music.rememberBackgroundMusicState
import io.github.dreammooncai.pvz2tool.view.CgVideoPlayer
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzExtractorDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.ResourcePair
import io.github.dreammooncai.pvz2tool.ui.dialog.rememberAssetExtractor
import io.github.dreammooncai.yukireflection.factory.toKClass
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.system.exitProcess
import com.highcapable.yukireflection.factory.field
import io.github.dreammooncai.pvz2tool.controller.GeneralFloatingDialogController
import io.github.dreammooncai.pvz2tool.service.RequestPermissionsVpn
import io.github.dreammooncai.pvz2tool.timer.TimerManager
import io.github.dreammooncai.pvz2tool.view.AsyncImageFromAssets
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class Pvz2InitializeActivity : ComponentActivity() {

    companion object {
        /**
         * 启动 Extra：若传入为 true，Activity 在初始化完成后自动触发「进入游戏」逻辑，
         * 用于「重启并进入游戏」场景（JS `app.restartGame()`）。
         */
        const val EXTRA_AUTO_ENTER_GAME = "io.github.dreammooncai.pvz2tool.extra.AUTO_ENTER_GAME"
    }

    // ======================== 修复：将所有初始化提前到类属性/onCreate最开始 ========================

    // 1. 权限申请 Launcher (类属性初始化，安全)
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                proceedWithInitialization()
            } else {
                Toast.makeText(this, "必须授予文件管理权限才能使用应用", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // 2. FilePickerManager 必须在这里初始化！(类属性初始化，确保在 onCreate 之前)
    // 即使现在用不到，也要先注册好 Launcher（其 pick() 异步选择器 Launcher 也在此注册）
    private val filePickerManager = FilePickerManager(this)

    // ======================== 存档相关状态 ========================
    private val importSaveInfoDialogState = PvzSaveInfoDialogState()
    private val importOperationState = PvzSaveOperationState()

    // ======================== 重启后自动进入游戏标记 ========================
    /** 由启动 Intent 的 [EXTRA_AUTO_ENTER_GAME] 决定；为 true 时初始化完成后自动进入游戏 */
    private var autoEnterGame = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 读取「重启后自动进入游戏」标记
        autoEnterGame = intent?.getBooleanExtra(EXTRA_AUTO_ENTER_GAME, false) ?: false

        // ======================== 修复：不要在这里初始化 FilePickerManager ========================

        // 先检查权限
        if (checkAndRequestManageStoragePermission()) {
            proceedWithInitialization()
        }
    }

    // ======================== 核心初始化逻辑 ========================
    private fun proceedWithInitialization() {
        // 初始化配置
        InitializePvz2.init(this)
        // 注入文件选择器管理器，供 JS picker API 使用（其 Launcher 已在 Activity 属性初始化阶段注册）
        InitializePvz2.filePickerManager = filePickerManager

        // 初始化定时器（从 dream.yml schedules 注册 + 重调度已持久化的）
        TimerManager.initFromConfig(
            this, InitializePvz2.config.schedules
        )

        // 处理通知点击：如果从通知点击进入，执行绑定的 JS
        handleNotificationAction(intent)

        // 设置UI
        setContent {
            Pvz2ToolTheme {
                InitializePvz2.errorScreenState?.let { state ->
                    Pvz2ErrorScreen(
                        state = state,
                        onCloseClick = {
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        }
                    )
                    return@Pvz2ToolTheme
                }

                var showCgVideo by remember { mutableStateOf(false) }
                var cgVideoSkipped by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (InitializePvz2.hasVersionChanges()) {
                        showCgVideo = true
                    }
                }

                val onCgSkip: () -> Unit = {
                    showCgVideo = false
                    cgVideoSkipped = true
                }

                if (showCgVideo) {
                    // 重启后自动进入游戏：跳过开场 CG 视频
                    if (autoEnterGame) {
                        LaunchedEffect(Unit) { onCgSkip() }
                    }
                    CgVideoPlayer(
                        videoPath = InitializePvz2.config.ui.assets.resolvedCgVideoPath,
                        onSkip = onCgSkip,
                        onVideoEnd = onCgSkip,
                        posterImagePath = InitializePvz2.config.ui.assets.cgVideoPoster?.takeIf { it.isNotEmpty() }?.let { JsFileResolver.resolvePlaceholders(it) },
                        loadTimeoutMillis = InitializePvz2.config.ui.assets.cgVideoLoadTimeout
                    )
                    return@Pvz2ToolTheme
                }

                // ======================== 精简模式：CG 播放完毕后跳过主界面直接启动 ========================
                if (InitializePvz2.config.simplifiedLaunch) {
                    SimplifiedLaunchScreen(
                        onGotoGame = ::onGotoGame
                    )
                    return@Pvz2ToolTheme
                }

                val audioPath = InitializePvz2.config.ui.assets.resolvedBackgroundMusic
                val audioUrl = if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
                    audioPath
                } else {
                    "file:///android_asset/${Pvz2ToolConfig.PATH_NAME}/$audioPath".toUri().toString()
                }
                val bgMusicState = rememberBackgroundMusicState(audioUrl, initialVolume = InitializePvz2.initialBgMusicVolume)

                // 将 bgMusicState 注入 PvzToolGlobals，供 JS audio API 访问
                LaunchedEffect(bgMusicState) {
                    PvzToolGlobals.bgMusicState = bgMusicState
                }

                LaunchedEffect(InitializePvz2.isBgMusicOn, InitializePvz2.isAppForeground) {
                    if (InitializePvz2.isBgMusicOn && InitializePvz2.isAppForeground) {
                        bgMusicState.resumeWithFadeIn(fadeDuration = 1500)
                    } else {
                        bgMusicState.pauseWithFadeOut(fadeDuration = 1500)
                    }
                }

                PvzSaveInfoDialog(importSaveInfoDialogState)
                PvzSaveOperationResultDialog(
                    operationState = importOperationState,
                    onRetry = null
                )
                key(InitializePvz2.mPvz2MainScreenReloadKey) {
                    val standalone = hasIntegratorActivity()
                    Pvz2MainScreen(
                        onGotoGameClick = ::onGotoGame,
                        onResetDataClick = ::onResetDataClick,
                        onCloseToolbox = {
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        },
                        onStateChanged = {},
                        isStandaloneApp = standalone,
                        onGoToIntegrator = {
                            startActivity(Intent(this@Pvz2InitializeActivity, ToolboxIntegratorActivity::class.java))
                        },
                    )
                }
                // 重启后自动进入游戏：主界面就绪即触发「进入游戏」逻辑
                if (autoEnterGame) {
                    LaunchedEffect(Unit) { onGotoGame() }
                }
            }
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        handleImportIntent(intent)
    }

    // ======================== 权限检查逻辑 ========================
    private fun checkAndRequestManageStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        if (!hasPermissionDeclared(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) return true
        if (Environment.isExternalStorageManager()) return true

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${packageName}".toUri()
        }
        try {
            manageStorageLauncher.launch(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            manageStorageLauncher.launch(fallbackIntent)
        }
        return false
    }

    private fun hasPermissionDeclared(permission: String): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions?.contains(permission) == true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 是否为「工具本体（standalone）」：判断当前运行包内是否声明了集成器 Activity。
     * 不能靠写死的包名（io.github.dreammooncai.pvz2tool）判断——并存设计可能使用不同的
     * 包名/签名做共存安装，但仍是工具本体（内置集成器）；而由集成器合并产出的纯游戏包不会
     * 带集成器 Activity。故以「ToolboxIntegratorActivity 是否可被解析」作为真正的判据。
     */
    private fun hasIntegratorActivity(): Boolean {
        return try {
            val intent = Intent(this, ToolboxIntegratorActivity::class.java)
            packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    // ======================== 其他原有逻辑 ========================
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportIntent(intent)
    }

    private fun showAntiDistribution() {
        // 本地工作目录优先，回退 APK assets；文件缺失则静默跳过
        val input = AssetExtractorHolder.openInputStream("${Pvz2ToolConfig.PATH_NAME}/anti-distribution.txt")
            ?: return
        input.use { stream ->
            stream.reader().readLines().forEach { line ->
                Toast.makeText(this, line, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onGotoGame() {
        showAntiDistribution()
        val gameActivityClass = runCatching {
            InitializePvz2.config.gameActivity.toKClass().java
        }.onFailure { e ->
            InitializePvz2.errorScreenState = Pvz2ErrorScreenState(
                InitializePvz2.config.ui.error.gameActivityInvalid, e
            )
        }.getOrNull() ?: return

        val intent = Intent(this, gameActivityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val application = applicationContext as Application
        var setupDone = false

        // 全局游戏Activity生命周期监听器（零侵入核心）
        val lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                if (activity::class.java != gameActivityClass) return

                activity.window.decorView.post {
                    try {
                        if (InitializePvz2.mGLView == null)
                            InitializePvz2.mGLView = gameActivityClass.field {
                                name = "mGLView"
                                superClass()
                            }.get(activity).cast<GLSurfaceView>() ?: return@post
                        GameDisplayFloatingController.applyGameDisplaySettings(activity)
                        applyImmersiveMode(activity)
                        if (!FloatingBallController.isShow.value && SettingsDialogState.isShowFloatingWindow) {
                            FloatingBallController.showFloatingControl(activity)
                        }
                        registerBackPressHandler(activity)
                        if (!setupDone) {
                            watchGameViewLayoutChange(activity)
                            setupDone = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity::class.java != gameActivityClass) return
                InitializePvz2.mGLView = null
                application.unregisterActivityLifecycleCallbacks(this)
            }

            /**
             * 应用沉浸式模式
             */
            private fun applyImmersiveMode(activity: Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ 推荐方式
                    @Suppress("DEPRECATION")
                    activity.window.setDecorFitsSystemWindows(false)

                    // 完全隐藏状态栏和导航栏（沉浸式沉浸模式）
                    activity.window.insetsController?.apply {
                        hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    // Android 9-10 兼容方式
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                }
            }


            /**
             * 通过 [Window.Callback] 包装拦截返回键，适配所有 Activity 类型（包括原生 android.app.Activity）。
             * 当 [SettingsDialogState.isUseExitConfirm] 开启时弹出退出确认弹窗。
             */
            @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
            private fun registerBackPressHandler(activity: Activity) {
                val window = activity.window ?: return
                val originalCallback = window.callback ?: return
                if (originalCallback is IPvzToolBackPress) return
                window.callback = object : Window.Callback by originalCallback,IPvzToolBackPress {
                    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            if (!SettingsDialogState.isUseExitConfirm) {
                                return originalCallback.dispatchKeyEvent(event)
                            }
                            GeneralFloatingDialogController.showExitConfirm(activity) {
                                activity.finish()
                                Process.killProcess(Process.myPid())
                                exitProcess(0)
                            }
                            return true
                        }
                        return originalCallback.dispatchKeyEvent(event)
                    }
                }
            }
        }

        application.registerActivityLifecycleCallbacks(lifecycleCallback)
        InitializePvz2.isBgMusicOn = false
        startActivity(intent)
        finish()
    }

    /**
     * 监听视图尺寸变化（小窗、分屏、横竖屏）
     * 修复：使用OnLayoutChangeListener替代OnGlobalLayoutListener，监听contentParent而非rootView
     */
    private fun watchGameViewLayoutChange(activity: Activity) {
        try {
            val contentParent = activity.findViewById<FrameLayout>(android.R.id.content)
            val layoutRunnable = Runnable {
                GameDisplayFloatingController.modifyGameLayoutWithPadding(activity)
            }

            // ======================== 新增：配置变化监听 ========================
            val configCallback = object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    // 配置变更后，复用原有防抖逻辑，等待布局测量完成后执行修正
                    contentParent.removeCallbacks(layoutRunnable)
                    contentParent.postDelayed(layoutRunnable, 120)
                }
                @Deprecated("Deprecated in Java")
                override fun onLowMemory() {}
            }
            activity.registerComponentCallbacks(configCallback)

            contentParent.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                // 尺寸未变化直接跳过
                if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
                    return@addOnLayoutChangeListener
                }

                // 取消前一次未执行的任务，只保留最后一次
                contentParent.removeCallbacks(layoutRunnable)
                contentParent.postDelayed(layoutRunnable, 120) // 120ms 防抖，覆盖分屏动画帧间隔
            }

            // 初始修正
            contentParent.post {
                GameDisplayFloatingController.modifyGameLayoutWithPadding(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onResetDataClick() {
//        FloatingBallController.showFloatingControl(this)
//        return
        if (SettingsDialogState.isUseResetPacketDeepClearing) {
            InitializePvz2.config.getSmfDirectoryFile().deleteRecursively()
        }
        JsSmfDataManager.clearCache()
        JsPvz.clearCache()
        JsDex.clearCache() // 同步清理 DEX 加载产生的临时缓存（dex_load/、dex_opt_*、dex_url_*）
        // 保留当前版本，避免重置后版本跳变导致存档迁移到错误目录
        val currentVersion = InitializePvz2.mPvz2ScreenStateFlow.value.selectedVersion
        InitializePvz2.updateScreenState {
            Pvz2ScreenState(selectedVersion = currentVersion)
        }
        InitializePvz2.mSfmVersion = Version.min
        Pvz2MainScreenUiState.resetAll()
        InitializePvz2.mPvz2MainScreenReloadKey++
    }

    private fun handleImportIntent(intent: Intent?) {
        intent ?: return
        val fileUri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        fileUri ?: return
        val isPvz2SaveFile = intent.type == PvzSaveFileManager.SHARE_FILE_MIME_TYPE
                || fileUri.path?.endsWith(PvzSaveFileManager.SHARE_FILE_EXTENSION) == true
        if (!isPvz2SaveFile) return

        val config = InitializePvz2.config.ui.save
        PvzSaveFileManager.importSharedSaveFile(
            context = this,
            fileUri = fileUri,
            defaultImportDesc = config.defaultImportDesc,
            defaultImportNamePrefix = config.defaultImportNamePrefix,
            onEachSaveConfig = { defaultName, defaultDesc, importSuccessTip ->
                suspendCancellableCoroutine { cont ->
                    val dialogTitle = importSuccessTip ?: config.saveInfoTitle
                    importSaveInfoDialogState.show(
                        title = dialogTitle,
                        name = defaultName,
                        desc = defaultDesc,
                        onDismiss = { if (cont.isActive) cont.resume(null) }
                    ) { name, desc ->
                        if (cont.isActive) cont.resume(name to desc)
                    }
                    cont.invokeOnCancellation {  }
                }
            },
            onSingleResult = importOperationState::postResult,
            onWaitForResultDismiss = importOperationState::awaitDismiss,
            onFinalResult = {
                importOperationState.postResult(it)
                intent.data = null
                intent.removeExtra(Intent.EXTRA_STREAM)
            }
        )
    }

    override fun onPause() {
        super.onPause()
        InitializePvz2.isAppForeground = false
        InitializePvz2.isBgMusicOn = false
    }

    override fun onResume() {
        super.onResume()
        InitializePvz2.isAppForeground = true
        InitializePvz2.initBgMusicOn()
    }
}

interface IPvzToolBackPress

// ======================== 精简模式：无本地配置时直接启动游戏 ========================

/**
 * 精简启动界面
 * 当检测到没有本地 yml 配置文件时，跳过完整 UI，直接解压 base 资源并进入游戏。
 *
 * 流程：
 * 1. 显示简洁的加载提示
 * 2. 解压 version/base/smf（通用基础资源）
 * 3. 完成后自动调用 onGotoGame 进入游戏
 */
@Composable
private fun SimplifiedLaunchScreen(
    rootDirectory: File = InitializePvz2.context.getExternalFilesDir(null)!!.parentFile!!,
    onGotoGame: () -> Unit = {}
) {
    Pvz2ToolConfig.rootDirectory = rootDirectory

    val context = LocalContext.current
    val extractorHolder = rememberAssetExtractor(context)

    // 提取进度状态
    val uiState by extractorHolder.extractor.uiState

    // 是否已触发提取
    var extractionStarted by remember { mutableStateOf(false) }
    // 防止多次触发 onGotoGame()
    var gotoGameCalled by remember { mutableStateOf(false) }
    val gotoGameOnce: () -> Unit = {
        if (!gotoGameCalled) {
            gotoGameCalled = true
            // 记录版本，下次启动跳过 CG（仅版本变化时重播）
            InitializePvz2.mSfmVersion = InitializePvz2.versionName
            onGotoGame()
        }
    }

    // 提取完成后的回调
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            delay(500.milliseconds)
            gotoGameOnce()
        }
    }

    // 首次进入时自动触发提取
    LaunchedEffect(Unit) {
        if (!extractionStarted) {
            extractionStarted = true

            val resourcesToExtract = mutableListOf<ResourcePair>()
            val targetDir = InitializePvz2.config.getSmfDirectoryFile()

            val baseAssetPath = InitializePvz2.simpleConfig?.baseAssetPath ?: "version/base/smf"
            resourcesToExtract.add(
                AssetExtractorHolder.resource(
                    internalPath = baseAssetPath,
                    targetDir = targetDir,
                    sectionName = "基础资源"
                )
            )

            if (resourcesToExtract.isNotEmpty()) {
                extractorHolder.setOnDismissListener {
                    if (it.isComplete) gotoGameOnce()
                }
                extractorHolder.extract(*resourcesToExtract.toTypedArray())
            } else {
                gotoGameOnce()
            }
        }
    }

    // VPN 权限请求（与完整模式 Pvz2MainScreen 一致）
    if (SettingsDialogState.isShowFloatingWindow) {
        RequestPermissionsVpn({
            Toast.makeText(
                InitializePvz2.context, "未同意VPN权限，无法显示断网功能.",
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    PvzExtractorDialog(
        uiState = uiState,
        isShowNotUpdate = false,
        onDismissRequest = {
            if (uiState.isComplete) gotoGameOnce()
        }
    )
}

    /** 处理通知点击：从通知进入时执行绑定的 JS 脚本。 */
    private fun handleNotificationAction(intent: Intent?) {
        val action = intent?.getStringExtra("notification_action") ?: return
        if (action.isBlank()) return
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                PvzToolJsEngine.executeScript(action, source = "通知点击")
            } catch (_: Exception) {}
        }
    }

