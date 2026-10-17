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

/** 领取到的下载信息（POST /galgame-resources/{id}/downloads）。 */
@Serializable
data class ResourceDownload(
    @SerialName("object") val obj: String = "galgame_resource_download",
    @SerialName("download_urls") val downloadUrls: List<String> = emptyList(),
    @SerialName("extraction_code") val extractionCode: String = "",
    @SerialName("archive_password") val archivePassword: String = "",
)

/** 资源点赞结果（PUT/DELETE /galgame-resources/{id}/like）。 */
@Serializable
data class ResourceEngagement(
    @SerialName("object") val obj: String = "galgame_resource_engagement",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    val viewer: ResourceEngagementViewer? = null,
)

@Serializable
data class ResourceEngagementViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
)

/** 失效报告结果（POST /galgame-resources/{id}/expiry-reports）。 */
@Serializable
data class ResourceExpiryReport(
    @SerialName("object") val obj: String = "galgame_resource_expiry_report",
    @SerialName("resource_id") val resourceId: String = "",
    /** alive / dead / unchecked */
    val verdict: String = "unchecked",
    /** 报告后的资源状态：valid / expired */
    val state: String = "valid",
)

/** 资源编辑源数据（GET /galgame-resources/{id}/source）。 */
@Serializable
data class ResourceSource(
    @SerialName("object") val obj: String = "galgame_resource_source",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("resource_type") val resourceType: String = "game",
    val title: String = "",
    @SerialName("version_label") val versionLabel: String? = null,
    @SerialName("resource_languages") val resourceLanguages: List<String> = emptyList(),
    @SerialName("resource_platforms") val resourcePlatforms: List<String> = emptyList(),
    @SerialName("resource_runtimes") val resourceRuntimes: List<String> = emptyList(),
    val size: String = "",
    @SerialName("download_urls") val downloadUrls: List<String> = emptyList(),
    @SerialName("extraction_code") val extractionCode: String = "",
    @SerialName("archive_password") val archivePassword: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
)

/** moyu 补丁页（GET /works/{id}/moyu-patches）。 */
@Serializable
data class MoyuPatchList(
    @SerialName("object") val obj: String = "list",
    val items: List<MoyuPatch> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class MoyuPatch(
    @SerialName("object") val obj: String = "moyu_patch",
    val id: String = "",
    @SerialName("web_url") val webUrl: String = "",
    val resources: List<MoyuPatchResource> = emptyList(),
)

/** moyu 上的补丁资源；只有网页地址，没有下载链接。 */
@Serializable
data class MoyuPatchResource(
    @SerialName("object") val obj: String = "moyu_patch_resource",
    val id: String = "",
    val name: String? = null,
    val storage: String = "",
    val size: String = "",
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("note_markdown") val noteMarkdown: String? = null,
    val types: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("web_url") val webUrl: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val publisher: UserRef = UserRef(),
)
