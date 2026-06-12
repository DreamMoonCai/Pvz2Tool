package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.dreammooncai.pvz2tool.InitializePvz2

// 单状态类封装所有设置相关属性+行为，避免多回调
object SettingsDialogState {
    var isShow by mutableStateOf(false)
    var isUseSolidColorBackground by mutableStateOf(InitializePvz2.settings["isUseSolidColorBackground", InitializePvz2.config.ui.assets.isUseSolidColorBackground])
        private set
    var isUseResetPacketDeepClearing by mutableStateOf(InitializePvz2.settings["isUseResetPacketDeepClearing", true])
        private set
    var isUseDisconnectTheNetworkAndStart by mutableStateOf(InitializePvz2.settings["isUseDisconnectTheNetworkAndStart", false])
        private set
    var isUseShowNotUpdate by mutableStateOf(InitializePvz2.settings["isUseShowNotUpdate", false])
        private set
    var isUseCustomGameDisplay by mutableStateOf(
        InitializePvz2.settings["isUseCustomGameDisplay", InitializePvz2.config.ui.settings.gameDisplay.isUseCustomGameDisplay]
    )
        private set

    // ── 游戏画面子设置 ──
    /** 允许随意翻转（支持竖屏）*/
    var isAllowRotation by mutableStateOf(
        InitializePvz2.settings["isAllowRotation", InitializePvz2.config.ui.settings.gameDisplay.isAllowRotation]
    )
        private set
    /** 显示模式：fullscreen / ratio / size */
    var displayMode by mutableStateOf(
        InitializePvz2.settings["displayMode", InitializePvz2.config.ui.settings.gameDisplay.displayMode]
    )
        private set
    /** 自定义宽度（dp，仅 displayMode=size 时生效）*/
    var windowWidth by mutableStateOf(
        InitializePvz2.settings["windowWidth", InitializePvz2.config.ui.settings.gameDisplay.windowWidth]
    )
        private set
    /** 自定义高度（dp，仅 displayMode=size 时生效）*/
    var windowHeight by mutableStateOf(
        InitializePvz2.settings["windowHeight", InitializePvz2.config.ui.settings.gameDisplay.windowHeight]
    )
        private set
    /** 自定义宽高比（仅 displayMode=ratio 时生效）*/
    var windowRatio by mutableStateOf(
        InitializePvz2.settings["windowRatio", InitializePvz2.config.ui.settings.gameDisplay.windowRatio]
    )
        private set

    // 初始化：持久化读取
    init {
        isUseSolidColorBackground = InitializePvz2.settings["isUseSolidColorBackground", InitializePvz2.config.ui.assets.isUseSolidColorBackground]
        isUseResetPacketDeepClearing = InitializePvz2.settings["isUseResetPacketDeepClearing", true]
        isUseDisconnectTheNetworkAndStart = InitializePvz2.settings["isUseDisconnectTheNetworkAndStart", false]
        isUseShowNotUpdate = InitializePvz2.settings["isUseShowNotUpdate", false]
        isUseCustomGameDisplay = InitializePvz2.settings["isUseCustomGameDisplay", InitializePvz2.config.ui.settings.gameDisplay.isUseCustomGameDisplay]
        isAllowRotation = InitializePvz2.settings["isAllowRotation", InitializePvz2.config.ui.settings.gameDisplay.isAllowRotation]
        displayMode = InitializePvz2.settings["displayMode", InitializePvz2.config.ui.settings.gameDisplay.displayMode]
        windowWidth = InitializePvz2.settings["windowWidth", InitializePvz2.config.ui.settings.gameDisplay.windowWidth]
        windowHeight = InitializePvz2.settings["windowHeight", InitializePvz2.config.ui.settings.gameDisplay.windowHeight]
        windowRatio = InitializePvz2.settings["windowRatio", InitializePvz2.config.ui.settings.gameDisplay.windowRatio]
    }

    // 关闭弹窗
    fun dismiss() {
        isShow = false
    }

    // 切换背景模式（自动持久化）
    fun toggleBackgroundMode() {
        isUseSolidColorBackground = !isUseSolidColorBackground
        InitializePvz2.settings["isUseSolidColorBackground"] = isUseSolidColorBackground
    }

    fun toggleResetPacketDeepClearing() {
        isUseResetPacketDeepClearing = !isUseResetPacketDeepClearing
        InitializePvz2.settings["isUseResetPacketDeepClearing"] = isUseResetPacketDeepClearing
    }

    fun toggleDisconnectTheNetworkAndStart() {
        isUseDisconnectTheNetworkAndStart = !isUseDisconnectTheNetworkAndStart
        InitializePvz2.settings["isUseDisconnectTheNetworkAndStart"] = isUseDisconnectTheNetworkAndStart
    }

    fun toggleShowNotUpdate() {
        isUseShowNotUpdate = !isUseShowNotUpdate
        InitializePvz2.settings["isUseShowNotUpdate"] = isUseShowNotUpdate
    }

    fun toggleCustomGameDisplay() {
        isUseCustomGameDisplay = !isUseCustomGameDisplay
        InitializePvz2.settings["isUseCustomGameDisplay"] = isUseCustomGameDisplay
    }

    fun toggleAllowRotation() {
        isAllowRotation = !isAllowRotation
        InitializePvz2.settings["isAllowRotation"] = isAllowRotation
    }

    fun updateDisplayMode(mode: String) {
        displayMode = mode
        InitializePvz2.settings["displayMode"] = displayMode
    }

    fun updateWindowWidth(width: Int) {
        windowWidth = width
        InitializePvz2.settings["windowWidth"] = windowWidth
    }

    fun updateWindowHeight(height: Int) {
        windowHeight = height
        InitializePvz2.settings["windowHeight"] = windowHeight
    }

    fun updateWindowRatio(ratio: Float) {
        windowRatio = ratio
        InitializePvz2.settings["windowRatio"] = windowRatio
    }
}