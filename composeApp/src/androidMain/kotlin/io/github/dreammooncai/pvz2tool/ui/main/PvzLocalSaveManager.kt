package io.github.dreammooncai.pvz2tool.ui.main

import android.content.Context
import io.github.dreammooncai.pvz2tool.InitializePvz2
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object PvzLocalSaveManager {
    private const val LOCAL_SAVE_DIR_NAME = "local_saves"
    private const val LOCAL_SAVE_META_FILE = "save_meta.json"

    // 序列化配置（兼容SDK21）
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    // 存档刷新触发器核心：用于通知Compose UI更新
    private var refreshCounter = 0L
    private val refreshListeners = mutableListOf<(Long) -> Unit>()

    /**
     * 获取本地存档根目录（适配SDK21的外部存储）
     */
    fun getLocalSaveRootDir(context: Context): File {
        // 1. 目标目录：内部存储 文件夹（最终要返回的目录）
        val internalDir = File(context.filesDir, LOCAL_SAVE_DIR_NAME)
        // 提前创建目录（不存在则创建，存在则忽略）
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }

        // 2. 源目录：外部存储 对应文件夹
        val externalDir = context.getExternalFilesDir(null)?.resolve(LOCAL_SAVE_DIR_NAME)

        // 3. 如果外部目录存在，且是文件夹，则复制到内部（跳过已存在）
        externalDir?.takeIf { it.exists() && it.isDirectory }?.let { sourceDir ->
            try {
                // 核心：递归复制，overwrite = false 【跳过已存在的子项】
                sourceDir.copyRecursively(
                    target = internalDir,
                    overwrite = false,
                    onError = { file, exception ->
                        // 复制出错时的回调（比如文件权限、IO异常）
                        // 选择继续执行，不中断整体复制
                        println("复制文件失败：${file.absolutePath}, 错误：${exception.message}")
                        OnErrorAction.SKIP
                    }
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // 4. 始终返回内部存储目录
        return internalDir
    }

    /**
     * 保存存档元数据
     */
    fun saveLocalSaveMeta(context: Context, entity: PvzLocalSaveEntity) {
        try {
            val rootDir = getLocalSaveRootDir(context)
            if (!rootDir.exists()) rootDir.mkdirs()

            val metaFile = File(rootDir, "${entity.id}_$LOCAL_SAVE_META_FILE")
            val jsonStr = json.encodeToString(PvzLocalSaveEntity.serializer(), entity)
            metaFile.writeText(jsonStr, Charsets.UTF_8)

            // 保存成功后触发UI刷新
            triggerRefresh()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取所有本地存档（优化异常处理）
     */
    fun getAllLocalSaves(context: Context): List<PvzLocalSaveEntity> {
        return try {
            val rootDir = getLocalSaveRootDir(context)
            if (!rootDir.exists()) return emptyList()

            rootDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(LOCAL_SAVE_META_FILE) }
                ?.mapNotNull { file ->
                    runCatching {
                        val jsonStr = file.readText(Charsets.UTF_8)
                        json.decodeFromString(PvzLocalSaveEntity.serializer(), jsonStr)
                    }.getOrNull()
                }
                ?.sortedByDescending { it.createTime.toLongOrNull() ?: 0 }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 替换原有 deleteLocalSave 方法
    fun deleteLocalSave(context: Context, saveId: String): PvzSaveOperationResult {
        val saveConfig = InitializePvz2.config.ui.save
        return try {
            val rootDir = getLocalSaveRootDir(context)

            // 先获取存档实体（删除元数据文件之前）
            val saveEntity = getAllLocalSaves(context).find { it.id == saveId }

            // 先删除存档目录
            saveEntity?.let {
                val saveDir = File(it.savePath)
                if (saveDir.exists() && saveDir.isDirectory) {
                    val isDirDeleted = saveDir.deleteRecursively()
                    if (!isDirDeleted) {
                        return PvzSaveOperationResult(
                            type = PvzSaveOperationType.DELETE,
                            isSuccess = false,
                            message = "存档目录删除失败：${saveDir.absolutePath}"
                        )
                    }
                }
            } ?: return PvzSaveOperationResult(
                type = PvzSaveOperationType.DELETE,
                isSuccess = false,
                message = "未找到指定存档：$saveId"
            )

            // 后删除元数据文件
            val metaFile = File(rootDir, "${saveId}_$LOCAL_SAVE_META_FILE")
            if (metaFile.exists()) {
                metaFile.delete()
            }

            // 触发UI刷新
            triggerRefresh()

            PvzSaveOperationResult(
                type = PvzSaveOperationType.DELETE,
                isSuccess = true,
                message = saveConfig.deleteSuccessTip // 读取配置
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PvzSaveOperationResult(
                type = PvzSaveOperationType.DELETE,
                isSuccess = false,
                message = String.format(saveConfig.deleteFailTipPrefix, e.message ?: "未知错误"), // 读取配置
                exception = e
            )
        }
    }

    /**
     * 生成唯一存档ID（优化算法）
     */
    fun generateSaveId(): String {
        val time = System.currentTimeMillis()
        val random = Random().nextInt(9999)
        return "${time}_$random"
    }

    // ======================== 存档刷新触发器相关方法 ========================
    /**
     * 触发存档列表刷新（通知所有监听者）
     */
    fun triggerRefresh() {
        refreshCounter++
        refreshListeners.forEach { it(refreshCounter) }
    }

    /**
     * 注册存档刷新监听器
     */
    fun addRefreshListener(listener: (Long) -> Unit) {
        refreshListeners.add(listener)
        // 立即触发一次刷新，确保初始状态正确
        listener(refreshCounter)
    }

    /**
     * 移除存档刷新监听器
     */
    fun removeRefreshListener(listener: (Long) -> Unit) {
        refreshListeners.remove(listener)
    }
}