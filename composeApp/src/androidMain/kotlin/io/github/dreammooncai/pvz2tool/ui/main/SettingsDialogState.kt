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
    /** 是否开启悬浮窗 */
    var isShowFloatingWindow by mutableStateOf(InitializePvz2.settings["isShowFloatingWindow", InitializePvz2.config.ui.settings.isShowFloatingWindow])
        private set
    var isUseShowNotUpdate by mutableStateOf(InitializePvz2.settings["isUseShowNotUpdate", false])
        private set
    /** 退出游戏二次确认 */
    var isUseExitConfirm by mutableStateOf(InitializePvz2.settings["isUseExitConfirm", InitializePvz2.config.ui.settings.isUseExitConfirm])
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
        isShowFloatingWindow = InitializePvz2.settings["isShowFloatingWindow", InitializePvz2.config.ui.settings.isShowFloatingWindow]
        isUseShowNotUpdate = InitializePvz2.settings["isUseShowNotUpdate", false]
        isUseExitConfirm = InitializePvz2.settings["isUseExitConfirm", InitializePvz2.config.ui.settings.isUseExitConfirm]
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

    fun toggleShowFloatingWindow() {
        isShowFloatingWindow = !isShowFloatingWindow
        InitializePvz2.settings["isShowFloatingWindow"] = isShowFloatingWindow
    }

    fun toggleShowNotUpdate() {
        isUseShowNotUpdate = !isUseShowNotUpdate
        InitializePvz2.settings["isUseShowNotUpdate"] = isUseShowNotUpdate
    }

    fun toggleExitConfirm() {
        isUseExitConfirm = !isUseExitConfirm
        InitializePvz2.settings["isUseExitConfirm"] = isUseExitConfirm
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

    var lastScreenSize = arrayOf(0,0,0,0,0,0)

    fun calcRatioAndPadding(
        screenWidth: Int,
        screenHeight: Int
    ) = (if (!isUseCustomGameDisplay) {
        // 未启用自定义：全屏模式，游戏内容充满 contentParent
        arrayOf(screenWidth, screenHeight, 0, 0, 0, 0)
    } else when (displayMode) {
        "fullscreen" -> {
            arrayOf(screenWidth, screenHeight, 0, 0, 0, 0)
        }
        "ratio" -> {
            val ratio = windowRatio.coerceAtLeast(0.1f)
            calcRatioAndPadding(screenWidth, screenHeight, ratio)
        }
        "size" -> {
            val density = InitializePvz2.context.resources.displayMetrics.density
            // 0 = 未配置，使用屏幕实际尺寸
            val tw = if (windowWidth > 0) (windowWidth * density).toInt().coerceAtLeast(1) else screenWidth
            val th = if (windowHeight > 0) (windowHeight * density).toInt().coerceAtLeast(1) else screenHeight
            val pl = ((screenWidth - tw) / 2).coerceAtLeast(0)
            val pt = ((screenHeight - th) / 2).coerceAtLeast(0)
            arrayOf(tw, th, pl, pt, pl, pt)
        }
        else -> calcRatioAndPadding(screenWidth, screenHeight, 3.0f / 2.0f)
    }).also { result -> lastScreenSize = result }
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