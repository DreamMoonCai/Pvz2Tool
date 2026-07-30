package io.github.dreammooncai.pvz2tool.js.code

import com.highcapable.yukireflection.factory.constructor
import com.highcapable.yukireflection.factory.field
import com.highcapable.yukireflection.factory.method
import com.highcapable.yukireflection.factory.toClass
import dalvik.system.DexClassLoader
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import java.io.File
import java.net.URL

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
 */
private val defaultClassLoader: ClassLoader
    get() = InitializePvz2.context.classLoader

/**
 * 把 JS 侧的类型名解析为 [Class]。[loader] 非 null 时用于解析自定义/DEX 内的类。
 * 支持基础类型缩写（int/long/.../boolean/string 等）与完整类名（走 [toClass]）。
 */
private fun resolveType(name: String, loader: ClassLoader?): Class<*>? = runCatching {
    when (name.lowercase().trim()) {
        // 短名一律解析为基本类型（int.class 等）；装箱类型请用全限定名（如 java.lang.Integer）。
        // YukiReflection 的 param 按 Class 精确匹配、不会在基本/装箱间自动转换（见 KReflectionTool.typeEq），
        // 因此必须解析成基本型才能匹配声明为 int 的方法；装箱版由下方 findWithTypeRetry 的回退覆盖。
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
 * 基本类型与其装箱类型的双向对应表。YukiReflection 的 [param] 按 [Class] 精确匹配，不会在基本类型与装箱类型之间自动转换
 * （见其 `KReflectionTool.typeEq`），因此用「int」解析出的原始 `int.class` 与声明为 `int` / `java.lang.Integer` 的方法签名
 * 需要分别尝试两侧——[findWithTypeRetry] 即负责对每条参数做「基本↔装箱」回退。
 */
private val counterpartMap: Map<Class<*>, Class<*>> = run {
    val pairs = listOf(
        Boolean::class.java to (Boolean::class.javaPrimitiveType ?: Boolean::class.java),
        Byte::class.java to (Byte::class.javaPrimitiveType ?: Byte::class.java),
        Char::class.java to (Char::class.javaPrimitiveType ?: Char::class.java),
        Short::class.java to (Short::class.javaPrimitiveType ?: Short::class.java),
        Int::class.java to (Int::class.javaPrimitiveType ?: Int::class.java),
        Long::class.java to (Long::class.javaPrimitiveType ?: Long::class.java),
        Float::class.java to (Float::class.javaPrimitiveType ?: Float::class.java),
        Double::class.java to (Double::class.javaPrimitiveType ?: Double::class.java),
        Void::class.java to (Void::class.javaPrimitiveType ?: Void::class.java)
    )
    buildMap { pairs.forEach { (boxed, prim) -> put(boxed, prim); put(prim, boxed) } }
}

private fun companionTypes(types: List<Class<*>>): List<Class<*>> = types.map { counterpartMap[it] ?: it }

/**
 * 用 [types] 构造 YukiReflection finder 并取结果；若按原始类型匹配失败（典型如基本/装箱不匹配），
 * 自动用 [companionTypes] 的对应类型重试一次。任一成功即返回结果，均失败返回 null。
 */
private inline fun <R> findWithTypeRetry(
    types: List<Class<*>>?,
    crossinline build: (List<Class<*>>?) -> R
): R? {
    if (types == null) return runCatching { build(null) }.getOrNull()
    return runCatching { build(types) }
        .recoverCatching { build(companionTypes(types)) }
        .getOrNull()
}

/**
 * JS 实参 → Kotlin 值。keight 的 [Wrapper] 机制会在 [JsAny.toKotlin] 时还原出原始 Java 对象
 * （Class / 实例 / ClassLoader 等），因此这里只需直接调用 [JsAny.toKotlin] 即可，无需任何 id 查找。
 */
context(runtime: ScriptRuntime)
private fun convertArg(arg: JsAny?): Any? = arg?.toKotlin(runtime)

/** Kotlin 值 → JS 值。活对象/Class 包装为 [Wrapper] 子类句柄，基础类型用 keight 的 `.js`，null 返回 null。 */
private fun convertResult(value: Any?, loader: ClassLoader? = null): JsAny? {
    if (value == null) return null
    return when (value) {
        is JsAny -> value
        is String -> value.js
        is Boolean -> value.js
        is Int -> value.js
        is Long -> value.js
        is Double -> value.js
        is Float -> value.js
        is Short -> value.js
        is Byte -> value.js
        is Char -> value.toString().js
        is Class<*> -> classWrapper(value, loader)
        else -> instanceWrapper(value, loader)
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
        is Int -> Int::class.java
        is Long -> Long::class.java
        is Double -> Double::class.java
        is Float -> Float::class.java
        is Boolean -> Boolean::class.java
        is String -> String::class.java
        is Class<*> -> v
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
        listOf("method".js, "方法".js).func { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val types = parseTypeList(args.getOrNull(1), loader)
            methodWrapper(clazz, name, types, loader)
        }
        listOf("field".js, "字段".js).func { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            fieldWrapper(clazz, name, loader)
        }
        listOf("constructor".js, "构造器".js, "构造".js).func { args ->
            val types = parseTypeList(args.getOrNull(0), loader)
            constructorWrapper(clazz, types, loader)
        }
        listOf("newInstance".js, "新建".js, "实例化".js).func { args ->
            val types = inferTypes(args)
            val cargs = args.map { convertArg(it) }.toTypedArray()
            findWithTypeRetry(types) { ts ->
                val finder = clazz.constructor {
                    if (ts!!.isEmpty()) emptyParam() else param(*ts.toTypedArray())
                }
                val proxy = finder.get()
                convertResult(proxy.call(*cargs), loader)
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
        // call(instance?, ...args)：第一个参数为实例（可为 null 表示静态），其余为方法实参
        listOf("call".js, "调用".js).func { args ->
            val instance = args.getOrNull(0)?.let { convertArg(it) }
            val methodArgs = args.drop(1).map { convertArg(it) }.toTypedArray()
            findWithTypeRetry(types) { ts ->
                val finder = clazz.method {
                    this.name = name
                    if (ts != null && ts.isNotEmpty()) param(*ts.toTypedArray())
                }
                val proxy = finder.get(instance)
                convertResult(proxy.call(*methodArgs), loader)
            }
        }
        // invoke(...args)：静态调用（instance = null）
        listOf("invoke".js, "执行".js).func { args ->
            val methodArgs = args.map { convertArg(it) }.toTypedArray()
            findWithTypeRetry(types) { ts ->
                val finder = clazz.method {
                    this.name = name
                    if (ts != null && ts.isNotEmpty()) param(*ts.toTypedArray())
                }
                val proxy = finder.get(null)
                convertResult(proxy.call(*methodArgs), loader)
            }
        }
    }
}

private fun fieldWrapper(clazz: Class<*>, name: String, loader: ClassLoader?): JsObject {
    return Object("field") {
        listOf("name".js, "字段名".js) eq name.js
        listOf("get".js, "读取".js, "获取".js).func { args ->
            val instance = args.getOrNull(0)?.let { convertArg(it) }
            runCatching { convertResult(clazz.field { this.name = name }.get(instance).any(), loader) }.getOrNull()
        }
        listOf("set".js, "写入".js, "设置".js).func { args ->
            val instance = args.getOrNull(0)?.let { convertArg(it) }
            val value = args.getOrNull(1)?.let { convertArg(it) }
            runCatching { clazz.field { this.name = name }.get(instance).set(value); null }.getOrNull()
        }
    }
}

private fun constructorWrapper(clazz: Class<*>, types: List<Class<*>>?, loader: ClassLoader?): JsObject {
    return Object("constructor") {
        listOf("newInstance".js, "新建".js, "实例化".js).func { args ->
            val cargs = args.map { convertArg(it) }.toTypedArray()
            findWithTypeRetry(types) { ts ->
                val finder = clazz.constructor {
                    if (ts == null || ts.isEmpty()) emptyParam() else param(*ts.toTypedArray())
                }
                val proxy = finder.get()
                convertResult(proxy.call(*cargs), loader)
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
        // call(methodName, ...args)：在自身上调用方法
        listOf("call".js, "调用方法".js).func { args ->
            val methodName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val methodArgs = args.drop(1).map { convertArg(it) }.toTypedArray()
            runCatching {
                val proxy = obj.javaClass.method { this.name = methodName }.get(obj)
                convertResult(proxy.call(*methodArgs), loader)
            }.getOrNull()
        }
        listOf("get".js, "读字段".js).func { args ->
            val fieldName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            runCatching { convertResult(obj.javaClass.field { this.name = fieldName }.get(obj).any(), loader) }.getOrNull()
        }
        listOf("set".js, "写字段".js).func { args ->
            val fieldName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val value = args.getOrNull(1)?.let { convertArg(it) }
            runCatching { obj.javaClass.field { this.name = fieldName }.get(obj).set(value); null }.getOrNull()
        }
        listOf("getId".js, "取ID".js).func { _ -> System.identityHashCode(obj).js }
        listOf("getClass".js, "取类".js).func { _ -> obj.javaClass.let { classWrapper(it, loader) } }
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
        listOf("findClass".js, "查找类".js).func { args ->
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
        // 从文件路径加载 .dex/.apk/.jar；第二个参数为可选父加载器句柄（即另一个 dex 对象）
        listOf("load".js, "加载".js, "loadDex".js).func { args ->
            val path = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val parent = resolveParent(args.getOrNull(1))
            val optimized = File(InitializePvz2.context.cacheDir, "dex_opt_${System.currentTimeMillis()}").also { it.mkdirs() }
            runCatching {
                val loader = DexClassLoader(path, optimized.absolutePath, null, parent)
                loaderWrapper(loader, path)
            }.getOrNull()
        }
        // 从 assets 提取后加载
        listOf("loadFromAsset".js, "从资源加载".js).func { args ->
            val assetName = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val parent = resolveParent(args.getOrNull(1))
            runCatching {
                val out = File(InitializePvz2.context.cacheDir, "dex_asset_${System.currentTimeMillis()}.dex").also { it.parentFile?.mkdirs() }
                InitializePvz2.context.assets.open(assetName).use { input -> out.outputStream().use { input.copyTo(it) } }
                val optimized = File(InitializePvz2.context.cacheDir, "dex_opt_asset").also { it.mkdirs() }
                val loader = DexClassLoader(out.absolutePath, optimized.absolutePath, null, parent)
                loaderWrapper(loader, out.absolutePath)
            }.getOrNull()
        }
        // 从网络 URL 下载后加载
        listOf("loadFromUrl".js, "从网络加载".js).func { args ->
            val url = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val parent = resolveParent(args.getOrNull(1))
            runCatching {
                val out = File(InitializePvz2.context.cacheDir, "dex_url_${System.currentTimeMillis()}.dex").also { it.parentFile?.mkdirs() }
                URL(url).openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
                val optimized = File(InitializePvz2.context.cacheDir, "dex_opt_url").also { it.mkdirs() }
                val loader = DexClassLoader(out.absolutePath, optimized.absolutePath, null, parent)
                loaderWrapper(loader, out.absolutePath)
            }.getOrNull()
        }
    }

    context(runtime: ScriptRuntime)
    private fun resolveParent(arg: JsAny?): ClassLoader {
        if (arg == null) return defaultClassLoader
        // 若传入的是另一个 dex 句柄（JsLoaderWrapper），toKotlin 会还原出原始 ClassLoader
        return arg.toKotlin(runtime) as? ClassLoader ?: defaultClassLoader
    }
}

// ======================== 全局对象：reflect ========================

object JsReflect {
    val js = Object("reflect") {
        // 按类名取 Class 句柄；第二个参数为可选 DEX 加载器句柄（或其 ClassLoader），用于反射 DEX 内的类
        listOf("findClass".js, "查找类".js, "反射".js).func { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val loader = args.getOrNull(1)?.toKotlin(this) as? ClassLoader
            runCatching { classWrapper(name.toClass(loader ?: defaultClassLoader, true), loader) }.getOrNull()
        }
    }
}
