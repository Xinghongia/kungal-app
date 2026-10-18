package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 站点更新日志（GET /update-logs）。 */
@Serializable
data class UpdateLog(
    @SerialName("object") val obj: String = "update_log",
    val id: String = "",
    @SerialName("change_type") val changeType: String = "",
    @SerialName("release_version") val releaseVersion: String = "",
    val text: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: UpdateLogViewer? = null,
)

/** 当前用户对更新日志的管理权限；匿名或 Bearer 登录时为 null。 */
@Serializable
data class UpdateLogViewer(
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)
