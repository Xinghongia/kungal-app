package com.kungal.core.net

/**
 * 需要响应头（ETag / Location）的调用结果。编辑提案的并发控制靠 ETag：
 * 读取时拿到，写回时用 `If-Match` 原样带回。
 */
data class ApiResponse<T>(
    val body: T,
    val etag: String? = null,
    val location: String? = null,
)
