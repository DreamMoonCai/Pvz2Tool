package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle

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

    // PVZ风格颜色常量（保持不变）
    val pvzPrimaryGreen = Color(0xFF8ED229)
    val pvzBorderGreen = Color(0xFF78A52B)
    val pvzDarkGreen = Color(0xFF344702)
    val pvzBgColor = Color(0xFFF3EEB9)
    val pvzTextColor = Color(0xFF423F00)

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
                // 存档名称输入框（PVZ风格保持不变，仅 value 用 inputName.value）
                OutlinedTextField(
                    value = inputName.value,
                    onValueChange = { inputName.value = it },
                    label = {
                        PvzRichText(
                            text = saveConfig.saveNameLabel,
                            defaultStyle = PvzTextStyle(pvzTextColor, null),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(pvzBgColor, RoundedCornerShape(15.dp))
                        .padding(2.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = pvzBorderGreen,
                        focusedBorderColor = pvzPrimaryGreen,
                        focusedTextColor = pvzTextColor,
                        unfocusedTextColor = pvzTextColor,
                        cursorColor = pvzDarkGreen,
                        focusedLabelColor = pvzDarkGreen,
                        unfocusedLabelColor = pvzTextColor,
                        disabledBorderColor = pvzBorderGreen.copy(alpha = 0.5f),
                        disabledTextColor = pvzTextColor.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(15.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = pvzTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                )

                // 存档描述输入框（同理，仅 value 用 inputDesc.value）
                OutlinedTextField(
                    value = inputDesc.value,
                    onValueChange = { inputDesc.value = it },
                    label = {
                        PvzRichText(
                            text = saveConfig.saveDescLabel,
                            defaultStyle = PvzTextStyle(pvzTextColor, null),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(pvzBgColor, RoundedCornerShape(15.dp))
                        .padding(2.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = pvzBorderGreen,
                        focusedBorderColor = pvzPrimaryGreen,
                        focusedTextColor = pvzTextColor,
                        unfocusedTextColor = pvzTextColor,
                        cursorColor = pvzDarkGreen,
                        focusedLabelColor = pvzDarkGreen,
                        unfocusedLabelColor = pvzTextColor
                    ),
                    shape = RoundedCornerShape(15.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = pvzTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
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