package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.dialog.PvzStyledDialog
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzOrangeButton // 新增：重试按钮用橙色区分
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle

/**
 * 存档操作结果弹窗（基于通用PVZ风格弹窗框架，优化图标和失败重试交互）
 * @param operationState 操作状态
 * @param onDismiss 关闭弹窗回调
 * @param onRetry 重试操作回调（仅失败时生效）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzSaveOperationResultDialog(
    operationState: PvzSaveOperationState,
    onDismiss: () -> Unit = { operationState.reset() },
    onRetry: ((result: PvzSaveOperationResult) -> Unit)? = null // 新增：重试回调
) {
    val result = operationState.result ?: return
    val config = InitializePvz2.config
    val saveConfig = config.ui.save
    val operationConfig = saveConfig.operation

    // 复用通用PVZ风格弹窗框架
    PvzStyledDialog(
        isVisible = operationState.showResultDialog,
        titleText = when (result.type) {
            PvzSaveOperationType.BACKUP -> operationConfig.backup
            PvzSaveOperationType.EXPORT -> operationConfig.export
            PvzSaveOperationType.IMPORT -> operationConfig.import
            PvzSaveOperationType.DELETE -> operationConfig.delete
            PvzSaveOperationType.DELETE_GAME_SAVE -> operationConfig.deleteGameSave
            PvzSaveOperationType.COVER -> operationConfig.cover
            PvzSaveOperationType.SAVE_META -> operationConfig.saveMeta
        },
        onDismissRequest = onDismiss,
        dismissible = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        bottomContent = {
            Spacer(modifier = Modifier.height(20.dp))

            // 2. 按钮区域：失败时显示重试+确认，成功时仅显示确认
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp) // 按钮之间的间距
            ) {
                // 重试按钮：仅失败且有重试回调时显示
                if (!result.isSuccess && onRetry != null) {
                    PvzOrangeButton(
                        text = saveConfig.retryButtonText,
                        modifier = Modifier
                            .weight(1f) // 均分宽度
                            .height(48.dp),
                        onClick = {
                            onDismiss()
                            onRetry(result)
                        }
                    )
                }

                // 确认按钮：始终显示
                PvzGreenButton(
                    text = "确认",
                    modifier = Modifier
                        .weight(1f) // 均分宽度
                        .height(48.dp),
                    onClick = onDismiss
                )
            }
        }
    ) {
        // 1. 替换为语义更贴合的系统图标
        Icon(
            imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (result.isSuccess) "操作成功" else "操作失败",
            tint = if (result.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 结果消息
        PvzRichText(
            text = result.message,
            fontSize = 14.sp,
            defaultStyle = PvzTextOliveStyle.copy(shadowColor = null),
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}