package io.github.dreammooncai.pvz2tool.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzSettingsDialog(filePickerManager: FilePickerManager? = null) {
    if (!SettingsDialogState.isShow) return
    val config = InitializePvz2.config
    val settingsConfig = config.ui.settings
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
        ) { currentRoute, _ ->
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
                    }
                }

                else -> {}
            }
        }
    }
}