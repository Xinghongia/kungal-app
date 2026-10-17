package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Galgame 下载资源（GET /galgame-resources、/works/{id}/resources）。
 * 资源本身不托管文件，只记录 1–20 条下载链接与提取码/解压码；
 * 下载链接需通过 POST /galgame-resources/{id}/downloads 领取。
 */
@Serializable
data class GalgameResource(
    @SerialName("object") val obj: String = "galgame_resource",
    val id: String = "",
    val work: WorkRef? = null,
    val author: UserRef = UserRef(),
    @SerialName("resource_type") val resourceType: String = "game",
    @SerialName("resource_languages") val resourceLanguages: List<String> = emptyList(),
    @SerialName("resource_platforms") val resourcePlatforms: List<String> = emptyList(),
    @SerialName("resource_runtimes") val resourceRuntimes: List<String> = emptyList(),
    val title: String = "",
    @SerialName("version_label") val versionLabel: String? = null,
    val size: String = "",
    @SerialName("provider_names") val providerNames: List<String> = emptyList(),
    val content: ContentDocument? = null,
    val state: String = "valid",
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val dlsite: DlsiteOffer? = null,
    val viewer: GalgameResourceViewer? = null,
) {
    val isExpired: Boolean get() = state == "expired"
}

/** 当前登录用户对资源的操作状态；未登录时为 null。 */
@Serializable
data class GalgameResourceViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)

/** 资源详情 404 时查询是否迁移到 LetMoe（GET /galgame-resource-relocations/{id}）。 */
@Serializable
data class ResourceRelocation(
    @SerialName("object") val obj: String = "galgame_resource_relocation",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("work_id") val workId: String = "",
    val url: String = "",
)
