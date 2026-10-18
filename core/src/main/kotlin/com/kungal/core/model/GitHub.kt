package com.kungal.core.model

import kotlinx.serialization.Serializable

/** GitHub 公开贡献日历里的一天。 */
@Serializable
data class GitHubContributionDay(
    val date: String = "",
    val level: Int = 0,
)

/**
 * GitHub 公开贡献页的解析结果。
 * [weeks] 按 GitHub 的排法组织：每周 7 格，周日在前；最后一周未来日期为 null。
 */
@Serializable
data class GitHubContributions(
    val total: Int = 0,
    val days: List<GitHubContributionDay> = emptyList(),
    val weeks: List<List<GitHubContributionDay?>> = emptyList(),
)
