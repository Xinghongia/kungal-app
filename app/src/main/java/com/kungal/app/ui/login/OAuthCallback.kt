package com.kungal.app.ui.login

import com.kungal.core.api.AccountAuthApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** 网页 OAuth 回调里的结果（成功带 code + state，失败带 error）。 */
internal data class OAuthCallback(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
)

/** 只认站点注册的回调地址，避免把其他页面的参数误当授权结果。 */
internal fun parseOAuthCallback(url: String): OAuthCallback? {
    val parsed = url.toHttpUrlOrNull() ?: return null
    val expected = AccountAuthApi.REDIRECT_URI.toHttpUrl()
    if (parsed.scheme != expected.scheme ||
        parsed.host != expected.host ||
        parsed.encodedPath != expected.encodedPath
    ) {
        return null
    }
    return OAuthCallback(
        code = parsed.queryParameter("code"),
        state = parsed.queryParameter("state"),
        error = parsed.queryParameter("error"),
        errorDescription = parsed.queryParameter("error_description"),
    )
}
