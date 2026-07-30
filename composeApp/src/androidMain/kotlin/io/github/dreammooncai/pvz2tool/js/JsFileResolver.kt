package io.github.dreammooncai.pvz2tool.js

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.github.dreammooncai.pvz2tool.DynamicSection
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.Pvz2ToolConfig
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import java.io.File
import java.io.FileOutputStream

private val SUPPORTED_PREFIXES = listOf(JsFileResolver.WORK_DIR, JsFileResolver.GAME_SAVES, JsFileResolver.GAME_SMF, JsFileResolver.SMF, JsFileResolver.ITEM, JsFileResolver.JS_DIR, JsFileResolver.APP_DATA, JsFileResolver.APP_FILES, JsFileResolver.APP_CACHE, JsFileResolver.ANDROID_DATA, JsFileResolver.ANDROID_FILES, JsFileResolver.ANDROID_CACHE)

/**
 * JS 环境中的文件路径解析器。
 *
 * 路径解析规则（与 Pvz2MainScreen.kt 保持一致）：
 * - rootDirectory = getExternalFilesDir(null)
 *   = /storage/emulated/0/Android/data/com.popcap.pvz2restart/files/
 * - section.resolveTargetDirectory(rootDirectory, config)
 *   = rootDirectory.resolve(section.targetPath ?: config.smfDirectory)
 *
 * 占位符前缀：
 * - `$WORK_DIR`    → 用户通过 SAF 选择的工作目录（工具箱配置根）
 * - `$GAME_SAVES` → 游戏存档目录（rootDirectory + saves.targetPath）
 * - `$GAME_SMF`    → 游戏目录 smf（rootDirectory + config.smfDirectory）
 * - `$SMF`         → 当前选中版本的 smf 目录（workDir + version.assetPath）
 * - `$ITEM`     → 栏目下的 item assetPath，降级到 $SMF
 *
 * 配置支持热加载：每次直接读取 InitializePvz2.config，不做持久缓存。
 */
open class JsFileResolver(
    val jsContext: ActiveJsContext? = null
) {

    constructor(section: DynamicSection?,
                item: SectionItem?,
                version: VersionDef): this(ActiveJsContext(section, item, version))

    companion object : JsFileResolver() {
        // ======================== 占位符常量 ========================

        const val WORK_DIR = $$"$WORK_DIR"
        const val GAME_SAVES = $$"$GAME_SAVES"
        const val GAME_SMF = $$"$GAME_SMF"
        const val SMF = $$"$SMF"
        const val ITEM = $$"$ITEM"
        const val JS_DIR = $$"$JS_DIR"

        // 应用内部存储（/data/user/0/<pkg>/...）
        const val APP_DATA = $$"$APP_DATA"
        const val APP_FILES = $$"$APP_FILES"
        const val APP_CACHE = $$"$APP_CACHE"

        // 应用外部存储（/storage/emulated/0/Android/data/<pkg>/...）
        const val ANDROID_DATA = $$"$ANDROID_DATA"
        const val ANDROID_FILES = $$"$ANDROID_FILES"
        const val ANDROID_CACHE = $$"$ANDROID_CACHE"


        // ======================== 占位符路径解析（非 JS 环境可用） ========================

        /**
         * 将路径字符串中的占位符变量展开为实际绝对路径。
         * 此方法不依赖 JS 上下文，可在配置解析、UI 渲染等场景中直接调用。
         *
         * 支持的占位符：
         * - `$WORK_DIR` → 用户 SAF 工作目录
         * - `$GAME_SAVES` → 游戏存档目录
         * - `$GAME_SMF` → 游戏 SMF 目录
         * - `$APP_DATA` / `$APP_FILES` / `$APP_CACHE` → 应用内部存储
         * - `$ANDROID_DATA` / `$ANDROID_FILES` / `$ANDROID_CACHE` → 应用外部存储
         *
         * 注意：`$SMF`、`$ITEM`、`$JS_DIR` 依赖版本/栏目上下文，此方法无法解析，
         * 仅在 JS 环境中通过 JsFileResolver 实例解析。
         *
         * @param path 可能包含占位符的路径字符串，如 `$APP_DATA/custom/file.txt`
         * @return 展开后的绝对路径；若占位符无法解析则原样返回
         */
        fun resolvePlaceholders(path: String): String {
            if (!path.startsWith("$")) return path

            val ctx = InitializePvz2.context

            // 简单占位符：不需要上下文即可解析
            val resolved = when {
                path == WORK_DIR || path.startsWith("$WORK_DIR/") -> {
                    val workDir = InitializePvz2.config.getLocalWorkDir(ctx)
                    val root = workDir?.uri?.path?.let { File(it) }
                    resolveWithRoot(root, path, WORK_DIR)
                }
                path == GAME_SAVES || path.startsWith("$GAME_SAVES/") -> {
                    // 内联解析，避免通过 resolveTargetDirectory() 递归回调
                    val savesSection = InitializePvz2.config.sections.find { it.id == "saves" }
                    val root = savesSection?.let { section ->
                        val tp = section.targetPath
                        when (tp) {
                            null -> Pvz2ToolConfig.rootDirectory.resolve(InitializePvz2.config.smfDirectory)
                            else -> {
                                val resolvedTp = if (tp.startsWith("/")) tp else tp  // targetPath 本身不递归展开
                                if (resolvedTp.startsWith("/")) File(resolvedTp) else Pvz2ToolConfig.rootDirectory.resolve(resolvedTp)
                            }
                        }
                    }
                    resolveWithRoot(root, path, GAME_SAVES)
                }
                path == GAME_SMF || path.startsWith("$GAME_SMF/") -> {
                    val root = Pvz2ToolConfig.rootDirectory.resolve(InitializePvz2.config.smfDirectory)
                    resolveWithRoot(root, path, GAME_SMF)
                }
                path == APP_DATA || path.startsWith("$APP_DATA/") -> {
                    resolveWithRoot(ctx.dataDir, path, APP_DATA)
                }
                path == APP_FILES || path.startsWith("$APP_FILES/") -> {
                    resolveWithRoot(ctx.filesDir, path, APP_FILES)
                }
                path == APP_CACHE || path.startsWith("$APP_CACHE/") -> {
                    resolveWithRoot(ctx.cacheDir, path, APP_CACHE)
                }
                path == ANDROID_DATA || path.startsWith("$ANDROID_DATA/") -> {
                    val root = ctx.getExternalFilesDir(null)?.parentFile
                    resolveWithRoot(root, path, ANDROID_DATA)
                }
                path == ANDROID_FILES || path.startsWith("$ANDROID_FILES/") -> {
                    val root = ctx.getExternalFilesDir(null)
                    resolveWithRoot(root, path, ANDROID_FILES)
                }
                path == ANDROID_CACHE || path.startsWith("$ANDROID_CACHE/") -> {
                    val root = ctx.externalCacheDir
                    resolveWithRoot(root, path, ANDROID_CACHE)
                }
                else -> null
            }

            return resolved ?: path
        }

        private fun resolveWithRoot(root: File?, path: String, placeholder: String): String? {
            if (root == null) return null
            val subPath = path.removePrefix(placeholder).trimStart('/')
            return if (subPath.isEmpty()) root.absolutePath else File(root, subPath).absolutePath
        }

        // ======================== 工具方法 ========================

        /**
         * 将 DocumentFile 转换为实际可读写的 File 对象。
         *
         * 支持的场景：
         * 1. `file://` 协议 → 直接取 path
         * 2. `content://com.android.externalstorage.documents/...` (Android 内置 ExternalStorageProvider)
         *    → 解析 docId 为 `/storage/emulated/0/<relative>` 或 `/storage/<uuid>/<relative>`
         * 3. 其他 content URI → 尝试通过 ContentResolver 查询 _data 列
         *
         * @return 若无法转换（如第三方 SAF 提供者无权限），返回 null
         */
        fun documentFileToFile(docFile: DocumentFile): File? {
            val uri = docFile.uri
            val ctx = InitializePvz2.context

            // 1. 本身就是 file:// 协议
            if ("file" == uri.scheme) {
                return uri.path?.let { File(it) }
            }

            // 2. Android 内置 ExternalStorageProvider（最常见）
            if ("content" == uri.scheme && "com.android.externalstorage.documents" == uri.authority) {
                val docId = try {
                    DocumentsContract.getDocumentId(uri)
                } catch (e: IllegalArgumentException) {
                    null
                } ?: return null

                val split = docId.split(":", limit = 2)
                if (split.size >= 2) {
                    val type = split[0]
                    val relativePath = try {
                        java.net.URLDecoder.decode(split[1], "UTF-8")
                    } catch (e: Exception) {
                        split[1]
                    }

                    if ("primary".equals(type, ignoreCase = true)) {
                        // 尝试 /storage/emulated/0
                        val f1 = File("/storage/emulated/0/$relativePath")
                        if (f1.exists() && f1.canWrite()) return f1
                        val f2 = File("/sdcard/$relativePath")
                        if (f2.exists() && f2.canWrite()) return f2
                    } else {
                        // SD 卡路径，如 /storage/1A0B-2C3D/...
                        val f = File("/storage/$type/$relativePath")
                        if (f.exists() && f.canWrite()) return f
                    }
                }

                // 3. 通用 ContentResolver 查询（兜底）
                return queryFilePathFromContentUri(ctx, uri)
            }

            // 3. 其他 content:// URI → ContentResolver 兜底
            return queryFilePathFromContentUri(ctx, uri)
        }

        /**
         * 向 DocumentFile 写入字节数据（不依赖 File 路径）。
         * 先尝试转换为 File 路径写入；失败则通过 ContentResolver 写入。
         */
        fun writeToDocumentFile(docFile: DocumentFile, data: ByteArray, context: Context) {
            // 方案1：File 路径（最可靠）
            val file = documentFileToFile(docFile)
            if (file != null) {
                FileOutputStream(file, false).use { it.write(data); it.flush() }
                return
            }

            // 方案2：ContentResolver（SAF 标准方式）
            val targetDoc = ensureFileExists(docFile)
            context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { output ->
                output.write(data)
                output.flush()
            } ?: throw IllegalStateException("无法打开文件输出流: ${targetDoc.uri}")
        }

        /**
         * 从 DocumentFile 读取字节数据。
         */
        fun readFromDocumentFile(docFile: DocumentFile, context: Context): ByteArray {
            // 方案1：File 路径
            val file = documentFileToFile(docFile)
            if (file != null) {
                return file.readBytes()
            }

            // 方案2：ContentResolver
            return context.contentResolver.openInputStream(docFile.uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("无法读取文件: ${docFile.uri}")
        }

    }

    // ======================== JS 执行上下文（Map + ThreadLocal ID） ========================

    /**
     * 当前正在执行的 JS 脚本对应的 Section/Item 上下文。
     * 用于解析 `$SMF` 等依赖当前按钮配置的路径。
     */
    data class ActiveJsContext(
        val section: DynamicSection?,
        val item: SectionItem?,
        val version: VersionDef
    )


    // ======================== 新增：占位符路径转 internalPath ========================

    /**
     * 将 JS 占位符路径转换为 [AssetExtractorHolder.resource] 可使用的 internalPath。
     *
     * 转换规则：
     * - `$SMF` / `$SMF/xxx`       → `version/assetPath[/xxx]`（如 `version/new/smf/files/ver.txt`）
     * - `$ITEM` / `$ITEM/xxx`     → `version/id/sectionId/itemId[/xxx]`（降级到 `$SMF`）
     * - `$JS_DIR` / `$JS_DIR/xxx`   → `version/enterGamePath[/xxx]`
     * - `$WORK_DIR` / `$WORK_DIR/xxx` → `""`（本地工作目录，无 assets 对应路径，返回 null）
     * - `/absolute/path`            → `absolute/path`（去掉开头 `/`，补 `pvz2tool/` 前缀由调用方负责）
     * - `relative/path`           → `relative/path`（相对路径，原样返回）
     *
     * **降级规则**（与 [resolveSmfDocumentFile] 一致）：
     * 1. 优先查找 primaryPath（版本专属目录）
     * 2. 若不存在，回退到 fallbackPath（baseAssetPath）
     * 3. 仍未找到，回退到 fallbackRootPath
     *
     * @return internalPath（不含 `pvz2tool/` 前缀），若无法转换（如 `$WORK_DIR`）则返回 null。
     */
    fun resolveToInternalPath(placeholderPath: String): String? {
        // 规范化：裸相对路径（不以 / 或 $ 开头）自动补充 $WORK_DIR/ 前缀
        val path = if (placeholderPath.startsWith("/") || placeholderPath.startsWith("$")) {
            placeholderPath
        } else {
            "$WORK_DIR/$placeholderPath"
        }

        val basePlaceholder = when {
            path.startsWith(WORK_DIR) -> WORK_DIR
            path.startsWith(JS_DIR) -> JS_DIR
            path.startsWith(ITEM) -> ITEM
            path.startsWith(SMF) -> SMF
            path.startsWith("/") -> return path
            // 绝对路径占位符没有 assets 内部路径对应
            path.startsWith(APP_DATA) || path.startsWith(APP_FILES) || path.startsWith(APP_CACHE) ||
            path.startsWith(ANDROID_DATA) || path.startsWith(ANDROID_FILES) || path.startsWith(ANDROID_CACHE) -> return null
            else -> return path // 相对路径原样返回
        }

        // $WORK_DIR 对应 assets/pvz2tool/ 根目录
        if (basePlaceholder == WORK_DIR) {
            val subPath = path.removePrefix(WORK_DIR).trimStart('/')
            return subPath.ifEmpty { "" }
        }

        val activeCtx = jsContext ?: return null
        val version = activeCtx.version

        // 计算 primaryPath / fallbackPath / fallbackRootPath（与 resolveSmfDocumentFile 一致）
        val (primaryPath, fallbackPath, fallbackRootPath) = when (basePlaceholder) {
            ITEM -> {
                val section = activeCtx.section
                val item = activeCtx.item
                if (section != null && item != null) {
                    Triple(item.resolvePath(section, version), version.resolveAssetPath(), version.baseAssetPath)
                } else {
                    Triple(version.resolveAssetPath(), version.baseAssetPath, null)
                }
            }
            JS_DIR -> {
                val section = activeCtx.section
                val item = activeCtx.item
                if (section != null && item != null) {
                    val itemPath = item.resolveJsPath(section, version).substringBeforeLast('/')
                    Triple(itemPath, section.resolveJsPath(version).substringBeforeLast('/'), version.resolveEnterGamePath().substringBeforeLast('/'))
                } else if (section != null) {
                    Triple(section.resolveJsPath(version).substringBeforeLast('/'), version.resolveEnterGamePath().substringBeforeLast('/'), null)
                } else {
                    Triple(version.resolveEnterGamePath().substringBeforeLast('/'), null, null)
                }
            }
            else -> { // SMF
                Triple(version.resolveAssetPath(), version.baseAssetPath, null)
            }
        }

        val subPath = path.removePrefix(basePlaceholder).trimStart('/')

        // 按优先级尝试：primary → fallback → fallbackRoot（与 resolveSmfDocumentFile 一致）
        // 检查 asset 中是否存在 base + subPath（文件或目录）
        fun assetPathExists(assetRelativeBase: String): Boolean {
            val fullAssetPath = if (assetRelativeBase.isEmpty())
                subPath
            else if (subPath.isEmpty())
                assetRelativeBase
            else
                "$assetRelativeBase/$subPath"

            return AssetExtractorHolder.exist(fullAssetPath)
        }

        val chosenBase = listOfNotNull(primaryPath, fallbackPath, fallbackRootPath)
            .firstOrNull { it.isNotEmpty() && assetPathExists(it) }

        if (chosenBase == null) return null

        val result = if (subPath.isEmpty()) chosenBase else "$chosenBase/$subPath"
        return result.ifEmpty { null }
    }

    // ======================== 占位符 → 绝对路径字符串（不要求存在） ========================

    /**
     * 将占位符路径展开为绝对路径字符串，**不要求目标文件/目录已存在**。
     *
     * 与 [resolvePlaceholders]（companion，仅非上下文占位符）不同，本方法同时支持
     * 需版本/栏目上下文的 `$SMF` / `$ITEM` / `$JS_DIR`。
     *
     * 适用于 `path.解析路径()` 这类「只想得到最终路径字符串、不需要实际读写文件」的场景。
     * 占位符可当作路径前缀拼接子路径，例如：
     * - `$ANDROID_FILES/test`              → `/storage/emulated/0/Android/data/<pkg>/files/test`
     * - `$SMF/abc/def.txt`                → workDir/<version.assetPath>/abc/def.txt
     *
     * @param placeholderPath 含占位符的路径（支持拼接子路径，子路径不必存在）
     * @return 展开后的绝对路径；无法解析（如缺少上下文、根目录不可用）时返回 null
     */
    fun resolveToAbsolutePath(placeholderPath: String, context: Context): String? {
        val path = placeholderPath.trim()
        // 绝对路径：原样返回
        if (path.startsWith("/")) return path
        // 裸相对路径（不以 $ 开头）：自动补 $WORK_DIR 前缀
        if (!path.startsWith("$")) {
            val workRoot = getWorkDir(context)?.let { documentFileToFile(it) }?.absolutePath ?: return null
            return File(workRoot, path).absolutePath
        }

        // 绝对路径占位符（$APP_DATA / $ANDROID_FILES 等）：根目录 + 子路径，不检查存在性
        val abs = resolveAbsolutePlaceholderPath(path, context)
        if (abs != null) return abs

        // $WORK_DIR / $GAME_SAVES / $GAME_SMF
        val (prefix, rootDoc) = when {
            path.startsWith(WORK_DIR) -> WORK_DIR to getWorkDir(context)
            path.startsWith(GAME_SAVES) -> GAME_SAVES to getGameSaves()
            path.startsWith(GAME_SMF) -> GAME_SMF to getGameSmf()
            else -> null to null
        }
        if (prefix != null) {
            val root = rootDoc ?: return null
            val rootPath = documentFileToFile(root)?.absolutePath ?: return null
            val sub = path.removePrefix(prefix).trimStart('/')
            return if (sub.isEmpty()) rootPath else File(rootPath, sub).absolutePath
        }

        // $SMF / $ITEM / $JS_DIR（需上下文）：先解析根目录路径，再拼接子路径（不检查存在性）
        val basePlaceholder = when {
            path.startsWith(JS_DIR) -> JS_DIR
            path.startsWith(ITEM) -> ITEM
            path.startsWith(SMF) -> SMF
            else -> null
        }
        if (basePlaceholder != null) {
            val baseDoc = resolveSmfDocumentFile(basePlaceholder, context) ?: return null
            val basePath = documentFileToFile(baseDoc)?.absolutePath ?: baseDoc.uri.path ?: return null
            val sub = path.removePrefix(basePlaceholder).trimStart('/')
            return if (sub.isEmpty()) basePath else File(basePath, sub).absolutePath
        }

        return null
    }

    // ======================== 公开 API ========================

    /**
     * 将占位符前缀解析为 [DocumentFile]。
     * 可用于创建子文件、子目录等 DocumentFile 操作。
     */
    fun resolve(prefix: String, relativePath: String, context: Context): DocumentFile? {
        val root: DocumentFile? = when (prefix) {
            WORK_DIR -> getWorkDir(context)
            GAME_SAVES -> getGameSaves()
            GAME_SMF -> getGameSmf()
            // $SMF 已在 resolveToPath 中特殊处理
            else -> null
        }

        if (root == null) return null
        if (relativePath.isEmpty()) return root
        return buildDocumentFilePath(root, relativePath, createIntermediate = false)
    }

    /**
     * 解析路径字符串，返回 [DocumentFile]。
     * 示例：
     * - "$WORK_DIR/config.json"        → workDir 下的 config.json
     * - "$GAME_SAVES/UserData.rton"    → 游戏存档下的 UserData.rton
     * - "$SMF/main.rton"               → 当前按钮 smf 目录下的 main.rton
     * - "/absolute/path/to/file"       → 绝对路径，直接返回 DocumentFile
     */
    fun resolve(path: String, context: Context): DocumentFile? {
        // 绝对路径占位符：直接解析为本地目录
        val absDoc = resolveAbsolutePlaceholder(path, context)
        if (absDoc != null) return absDoc

        // $SMF 特殊处理
        if (path == SMF || path.startsWith("$SMF/") || path == ITEM || path.startsWith("$ITEM/") || path == JS_DIR || path.startsWith("$JS_DIR/") || path == WORK_DIR || path.startsWith("$WORK_DIR/")) {
            val docFile = resolveSmfDocumentFile(path, context) ?: return null
            return docFile
        }

        // 解析占位符路径
        val parsed = parsePlaceholder(path)
        if (parsed != null) {
            return resolve(parsed.first, parsed.second, context)
        }

        // 支持绝对路径：直接创建 DocumentFile
        if (path.startsWith("/")) {
            val file = File(path)
            return if (file.exists()) DocumentFile.fromFile(file) else null
        }

        return null
    }

    /**
     * 将占位符路径解析为「输出目标」DocumentFile，支持目标文件**尚不存在**的情况。
     *
     * 仅支持 SAF 树根占位符（`$WORK_DIR` / `$GAME_SAVES` / `$GAME_SMF`）：
     * 会解析并创建父目录（含中间目录），然后确保目标文件存在（不存在则创建），
     * 以支持「把内容写入一个此前不存在的文件」的场景（如 `picker.copy`）。
     *
     * 对于只读占位符（`$SMF` / `$ITEM` / `$JS_DIR`）仍返回 `null`
     * （这些目录通常只读，不支持在树内新建输出文件）。
     *
     * @return 目标文件 DocumentFile；若前缀不支持创建或根目录不可用时返回 `null`
     */
    fun resolveForOutput(placeholderPath: String, context: Context): DocumentFile? {
        // 绝对路径占位符（$APP_DATA 等）：其目标必须已存在
        val absDoc = resolveAbsolutePlaceholder(placeholderPath, context)
        if (absDoc != null) return absDoc

        val parsed = parsePlaceholder(placeholderPath) ?: return null
        val (prefix, relativePath) = parsed
        // 仅 SAF 树根支持在树内新建输出文件
        if (prefix != WORK_DIR && prefix != GAME_SAVES && prefix != GAME_SMF) return null

        val root = when (prefix) {
            WORK_DIR -> getWorkDir(context)
            GAME_SAVES -> getGameSaves()
            GAME_SMF -> getGameSmf()
            else -> null
        } ?: return null

        val subPath = relativePath.trimStart('/')
        if (subPath.isEmpty()) return root
        val segments = subPath.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return root
        val fileName = segments.last()
        val parentRelPath = segments.dropLast(1).joinToString("/")

        // 创建父目录（含中间目录）
        val parentDir = if (parentRelPath.isEmpty()) root
        else buildDocumentFilePath(root, parentRelPath, createIntermediate = true)
            ?: return null
        if (!parentDir.isDirectory) return null

        // 确保目标文件存在（同名目录则删除后重建为文件）
        val existing = parentDir.findFile(fileName)
        if (existing != null) {
            if (existing.isFile) return existing
            existing.delete()
        }
        return parentDir.createFile("*/*", fileName)
            ?: throw IllegalStateException("无法在 $prefix 中创建文件: $fileName")
    }

    // ======================== 绝对路径占位符解析 ========================

    /**
     * 解析绝对路径占位符（$APP_DATA、$ANDROID_FILES 等）。
     * 这些占位符对应固定的本地目录，不依赖版本/栏目上下文。
     *
     * @return 解析后的 DocumentFile，若路径不匹配任何绝对路径占位符则返回 null
     */
    private fun resolveAbsolutePlaceholder(path: String, context: Context): DocumentFile? {
        val basePlaceholder = when {
            path.startsWith(APP_DATA) -> APP_DATA
            path.startsWith(APP_FILES) -> APP_FILES
            path.startsWith(APP_CACHE) -> APP_CACHE
            path.startsWith(ANDROID_DATA) -> ANDROID_DATA
            path.startsWith(ANDROID_FILES) -> ANDROID_FILES
            path.startsWith(ANDROID_CACHE) -> ANDROID_CACHE
            else -> return null
        }

        val rootFile: File = when (basePlaceholder) {
            APP_DATA -> context.dataDir
            APP_FILES -> context.filesDir
            APP_CACHE -> context.cacheDir
            ANDROID_DATA -> context.getExternalFilesDir(null)?.parentFile ?: return null
            ANDROID_FILES -> context.getExternalFilesDir(null) ?: return null
            ANDROID_CACHE -> context.externalCacheDir ?: return null
            else -> return null
        }

        val subPath = path.removePrefix(basePlaceholder).trimStart('/')
        val target = if (subPath.isEmpty()) rootFile else File(rootFile, subPath)
        return if (target.exists()) DocumentFile.fromFile(target) else null
    }

    /**
     * 解析绝对路径占位符为路径字符串（不要求目标存在）。
     * 仅用于 [resolveToAbsolutePath] 的字符串展开场景。
     */
    private fun resolveAbsolutePlaceholderPath(path: String, context: Context): String? {
        val basePlaceholder = when {
            path.startsWith(APP_DATA) -> APP_DATA
            path.startsWith(APP_FILES) -> APP_FILES
            path.startsWith(APP_CACHE) -> APP_CACHE
            path.startsWith(ANDROID_DATA) -> ANDROID_DATA
            path.startsWith(ANDROID_FILES) -> ANDROID_FILES
            path.startsWith(ANDROID_CACHE) -> ANDROID_CACHE
            else -> return null
        }

        val rootFile: File = when (basePlaceholder) {
            APP_DATA -> context.dataDir
            APP_FILES -> context.filesDir
            APP_CACHE -> context.cacheDir
            ANDROID_DATA -> context.getExternalFilesDir(null)?.parentFile ?: return null
            ANDROID_FILES -> context.getExternalFilesDir(null) ?: return null
            ANDROID_CACHE -> context.externalCacheDir ?: return null
            else -> return null
        }

        val subPath = path.removePrefix(basePlaceholder).trimStart('/')
        return if (subPath.isEmpty()) rootFile.absolutePath else File(rootFile, subPath).absolutePath
    }

    /**
     * 解析绝对路径占位符为本地 [File]（不要求目标存在）。
     * 仅处理 `$APP_DATA` / `$APP_FILES` / `$APP_CACHE` / `$ANDROID_DATA` / `$ANDROID_FILES` / `$ANDROID_CACHE`。
     * 用于写入型解析（`resolveOutput`）：这些占位符的根目录是常规本地目录，可直接在子路径创建文件/目录。
     * 非绝对占位符（如 `$WORK_DIR` / `$SMF`）返回 null，交由 SAF 树流程处理。
     */
    fun resolveAbsolutePlaceholderFile(placeholderPath: String, context: Context): File? {
        val basePlaceholder = when {
            placeholderPath.startsWith(APP_DATA) -> APP_DATA
            placeholderPath.startsWith(APP_FILES) -> APP_FILES
            placeholderPath.startsWith(APP_CACHE) -> APP_CACHE
            placeholderPath.startsWith(ANDROID_DATA) -> ANDROID_DATA
            placeholderPath.startsWith(ANDROID_FILES) -> ANDROID_FILES
            placeholderPath.startsWith(ANDROID_CACHE) -> ANDROID_CACHE
            else -> return null
        }

        val rootFile: File = when (basePlaceholder) {
            APP_DATA -> context.dataDir
            APP_FILES -> context.filesDir
            APP_CACHE -> context.cacheDir
            ANDROID_DATA -> context.getExternalFilesDir(null)?.parentFile ?: return null
            ANDROID_FILES -> context.getExternalFilesDir(null) ?: return null
            ANDROID_CACHE -> context.externalCacheDir ?: return null
            else -> return null
        }

        val subPath = placeholderPath.removePrefix(basePlaceholder).trimStart('/')
        return if (subPath.isEmpty()) rootFile else File(rootFile, subPath)
    }

    // ======================== $SMF 特殊解析 ========================

    /**
     * $SMF 和 $ITEM 解析实现。
     *
     * **$SMF 规则**：
     * - 纯 "$SMF"       → workDir/<version.assetPath> 目录本身
     * - "$SMF/subpath"  → workDir/<version.assetPath>/subpath 子文件
     * - 无 workDir      → asset/pvz2tool/<version.assetPath>（只读，按需复制到缓存）
     *
     * **$ITEM 规则**：
     * - 纯 "$ITEM"   → 栏目下的 item assetPath（默认 version/版本ID/栏目ID/功能项ID）
     * - "$ITEM/subpath" → item assetPath 下的子文件
     * - **降级**：item assetPath 不存在时，降级到 $SMF（版本目录）
     *
     * **$SMF 降级规则**：当版本目录不存在时，自动回退到 baseAssetPath：
     * 1. 优先查找 `version.assetPath`（版本专属目录）
     * 2. 版本目录不存在时，回退到 `baseAssetPath`（基础资源目录）
     *
     * 配置热加载：每次直接读取 InitializePvz2.config。
     */
    private fun resolveSmfDocumentFile(placeholderPath: String, context: Context): DocumentFile? {

        // $WORK_DIR 直接映射到 SAF 工作目录树（仅查找，不创建）
        if (placeholderPath.startsWith(WORK_DIR)) {
            val workDir = getWorkDir(context) ?: return null
            val subPath = placeholderPath.removePrefix(WORK_DIR).trimStart('/')
            return if (subPath.isEmpty()) workDir
            else buildDocumentFilePath(workDir, subPath, createIntermediate = false)
        }

        val activeCtx = jsContext ?: return null
        val version = activeCtx.version

        val basePlaceholder = when {
            placeholderPath.startsWith(JS_DIR) -> JS_DIR
            placeholderPath.startsWith(ITEM) -> ITEM
            else -> SMF
        }

        val (primaryPath, fallbackPath, fallbackRootPath) = when {
            placeholderPath.startsWith(JS_DIR) -> {
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
            placeholderPath.startsWith(ITEM) -> {
                val section = activeCtx.section
                val item = activeCtx.item
                if (section != null && item != null) {
                    val itemPath = item.resolvePath(section, version)
                    Triple(itemPath,version.resolveAssetPath(),version.baseAssetPath)
                } else {
                    Triple(version.resolveAssetPath(),version.baseAssetPath,null)
                }
            }

            else -> Triple(version.resolveAssetPath(),version.baseAssetPath,null)
        }

        if (primaryPath.isEmpty()) return null

        // 优先从 workDir 解析
        val workDir = getWorkDir(context)

        val smfBase = workDir?.let { workDir ->
            // workDir 模式
            val primaryDir = buildDocumentFilePath(workDir, primaryPath, createIntermediate = false)
            if (primaryDir != null && primaryDir.exists()) {
                primaryDir
            } else {
                // 降级路径
                fallbackPath?.let { fbPath ->
                    val fallbackDir = buildDocumentFilePath(workDir, fbPath, createIntermediate = false)
                    if (fallbackDir != null && fallbackDir.exists()) {
                        fallbackDir
                    } else {
                        fallbackRootPath?.let { rootPath ->
                            buildDocumentFilePath(workDir, rootPath, createIntermediate = false)
                        }
                    }
                }
            }
        } ?: getSmfFromAssetWithFallback(primaryPath, fallbackPath,fallbackRootPath)
        if (smfBase == null) return null

        return if (placeholderPath == basePlaceholder) {
            // 纯占位符 → 目录本身
            smfBase
        } else {
            // 带子路径 → 目录下的子文件
            val subPath = placeholderPath.removePrefix(basePlaceholder).trimStart('/')
            if (subPath.isEmpty()) smfBase
            else buildDocumentFilePath(smfBase, subPath, createIntermediate = false)
        }
    }

    // ======================== $GAME_SMF 特殊解析 ========================

    /**
     * 游戏目录 smf：rootDirectory + config.smfDirectory。
     * 与 Pvz2MainScreen.kt 的 section.resolveTargetDirectory() 保持一致。
     */
    private fun getGameSmf(): DocumentFile? {
        val gameSmf = jsContext?.section?.resolveTargetDirectory() ?: InitializePvz2.config.getSmfDirectoryFile()
        return if (gameSmf.exists()) DocumentFile.fromFile(gameSmf) else null
    }
}


// ======================== 目录缓存实现 ========================

private fun getWorkDir(context: Context): DocumentFile? = InitializePvz2.config.getLocalWorkDir(context)

/**
 * 游戏存档目录：从配置 sections[id="saves"].targetPath 解析。
 *
 * 与 Pvz2MainScreen.kt 的 section.resolveTargetDirectory() 保持一致：
 * - rootDirectory = getExternalFilesDir(null)
 * - target = rootDirectory.resolve(savesSection.targetPath)
 *
 * targetPath 支持 File.resolve() 语义：
 * - 相对路径（如 "com.popcap.pvz2restart/No_Backup"）→ 拼接到 rootDirectory 后
 * - 绝对路径 → 直接使用
 *
 * 配置热加载：每次直接读取 InitializePvz2.config.sections。
 */
private fun getGameSaves(): DocumentFile? {
    val savesSection = InitializePvz2.config.sections.find { it.id == "saves" } ?: return null

    val targetPath = savesSection.resolveTargetDirectory()

    return if (targetPath.exists()) DocumentFile.fromFile(targetPath) else null
}

/**
 * 从 asset 目录解析 smf 路径。
 * assetPath 是相对于 pvz2tool 的路径，如 "version/new/smf"
 * asset 完整路径格式：pvz2tool/version/new/smf
 *
 * **降级规则**：
 * 1. 优先查找 primaryPath（版本专属目录）
 * 2. 若不存在，回退到 fallbackPath（baseAssetPath）
 */
private fun getSmfFromAssetWithFallback(primaryPath: String, fallbackPath: String?, fallbackRootPath: String?): DocumentFile? {
    // 优先检查版本专属目录
    val primaryFullPath = "pvz2tool/${primaryPath.trimEnd('/')}"
    try {
        val list = InitializePvz2.context.assets.list(primaryFullPath)
        if (!list.isNullOrEmpty()) {
            val cacheDir = getAssetCacheDir(primaryFullPath)
            if (cacheDir != null && cacheDir.exists()) {
                return DocumentFile.fromFile(cacheDir)
            }
        }
    } catch (e: Exception) {
        // 版本目录不存在，继续降级
    }

    // 降级到 baseAssetPath
    fallbackPath?.let { basePath ->
        val fallbackFullPath = "pvz2tool/${basePath.trimEnd('/')}"
        try {
            val list = InitializePvz2.context.assets.list(fallbackFullPath)
            if (!list.isNullOrEmpty()) {
                val cacheDir = getAssetCacheDir(fallbackFullPath)
                if (cacheDir != null && cacheDir.exists()) {
                    return DocumentFile.fromFile(cacheDir)
                }
            }
        } catch (e: Exception) {
            // baseAssetPath 也不存在
        }
    }

    // 降级到 baseAssetPath
    fallbackRootPath?.let { basePath ->
        val fallbackFullPath = "pvz2tool/${basePath.trimEnd('/')}"
        try {
            val list = InitializePvz2.context.assets.list(fallbackFullPath)
            if (!list.isNullOrEmpty()) {
                val cacheDir = getAssetCacheDir(fallbackFullPath)
                if (cacheDir != null && cacheDir.exists()) {
                    return DocumentFile.fromFile(cacheDir)
                }
            }
        } catch (e: Exception) {
            // baseAssetPath 也不存在
        }
    }

    return null
}

/**
 * 将 asset 目录复制到缓存，并返回缓存目录。
 * assetPath 格式：pvz2tool/version/new/smf
 * 返回：<cache>/pvz2tool_version_new_smf
 */
private fun getAssetCacheDir(assetPath: String): File? {
    val ctx = InitializePvz2.context
    val cacheName = assetPath.replace("/", "_")
    val cacheDir = File(ctx.cacheDir, "asset_$cacheName")

    if (cacheDir.exists() && cacheDir.isDirectory) {
        return cacheDir
    }

    return try {
        cacheDir.mkdirs()
        val files = ctx.assets.list(assetPath) ?: return null
        files.forEach { fileName ->
            val destFile = File(cacheDir, fileName)
            ctx.assets.open("$assetPath/$fileName").use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        cacheDir
    } catch (e: Exception) {
        null
    }
}

private fun parsePlaceholder(path: String): Pair<String, String>? {
    for (prefix in SUPPORTED_PREFIXES) {
        if (path.startsWith(prefix)) {
            val relative = path.removePrefix(prefix)
            val pair = Pair(prefix, relative.trimStart('/'))
            return pair
        }
    }
    return null
}

/**
 * 在 DocumentFile 树中查找或创建路径。
 * @param root 根目录 DocumentFile
 * @param path 相对路径（如 "android/data/com.popcap/files"）
 * @param createIntermediate 是否创建中间目录（false = 仅查找）
 */
private fun buildDocumentFilePath(root: DocumentFile, path: String, createIntermediate: Boolean): DocumentFile? {
    var current: DocumentFile? = root
    for (segment in path.split("/").filter { it.isNotEmpty() }) {
        if (current == null) return null
        var child = current.findFile(segment)
        if (child == null) {
            if (createIntermediate) {
                child = current.createDirectory(segment)
            } else {
                return null
            }
        }
        current = child
    }
    return current
}

private fun queryFilePathFromContentUri(context: Context, uri: Uri): File? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val colIndex = cursor.getColumnIndex("_data")
                if (colIndex != -1) {
                    val path = cursor.getString(colIndex)
                    if (!path.isNullOrEmpty()) {
                        val f = File(path)
                        if (f.exists() && f.canWrite()) f else null
                    } else null
                } else null
            } else null
        }
    } catch (e: Exception) {
        null
    }
}

private fun ensureFileExists(docFile: DocumentFile): DocumentFile {
    if (docFile.exists()) {
        if (docFile.isFile) return docFile
        // 同名文件存在但类型为目录，删除后重建
        docFile.delete()
    }
    val parent = docFile.parentFile ?: throw IllegalStateException("无法获取父目录")
    val fileName = docFile.name ?: "unknown_file"
    return parent.createFile(docFile.type ?: "*/*", fileName)
        ?: throw IllegalStateException("无法创建文件: $fileName")
}