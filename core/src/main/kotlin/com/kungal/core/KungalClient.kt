package com.kungal.core

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
        const val USER_AGENT = "kungal-app/0.2.0 (Android)"
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
            if (value != null) builder.addQueryParameter(key, value.toString())
        }
        return builder.build()
    }

    suspend fun <T> get(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path, query)).get().build(), deserializer)

    suspend fun <T> postJson(
        path: String,
        body: String,
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val builder = request(url(path)).post(body.toRequestBody(JSON_MEDIA_TYPE))
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey)
        return execute(builder.build(), deserializer)
    }

    /** 无请求体的 PUT（如 PUT /topics/{id}/reactions/{reaction}）。 */
    suspend fun <T> put(
        path: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path)).put(EMPTY_BODY).build(), deserializer)

    /** 带 JSON 请求体的 PUT（如 PUT /me/nsfw-display）。 */
    suspend fun <T> putJson(
        path: String,
        body: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(
        request(url(path)).put(body.toRequestBody(JSON_MEDIA_TYPE)).build(),
        deserializer,
    )

    /** 带 JSON 请求体的 PATCH（如编辑话题/回复/评论）。 */
    suspend fun <T> patchJson(
        path: String,
        body: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(
        request(url(path)).patch(body.toRequestBody(JSON_MEDIA_TYPE)).build(),
        deserializer,
    )

    /** multipart 上传（如 POST /images）。 */
    suspend fun <T> postMultipart(
        path: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        fields: Map<String, String> = emptyMap(),
        idempotencyKey: String? = null,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fileBytes.toRequestBody(contentType.toMediaType()).let { part ->
            bodyBuilder.addFormDataPart("file", fileName, part)
        }
        fields.forEach { (key, value) -> bodyBuilder.addFormDataPart(key, value) }
        val builder = request(url(path)).post(bodyBuilder.build())
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

    /** 无请求体、但需要解析响应的 POST（如抽奖兑换码揭示）。 */
    suspend fun <T> postNoBodyJson(
        path: String,
        deserializer: DeserializationStrategy<T>,
    ): T = execute(request(url(path)).post(EMPTY_BODY).build(), deserializer)

    /** 无响应体的 DELETE（如删除回复/评论/草稿，204）。 */
    suspend fun deleteNoBody(path: String) {
        withContext(Dispatchers.IO) {
            val response = http.newCall(request(url(path)).delete().build()).execute()
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
        withContext(Dispatchers.IO) {
            val response = http.newCall(request).execute()
            response.use { r ->
                val bodyText = r.body.string()
                if (!r.isSuccessful) {
                    throw toException(r.code, r.header("Retry-After"), bodyText)
                }
                try {
                    json.decodeFromString(deserializer, bodyText)
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
