package io.github.dreammooncai.pvz2tool.js.code

import android.content.Context
import android.widget.Toast
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 轻提示（Toast）全局对象：`toast`。
 *
 * 在脚本中弹出一个 Android 系统 Toast。因 JS 引擎运行于后台线程
 * （`Dispatchers.Default`），所有 Toast 都会切回主线程（`Dispatchers.Main`）
 * 后再显示，避免「非主线程调用 Toast」导致崩溃；失败时静默吞掉，不影响脚本。
 *
 * 用法：
 * ```js
 * // 短提示（默认）
 * toast.show("保存成功");
 * toast.吐司("操作完成");
 *
 * // 长提示
 * toast.show("正在加载资源...", "long");
 * toast.long("这条会停留久一点");
 *
 * // 也可传数字：0 = 短，其它（如 1）= 长
 * toast.show("提示", 1);
 * toast.short("短提示");
 * ```
 *
 * 注意：原生 Toast 仅支持「短 / 长」两档时长，不支持任意毫秒；若需自定义时长需自行
 * 实现自定义视图，本对象暂不提供。
 */
object JsToast {

    private val context: Context
        get() = InitializePvz2.context

    /**
     * 在主线程显示一个 Toast。JS 引擎线程非主线程，必须切回主线程构造并 show。
     *
     * @param message  提示文本；空白直接忽略（不弹）
     * @param duration [Toast.LENGTH_SHORT] 或 [Toast.LENGTH_LONG]
     */
    private suspend fun showToast(message: String, duration: Int) {
        if (message.isBlank()) return
        val ctx = context
        withContext(Dispatchers.Main) {
            runCatching {
                Toast.makeText(ctx, message, duration).show()
            }
        }
    }

    val js = Object("toast") {
        // 显示 Toast：toast.show(message, duration?) -> void
        // duration 可省略（默认短）；可传 "short"/"短"/0 或 "long"/"长"/1
        listOf("show".js, "显示".js, "提示".js, "吐司".js).func(
            FunctionParam("message"), FunctionParam("duration")
        ) { args ->
            val message = args.getOrNull(0).orNull?.let { toString(it) } ?: ""
            val duration = args.getOrNull(1).orNull?.let { arg ->
                val s = toString(arg).trim().lowercase()
                when {
                    s == "long" || s == "长" -> Toast.LENGTH_LONG
                    s == "short" || s == "短" -> Toast.LENGTH_SHORT
                    s.toIntOrNull()?.let { it != 0 } == true -> Toast.LENGTH_LONG
                    s.toDoubleOrNull()?.let { it != 0.0 } == true -> Toast.LENGTH_LONG
                    else -> Toast.LENGTH_SHORT
                }
            } ?: Toast.LENGTH_SHORT
            showToast(message, duration)
            null
        }

        // 便捷：短提示 toast.short(message) / toast.短(message)
        listOf("short".js, "短".js).func(FunctionParam("message")) { args ->
            val message = args.getOrNull(0).orNull?.let { toString(it) } ?: ""
            showToast(message, Toast.LENGTH_SHORT)
            null
        }

        // 便捷：长提示 toast.long(message) / toast.长(message)
        listOf("long".js, "长".js).func(FunctionParam("message")) { args ->
            val message = args.getOrNull(0).orNull?.let { toString(it) } ?: ""
            showToast(message, Toast.LENGTH_LONG)
            null
        }
    }
}
