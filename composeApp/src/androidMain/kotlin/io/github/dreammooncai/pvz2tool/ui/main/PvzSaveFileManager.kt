package io.github.dreammooncai.pvz2tool.ui.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume

object PvzSaveFileManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val gameSaveChangeListeners = mutableListOf<(Long) -> Unit>()

    /**
     * 注册游戏存档变化监听（游戏目录pp.dat被修改时触发）
     */
    fun addGameSaveChangeListener(listener: (Long) -> Unit) {
        gameSaveChangeListeners.add(listener)
    }

    /**
     * 移除游戏存档变化监听
     */
    fun removeGameSaveChangeListener(listener: (Long) -> Unit) {
        gameSaveChangeListeners.remove(listener)
    }

    /**
     * 发送游戏存档变化通知（所有修改游戏pp.dat的操作完成后调用）
     */
    fun notifyGameSaveChanged() {
        val time = System.currentTimeMillis()
        gameSaveChangeListeners.forEach { it(time) }
    }

    /**
     * 等待操作结果弹窗关闭的回调
     */
    typealias OnWaitForResultDismiss = suspend () -> Unit

    /**
     * 备份当前存档（本地文件版）
     */
    fun backupCurrentSave(
        context: Context,
        gameSaveDir: File,
        backupDir: File = PvzLocalSaveManager.getLocalSaveRootDir(context)
    ): PvzSaveOperationResult {
        val saveConfig = InitializePvz2.config.ui.save // 获取配置
        return try {
            if (!gameSaveDir.exists() || !gameSaveDir.isDirectory || gameSaveDir.list()?.isEmpty() ?: true) {
                return PvzSaveOperationResult(
                    type = PvzSaveOperationType.BACKUP,
                    isSuccess = false,
                    message = "当前存档目录不存在或不是有效目录，或为空目录"
                )
            }

            gameSaveDir.copyRecursively(backupDir,true)

            // 触发刷新
            PvzLocalSaveManager.triggerRefresh()

            PvzSaveOperationResult(
                type = PvzSaveOperationType.BACKUP,
                isSuccess = true,
                message = String.format(saveConfig.backupSuccessTip, backupDir.absolutePath) // 读取配置
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PvzSaveOperationResult(
                type = PvzSaveOperationType.BACKUP,
                isSuccess = false,
                message = String.format(saveConfig.backupFailTipPrefix, e.message ?: "未知错误"), // 读取配置
                exception = e
            )
        }
    }

    /**
     * 用本地存档覆盖游戏存档
     */
    fun coverGameSaveWithLocalSave(
        localSaveDir: File,
        gameSaveDir: File
    ): PvzSaveOperationResult {
        val saveConfig = InitializePvz2.config.ui.save // 获取配置
        return try {
            if (!localSaveDir.exists() || !localSaveDir.isDirectory || localSaveDir.list()?.isEmpty() ?: true) {
                return PvzSaveOperationResult(
                    type = PvzSaveOperationType.COVER,
                    isSuccess = false,
                    message = "本地存档目录不存在或不是有效目录，或为空目录"
                )
            }

            // 清空目标目录
            if (gameSaveDir.exists()) {
                gameSaveDir.deleteRecursively()
            }

            localSaveDir.copyRecursively(gameSaveDir,true)

            notifyGameSaveChanged()

            PvzSaveOperationResult(
                type = PvzSaveOperationType.COVER,
                isSuccess = true,
                message = saveConfig.coverSuccessTip // 读取配置
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PvzSaveOperationResult(
                type = PvzSaveOperationType.COVER,
                isSuccess = false,
                message = String.format(saveConfig.coverFailTipPrefix, e.message ?: "未知错误"), // 读取配置
                exception = e
            )
        }
    }

    /**
     * 删除当前游戏游玩存档（清空游戏存档目录内所有内容，保留目录本身避免游戏崩溃）
     */
    fun deleteCurrentGameSave(
        gameSaveDir: File
    ): PvzSaveOperationResult {
        val saveConfig = InitializePvz2.config.ui.save // 复用你的配置读取逻辑
        return try {
            // 前置强校验，避免误删
            if (!gameSaveDir.exists() || !gameSaveDir.isDirectory) {
                return PvzSaveOperationResult(
                    type = PvzSaveOperationType.DELETE_GAME_SAVE,
                    isSuccess = false,
                    message = "游戏存档目录不存在或不是有效目录，无法删除"
                )
            }

            // 复用你覆盖存档时的清空逻辑，彻底删除目录内所有内容，保留父目录
            gameSaveDir.deleteRecursively()

            notifyGameSaveChanged()

            PvzSaveOperationResult(
                type = PvzSaveOperationType.DELETE_GAME_SAVE,
                isSuccess = true,
                message = saveConfig.deleteGameSaveSuccessTip
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PvzSaveOperationResult(
                type = PvzSaveOperationType.DELETE_GAME_SAVE,
                isSuccess = false,
                message = String.format(saveConfig.deleteGameSaveFailTipPrefix, e.message ?: "未知错误"),
                exception = e
            )
        }
    }

    /**
     * 导出存档到DocumentFile（适配SDK21+，异步）
     */
    fun exportGameSaveToDocumentFile(
        context: Context,
        sourceDir: File,
        targetDoc: DocumentFile,
        onResult: (PvzSaveOperationResult) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val result = try {
                if (!sourceDir.exists() || !sourceDir.isDirectory || sourceDir.list()?.isEmpty() ?: true) {
                    PvzSaveOperationResult(
                        type = PvzSaveOperationType.EXPORT,
                        isSuccess = false,
                        message = "游戏存档目录不存在或不是有效目录，或为空目录"
                    )
                } else {
                    copyFileToDocumentFile(context, sourceDir, targetDoc)
                    PvzSaveOperationResult(
                        type = PvzSaveOperationType.EXPORT,
                        isSuccess = true,
                        message = "存档导出成功"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PvzSaveOperationResult(
                    type = PvzSaveOperationType.EXPORT,
                    isSuccess = false,
                    message = "导出失败：${e.message ?: "未知错误"}",
                    exception = e
                )
            }

            launch(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    private const val TARGET_SAVE_FILE = "pp.dat" // 目标存档文件名

    // ======================== 新增：批量搜索结果封装类 ========================
    /**
     * 封装找到的有效存档信息
     */
    sealed class ValidSaveSource {
        data class DocumentFileSource(val doc: DocumentFile, val tempExtractDir: File? = null) : ValidSaveSource()
        data class FileSource(val file: File, val tempExtractDir: File? = null) : ValidSaveSource()

        val dirName: String
            get() = when (this) {
                is DocumentFileSource -> doc.name ?: "unknown"
                is FileSource -> file.name
            }
    }

    // ======================== 新增：批量搜索所有有效存档 ========================
    /**
     * 智能搜索所有含 pp.dat 的有效存档（含压缩包解压）
     * @return 找到的有效存档列表 + 需要清理的临时解压目录列表
     */
    suspend fun findAllValidSaves(
        context: Context,
        sourceDoc: DocumentFile
    ): Pair<List<ValidSaveSource>, List<File>> = withContext(Dispatchers.IO) {
        val validSaves = mutableListOf<ValidSaveSource>()
        val tempDirsToClean = mutableListOf<File>()

        // 1. 搜索当前目录树中的所有有效存档
        validSaves.addAll(findAllValidSaveDirsInDocumentFile(context, sourceDoc))

        // 2. 搜索并解压所有压缩包，收集有效存档
        val archiveResults = findAndExtractAllValidSavesFromArchives(context, sourceDoc, sourceDoc.name ?: "archive")
        validSaves.addAll(archiveResults.first)
        tempDirsToClean.addAll(archiveResults.second)

        return@withContext validSaves to tempDirsToClean
    }

    /**
     * 在 DocumentFile 树中递归搜索所有含 pp.dat 的目录
     */
    private fun findAllValidSaveDirsInDocumentFile(
        context: Context,
        rootDoc: DocumentFile
    ): List<ValidSaveSource.DocumentFileSource> {
        val results = mutableListOf<ValidSaveSource.DocumentFileSource>()

        if (rootDoc.isDirectory) {
            val hasTargetFile = rootDoc.listFiles().any { it.isFile && it.name == TARGET_SAVE_FILE }
            if (hasTargetFile) {
                results.add(ValidSaveSource.DocumentFileSource(rootDoc))
            }

            // 递归搜索子目录
            rootDoc.listFiles().forEach { child ->
                if (child.isDirectory) {
                    results.addAll(findAllValidSaveDirsInDocumentFile(context, child))
                }
            }
        }
        return results
    }

    /**
     * 搜索所有压缩包并解压查找有效存档
     * @return (有效存档列表, 需要清理的临时目录列表)
     */
    /**
     * 搜索所有压缩包并解压查找有效存档
     * @return (有效存档列表, 需要清理的临时目录列表)
     */
    private fun findAndExtractAllValidSavesFromArchives(
        context: Context,
        rootDoc: DocumentFile,
        tempDirSuffix: String,
    ): Pair<List<ValidSaveSource.FileSource>, List<File>> {
        val validSaves = mutableListOf<ValidSaveSource.FileSource>()
        val tempDirs = mutableListOf<File>()

        rootDoc.listFiles().forEach { doc ->
            // 同时匹配 .zip 和 .pvz2saves 后缀
            val isArchive = doc.isFile && (
                    doc.name?.endsWith(".zip", ignoreCase = true) == true ||
                            doc.name?.endsWith(SHARE_FILE_EXTENSION, ignoreCase = true) == true
                    )

            if (isArchive) {
                val tempExtractDir = File(
                    context.cacheDir,
                    "temp_extract_${tempDirSuffix}_${System.currentTimeMillis()}"
                )
                if (!tempExtractDir.exists()) tempExtractDir.mkdirs()
                tempDirs.add(tempExtractDir)

                try {
                    extractZip(context, doc.uri, tempExtractDir)
                    val foundDirs = findAllValidSaveDirsInFile(tempExtractDir)
                    validSaves.addAll(
                        foundDirs.map { ValidSaveSource.FileSource(it, tempExtractDir) }
                    )

                    // 新增：递归搜索解压后的目录里的 .pvz2saves/.zip
                    val nestedDoc = DocumentFile.fromFile(tempExtractDir)
                    val nestedResults = findAndExtractAllValidSavesFromArchives(
                        context, nestedDoc, "nested_${tempDirSuffix}"
                    )
                    validSaves.addAll(nestedResults.first)
                    tempDirs.addAll(nestedResults.second)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 递归搜索子目录中的压缩包
            if (doc.isDirectory) {
                val childResults = findAndExtractAllValidSavesFromArchives(
                    context, doc, doc.name ?: "sub_archive"
                )
                validSaves.addAll(childResults.first)
                tempDirs.addAll(childResults.second)
            }
        }
        return validSaves to tempDirs
    }

    /**
     * 在 File 树中递归搜索所有含 pp.dat 的目录
     */
    private fun findAllValidSaveDirsInFile(rootDir: File): List<File> {
        val results = mutableListOf<File>()

        if (rootDir.isDirectory) {
            val hasTargetFile = rootDir.listFiles()?.any { it.isFile && it.name == TARGET_SAVE_FILE } == true
            if (hasTargetFile) {
                results.add(rootDir)
            }

            rootDir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    results.addAll(findAllValidSaveDirsInFile(child))
                }
            }
        }
        return results
    }

    // ======================== 新增：单次导入封装（供UI层循环调用） ========================
    /**
     * 单次导入单个有效存档到指定目录
     */
    fun importSingleSave(
        context: Context,
        source: ValidSaveSource,
        targetDir: File,
        onResult: (PvzSaveOperationResult) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val result = try {
                if (!targetDir.exists()) targetDir.mkdirs()

                when (source) {
                    is ValidSaveSource.DocumentFileSource -> {
                        copyDocumentFileContentToLocal(context, source.doc, targetDir)
                    }
                    is ValidSaveSource.FileSource -> {
                        source.file.copyRecursively(targetDir, overwrite = true)
                    }
                }

                PvzLocalSaveManager.triggerRefresh()

                PvzSaveOperationResult(
                    type = PvzSaveOperationType.IMPORT,
                    isSuccess = true,
                    message = "存档导入成功"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                if (targetDir.exists()) {
                    runCatching { targetDir.deleteRecursively() }
                }
                PvzSaveOperationResult(
                    type = PvzSaveOperationType.IMPORT,
                    isSuccess = false,
                    message = "导入失败：${e.message ?: "未知错误"}",
                    exception = e
                )
            }

            launch(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    /**
     * 批量导入配置回调：每个存档需要配置时触发
     * @param defaultName 自动生成的默认存档名
     * @param defaultDesc 默认描述
     * @param importSuccessTip 导入成功的提示语（用于显示在配置弹窗）
     * @return 返回用户输入的 Pair(name, desc)，返回 null 表示取消该存档导入
     */
    typealias OnSaveConfigCallback = suspend (
        defaultName: String,
        defaultDesc: String,
        importSuccessTip: String? // 新增：导入成功提示
    ) -> Pair<String, String>?

    /**
     * 智能批量导入所有存档（极简入口，所有复杂逻辑内聚）
     * @param context 上下文
     * @param sourceDoc 用户选择的目录 DocumentFile
     * @param defaultImportDesc 存档默认描述
     * @param defaultImportNamePrefix 存档名前缀
     * @param onEachSaveConfig 每个存档需要配置时的回调（弹弹窗用）
     * @param onSingleResult 单个存档导入结果回调（更新状态用）
     * @param onFinalResult 全部导入完成后的最终结果回调
     */
    fun batchImportAllSaves(
        context: Context,
        sourceDoc: DocumentFile,
        defaultImportDesc: String,
        defaultImportNamePrefix: String,
        onEachSaveConfig: OnSaveConfigCallback,
        onSingleResult: (PvzSaveOperationResult) -> Unit,
        onWaitForResultDismiss: OnWaitForResultDismiss, // 新增：等待弹窗关闭的回调
        onFinalResult: (PvzSaveOperationResult) -> Unit
    ) {
        scope.launch {
            val tempDirsToClean = mutableListOf<File>()
            var successCount = 0
            var totalCount = 0

            try {
                val (validSaves, tempDirs) = findAllValidSaves(context, sourceDoc)
                tempDirsToClean.addAll(tempDirs)
                totalCount = validSaves.size

                if (totalCount == 0) {
                    val emptyResult = PvzSaveOperationResult(
                        type = PvzSaveOperationType.IMPORT,
                        isSuccess = false,
                        message = "未找到任何包含 $TARGET_SAVE_FILE 的有效存档"
                    )
                    withContext(Dispatchers.Main) {
                        onSingleResult(emptyResult)
                        onFinalResult(emptyResult)
                    }
                    return@launch
                }

                val pendingList = validSaves.map { source ->
                    val newId = PvzLocalSaveManager.generateSaveId()
                    val targetDir = File(PvzLocalSaveManager.getLocalSaveRootDir(context), newId)
                    val defaultName = if (totalCount == 1) {
                        "${defaultImportNamePrefix}_${System.currentTimeMillis()}"
                    } else {
                        "${defaultImportNamePrefix}_${source.dirName}"
                    }
                    Triple(source, newId, targetDir) to defaultName
                }

                pendingList.forEachIndexed { index, (pendingData, defaultName) ->
                    val (source, newId, targetDir) = pendingData
                    val currentIndex = index + 1
                    val successTip = "存档 $currentIndex/$totalCount 是否导入"

                    // 1. 执行导入
                    val importResult = suspendCancellableCoroutine { cont ->
                        importSingleSave(context, source, targetDir) { result ->
                            if (cont.isActive) {
                                cont.resume(result)
                            }
                        }
                        cont.invokeOnCancellation {
                            runCatching { targetDir.deleteRecursively() }
                        }
                    }

                    if (!importResult.isSuccess) {
                        // 导入失败：弹操作结果弹窗，等待用户关闭
                        val failResult = importResult.copy(
                            message = "存档 $currentIndex/$totalCount 导入失败：${importResult.message}"
                        )
                        withContext(Dispatchers.Main) {
                            onSingleResult(failResult)
                            onWaitForResultDismiss() // 等待用户关闭弹窗
                        }
                        return@forEachIndexed
                    }

                    // 2. 导入成功：不弹操作结果弹窗，直接弹配置弹窗，传入成功提示
                    val userConfig = withContext(Dispatchers.Main) {
                        onEachSaveConfig(defaultName, defaultImportDesc, successTip)
                    }

                    if (userConfig == null) {
                        // 用户取消：删除已导入文件
                        runCatching { targetDir.deleteRecursively() }
                        return@forEachIndexed
                    }

                    // 3. 保存元信息
                    val (saveName, saveDesc) = userConfig
                    val saveEntity = PvzLocalSaveEntity(
                        id = newId,
                        name = saveName,
                        desc = saveDesc,
                        savePath = targetDir.absolutePath,
                        createTime = System.currentTimeMillis().toString()
                    )
                    PvzLocalSaveManager.saveLocalSaveMeta(context, saveEntity)
                    successCount++
                }

                // 4. 全部处理完成，弹最终结果
                val finalResult = PvzSaveOperationResult(
                    type = PvzSaveOperationType.IMPORT,
                    isSuccess = successCount > 0,
                    message = "批量导入完成：成功 $successCount 个，失败 ${totalCount - successCount} 个"
                )
                withContext(Dispatchers.Main) { onFinalResult(finalResult) }

            } catch (e: Exception) {
                e.printStackTrace()
                val errorResult = PvzSaveOperationResult(
                    type = PvzSaveOperationType.IMPORT,
                    isSuccess = false,
                    message = "批量导入失败：${e.message ?: "未知错误"}",
                    exception = e
                )
                withContext(Dispatchers.Main) {
                    onSingleResult(errorResult)
                    onFinalResult(errorResult)
                }
            } finally {
                withContext(Dispatchers.IO) {
                    tempDirsToClean.forEach { dir ->
                        runCatching { dir.deleteRecursively() }
                    }
                }
                PvzLocalSaveManager.triggerRefresh()
            }
        }
    }

    // ======================== 辅助方法：解压与复制 ========================

    /**
     * 解压 Zip 文件
     */
    @Throws(IOException::class)
    private fun extractZip(context: Context, zipUri: Uri, targetDir: File) {
        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    val targetFile = File(targetDir, entry.name)

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { output ->
                            zipInputStream.copyTo(output)
                        }
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        } ?: throw IOException("无法打开压缩包文件")
    }

    /**
     * 复制 DocumentFile 目录的内容（不包含目录本身）
     */
    private fun copyDocumentFileContentToLocal(
        context: Context,
        sourceDoc: DocumentFile,
        targetDir: File
    ) {
        sourceDoc.listFiles().forEach { childDoc ->
            copyDocumentFileToLocal(context, childDoc, targetDir)
        }
    }

    const val SHARE_FILE_EXTENSION = ".pvz2saves" // 特殊后缀
    const val SHARE_FILE_MIME_TYPE = "application/x-pvz2saves" // 自定义 MIME 类型

    // ======================== 新增：打包所有本地存档为特殊后缀文件 ========================
    /**
     * 打包所有本地存档为 .pvz2saves 文件
     * @param context 上下文
     * @param localSaves 本地存档列表（从 PvzLocalSaveManager.getAllLocalSaves 获取）
     * @return 打包后的文件 Uri（用于分享）
     */
    suspend fun packAllLocalSavesForShare(
        context: Context,
        localSaves: List<PvzLocalSaveEntity>
    ): Uri? = withContext(Dispatchers.IO) {
        if (localSaves.isEmpty()) return@withContext null

        // 1. 创建临时打包文件（先写 .zip，最后改后缀）
        val tempZipFile = File(context.cacheDir, "all_saves_${System.currentTimeMillis()}.zip")
        val shareFile = File(context.cacheDir, "all_saves_${System.currentTimeMillis()}$SHARE_FILE_EXTENSION")

        try {
            // 2. 用 ZipOutputStream 打包所有存档
            FileOutputStream(tempZipFile).use { fileOut ->
                ZipOutputStream(fileOut).use { zipOut ->
                    localSaves.forEach { save ->
                        val saveDir = File(save.savePath)
                        if (saveDir.exists() && saveDir.isDirectory) {
                            // 把每个存档目录作为 Zip 的子目录
                            addDirToZip(saveDir, save.id, zipOut)
                        }
                    }
                    zipOut.finish()
                }
            }

            // 3. 重命名为特殊后缀
            if (tempZipFile.renameTo(shareFile)) {
                // 4. 用 FileProvider 获取 Uri（Android 7.0+ 必须）
                // 注意：需要在 res/xml/file_paths.xml 中配置缓存目录
                val authority = "${context.packageName}.fileprovider"
                androidx.core.content.FileProvider.getUriForFile(context, authority, shareFile)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 清理临时文件
            tempZipFile.delete()
            shareFile.delete()
            null
        }
    }

    /**
     * 递归添加目录到 Zip
     */
    @Throws(IOException::class)
    private fun addDirToZip(sourceDir: File, zipEntryPath: String, zipOut: ZipOutputStream) {
        sourceDir.listFiles()?.forEach { file ->
            val entryPath = "$zipEntryPath/${file.name}"
            if (file.isDirectory) {
                // 目录：创建 ZipEntry，递归添加子文件
                zipOut.putNextEntry(ZipEntry("$entryPath/"))
                zipOut.closeEntry()
                addDirToZip(file, entryPath, zipOut)
            } else {
                // 文件：写入 Zip
                FileInputStream(file).use { fileIn ->
                    BufferedInputStream(fileIn).use { bufferedIn ->
                        zipOut.putNextEntry(ZipEntry(entryPath))
                        bufferedIn.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }

    // ======================== 新增：从 Uri 导入分享的 .pvz2saves 文件 ========================
    /**
     * 导入分享/打开的 .pvz2saves 文件
     * @param context 上下文
     * @param fileUri 文件 Uri（从 Intent 获取）
     * @param defaultImportDesc 存档默认描述
     * @param defaultImportNamePrefix 存档名前缀
     * @param onEachSaveConfig 每个存档需要配置时的回调
     * @param onSingleResult 单个存档导入结果回调
     * @param onFinalResult 全部导入完成后的最终结果回调
     */
    fun importSharedSaveFile(
        context: Context,
        fileUri: Uri,
        defaultImportDesc: String,
        defaultImportNamePrefix: String,
        onEachSaveConfig: OnSaveConfigCallback,
        onSingleResult: (PvzSaveOperationResult) -> Unit,
        onWaitForResultDismiss: OnWaitForResultDismiss,
        onFinalResult: (PvzSaveOperationResult) -> Unit
    ) {
        scope.launch {
            val tempDirsToClean = mutableListOf<File>()

            try {
                // 1. 把 Uri 对应的文件复制到缓存目录（处理 ContentResolver 权限问题）
                val tempShareFile = File(context.cacheDir, "shared_saves_${System.currentTimeMillis()}$SHARE_FILE_EXTENSION")
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    FileOutputStream(tempShareFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("无法打开分享的文件")
                tempDirsToClean.add(tempShareFile)

                // 2. 解压文件到临时目录
                val tempExtractDir = File(context.cacheDir, "extract_shared_${System.currentTimeMillis()}")
                if (!tempExtractDir.exists()) tempExtractDir.mkdirs()
                tempDirsToClean.add(tempExtractDir)

                // 3. 复用你已有的解压逻辑（把 .pvz2saves 当 .zip 处理）
                extractZip(context, Uri.fromFile(tempShareFile), tempExtractDir)

                // 4. 把解压后的目录包装成 DocumentFile，复用你已有的批量导入逻辑
                val sourceDoc = DocumentFile.fromFile(tempExtractDir)

                // 5. 直接调用你已有的 batchImportAllSaves 逻辑（稍微调整一下，复用内部逻辑）
                // 这里为了不重复代码，我们直接把 sourceDoc 传给 batchImportAllSaves
                batchImportAllSaves(
                    context = context,
                    sourceDoc = sourceDoc,
                    defaultImportDesc = defaultImportDesc,
                    defaultImportNamePrefix = defaultImportNamePrefix,
                    onEachSaveConfig = onEachSaveConfig,
                    onSingleResult = onSingleResult,
                    onWaitForResultDismiss = onWaitForResultDismiss,
                    onFinalResult = onFinalResult
                )

            } catch (e: Exception) {
                // 全局异常处理
                e.printStackTrace()
                val errorResult = PvzSaveOperationResult(
                    type = PvzSaveOperationType.IMPORT,
                    isSuccess = false,
                    message = "导入分享文件失败：${e.message ?: "未知错误"}",
                    exception = e
                )
                withContext(Dispatchers.Main) {
                    onSingleResult(errorResult)
                    onFinalResult(errorResult)
                }
            }
            // 清理临时文件
            tempDirsToClean.forEach { dir ->
                runCatching { dir.deleteRecursively() }
            }
        }
    }

    // ======================== 核心修复：新增递归复制目录方法 ========================

    /**
     * 递归复制本地File到DocumentFile（适配SDK21+）
     */
    private fun copyFileToDocumentFile(
        context: Context,
        sourceFile: File,
        targetDoc: DocumentFile
    ) {
        if (sourceFile.isDirectory) {
            // 创建子目录（SDK21兼容）
            val childDoc = targetDoc.findFile(sourceFile.name) ?: targetDoc.createDirectory(sourceFile.name)
            ?: throw IOException("无法创建目录：${sourceFile.name}")

            sourceFile.listFiles()?.forEach { childFile ->
                copyFileToDocumentFile(context, childFile, childDoc)
            }
        } else if (sourceFile.isFile) {
            // 创建文件（适配SDK21的MIME类型）
            val mimeType = context.contentResolver.getType(Uri.fromFile(sourceFile)) ?: "*/*"
            val targetFileDoc = targetDoc.findFile(sourceFile.name) ?: targetDoc.createFile(mimeType, sourceFile.name)
            ?: throw IOException("无法创建文件：${sourceFile.name}")

            // 复制文件内容
            context.contentResolver.openOutputStream(targetFileDoc.uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    copyStream(input, output)
                }
            } ?: throw IOException("无法打开输出流：${sourceFile.name}")
        }
    }

    /**
     * 递归复制DocumentFile到本地File（适配SDK21+）
     */
    private fun copyDocumentFileToLocal(
        context: Context,
        sourceDoc: DocumentFile,
        targetDir: File
    ) {
        if (sourceDoc.isDirectory) {
            val localDir = File(targetDir, sourceDoc.name ?: "")
            if (!localDir.exists()) localDir.mkdirs()

            sourceDoc.listFiles().forEach { childDoc ->
                copyDocumentFileToLocal(context, childDoc, localDir)
            }
        } else if (sourceDoc.isFile) {
            val localFile = File(targetDir, sourceDoc.name ?: "")
            context.contentResolver.openInputStream(sourceDoc.uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    copyStream(input, output)
                }
            } ?: throw IOException("无法打开输入流：${sourceDoc.name}")
        }
    }

    /**
     * 通用流复制方法（优化性能）
     */
    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024 * 8) // 增大缓冲区提升速度
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }
        output.flush()
    }

    /**
     * 检查应用是否注册了所需的 FileProvider
     * @param context 上下文
     * @return 是否注册了正确的 FileProvider
     */
    fun isFileProviderRegistered(context: Context): Boolean {
        val authority = "${context.packageName}.fileprovider"
        return try {
            // 通过 PackageManager 查询是否有该 authority 的 Provider
            context.packageManager.resolveContentProvider(authority, 0) != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ======================== 游戏存档版本隔离功能 ========================

    private const val TARGET_SAVE_FILE_NAME = "pp.dat"

    /**
     * 检查版本存档目录是否存在有效的游戏存档（pp.dat）
     * @param versionSaveDir 版本存档目录
     * @return 是否存在有效的存档
     */
    fun hasVersionSave(versionSaveDir: File): Boolean {
        if (!versionSaveDir.exists() || !versionSaveDir.isDirectory) return false
        val ppDatFile = File(versionSaveDir, TARGET_SAVE_FILE_NAME)
        return ppDatFile.exists() && ppDatFile.length() > 0
    }

    /**
     * 备份当前游戏存档到指定版本目录
     * @param sourceDir 当前游戏存档目录（通常是 saves/）
     * @param targetVersionSaveDir 目标版本存档目录
     * @return 是否备份成功
     */
    fun backupGameSaveToVersionDir(sourceDir: File, targetVersionSaveDir: File): Boolean {
        return try {
            // 检查源目录是否有有效存档
            if (!hasVersionSave(sourceDir)) {
                Log.d("VersionSaveBackup", "源目录无有效存档，跳过备份")
                return true
            }

            // 确保目标目录存在
            if (targetVersionSaveDir.exists()) targetVersionSaveDir.deleteRecursively()

            sourceDir.copyRecursively(targetVersionSaveDir, true)
            Log.d("VersionSaveBackup", "存档备份到: ${targetVersionSaveDir.absolutePath}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VersionSaveBackup", "备份失败: ${e.message}")
            false
        }
    }

    /**
     * 从指定版本目录恢复存档到目标目录
     * @param sourceVersionSaveDir 源版本存档目录
     * @param targetDir 目标游戏存档目录
     * @return 是否恢复成功
     */
    fun restoreGameSaveFromVersionDir(sourceVersionSaveDir: File, targetDir: File): Boolean {
        return try {
            // 检查版本存档是否存在
            if (!hasVersionSave(sourceVersionSaveDir)) {
                Log.d("VersionSaveRestore", "版本目录无存档，无需恢复")
                return false
            }

            // 清空目标目录
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }

            sourceVersionSaveDir.copyRecursively(targetDir, true)
            Log.d("VersionSaveRestore", "从 ${sourceVersionSaveDir.absolutePath} 恢复到 ${targetDir.absolutePath}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VersionSaveRestore", "恢复失败: ${e.message}")
            false
        }
    }

    /**
     * 切换版本时执行存档隔离逻辑
     * @param currentGameSaveDir 当前游戏存档目录（saves/）
     * @param currentVersion 当前版本（用于备份当前位置）
     * @param newVersion 新选定的版本（用于恢复新版本的存档）
     * @param targetSmfDir 目标 SMF 目录（需要清理旧版本文件）
     * @return Pair(是否需要切换, 新存档目录是否有存档)
     */
    fun switchVersionSaveIsolation(
        currentGameSaveDir: File,
        currentVersion: VersionDef?,
        newVersion: VersionDef,
        targetSmfDir: File
    ): Pair<Boolean, Boolean> {
        if (currentVersion == null) {
            // 首次切换或同版本，无需处理
            return Pair(false, hasVersionSave(newVersion.resolveGameSaveDirectory()))
        }

        val oldVersionSaveDir = currentVersion.resolveGameSaveDirectory()
        val newVersionSaveDir = newVersion.resolveGameSaveDirectory()

        // 1. 备份当前存档到旧版本目录
        val backupSuccess = backupGameSaveToVersionDir(currentGameSaveDir, oldVersionSaveDir)

        // 2. 检查新版本是否有存档
        val hasNewVersionSave = hasVersionSave(newVersionSaveDir)

        // 3. 如果新版本有存档，恢复它；如果没有，清空当前存档
        if (hasNewVersionSave) {
            restoreGameSaveFromVersionDir(newVersionSaveDir, currentGameSaveDir)
            newVersionSaveDir.deleteRecursively()
        } else {
            currentGameSaveDir.deleteRecursively()
            Log.d("VersionSaveSwitch", "新版本无存档，已清空当前游戏存档")
        }

        // 4. 清理目标 SMF 目录中不属于新版本的文件
        cleanTargetSmfDirForNewVersion(newVersion, targetSmfDir)

        // 5. 通知存档变化
        notifyGameSaveChanged()

        return Pair(backupSuccess, hasNewVersionSave)
    }

    /**
     * 清理目标 SMF 目录中不属于新版本的文件
     * 根据新版本的 assets 资源目录内容，删除目标目录中不存在的文件
     * @param newVersion 新版本
     * @param targetSmfDir 目标 SMF 目录
     */
    fun cleanTargetSmfDirForNewVersion(newVersion: VersionDef, targetSmfDir: File) {
        try {
            // 获取新版本在 assets 中的资源路径
            val versionSmfAssetPath = newVersion.resolveAssetPath()

            // 使用 AssetExtractorHolder.listResources 获取文件列表（本地优先）
            val newVersionFileNames = AssetExtractorHolder.listResources(versionSmfAssetPath)
                .map { it.substringAfterLast("/") }  // 只保留文件名
                .toSet()

            // 清理目标目录中不属于新版本的文件
            cleanOrphanedFilesByName(targetSmfDir, newVersionFileNames)

            Log.d("VersionSmfClean", "已清理目标 SMF 目录，新版本包含 ${newVersionFileNames.size} 个文件")
        } catch (e: Exception) {
            Log.e("VersionSmfClean", "清理 SMF 目录失败: ${e.message}")
        }
    }

    /**
     * 清理孤立文件：删除目标目录中存在但不在新版本文件列表中的文件
     * @param targetDir 目标目录
     * @param newVersionFileNames 新版本的文件名集合
     */
    private fun cleanOrphanedFilesByName(targetDir: File, newVersionFileNames: Set<String>) {
        if (!targetDir.exists()) return

        // 目标目录的文件名集合
        val targetFileNames = targetDir.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()

        // 找出需要删除的文件（目标目录有但新版本没有的）
        val orphanedFiles = targetFileNames.filter { fileName ->
            newVersionFileNames.contains(fileName)
        }

        if (orphanedFiles.isEmpty()) {
            Log.d("CleanOrphan", "没有需要清理的孤立文件")
            return
        }

        // 删除孤立文件
        orphanedFiles.forEach { fileName ->
            val file = File(targetDir, fileName)
            if (file.exists()) {
                Log.d("CleanOrphan", "删除孤立文件: ${file.absolutePath}")
                file.delete()
            }
        }

        // 清理空目录
        targetDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            cleanEmptyDirectory(subDir)
        }
    }

    /**
     * 递归清理空目录
     */
    private fun cleanEmptyDirectory(dir: File) {
        if (!dir.exists()) return

        // 先递归处理子目录
        dir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            cleanEmptyDirectory(subDir)
        }

        // 如果目录为空，删除它
        if (dir.listFiles()?.isEmpty() == true) {
            Log.d("CleanDir", "删除空目录: ${dir.absolutePath}")
            dir.delete()
        }
    }

}