package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.ui.PvzInput

/**
 * 存档信息输入弹窗（适配PVZ视觉风格）
 * @param dialogState 弹窗状态类，封装所有相关状态和回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzSaveInfoDialog(dialogState: PvzSaveInfoDialogState) {
    val config = InitializePvz2.config
    val saveConfig = config.ui.save
    val context = LocalContext.current

    val inputName = remember { mutableStateOf("") }
    val inputDesc = remember { mutableStateOf("") }

    // 输入框统一使用 PvzInput（默认 GREEN 主题）

    if (dialogState.showDialog.value) {
        LaunchedEffect(dialogState.showCount.intValue) {
            inputName.value = dialogState.defaultName.value
            inputDesc.value = dialogState.defaultDesc.value
        }
        BasicAlertDialog(
            onDismissRequest = {
                dialogState.onDismiss.invoke()
                dialogState.reset()
            }, properties = DialogProperties(
                dismissOnClickOutside = true, usePlatformDefaultWidth = false
            )
        ) {
            PvzPopupContent(
                title = dialogState.title.value, showBackButton = false, onClose = { dialogState.reset() }, isInternalCard = false
            ) {
                // 存档名称输入框
                PvzInput(
                    value = inputName.value,
                    onValueChange = { inputName.value = it },
                    label = saveConfig.saveNameLabel,
                    placeholder = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                // 存档描述输入框
                PvzInput(
                    value = inputDesc.value,
                    onValueChange = { inputDesc.value = it },
                    label = saveConfig.saveDescLabel,
                    placeholder = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    multiline = true,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // 操作按钮（保持不变）
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PvzRedButton(
                        text =  saveConfig.cancelButton, modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        dialogState.onDismiss.invoke()
                        dialogState.reset()
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    PvzGreenButton(
                        text = saveConfig.confirmButton, modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (inputName.value.isBlank()) {
                            Toast.makeText(context, saveConfig.saveNameEmptyTip, Toast.LENGTH_SHORT).show()
                            return@PvzGreenButton
                        }
                        dialogState.onConfirm.invoke(inputName.value, inputDesc.value)
                        dialogState.reset()
                    }
                }
            }
        }
    }
}