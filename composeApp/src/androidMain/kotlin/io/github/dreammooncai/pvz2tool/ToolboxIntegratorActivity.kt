package io.github.dreammooncai.pvz2tool

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.ui.integration.ToolboxIntegratorScreen

/**
 * 工具箱集成器 —— 独立 Activity（主显示）。
 *
 * 功能：把本应用自身 APK（资源包 id 0x66）作为「新版本」，按 README 适配流程
 * 注入到一个目标游戏 APK（dex 合并 / arsc 搬移 / manifest 改写 / res·lib·assets 合并）。
 *
 * 目标形态仅 APK；最终产出未签名 APK，由 MT 管理器签名安装。
 */
class ToolboxIntegratorActivity : ComponentActivity() {

    private val filePickerManager = FilePickerManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        InitializePvz2.init(this)
        InitializePvz2.filePickerManager = filePickerManager

        // 确保 config 已初始化（init() 中的 initConfig() 可能因异常而未赋值）
        if (!InitializePvz2.isConfigReady()) {
            runCatching {
                InitializePvz2.initConfig()
            }.onFailure { e ->
                Log.e("ToolboxIntegrator", "配置初始化失败", e)
            }
        }
        val configReady = InitializePvz2.isConfigReady()

        setContent {
            Pvz2ToolTheme {
                ToolboxIntegratorScreen(
                    filePickerManager = filePickerManager,
                    configReady = configReady,
                    onBack = { finish() }
                )
            }
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
}
