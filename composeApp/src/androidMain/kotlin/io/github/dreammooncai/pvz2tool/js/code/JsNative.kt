package io.github.dreammooncai.pvz2tool.js.code

import android.annotation.SuppressLint
import android.os.Build
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.InitializePvz2
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.JsFileAccess
import io.github.dreammooncai.pvz2tool.js.eq
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import java.io.File
import java.net.URL

/**
 * 原生（`.so`）动态库加载 API。
 *
 * 提供全局对象 `native`（中文别名 `原生` / `so加载`），用于在脚本运行时把外部的 `.so` 二进制动态库
 * 加载进当前进程，从而让之后 [JsDex]（`dex.load`）加载的 DEX 中声明的 `external fun`（JNI 原生方法）
 * 能够获得实现。`System.load` 是进程级全局操作——同一动态库先 `native.load`、后 `dex.load` 即可打通
 * JNI 调用链；与 [JsDex] 的加载顺序无关子加载器嵌套，但顺序上应先加载原生库。
 *
 * 加载机制：
 * - `native.load(path)` → 按统一路径规则解析 `.so` 文件，复制到应用私有缓存（`cache/native_load/`），
 *   调 [System.load] 把动态库映射到进程。与原生 `System.load` 一致，库以「绝对文件路径」加载，
 *   且同一文件重复加载会抛 `UnsatisfiedLinkError`，因此本对象内部维护已加载路径集合，重复加载时跳过并提示。
 * - `native.loadLibrary(name)` → 等价 [System.loadLibrary]，加载 `lib<name>.so`
 *   （从应用原生库目录或 `java.library.path` 查找，适用于 APK 内置库）。
 *
 * ABI 校验：Android 上 `.so` 必须与设备 ABI 匹配，否则 dlopen 失败。加载前会读取 `.so` 路径中的 ABI
 * 目录名（如 `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86`），若与 [Build.SUPPORTED_ABIS] 完全无交集
 * 则给出警告（仍会尝试加载，错误信息由系统 dlopen 抛出并记日志）。
 *
 * ⚠️ 与 [JsDex] 不同：`.so` 不要求去掉写权限（无 `Writable dex file` 安全限制），故仅复制到缓存目录、
 * 不置 `0444`。已 dlopen 进进程的库无法在运行时卸载，`clearCache()` 仅清理本对象跟踪状态与缓存文件，
 * 真正卸载需重启应用。
 */
object JsNative {

    val js = Object("native") {
        // 统一加载入口：路径规则与其余文件 API 完全一致（见 [io.github.dreammooncai.pvz2tool.js.JsFileAccess]）
        // - 绝对路径（/ 开头）→ 直接作为本地文件加载
        // - 相对路径 / $WORK_DIR 等占位符 → 走 JsFileAccess.resolveInput（工作目录优先，无则回退 assets/pvz2tool/）
        // - http(s):// URL → 先下载到缓存再加载
        listOf("load".js, "加载".js, "loadSo".js).func("path") { args ->
            val path = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            val soFile = resolveSoFile(path)
            // 同一文件重复加载会抛 UnsatisfiedLinkError("... already added")，跳过已加载项
            val abs = soFile.absolutePath
            if (loadedPaths.contains(abs)) {
                JsConsole.warn("native.load: $abs 已加载，跳过重复加载")
                return@func nativeHandle(soFile)
            }
            // ABI 预检：路径含 ABI 目录名但与设备支持的 ABI 完全无交集时给警告
            warnIfAbiMismatch(soFile)
            JsConsole.info("native.load: ${soFile.name} (${soFile.absolutePath})")
            try {
                soFile.setWritable(false)
                @SuppressLint("UnsafeDynamicallyLoadedCode")
                System.load(abs)
                loadedPaths += abs
                JsConsole.success("native.load 完成: ${soFile.name} → 已加载")
                nativeHandle(soFile)
            } catch (e: Throwable) {
                JsConsole.error("native.load 失败: ${soFile.name}", e)
                throw e
            }
        }

        // 按库名加载：等价 System.loadLibrary(name)，加载 lib<name>.so
        listOf("loadLibrary".js, "按名称加载".js).func("name") { args ->
            val name = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func null
            JsConsole.info("native.loadLibrary: $name")
            try {
                System.loadLibrary(name)
                loadedLibraries += name
                JsConsole.success("native.loadLibrary 完成: $name → 已加载")
                libraryHandle(name)
            } catch (e: Throwable) {
                JsConsole.error("native.loadLibrary 失败: $name", e)
                throw e
            }
        }

        // 查询某路径 / 库名是否已加载
        listOf("isLoaded".js, "是否已加载".js).func("path") { args ->
            val p = args.getOrNull(0).orNull?.toString()?.trim() ?: return@func false.js
            (loadedPaths.contains(p) || loadedLibraries.contains(p)).js
        }

        // 返回本次进程内已加载的动态库清单（绝对路径 + 库名）
        listOf("loadedLibraries".js, "已加载列表".js).func { _ ->
            (loadedPaths + loadedLibraries).map { it.js }.js
        }

        // 清理 .so 加载缓存与加载状态跟踪（已 dlopen 进进程的库需重启才能卸载）
        listOf("clearCache".js, "清理缓存".js).func { _ ->
            clearCache()
            JsConsole.info("native.clearCache: 已清理 .so 加载缓存")
            null
        }
    }

    private val ctx get() = InitializePvz2.context

    // 已加载的动态库（绝对路径 / 库名），用于去重与状态查询。
    // 引擎运行于后台线程，但同一脚本串行执行，加载动作本身在单线程内完成，用 @Volatile + 重建集合即可。
    @Volatile
    private var loadedPaths = mutableSetOf<String>()
    @Volatile
    private var loadedLibraries = mutableSetOf<String>()

    /**
     * 按统一路径规则解析出可加载的 `.so` 文件并复制到应用私有缓存后返回。
     *
     * 与 [JsDex] 不同，`.so` 没有「不可写」安全限制，故只复制、不置 `0444`。
     * 复制后的缓存文件在进程退出时自动删除（`deleteOnExit`），同时也在 `clearCache()` 中显式清理。
     */
    private fun resolveSoFile(path: String): File {
        val trimmed = path.trim()
        val src = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            downloadToCache(trimmed)
        } else {
            JsFileAccess.resolveInput(trimmed, ctx)?.file
                ?: error("无法解析 .so 路径: $trimmed（请确认文件存在于工作目录或 assets/pvz2tool/，或使用 http(s):// URL）")
        }
        val dst = File(File(ctx.cacheDir, "native_load"), "so_${System.currentTimeMillis()}_${src.name}").also {
            it.parentFile?.mkdirs()
            src.inputStream().use { input -> it.outputStream().use { input.copyTo(it) } }
            it.deleteOnExit() // 与其他缓存一致：进程退出时自动清理
        }
        return dst
    }

    private fun downloadToCache(url: String): File {
        val out = File(ctx.cacheDir, "native_url_${System.currentTimeMillis()}.so").also { it.parentFile?.mkdirs() }
        URL(url).openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        out.deleteOnExit() // 与其他缓存一致：进程退出时自动清理
        return out
    }

    /** 若 `.so` 路径中的父目录名声明了某个 ABI 且该 ABI 不在设备支持列表中，给出警告。 */
    private fun warnIfAbiMismatch(soFile: File) {
        val abiFolder = soFile.parentFile?.name?.lowercase() ?: return
        val declared = when {
            abiFolder.contains("arm64-v8a") -> "arm64-v8a"
            abiFolder.contains("armeabi-v7a") || abiFolder.contains("armeabi") -> "armeabi-v7a"
            abiFolder.contains("x86_64") -> "x86_64"
            abiFolder.contains("x86") -> "x86"
            else -> return // 父目录名不含已知 ABI，跳过预检
        }
        val supported = Build.SUPPORTED_ABIS.map { it.lowercase() }
        if (declared !in supported) {
            JsConsole.warn(
                "native.load ABI 警告: 该 .so 声明架构 $declared，" +
                    "但设备支持 ${Build.SUPPORTED_ABIS.joinToString()}，可能无法加载（dlopen 失败）"
            )
        }
    }

    private fun nativeHandle(soFile: File): JsObject = JsNativeSoHandle(soFile)
    private fun libraryHandle(name: String): JsObject = JsNativeNamedHandle(name)

    /**
     * 清理 `.so` 加载产生的临时缓存（`[cacheDir]/native_load/`、`native_url_*.so`），
     * 并重置加载状态跟踪。与项目其余缓存清理机制（[JsDex.clearCache] 等）保持一致，
     * 在「重置数据」等生命周期里调用。
     */
    fun clearCache() {
        val root = ctx.cacheDir
        File(root, "native_load").deleteRecursively()
        root.listFiles { f -> f.name.startsWith("native_url_") && f.name.endsWith(".so") }?.forEach { it.delete() }
        // 已 dlopen 进进程的库无法卸载，仅重置本对象的跟踪状态；真正卸载需重启应用
        loadedPaths = mutableSetOf()
        loadedLibraries = mutableSetOf()
    }
}

// ======================== 按路径加载的 .so 句柄 ========================

private class JsNativeSoHandle(
    private val file: File
) : JsObjectImpl("nativeLib"), Wrapper<File> {

    override val value: File get() = file
    override fun toKotlin(runtime: ScriptRuntime): Any = file

    private val js = Object("nativeLib") {
        listOf("path".js, "路径".js) eq file.absolutePath.js
        listOf("name".js, "名称".js) eq file.name.js
        listOf("loaded".js, "已加载".js) eq true.js
        listOf("toString".js).func { _ -> "NativeLibrary(${file.name})".js }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? = js.get(property, runtime)
}

// ======================== 按名称加载的库句柄 ========================

private class JsNativeNamedHandle(
    private val libName: String
) : JsObjectImpl("nativeLibByName"), Wrapper<String> {

    override val value: String get() = libName
    override fun toKotlin(runtime: ScriptRuntime): Any = libName

    private val js = Object("nativeLibByName") {
        listOf("name".js, "名称".js) eq libName.js
        listOf("loaded".js, "已加载".js) eq true.js
        listOf("toString".js).func { _ -> "NativeLibrary($libName)".js }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? = js.get(property, runtime)
}
