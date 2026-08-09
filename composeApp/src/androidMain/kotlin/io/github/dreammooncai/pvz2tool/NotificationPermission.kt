package io.github.dreammooncai.pvz2tool

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 通知权限（POST_NOTIFICATIONS）检测工具。
 *
 * 真正的运行时请求由 [Pvz2InitializeActivity] 在启动时发起；此处仅提供「是否已授权」的纯检测，
 * 供 JS API `notifications.isGranted()` / `通知.已授权()` 使用，不涉及任何请求/弹窗逻辑。
 */
object NotificationPermission {
    /** 是否已授予通知权限（API < 33 永远返回 true，该版本由安装时授予）。 */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
