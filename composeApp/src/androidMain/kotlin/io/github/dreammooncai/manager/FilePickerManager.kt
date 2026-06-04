package io.github.dreammooncai.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.dreammooncai.pvz2tool.InitializePvz2

/**
 * 为Activity提供目录选择Launcher的管理类
 * 特性：1. 提前注册避免生命周期问题 2. 持久化上次选择的目录 3. 适配多SDK版本
 */
class FilePickerManager(private val activity: ComponentActivity) {
    // 通用的选择器Launcher（适配文件/目录选择）
    private val pickerLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // 1. 持久化Uri权限（文件/目录通用）
                    try {
                        activity.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 2. 根据选择类型创建DocumentFile
                    val documentFile = if (isCurrentPickDirectory) {
                        DocumentFile.fromTreeUri(activity, uri) // 目录用fromTreeUri
                    } else {
                        DocumentFile.fromSingleUri(activity, uri) // 文件用fromSingleUri
                    }

                    // 3. 保存有效Uri到SP（区分文件/目录）
                    if (documentFile?.exists() == true) {
                        val key = if (isCurrentPickDirectory) KEY_LAST_SELECTED_DIR else KEY_LAST_SELECTED_FILE
                        InitializePvz2.settings[key] = uri.toString()
                    }

                    // 4. 触发回调
                    onPickerSelected?.invoke(uri, documentFile)
                } ?: onPickerSelected?.invoke(null, null)
            } else {
                onPickerSelected?.invoke(null, null)
            }
            // 回调后清空引用，避免内存泄漏
            onPickerSelected = null
            // 重置当前选择类型标记
            isCurrentPickDirectory = true
        }

    // 回调函数（单次使用，回调后清空）
    private var onPickerSelected: ((Uri?, DocumentFile?) -> Unit)? = null

    // 标记当前选择的是目录还是文件（用于Launcher回调中区分处理）
    private var isCurrentPickDirectory: Boolean = true

    /**
     * 启动选择器（支持文件/目录选择）
     * @param isDirectory 是否选择目录（true=目录，false=文件）
     * @param fileMimeType 文件选择时的MIME类型（仅isDirectory=false时生效，默认所有类型）
     * @param onSelected 选择结果回调（Uri=选中的文件/目录Uri，DocumentFile=对应的文件/目录对象）
     */
    @SuppressLint("ObsoleteSdkInt")
    fun launch(
        isDirectory: Boolean = true,
        fileMimeType: String = "*/*",
        onSelected: (uri: Uri?, doc: DocumentFile?) -> Unit
    ) {
        // 1. 校验Activity状态
        if (activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)) {
            Toast.makeText(activity, "页面已销毁，无法打开选择器", Toast.LENGTH_SHORT).show()
            onSelected(null, null)
            return
        }

        // 2. 标记当前选择类型
        isCurrentPickDirectory = isDirectory

        // 3. 构建选择器Intent（区分文件/目录）
        val intent = if (isDirectory) {
            // 目录选择Intent（保留原逻辑）
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)

                // SDK21+显示高级选项
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putExtra("android.provider.extra.SHOW_ADVANCED", true)
                    putExtra("android.provider.extra.SHOW_FILES", true)
                }

                // SDK26+设置初始目录
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val initialUri = getInitialUri(isDirectory)
                    initialUri?.let {
                        try {
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else {
            // 文件选择Intent（新增逻辑）
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)

                // 设置文件类型（默认所有类型）
                type = fileMimeType
                // 允许读取文件名称等元数据
                addCategory(Intent.CATEGORY_OPENABLE)

                // SDK26+设置初始目录（文件选择时也基于上次目录/文件路径定位）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val initialUri = getInitialUri(isDirectory)
                    initialUri?.let {
                        try {
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // 4. 校验Intent是否可解析并启动
        val resolveInfo = activity.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_ALL
        )

        onPickerSelected = onSelected
        try {
            if (resolveInfo != null) {
                pickerLauncher.launch(intent)
            } else {
                // 无可用应用时，尝试用系统默认选择器
                pickerLauncher.launch(Intent.createChooser(intent, "选择文件/目录"))
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "打开选择器失败：${e.message}", Toast.LENGTH_SHORT).show()
            onSelected(null, null)
            onPickerSelected = null
        }
    }

    /**
     * 获取初始Uri（区分文件/目录）
     * @param isDirectory true=获取上次目录Uri，false=获取上次文件Uri
     */
    private fun getInitialUri(isDirectory: Boolean): Uri? {
        // 1. 优先读取上次保存的Uri
        val key = if (isDirectory) KEY_LAST_SELECTED_DIR else KEY_LAST_SELECTED_FILE
        val lastUriStr: String? = InitializePvz2.settings[key]
        if (!lastUriStr.isNullOrEmpty()) {
            val lastUri = lastUriStr.toUri()
            // 校验Uri有效性
            val lastDoc = if (isDirectory) {
                DocumentFile.fromTreeUri(activity, lastUri)
            } else {
                DocumentFile.fromSingleUri(activity, lastUri)
            }
            if (lastDoc?.exists() == true) {
                return lastUri
            } else {
                // 无效则清空保存的Uri
                InitializePvz2.settings[key] = null
            }
        }

        // 2. 无上次Uri时，返回外部存储根目录（文件/目录选择通用）
        return try {
            val externalUriStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "content://com.android.externalstorage.documents/tree/primary"
            } else {
                "content://com.android.externalstorage.documents/tree/primary%3A"
            }
            externalUriStr.toUri()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 伴生对象：管理常量和实例
     */
    companion object {
        // 区分文件/目录的持久化Key
        private const val KEY_LAST_SELECTED_DIR = "last_selected_directory_uri"
        private const val KEY_LAST_SELECTED_FILE = "last_selected_file_uri"
    }
}