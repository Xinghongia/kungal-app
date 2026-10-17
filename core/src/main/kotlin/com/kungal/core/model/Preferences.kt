package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 云端偏好文档（/me/preferences）：doc 是站点定义的 JSON 对象，
 * 客户端只改自己认识的键，其余键（含 feed_tabs）原样保留。
 */
@Serializable
data class CloudPreferences(
    @SerialName("object") val obj: String = "preferences",
    val version: Long = 0,
    @SerialName("written_at") val writtenAt: String? = null,
    val doc: JsonObject = JsonObject(emptyMap()),
)
