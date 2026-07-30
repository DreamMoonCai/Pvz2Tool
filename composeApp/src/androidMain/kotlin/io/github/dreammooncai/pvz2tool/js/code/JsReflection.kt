package io.github.dreammooncai.pvz2tool.js.code

import com.highcapable.yukireflection.factory.constructor
import com.highcapable.yukireflection.factory.field
import com.highcapable.yukireflection.factory.method
import com.highcapable.yukireflection.factory.toClass
import dalvik.system.DexClassLoader
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import java.io.File
import java.net.URL
import kotlin.reflect.KClass

/**
 * DEX 加载与反射 API（基于 YukiReflection 风格）。
 *
 * 提供两个全局对象：
 * - `dex`：加载外部 .dex / .apk / .jar 到独立 [DexClassLoader]，返回可复用的类加载器句柄，
 *   句柄可传给 `reflect.findClass(name, loader)` 以反射 DEX 内的类。
 * - `reflect`：按类名取得 [Class] 的 JS 包装，并提供 YukiReflection 风格的类 / 方法 / 字段 /
 *   构造器 / 实例操作（链式调用）。
 *
 * 句柄互通机制（基于 keight 的 [Wrapper] 接口，无需 id 注册表）：
 * - keight 中每个可跨语言的对象都实现 `Wrapper<T>`（[io.github.alexzhirkevich.keight.Wrapper]），
 *   把原始 Java 对象藏在 `value` 里，并 override `toKotlin` 返回 `value`。
 * - 本文件的 [JsClassWrapper] / [JsInstanceWrapper] / [JsLoaderWrapper] 均继承 [JsObjectImpl] 并
 *   实现 `Wrapper<T>`，`toKotlin` 直接返回被包装的原始对象（Class / 实例 / ClassLoader）。
 * - 这三个句柄类内部均以一个背后字段 `js = Object { ... }` 定义全部 JS 属性与方法（沿用项目通用的
 *   `.func { }` DSL），`get()` 直接转调 `js.get(...)`，从而兼顾 `Wrapper` 的 unwrap 能力与代码可读性。
 * - 因此 JS 把句柄作为实参回传时，`convertArg` 只需 `arg.toKotlin(runtime)` 即可还原原始 Java 对象，
 *   从而支持「拿实例再调用其方法 / 读写其字段」「把实例作为另一方法的参数」等链式用法——无需任何
 *   全局注册表或 id 属性。
 *
 * 所有 YukiReflection 调用均包在 `runCatching` 中，失败静默返回 null，不中断脚本。
 *
 * ⚠️ 关于 `.func` 的形参声明（重要）：
 * keight 的 `func` 会用 `FunctionParam` 列表声明 JS 函数的形参，body 收到的 `args` 列表**长度恰好等于
 * 声明的形参个数**；不写形参（`.func { args -> }`）则 `args` 永远是空列表，JS 传入的实参会全部丢失。
 * 因此本文件中所有「需要读取 JS 实参」的 `.func` 都必须显式声明形参：固定个数用 `func("a","b"){...}`，
 * 不定长（如 `call/invoke/newInstance`）用 `func(FunctionParam("args", isVararg = true)){...}` 并通过
 * [flattenVarargs] 把单个「JS 数组」元素还原成扁平的实参列表。
 */
private val defaultClassLoader: ClassLoader
    get() = InitializePvz2.context.classLoader

/**
 * 把 vararg 形式的实参还原成扁平的 [JsAny?] 列表。
 *
 * keight 中声明 `isVararg = true` 的形参，body 收到的 `args` 只含一个元素，其值为「全部 JS 实参」
 * 组成的 JS 数组（见 `SimpleFunctionParam.set` 的 `arguments.drop(index).js`）。该 JS 数组在 keight 里
 * 即 `JsArrayWrapper`，而 `JsArrayWrapper` 通过 `MutableList<JsAny?> by value` **本身就是一个
 * `List<JsAny?>`**，其元素就是原始的 [JsAny] 包装（含我们自定义的 [Wrapper] 子类，如 [JsClassWrapper]、
 * [JsInstanceWrapper]）。
 *
 * ⚠️ 关键点：绝不可对数组调用 [JsAny.toKotlin] —— 那会（见 `JsArrayWrapper.toKotlin`）递归把每个元素都
 * 转成原生 Kotlin 值，从而**剥掉 [Wrapper] 身份**，导致后续 `filterIsInstance<JsAny>()` 恒为 0、实例 /
 * 类句柄全部丢失、反射调用拿不到任何实参。直接把数组当作 `List<JsAny?>` 取出即可完整保留每个元素的包装
 * 身份，后续由 [convertArg] / [inferTypes] 按需逐个 `toKotlin` 还原。
 * 若 `args` 不是单元素 vararg 形态（理论上不会发生），则原样返回。
 */
private fun flattenVarargs(args: List<JsAny?>): List<JsAny?> {
    val first = args.getOrNull(0)
    // 仅当「单元素 + 该元素是 JS 数组（实现了 List）」时才视为 vararg 展开；
    // JsArrayWrapper 实现了 List<JsAny?>，其元素本身就是未转换的原始 JsAny 包装。
    if (first != null && args.size == 1 && first is List<*>) {
        @Suppress("UNCHECKED_CAST")
        return first as List<JsAny?>
    }
    return args
}

/**
 * 把 JS 侧的类型名解析为 [Class]。[loader] 非 null 时用于解析自定义/DEX 内的类。
 * 支持基础类型缩写（int/long/.../boolean/string 等）与完整类名（走 [toClass]）。
 */
private fun resolveType(name: String, loader: ClassLoader?): Class<*>? = runCatching {
    when (name.lowercase().trim()) {
        // 短名一律解析为基本类型（int.class 等）；装箱类型请用全限定名（如 java.lang.Integer）。
        // YukiReflection 的 param 按 Class 精确匹配、不会在基本/装箱间自动转换（见 KReflectionTool.typeEq），
        // 因此必须解析成基本型才能匹配声明为 int 的方法；装箱版由下方 numericCandidates 的候选覆盖。
        "int" -> Int::class.javaPrimitiveType
        "integer" -> Int::class.java
        "long" -> Long::class.javaPrimitiveType
        "double" -> Double::class.javaPrimitiveType
        "float" -> Float::class.javaPrimitiveType
        "short" -> Short::class.javaPrimitiveType
        "byte" -> Byte::class.javaPrimitiveType
        "boolean", "bool" -> Boolean::class.javaPrimitiveType
        "char", "character" -> Char::class.javaPrimitiveType
        "void" -> Void::class.javaPrimitiveType
        "string" -> String::class.java
        else -> name.trim().toClass(loader ?: defaultClassLoader, false)
    }
}.getOrNull()

/**
 * 基本类型与其装箱类型的双向对应表。用于 [numericCandidates] 生成「基本↔装箱」候选。
 */
// ⚠️ 关键坑：Kotlin 里 `Int::class.java` 返回的是基本型 `int`（primitive Class），
// 而 `inferTypes` 经由 `v.javaClass` 还原 JS 数字得到的是**装箱型**（java.lang.Long / java.lang.Integer / java.lang.Double …）。
// 因此 counterpartMap 与 isIntegerLike/isDecimalLike 必须同时包含「装箱型(javaObjectType)」与「基本型(javaPrimitiveType)」，
// 否则装箱 Long 永远匹配不上基本型集合，numericCandidates 就生成不出 [int,int]，实例方法推断调用会整体失败（返回 null）。
private val counterpartMap: Map<Class<*>, Class<*>> = run {
    val pairs = listOf(
        Boolean::class.javaObjectType to (Boolean::class.javaPrimitiveType ?: Boolean::class.javaObjectType),
        Byte::class.javaObjectType to (Byte::class.javaPrimitiveType ?: Byte::class.javaObjectType),
        Char::class.javaObjectType to (Char::class.javaPrimitiveType ?: Char::class.javaObjectType),
        Short::class.javaObjectType to (Short::class.javaPrimitiveType ?: Short::class.javaObjectType),
        Int::class.javaObjectType to (Int::class.javaPrimitiveType ?: Int::class.javaObjectType),
        Long::class.javaObjectType to (Long::class.javaPrimitiveType ?: Long::class.javaObjectType),
        Float::class.javaObjectType to (Float::class.javaPrimitiveType ?: Float::class.javaObjectType),
        Double::class.javaObjectType to (Double::class.javaPrimitiveType ?: Double::class.javaObjectType),
        Void::class.javaObjectType to (Void::class.javaPrimitiveType ?: Void::class.javaObjectType)
    )
    buildMap { pairs.forEach { (boxed, prim) -> put(boxed, prim); put(prim, boxed) } }
}

private fun KClass<*>.isIntegerLike(): Boolean = this in setOf(
    Int::class,
    Long::class,
    Short::class,
    Byte::class
)
private fun KClass<*>.isDecimalLike(): Boolean = this in setOf(
    Double::class,
    Float::class
)

/**
 * 根据「用户显式指定的参数类型」或「推断出的参数类型」生成一组待尝试的参数类型列表。
 *
 * YukiReflection 的 [param] 按 [Class] 精确匹配、不会在基本/装箱/整型家族间自动转换，
 * 而 keight 把 JS 数字统一还原成 `Long`/`Double`；因此：
 * - 显式类型：`[用户类型]` + 其装箱/基本对应 + 整型家族（int/Integer/long/Long）+ 浮点家族（double/Double/float）候选；
 * - 推断类型：先用 [inferTypes] 推出 JS 实参的真实 Kotlin 类型，再做同样扩展。
 * 调用方逐个候选尝试，命中即止；命中后用 [coerceArg] 把实参值强制收敛到该候选类型，避免
 * 出现 `argument N has type int, got java.lang.Long` 这类 Kotlin 反射严格类型校验失败。
 */
private fun numericCandidates(types: List<Class<*>>): List<List<Class<*>>> {
    if (types.isEmpty()) return listOf(emptyList())
    val variants = mutableListOf<List<Class<*>>>()
    variants += types
    variants += types.map { counterpartMap[it] ?: it }
    variants += types.map { if (it.kotlin.isIntegerLike()) Int::class.javaPrimitiveType!! else it }
    variants += types.map { if (it.kotlin.isIntegerLike()) Int::class.java else it }
    variants += types.map { if (it.kotlin.isIntegerLike()) Long::class.javaPrimitiveType!! else it }
    variants += types.map { if (it.kotlin.isIntegerLike()) Long::class.java else it }
    variants += types.map { if (it.kotlin.isDecimalLike()) Double::class.javaPrimitiveType!! else it }
    variants += types.map { if (it.kotlin.isDecimalLike()) Double::class.java else it }
    variants += types.map { if (it.kotlin.isDecimalLike()) Float::class.javaPrimitiveType!! else it }
    return variants.distinct()
}

context(runtime: ScriptRuntime)
private fun inferCandidates(args: List<JsAny?>): List<List<Class<*>>> {
    val inferred = inferTypes(args)
    if (inferred.isEmpty()) return listOf(emptyList())
    return numericCandidates(inferred)
}

/**
 * 依次尝试每组候选参数类型；任意一组命中（build 返回非 null）即返回结果，均失败返回 null。
 */
private inline fun <R> findWithCandidates(
    candidates: List<List<Class<*>>>,
    crossinline build: (List<Class<*>>) -> R
): R? {
    for (c in candidates) {
        val r = runCatching { build(c) }.getOrNull()
        if (r != null) return r
    }
    return null
}

/**
 * 把 JS 侧还原出的 Kotlin 实参值，强制收敛到目标参数类型 [target]，以满足 Kotlin/Java 反射的严格类型校验。
 *
 * - 数字：按目标整型/浮点类型做 toInt/toLong/...（keight 把 JS 数字统一还原成 Long/Double，而反射目标常是 int，
 *   故 `Long -> Int` 这种收敛是修复 `argument N has type int, got java.lang.Long` 的关键）；
 * - String/CharSequence：仅当值本身可转字符串时转；保留原始实例（不把对象 toString 后误传）；
 * - 其余（实例句柄、Class 句柄等）原样返回。
 */
private fun coerceArg(value: Any?, target: KClass<*>): Any? {
    if (value == null) return null
    val t = target
    if (value is Number) {
        return when (t) {
            Int::class -> value.toInt()
            Long::class -> value.toLong()
            Double::class -> value.toDouble()
            Float::class -> value.toFloat()
            Short::class -> value.toShort()
            Byte::class -> value.toByte()
            Char::class -> value.toInt().toChar()
            else -> value
        }
    }
    if (value is Boolean && (t == Boolean::class)) return value
    if (value is CharSequence) return value.toString()
    return value
}

/**
 * JS 实参 → Kotlin 值。keight 的 [Wrapper] 机制会在 [JsAny.toKotlin] 时还原出原始 Java 对象
 * （Class / 实例 / ClassLoader 等），因此这里只需直接调用 [JsAny.toKotlin] 即可，无需任何 id 查找。
 */
context(runtime: ScriptRuntime)
private fun convertArg(arg: JsAny?): Any? = arg?.toKotlin(runtime)

/**
 * 任意 Kotlin 值 → keight 官方原生 JS 表示（严格对齐 `Mapping.kt`）。
 * 用于 [JsInstanceWrapper] 的 `value`/`原值`/`js` 字段：把被包装的原始对象转成 JS 原生值
 * （String→原生字符串、List→JS 数组、Set→JS 集合、Map→JS 对象、数组→JS 数组、数字/Boolean→原生、Regex→JsRegexWrapper、Throwable→Error）。
 * 自定义对象无原生映射时兜底为字符串表示。
 */
private fun toNativeJs(v: Any?): JsAny? {
    if (v == null) return null
    return when (v) {
        is JsAny -> v
        is Number -> v.js
        is Boolean -> v.js
        is CharSequence -> v.js                      // String / StringBuilder / StringBuffer → 原生字符串
        is Char -> v.toString().js
        is Regex -> v.js
        is Throwable -> v.js
        is List<*> -> v.map { toNativeJs(it) }.js
        is Set<*> -> v.map { toNativeJs(it) }.toSet().js
        is Map<*, *> -> v.entries.associate { (k, vv) -> toNativeJs(k) to toNativeJs(vv) }.js
        is IntArray, is LongArray, is FloatArray, is DoubleArray, is ShortArray, is ByteArray, is Array<*> -> {
            val arr = v as Any
            val len = java.lang.reflect.Array.getLength(arr)
            val list = ArrayList<JsAny?>(len)
            repeat(len) { i -> list.add(toNativeJs(java.lang.reflect.Array.get(arr, i))) }
            list.js
        }
        else -> instanceWrapper(v, v::class.java.classLoader)
    }
}

/**
 * Kotlin 值 → JS 值（极简、统一）：
 * - [JsAny] → 原样返回（已是 JS 值）；
 * - [Class] → Class 句柄（可继续 `.method`/`.field`）；
 * - [Number] / [Boolean] → keight 原生值（`value.js`），数字 `===` 按值比较可用；
 * - **其余一切引用类型**（`String`、[List]/[Set]/[Map]、数组、[Regex]、[Throwable]、自定义类、`StringBuffer` 等）
 *   → 一律 [instanceWrapper]，保留 `.call`/`.get`/`.set`，可继续反射；
 *   其 JS 原生表示通过 `value`/`原值`/`js` 字段取得（内部走 [toNativeJs]）。
 *
 * 规则只有一条「是不是数字/Class」，与具体类型无关，简单且无遗漏类型。
 * - null → null。
 */
private fun convertResult(value: Any?, loader: ClassLoader? = null): JsAny? {
    if (value == null) return null
    return when (value) {
        is JsAny -> value
        is Class<*> -> classWrapper(value, loader)
        is Number -> value.js                          // 数字原生（=== 按值比较可用）
        is Boolean -> value.js                         // JSBooleanWrapper
        else -> instanceWrapper(value, loader)         // 其余全部 → JsInstanceWrapper，保留 .call；原生值经 .value 取得
    }
}

context(runtime: ScriptRuntime)
private fun parseTypeList(arg: JsAny?, loader: ClassLoader?): List<Class<*>>? {
    if (arg == null) return null
    val list = arg.toKotlin(runtime) as? List<*> ?: return null
    return list.mapNotNull { item ->
        val s = item?.toString()?.trim() ?: return@mapNotNull null
        resolveType(s, loader)
    }
}

context(runtime: ScriptRuntime)
private fun inferTypes(args: List<JsAny?>): List<Class<*>> = args.mapNotNull { arg ->
    when (val v = arg?.toKotlin(runtime)) {
        is Class<*> -> v
        is Boolean -> Boolean::class.java
        is String -> String::class.java
        is Number -> v.javaClass          // Int/Long/Double/Float/Short/Byte → 各自装箱 Class；基本↔装箱/整型家族回退由 numericCandidates 负责
        else -> null
    }
}

// ======================== Class 句柄（Wrapper 子类） ========================

private fun classWrapper(clazz: Class<*>, loader: ClassLoader? = null): JsObject = JsClassWrapper(clazz, loader)

private class JsClassWrapper(
    private val clazz: Class<*>,
    private val loader: ClassLoader? = null
) : JsObjectImpl("class"), Wrapper<Class<*>> {

    override val value: Class<*> get() = clazz
    override fun toKotlin(runtime: ScriptRuntime): Any = clazz

    // 全部 JS 属性/方法定义在一个背后字段里，复用项目通用的 .func DSL
    private val js = Object("class") {
        listOf("name".js, "类名".js) eq clazz.name.js
        listOf("simpleName".js, "短名".js) eq (clazz.simpleName ?: "").js
        // method(name, [types?])：声明固定形参，body 才能拿到 JS 实参
        listOf("method".js, "方法".js).func("name", "types") { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val types = parseTypeList(args.getOrNull(1), loader)
            methodWrapper(clazz, name, types, loader)
        }
        // field(name)
        listOf("field".js, "字段".js).func("name") { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            fieldWrapper(clazz, name, loader)
        }
        // constructor([types?])
        listOf("constructor".js, "构造器".js, "构造".js).func("types") { args ->
            val types = parseTypeList(args.getOrNull(0), loader)
            constructorWrapper(clazz, types, loader)
        }
        // newInstance(...args)：不定长实参，用 vararg
        listOf("newInstance".js, "新建".js, "实例化".js).func(FunctionParam("args", isVararg = true)) { args ->
            val all = flattenVarargs(args)
            val cargs = all.map { convertArg(it) }.toTypedArray()
            findWithCandidates(inferCandidates(all)) { ts ->
                val finder = clazz.constructor {
                    if (ts.isEmpty()) emptyParam() else param(*ts.toTypedArray())
                }
                val proxy = finder.get()
                val coerced = cargs.mapIndexed { i, a -> coerceArg(a, ts.getOrNull(i)?.kotlin ?: Any::class) }.toTypedArray()
                convertResult(proxy.call(*coerced), loader)
            }
        }
        listOf("getSuperclass".js, "父类".js, "超类".js).func { _ ->
            clazz.superclass?.let { classWrapper(it, loader) }
        }
        listOf("getDeclaredMethods".js, "方法列表".js).func { _ ->
            runCatching { clazz.declaredMethods.map { it.name }.distinct().map { it.js } }
                .getOrNull()?.let { listOf(*it.toTypedArray()).js }
        }
        listOf("getDeclaredFields".js, "字段列表".js).func { _ ->
            runCatching { clazz.declaredFields.map { it.name }.distinct().map { it.js } }
                .getOrNull()?.let { listOf(*it.toTypedArray()).js }
        }
        listOf("toString".js).func { _ -> clazz.name.js }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? = js.get(property, runtime)
}

// ======================== Method / Field / Constructor 句柄（接收者，不回传） ========================

private fun methodWrapper(clazz: Class<*>, name: String, types: List<Class<*>>?, loader: ClassLoader?): JsObject {
    return Object("method") {
        listOf("name".js, "方法名".js) eq name.js
        // call(instance?, ...args)：不定长实参，用 vararg
        listOf("call".js, "调用".js).func(FunctionParam("args", isVararg = true)) { args ->
            val all = flattenVarargs(args)
            val instance = all.getOrNull(0)?.let { convertArg(it) }
            val methodArgs = all.drop(1)
            val candidates = if (types != null) numericCandidates(types) else inferCandidates(methodArgs)
            val margs = methodArgs.map { convertArg(it) }.toTypedArray()
            findWithCandidates(candidates) { ts ->
                val finder = clazz.method {
                    this.name = name
                    if (ts.isNotEmpty()) param(*ts.toTypedArray())
                }
                val proxy = finder.get(instance)
                val coerced = margs.mapIndexed { i, a -> coerceArg(a, ts.getOrNull(i)?.kotlin ?: Any::class) }.toTypedArray()
                convertResult(proxy.call(*coerced), loader)
            }
        }
        // invoke(...args)：静态调用（instance = null），不定长实参
        listOf("invoke".js, "执行".js).func(FunctionParam("args", isVararg = true)) { args ->
            val all = flattenVarargs(args)
            val candidates = if (types != null) numericCandidates(types) else inferCandidates(all)
            val margs = all.map { convertArg(it) }.toTypedArray()
            findWithCandidates(candidates) { ts ->
                val finder = clazz.method {
                    this.name = name
                    if (ts.isNotEmpty()) param(*ts.toTypedArray())
                }
                val proxy = finder.get(null)
                val coerced = margs.mapIndexed { i, a -> coerceArg(a, ts.getOrNull(i)?.kotlin ?: Any::class) }.toTypedArray()
                convertResult(proxy.call(*coerced), loader)
            }
        }
    }
}

private fun fieldWrapper(clazz: Class<*>, name: String, loader: ClassLoader?): JsObject {
    return Object("field") {
        listOf("name".js, "字段名".js) eq name.js
        // get(instance?)
        listOf("get".js, "读取".js, "获取".js).func("instance") { args ->
            val instance = args.getOrNull(0)?.let { convertArg(it) }
            runCatching { convertResult(clazz.field { this.name = name }.get(instance).any(), loader) }.getOrNull()
        }
        // set(instance?, value)
        listOf("set".js, "写入".js, "设置".js).func("instance", "value") { args ->
            val instance = args.getOrNull(0)?.let { convertArg(it) }
            val value = args.getOrNull(1)?.let { convertArg(it) }
            runCatching { clazz.field { this.name = name }.get(instance).set(value); null }.getOrNull()
        }
    }
}

private fun constructorWrapper(clazz: Class<*>, types: List<Class<*>>?, loader: ClassLoader?): JsObject {
    return Object("constructor") {
        // newInstance(...args)：不定长实参，用 vararg
        listOf("newInstance".js, "新建".js, "实例化".js).func(FunctionParam("args", isVararg = true)) { args ->
            val all = flattenVarargs(args)
            val candidates = if (types != null) numericCandidates(types) else inferCandidates(all)
            val cargs = all.map { convertArg(it) }.toTypedArray()
            findWithCandidates(candidates) { ts ->
                val finder = clazz.constructor {
                    if (ts.isEmpty()) emptyParam() else param(*ts.toTypedArray())
                }
                val proxy = finder.get()
                val coerced = cargs.mapIndexed { i, a -> coerceArg(a, ts.getOrNull(i)?.kotlin ?: Any::class) }.toTypedArray()
                convertResult(proxy.call(*coerced), loader)
            }
        }
    }
}

// ======================== 实例句柄（Wrapper 子类） ========================

private fun instanceWrapper(obj: Any, loader: ClassLoader? = null): JsObject = JsInstanceWrapper(obj, loader)

private class JsInstanceWrapper(
    private val obj: Any,
    private val loader: ClassLoader? = null
) : JsObjectImpl("instance"), Wrapper<Any> {

    override val value: Any get() = obj
    override fun toKotlin(runtime: ScriptRuntime): Any = obj

    private val js = Object("instance") {
        // call(methodName, ...args)：不定长实参，用 vararg
        listOf("call".js, "调用方法".js).func(FunctionParam("args", isVararg = true)) { args ->
            val all = flattenVarargs(args)
            val methodName = all.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val methodArgs = all.drop(1)
            val candidates = inferCandidates(methodArgs)
            val margs = methodArgs.map { convertArg(it) }.toTypedArray()
            runCatching {
                for (ts in candidates) {
                    val r = runCatching {
                        val finder = obj.javaClass.method {
                            this.name = methodName
                            if (ts.isNotEmpty()) param(*ts.toTypedArray())
                        }
                        val proxy = finder.get(obj)
                        val coerced = margs.mapIndexed { i, a -> coerceArg(a, ts.getOrNull(i)?.kotlin ?: Any::class) }.toTypedArray()
                        convertResult(proxy.call(*coerced), loader)
                    }.getOrNull()
                    if (r != null) return@runCatching r
                }
                null
            }.getOrNull()
        }
        // get(fieldName)
        listOf("get".js, "读字段".js).func("fieldName") { args ->
            val fieldName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            runCatching { convertResult(obj.javaClass.field { this.name = fieldName }.get(obj).any(), loader) }.getOrNull()
        }
        // set(fieldName, value)
        listOf("set".js, "写字段".js).func("fieldName", "value") { args ->
            val fieldName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val value = args.getOrNull(1)?.let { convertArg(it) }
            runCatching { obj.javaClass.field { this.name = fieldName }.get(obj).set(value); null }.getOrNull()
        }
        listOf("getId".js, "取ID".js).func { _ -> System.identityHashCode(obj).js }
        listOf("getClass".js, "取类".js).func { _ -> obj.javaClass.let { classWrapper(it, loader) } }
        // value()/原值()/js()：取得被包装对象的 keight 原生 JS 值（String→原生字符串、List→JS 数组、数组→JS 数组…），零参方法
        listOf("value".js, "原值".js, "js".js) eq JsProperty { toNativeJs(obj) }
        listOf("toString".js).func { _ -> obj.toString().js }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? = js.get(property, runtime)
}

// ======================== DEX 类加载器句柄（Wrapper 子类） ========================

private fun loaderWrapper(loader: ClassLoader, path: String): JsObject = JsLoaderWrapper(loader, path)

private class JsLoaderWrapper(
    private val loader: ClassLoader,
    private val path: String
) : JsObjectImpl("dexLoader"), Wrapper<ClassLoader> {

    override val value: ClassLoader get() = loader
    override fun toKotlin(runtime: ScriptRuntime): Any = loader

    private val js = Object("dexLoader") {
        listOf("path".js, "路径".js) eq path.js
        // findClass(name)
        listOf("findClass".js, "查找类".js).func("name") { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            runCatching { classWrapper(name.toClass(loader, true), loader) }.getOrNull()
        }
        listOf("toString".js).func { _ -> "DexClassLoader($path)".js }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? = js.get(property, runtime)
}

// ======================== 全局对象：dex ========================

object JsDex {
    val js = Object("dex") {
        // 统一加载入口：路径规则与其余 API 完全一致（见 [io.github.dreammooncai.pvz2tool.js.JsFileAccess]）
        // - 绝对路径（/ 开头）→ 直接作为本地文件加载
        // - 相对路径 / $WORK_DIR 等占位符 → 走 JsFileAccess.resolveInput（工作目录优先，无则回退 assets/pvz2tool/）
        // - http(s):// URL → 先下载到缓存再加载
        // 第二个参数 parent 为可选父加载器句柄（即另一个 dex 对象）。
        listOf("load".js, "加载".js, "loadDex".js).func("path", "parent") { args ->
            val path = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val parent = resolveParent(args.getOrNull(1))
            runCatching {
                val dexFile = resolveDexFile(path)
                val optimized = File(InitializePvz2.context.cacheDir, "dex_opt_${System.currentTimeMillis()}").also {
                    it.mkdirs()
                    it.deleteOnExit() // 与其他缓存一致：进程退出时自动清理（API26+ 优化目录不被使用、恒为空，可正常删除）
                }
                val loader = DexClassLoader(dexFile.absolutePath, optimized.absolutePath, null, parent)
                loaderWrapper(loader, dexFile.absolutePath)
            }.getOrNull()
        }
    }

    private val ctx get() = InitializePvz2.context

    context(runtime: ScriptRuntime)
    private fun resolveParent(arg: JsAny?): ClassLoader {
        if (arg == null) return defaultClassLoader
        // 若传入的是另一个 dex 句柄（JsLoaderWrapper），toKotlin 会还原出原始 ClassLoader
        return arg.toKotlin(runtime) as? ClassLoader ?: defaultClassLoader
    }

    /**
     * 按统一路径规则解析出可加载的 DEX 文件，并复制到应用私有缓存、去掉写权限后返回。
     *
     * Android 要求被 [DexClassLoader] 加载的 DEX 文件不可被其他用户写入，否则抛
     * `SecurityException: Writable dex file ... is not allowed.`。
     * 应用进程 umask 通常为 0，经 [java.io.FileOutputStream] 写出的缓存文件权限为 0666（全员可写），
     * 因此这里统一复制到 `cache/dex_load/` 私有目录并置为 0444（仅可读）再加载——
     * 既规避安全异常，也不对用户原始文件产生写权限方面的副作用。
     */
    private fun resolveDexFile(path: String): File {
        val trimmed = path.trim()
        val src = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            downloadToCache(trimmed)
        } else {
            JsFileAccess.resolveInput(trimmed, ctx)?.file
                ?: error("无法解析 DEX 路径: $trimmed（请确认文件存在于工作目录或 assets/pvz2tool/，或使用 http(s):// URL）")
        }
        val dst = File(File(ctx.cacheDir, "dex_load"), "dex_${System.currentTimeMillis()}_${src.name}").also {
            it.parentFile?.mkdirs()
            src.inputStream().use { input -> it.outputStream().use { input.copyTo(it) } }
            it.setWritable(false, false)
            it.deleteOnExit() // 与其他缓存一致：进程退出时自动清理
        }
        return dst
    }

    private fun downloadToCache(url: String): File {
        val out = File(ctx.cacheDir, "dex_url_${System.currentTimeMillis()}.dex").also { it.parentFile?.mkdirs() }
        URL(url).openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        out.deleteOnExit() // 与其他缓存一致：进程退出时自动清理
        return out
    }

    /**
     * 清理 DEX 加载产生的临时缓存（[cacheDir]/dex_load/、dex_opt_*、dex_url_*），
     * 与项目其余缓存清理机制（[io.github.dreammooncai.pvz2tool.js.JsFileAccess.clearCache] 等）保持一致，
     * 在「重置数据」等生命周期里调用。
     */
    fun clearCache() {
        val root = ctx.cacheDir
        File(root, "dex_load").deleteRecursively()
        root.listFiles { f -> f.isDirectory && f.name.startsWith("dex_opt_") }?.forEach { it.deleteRecursively() }
        root.listFiles { f -> f.name.startsWith("dex_url_") && f.name.endsWith(".dex") }?.forEach { it.delete() }
    }
}

// ======================== 全局对象：reflect ========================

object JsReflect {
    val js = Object("reflect") {
        // 按类名取 Class 句柄；第二个参数为可选 DEX 加载器句柄（或其 ClassLoader），用于反射 DEX 内的类
        listOf("findClass".js, "查找类".js, "反射".js).func("name", "loader") { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val loader = args.getOrNull(1)?.toKotlin(this) as? ClassLoader
            runCatching { classWrapper(name.toClass(loader ?: defaultClassLoader, true), loader) }.getOrNull()
        }
    }
}
