package io.github.dreammooncai.pvz2tool

import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.dreammooncai.pvz2tool.js.JsConsole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 探测 keight 引擎原生内置了哪些标准全局标识符。
 *
 * 本测试**自包含**：自己创建一个纯净 runtime（不挂载任何 PvzTool 自定义全局、
 * 不加 globalThis 别名），因此结果 100% 反映引擎自身能力，可用于判断脚本依赖的
 * 标准函数（parseInt / atob / setTimeout …）是否可直接用，还是需要我们手动补齐。
 * 探针逻辑只存在于此测试内，不污染正式源码。
 *
 * 注意：keight 不会把全局变量暴露为 globalThis 的自有属性
 * （`Object.getOwnPropertyNames(globalThis)` 返回空数组），故采用「手挑标准清单 +
 * 显式 typeof 求值」而非枚举方案。
 *
 * 运行：./gradlew :composeApp:testDebugUnitTest --tests "*BuiltinGlobalsProbeTest*"
 */
class BuiltinGlobalsProbeTest {

    @Test
    fun probeEngineBuiltins() = runBlocking {
        val runtime = JSRuntime(
            context = Dispatchers.Default + SupervisorJob(),
            console = JsConsole
        )
        val engine = JSEngine(runtime)

        // engine.compile(...).invoke() 是挂起调用，故 typeofOf 必须是 suspend fun
        suspend fun typeofOf(name: String): String =
            runCatching { runtime.toString(engine.compile("typeof $name").invoke()) }
                .getOrDefault("undefined")

        val candidates = listOf(
            // 值属性
            "NaN", "Infinity",
            // 函数属性（宿主环境常见）
            "eval", "isFinite", "isNaN", "parseFloat", "parseInt",
            "decodeURI", "decodeURIComponent", "encodeURI", "encodeURIComponent",
            "escape", "unescape", "atob", "btoa",
            // 基础对象
            "Object", "Function", "Boolean", "Symbol", "Error",
            "EvalError", "RangeError", "ReferenceError", "SyntaxError",
            "TypeError", "URIError", "AggregateError", "Promise",
            // 文本 / 正则
            "String", "RegExp",
            // 类型化数组 / 缓冲
            "Array", "ArrayBuffer", "SharedArrayBuffer", "DataView",
            "Uint8Array", "Uint8ClampedArray", "Int8Array", "Uint16Array",
            "Int16Array", "Uint32Array", "Int32Array", "Float32Array", "Float64Array",
            "BigInt64Array", "BigUint64Array", "BigInt",
            // 集合
            "Map", "Set", "WeakMap", "WeakSet",
            // 数值 / 时间
            "Number", "Math", "Date",
            // 结构化
            "JSON",
            // 定时器 / 控制（宿主环境，纯引擎通常没有）
            "setTimeout", "clearTimeout", "setInterval", "clearInterval",
            "queueMicrotask", "setImmediate",
            // 宿主别名
            "globalThis", "window", "self", "console", "print",
            "alert", "prompt", "document", "fetch", "navigator", "XMLHttpRequest"
        )
        val result = LinkedHashMap<String, String>()
        for (name in candidates) result[name] = typeofOf(name)

        val builtin = result.filter { it.value != "undefined" }.toSortedMap()
        val missing = result.filter { it.value == "undefined" }.keys.sorted()
        println("keight 引擎内置全局探测（纯净 runtime，未挂载 PvzTool 自定义全局）")
        println("== 内置（typeof 非 undefined）==")
        builtin.forEach { (k, v) -> println("  $k : $v") }
        println("== 缺失（typeof undefined，脚本需谨慎或手动补齐）==")
        missing.forEach { println("  $it") }

        // 关键断言：引擎应原生内置这些；缺失的（atob / decodeURI* 等）由我们手动补齐
        assertTrue(typeofOf("globalThis") == "object", "globalThis 必须被引擎内置")
        assertTrue(typeofOf("JSON") == "object", "JSON 应内置")
        assertTrue(typeofOf("parseInt") == "function", "parseInt 应内置")
        println("[probe] atob typeof = ${typeofOf("atob")} （应为 undefined -> 由我们补齐）")
        println("[probe] decodeURIComponent typeof = ${typeofOf("decodeURIComponent")} （应为 undefined -> 由我们补齐）")
    }
}
