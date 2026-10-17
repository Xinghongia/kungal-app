package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 收录月份（/works/collected-months）：作品首次被本站收录的年月。 */
@Serializable
data class WorkCollectedMonth(
    val year: Int = 0,
    val month: Int = 0,
)

@Serializable
data class WorkCollectedMonths(
    @SerialName("object") val obj: String = "work_collected_months",
    val items: List<WorkCollectedMonth> = emptyList(),
)

/** 标签屏蔽（/me/blocked-tags）：tree = 连同子标签一起排除，exact = 只排除该标签。 */
@Serializable
data class TagBlock(
    @SerialName("object") val obj: String = "tag_block",
    val id: String = "",
    val mode: String = "tree",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class TagBlockList(
    @SerialName("object") val obj: String = "list",
    val items: List<TagBlock> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)
