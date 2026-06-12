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
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.controller.RestoreNetworkFloatingController
import io.github.dreammooncai.pvz2tool.js.JsSmfDataManager
import io.github.dreammooncai.pvz2tool.js.code.JsPvz
import io.github.dreammooncai.pvz2tool.js.code.PvzToolGlobals
import io.github.dreammooncai.pvz2tool.service.LocalVpnService
import io.github.dreammooncai.pvz2tool.ui.main.*
import io.github.dreammooncai.pvz2tool.ui.music.rememberBackgroundMusicState
import io.github.dreammooncai.pvz2tool.view.CgVideoPlayer
import io.github.dreammooncai.util.ContextUtil
import io.github.dreammooncai.yukireflection.factory.toKClass
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import com.highcapable.yukireflection.factory.field
import com.highcapable.yukireflection.factory.method
import io.github.dreammooncai.yukireflection.factory.function
import io.github.dreammooncai.yukireflection.factory.property
import kotlin.time.Duration.Companion.milliseconds

class Pvz2InitializeActivity : ComponentActivity() {

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
    // 即使现在用不到，也要先注册好 Launcher
    private val filePickerManager = FilePickerManager(this)

    // ======================== 存档相关状态 ========================
    private val importSaveInfoDialogState = PvzSaveInfoDialogState()
    private val importOperationState = PvzSaveOperationState()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
                    CgVideoPlayer(
                        videoPath = InitializePvz2.config.ui.assets.resolvedCgVideoPath,
                        onSkip = onCgSkip,
                        onVideoEnd = onCgSkip,
                        posterImagePath = InitializePvz2.config.ui.assets.cgVideoPoster.takeIf { !it.isNullOrEmpty() },
                        loadTimeoutMillis = InitializePvz2.config.ui.assets.cgVideoLoadTimeout
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

                LaunchedEffect(InitializePvz2.isBgMusicOn) {
                    if (InitializePvz2.isBgMusicOn) {
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
                    Pvz2MainScreen(
                        onGotoGameClick = ::onGotoGame,
                        onResetDataClick = ::onResetDataClick,
                        onCloseToolbox = {
                            Process.killProcess(Process.myPid())
                            exitProcess(0)
                        },
                        onStateChanged = {},
                        filePickerManager = filePickerManager // 直接传入已初始化好的对象
                    )
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

    // ======================== 其他原有逻辑 ========================
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportIntent(intent)
    }

    private fun showAntiDistribution() {
        assets.open("${Pvz2ToolConfig.PATH_NAME}/anti-distribution.txt").use { input ->
            input.reader().readLines().forEach { line ->
                Toast.makeText(this,line, Toast.LENGTH_LONG).show()
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

                        // 应用自定义游戏画面设置
                        applyGameDisplaySettings(activity)

                        // 应用沉浸式模式
                        applyImmersiveMode(activity)

                        watchGameViewLayoutChange(activity)

                        if (SettingsDialogState.isUseDisconnectTheNetworkAndStart) {
                            runCatching {
                                LocalVpnService.startVpn(activity)
                                RestoreNetworkFloatingController.showFloatingControl(activity)
                            }.onFailure {
                                LocalVpnService.stopVpn(activity)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        application.unregisterActivityLifecycleCallbacks(this)
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
             * 将 [SettingsDialogState] 中的自定义画面设置应用到游戏 Activity。
             * 此处只处理屏幕方向；窗口大小/比例/内容填充由 modifyGameLayoutWithPadding 负责。
             * 仅在 isUseCustomGameDisplay 开启时生效；
             * 关闭时不干预，由游戏自身的 manifest 设置决定。
             */
            private fun applyGameDisplaySettings(activity: Activity) {
                if (!SettingsDialogState.isUseCustomGameDisplay) return

                activity.requestedOrientation = if (SettingsDialogState.isAllowRotation) {
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
                modifyGameLayoutWithPadding(activity)
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
                modifyGameLayoutWithPadding(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /**
     * 布局修正 + GL渲染刷新（小窗/分屏自动适配）
     * - 未启用自定义画面：全屏（充满 contentParent）
     * - 启用后按 displayMode 选择策略：fullscreen/ratio/size
     */
    private fun modifyGameLayoutWithPadding(activity: Activity) {
        try {
            val contentParent = activity.findViewById<FrameLayout>(android.R.id.content)
            if (contentParent.isEmpty()) return

            val originalGameRoot = contentParent.getChildAt(0) as ViewGroup
            contentParent.setBackgroundResource(R.drawable.game_side_bg)

            val windowWidth = contentParent.width
            val windowHeight = contentParent.height

            // 安全校验：尺寸为0时不执行，避免触发Surface销毁
            if (windowWidth <= 0 || windowHeight <= 0) return

            // ── 根据自定义画面设置选择布局策略 ──
            val result: Array<Int> = if (!SettingsDialogState.isUseCustomGameDisplay) {
                // 未启用自定义：全屏模式，游戏内容充满 contentParent
                arrayOf(windowWidth, windowHeight, 0, 0, 0, 0)
            } else when (SettingsDialogState.displayMode) {
                "fullscreen" -> {
                    arrayOf(windowWidth, windowHeight, 0, 0, 0, 0)
                }
                "ratio" -> {
                    val ratio = SettingsDialogState.windowRatio.coerceAtLeast(0.1f)
                    calcRatioAndPadding(windowWidth, windowHeight, ratio)
                }
                "size" -> {
                    val density = activity.resources.displayMetrics.density
                    val tw = (SettingsDialogState.windowWidth * density).toInt().coerceAtLeast(1)
                    val th = (SettingsDialogState.windowHeight * density).toInt().coerceAtLeast(1)
                    val pl = ((windowWidth - tw) / 2).coerceAtLeast(0)
                    val pt = ((windowHeight - th) / 2).coerceAtLeast(0)
                    arrayOf(tw, th, pl, pt, pl, pt)
                }
                else -> calcRatioAndPadding(windowWidth, windowHeight, 3.0f / 2.0f)
            }
            val targetWidth = result[0]
            val targetHeight = result[1]
            val pl = result[2]
            val pt = result[3]
            val pr = result[4]
            val pb = result[5]

            // 二次校验：目标GL尺寸不能为0
            if (targetWidth <= 0 || targetHeight <= 0) return

            // 只有Padding真正变化时才设置，避免无意义requestLayout
            if (originalGameRoot.paddingLeft != pl
                || originalGameRoot.paddingTop != pt
                || originalGameRoot.paddingRight != pr
                || originalGameRoot.paddingBottom != pb
            ) {
                originalGameRoot.setPadding(pl, pt, pr, pb)
                originalGameRoot.requestLayout()
            }

            InitializePvz2.updateGlViewSize(targetWidth, targetHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 按指定比例计算目标尺寸 + 居中 Padding
     * @return 依次返回: targetWidth, targetHeight, left, top, right, bottom
     */
    private fun calcRatioAndPadding(
        screenWidth: Int,
        screenHeight: Int,
        targetRatio: Float
    ): Array<Int> {
        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (screenRatio > targetRatio) {
            targetHeight = screenHeight
            targetWidth = (targetHeight * targetRatio).toInt()
        } else {
            targetWidth = screenWidth
            targetHeight = (targetWidth / targetRatio).toInt()
        }

        val paddingLeft: Int
        val paddingTop: Int
        val paddingRight: Int
        val paddingBottom: Int

        if (screenRatio > targetRatio) {
            val horizontalPadding = (screenWidth - targetWidth) / 2
            paddingLeft = horizontalPadding
            paddingRight = horizontalPadding
            paddingTop = 0
            paddingBottom = 0
        } else {
            val verticalPadding = (screenHeight - targetHeight) / 2
            paddingTop = verticalPadding
            paddingBottom = verticalPadding
            paddingLeft = 0
            paddingRight = 0
        }

        return arrayOf(targetWidth, targetHeight, paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    fun onResetDataClick() {
        if (SettingsDialogState.isUseResetPacketDeepClearing) {
            InitializePvz2.config.getSmfDirectoryFile().deleteRecursively()
        }
        JsSmfDataManager.clearCache()
        JsPvz.clearCache()
        InitializePvz2.updateScreenState { Pvz2ScreenState() }
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
        InitializePvz2.isBgMusicOn = false
    }

    override fun onResume() {
        super.onResume()
        InitializePvz2.initBgMusicOn()
    }
}