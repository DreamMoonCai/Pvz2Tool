package io.github.dreammooncai.pvz2tool.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ObjectScope
import io.github.alexzhirkevich.keight.js.Undefined

/** 如果 JsAny 是 Undefined，返回 null；否则返回原值 */
val JsAny?.orNull: JsAny?
    get() = if (this is Undefined) null else this

context(runtime: ScriptRuntime)
suspend fun JsAny?.toNumber() = runtime.toNumber(this)

context(runtime: ScriptRuntime)
suspend fun JsAny?.toBoolean() = toNumber() == 1

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