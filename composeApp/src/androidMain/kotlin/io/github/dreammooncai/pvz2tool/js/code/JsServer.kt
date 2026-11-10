package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.JsConsole
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.github.dreammooncai.pvz2tool.js.toKotlin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseQueryString
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.queryString
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException

/**
 * JS 本地 HTTP 服务器对象。
 *
 * JS 中通过 `server` / `本地服务器` / `服务器` 访问。
 * 基于 Ktor（CIO 引擎）在应用内启动一个本地 HTTP 服务器，把每一个到达的请求交给 JS 编写的
 * `handler` 处理，因此可以非常方便地用 JS 脚本对外暴露接口、提供本地网页、或与游戏 / 外部程序进行互相调用。
 *
 * 基本用法：
 * ```js
 * let srv = server.start({
 *   host: "0.0.0.0",     // 可选，监听地址，默认 "0.0.0.0"（监听所有网卡）
 *   port: 8080,          // 可选，监听端口，默认 8080
 *   handler: function(req) {
 *     // req: { method, path, url, query, queryParams, headers, body, json() }
 *     return { body: { ok: true, path: req.path } };   // 对象自动序列化为 JSON
 *   }
 * });
 * console.log(srv.url);  // "http://0.0.0.0:8080"
 * srv.stop();
 * ```
 *
 * ## 路由
 * 所有方法（GET/POST/PUT/DELETE/...）与所有路径（含 `/`）都会交给同一个 handler，
 * 由 JS 自行根据 `req.method` / `req.path` 分发。
 *
 * ## handler 返回值
 * - **字符串 / 数字 / 布尔**：作为 `200 text/plain` 响应体（按 JS 的字符串转换规则）；
 * - **普通对象**（不含任何响应规格字段）：整体 JSON 序列化，按 `application/json` 返回；
 * - **响应规格对象**：含 `status` / `headers` / `contentType` / `body` 中任意一个字段即按规格解释
 *   - `status`（或 `状态码`）：HTTP 状态码，默认 `200`
 *   - `headers`（或 `响应头`）：响应头对象
 *   - `contentType`（或 `内容类型`）：内容类型字符串
 *   - `body`（或 `响应体`）：字符串直接作为响应体；对象自动 JSON 序列化
 * - **null / undefined**：返回 `204 No Content`。
 *
 * handler 也支持 async 函数（返回 Promise），引擎会自动等待其完成后再响应。
 *
 * ## 端口冲突
 * 同一 `host:port` 重复 `start` 不会崩溃、也不会叠加实例：会复用已运行的服务器并返回它的句柄
 * （句柄 `error` 字段为空字符串）。端口被其它进程占用时返回 `error` 句柄，不抛异常。
 *
 * ## 明文（Cleartext）HTTP 注意
 * 若从应用内部（例如 yml 示例里的 `http.get("http://127.0.0.1:" + port + "...")`）访问本服务器，
 * Android 9+（API 28+）默认禁止明文 HTTP，会抛 `Cleartext HTTP traffic to 127.0.0.1 not permitted`。
 * 已在 `AndroidManifest.xml` 的 `<application>` 设置 `android:usesCleartextTraffic="true"` 放行。
 * 若改为更严格的 network_security_config 方案，需把 `127.0.0.1` / `localhost` 加入明文白名单。
 */
object JsServer {

    /** 运行中的服务器句柄表：id -> RunningServer */
    private val servers = ConcurrentHashMap<String, RunningServer>()
    private val idGen = AtomicInteger(0)

    /**
     * 服务器协程异常兜底：任何异步失败（监听断开、连接异常等）都只记日志，
     * 绝不把异常抛到默认 handler 导致整个应用崩溃。
     */
    private val serverExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        JsConsole.error("server：服务器协程异常:", throwable)
    }
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + serverExceptionHandler)

    private data class RunningServer(
        val id: String,
        val host: String,
        val port: Int,
        val engineDispatcher: CoroutineDispatcher,
        val runtime: ScriptRuntime,
        val handler: JSFunction,
        val embedded: EmbeddedServer<*, *>
    )

    val js = Object("server") {

        // ======================== start / 启动 / 开始 / create / 创建 ========================
        listOf("start".js, "启动".js, "开始".js, "create".js, "创建".js).func(
            FunctionParam("options"), FunctionParam("host"), FunctionParam("handler")
        ) { args ->
            val runtime = this
            val engineDispatcher = this.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                ?: Dispatchers.Default

            val a0 = args.getOrNull(0).orNull
            val a1 = args.getOrNull(1).orNull
            val a2 = args.getOrNull(2).orNull

            var port = 8080
            var host = "0.0.0.0"
            var handlerFn: JSFunction? = null

            when {
                // 形态1：server.start(handler) 或 server.start(handler, { host, port })
                a0 is JSFunction -> {
                    handlerFn = a0
                    if (a1 is JsObject) {
                        (a1.get("host".js, runtime).orNull ?: a1.get("主机".js, runtime).orNull)?.let { host = toString(it) }
                        (a1.get("port".js, runtime).orNull ?: a1.get("端口".js, runtime).orNull)?.let { port = toNumber(it).toInt() }
                    }
                }
                // 形态3：server.start(options)  options 为对象
                a0 is JsObject -> {
                    handlerFn = (a0.get("handler".js, runtime).orNull ?: a0.get("处理器".js, runtime).orNull) as? JSFunction
                    if (handlerFn == null && a1 is JSFunction) handlerFn = a1
                    if (handlerFn == null && a2 is JSFunction) handlerFn = a2
                    (a0.get("host".js, runtime).orNull ?: a0.get("主机".js, runtime).orNull)?.let { host = toString(it) }
                    (a0.get("port".js, runtime).orNull ?: a0.get("端口".js, runtime).orNull)?.let { port = toNumber(it).toInt() }
                }
                // 形态2：server.start(port, handler) / server.start(port, host, handler)
                // 注意：keight 把 JS 数字表示为 JsNumber（非 Kotlin Number），故不能用 `a0 is Number` 判定，
                // 改为用 toNumber 兜底转换；同时把 JsObject/JSFunction 分支前置，避免对象/函数被误判为数字。
                else -> {
                    val a0Num = (a0 as? Number)?.toInt() ?: runCatching { toNumber(a0).toInt() }.getOrNull()
                    if (a0Num != null) {
                        port = a0Num
                        when (a1) {
                            is JSFunction -> handlerFn = a1
                            is JsObject -> readHostPortFromOptions(a1, runtime)?.let { (h, p) -> host = h; port = p }
                            else -> a1?.toKotlin()?.takeIf { it is String }?.let { host = it.toString() }
                        }
                        if (a2 is JSFunction) handlerFn = a2
                    }
                }
            }

            if (handlerFn == null) {
                JsConsole.error("server.start 失败：必须提供 handler 函数")
                return@func buildServerHandleError("缺少 handler 参数")
            }

            val finalHost = host
            val finalPort = port
            val finalHandler = handlerFn

            // 端口范围校验
            if (finalPort !in 1..65535) {
                JsConsole.error("server.start 失败：端口 $finalPort 不在合法范围 1~65535")
                return@func buildServerHandleError("端口非法: $finalPort")
            }

            // 同 host:port 已在运行 → 复用，避免重复绑定（BindException）与双实例叠加
            servers.values.firstOrNull { it.port == finalPort && it.host == finalHost }?.let { existing ->
                JsConsole.warn("server.start：$finalHost:$finalPort 已在运行（id=${existing.id}），复用已有实例")
                return@func buildServerHandle(existing)
            }

            // 端口被其它进程占用 → 提前给出可读错误，而不是抛异常
            if (!isPortAvailable(finalHost, finalPort)) {
                JsConsole.error("server.start 失败：端口 $finalPort 已被占用")
                return@func buildServerHandleError("端口已被占用: $finalPort")
            }

            val id = "srv_${idGen.incrementAndGet()}"

            try {
                val embedded = serverScope.embeddedServer(
                    CIO,
                    port = finalPort,
                    host = finalHost,
                    parentCoroutineContext = serverExceptionHandler
                ) {
                    // 用 Call 阶段拦截器接管「所有方法 + 所有路径」。
                    // 不能用 routing { handle { } }：那只会在路由根节点注册，只能命中 "/"，其余路径一律 404。
                    intercept(ApplicationCallPipeline.Call) {
                        val call = context
                        try {
                            val reqObj = withContext(engineDispatcher) { buildRequestObject(call) }
                            val raw = withContext(engineDispatcher) { finalHandler.invoke(listOf(reqObj), runtime) }
                            val resp = withContext(engineDispatcher) { runtime.resolveResponse(raw) }
                            resp.headers.forEach { (k, v) -> call.response.headers.append(k, v) }
                            call.respondText(
                                text = resp.body,
                                contentType = resp.contentType,
                                status = httpStatus(resp.status)
                            )
                        } catch (t: Throwable) {
                            if (t is CancellationException) throw t
                            JsConsole.error(
                                "server：处理 ${call.request.httpMethod.value} ${call.request.path()} 时出错:", t
                            )
                            runCatching {
                                call.respondText(
                                    text = "Internal Server Error: ${t.message}",
                                    contentType = ContentType.Text.Plain,
                                    status = HttpStatusCode.InternalServerError
                                )
                            }
                        } finally {
                            // 已自行响应，终止管线，避免继续往下走产生 404
                            finish()
                        }
                    }
                }
                // 直接走 suspend 版本：当前已在协程内，避免 start() 内部的 runBlocking
                // 占用 JS 引擎线程、在极端情况下把引擎线程池拖死
                embedded.startSuspend(wait = false)

                val running = RunningServer(
                    id = id, host = finalHost, port = finalPort,
                    engineDispatcher = engineDispatcher, runtime = runtime,
                    handler = finalHandler, embedded = embedded
                )
                servers[id] = running
                buildServerHandle(running)
            } catch (e: Exception) {
                JsConsole.error("server.start 失败（端口 $finalPort 无法绑定）:", e)
                buildServerHandleError("启动失败: ${e.message}")
            }
        }

        // ======================== stop / 关闭 / 停止 ========================
        // server.stop(id) 关闭指定服务器；server.stop() 无参则关闭全部
        listOf("stop".js, "关闭".js, "停止".js).func(FunctionParam("id")) { args ->
            val id = args.getOrNull(0).orNull?.let { toString(it) }
            if (id != null) stopServer(id) else stopAllServers()
            null
        }

        // ======================== stopAll / 关闭全部 ========================
        listOf("stopAll".js, "关闭全部".js, "停止全部".js, "全部停止".js).func {
            stopAllServers()
            null
        }

        // ======================== list / 列表 ========================
        listOf("list".js, "列表".js, "运行中".js).func {
            servers.values.map { s -> buildServerInfo(s) }.js
        }
    }

    // ==================== 内部实现 ====================

    /** 从 options 对象中读取 host / port（无则返回 null） */
    private suspend fun ScriptRuntime.readHostPortFromOptions(opts: JsObject, runtime: ScriptRuntime): Pair<String, Int>? {
        val h = (opts.get("host".js, runtime).orNull ?: opts.get("主机".js, runtime).orNull)?.let { toString(it) }
        val p = (opts.get("port".js, runtime).orNull ?: opts.get("端口".js, runtime).orNull)?.let { toNumber(it).toInt() }
        return if (h != null || p != null) (h ?: "0.0.0.0") to (p ?: 8080) else null
    }

    /** 端口可用性预检：能成功 bind 即视为可用。仅用于给出可读错误，真正绑定仍以 embeddedServer 为准。 */
    private fun isPortAvailable(host: String, port: Int): Boolean = try {
        val addr = if (host == "0.0.0.0" || host == "::" || host.isBlank()) null else InetAddress.getByName(host)
        ServerSocket(port, 0, addr).use { true }
    } catch (e: Exception) {
        false
    }

    /** 状态码兜底：Ktor 的 fromValue 只接受 1~999，越界会抛异常；这里统一收敛到合法范围。 */
    private fun httpStatus(code: Int): HttpStatusCode =
        if (code in 100..599) runCatching { HttpStatusCode.fromValue(code) }.getOrDefault(HttpStatusCode.OK)
        else HttpStatusCode.InternalServerError

    private fun stopServer(id: String) {
        servers.remove(id)?.let { s ->
            runCatching { s.embedded.stop(gracePeriodMillis = 500, timeoutMillis = 2000) }
                .onFailure { JsConsole.warn("server.stop 关闭 [$id] 时出错:", it) }
        }
    }

    private fun stopAllServers() {
        servers.keys.toList().forEach { stopServer(it) }
    }

    /** 在引擎线程上构建请求对象（method/path/url/query/queryParams/headers/body/json） */
    private suspend fun ScriptRuntime.buildRequestObject(
        call: ApplicationCall
    ): JsAny {
        val method = call.request.httpMethod.value
        val path = call.request.path()
        val query = call.request.queryString()
        val bodyText = runCatching { call.receiveText() }.getOrDefault("")
        val headers = mutableMapOf<String, String>()
        call.request.headers.forEach { k, v -> headers[k] = v.last() }
        val queryParams = runCatching { parseQueryString(query) }.getOrNull()

        return Object("request") {
            "method".js eq method.js
            "path".js eq path.js
            "url".js eq (path + if (query.isNotBlank()) "?$query" else "").js
            "query".js eq query.js
            "queryParams".js eq Object("requestQueryParams") {
                queryParams?.entries()?.forEach { (k, v) -> k.js eq (v.lastOrNull() ?: "").js }
            }
            "headers".js eq Object("requestHeaders") {
                headers.forEach { (k, v) -> k.js eq v.js }
            }
            "body".js eq bodyText.js
            listOf("json".js, "解析JSON".js).func {
                try { PvzToolJsEngine.parse(bodyText) } catch (e: Exception) { null }
            }
        }
    }

    private data class RespSpec(
        val status: Int,
        val contentType: ContentType,
        val headers: Map<String, String>,
        val body: String
    )

    /** 判断对象是否含有任意一个给定键（中文键与英文键等价） */
    private suspend fun ScriptRuntime.hasAnyKey(obj: JsObject, vararg keys: String): Boolean =
        keys.any { obj.get(it.js, this).orNull != null }

    /** 将 handler 的返回值转换为统一响应规格；支持 async handler（返回 Promise） */
    private suspend fun ScriptRuntime.resolveResponse(raw: JsAny?): RespSpec {
        // 等待 async handler：若返回值是一个 JS Promise，keight 会把它转为 Kotlin Deferred/Job
        val value: JsAny? = when (val k = raw?.toKotlin()) {
            is Deferred<*> -> k.await() as? JsAny?
            is Job -> {
                k.join()
                null
            }
            else -> raw
        }

        if (value == null) {
            return RespSpec(204, ContentType.Text.Plain, emptyMap(), "")
        }

        val obj = value as? JsObject
        if (obj != null) {
            // 只要出现 status/headers/contentType/body 之一，就当作「响应规格对象」解释
            val isSpec = hasAnyKey(obj, "status", "状态码")
                || hasAnyKey(obj, "headers", "响应头")
                || hasAnyKey(obj, "contentType", "内容类型")
                || hasAnyKey(obj, "body", "响应体")

            // 普通对象：整体 JSON 序列化，按 application/json 返回
            if (!isSpec) {
                return RespSpec(200, ContentType.Application.Json, emptyMap(), PvzToolJsEngine.stringify(obj))
            }

            val status = (obj.get("status".js, this).orNull ?: obj.get("状态码".js, this).orNull)
                ?.let { toNumber(it).toInt() } ?: 200
            val ctStr = (obj.get("contentType".js, this).orNull ?: obj.get("内容类型".js, this).orNull)
                ?.let { toString(it) }
            val bodyAny = obj.get("body".js, this).orNull ?: obj.get("响应体".js, this).orNull

            val userHeaders = mutableMapOf<String, String>()
            val userHeadersObj = (obj.get("headers".js, this).orNull ?: obj.get("响应头".js, this).orNull) as? JsObject
            if (userHeadersObj != null) {
                // toKotlin 会递归转成 Kotlin 基本类型，此处 v.toString() 拿到的是真正的字符串值
                val km = userHeadersObj.toKotlin()
                if (km is Map<*, *>) km.forEach { (k, v) -> if (k != null && v != null) userHeaders[k.toString()] = v.toString() }
            }

            val (finalBody, baseCt) = when (bodyAny) {
                null -> "" to ContentType.Text.Plain
                is JsObject -> PvzToolJsEngine.stringify(bodyAny) to ContentType.Application.Json // 必须走 ScriptRuntime.toString 做 JS 字符串转换；直接调用 Kotlin 的 toString() 会拿到包装类名
                else -> this.toString(bodyAny) to ContentType.Text.Plain
            }
            val resolvedCt = ctStr?.let { runCatching { ContentType.parse(it) }.getOrNull() ?: baseCt } ?: baseCt
            return RespSpec(status, resolvedCt, userHeaders, finalBody)
        }

        // 其它（字符串 / 数字 / 布尔）统一作为 200 text/plain，同样走 JS 字符串转换
        return RespSpec(200, ContentType.Text.Plain, emptyMap(), this.toString(value))
    }

    /** 构建 server 句柄对象（id/host/port/url/isRunning/stop/error） */
    private fun buildServerHandle(running: RunningServer): JsAny {
        val id = running.id
        return Object("serverHandle") {
            "id".js eq id.js
            "host".js eq running.host.js
            "port".js eq running.port.js
            "url".js eq "http://${running.host}:${running.port}".js
            listOf("isRunning".js, "是否在运行".js).func { _ -> servers.containsKey(id).js }
            listOf("stop".js, "关闭".js, "停止".js).func { _ -> stopServer(id); null }
            "error".js eq "".js
        }
    }

    /** 构建 server 信息对象（用于 server.list） */
    private fun buildServerInfo(s: RunningServer): JsAny {
        return Object("serverInfo") {
            "id".js eq s.id.js
            "host".js eq s.host.js
            "port".js eq s.port.js
            "url".js eq "http://${s.host}:${s.port}".js
            "isRunning".js eq true.js
        }
    }

    /** 启动失败时的错误句柄 */
    private fun buildServerHandleError(msg: String): JsAny {
        return Object("serverHandle") {
            "id".js eq "".js
            "host".js eq "".js
            "port".js eq 0.js
            "url".js eq "".js
            listOf("isRunning".js, "是否在运行".js).func { _ -> false.js }
            listOf("stop".js, "关闭".js, "停止".js).func { _ -> null }
            "error".js eq msg.js
        }
    }
}
