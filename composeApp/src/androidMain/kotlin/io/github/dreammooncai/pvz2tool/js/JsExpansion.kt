package io.github.dreammooncai.pvz2tool.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.Undefined

/** 如果 JsAny 是 Undefined，返回 null；否则返回原值 */
val JsAny?.orNull: JsAny?
    get() = if (this is Undefined) null else this

context(runtime: ScriptRuntime)
suspend fun JsAny?.toNumber() = runtime.toNumber(this)

/**
 * JS 值 → 布尔（keight 包装对象专用）。
 *
 * 🔴 陷阱：绝不能写成 `toNumber() == 1`。keight 的 [toNumber] 返回 `Number`，
 * 运行时实际是 `Long(1)`；Kotlin 里 `Long(1) == Int(1)` 恒为 **false**（类型不同，
 * `Long.equals(Int)` 返回 false），`Number.equals(Int)` 同理。结果是所有布尔选项
 * （reorderable / cancelable / showIndex / selectAll / searchable …）静默变 false，
 * 且编译器不报错——曾导致整个拖动排序功能「毫无反应」且极难定位。
 *
 * 正确做法：先把 [toNumber] 的结果收敛到同一类型（这里统一转 Double）再判断。
 * 语义对齐 JS ToBoolean：非零即 true（不只是 1），NaN / null / undefined 为 false。
 */
context(runtime: ScriptRuntime)
suspend fun JsAny?.toBoolean(): Boolean? {
    if (this == null || this is Undefined) return null
    val n = toNumber().toDouble()
    return n != 0.0 && !n.isNaN()
}

context(runtime: ScriptRuntime)
suspend fun JsAny?.toBooleanOrThrowable(): Boolean = toBoolean() ?: error("没有可转换的必填值!")

context(runtime: ScriptRuntime)
suspend fun JsAny?.get(property: JsAny?): JsAny? = this?.get(property,runtime)

context(runtime: ScriptRuntime)
suspend fun JsAny?.toText(): String? {
    if (this == null || this is Undefined) return null
    return runtime.toString(this)
}

context(runtime: ScriptRuntime)
suspend fun JsAny?.toTextOrThrowable(): String = toText() ?: error("没有可转换的必填值!")

context(runtime: ScriptRuntime)
fun JsAny.toKotlin() = toKotlin(runtime)

context(scope: ObjectScope)
infix fun List<JsAny?>.eq(value: JsAny?) = with(scope) {
    forEach { key -> key eq value }
}

context(scope: ObjectScope)
fun List<JsAny?>.eq(value: JsAny?, writable: Boolean? = null, configurable: Boolean? = false, enumerable: Boolean? = null) = with(scope) {
    forEach { key -> key.eq(value, writable, configurable, enumerable) }
}

context(scope: ObjectScope)
fun List<JsAny?>.func(
    vararg args: FunctionParam,
    body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) = with(scope) {
    forEach { key -> key.func(*args, body = body) }
}

context(scope: ObjectScope)
fun List<JsAny?>.func(
    vararg args: String,
    params: (String) -> FunctionParam = { FunctionParam(it) },
    body: suspend ScriptRuntime.(args: List<JsAny?>) -> JsAny?
) = with(scope) {
    forEach { key -> key.func(*args, params = params, body = body) }
}