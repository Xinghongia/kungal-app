package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 版块（Section）：站点分类下的二级页面，带话题数/浏览数与最新话题。
 * 对应 v1 GET /sections?category=...
 */
@Serializable
data class Section(
    @SerialName("object") val obj: String = "section",
    val section: String = "",
    val category: String = "",
    @SerialName("topic_count") val topicCount: Int = 0,
    @SerialName("view_count") val viewCount: Long = 0,
    @SerialName("latest_topic") val latestTopic: SectionLatestTopic? = null,
)

@Serializable
data class SectionLatestTopic(
    val id: String = "",
    val title: String = "",
    @SerialName("created_at") val createdAt: String = "",
)
