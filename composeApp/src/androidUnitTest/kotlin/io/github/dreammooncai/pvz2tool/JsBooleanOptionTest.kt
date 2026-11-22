package io.github.dreammooncai.pvz2tool

import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.js.toBoolean
import io.github.dreammooncai.pvz2tool.js.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 回归测试：JS options 里的布尔值必须能被 `orNull?.toBoolean()` 正确解析。
 *
 * 历史 bug：`toBoolean()` 曾写成 `toNumber() == 1`，而 keight 的 `toNumber()` 返回
 * `Number`（运行时是 `Long`），Kotlin 里 `Long(1) == Int(1)` 恒 false → 所有布尔 option
 * （reorderable / cancelable / showIndex / selectAll …）静默失效，拖动排序等完全无反应。
 *
 * 本测试用真实的 keight runtime + `JsObject.get()` 路径取值，防止再次回归。
 */
class JsBooleanOptionTest {
    @Test
    fun booleanOptionsParseCorrectly() = runBlocking {
        val runtime = JSRuntime(context = Dispatchers.Default + SupervisorJob(), console = JsConsole)
        val engine = JSEngine(runtime)
        with(runtime) {
            suspend fun eval(script: String): JsAny? = engine.compile(script).invoke()

            val trueVal = eval("true")
            val falseVal = eval("false")
            assertTrue(trueVal.orNull?.toBoolean() == true, "JS true 必须解析为 true")
            assertFalse(falseVal.orNull?.toBoolean() == true, "JS false 必须解析为 false")

            // 真实路径：options 对象 → get → orNull → toBoolean
            val obj = eval("({reorderable:true, cancelable:false, showIndex:true})") as JsObject
            val reorderable = obj.get("reorderable".js).orNull?.toBoolean() ?: false
            val cancelable = obj.get("cancelable".js).orNull?.toBoolean() ?: false
            val showIndex = obj.get("showIndex".js).orNull?.toBoolean() ?: false
            assertTrue(reorderable, "reorderable:true 必须为 true")
            assertFalse(cancelable, "cancelable:false 必须为 false")
            assertTrue(showIndex, "showIndex:true 必须为 true")
        }
    }
}
