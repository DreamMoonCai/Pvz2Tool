package io.github.dreammooncai.pvz2tool.ui.dialog

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzTutorialDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false,usePlatformDefaultWidth = false)
    ) {
        PvzPopupContent(
            title = "📖 使用教程", showBackButton = false, onClose = onDismiss
        ) {
            PvzPopupText(InitializePvz2.config.ui.tutorial)
        }
    }
}
