package com.kungal.core.net

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * RFC 9457 problem+json，kungal v1 的全部错误响应形状。
 * code 为稳定的大写下划线标识（见站点 /api/v1/problems）；未知 code 按 status 兜底。
 */
@Serializable
data class Problem(
    val type: String = "",
    val title: String = "",
    val status: Int = 0,
    val detail: String = "",
    val instance: String = "",
    val code: String = "",
    @kotlinx.serialization.SerialName("request_id")
    val requestId: String = "",
    val errors: List<JsonElement> = emptyList(),
    /** ENTITY_MERGED 的合并目标 id（服务端 problem 扩展字段）。 */
    @kotlinx.serialization.SerialName("current_id")
    val currentId: String? = null,
) {
    /** 401 且 code 属于凭证类错误 → 需要引导用户重新登录。 */
    val requiresLogin: Boolean
        get() = code == "MISSING_CREDENTIAL" || code == "INVALID_CREDENTIAL"
}
