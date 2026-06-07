package io.github.dreammooncai.pvz2tool.ui.dialog

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.pvz2tool.view.PvzRichText
import io.github.dreammooncai.pvz2tool.view.PvzTextStyle
import androidx.core.net.toUri
import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.decompressZLib
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.isZlib
import io.github.dreammooncai.pvz2tool.pop.core.rsb.util.toZlibInputStreamAndClose
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetResourceExtractor.ExtractFileInfo
import io.github.dreammooncai.pvz2tool.view.PvzProgressBar
import io.github.dreammooncai.util.getAssetLastModified
import io.github.dreammooncai.util.isAssetDirExist
import io.github.dreammooncai.util.isAssetFileExist
import io.github.dreammooncai.util.openUriInputStreamOrAsset
import io.github.dreammooncai.util.toFile
import kotlin.use

// ======================== 0. 资源源密封类 ========================
/**
 * 统一资源来源：可能是 Assets，也可能是本地文件（DocumentFile）
 */
sealed class ResourceSource {
    data class AssetPath(val path: String) : ResourceSource()
    data class LocalDocument(val document: DocumentFile) : ResourceSource() // 修改：File -> DocumentFile
}

// ======================== 1. 状态与配置类 ========================

/**
 * UI 状态：由提取器更新，Compose 观察此状态进行渲染
 */
data class ExtractorUiState(
    val isVisible: Boolean = false,
    val progress: Int = 0,
    val message: String = "",
    val isComplete: Boolean = false,
    val isContinue: Boolean = false,
    val isNotUpdate: Boolean = false,
)

data class ResourcePair(
    val source: ResourceSource,
    val targetDir: DocumentFile,
    val forceOverride: Boolean = false,
    val sectionName: String = "" // 【新增】功能栏名称
) {
    // 【修改】辅助构造函数增加 sectionName
    constructor(assetPath: String, targetDir: DocumentFile, forceOverride: Boolean = false, sectionName: String = "") :
            this(ResourceSource.AssetPath(assetPath), targetDir, forceOverride, sectionName)

    constructor(localDocument: DocumentFile, targetDir: DocumentFile, forceOverride: Boolean = false, sectionName: String = "") :
            this(ResourceSource.LocalDocument(localDocument), targetDir, forceOverride, sectionName)
}

// ======================== 2. 提取器逻辑类 ========================

private const val TAG = "PvzAssetExtractor"

private val textConfig get() = InitializePvz2.config.ui.extractor

class AssetResourceExtractor(
    private val context: Context,
    private val scope: CoroutineScope
) {

    val uiState = mutableStateOf(ExtractorUiState())

    private val activeProcessingFiles = ConcurrentHashMap<String, String>()
    // 【新增】回调：当文件被跳过（未解压）时触发
    private var onFileSkipped: ((ExtractFileInfo) -> Unit)? = null
    private var onCompleteListener: ((ExtractorUiState) -> Unit)? = null
    private var onDismissListener: ((ExtractorUiState) -> Unit)? = null

    private val failedFiles = ConcurrentHashMap<Uri, ExtractFileInfo>()
    private val maxRetryCount = 1

    fun setOnCompleteListener(listener: ((ExtractorUiState) -> Unit)?) {
        onCompleteListener = listener
    }

    fun setOnDismissListener(listener: ((ExtractorUiState) -> Unit)?) {
        onDismissListener = listener
    }

    fun setOnFileSkipped(listener: ((ExtractFileInfo) -> Unit)?) {
        onFileSkipped = listener
    }

    // ======================== 获取文件列表 ========================
    /**
     * 递归列出 assets 目录下所有文件的相对路径
     */
    fun listAssetFiles(assetPath: String): List<String> {
        val normalizedPath = assetPath.trim('/')
        return listAssetFilesRecursive(normalizedPath)
    }

    private fun listAssetFilesRecursive(assetPath: String): List<String> {
        val normalizedPath = assetPath.trim('/')
        return when {
            context.isAssetFileExist(normalizedPath) -> {
                // 是文件，返回自身
                listOf(normalizedPath)
            }
            context.isAssetDirExist(normalizedPath) -> {
                // 是目录，递归遍历子项
                val childNames = context.assets.list(normalizedPath) ?: emptyArray()
                childNames.flatMap { childName ->
                    val childPath = if (normalizedPath.isEmpty()) childName else "$normalizedPath/$childName"
                    listAssetFilesRecursive(childPath)
                }
            }
            else -> emptyList()
        }
    }

    // ======================== 完整的 extractResources 方法 ========================
    fun extractResources(
        vararg pairs: ResourcePair,
    ) {
        if (pairs.isEmpty()) {
            onCompleteListener?.let { listener ->
                onCompleteListener = null
                listener(uiState.value)
            }
            onDismissListener?.let { listener ->
                onDismissListener = null
                listener(uiState.value)
            }
            onCompleteListener = null
            onFileSkipped = null
            return
        }

        uiState.value = ExtractorUiState(
            isVisible = true,
            message = textConfig.initialLoadingProgressTip
        )

        scope.launch(Dispatchers.IO) {
            try {
                failedFiles.clear()

                val allNeedExtractFiles = mutableListOf<ExtractFileInfo>()

                // 1. 统一扫描：遍历所有ResourcePair，收集所有文件（含重复）
                pairs.forEach { pair ->
                    val (source, targetDir, forceOverride,sectionName) = pair
                    val safeTargetDir = safeCreateDocumentDir(targetDir, context)
                    when (source) {
                        is ResourceSource.AssetPath -> {
                            traverseAssets(
                                source.path,
                                safeTargetDir,
                                allNeedExtractFiles,
                                forceOverride,
                                sectionName,
                                isRootPath = true
                            )
                        }
                        is ResourceSource.LocalDocument -> {
                            traverseLocalDocument(
                                source.document,
                                safeTargetDir,
                                allNeedExtractFiles,
                                forceOverride,
                                sectionName,
                                isRootPath = true
                            )
                        }
                    }
                }

                // ======================== 核心修复：文件去重 ========================
                // 按文件唯一标识（targetDocument.uri）分组，只保留最后一个（优先级最高）
                val deduplicatedFiles = allNeedExtractFiles
                    .groupBy { it.targetDocument.uri.toString() } // 按文件URI去重
                    .mapValues { it.value.last() } // 保留每个文件最后一个处理的版本
                    .values.toList()
                // =================================================================

                // 2. 筛选需要解压的文件（基于去重后的列表）
                val needExtractFiles = deduplicatedFiles.filter { it.needExtract }
                val totalFiles = needExtractFiles.size

                if (needExtractFiles.isEmpty()) {
                    val sectionNames = pairs.map { it.sectionName }.toSet().filter { it.isNotBlank() }
                    updateProgress(100, String.format(textConfig.noNeedExtractTip,sectionNames.joinToString(", ")), isComplete = true, isNotUpdate = true)
                    return@launch
                }

                // 3. 准备解压（后续逻辑不变）
                val extractedSize = AtomicLong(0)
                val processedFiles = AtomicLong(0)
                val lastProgressUpdateTime = AtomicLong(0)
                val progressUpdateIntervalMs = 100L

                val knownTotalSize = needExtractFiles.sumOf { it.size }
                val hasUnknownSizeFiles = needExtractFiles.any { it.size <= 0 }

                val threadCount = minOf(Runtime.getRuntime().availableProcessors(), 4)
                val semaphore = Semaphore(threadCount)

                suspend fun processBatch(fileList: List<ExtractFileInfo>) {
                    coroutineScope {
                        fileList.forEach { fileInfo ->
                            launch {
                                processExtractFile(
                                    fileInfo = fileInfo,
                                    semaphore = semaphore,
                                    extractedSize = extractedSize,
                                    processedFiles = processedFiles,
                                    lastProgressUpdateTime = lastProgressUpdateTime,
                                    progressUpdateIntervalMs = progressUpdateIntervalMs,
                                    knownTotalSize = knownTotalSize,
                                    hasUnknownSizeFiles = hasUnknownSizeFiles,
                                    totalFiles = totalFiles
                                )
                            }
                        }
                    }
                }

                // 4. 第一轮处理
                updateProgress(0, textConfig.initialLoadingProgressTip, isComplete = true)
                processBatch(needExtractFiles)

                // 5. 【新增】重试循环逻辑
                var currentRetry = 0
                while (failedFiles.isNotEmpty() && currentRetry < maxRetryCount) {
                    currentRetry++
                    val filesToRetry = failedFiles.values.toList()

                    Log.w(TAG, "有 ${filesToRetry.size} 个文件失败，开始第 $currentRetry 次重试...")
                    updateProgress(uiState.value.progress, "正在重试失败文件 ($currentRetry/$maxRetryCount)...")

                    // 清空失败列表，准备记录新一轮的结果
                    failedFiles.clear()

                    // 执行重试
                    processBatch(filesToRetry)
                }

                // 6. 最终结果处理
                activeProcessingFiles.clear()
                val sectionNames = pairs.map { it.sectionName }.toSet().filter { it.isNotBlank() }

                if (failedFiles.isNotEmpty()) {
                    // 尽管重试了，依然有失败
                    val msg = String.format(textConfig.extractCompleteTip, sectionNames.joinToString(", ")) + " (部分失败: ${failedFiles.size})"
                    updateProgress(100, msg, isComplete = true)
                    Log.e(TAG, "提取结束，但仍有 ${failedFiles.size} 个文件无法恢复。")
                } else {
                    // 全部成功（或重试后成功）
                    updateProgress(100, String.format(textConfig.extractCompleteTip, sectionNames.joinToString(", ")), isComplete = true)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    // ======================== 优化后的文件处理方法 ========================
    private suspend fun processExtractFile(
        fileInfo: ExtractFileInfo,
        semaphore: Semaphore,
        extractedSize: AtomicLong,
        processedFiles: AtomicLong,
        lastProgressUpdateTime: AtomicLong,
        progressUpdateIntervalMs: Long,
        knownTotalSize: Long,
        hasUnknownSizeFiles: Boolean,
        totalFiles: Int
    ) {
        semaphore.acquire()
        try {
            val fileName = fileInfo.targetDocument.name ?: "未知文件"
            activeProcessingFiles[fileName] = fileInfo.sectionName
            Log.d(TAG, "开始处理文件: $fileName")

            // 核心修复1：确保目标文件的父目录存在（处理parentFile为null）
            val targetParentDir = fileInfo.targetDocument.parentFile
            if (targetParentDir != null) {
                safeCreateDocumentDir(targetParentDir, context)
            } else {
                // parentFile为null时的兜底处理
                val fallbackDir = fileInfo.targetDocument.toFile(context)?.parentFile
                if (fallbackDir?.mkdirs() != true) {
                    throw IOException("目标文件父目录为null，且无法通过File方式创建")
                }
            }

            // 核心修复2：检查目标文件可写性
            if (!fileInfo.targetDocument.canWrite()) {
                throw IOException("目标文件不可写: $fileName")
            }

            // 核心修复3：打开输入流（保持原有逻辑）
            val inputStream = CoroutineBinaryStream.openStream(fileInfo.size) {
                when (fileInfo.source) {
                    is ResourceSource.AssetPath -> context.assets.open(fileInfo.source.path)
                    is ResourceSource.LocalDocument -> {
                        fileInfo.source.document.toFile(context)?.inputStream() ?:
                        context.contentResolver.openInputStream(fileInfo.source.document.uri)
                        ?: throw IOException("无法打开本地文件流: ${fileInfo.source.document.name}")
                    }
                }
            }.toZlibInputStreamAndClose()

            inputStream.use { input ->

                val targetFile = fileInfo.targetDocument.toFile(context)

                var name: String

                val outputStream = if (targetFile != null) {
                    name = targetFile.name
                    FileOutputStream(targetFile, false)
                } else {
                    // 确保目标文件存在（不存在则创建）
                    val targetDoc = if (fileInfo.targetDocument.exists()) {
                        // 【关键修复 1】如果文件已存在，先尝试删除它！
                        // 这是最稳妥的方案，防止内容残留
                        fileInfo.targetDocument.delete()
                        // 重新创建
                        val parentDir = fileInfo.targetDocument.parentFile ?: safeCreateDocumentDir(
                            DocumentFile.fromFile(File(context.cacheDir, "temp")),
                            context
                        )
                        val mimeType = fileInfo.targetDocument.type ?: "*/*"
                        val fileName = fileInfo.targetDocument.name ?: "unknown_file"
                        parentDir.createFile(mimeType, if (fileInfo.targetDocument.type != null) File(fileName).nameWithoutExtension else fileName)
                            ?: throw IOException("目标文件删除后无法重建: $fileName")
                    } else {
                        // 文件不存在，正常创建逻辑
                        val parentDir = fileInfo.targetDocument.parentFile ?: safeCreateDocumentDir(
                            DocumentFile.fromFile(File(context.cacheDir, "temp")),
                            context
                        )
                        val mimeType = fileInfo.targetDocument.type ?: "*/*"
                        val fileName = fileInfo.targetDocument.name ?: "unknown_file"
                        parentDir.createFile(mimeType, if (fileInfo.targetDocument.type != null) File(fileName).nameWithoutExtension else fileName)
                            ?: throw IOException("目标文件不存在且无法创建: $fileName")
                    }

                    name = targetDoc.name ?: "unknown_file"
                    // 【关键修复 2】打开输出流，强制使用 "wt" 模式
                    // 即使上面的删除失败了，"wt" 模式也能保证文件被截断
                    context.contentResolver.openOutputStream(targetDoc.uri, "wt")
                        ?: throw IOException("无法创建目标文件输出流: ${targetDoc.name}")
                }

                outputStream.use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        extractedSize.addAndGet(bytesRead.toLong())

                        // ... (进度更新逻辑保持不变) ...
                        val now = System.currentTimeMillis()
                        val last = lastProgressUpdateTime.get()
                        if (now - last >= progressUpdateIntervalMs) {
                            if (lastProgressUpdateTime.compareAndSet(last, now)) {
                                val progress = if (!hasUnknownSizeFiles && knownTotalSize > 0) {
                                    (extractedSize.get() * 100 / knownTotalSize).toInt()
                                } else {
                                    (processedFiles.get() * 100 / totalFiles).toInt()
                                }
                                updateProgress(progress, getProcessingTip())
                            }
                        }
                    }
                    output.flush()
                    // 确保所有数据都刷入磁盘后再关闭
                    Log.d(TAG, "文件写入完成: $name")
                }
            }

            if (fileInfo.sourceLastModified > 0) {
                val targetFile = fileInfo.targetDocument.toFile(context)
                if (targetFile != null) {
                    // 情况 A：如果是普通 File 对象，直接设置
                    targetFile.setLastModified(fileInfo.sourceLastModified)
                } else {
                    // 情况 B：如果是 SAF (DocumentFile)，尝试通过 ContentResolver 修改
                    // 注意：这需要 Android API >= 29，或者 DocumentsContract 支持
                    try {
                        val values = ContentValues().apply {
                            put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, fileInfo.sourceLastModified)
                        }
                        context.contentResolver.update(
                            fileInfo.targetDocument.uri,
                            values,
                            null,
                            null
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "无法通过 SAF 设置文件时间: ${fileInfo.targetDocument.name}", e)
                        // SAF 修改失败通常是权限或 ROM 限制，一般不视为致命错误
                    }
                }
            }

            processedFiles.incrementAndGet()
            Log.d(TAG, "处理完成: $fileName")
        } catch (e: Exception) {
            // 【修改】捕获所有异常，不调用 handleError（避免中断整体流程），而是记录下来
            val fileName = fileInfo.targetDocument.name ?: "未知文件"
            Log.w(TAG, "文件处理出错（将记录以便重试）: $fileName", e)
            // 记录失败
            failedFiles[fileInfo.targetDocument.uri] = fileInfo
        } finally {
            val fileName = fileInfo.targetDocument.name ?: "未知文件"
            activeProcessingFiles.remove(fileName)
            semaphore.release()
        }
    }

    // ======================== 完整的安全创建目录方法 ========================
    private fun safeCreateDocumentDir(targetDir: DocumentFile, context: Context): DocumentFile {
        // 1. 基础存在性和类型检查
        if (targetDir.exists() && targetDir.isDirectory) {
            return targetDir
        }

        val dirName = targetDir.name ?: "unknown_dir_${System.currentTimeMillis()}"

        // 2. 处理parentFile为null的情况（核心兜底逻辑）
        val parentDir = targetDir.parentFile ?: run {
            Log.w(TAG, "parentFile为null，尝试通过File方式兜底创建")
            // 转换为File对象
            val targetFile = targetDir.toFile(context) ?: throw IOException(
                "parentFile为null且无法转换为File: ${targetDir.uri}"
            )
            // 创建目录
            if (!targetFile.mkdirs()) {
                throw IOException("File方式创建目录失败: ${targetFile.absolutePath}")
            }
            // 转回DocumentFile并返回
            return DocumentFile.fromFile(targetFile)
        }

        // 3. 递归创建父目录
        val createdParentDir = safeCreateDocumentDir(parentDir, context)

        // 4. 检查父目录是否可写
        if (!createdParentDir.canWrite()) {
            throw IOException("父目录不可写: ${createdParentDir.uri}")
        }

        // 5. 处理同名冲突
        val existing = createdParentDir.findFile(dirName)
        if (existing != null) {
            if (existing.isDirectory) {
                return existing // 已有同名目录，直接返回
            } else {
                // 存在同名文件，先删除
                if (!existing.delete()) {
                    throw IOException("无法删除同名文件: $dirName")
                }
            }
        }

        // 6. 创建当前目录
        val newDir = createdParentDir.createDirectory(dirName)
        if (newDir != null && newDir.exists()) {
            Log.d(TAG, "成功创建目录: ${newDir.uri}")
            return newDir
        }

        // 7. DocumentFile创建失败，最终兜底（File方式）
        Log.w(TAG, "DocumentFile创建目录失败，尝试File方式兜底")
        val parentFile = createdParentDir.toFile(context) ?: throw IOException(
            "无法将父目录转换为File: ${createdParentDir.uri}"
        )
        val targetFile = File(parentFile, dirName)
        if (!targetFile.mkdirs()) {
            throw IOException("最终兜底失败，无法创建目录: ${targetFile.absolutePath}")
        }
        val fallbackDir = DocumentFile.fromFile(targetFile)
        Log.d(TAG, "File方式兜底创建目录成功: ${fallbackDir.uri}")
        return fallbackDir
    }

    private fun traverseAssets(
        assetPath: String,
        targetDir: DocumentFile,
        allNeedExtractFiles: MutableList<ExtractFileInfo>,
        forceOverride: Boolean,
        sectionName: String,
        isRootPath: Boolean = false // 新增参数
    ) {
        val normalizedAssetPath = assetPath.trim('/')

        when {
            context.isAssetFileExist(normalizedAssetPath) -> {
                // 文件逻辑保持不变
                val fileName = normalizedAssetPath.substringAfterLast("/")
                if (fileName.lowercase().endsWith(".js") || fileName.lowercase().contains("placeholders")) {
                    Log.d(TAG, "跳过 JS 文件 (Assets): $normalizedAssetPath")
                    return
                }
                var targetDocument = targetDir.findFile(fileName)

                if (targetDocument == null) {
                    targetDocument = targetDir.createFile("*/*", fileName)
                        ?: throw IOException("无法在目录 ${targetDir.uri} 创建文件: $fileName")
                } else if (!targetDocument.isFile) {
                    targetDocument.delete()
                    targetDocument = targetDir.createFile("*/*", fileName)
                        ?: throw IOException("删除同名目录后仍无法创建文件: $fileName")
                }

                val fileInfo = getExtractFileInfo(
                    source = ResourceSource.AssetPath(normalizedAssetPath),
                    targetDocument = targetDocument,
                    forceOverride = forceOverride,
                    sectionName = sectionName // 【新增】
                )
                checkAndAddToList(fileInfo, allNeedExtractFiles)
            }

            context.isAssetDirExist(normalizedAssetPath) -> {
                val currentDirName = if (normalizedAssetPath.isEmpty()) {
                    ""
                } else {
                    normalizedAssetPath.substringAfterLast("/")
                }

                // 计算当前的目标目录
                val currentTargetDir = if (isRootPath || currentDirName.isEmpty()) {
                    // 如果是根路径，或者 assetPath 本身就是空根，直接使用传入的 targetDir
                    targetDir
                } else {
                    // 非根路径，正常创建目录
                    var dir = targetDir.findFile(currentDirName)
                    if (dir == null) {
                        dir = targetDir.createDirectory(currentDirName) ?: throw IOException("无法创建目录: $currentDirName")
                    } else if (!dir.isDirectory) {
                        dir.delete()
                        dir = targetDir.createDirectory(currentDirName) ?: throw IOException("删除同名文件后仍无法创建目录: $currentDirName")
                    }
                    dir
                }

                // 遍历子项
                val childNames = context.assets.list(normalizedAssetPath) ?: emptyArray()
                childNames.forEach { childName ->
                    val childAssetPath = if (normalizedAssetPath.isEmpty()) {
                        childName
                    } else {
                        "$normalizedAssetPath/$childName"
                    }
                    // 递归调用，isRootPath 设为 false
                    traverseAssets(childAssetPath, currentTargetDir, allNeedExtractFiles, forceOverride, sectionName = sectionName,isRootPath = false)
                }
            }
        }
    }

    // ======================== 修复后的 traverseLocalDocument ========================
    private fun traverseLocalDocument(
        sourceDoc: DocumentFile,
        targetDir: DocumentFile,
        allNeedExtractFiles: MutableList<ExtractFileInfo>,
        forceOverride: Boolean,
        sectionName: String,
        isRootPath: Boolean = false
    ) {
        if (!sourceDoc.exists()) return
        val safeTargetDir = safeCreateDocumentDir(targetDir, context)

        if (sourceDoc.isFile) {
            // 文件逻辑保持不变
            val fileName = sourceDoc.name ?: "unknown_file_${System.currentTimeMillis()}"
            if (fileName.lowercase().endsWith(".js") || fileName.lowercase().contains("placeholders")) {
                Log.d(TAG, "跳过 JS 文件 (Local): ${sourceDoc.uri}")
                return
            }
            if (!safeTargetDir.canWrite()) return

            var targetDocument = safeTargetDir.findFile(fileName)
            if (targetDocument == null || !targetDocument.exists()) {
                val mimeType = sourceDoc.type ?: "*/*"
                targetDocument = safeTargetDir.createFile(mimeType, if (sourceDoc.type != null) File(fileName).nameWithoutExtension else fileName)
                    ?: throw IOException("无法创建文件: $fileName")
            } else if (!targetDocument.isFile) {
                if (targetDocument.delete()) {
                    targetDocument = safeTargetDir.createFile(sourceDoc.type ?: "*/*", if (sourceDoc.type != null) File(fileName).nameWithoutExtension else fileName)
                        ?: throw IOException("删除同名目录后仍无法创建文件: $fileName")
                }
            }

            val fileInfo = getExtractFileInfo(
                source = ResourceSource.LocalDocument(sourceDoc),
                targetDocument = targetDocument,
                forceOverride = forceOverride,
                sectionName = sectionName // 【新增】
            )
            checkAndAddToList(fileInfo, allNeedExtractFiles)
        } else if (sourceDoc.isDirectory) {
            val dirName = sourceDoc.name ?: "unknown_dir_${System.currentTimeMillis()}"
            val currentTargetDir = if (isRootPath || sourceDoc.name.isNullOrEmpty()) {
                safeTargetDir
            } else {
                // 计算当前的目标目录
                var childTargetDir = safeTargetDir.findFile(dirName)
                if (childTargetDir == null || !childTargetDir.exists()) {
                    childTargetDir = safeTargetDir.createDirectory(dirName) ?: throw IOException("无法创建目录: $dirName")
                } else if (!childTargetDir.isDirectory) {
                    if (childTargetDir.delete()) {
                        childTargetDir = safeTargetDir.createDirectory(dirName) ?: throw IOException("删除同名文件后仍无法创建目录: $dirName")
                    }
                }
                childTargetDir
            }

            // 遍历子文件/目录
            sourceDoc.listFiles().forEach { childDoc ->
                // 递归调用，isRootPath 设为 false
                traverseLocalDocument(childDoc, currentTargetDir, allNeedExtractFiles, forceOverride, sectionName = sectionName,isRootPath = false)
            }
        }
    }

    private fun ensureDocumentDirExists(targetDir: DocumentFile) {
        if (targetDir.exists() && targetDir.isDirectory) return

        // 递归创建父目录
        val parent = targetDir.parentFile
        if (parent != null && (!parent.exists() || !parent.isDirectory)) {
            ensureDocumentDirExists(parent)
        }

        // 确保父目录存在且是目录
        if (parent == null || !parent.exists() || !parent.isDirectory) {
            throw IOException("父目录不存在或不是目录: ${parent?.uri ?: "null"}")
        }

        // 创建当前目录（使用DocumentFile原生方法）
        val dirName = targetDir.name ?: "unknown_dir"
        parent.createDirectory(dirName) ?: throw IOException("无法创建目录: $dirName")
    }


    private fun checkAndAddToList(info: ExtractFileInfo, list: MutableList<ExtractFileInfo>) {
        if (!info.needExtract) {
            val displayName = info.targetDocument.name ?: "未知文件"
            val sectionLabel = if (info.sectionName.isNotEmpty()) "【${info.sectionName}】" else ""
            updateProgress(0, String.format("${sectionLabel}${textConfig.fileSkipTipPrefix}", displayName), isComplete = true)
            onFileSkipped?.invoke(info)
        }
        list.add(info)
    }

    // ======================== 统一的文件信息获取 (适配DocumentFile) ========================

    data class ExtractFileInfo(
        val source: ResourceSource,
        val targetDocument: DocumentFile,
        val size: Long,
        val needExtract: Boolean,
        val forceOverride: Boolean = false,
        val sectionName: String = "",
        val sourceLastModified: Long = 0L
    )

    private fun getExtractFileInfo(
        source: ResourceSource,
        targetDocument: DocumentFile,
        forceOverride: Boolean,
        sectionName: String = ""
    ): ExtractFileInfo {
        val sourceSize: Long
        var needExtract: Boolean
        val sourceLastModified: Long

        when (source) {
            is ResourceSource.AssetPath -> {
                sourceSize = try {
                    context.assets.openFd(source.path).use { it.length }
                } catch (e: IOException) {
                    runCatching { context.assets.open(source.path).use { it.available().toLong() } }.getOrElse { -1 }
                }
                sourceLastModified = context.getAssetLastModified(source.path)
                val targetLength = targetDocument.length()

                needExtract = if (!targetDocument.exists() || targetLength == 0L) {
                    true
                } else {
                    if (forceOverride) {
                        true
                    }
                    // 3. 【新增】时间校验：如果源文件时间 <= 目标文件时间，则不覆盖
                    else if (sourceLastModified > 0L && sourceLastModified <= targetDocument.lastModified()) {
                        Log.d(TAG, "源文件时间旧于目标文件，跳过覆盖: ${targetDocument.uri}")
                        false
                    }
                    // 4. 原有逻辑：大小不同则覆盖
                    else {
                        targetDocument.length() != sourceSize
                    }
                }
            }
            is ResourceSource.LocalDocument -> {
                sourceSize = source.document.length()
                sourceLastModified = source.document.lastModified() // 直接获取本地文件时间
                val targetLength = targetDocument.length()

                needExtract = if (!targetDocument.exists() || targetLength == 0L) {
                    true
                } else {
                    if (forceOverride) {
                        true
                    }
                    // 【新增】时间校验：如果源文件时间 <= 目标文件时间，则不覆盖
                    else if (sourceLastModified <= targetDocument.lastModified()) {
                        Log.d(TAG, "源文件时间旧于目标文件，跳过覆盖: ${targetDocument.uri}")
                        false
                    }
                    // 原有逻辑：大小不同则覆盖
                    else {
                        targetDocument.length() != sourceSize
                    }
                }
            }
        }

        return ExtractFileInfo(source, targetDocument, sourceSize, needExtract, forceOverride, sectionName)
    }

    // ======================== 辅助方法 ========================
    private fun handleError(e: Exception) {
        activeProcessingFiles.clear()
        val errorMsg = "${textConfig.extractFailTipPrefix}${e.message ?: "未知错误"}"
        updateProgress(0, errorMsg, isComplete = true)
        Log.e(TAG, "解压失败", e)
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, "${textConfig.toastErrorPrefix}${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateProgress(progress: Int, message: String, isComplete: Boolean = false, isNotUpdate: Boolean = false) {
        uiState.value = uiState.value.copy(
            progress = progress.coerceIn(0, 100),
            message = message,
            isComplete = isComplete,
            isNotUpdate = isNotUpdate,
            isContinue = this.onDismissListener == null
        )
        if (isComplete && progress >= 100) {
            onCompleteListener?.let { listener ->
                onCompleteListener = null
                listener(uiState.value)
            }
        }
    }

    fun dismiss() {
        uiState.value = uiState.value.copy(isVisible = false)
        onDismissListener?.let { listener ->
            onDismissListener = null
            listener(uiState.value)
        }
        onCompleteListener = null
        onFileSkipped = null
    }

    private fun getProcessingTip(): String {
        val activeFiles = activeProcessingFiles.keys.toList()
        // 【新增】获取当前处理的栏目名称（取第一个活跃文件的栏目名称）
        val currentSectionName = activeProcessingFiles.entries.firstOrNull()?.value ?: ""
        val sectionLabel = if (currentSectionName.isNotEmpty()) "【$currentSectionName】" else ""

        return when {
            activeFiles.isEmpty() -> "${sectionLabel}${textConfig.waitingTip}"
            activeFiles.size == 1 -> "${sectionLabel}${textConfig.singleFileProcessingTip}${activeFiles[0]}"
            else -> sectionLabel + String.format(textConfig.multiFileProcessingTip, activeFiles.size) + "\n${activeFiles.joinToString(", ")}"
        }
    }


}

// ======================== 3. Compose UI 组件 (保持原样) ========================

@Composable
fun PvzExtractorDialog(
    uiState: ExtractorUiState,
    isShowNotUpdate: Boolean = true,
    onDismissRequest: () -> Unit
) {
    val textConfig = InitializePvz2.config.ui.extractor

    if (uiState.isVisible && uiState.isComplete && uiState.isNotUpdate && !isShowNotUpdate) return onDismissRequest()

    // 复用通用弹窗框架
    PvzStyledDialog(
        isVisible = uiState.isVisible,
        titleText = textConfig.dialogTitle,
        // 保留原有的关闭逻辑：仅当完成时才响应关闭
        onDismissRequest = {
            if (uiState.isComplete) onDismissRequest()
        },
        dismissible = false, // 提取过程中不允许点击外部关闭
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        bottomContent = {
            Spacer(modifier = Modifier.height(15.dp))

            PvzProgressBar(
                progress = uiState.progress / 100f,
                label = if (uiState.isComplete)
                    if (uiState.isContinue) textConfig.continueButtonText else textConfig.completeButtonText
                else null,
                modifier = Modifier.fillMaxWidth(),
                onLabelClick = onDismissRequest
            )
        }
    ) {
        PvzRichText(
            text = "${uiState.message} (${uiState.progress}%)",
            defaultStyle = PvzTextStyle(Color(0xFF423F00)),
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ======================== 4. 辅助调用类 (Holder) (适配DocumentFile) ========================

@Composable
fun rememberAssetExtractor(
    context: Context = LocalContext.current
): AssetExtractorHolder {
    val scope = rememberCoroutineScope()
    return remember {
        AssetExtractorHolder(
            AssetResourceExtractor(
                context = context,
                scope = scope
            )
        )
    }
}

class AssetExtractorHolder(
    val extractor: AssetResourceExtractor
) {
    companion object {
        // ======================== 获取资源文件列表 ========================
        /**
         * 获取 assets 目录下所有文件的相对路径列表
         * 自动补充路径前缀
         */
        fun listAssetFiles(assetPath: String): List<String> {
            val normalizedPath = complementThePrefix(assetPath)
            return listAssetFilesRecursive(normalizedPath)
        }

        private fun listAssetFilesRecursive(assetPath: String): List<String> {
            return when {
                InitializePvz2.context.isAssetFileExist(assetPath) -> {
                    listOf(assetPath)
                }
                InitializePvz2.context.isAssetDirExist(assetPath) -> {
                    val childNames = InitializePvz2.context.assets.list(assetPath) ?: emptyArray()
                    childNames.flatMap { childName ->
                        val childPath = if (assetPath.isEmpty()) childName else "$assetPath/$childName"
                        listAssetFilesRecursive(childPath)
                    }
                }
                else -> emptyList()
            }
        }

        /**
         * 获取资源目录下所有文件的相对路径列表（本地优先）
         * 如果本地目录存在，从本地获取；否则从 assets 获取
         */
        fun listResources(internalPath: String): List<String> {
            val localWorkDir = runCatching { InitializePvz2.config.getLocalWorkDir(InitializePvz2.context) }.getOrNull()

            if (localWorkDir != null) {
                val localDocument = buildDocumentFilePath(localWorkDir, removeThePrefix(internalPath))
                if (localDocument?.exists() == true) {
                    return listLocalDocumentFiles(localDocument)
                }
            }
            // 本地没有，从 assets 获取
            return listAssetFiles(internalPath)
        }

        private fun listLocalDocumentFiles(document: DocumentFile): List<String> {
            return if (document.isFile) {
                listOf(document.name ?: "")
            } else {
                document.listFiles().flatMap { child ->
                    val prefix = if (document.name.isNullOrEmpty()) "" else "${document.name}/"
                    listLocalDocumentFiles(child).map { "$prefix$it" }
                }
            }
        }

        /**
         * 打开一个资源流或URL
         * 优先级：URL > getLocalWorkDir (SAF/本地文件) > Assets
         */
        fun open(path: String): Uri? {
            // 0. 检测是否为 URL（支持 http:// 和 https://）
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Log.d("SmartResource", "使用远程 URL: $path")
                return Uri.parse(path)
            }

            // 1. 获取本地工作目录
            val localWorkDir = runCatching { InitializePvz2.config.getLocalWorkDir(InitializePvz2.context) }.getOrNull()

            if (localWorkDir != null) {
                // 尝试在本地目录中递归寻找文件
                val localDocument = buildDocumentFilePath(localWorkDir, removeThePrefix(path))

                if (localDocument != null && localDocument.exists() && localDocument.isFile) {
                    return try {
                        Log.d("SmartResource", "从本地目录读取: ${localDocument.uri}")
                        localDocument.uri
                    } catch (e: Exception) {
                        Log.e("SmartResource", "读取本地文件失败: $path", e)
                        null
                    }
                }
            }
            val assetPath = complementThePrefix(path)
            Log.d("SmartResource", "从 Assets 读取: $assetPath")
            // 2. 如果本地没有，则从 Assets 获取
            return if (InitializePvz2.context.isAssetFileExist(assetPath)) "file:///android_asset/$assetPath".toUri() else null
        }

        fun openInputStream(path: String): InputStream? = runCatching {
            open(path)?.let { InitializePvz2.context.openUriInputStreamOrAsset(it) }
        }.getOrNull()

        fun exist(path: String): Boolean {
            val localWorkDir = runCatching { InitializePvz2.config.getLocalWorkDir(InitializePvz2.context) }.getOrNull()
            if (localWorkDir != null) {
                val localDocument = buildDocumentFilePath(localWorkDir, removeThePrefix(path))
                if (localDocument != null && localDocument.exists()) return true
            }
            val assetPath = complementThePrefix(path)
            return InitializePvz2.context.isAssetFileExist(assetPath) || InitializePvz2.context.isAssetDirExist(assetPath)
        }

        private fun complementThePrefix(path: String): String = if (path.startsWith("${Pvz2ToolConfig.PATH_NAME}/")) path else if (path.startsWith("/")) "${Pvz2ToolConfig.PATH_NAME}$path" else "${Pvz2ToolConfig.PATH_NAME}/$path"

        private fun removeThePrefix(path: String): String = path.removePrefix("${Pvz2ToolConfig.PATH_NAME}/").removePrefix(Pvz2ToolConfig.PATH_NAME)

        fun resource(
            internalPath: String,
            targetDir: DocumentFile,
            forceOverride: Boolean = false,
            sectionName: String = "" // 【新增】
        ): ResourcePair {
            val localWorkDir = InitializePvz2.config.getLocalWorkDir(InitializePvz2.context) ?: run {
                Log.w("SmartResource", "本地工作目录为空，使用APK Assets")
                return ResourcePair(ResourceSource.AssetPath(complementThePrefix(internalPath)), targetDir, forceOverride, sectionName)
            }

            val localDocument = buildDocumentFilePath(localWorkDir, removeThePrefix(internalPath))

            return if (localDocument?.exists() == true) {
                Log.d("SmartResource", "使用本地文件: ${localDocument.uri}")
                ResourcePair(ResourceSource.LocalDocument(localDocument), targetDir, forceOverride, sectionName)
            } else {
                Log.d("SmartResource", "使用APK Assets: $internalPath")
                ResourcePair(ResourceSource.AssetPath(complementThePrefix(internalPath)), targetDir, forceOverride, sectionName)
            }
        }

        // 修复路径构建方法：仅为目录创建子目录，文件直接放在对应目录下
        private fun buildDocumentFilePath(root: DocumentFile, path: String): DocumentFile? {
            var current = root
            val segments = path.split("/").filter { it.isNotEmpty() }

            segments.forEach { segment ->
                current = current.listFiles().firstOrNull { it.name == segment } ?: return null
            }
            return current
        }

        // 重载方法：适配DocumentFile
        fun resource(internalPath: String, targetDir: File,forceOverride: Boolean = false, sectionName: String = ""): ResourcePair = resource(internalPath, DocumentFile.fromFile(targetDir),forceOverride,sectionName)
        fun resource(localDocument: DocumentFile, targetDir: DocumentFile,forceOverride: Boolean = false, sectionName: String = "") = ResourcePair(localDocument, targetDir,forceOverride,sectionName)

        fun resource(localDocument: File, targetDir: File,forceOverride: Boolean = false, sectionName: String = "") = ResourcePair(DocumentFile.fromFile(localDocument), DocumentFile.fromFile(targetDir),forceOverride,sectionName)

        // 兼容旧Pair格式（String -> DocumentFile）
        fun resource(pair: Pair<String, DocumentFile>,forceOverride: Boolean = false, sectionName: String = "") = ResourcePair(pair.first, pair.second,forceOverride,sectionName)

    }

    fun setOnCompleteListener(listener: ((ExtractorUiState) -> Unit)?) {
        extractor.setOnCompleteListener(listener)
    }

    fun setOnDismissListener(listener: ((ExtractorUiState) -> Unit)?) {
        extractor.setOnDismissListener(listener)
    }

    fun setOnFileSkipped(listener: ((ExtractFileInfo) -> Unit)?) {
        extractor.setOnFileSkipped(listener)
    }

    // 【修改】最终 extract 调用
    fun extract(
        vararg pairs: ResourcePair
    ) {
        extractor.extractResources(*pairs)
    }

    // 保留一个简易兼容版
    fun extract(
        assetPath: String,
        targetDir: DocumentFile,
        forceOverride: Boolean = false,
        sectionName: String = "",
    ) {
        extract(resource(assetPath, targetDir, forceOverride, sectionName))
    }

    fun extract(
        assetPath: String,
        targetDir: File,
        forceOverride: Boolean = false,
        sectionName: String = ""
    ) {
        extract(resource(assetPath, DocumentFile.fromFile(targetDir), forceOverride, sectionName))
    }

    // ======================== 获取文件列表方法 ========================
    /**
     * 获取 assets 目录下所有文件的相对路径列表
     */
    fun listAssetFiles(assetPath: String): List<String> = extractor.listAssetFiles(assetPath)
}