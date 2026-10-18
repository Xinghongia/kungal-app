package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 网站分类引用（列表条目里的 website_category）。 */
@Serializable
data class WebsiteCategoryRef(
    @SerialName("object") val obj: String = "website_category",
    val id: String = "",
    val slug: String = "",
    val label: String = "",
)

/** 网站资料库分类（/website-categories），sort_order 升序即网页顺序。 */
@Serializable
data class WebsiteCategory(
    @SerialName("object") val obj: String = "website_category",
    val id: String = "",
    val slug: String = "",
    val label: String = "",
    val description: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("website_count") val websiteCount: Int = 0,
)

/** 网站标签（含价值 level，网站 score 为标签 level 之和）。 */
@Serializable
data class WebsiteTag(
    @SerialName("object") val obj: String = "website_tag",
    val id: String = "",
    val slug: String = "",
    val label: String = "",
    val description: String = "",
    val level: Int = 0,
    @SerialName("website_tag_group_id") val websiteTagGroupId: String? = null,
)

/** 网站标签分组（value bar 的分段依据）。 */
@Serializable
data class WebsiteTagGroup(
    @SerialName("object") val obj: String = "website_tag_group",
    val id: String = "",
    val slug: String = "",
    val label: String = "",
    val description: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_multi_select") val isMultiSelect: Boolean = false,
)

/** 资料库列表条目（GET /websites）。 */
@Serializable
data class WebsiteEntry(
    @SerialName("object") val obj: String = "website",
    val id: String = "",
    val host: String = "",
    val title: String = "",
    val description: String = "",
    val icon: ImageInfo? = null,
    @SerialName("external_icon_url") val externalIconUrl: String? = null,
    @SerialName("website_category") val websiteCategory: WebsiteCategoryRef = WebsiteCategoryRef(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    /** normal 正常 / unreachable 无法访问 / closed 已关站。 */
    val state: String = "normal",
    val score: Int = 0,
)

/** 列表信封：include_total=true 时才有 total。 */
@Serializable
data class WebsiteFeedPage(
    @SerialName("object") val obj: String = "list",
    val items: List<WebsiteEntry> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Int? = null,
)

@Serializable
data class WebsiteViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)

/** 网站详情（GET /websites/{host}；每次读取会增加一次访问量）。 */
@Serializable
data class WebsiteDetail(
    @SerialName("object") val obj: String = "website",
    val id: String = "",
    val host: String = "",
    val title: String = "",
    val description: String = "",
    val icon: ImageInfo? = null,
    @SerialName("external_icon_url") val externalIconUrl: String? = null,
    @SerialName("website_category") val websiteCategory: WebsiteCategoryRef = WebsiteCategoryRef(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    val state: String = "normal",
    val score: Int = 0,
    val language: String = "",
    val urls: List<String> = emptyList(),
    val founded: String = "",
    @SerialName("website_tags") val websiteTags: List<WebsiteTag> = emptyList(),
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: WebsiteViewer? = null,
)

@Serializable
data class WebsiteEngagementViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
)

/** 点赞/收藏后的计数与本人状态（PUT/DELETE /websites/{host}/{like|favorite}）。 */
@Serializable
data class WebsiteEngagement(
    @SerialName("object") val obj: String = "website_engagement",
    @SerialName("website_id") val websiteId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    val viewer: WebsiteEngagementViewer? = null,
)
