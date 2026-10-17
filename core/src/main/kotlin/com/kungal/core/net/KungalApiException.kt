package com.kungal.core.net

/**
 * v1 API 的失败结果。problem 为解析出的 RFC 9457 错误体；解析失败时为 null。
 */
class KungalApiException(
    val problem: Problem?,
    val httpStatus: Int,
    message: String,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    val requiresLogin: Boolean
        get() = httpStatus == 401 || problem?.requiresLogin == true

    val isRateLimited: Boolean
        get() = httpStatus == 429
}
