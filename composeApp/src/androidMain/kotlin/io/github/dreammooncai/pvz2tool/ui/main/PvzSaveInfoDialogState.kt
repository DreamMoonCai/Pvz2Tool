package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * 存档信息弹窗的统一状态类
 * 封装所有弹窗相关状态，避免参数分散
 */
data class PvzSaveInfoDialogState(
    // 控制弹窗显示/隐藏
    val showDialog: MutableState<Boolean> = mutableStateOf(false),
    var title: MutableState<String>  = mutableStateOf(""),
    // 默认存档名称
    val defaultName: MutableState<String> = mutableStateOf(""),
    // 默认存档描述
    val defaultDesc: MutableState<String> = mutableStateOf(""),
    // 取消按钮回调
    var onDismiss: () -> Unit = {},
    // 确认按钮回调
    var onConfirm: (name: String, desc: String) -> Unit = { _, _ -> }
) {

    val showCount: IntState
        field = mutableIntStateOf(0)

    /**
     * 重置弹窗状态（复用弹窗时使用）
     */
    fun reset() {
        showDialog.value = false
        defaultName.value = ""
        defaultDesc.value = ""
        onDismiss = {}
        onConfirm = { _, _ -> }
    }

    /**
     * 显示弹窗并设置初始值
     */
    fun show(title: String = "",name: String = "", desc: String = "",onDismiss: () -> Unit = {}, onConfirm: (String, String) -> Unit) {
        this.title.value = title
        defaultName.value = name
        defaultDesc.value = desc
        this.onDismiss = onDismiss
        this.onConfirm = onConfirm
        showCount.intValue++
        showDialog.value = true
    }
}