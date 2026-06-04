package io.github.dreammooncai.pvz2tool.ui.main

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 存档操作类型枚举
 */
enum class PvzSaveOperationType {
    BACKUP,        // 备份存档
    EXPORT,        // 导出存档
    IMPORT,        // 导入存档
    DELETE,        // 删除存档
    COVER,         // 覆盖存档
    DELETE_GAME_SAVE, // 删除游戏存档
    SAVE_META      // 保存存档元数据
}

/**
 * 存档操作结果状态类
 * 统一封装所有存档操作的结果信息
 */
data class PvzSaveOperationResult(
    val type: PvzSaveOperationType,  // 操作类型
    val isSuccess: Boolean,          // 是否成功
    val message: String,             // 提示消息
    val exception: Exception? = null // 异常信息（可选）
)

/**
 * 存档操作状态管理类（适配Compose响应式状态）
 */
class PvzSaveOperationState {
    var result: PvzSaveOperationResult? by mutableStateOf(null)
    val showResultDialog: Boolean
        get() = result != null

    // 新增：用于等待弹窗关闭的 Continuation
    private var dismissContinuation: CancellableContinuation<Unit>? = null

    fun reset() {
        result = null
        dismissContinuation?.resume(Unit)
        dismissContinuation = null
    }

    fun postResult(result: PvzSaveOperationResult) {
        this.result = result
    }

    // 新增：挂起等待弹窗关闭
    suspend fun awaitDismiss() {
        if (result == null) return

        suspendCancellableCoroutine { cont ->
            dismissContinuation = cont
            cont.invokeOnCancellation {
                dismissContinuation = null
            }
        }
    }
}