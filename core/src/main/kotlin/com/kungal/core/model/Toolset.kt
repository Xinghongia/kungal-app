package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 工具集详情（GET /toolsets/{id}）。每次读取会让浏览数 +1。 */
@Serializable
data class Toolset(
    @SerialName("object") val obj: String = "toolset",
    val id: String = "",
    val title: String = "",
    val aliases: List<String> = emptyList(),
    @SerialName("toolset_type") val toolsetType: String = "others",
    @SerialName("interface_language") val interfaceLanguage: String = "zh-cn",
    val platform: String = "windows",
    @SerialName("release_channel") val releaseChannel: String = "stable",
    @SerialName("homepage_urls") val homepageUrls: List<String> = emptyList(),
    val author: UserRef = UserRef(),
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("practicality_average") val practicalityAverage: Double? = null,
    @SerialName("practicality_count") val practicalityCount: Int = 0,
    @SerialName("practicality_distribution") val practicalityDistribution: List<Int> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("resource_updated_at") val resourceUpdatedAt: String? = null,
    val content: ContentDocument? = null,
    val contributors: List<UserRef> = emptyList(),
    @SerialName("toolset_resources") val toolsetResources: List<ToolsetResource> = emptyList(),
    val viewer: ToolsetViewer? = null,
)

/** 当前登录用户对工具集的状态；未登录时为 null。 */
@Serializable
data class ToolsetViewer(
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("practicality_rating") val practicalityRating: Int? = null,
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
)

/** 工具集详情里的资源条目（file = S3 托管，link = 外链）。 */
@Serializable
data class ToolsetResource(
    @SerialName("object") val obj: String = "toolset_resource",
    val id: String = "",
    @SerialName("toolset_resource_type") val type: String = "link",
    /** file 资源：托管归档的大小（字节）。 */
    val archive: ToolsetResourceArchive? = null,
    /** link 资源：发布者填写的体积文本。 */
    val link: ToolsetResourceLink? = null,
    val note: String? = null,
    @SerialName("download_count") val downloadCount: Int = 0,
    val poster: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
    val viewer: ToolsetResourceViewer? = null,
)

@Serializable
data class ToolsetResourceArchive(
    @SerialName("file_size") val fileSize: Long = 0,
)

@Serializable
data class ToolsetResourceLink(
    @SerialName("size_label") val sizeLabel: String = "",
)

@Serializable
data class ToolsetResourceViewer(
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)

/** 收藏/取消收藏结果（PUT/DELETE /toolsets/{id}/favorite）。 */
@Serializable
data class ToolsetEngagement(
    @SerialName("object") val obj: String = "toolset_engagement",
    @SerialName("toolset_id") val toolsetId: String = "",
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    val viewer: ToolsetEngagementViewer? = null,
)

@Serializable
data class ToolsetEngagementViewer(
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
)

/** 实用性评分结果（PUT /toolsets/{id}/practicality）。 */
@Serializable
data class ToolsetPracticality(
    @SerialName("object") val obj: String = "toolset_practicality",
    @SerialName("toolset_id") val toolsetId: String = "",
    @SerialName("practicality_average") val practicalityAverage: Double? = null,
    @SerialName("practicality_count") val practicalityCount: Int = 0,
    @SerialName("practicality_distribution") val practicalityDistribution: List<Int> = emptyList(),
    val viewer: ToolsetPracticalityViewer? = null,
)

@Serializable
data class ToolsetPracticalityViewer(
    @SerialName("practicality_rating") val practicalityRating: Int? = null,
)

/** 工具集编辑源数据（GET /toolsets/{id}/source）。 */
@Serializable
data class ToolsetSource(
    @SerialName("object") val obj: String = "toolset_source",
    @SerialName("toolset_id") val toolsetId: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
)

/** 工具资源下载（POST /toolsets/{id}/resources/{rid}/downloads）。 */
@Serializable
data class ToolsetDownload(
    @SerialName("object") val obj: String = "toolset_download",
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("extraction_code") val extractionCode: String = "",
    @SerialName("archive_password") val archivePassword: String = "",
)

/** 工具资源编辑源数据（GET /toolsets/{id}/resources/{rid}/source）。 */
@Serializable
data class ToolsetResourceSource(
    @SerialName("object") val obj: String = "toolset_resource_source",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("extraction_code") val extractionCode: String = "",
    @SerialName("archive_password") val archivePassword: String = "",
    val note: String? = null,
    @SerialName("size_label") val sizeLabel: String = "",
    @SerialName("link_url") val linkUrl: String = "",
)

/** 上传会话（POST/GET/PATCH /toolsets/{id}/uploads[/upload_id]）。 */
@Serializable
data class ToolsetUpload(
    @SerialName("object") val obj: String = "toolset_upload",
    val id: String = "",
    @SerialName("toolset_id") val toolsetId: String = "",
    val filename: String = "",
    @SerialName("file_size") val fileSize: Long = 0,
    /** pending / completed */
    val state: String = "pending",
    @SerialName("is_multipart") val isMultipart: Boolean = false,
    @SerialName("upload_url") val uploadUrl: String? = null,
    @SerialName("part_size") val partSize: Long? = null,
    @SerialName("part_urls") val partUrls: List<ToolsetUploadPart> = emptyList(),
    @SerialName("uploaded_parts") val uploadedParts: List<ToolsetUploadedPart> = emptyList(),
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class ToolsetUploadPart(
    @SerialName("part_number") val partNumber: Int = 0,
    val url: String = "",
)

@Serializable
data class ToolsetUploadedPart(
    @SerialName("part_number") val partNumber: Int = 0,
    val etag: String = "",
    @SerialName("byte_count") val byteCount: Long = 0,
)

@Serializable
data class ToolsetUploadCreate(
    val filename: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("content_type") val contentType: String,
)

@Serializable
data class ToolsetUploadCompletePart(
    @SerialName("part_number") val partNumber: Int,
    val etag: String,
)
