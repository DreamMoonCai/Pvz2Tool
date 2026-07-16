package io.github.dreammooncai.pvz2tool.controller

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isEmpty
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.main.GameDisplaySettingsContent
import io.github.dreammooncai.pvz2tool.ui.main.SettingsDialogState
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupContent
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupItemSwitch
import io.github.dreammooncai.pvz2tool.ui.popup.PvzPopupRow
import io.github.dreammooncai.pvz2tool.view.PvzGreenButton
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import kotlin.math.abs
import kotlin.math.round

object GameDisplayFloatingController {

    /**
     * 将 [SettingsDialogState] 中的自定义画面设置应用到游戏 Activity。
     * 此处只处理屏幕方向；窗口大小/比例/内容填充由 modifyGameLayoutWithPadding 负责。
     * 仅在 isUseCustomGameDisplay 开启时生效；
     * 关闭时不干预，由游戏自身的 manifest 设置决定。
     */
    fun applyGameDisplaySettings(activity: Activity) {
        if (!SettingsDialogState.isUseCustomGameDisplay) return

        activity.requestedOrientation = if (SettingsDialogState.isAllowRotation) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    /**
     * 布局修正 + GL渲染刷新（小窗/分屏自动适配）
     * - 未启用自定义画面：全屏（充满 contentParent）
     * - 启用后按 displayMode 选择策略：fullscreen/ratio/size
     */
    fun modifyGameLayoutWithPadding(activity: Activity) {
        try {
            val contentParent = activity.findViewById<FrameLayout>(android.R.id.content)
            if (contentParent.isEmpty()) return

            val originalGameRoot = contentParent.getChildAt(0) as ViewGroup
            val sideBgPath = InitializePvz2.config.ui.assets.sideBgImage
            val resolvedSideBgPath = if (sideBgPath.startsWith("/")) sideBgPath else "images/$sideBgPath"
            AssetExtractorHolder.openInputStream(resolvedSideBgPath)?.use { stream ->
                contentParent.background = android.graphics.drawable.BitmapDrawable(
                    activity.resources, android.graphics.BitmapFactory.decodeStream(stream)
                )
            }

            val windowWidth = contentParent.width
            val windowHeight = contentParent.height

            // 安全校验：尺寸为0时不执行，避免触发Surface销毁
            if (windowWidth <= 0 || windowHeight <= 0) return

            // ── 根据自定义画面设置选择布局策略 ──
            val result: Array<Int> = SettingsDialogState.calcRatioAndPadding(windowWidth,windowHeight)
            val targetWidth = result[0]
            val targetHeight = result[1]
            val pl = result[2]
            val pt = result[3]
            val pr = result[4]
            val pb = result[5]

            // 二次校验：目标GL尺寸不能为0
            if (targetWidth <= 0 || targetHeight <= 0) return

            // 只有Padding真正变化时才设置，避免无意义requestLayout
            if (originalGameRoot.paddingLeft != pl
                || originalGameRoot.paddingTop != pt
                || originalGameRoot.paddingRight != pr
                || originalGameRoot.paddingBottom != pb
            ) {
                originalGameRoot.setPadding(pl, pt, pr, pb)
                originalGameRoot.requestLayout()
            }

            InitializePvz2.updateGlViewSize(targetWidth, targetHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 显示悬浮的画面设置弹窗
     * @param activity 宿主 Activity
     */
    fun show(activity: Activity) {
        GeneralFloatingDialogController.showDialog(activity) {
            BaseFloatingDialog(content = {
                GameDisplayDialogContent()
            })
        }
    }

    private fun applyAndDismiss(activity: Activity) {
        applyGameDisplaySettings(activity)
        modifyGameLayoutWithPadding(activity)
        GeneralFloatingDialogController.dismissDialog()
    }

    @Composable
    private fun GameDisplayDialogContent() {
        Column(
            modifier = Modifier
                .width(400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PvzPopupContent(
                title = InitializePvz2.config.ui.settings.customGameDisplayTitle,
                showBackButton = false,
                onClose = { GeneralFloatingDialogController.dismissDialog() },
                bottomContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val activity = LocalActivity.current!!
                        PvzGreenButton(
                            text = InitializePvz2.config.ui.settings.applyButtonText,
                            modifier = Modifier
                                .width(160.dp)
                                .height(44.dp)
                        ) {
                            applyAndDismiss(activity)
                        }
                    }
                }
            ) {
                GameDisplaySettingsContent()
            }
        }
    }
}