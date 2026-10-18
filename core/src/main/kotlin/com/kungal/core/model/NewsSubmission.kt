package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 我的情报投稿（/me/news-submissions）。同一 id 空间与公开情报条目共用：
 * pending 待审核 / published 已发布 / rejected 未通过 / withdrawn 已撤回。
 */
@Serializable
data class NewsSubmission(
    @SerialName("object") val obj: String = "news_submission",
    val id: String = "",
    val state: String = "pending",
    @SerialName("news_source") val newsSource: String = "community",
    val lane: String = "news",
    val title: String = "",
    val preview: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
    @SerialName("source_url") val sourceUrl: String = "",
    val banner: ImageInfo? = null,
    @SerialName("published_at") val publishedAt: String = "",
)

/** 新建投稿请求体：正文与原文链接可留空，但两者不能同时为空（界面校验）。 */
@Serializable
data class NewsSubmissionCreate(
    val title: String,
    val preview: String,
    @SerialName("content_markdown") val contentMarkdown: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("banner_image_hash") val bannerImageHash: String? = null,
)

/** 更新投稿：只发变化的字段；`banner_image_hash = ""` 表示移除封面。 */
@Serializable
data class NewsSubmissionPatch(
    val title: String? = null,
    val preview: String? = null,
    @SerialName("content_markdown") val contentMarkdown: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("banner_image_hash") val bannerImageHash: String? = null,
    /** 只允许 "withdrawn"，且只能单独发送（仅 published 状态可撤回）。 */
    val state: String? = null,
)
