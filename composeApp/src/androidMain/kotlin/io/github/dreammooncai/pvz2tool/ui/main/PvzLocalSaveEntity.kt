package io.github.dreammooncai.pvz2tool.ui.main

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ======================== 2. 数据模型扩展 ========================
// 存档实体类（添加序列化注解）
@Serializable
data class PvzLocalSaveEntity(
    val id: String,
    val name: String,
    val desc: String,
    val savePath: String,
    val createTime: String = "${System.currentTimeMillis()}"
) {
    /**
     * 格式化创建时间（适配SDK21+）
     */
    fun getFormattedCreateTime(): String {
        return try {
            val time = createTime.toLong()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(time))
        } catch (e: Exception) {
            "未知时间"
        }
    }
}