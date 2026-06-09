package io.github.dreammooncai.pvz2tool

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
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
            data = Uri.parse("package:${packageName}")
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
        val intent = Intent(this, runCatching {
            InitializePvz2.config.gameActivity.toKClass().java
        }.onFailure { e -> InitializePvz2.errorScreenState = Pvz2ErrorScreenState(InitializePvz2.config.ui.error.gameActivityInvalid, e) }.getOrNull() ?: return)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (SettingsDialogState.isUseDisconnectTheNetworkAndStart) {
            MainScope().launch {
                delay(12.seconds)
                LocalVpnService.startVpn(InitializePvz2.context)
                runCatching {
                    val activity = ContextUtil.getCurrentActivity()!!
                    RestoreNetworkFloatingController.showFloatingControl(activity)
                }.onFailure {
                    LocalVpnService.stopVpn(InitializePvz2.context)
                }
            }
        }
        InitializePvz2.isBgMusicOn = false
        startActivity(intent)
        finish()
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