package com.kungal.core

import com.kungal.core.net.ApiResponse
import com.kungal.core.net.KungalApiException
import com.kungal.core.net.Problem
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * kungal v1 REST 客户端。所有请求同域 `https://www.kungal.com/api/v1`，带会话 cookie。
 * 错误统一抛 [KungalApiException]（problem+json 解析结果在里面）。
 */
class KungalClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val http: OkHttpClient = defaultHttp(),
    val session: SessionHolder = SessionHolder(),
    private val json: Json = KungalJson,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://www.kungal.com/api/v1"
        const val USER_AGENT = "kungal-app/0.5.0 (Android)"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null, 0, 0)

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val base = baseUrl.trimEnd('/')

    fun url(path: String, query: Map<String, Any?> = emptyMap()): HttpUrl {
        val builder = "$base$path".toHttpUrl().newBuilder()
        for ((key, value) in query) {
            when (value) {
                null -> Unit
                // 数组参数按重复键传递（如 work-records 的 state=wish&state=doing）
                is Iterable<*> -> value.filterNotNull().forEach { builder.addQueryParameter(key, it.toString()) }
                else -> builder.addQueryParameter(key, value.toString())
            }
        }
        return builder.build()
    }

    suspend fun <T> get(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path, query)).get().build(), deserializer)

    /** 同 [get]，但保留 ETag / Location 响应头（编辑提案并发控制用）。 */
    suspend fun <T> getWithResponse(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): ApiResponse<T> = executeWithResponse(request(url(path, query)).get().build(), deserializer)

    suspend fun <T> postJson(
        path: String,
        body: String,
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): T = postJsonWithResponse(path, body, idempotencyKey = idempotencyKey, deserializer = deserializer).body

    /** 同 [postJson]，但保留 ETag / Location 响应头（修正提案用）。 */
    suspend fun <T> postJsonWithResponse(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): ApiResponse<T> {
        val builder = request(url(path)).post(body.toRequestBody(JSON_MEDIA_TYPE))
        headers.forEach { (key, value) -> builder.header(key, value) }
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
        return executeWithResponse(builder.build(), deserializer)
    }

    /** 无请求体的 PUT（如 PUT /topics/{id}/reactions/{reaction}）。 */
    suspend fun <T> put(
        path: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path)).put(EMPTY_BODY).build(), deserializer)

    /** 带 JSON 请求体的 PUT（如 PUT /me/nsfw-display）；headers 用于 If-Match 等。 */
    suspend fun <T> putJson(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): T {
        val builder = request(url(path)).put(body.toRequestBody(JSON_MEDIA_TYPE))
        headers.forEach { (key, value) -> builder.header(key, value) }
        return execute(builder.build(), deserializer)
    }

    /** 无请求体、无响应体的 PUT（如置顶私信消息）。 */
    suspend fun putNoBody(path: String) {
        withContext(Dispatchers.IO) {
            val response = http.newCall(request(url(path)).put(EMPTY_BODY).build()).execute()
            response.use { r ->
                if (!r.isSuccessful) {
                    throw toException(r.code, r.header("Retry-After"), r.body.string())
                }
            }
        }
    }

    /** 带 JSON 请求体的 PATCH（如编辑话题/回复/评论）。headers 用于 If-Match 等。 */
    suspend fun <T> patchJson(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): T = patchJsonWithResponse(path, body, headers, deserializer).body

    /** 同 [patchJson]，但保留 ETag 响应头（审阅提案用）。 */
    suspend fun <T> patchJsonWithResponse(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): ApiResponse<T> {
        val builder = request(url(path)).patch(body.toRequestBody(JSON_MEDIA_TYPE))
        headers.forEach { (key, value) -> builder.header(key, value) }
        return executeWithResponse(builder.build(), deserializer)
    }

    /** multipart 上传（如 POST /images）。 */
    suspend fun <T> postMultipart(
        path: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        fields: Map<String, String> = emptyMap(),
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): T = uploadMultipart(
        method = "POST", path = path, fileBytes = fileBytes, fileName = fileName,
        contentType = contentType, fields = fields, idempotencyKey = idempotencyKey,
        deserializer = deserializer,
    )

    /** multipart PUT（如 PUT /me/avatar）。 */
    suspend fun <T> putMultipart(
        path: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        fields: Map<String, String> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): T = uploadMultipart(
        method = "PUT", path = path, fileBytes = fileBytes, fileName = fileName,
        contentType = contentType, fields = fields, idempotencyKey = null,
        deserializer = deserializer,
    )

    private suspend fun <T> uploadMultipart(
        method: String,
        path: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        fields: Map<String, String>,
        idempotencyKey: String?,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fileBytes.toRequestBody(contentType.toMediaType()).let { part ->
            bodyBuilder.addFormDataPart("file", fileName, part)
        }
        fields.forEach { (key, value) -> bodyBuilder.addFormDataPart(key, value) }
        val builder = request(url(path)).method(method, bodyBuilder.build())
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
        return execute(builder.build(), deserializer)
    }

    /** DELETE。 */
    suspend fun <T> delete(
        path: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path)).delete().build(), deserializer)

    /** 无请求体、无响应体的 POST（如浏览计数 /topics/{id}/views，204）。 */
    suspend fun postNoBody(path: String) {
        withContext(Dispatchers.IO) {
            val response = http.newCall(request(url(path)).post(EMPTY_BODY).build()).execute()
            response.use { r ->
                if (!r.isSuccessful) {
                    throw toException(r.code, r.header("Retry-After"), r.body.string())
                }
            }
        }
    }

    /** 无请求体、但需要解析响应的 POST（如抽奖兑换码揭示、签到）。 */
    suspend fun <T> postNoBodyJson(
        path: String,
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val builder = request(url(path)).post(EMPTY_BODY)
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
        return execute(builder.build(), deserializer)
    }

    /** 无响应体的 POST（如删除类操作、举报 204）。 */
    suspend fun postJsonNoContent(
        path: String,
        body: String,
        idempotencyKey: String? = null,
    ) {
        val builder = request(url(path)).post(body.toRequestBody(JSON_MEDIA_TYPE))
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
        withContext(Dispatchers.IO) {
            val response = http.newCall(builder.build()).execute()
            response.use { r ->
                if (!r.isSuccessful) {
                    throw toException(r.code, r.header("Retry-After"), r.body.string())
                }
            }
        }
    }

    /** 打开 WebSocket（私信实时通道用；鉴权走 Centrifugo token，不带 cookie）。 */
    fun newWebSocket(url: String, listener: WebSocketListener): WebSocket =
        http.newWebSocket(Request.Builder().url(url).build(), listener)

    /** 下载图片等二进制内容（内容寻址 CDN 不需要会话）。 */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        response.use { r ->
            if (!r.isSuccessful) {
                throw KungalApiException(
                    problem = null,
                    httpStatus = r.code,
                    message = "下载失败（HTTP ${r.code}）",
                )
            }
            r.body.bytes()
        }
    }

    /** 无响应体的 DELETE（如删除回复/评论/草稿/投稿，204）；headers 用于 If-Match。 */
    suspend fun deleteNoBody(path: String, headers: Map<String, String> = emptyMap()) {
        val builder = request(url(path)).delete()
        headers.forEach { (key, value) -> builder.header(key, value) }
        withContext(Dispatchers.IO) {
            val response = http.newCall(builder.build()).execute()
            response.use { r ->
                if (!r.isSuccessful) {
                    throw toException(r.code, r.header("Retry-After"), r.body.string())
                }
            }
        }
    }

    private fun request(url: HttpUrl): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
        session.current?.let { builder.header("Cookie", it.cookieHeader) }
        return builder
    }

    private suspend fun <T> execute(request: Request, deserializer: DeserializationStrategy<T>): T =
        executeWithResponse(request, deserializer).body

    private suspend fun <T> executeWithResponse(
        request: Request,
        deserializer: DeserializationStrategy<T>,
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        val response = http.newCall(request).execute()
        response.use { r ->
            val bodyText = r.body.string()
            if (!r.isSuccessful) {
                throw toException(r.code, r.header("Retry-After"), bodyText)
            }
            try {
                ApiResponse(
                    body = json.decodeFromString(deserializer, bodyText),
                    etag = r.header("ETag"),
                    location = r.header("Location"),
                )
            } catch (e: SerializationException) {
                throw KungalApiException(
                    problem = null,
                    httpStatus = r.code,
                    message = "响应解析失败：${e.message}",
                    cause = e,
                )
            }
        }
    }

    private fun toException(status: Int, retryAfterHeader: String?, body: String): KungalApiException {
        val problem = runCatching { json.decodeFromString(Problem.serializer(), body) }.getOrNull()
        val message = problem?.let { if (it.detail.isNotBlank()) it.detail else it.title }
            .takeUnless { it.isNullOrBlank() }
            ?: "请求失败（HTTP $status）"
        return KungalApiException(problem, status, message, retryAfterHeader?.toLongOrNull())
    }
}
