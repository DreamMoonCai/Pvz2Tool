package io.github.dreammooncai.pvz2tool.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.popup.MainPopup
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupHost
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemArrow
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupText
import io.github.dreammooncai.pvz2tool.ui.popup.SubPopup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvzAnnouncementDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false,usePlatformDefaultWidth = false)
    ) {
        PvzPopupHost(
            startDestination = MainPopup("公告"), onDismiss = onDismiss
        ) { currentRoute, navigator ->
            when (currentRoute) { // 1. 主页面
                is MainPopup -> {
                    PvzPopupContent(
                        title = currentRoute.title, showBackButton = false, onClose = onDismiss, bottomContent = {
                            PvzPopupText(
                                "${InitializePvz2.config.ui.versionLabel.stripPvzRichTags()}${InitializePvz2.config.ui.uiVersion.stripPvzRichTags()}",
                                modifier = Modifier.padding(5.dp),
                                horizontalArrangement = Arrangement.Center
                            )
                        }) {
                        InitializePvz2.config.announcement.dropLast(1).forEach { announcement ->
                            PvzPopupItemArrow(announcement.title) { // 点击跳转到子页面
                                navigator.navigate(SubPopup("${announcement.title}公告", announcement.content))
                            }
                        }
                        InitializePvz2.config.announcement.lastOrNull()?.let {announcement ->
                            PvzPopupItemArrow(announcement.title) { // 点击跳转到子页面
                                navigator.navigate(SubPopup("${announcement.title}公告", announcement.content))
                            }
                        }
                    }
                }

                // 2. 子页面
                is SubPopup -> {
                    PvzPopupContent(
                        title = currentRoute.title, showBackButton = true, // 子页面显示返回
                        onBack = { navigator.pop() }) {
                        PvzPopupText(currentRoute.data.toString())
                    }
                }
            }
        }
    }
}