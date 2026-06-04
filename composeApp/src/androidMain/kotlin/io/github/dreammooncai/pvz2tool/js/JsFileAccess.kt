package io.github.dreammooncai.pvz2tool.js

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.js.JsFileResolver.ActiveJsContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val cacheDir: File
    get() = File(InitializePvz2.context.cacheDir, "js_file_access").apply { mkdirs() }

/**
 * JS 文件访问统一封装。
 *
 * 处理以下场景：
 * 1. 工作目录未选择 → 使用 asset 作为备选
 * 2. 输入文件：DocumentFile → 尝试转 File；如果是 asset/无权限 → 复制到缓存 → 使用缓存 File
 * 3. 输出文件：DocumentFile 无写入权限 → 先输出到缓存 → 再复制回 DocumentFile
 */
open class JsFileAccess(
    val resolver: JsFileResolver
) {

    constructor(section: DynamicSection?,
                item: SectionItem?,
                version: VersionDef): this(JsFileResolver(ActiveJsContext(section, item, version)))

    companion object : JsFileAccess(JsFileResolver) {

        /**
         * 将 DocumentFile 复制到缓存目录
         */
        private fun copyToCache(docFile: DocumentFile, context: Context): InputFile? {
            return try {
                val cacheFile = File(cacheDir, "${UUID.randomUUID()}_${docFile.name ?: "temp"}")
                val data = JsFileResolver.readFromDocumentFile(docFile, context)
                cacheFile.writeBytes(data)
                InputFile(cacheFile, isCache = true, sourceDoc = docFile)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 清理所有缓存文件
         */
        fun clearCache() {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * 输入文件结果：包含可读取的 File 路径，以及是否需要清理的标记
     */
    data class InputFile(
        val file: File,
        /** 是否是缓存文件（需要在使用后清理） */
        val isCache: Boolean,
        /** 原始 DocumentFile（如果来源是 DocumentFile） */
        val sourceDoc: DocumentFile? = null
    )

    /**
     * 输出文件句柄：用于写入操作
     */
    class OutputHandle(
        private val context: Context,
        /** 实际写入的目标 File（可能是缓存） */
        val targetFile: File,
        /** 最终的 DocumentFile（写入完成后需要复制回去） */
        private val finalDoc: DocumentFile?,
        /** 是否是临时缓存 */
        private val isCache: Boolean
    ) {
        /**
         * 写入完成后调用，将缓存文件复制回最终目标（如果需要）
         */
        fun commit() {
            if (isCache && finalDoc != null) {
                // 将缓存文件复制回 DocumentFile
                val data = targetFile.readBytes()
                JsFileResolver.writeToDocumentFile(finalDoc, data, context)
                // 清理缓存
                targetFile.delete()
            }
        }

        /**
         * 取消操作，清理缓存文件
         */
        fun cancel() {
            if (isCache) {
                targetFile.delete()
            }
        }
    }

    /**
     * 解析输出文件路径，返回可写入的 OutputHandle。
     *
     * 处理流程：
     * 1. 绝对路径 → 直接使用 File
     * 2. 占位符路径 → DocumentFile → 尝试转换
     * 3. 无法转换 → 返回缓存 File 句柄，写入完成后自动复制回 DocumentFile
     */
    fun resolveOutput(placeholderPath: String, context: Context): OutputHandle? {
        // 绝对路径：直接使用 File
        if (placeholderPath.startsWith("/")) {
            val file = File(placeholderPath)
            // 确保父目录存在
            file.parentFile?.mkdirs()
            return OutputHandle(context, file, null, isCache = false)
        }

        // 1. 解析为 DocumentFile
        val docFile = resolver.resolve(placeholderPath, context)
            ?: return null // 输出不支持 asset

        // 2. 尝试转换为普通 File
        val file = JsFileResolver.documentFileToFile(docFile)
        if (file != null && file.canWrite()) {
            return OutputHandle(context, file, null, isCache = false)
        }

        // 3. 无法直接写入，使用缓存文件
        val cacheFile = File(cacheDir, "${UUID.randomUUID()}_${docFile.name ?: "temp"}")
        return OutputHandle(context, cacheFile, docFile, isCache = true)
    }

    /**
     * 解析输出文件路径，返回可写入的 OutputHandle。
     *
     * 处理流程：
     * 1. 绝对路径 → 直接使用 File
     * 2. 占位符路径 → DocumentFile → 尝试转换
     * 3. 无法转换 → 返回缓存 File 句柄，写入完成后自动复制回 DocumentFile
     */
    fun resolveOutputOrThrow(placeholderPath: String, context: Context): OutputHandle = resolveOutput(placeholderPath, context) ?: throw IllegalArgumentException("无法解析输出路径: $placeholderPath")

    /**
     * 解析输入文件路径，返回可读取的 File。
     *
     * 处理流程：
     * 1. 绝对路径 → 直接使用 File
     * 2. 占位符路径 → DocumentFile → 尝试转换
     * 3. 无法转换 → 复制到缓存目录 → 返回缓存 File
     */
    fun resolveInput(placeholderPath: String, context: Context): InputFile? {
        // 绝对路径：直接使用 File
        if (placeholderPath.startsWith("/")) {
            val file = File(placeholderPath)
            if (file.exists() && file.canRead()) {
                return InputFile(file, isCache = false)
            }
            return null
        }

        // 1. 解析为 DocumentFile
        val docFile = resolver.resolve(placeholderPath, context) ?: run {
            // 如果工作目录未选择，尝试从 asset 读取
            return resolveFromAsset(placeholderPath, context)
        }

        // 2. 尝试转换为普通 File
        val file = JsFileResolver.documentFileToFile(docFile)
        if (file != null && file.canRead()) {
            return InputFile(file, isCache = false, sourceDoc = docFile)
        }

        // 3. 无法直接访问，复制到缓存
        return copyToCache(docFile, context)
    }

    /**
     * 解析输入文件路径，返回可读取的 File。
     *
     * 处理流程：
     * 1. 绝对路径 → 直接使用 File
     * 2. 占位符路径 → DocumentFile → 尝试转换
     * 3. 无法转换 → 复制到缓存目录 → 返回缓存 File
     */
    fun resolveInputOrThrow(placeholderPath: String, context: Context): InputFile = resolveInput(placeholderPath,context) ?: throw IllegalArgumentException(
        if (placeholderPath.startsWith("\$") && !placeholderPath.startsWith("/"))
            "无法解析路径 \"$placeholderPath\"：请检查工作目录是否已选择，或目标文件是否存在"
        else
            "无法解析输入路径: $placeholderPath（文件不存在或无读取权限）"
    )

    /**
     * 从 asset 解析文件（当工作目录未选择时使用）
     *
     * **$SMF 降级规则**：
     * 1. 优先查找 primaryPath（版本专属目录）
     * 2. 若不存在，回退到 fallbackPath（baseAssetPath）
     *
     * **$ITEM 降级规则**：
     * 1. 优先查找 item assetPath（栏目专属目录）
     * 2. 若不存在，降级到 $SMF（版本目录）
     */
    private fun resolveFromAsset(placeholderPath: String, context: Context): InputFile? {
        val activeCtx = resolver.jsContext ?: return null
        val version = activeCtx.version

        val basePlaceholder = when {
            placeholderPath.startsWith(JsFileResolver.JS_DIR) -> JsFileResolver.JS_DIR
            placeholderPath.startsWith(JsFileResolver.ITEM) -> JsFileResolver.ITEM
            placeholderPath.startsWith(JsFileResolver.SMF) -> JsFileResolver.SMF
            else -> return null
        }

        // 判断是 $ITEM 还是 $SMF，并获取基础路径
        val (primaryPath, fallbackPath, fallbackRootPath) = when {
            placeholderPath.startsWith(JsFileResolver.JS_DIR) -> {
                // $ITEM：优先 item assetPath，降级到 version assetPath
                val section = activeCtx.section
                val item = activeCtx.item
                if (section != null && item != null) {
                    val itemPath = item.resolveJsPath(section, version).substringBeforeLast('/')
                    Triple(itemPath, section.resolveJsPath(version).substringBeforeLast('/'), version.resolveEnterGamePath().substringBeforeLast('/'))
                } else if (section != null) {
                    Triple(section.resolveJsPath(version).substringBeforeLast('/'), version.resolveEnterGamePath().substringBeforeLast('/'),null)
                } else {
                    Triple(version.resolveEnterGamePath().substringBeforeLast('/'), null,null)
                }
            }
            placeholderPath.startsWith(JsFileResolver.ITEM) -> {
                // $ITEM：优先 item assetPath，降级到 version assetPath
                val section = activeCtx.section
                val item = activeCtx.item
                if (section != null && item != null) {
                    val itemPath = item.resolvePath(section, version)
                    Triple(itemPath, version.resolveAssetPath(), version.baseAssetPath)
                } else {
                    // 没有 item 上下文，降级到 version assetPath
                    Triple(version.resolveAssetPath(), version.baseAssetPath,null)
                }
            }
            placeholderPath.startsWith(JsFileResolver.SMF) -> {
                Triple(version.resolveAssetPath(), version.baseAssetPath,null)
            }
            else -> return null // 其他占位符不支持 asset 回退
        }

        val subPath = placeholderPath.removePrefix(basePlaceholder).trimStart('/')
        // 优先从主路径读取
        val fileName = subPath.ifEmpty { primaryPath.substringAfterLast('/') }
        val fullPrimaryPath = if (subPath.isEmpty()) primaryPath else "$primaryPath/$subPath"
        var cacheFile = tryCopyAssetToCache(context, fullPrimaryPath, fileName)
        if (cacheFile != null) return cacheFile

        // 降级到 fallbackPath
        fallbackPath?.let { fbPath ->
            val fullFallbackPath = if (subPath.isEmpty()) fbPath else "$fbPath/$subPath"
            cacheFile = tryCopyAssetToCache(context, fullFallbackPath, fileName)
            if (cacheFile != null) return cacheFile
        }

        // 降级到 fallbackRootPath
        fallbackRootPath?.let { fbPath ->
            val fullFallbackPath = if (subPath.isEmpty()) fbPath else "$fbPath/$subPath"
            cacheFile = tryCopyAssetToCache(context, fullFallbackPath, fileName)
            if (cacheFile != null) return cacheFile
        }

        return null
    }

    /**
     * 尝试从 asset 复制文件到缓存
     */
    private fun tryCopyAssetToCache(context: Context, assetRelativePath: String, fileName: String): InputFile? {
        return try {
            val cacheFile = File(cacheDir, "${UUID.randomUUID()}_$fileName")
            context.assets.open("pvz2tool/$assetRelativePath").use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            InputFile(cacheFile, isCache = true, sourceDoc = null)
        } catch (e: Exception) {
            null
        }
    }
}
