package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 用户排行榜条目（/rankings/users）：按萌萌点/话题/回复/评论/资源排序。 */
@Serializable
data class UserRankingEntry(
    @SerialName("object") val obj: String = "user_ranking_entry",
    val rank: Int = 0,
    @SerialName("metric_value") val metricValue: Double = 0.0,
    val member: UserRef = UserRef(),
    val bio: String? = null,
)

/** 话题排行榜条目（/rankings/topics）；topic 正常不为 null。 */
@Serializable
data class TopicRankingEntry(
    @SerialName("object") val obj: String = "topic_ranking_entry",
    val rank: Int = 0,
    @SerialName("metric_value") val metricValue: Double = 0.0,
    val topic: Topic? = null,
)

/** Galgame 作品排行榜条目（/rankings/works）；creator 可为空。 */
@Serializable
data class WorkRankingEntry(
    @SerialName("object") val obj: String = "work_ranking_entry",
    val rank: Int = 0,
    @SerialName("metric_value") val metricValue: Double = 0.0,
    val work: WorkRef? = null,
    val creator: UserRef? = null,
)
