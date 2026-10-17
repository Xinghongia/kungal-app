package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.oauth.Pkce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 账号中心（NextMoe）返回的非 0 业务码；message 为服务端中文提示。 */
class AccountApiException(
    val code: Int,
    val httpStatus: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * NextMoe 账号中心的原生登录（与网页登录同一套 OAuth 流程）：
 * 账号密码 → access_token → 授权同意 → 论坛回调换取 `kungal_session` cookie。
 *
 * 只使用网站前端公开的 OAuth 客户端（[WEB_CLIENT_ID]），不冒用官方 App 的
 * Bearer client_id；密码只参与本次请求，不落盘、不写日志。
 */
class AccountAuthApi(
    private val oauthBaseUrl: String = DEFAULT_OAUTH_BASE_URL,
    private val forumBaseUrl: String = DEFAULT_FORUM_BASE_URL,
    private val http: OkHttpClient = defaultHttp(),
) {
    companion object {
        const val DEFAULT_OAUTH_BASE_URL = "https://account.nextmoe.com/api/v1"
        const val DEFAULT_FORUM_BASE_URL = "https://www.kungal.com/api"

        /** 鲲 Galgame 论坛网页端公开的 OAuth 客户端（来自站点前端配置）。 */
        const val WEB_CLIENT_ID = "4ed9bc99ec0a789a4796b83e22bd84c5"
        const val REDIRECT_URI = "https://www.kungal.com/auth/callback"
        const val SCOPE =
            "openid profile preferences catalog:read catalog:edit playtime:read playtime:write " +
                "folder:read folder:write chat:read chat:write"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(MemoryCookieJar())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Serializable
    private data class Envelope<T>(
        val code: Int = -1,
        val message: String = "",
        val data: T? = null,
    )

    @Serializable
    private data class LoginData(
        @SerialName("access_token") val accessToken: String = "",
    )

    @Serializable
    private data class ConsentData(
        @SerialName("redirect_url") val redirectUrl: String = "",
    )

    private data class RawResponse(
        val status: Int,
        val headers: Headers,
        val body: String,
    )

    /** 仅登录，拿到 OAuth access_token（不建立论坛会话）。 */
    suspend fun login(account: String, password: String): String {
        val data = postOAuth(
            path = "/auth/login",
            body = buildJsonObject {
                put("account", account)
                put("password", password)
            }.toString(),
            deserializer = LoginData.serializer(),
        )
        return data?.accessToken?.takeIf { it.isNotBlank() }
            ?: throw AccountApiException(-1, 0, "登录响应缺少凭证，请重试")
    }

    /** 发送注册邮箱验证码。 */
    suspend fun sendRegisterCode(name: String, email: String) {
        postOAuth(
            path = "/auth/register/send-code",
            body = buildJsonObject {
                put("name", name)
                put("email", email)
            }.toString(),
            deserializer = JsonElement.serializer(),
        )
    }

    /** 注册并拿到 OAuth access_token。 */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        code: String,
    ): String {
        val data = postOAuth(
            path = "/auth/register",
            body = buildJsonObject {
                put("name", name)
                put("email", email)
                put("password", password)
                put("code", code)
            }.toString(),
            deserializer = LoginData.serializer(),
        )
        return data?.accessToken?.takeIf { it.isNotBlank() }
            ?: throw AccountApiException(-1, 0, "注册响应缺少凭证，请重试")
    }

    /** 账号密码登录并完成 OAuth，返回可直接用于 API 的 `kungal_session=...`。 */
    suspend fun loginAndAuthorize(account: String, password: String): String =
        authorize(login(account, password))

    /** 注册并完成 OAuth，返回 `kungal_session=...`。 */
    suspend fun registerAndAuthorize(
        name: String,
        email: String,
        password: String,
        code: String,
    ): String = authorize(register(name, email, password, code))

    /**
     * 用 access_token 请求授权同意（论坛客户端 auto_consent），
     * 从回调地址取 code，再让论坛后端换取会话 cookie。
     */
    suspend fun authorize(accessToken: String): String {
        val pkce = Pkce.generate()
        val redirectUrl = postOAuth(
            path = "/oauth/authorize/consent",
            body = buildJsonObject {
                put("client_id", WEB_CLIENT_ID)
                put("redirect_uri", REDIRECT_URI)
                put("response_type", "code")
                put("scope", SCOPE)
                put("state", pkce.state)
                put("code_challenge", pkce.challenge)
                put("code_challenge_method", "S256")
            }.toString(),
            bearer = accessToken,
            deserializer = ConsentData.serializer(),
        )?.redirectUrl?.takeIf { it.isNotBlank() }
            ?: throw AccountApiException(-1, 0, "授权失败：未收到回调地址")

        val url = redirectUrl.toHttpUrlOrNull()
            ?: throw AccountApiException(-1, 0, "授权失败：回调地址无效")
        url.queryParameter("error")?.let { error ->
            throw AccountApiException(
                -1,
                0,
                oauthErrorMessage(error, url.queryParameter("error_description")),
            )
        }
        if (url.queryParameter("state") != pkce.state) {
            throw AccountApiException(-1, 0, "登录校验失败，请重试")
        }
        val code = url.queryParameter("code")?.takeIf { it.isNotBlank() }
            ?: throw AccountApiException(-1, 0, "授权失败：未收到授权码")
        return completeForumLogin(code, pkce.verifier)
    }

    private suspend fun completeForumLogin(code: String, verifier: String): String {
        val response = postRaw(
            url = "$forumBaseUrl/auth/oauth/callback",
            body = buildJsonObject {
                put("code", code)
                put("code_verifier", verifier)
            }.toString(),
        )
        val envelope = runCatching {
            KungalJson.decodeFromString(
                Envelope.serializer(JsonElement.serializer()),
                response.body,
            )
        }.getOrNull()
            ?: throw AccountApiException(
                -1,
                response.status,
                if (response.status == 0) "网络连接失败，请检查网络后重试"
                else "登录服务返回异常（HTTP ${response.status}）",
            )
        if (envelope.code != 0) {
            throw AccountApiException(
                envelope.code,
                response.status,
                envelope.message.ifBlank { "登录失败，请重试" },
            )
        }
        val sessionHeader = response.headers.values("Set-Cookie").firstOrNull {
            it.substringBefore(';').trim().startsWith("kungal_session=")
        } ?: throw AccountApiException(envelope.code, response.status, "登录成功但未收到会话，请重试")
        return sessionHeader.substringBefore(';').trim()
    }

    private suspend fun <T> postOAuth(
        path: String,
        body: String,
        bearer: String? = null,
        deserializer: KSerializer<T>,
    ): T? {
        val response = postRaw("$oauthBaseUrl$path", body, bearer)
        val envelope = runCatching {
            KungalJson.decodeFromString(Envelope.serializer(deserializer), response.body)
        }.getOrNull()
            ?: throw AccountApiException(
                -1,
                response.status,
                if (response.status == 0) "网络连接失败，请检查网络后重试"
                else "账号服务返回异常（HTTP ${response.status}）",
            )
        if (envelope.code != 0) {
            throw AccountApiException(
                envelope.code,
                response.status,
                envelope.message.ifBlank { "操作失败（${envelope.code}）" },
            )
        }
        return envelope.data
    }

    private suspend fun postRaw(url: String, body: String, bearer: String? = null): RawResponse =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(url)
                .post(body.toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("User-Agent", KungalClient.USER_AGENT)
            if (bearer != null) builder.header("Authorization", "Bearer $bearer")
            try {
                http.newCall(builder.build()).execute().use { response ->
                    RawResponse(
                        status = response.code,
                        headers = response.headers,
                        body = response.body.string(),
                    )
                }
            } catch (e: IOException) {
                throw AccountApiException(
                    -1,
                    0,
                    "网络连接失败，请检查网络后重试",
                    e,
                )
            }
        }

    private fun oauthErrorMessage(error: String, description: String?): String = when (error) {
        "login_required" -> "登录已失效，请重试"
        "consent_required" -> "需要手动确认授权，请改用网页登录"
        "access_denied" -> "授权被拒绝"
        "banned" -> "账号已被封禁"
        "stepup_required" -> "该账号需要额外验证，请改用网页登录"
        else -> description?.takeIf { it.isNotBlank() } ?: "授权失败（$error）"
    }

    /** 仅用于账号中心每次响应可能种下的 cookie（如 nm_browser）。 */
    private class MemoryCookieJar : CookieJar {
        private val byHost = mutableMapOf<String, MutableList<Cookie>>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = byHost.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { cookie ->
                list.removeAll { it.name == cookie.name && it.path == cookie.path }
                if (cookie.expiresAt > System.currentTimeMillis()) list.add(cookie)
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            byHost[url.host].orEmpty().filter { it.matches(url) }
    }
}
