package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRedButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextOliveStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzConfirmDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    confirmText: String = "确认",
    cancelText: String = "取消"
) {
    if (!isVisible) return

    PvzStyledDialog(
        isVisible = true,
        titleText = title,
        onDismissRequest = onDismiss,
        dismissible = onConfirm == null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomContent = {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onConfirm != null) {
                    PvzRedButton(
                        text = cancelText,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = onDismiss
                    )
                    PvzGreenButton(
                        text = confirmText,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            onConfirm()
                            onDismiss()
                        }
                    )
                } else {
                    PvzGreenButton(
                        text = confirmText,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            onDismiss()
                        }
                    )
                }
            }
        }
    ) {
        PvzRichText(message,fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp), defaultStyle = PvzTextOliveStyle.copy(shadowColor = null))
    }
}