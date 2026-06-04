package io.github.dreammooncai.pvz2tool.js

import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.SectionItem
import io.github.dreammooncai.pvz2tool.VersionDef
import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.pvz2tool.pop.core.rsb.RsbCommonConfig
import io.github.dreammooncai.pvz2tool.ui.dialog.AssetExtractorHolder
import io.github.dreammooncai.pvz2tool.ui.dialog.JsUiManager
import io.github.dreammooncai.util.toFile
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/** 全局 SMF 缓存根目录，按版本隔离 */
private val baseCacheDir: File =
    InitializePvz2.context.cacheDir.resolve("pvz2tool/js_smf_cache")

/**
 * 单个 SMF 文件（如 dynamic.rsb.smf）在某版本下的缓存管理器。
 *
 * 目录结构：
 * ```
 * cacheDir/pvz2tool/js_smf_cache/<versionId>/<smfFileName>/
 *   ├── original/          — 从 asset 复制过来的原始 SMF 文件
 *   ├── extracted/         — 解包后的基准目录（只解一次）
 *   └── modified/          — JS 修改写入的文件（与 extracted 同结构，仅保存差异）
 * ```
 *
 * load() 语义：先读 modified/，没有读 extracted/
 * save() 语义：只写 modified/
 * wasModified()：modified/ 非空即认为有改动
 */
class JsSmfEntryCache(
    val versionId: String,
    /** smf 文件全名，如 "dynamic.rsb.smf" */
    val smfFileName: String
) {
    private val entryDir: File = baseCacheDir.resolve(versionId).resolve(smfFileName)

    val originalDir: File = entryDir.resolve("original")
    val extractedDir: File = entryDir.resolve("extracted")
    val modifiedDir: File = entryDir.resolve("modified")

    /** modified/ 目录非空即有改动 */
    fun wasModified(): Boolean =
        modifiedDir.exists() && modifiedDir.listFiles()?.any() == true
}

/**
 * 版本级 SMF 数据管理器：
 * - 负责按 item.smfList 批量定位、解包、打包 SMF 文件
 * - 缓存按版本隔离（同版本不同 item 共享同一份解包缓存）
 * - SMF 文件查找优先级：
 *   1. `pvz2tool/version/<versionId>/smf/<name>.rsb.smf`（版本专属）
 *   2. `pvz2tool/<version.baseAssetPath>/<name>.rsb.smf`（baseAssetPath 默认 version/base/smf）
 */
class JsSmfDataManager(
    private val version: VersionDef,
    private val item: SectionItem,
    private val holder: AssetExtractorHolder,
    private val targetSmfDir: File?
) {

    companion object {
        fun clearCache() {
            baseCacheDir.deleteRecursively()
        }
    }

    /**
     * 根据 item.smfList 构建每个 SMF 名到 [JsSmfEntryCache] 的映射。
     * 键为不含扩展名的基名（如 "dynamic"），方便 JS 访问 `data.dynamic.*`。
     */
    val caches: Map<String, JsSmfEntryCache> by lazy {
        item.smfList.associateWith { name ->
            val fileName = if (name.endsWith(".rsb.smf", ignoreCase = true) || name.endsWith(".obb",ignoreCase = true)) name
            else "$name.rsb.smf"
            JsSmfEntryCache(version.id, fileName)
        }
    }

    // ======================== 公开操作 ========================

    /**
     * 准备所有 SMF：复制 asset 到 original/ 并解包到 extracted/（已解包的跳过）。
     */
    suspend fun prepareSmf() {
        if (caches.isEmpty()) return

        val skipList = mutableListOf<String>()
        suspendCancellableCoroutine { continuation ->
            holder.setOnFileSkipped {
                it.targetDocument.toFile(InitializePvz2.context)?.absolutePath?.let { path ->
                    skipList += path
                }
            }
            holder.setOnCompleteListener {
                holder.extractor.dismiss()
                continuation.resume(Unit)
            }

            holder.extract(*caches.mapNotNull { (_,cache) ->
                val versionPath = "${version.resolveAssetPath()}/${cache.smfFileName}"
                if (AssetExtractorHolder.exist(versionPath)) {
                    return@mapNotNull AssetExtractorHolder.resource(versionPath,cache.originalDir, sectionName = item.displayName)
                }
                val basePath = version.baseAssetPath?.let { "$it/${cache.smfFileName}" }
                if (basePath != null && AssetExtractorHolder.exist(basePath)) {
                    return@mapNotNull AssetExtractorHolder.resource(basePath,cache.originalDir, sectionName = item.displayName)
                }
                JsConsole.warn("找不到 SMF 文件：${cache.smfFileName}，已跳过")
                return@mapNotNull null
            }.toTypedArray())
            continuation.invokeOnCancellation {  }
        }

        JsUiManager.showProgress("初始化 SMF 数据","正在解包当前功能的SMF文件")
        caches.forEach { (_, cache) ->
            cache.originalDir.listFiles { it.extension.lowercase() in listOf("smf","obb") && (it.absolutePath !in skipList || !cache.extractedDir.exists()) }?.forEach { inFile ->
                val outFolder = cache.extractedDir
                if (outFolder.exists()) outFolder.deleteRecursively()
                Rsb.unpack(inFile, outFolder) {
                    autoRtonToJson = false
                    setOnError { error, message ->
                        JsConsole.error("RSB 初始化解包错误($message): ${error.stackTraceToString()}")
                    }
                    setOnProgress { progress, message ->
                        JsUiManager.updateProgress("正在解包当前功能所需使用的SMF文件...\n$message", progress)
                    }
                    setOnLog { level,message ->
                        when(level) {
                            RsbCommonConfig.LogLevel.INFO -> {}
                            RsbCommonConfig.LogLevel.WARN -> JsConsole.warn(message)
                            RsbCommonConfig.LogLevel.ERROR -> JsConsole.error(message)
                        }
                    }
                }
            }
        }
        JsUiManager.closeProgress()
    }

    /**
     * 打包所有有改动的 SMF：将 modified/ 合并回 extracted/，再打包到游戏 smf 目录。
     */
    suspend fun packSmf() {
        val dest = targetSmfDir ?: return

        val modifiedCaches = caches.values.filter { it.wasModified() }
        if (modifiedCaches.isEmpty()) return

        JsUiManager.showProgress("打包 SMF 数据", "检测到 SMF 数据变化，正在打包...")

        modifiedCaches.forEach { cache ->
            // 合并 modified/ → extracted/
            cache.modifiedDir.copyRecursively(cache.extractedDir, overwrite = true)
            cache.modifiedDir.deleteRecursively()

            val outFile = dest.resolve(cache.smfFileName)
            JsUiManager.updateProgress("打包 ${cache.smfFileName}...", -1f)
            Rsb.pack(cache.extractedDir, outFile) {
                smfCompress = false
                setOnError { error, message ->
                    JsConsole.error("RSB 打包错误($message): ${error.stackTraceToString()}")
                }
                setOnProgress { progress, message ->
                    JsUiManager.updateProgress("打包 ${cache.smfFileName}...\n$message", progress)
                }
                setOnLog { level, message ->
                    when (level) {
                        RsbCommonConfig.LogLevel.INFO  -> {}
                        RsbCommonConfig.LogLevel.WARN  -> JsConsole.warn(message)
                        RsbCommonConfig.LogLevel.ERROR -> JsConsole.error(message)
                    }
                }
            }
        }

        JsUiManager.closeProgress()
    }

    fun closePrepareSmf() {
        if (caches.isEmpty()) return
        caches.forEach { (_, cache) ->
            cache.extractedDir.deleteRecursively()
            cache.modifiedDir.deleteRecursively()
        }
    }
}
