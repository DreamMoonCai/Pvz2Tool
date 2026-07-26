package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.js.toKotlin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.milliseconds

/**
 * 异步 / 协程操作全局对象：`thread`（协程 / 线程）。
 *
 * 基于 keight 引擎的协程能力（[io.github.alexzhirkevich.keight.ScriptRuntime] 本身即 `CoroutineScope`，
 * 且引擎支持将 Kotlin 协程桥接为 JS `Promise`），提供面向 JS 脚本的异步原语：
 *
 * - `thread.run(task, args?)`       —— 在后台协程中执行 `task`，返回一个 **Promise**，resolve 为 task 的返回值（异步执行结果）。
 * - `thread.all([t1, t2, ...])`     —— 并发执行多个 task，返回一个 **Promise**，resolve 为结果数组（顺序与入参一致，类似 `Promise.all`）。
 * - `thread.race([t1, t2, ...])`    —— 并发执行多个 task，返回**最先完成**的那个结果（类似 `Promise.race`）。
 * - `thread.timeout(ms, task, args?)` —— 限时执行 `task`，超时则以异常 reject Promise（类似超时控制）。
 * - `thread.retry(count, task, args?)` —— 失败自动重试，最多 `count` 次；全部失败则 reject Promise。
 * - `thread.map(items, fn, concurrency?)` —— 将 `fn` 并发作用于数组每个元素，返回结果数组（顺序一致，可选限流）。
 * - `thread.launch(task, args?)`    —— 后台「即发即忘」执行 task，返回 Promise；任务异常会被记录到日志，不影响后续脚本。
 * - `thread.sleep(ms)`              —— 非阻塞等待 `ms` 毫秒，返回一个 **Promise**（底层 `delay`，不占用 JS 主线程）。
 * - `thread.interval(ms, task, args?)` / `setInterval` —— 每 `ms` 毫秒后台重复执行 `task`，返回可取消的定时器句柄。
 * - `thread.setTimeout(ms, task, args?)` —— 延时 `ms` 毫秒后后台单次执行 `task`，返回可取消的句柄。
 * - `thread.withContext(dispatcher, task, args?)` —— 在指定调度器（main/ui、io、default/computation、unconfined）上执行 `task`；JS 调用仍回到引擎线程，保证单线程安全。
 * - `thread.local(key, value?)` —— 引擎级共享变量读写（协程上下文的「状态」维度，跨脚本调用持久存在）。
 * - `thread.context(options)` —— 创建一个可定义 `name`/`dispatcher`、可整体 `cancel`、可共享 `local` 局部变量的协程上下文作用域对象（协程上下文的核心入口）。
 *
 * 说明：JS 运行时（QuickJS）本身是单线程的，纯 JS CPU 密集循环无法真正并行；但异步原语让
 * 切出去的 `await`、网络 / 文件等挂起调用、以及多个 task 之间的挂起点能够交错执行，
 * 从而把「重任务」放到后台协程、避免阻塞当前脚本的后续流程。所有 JS 回调都在引擎线程上执行，保证线程安全。
 *
 * 用法：
 * ```js
 * // 异步执行并拿到结果
 * let r = await thread.run(() => { return 1 + 2; });
 * console.log(r); // 3
 *
 * // 带参数
 * let r2 = await thread.run((x) => x * 2, 21);
 *
 * // 并发执行（类似 Promise.all）
 * let [a, b] = await thread.all([ () => heavyA(), () => heavyB() ]);
 *
 * // 竞速：谁先完成用谁（类似 Promise.race）
 * let first = await thread.race([ () => fast(), () => slow() ]);
 *
 * // 限时：超过 2 秒直接抛错（reject）
 * try { let r = await thread.timeout(2000, () => slow()); }
 * catch (e) { console.log("超时了", e); }
 *
 * // 重试：最多 3 次
 * let r = await thread.retry(3, () => maybeFail());
 *
 * // 并行映射：并发处理数组
 * let results = await thread.map([1, 2, 3], (x) => x * 10);
 *
 * // 定时重复（返回定时器句柄，可随时停止）
 * let timer = thread.interval(1000, () => console.log("每秒一次"));
 * thread.setTimeout(5000, () => timer.stop()); // 5 秒后停止
 *
 * // 即发即忘
 * thread.launch(() => { console.log("后台跑完了"); });
 *
 * // 非阻塞等待
 * await thread.sleep(1000);
 *
 * // 协程上下文：定义作用域 / 调度器 / 局部状态
 * let ctx = thread.context({ name: "worker", dispatcher: "io" });
 * ctx.local("token", "abc123");                       // 上下文局部变量
 * let r = await ctx.run((c) => { return c.local("token"); }); // 任务首个参数为上下文自身
 * console.log(r); // "abc123"
 * // 上下文同样支持 run/launch/all/withContext/cancel/isActive
 * ctx.cancel();                                       // 整体取消该上下文下的所有任务
 *
 * // 切换调度器：在 IO 线程跑重任务（JS 调用仍回引擎线程，安全）
 * let data = await thread.withContext("io", () => heavyIoWork());
 * ```
 */
object JsThread {

    /** 引擎级共享变量（协程上下文的「状态」维度），跨脚本调用持久存在 */
    private val globalLocals = ConcurrentHashMap<String, JsAny>()

    /** 按名称解析调度器（协程上下文的「dispatcher」维度） */
    private fun resolveDispatcher(name: String?): CoroutineDispatcher = when ((name ?: "default").lowercase()) {
        "main", "ui" -> Dispatchers.Main
        "io" -> Dispatchers.IO
        "computation", "compute", "cpu" -> Dispatchers.Default
        "default", "def" -> Dispatchers.Default
        "unconfined" -> Dispatchers.Unconfined
        else -> Dispatchers.Default
    }

    /** 在引擎线程上调用 JS 函数（QuickJS 单线程，所有 JS 调用必须回到引擎调度器以保证线程安全） */
    private suspend fun invokeOnEngine(scope: ScriptRuntime, engineDisp: CoroutineDispatcher, fn: JSFunction, args: List<JsAny?>) =
        withContext(engineDisp) { fn.invoke(args, scope) }

    val js = Object("thread") {
        // 异步执行 task，返回 Promise（resolve 为 task 返回值）
        listOf("run".js, "运行".js, "执行".js).func(
            FunctionParam("task"), FunctionParam("args",true)
        ) { args ->
            val task = args.getOrNull(0).orNull as? JSFunction ?: return@func null
            val args = args.getOrNull(1).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            async {
                task.invoke(args.filterIsInstance<JsAny?>(),this@func)
            }.js
        }

        // 并发执行多个 task，返回 Promise（resolve 为结果数组，顺序与入参一致）
        listOf("all".js, "全部".js, "并行".js).func(
            FunctionParam("tasks")
        ) { args ->
            val tasksAny = args.getOrNull(0).orNull ?: return@func null
            val tasks = tasksAny.toKotlin(this) as? List<*> ?: return@func null
            val fns = tasks.filterIsInstance<JSFunction>()
            if (fns.isEmpty()) return@func emptyList<JsAny>().js

            fns.map { fn ->
                async {
                    fn.invoke(emptyList(),this@func)
                }.js
            }.js
        }

        // 即发即忘：后台执行 task，返回 Promise；异常记入日志
        listOf("launch".js, "启动".js, "后台".js).func(
            FunctionParam("task"), FunctionParam("args", isVararg = true)
        ) { args ->
            val task = args.getOrNull(0).orNull as? JSFunction ?: return@func null
            val args = args.getOrNull(1).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            launch {
                runCatching {
                    task.invoke(args.filterIsInstance<JsAny?>(), this@func)
                    null
                }.onFailure { e ->
                    JsConsole.error("thread.launch 后台任务执行失败:", e)
                }
            }.js
        }

        // 非阻塞等待 ms 毫秒，返回 Promise（底层 delay，不占用 JS 主线程）
        listOf("sleep".js, "睡眠".js, "等待".js).func(
            FunctionParam("ms")
        ) { args ->
            val ms = toNumber(args.getOrNull(0).orNull).toLong().coerceAtLeast(0)
            async {
                delay(ms.milliseconds)
                null
            }.js
        }

        // 并发竞争：多个任务同时跑，返回最先完成的那个结果（类似 Promise.race）
        listOf("race".js, "竞争".js, "竞速".js).func(
            FunctionParam("tasks")
        ) { args ->
            val tasksAny = args.getOrNull(0).orNull ?: return@func null
            val tasks = tasksAny.toKotlin(this) as? List<*> ?: return@func null
            val fns = tasks.filterIsInstance<JSFunction>()
            if (fns.isEmpty()) return@func null
            async {
                val deferreds = fns.map { fn -> async { fn.invoke(emptyList(), this@func) } }
                select {
                    deferreds.forEach { d -> d.onAwait { it } }
                }
            }.js
        }

        // 限时执行：在 ms 毫秒内完成任务，超时则以异常 reject Promise（类似 Promise 超时）
        listOf("timeout".js, "超时".js).func(
            FunctionParam("ms"), FunctionParam("task"), FunctionParam("args", true)
        ) { args ->
            val ms = toNumber(args.getOrNull(0).orNull).toLong().coerceAtLeast(0)
            val task = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val taskArgs = args.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            async {
                withTimeout(ms.milliseconds) {
                    task.invoke(taskArgs.filterIsInstance<JsAny?>(), this@func)
                }
            }.js
        }

        // 重试执行：失败自动重试，最多 count 次；全部失败则 reject Promise
        listOf("retry".js, "重试".js).func(
            FunctionParam("count"), FunctionParam("task"), FunctionParam("args", true)
        ) { args ->
            val count = toNumber(args.getOrNull(0).orNull).toInt().coerceAtLeast(1)
            val task = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val taskArgs = args.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            async {
                var lastErr: Throwable? = null
                repeat(count) { i ->
                    runCatching {
                        task.invoke(taskArgs.filterIsInstance<JsAny?>(), this@func)
                    }.onSuccess { return@async it }
                        .onFailure { e ->
                            lastErr = e
                            JsConsole.warn("thread.retry 第 ${i + 1} 次执行失败:", e)
                        }
                }
                throw lastErr ?: RuntimeException("thread.retry 全部尝试失败")
            }.js
        }

        // 并行映射：将 fn 并发作用于数组每个元素，返回结果数组（顺序与入参一致）
        // concurrency 可选，限制最大并发数（缺省为全部并发）
        listOf("map".js, "映射".js, "并行映射".js).func(
            FunctionParam("items"), FunctionParam("fn"), FunctionParam("concurrency")
        ) { args ->
            val itemsAny = args.getOrNull(0).orNull ?: return@func null
            val itemsList = itemsAny.toKotlin(this) as? List<*> ?: return@func null
            val fn = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val concurrency = args.getOrNull(2).orNull?.let { toNumber(it).toInt() }?.coerceAtLeast(1)
                ?: itemsList.size
            async {
                val sem = Semaphore(concurrency)
                itemsList.mapIndexed { i, item ->
                    async {
                        sem.withPermit {
                            fn.invoke(
                                listOf((item as? JsAny), i.js),
                                this@func
                            )
                        }
                    }
                }.awaitAll().map { it }.js
            }.js
        }

        // 定时重复：每 ms 毫秒在后台执行一次 task，返回一个定时器句柄（不阻塞 await）
        // 句柄方法：stop()/停止()/取消() 停止定时器；isActive()/是否在运行() 是否仍在运行
        listOf("interval".js, "定时".js, "定时器".js, "setInterval".js).func(
            FunctionParam("ms"), FunctionParam("task"), FunctionParam("args", true)
        ) { args ->
            val ms = toNumber(args.getOrNull(0).orNull).toLong().coerceAtLeast(0)
            val task = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val taskArgs = args.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            val job = launch {
                while (true) {
                    delay(ms.milliseconds)
                    runCatching {
                        task.invoke(taskArgs.filterIsInstance<JsAny?>(), this@func)
                    }.onFailure { e ->
                        JsConsole.error("thread.interval 定时任务执行失败:", e)
                    }
                }
            }
            Object("timer") {
                listOf("stop".js, "停止".js, "取消".js).func { _ ->
                    job.cancel()
                    null
                }
                listOf("isActive".js, "是否在运行".js).func { _ ->
                    job.isActive.js
                }
            }
        }

        // 延时单次执行：ms 毫秒后在后台执行一次 task，返回一个可取消的句柄（不阻塞 await）
        // 句柄方法：cancel()/取消()/停止() 取消尚未执行的任务；isActive()/是否在运行() 是否仍在等待
        listOf("setTimeout".js, "延时执行".js, "延迟执行".js).func(
            FunctionParam("ms"), FunctionParam("task"), FunctionParam("args", true)
        ) { args ->
            val ms = toNumber(args.getOrNull(0).orNull).toLong().coerceAtLeast(0)
            val task = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val taskArgs = args.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            val job = launch {
                delay(ms.milliseconds)
                runCatching {
                    task.invoke(taskArgs.filterIsInstance<JsAny?>(), this@func)
                }.onFailure { e ->
                    JsConsole.error("thread.setTimeout 任务执行失败:", e)
                }
            }
            Object("timer") {
                listOf("cancel".js, "取消".js, "停止".js).func { _ ->
                    job.cancel()
                    null
                }
                listOf("isActive".js, "是否在运行".js).func { _ ->
                    job.isActive.js
                }
            }
        }

        // === 协程上下文（定义作用域 / 调度器 / 局部状态） ===

        // 引擎级共享变量：local(key, value?) 取值或赋值（协程上下文的「状态」维度，全局共享）
        listOf("local".js, "变量".js, "上下文变量".js).func(
            FunctionParam("key"), FunctionParam("value", true)
        ) { args ->
            val key = args.getOrNull(0).orNull?.toString() ?: return@func null
            val value = args.getOrNull(1).orNull
            if (value != null) {
                globalLocals[key] = value
                value
            } else {
                globalLocals[key]
            }
        }

        // 切换调度器执行：在指定 dispatcher 上运行 task；JS 调用仍回到引擎线程，保证单线程安全
        listOf("withContext".js, "切换上下文".js, "切换调度器".js).func(
            FunctionParam("dispatcher"), FunctionParam("task"), FunctionParam("args", true)
        ) { args ->
            val dispName = args.getOrNull(0).orNull?.toString()
            val task = args.getOrNull(1).orNull as? JSFunction ?: return@func null
            val taskArgs = args.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
            val engineDisp = this.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
            val target = resolveDispatcher(dispName)
            async {
                withContext(target) {
                    invokeOnEngine(this@func, engineDisp, task, taskArgs.filterIsInstance<JsAny?>())
                }
            }.js
        }

        // 创建协程上下文：返回一个可定义 name/dispatcher、可整体取消、可共享局部变量的作用域对象
        // thread.context(options) / thread.createContext(options) / thread.协程上下文 / thread.创建上下文
        // options: { name?, dispatcher?, onError? }（onError 暂未实现，异常统一由 JsConsole.error 记录）
        listOf("context".js, "协程上下文".js, "创建上下文".js, "createContext".js).func(
            FunctionParam("options")
        ) { args ->
            val optsAny = args.getOrNull(0).orNull
            val opts = when (val o = optsAny?.toKotlin(this)) {
                is Map<*, *> -> o
                is String -> mapOf("name" to o)
                else -> emptyMap()
            }
            val name = (opts["name"] ?: opts["名称"] ?: "context").toString()
            val dispName = (opts["dispatcher"] ?: opts["调度器"])?.toString()

            val parentJob = this.coroutineContext[Job]
            val handler = CoroutineExceptionHandler { _, e ->
                JsConsole.error("thread.context[$name] 任务异常:", e)
            }
            val ctxScope = CoroutineScope(this.coroutineContext + SupervisorJob(parentJob) + handler)
            val engineDisp = this.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
            val targetDisp = dispName?.let { resolveDispatcher(it) } ?: engineDisp
            val locals = ConcurrentHashMap<String, JsAny>()
            var ctxObj: JsAny? = null

            val obj = Object("context") {
                // 上下文名称
                listOf("name".js, "名称".js).func { _ -> name.js }

                // 上下文局部变量（协程上下文的「状态」维度，仅本上下文可见）
                listOf("local".js, "变量".js).func(
                    FunctionParam("key"), FunctionParam("value", true)
                ) { a ->
                    val k = a.getOrNull(0).orNull?.toString() ?: return@func null
                    val v = a.getOrNull(1).orNull
                    if (v != null) {
                        locals[k] = v
                        v
                    } else {
                        locals[k]
                    }
                }

                // 上下文作用域是否仍在运行
                listOf("isActive".js, "是否在运行".js).func { _ ->
                    (ctxScope.coroutineContext[Job]?.isActive ?: false).js
                }

                // 整体取消（取消该上下文下的所有任务）
                listOf("cancel".js, "取消".js, "停止".js).func { _ ->
                    ctxScope.cancel()
                    null
                }

                // 在上下文中异步执行（返回 Promise；首个参数为上下文对象自身，供任务内读取 local）
                listOf("run".js, "运行".js, "执行".js).func(
                    FunctionParam("task"), FunctionParam("args", true)
                ) { a ->
                    val scope = this
                    val task = a.getOrNull(0).orNull as? JSFunction ?: return@func null
                    val taskArgs = a.getOrNull(1).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
                    ctxScope.async {
                        withContext(targetDisp) {
                            invokeOnEngine(scope, engineDisp, task, listOf(ctxObj) + taskArgs.filterIsInstance<JsAny?>())
                        }
                    }.js
                }

                // 在上下文中即发即忘
                listOf("launch".js, "启动".js, "后台".js).func(
                    FunctionParam("task"), FunctionParam("args", true)
                ) { a ->
                    val scope = this
                    val task = a.getOrNull(0).orNull as? JSFunction ?: return@func null
                    val taskArgs = a.getOrNull(1).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
                    ctxScope.launch {
                        runCatching {
                            withContext(targetDisp) {
                                invokeOnEngine(scope, engineDisp, task, listOf(ctxObj) + taskArgs.filterIsInstance<JsAny?>())
                            }
                        }.onFailure { e ->
                            JsConsole.error("thread.context[$name] launch 任务执行失败:", e)
                        }
                    }
                    null
                }

                // 在上下文中并发执行多个 task（返回 Promise 数组，类似 Promise.all）
                listOf("all".js, "全部".js, "并行".js).func(
                    FunctionParam("tasks")
                ) { a ->
                    val scope = this
                    val tasksAny = a.getOrNull(0).orNull ?: return@func null
                    val tasks = tasksAny.toKotlin(this) as? List<*> ?: return@func null
                    val fns = tasks.filterIsInstance<JSFunction>()
                    if (fns.isEmpty()) return@func emptyList<JsAny>().js
                    ctxScope.async {
                        withContext(targetDisp) {
                            fns.map { fn ->
                                async { invokeOnEngine(scope, engineDisp, fn, listOf(ctxObj)) }.js
                            }.js
                        }
                    }.js
                }

                // 在上下文中切换调度器执行单次任务
                listOf("withContext".js, "切换上下文".js, "切换调度器".js).func(
                    FunctionParam("dispatcher"), FunctionParam("task"), FunctionParam("args", true)
                ) { a ->
                    val scope = this
                    val dName = a.getOrNull(0).orNull?.toString()
                    val task = a.getOrNull(1).orNull as? JSFunction ?: return@func null
                    val taskArgs = a.getOrNull(2).orNull?.toKotlin() as? List<*> ?: emptyList<JsAny?>()
                    val dTarget = resolveDispatcher(dName)
                    ctxScope.async {
                        withContext(dTarget) {
                            invokeOnEngine(scope, engineDisp, task, listOf(ctxObj) + taskArgs.filterIsInstance<JsAny?>())
                        }
                    }.js
                }
            }
            ctxObj = obj
            obj
        }
    }
}
