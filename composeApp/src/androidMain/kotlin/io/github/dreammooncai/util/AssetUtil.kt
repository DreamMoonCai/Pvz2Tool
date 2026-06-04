package io.github.dreammooncai.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * 读取APK内assets文件的修改时间（和MT管理器逻辑一致）
 * @param assetPath assets内的文件路径，比如 "video/opening.mp4"
 * @return 时间戳（毫秒），失败返回0L
 */
fun Context.getAssetLastModified(assetPath: String): Long {
    return runCatching {
        // 1. 拿到当前APP的APK安装路径
        val apkPath = applicationInfo.sourceDir
        val apkFile = File(apkPath)

        // 2. 以ZipFile打开APK，解析ZIP结构
        ZipFile(apkFile).use { zipFile ->
            // 3. 拼接assets内文件的完整ZIP条目路径
            val zipEntryPath = "assets/$assetPath"
            val zipEntry = zipFile.getEntry(zipEntryPath) ?: return@runCatching 0L

            // 4. 直接拿到ZIP条目的修改时间（和MT管理器读取的完全一致）
            return@runCatching zipEntry.time
        }
    }.getOrElse { 0L }
}

/**
 * 判断Assets下的文件是否存在
 * @param assetFilePath 相对于assets根目录的文件路径
 * @return 文件存在返回true，不存在/是文件夹返回false
 */
fun Context.isAssetFileExist(assetFilePath: String): Boolean {
    // 清洗路径，兼容带前置斜杠的异常输入
    val cleanPath = assetFilePath.trimStart('/')
    return runCatching { assets.open(cleanPath).use { true } }.getOrDefault(false)
}

/**
 * 判断Assets下的文件夹是否存在
 * @param assetDirPath 相对于assets根目录的文件夹路径
 * @return 文件夹存在返回true，不存在/是文件返回false
 */
fun Context.isAssetDirExist(assetDirPath: String): Boolean {
    // 清洗路径，兼容前后斜杠的异常输入
    val cleanPath = assetDirPath.trimStart('/').trimEnd('/')
    return runCatching {
        val fileList = assets.list(cleanPath)
        // 非空文件夹才会被打包，列表非空即为存在
        !fileList.isNullOrEmpty()
    }.getOrDefault(false)
}

fun DocumentFile.toFile(context: Context): File? {
    val uri = uri

    // 1. 最简单的情况：本身就是 file:// 协议
    if ("file" == uri.scheme) {
        return uri.path?.let { File(it) }
    }

    // 2. 尝试通过 DocumentsContract 获取 (Android 7.0+)
    // 注意：这通常只对由 DocumentsProvider 支持的、且是物理文件的 URI 有效
    if ("content" == uri.scheme) {
        // 尝试 2.1: 检查是否是 Android 内置的 "External Storage Provider"
        // 这种 URI 通常可以直接拼接路径
        if ("com.android.externalstorage.documents" == uri.authority) {
            val docId = try {
                DocumentsContract.getDocumentId(uri)
            } catch (e: IllegalArgumentException) {
                null
            }

            docId?.split(":")?.let { split ->
                if (split.size >= 2) {
                    val type = split[0]
                    val path = split[1]

                    // 处理 "primary" (内置存储)
                    if ("primary".equals(type, ignoreCase = true)) {
                        val file = File("/storage/emulated/0/$path")
                        if (file.exists() && file.canWrite()) return file
                        // 有些设备路径可能不一样，多试一个
                        val file2 = File("/sdcard/$path")
                        if (file2.exists() && file.canWrite()) return file2
                    } else {
                        // 处理 SD 卡 (如 "1A0B-2C3D:path")
                        val file = File("/storage/$type/$path")
                        if (file.exists() && file.canWrite()) return file
                    }
                }
            }
        }

        // 尝试 2.2: 通用的 ContentResolver 查询 (旧版 MediaStore 等)
        // 注意：Android 10+ 由于分区存储，这里通常会返回 null 或空
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex("_data") // 即 MediaStore.MediaColumns.DATA
                    if (colIndex != -1) {
                        val path = cursor.getString(colIndex)
                        if (!path.isNullOrEmpty()) {
                            val file = File(path)
                            if (file.exists() && file.canWrite()) return@use file
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.w("ContentResolver", "ContentResolver 查询路径失败", e)
            null
        }
    }

    // 3. 所有方法都失败了
    return null
}

fun Context.openUriInputStreamOrAsset(uri: Uri): InputStream {
    if (uri.scheme == "file" && uri.path?.startsWith("/android_asset/") == true) {
        val assetPath = uri.path!!.removePrefix("/android_asset/")
        return assets.open(assetPath)
    }

    contentResolver.openInputStream(uri)?.let { return it }

    throw FileNotFoundException("无法打开URI: $uri")
}

fun Context.openUriInputStreamOrAssetNull(uri: Uri): InputStream? = runCatching { openUriInputStreamOrAsset(uri) }.getOrNull()