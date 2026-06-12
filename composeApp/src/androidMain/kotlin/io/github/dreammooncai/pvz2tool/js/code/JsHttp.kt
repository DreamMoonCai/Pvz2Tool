package io.github.dreammooncai.pvz2tool.js.code

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.js.PvzToolJsEngine
import io.github.dreammooncai.pvz2tool.js.func
import io.github.dreammooncai.pvz2tool.js.orNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * JS 网络 HTTP 客户端对象
 *
 * JS 中通过 `http` / `网络` 访问，提供：
 * - http.get(url, options?)       — GET 请求
 * - http.post(url, body?, options?) — POST 请求
 * - http.put(url, body?, options?)  — PUT 请求
 * - http.delete(url, options?)    — DELETE 请求
 * - http.patch(url, body?, options?) — PATCH 请求
 * - http.head(url, options?)      — HEAD 请求
 * - http.request(options)         — 通用请求，options: { url, method, headers, body, contentType, timeout }
 *
 * 每个方法返回一个 Response 对象：
 * {
 *   status: number,           — HTTP 状态码
 *   ok: boolean,              — status 是否在 200..299
 *   statusText: string,       — 状态文本（如 "OK"）
 *   headers: object,          — 响应头（key: string => value: string）
 *   body: string,             — 响应体文本
 *   json(): any               — 将 body 解析为 JSON 对象
 * }
 *
 * options 参数（所有方法通用，均可选）：
 * {
 *   headers: object,          — 请求头（key: string => value: string）
 *   contentType: string,      — 请求 Content-Type（默认 "application/json"）
 *   timeout: number,          — 超时毫秒（默认 30000）
 * }
 */
object JsHttp {
    val js = Object("http") {

        // ======================== GET ========================
        listOf("get".js, "获取".js).func(
            FunctionParam("url"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val options = args.getOrNull(1)?.orNull
            val (headers, _, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.get(url) {
                        headers { appendAllHeaders(headers) }
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== POST ========================
        listOf("post".js, "提交".js).func(
            FunctionParam("url"),
            FunctionParam("body"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val body = args.getOrNull(1)?.orNull?.let { toString(it) } ?: ""
            val options = args.getOrNull(2)?.orNull
            val (headers, ct, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.post(url) {
                        contentType(ContentType.parse(ct))
                        headers { appendAllHeaders(headers) }
                        setBody(body)
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== PUT ========================
        listOf("put".js, "上传".js).func(
            FunctionParam("url"),
            FunctionParam("body"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val body = args.getOrNull(1)?.orNull?.let { toString(it) } ?: ""
            val options = args.getOrNull(2)?.orNull
            val (headers, ct, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.put(url) {
                        contentType(ContentType.parse(ct))
                        headers { appendAllHeaders(headers) }
                        setBody(body)
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== DELETE ========================
        listOf("delete".js, "删除".js).func(
            FunctionParam("url"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val options = args.getOrNull(1)?.orNull
            val (headers, _, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.delete(url) {
                        headers { appendAllHeaders(headers) }
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== PATCH ========================
        listOf("patch".js, "修改".js).func(
            FunctionParam("url"),
            FunctionParam("body"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val body = args.getOrNull(1)?.orNull?.let { toString(it) } ?: ""
            val options = args.getOrNull(2)?.orNull
            val (headers, ct, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.patch(url) {
                        contentType(ContentType.parse(ct))
                        headers { appendAllHeaders(headers) }
                        setBody(body)
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== HEAD ========================
        listOf("head".js, "头部".js).func(
            FunctionParam("url"),
            FunctionParam("options")
        ) { args ->
            val url = toString(args[0])
            val options = args.getOrNull(1)?.orNull
            val (headers, _, timeout) = parseOptions(options)

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = client.head(url) {
                        headers { appendAllHeaders(headers) }
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }

        // ======================== 通用 request ========================
        listOf("request".js, "请求".js).func(
            FunctionParam("options")
        ) { args ->
            val optionsAny = args.getOrNull(0)?.orNull
                ?: return@func null
            val options = optionsAny as? JsObject
                ?: return@func null

            val url = options.get("url".js, this)?.orNull?.let { toString(it) }
                ?: options.get("地址".js, this)?.orNull?.let { toString(it) }
                ?: return@func null

            val method = (options.get("method".js, this)?.orNull?.let { toString(it) }
                ?: options.get("方法".js, this)?.orNull?.let { toString(it) }
                ?: "GET").uppercase()

            val (hdrs, ct, timeout) = parseOptions(options)

            val body = options.get("body".js, this)?.orNull?.let { toString(it) }
                ?: options.get("数据".js, this)?.orNull?.let { toString(it) }
                ?: ""

            try {
                buildClient(timeout).use { client ->
                    val response: HttpResponse = when (method) {
                        "GET" -> client.get(url) {
                            headers { appendAllHeaders(hdrs) }
                        }

                        "POST" -> client.post(url) {
                            contentType(ContentType.parse(ct))
                            headers { appendAllHeaders(hdrs) }
                            setBody(body)
                        }

                        "PUT" -> client.put(url) {
                            contentType(ContentType.parse(ct))
                            headers { appendAllHeaders(hdrs) }
                            setBody(body)
                        }

                        "DELETE" -> client.delete(url) {
                            headers { appendAllHeaders(hdrs) }
                        }

                        "PATCH" -> client.patch(url) {
                            contentType(ContentType.parse(ct))
                            headers { appendAllHeaders(hdrs) }
                            setBody(body)
                        }

                        "HEAD" -> client.head(url) {
                            headers { appendAllHeaders(hdrs) }
                        }

                        else -> client.get(url) {
                            headers { appendAllHeaders(hdrs) }
                        }
                    }
                    buildResponseObject(response)
                }
            } catch (e: Exception) {
                buildErrorResponseObject(
                    statusCode = 0, statusText = "Request Failed: ${e::class.simpleName}", body = e.message ?: "Unknown error"
                )
            }
        }
    }

}

// ==================== 内部工具函数 ====================

/** 解析 options 对象，返回 Triple(headers: Map, contentType: String, timeout: Long) */
private suspend fun ScriptRuntime.parseOptions(
    optionsAny: JsAny?
): Triple<Map<String, String>, String, Long> {
    val options = optionsAny as? JsObject ?: return Triple(
        emptyMap(), "application/json", 30_000L
    )

    // 解析 headers
    val headersObj = (options.get("headers".js, this)
        ?: options.get("请求头".js, this))?.orNull
    val headers = mutableMapOf<String, String>()
    if (headersObj is JsObject) {
        val kotlinMap = headersObj.toKotlin(this)
        if (kotlinMap is Map<*, *>) {
            kotlinMap.forEach { (k, v) ->
                if (k != null && v != null) {
                    headers[k.toString()] = v.toString()
                }
            }
        }
    }

    // 解析 contentType
    val ct = (options.get("contentType".js, this)
        ?: options.get("内容类型".js, this))?.orNull?.let { toString(it) }
        ?: "application/json"

    // 解析 timeout（毫秒）
    val timeout = (options.get("timeout".js, this)
        ?: options.get("超时".js, this))?.orNull?.let { toNumber(it).toLong() }
        ?: 30_000L

    return Triple(headers, ct, timeout)
}

/** 构建错误响应对象（用于超时或网络异常） */
private fun buildErrorResponseObject(
    statusCode: Int, statusText: String, body: String
): JsAny {
    return Object("HttpResponse") {
        "status".js eq statusCode.js
        "ok".js eq false.js
        "statusText".js eq statusText.js
        "body".js eq body.js
        "headers".js eq Object("headers") {}
        listOf("json".js, "解析JSON".js).func {
            try {
                PvzToolJsEngine.parse(body)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/** 构建 Ktor HttpClient（每次请求独立 client，避免并发状态问题） */
private fun buildClient(timeoutMs: Long): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
        engine {
            connectTimeout = timeoutMs.toInt()
            socketTimeout = timeoutMs.toInt()
        }
    }
}

/** 将 Ktor 响应头追加到 Ktor HeadersBuilder */
private fun HeadersBuilder.appendAllHeaders(headers: Map<String, String>) {
    headers.forEach { (key, value) ->
        // 避免覆盖 Content-Type（已通过 contentType() 设置），但允许自定义其他头
        if (!key.equals(HttpHeaders.ContentType, ignoreCase = true)) {
            append(key, value)
        }
    }
}

/** 将 Ktor HttpResponse 转为 JS 对象 */
private suspend fun buildResponseObject(
    response: HttpResponse
): JsAny {
    val statusCode = response.status.value
    val statusDesc = response.status.description
    val bodyText = try { response.bodyAsText() } catch (e: Exception) { "" }

    // 收集响应头
    val responseHeaders = mutableMapOf<String, String>()
    response.headers.forEach { key, values ->
        responseHeaders[key] = values.joinToString(", ")
    }

    return Object("response") {
        "status".js eq statusCode.js
        "ok".js eq (statusCode in 200..299).js
        "statusText".js eq statusDesc.js
        "body".js eq bodyText.js
        // 响应头对象
        "headers".js eq Object("responseHeaders") {
            responseHeaders.forEach { (key, value) ->
                key.js eq value.js
            }
        }
        // json() 方法：将 body 解析为 JS 对象
        listOf("json".js, "解析JSON".js).func {
            try {
                PvzToolJsEngine.parse(bodyText)
            } catch (e: Exception) {
                null
            }
        }
    }
}
