package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Poll(
    @SerialName("object") val obj: String = "poll",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("choice_type") val choiceType: String = "single",
    @SerialName("min_choice") val minChoice: Int = 1,
    @SerialName("max_choice") val maxChoice: Int = 1,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("result_visibility") val resultVisibility: String = "always",
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("can_change_vote") val canChangeVote: Boolean = true,
    val author: UserRef = UserRef(),
    val options: List<PollOption> = emptyList(),
    val results: PollResults? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: PollViewer? = null,
)

@Serializable
data class PollOption(
    @SerialName("object") val obj: String = "poll_option",
    val id: String = "",
    val text: String = "",
)

@Serializable
data class PollResults(
    @SerialName("total_vote_count") val totalVoteCount: Int = 0,
    @SerialName("voter_count") val voterCount: Int = 0,
    val options: List<PollOptionResult> = emptyList(),
    @SerialName("sample_voters") val sampleVoters: List<UserRef> = emptyList(),
)

@Serializable
data class PollOptionResult(
    @SerialName("option_id") val optionId: String = "",
    @SerialName("vote_count") val voteCount: Int = 0,
)

@Serializable
data class PollViewer(
    @SerialName("has_voted") val hasVoted: Boolean = false,
    @SerialName("chosen_option_ids") val chosenOptionIds: List<String> = emptyList(),
    @SerialName("can_vote") val canVote: Boolean = false,
    @SerialName("can_change_vote") val canChangeVote: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_view_results") val canViewResults: Boolean = false,
)

/** 投票记录里的一条（匿名投票永远为空）。 */
@Serializable
data class PollVote(
    @SerialName("object") val obj: String = "poll_vote",
    val id: String = "",
    val voter: UserRef = UserRef(),
    @SerialName("option_ids") val optionIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class PollCreate(
    val title: String,
    val description: String? = null,
    @SerialName("choice_type") val choiceType: String = "single",
    @SerialName("result_visibility") val resultVisibility: String = "always",
    val options: List<PollOptionCreate>,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("min_choice") val minChoice: Int? = null,
    @SerialName("max_choice") val maxChoice: Int? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("can_change_vote") val canChangeVote: Boolean = true,
)

@Serializable
data class PollOptionCreate(val text: String)

@Serializable
data class PollVoteBody(
    @SerialName("option_ids") val optionIds: List<String>,
)
