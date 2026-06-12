package io.github.dreammooncai.pvz2tool.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.manager.FilePickerManager
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzExtractorDialog
import io.github.dreammooncai.pvz2tool.ui.dialog.rememberAssetExtractor
import io.github.dreammooncai.pvz2tool.ui.popup.MainPopup
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupHost
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemArrow
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemSwitch
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupRow
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzSettingsDialog(filePickerManager: FilePickerManager? = null) {
    if (!SettingsDialogState.isShow) return
    val config = InitializePvz2.config
    val settingsConfig = config.ui.settings
    val gameDisplayConfig = settingsConfig.gameDisplay
    val context = LocalContext.current

    // 导入SMF所需的AssetExtractorHolder
    val extractorHolder = rememberAssetExtractor(context)
    val uiState by extractorHolder.extractor.uiState
    PvzExtractorDialog(
        uiState = uiState,
        isShowNotUpdate = SettingsDialogState.isUseShowNotUpdate,
        onDismissRequest = { extractorHolder.extractor.dismiss() }
    )

    BasicAlertDialog(
        onDismissRequest = { SettingsDialogState.dismiss() },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        PvzPopupHost(
            startDestination = MainPopup(settingsConfig.title),
            onDismiss = { SettingsDialogState.dismiss() }
        ) { currentRoute, navigator ->
            when (currentRoute) {
                is MainPopup -> {
                    PvzPopupContent(
                        title = currentRoute.title,
                        showBackButton = false,
                        onClose = { SettingsDialogState.dismiss() },
                        bottomContent = {
                            PvzPopupText(
                                "工具箱作者：{{link-red-shadow|https://b23.tv/mVOm6n3:@汽水Boy Dream moon.}}",
                                modifier = Modifier.padding(5.dp),
                                horizontalArrangement = Arrangement.Center
                            )
                        }
                    ) {
                        // 替换硬编码开关文字
                        PvzPopupItemSwitch(
                            title = settingsConfig.solidBackgroundMode,
                            selected = SettingsDialogState.isUseSolidColorBackground,
                            onCheckedChange = { SettingsDialogState.toggleBackgroundMode() }
                        )
                        PvzPopupItemArrow(settingsConfig.changeTheProfileReadLocation) {
                            filePickerManager?.launch { uri, docFile ->
                                if (uri != null && docFile != null) {
                                    val yml = docFile.listFiles().find { it.name?.endsWith(".yml", ignoreCase = true) == true || it.name?.endsWith(".yaml", ignoreCase = true) == true }
                                    if (yml == null) {
                                        Toast.makeText(context, "请在目录准备好.yml/.yaml格式的配置文件", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    if (docFile.name != "pvz2tool") {
                                        Toast.makeText(context, "请选择pvz2tool文件夹,当前${docFile.name}", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    InitializePvz2.mLocalConfigDirUri = docFile.uri
                                    InitializePvz2.initConfig()
                                    InitializePvz2.mPvz2MainScreenReloadKey++
                                    SettingsDialogState.dismiss()
                                } else Toast.makeText(context, config.ui.noValidDirTip, Toast.LENGTH_SHORT).show()
                            }

                        }
                        // 替换硬编码按钮文字
                        PvzPopupItemArrow(settingsConfig.reloadConfig) {
                            InitializePvz2.initConfig()
                            InitializePvz2.mPvz2MainScreenReloadKey++
                            SettingsDialogState.dismiss()
                        }
                        PvzPopupItemArrow(settingsConfig.importSmfFile) {
                            // 选择SMF文件并导入到游戏目录
                            filePickerManager?.launch(isDirectory = false) { uri, docFile ->
                                if (uri != null && docFile != null) {
                                    val targetDir = config.getSmfDirectoryFile()
                                    // 使用AssetExtractorHolder导入文件
                                    val resourcePair = AssetExtractorHolder.resource(
                                        localDocument = docFile,
                                        targetDir = DocumentFile.fromFile(targetDir),
                                        forceOverride = true,
                                        sectionName = settingsConfig.importSmfFile
                                    )
                                    extractorHolder.extract(resourcePair)
                                }
                            }
                        }
                        PvzPopupItemSwitch(
                            title = settingsConfig.playBackgroundMusic,
                            selected = InitializePvz2.isBgMusicOn,
                            onCheckedChange = { InitializePvz2.saveBgMusicOn(!InitializePvz2.isBgMusicOn) }
                        )
                        PvzPopupItemSwitch(
                            title = settingsConfig.resetPacketDeepClearing,
                            selected = SettingsDialogState.isUseResetPacketDeepClearing,
                            onCheckedChange = { SettingsDialogState.toggleResetPacketDeepClearing() }
                        )
                        PvzPopupItemSwitch(
                            title = settingsConfig.showNotUpdate,
                            selected = SettingsDialogState.isUseShowNotUpdate,
                            onCheckedChange = { SettingsDialogState.toggleShowNotUpdate() }
                        )
                        // ── 自定义游戏画面：总开关 ──
                        PvzPopupItemSwitch(
                            title = settingsConfig.customGameDisplay,
                            selected = SettingsDialogState.isUseCustomGameDisplay,
                            onCheckedChange = { SettingsDialogState.toggleCustomGameDisplay() }
                        )
                        // 开关开启时才显示跳转箭头
                        if (SettingsDialogState.isUseCustomGameDisplay) {
                            PvzPopupItemArrow(settingsConfig.customGameDisplayTitle) {
                                navigator.navigate(SubPopup(settingsConfig.customGameDisplayTitle))
                            }
                        }
                    }
                }

                is SubPopup -> {
                    if (currentRoute.title == settingsConfig.customGameDisplayTitle)
                        GameDisplaySettingsPage(
                            title = currentRoute.title,
                            gameDisplayConfig = gameDisplayConfig,
                            onBack = { navigator.pop() }
                        )
                }
            }
        }
    }
}

/**
 * 游戏画面设置子页面
 * 包含：允许随意翻转、显示模式（全屏 / 自定义宽高 / 自定义比例）
 */
@Composable
private fun GameDisplaySettingsPage(
    title: String,
    gameDisplayConfig: io.github.dreammooncai.pvz2tool.Pvz2ToolConfigGameDisplay,
    onBack: () -> Unit
) {
    PvzPopupContent(
        title = title,
        showBackButton = true,
        onBack = onBack
    ) {
        // ── 1. 允许随意翻转界面（支持竖屏）──
        PvzPopupItemSwitch(
            title = gameDisplayConfig.allowRotation,
            selected = SettingsDialogState.isAllowRotation,
            onCheckedChange = { SettingsDialogState.toggleAllowRotation() }
        )

        // ── 2. 显示模式：三选一 ──
        // 全屏
        PvzPopupItemSwitch(
            title = gameDisplayConfig.fullscreen,
            selected = SettingsDialogState.displayMode == "fullscreen",
            onCheckedChange = { SettingsDialogState.updateDisplayMode("fullscreen") }
        )

        // 自定义比例
        PvzPopupItemSwitch(
            title = gameDisplayConfig.customWindowRatio,
            selected = SettingsDialogState.displayMode == "ratio",
            onCheckedChange = { SettingsDialogState.updateDisplayMode("ratio") }
        )

        // 比例值输入（仅 ratio 模式显示）
        if (SettingsDialogState.displayMode == "ratio") {
            PvzPopupRow {
                PvzRichText(
                    "宽高比（支持 1.5 或 3:2）",
                    defaultStyle = PvzTextStyle(Color(0xFF423F00), null),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                var ratioText by remember(SettingsDialogState.windowRatio) {
                    mutableStateOf(formatRatio(SettingsDialogState.windowRatio))
                }
                GameDisplayTextField(
                    value = ratioText,
                    onValueChange = { text ->
                        ratioText = text
                        parseRatioText(text)?.let { v ->
                            if (v > 0f) SettingsDialogState.updateWindowRatio(v)
                        }
                    },
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.width(90.dp)
                )
            }
        }

        // 自定义宽高
        PvzPopupItemSwitch(
            title = gameDisplayConfig.customWindowSize,
            selected = SettingsDialogState.displayMode == "size",
            onCheckedChange = { SettingsDialogState.updateDisplayMode("size") }
        )

        // 宽高值输入（仅 size 模式显示）
        if (SettingsDialogState.displayMode == "size") {
            PvzPopupRow {
                PvzRichText(
                    "宽度（dp）",
                    defaultStyle = PvzTextStyle(Color(0xFF423F00), null),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                var widthText by remember { mutableStateOf(SettingsDialogState.windowWidth.toString()) }
                GameDisplayTextField(
                    value = widthText,
                    onValueChange = { text ->
                        widthText = text
                        text.toIntOrNull()?.let { v ->
                            if (v > 0) SettingsDialogState.updateWindowWidth(v)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(90.dp)
                )
            }
            PvzPopupRow {
                PvzRichText(
                    "高度（dp）",
                    defaultStyle = PvzTextStyle(Color(0xFF423F00), null),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                var heightText by remember { mutableStateOf(SettingsDialogState.windowHeight.toString()) }
                GameDisplayTextField(
                    value = heightText,
                    onValueChange = { text ->
                        heightText = text
                        text.toIntOrNull()?.let { v ->
                            if (v > 0) SettingsDialogState.updateWindowHeight(v)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(90.dp)
                )
            }
        }
    }
}

/**
 * 解析宽高比输入文本，支持两种格式：
 * - 小数：如 "1.5"
 * - 冒号分隔：如 "3:2"
 * 返回 null 表示无法解析。
 */
private fun parseRatioText(text: String): Float? {
    val colonIdx = text.indexOf(':')
    if (colonIdx >= 0) {
        val a = text.substring(0, colonIdx).trim().toFloatOrNull()
        val b = text.substring(colonIdx + 1).trim().toFloatOrNull()
        if (a != null && b != null && b != 0f) return a / b
    }
    return text.toFloatOrNull()
}

/**
 * 将存储的宽高比 float 格式化为用户友好的字符串。
 * 如果能表示为简单的 "a:b" 形式（分母 ≤ 20），则优先使用冒号格式；
 * 否则返回小数形式。
 */
private fun formatRatio(ratio: Float): String {
    if (ratio <= 0f) return ratio.toString()
    for (den in 1..20) {
        val num = ratio * den
        if (kotlin.math.abs(num - kotlin.math.round(num)) < 0.01f && kotlin.math.round(num).toInt() > 0) {
            return "${kotlin.math.round(num).toInt()}:$den"
        }
    }
    return ratio.toString()
}

/** 游戏画面设置页面中复用的数字输入框 */
@Composable
private fun GameDisplayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .border(1.dp, Color(0xFF9E927C), RoundedCornerShape(4.dp))
            .background(Color(0xFFF5EDD0), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        textStyle = TextStyle(
            color = Color(0xFF423F00),
            fontSize = 18.sp
        ),
        cursorBrush = SolidColor(Color(0xFF423F00)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
