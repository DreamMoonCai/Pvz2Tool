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

    // 初始化：持久化读取
    init {
        isUseSolidColorBackground = InitializePvz2.settings["isUseSolidColorBackground", InitializePvz2.config.ui.assets.isUseSolidColorBackground]
        isUseResetPacketDeepClearing = InitializePvz2.settings["isUseResetPacketDeepClearing", true]
        isUseDisconnectTheNetworkAndStart = InitializePvz2.settings["isUseDisconnectTheNetworkAndStart", false]
        isUseShowNotUpdate = InitializePvz2.settings["isUseShowNotUpdate", false]
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
}