package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 头像挂件（商城装扮）；animated_url 为空时用静态图。 */
@Serializable
data class AvatarFrame(
    @SerialName("static_url") val staticUrl: String = "",
    @SerialName("animated_url") val animatedUrl: String? = null,
)

/** 本地化名称：{ "zh-Hans": {"value": "...", "is_machine": false}, ... }。 */
@Serializable
data class LocalizedText(
    val value: String = "",
    @SerialName("is_machine") val isMachine: Boolean = false,
)

/** 用户公开主页（GET /users/{id}）。 */
@Serializable
data class UserProfile(
    @SerialName("object") val obj: String = "user",
    val id: String = "",
    val name: String? = null,
    val avatar: ImageInfo? = null,
    @SerialName("avatar_frame") val avatarFrame: AvatarFrame? = null,
    val bio: String? = null,
    @SerialName("about_html") val aboutHtml: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    val moemoepoint: Int = 0,
    val counts: UserCounts = UserCounts(),
)

/** 用户统计（站点 /user/{id} 头部与「关于」页共用）。 */
@Serializable
data class UserCounts(
    @SerialName("topic_count") val topicCount: Int = 0,
    @SerialName("poll_count") val pollCount: Int = 0,
    @SerialName("lottery_count") val lotteryCount: Int = 0,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("topic_comment_count") val topicCommentCount: Int = 0,
    @SerialName("community_comment_count") val communityCommentCount: Int? = null,
    @SerialName("published_galgame_count") val publishedGalgameCount: Int = 0,
    @SerialName("contributed_galgame_count") val contributedGalgameCount: Int = 0,
    @SerialName("published_galgame_today_count") val publishedGalgameTodayCount: Int = 0,
    @SerialName("galgame_rating_count") val galgameRatingCount: Int = 0,
    @SerialName("galgame_resource_count") val galgameResourceCount: Int = 0,
    @SerialName("toolset_count") val toolsetCount: Int = 0,
    @SerialName("toolset_resource_count") val toolsetResourceCount: Int = 0,
    @SerialName("received_upvote_count") val receivedUpvoteCount: Int = 0,
    @SerialName("received_like_count") val receivedLikeCount: Int = 0,
    @SerialName("received_dislike_count") val receivedDislikeCount: Int = 0,
    @SerialName("topic_today_count") val topicTodayCount: Int = 0,
    @SerialName("follower_count") val followerCount: Int? = null,
    @SerialName("following_count") val followingCount: Int? = null,
)

/** 用户主页「话题」列表项（/users/{id}/topics）。 */
@Serializable
data class UserTopicItem(
    @SerialName("object") val obj: String = "topic",
    val id: String = "",
    val title: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

/** 用户主页「回复」列表项（/users/{id}/replies）。 */
@Serializable
data class UserReplyItem(
    @SerialName("object") val obj: String = "reply",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val floor: Int = 0,
    val excerpt: String = "",
    val author: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

/** 用户主页「评论」列表项（/users/{id}/comments）。 */
@Serializable
data class UserCommentItem(
    @SerialName("object") val obj: String = "comment",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val excerpt: String = "",
    val author: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

/** 少量字段的会社引用（作品卡片用）。 */
@Serializable
data class CompanyRef(
    @SerialName("object") val obj: String = "company",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
)

/** 作品引用（评分/资源列表里的轻量作品）。 */
@Serializable
data class WorkRef(
    @SerialName("object") val obj: String = "work",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val cover: ImageInfo? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
)

/** 作品摘要（用户主页「Galgame」标签）。 */
@Serializable
data class WorkSummary(
    @SerialName("object") val obj: String = "work",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val cover: ImageInfo? = null,
    val banner: ImageInfo? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("is_published") val isPublished: Boolean = true,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_date_precision") val releaseDatePrecision: String? = null,
    val maker: CompanyRef? = null,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("rating_score") val ratingScore: Double? = null,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("resource_platforms") val resourcePlatforms: List<String> = emptyList(),
    @SerialName("resource_languages") val resourceLanguages: List<String> = emptyList(),
    @SerialName("resource_updated_at") val resourceUpdatedAt: String? = null,
)

/** 用户游戏库记录项（/users/{id}/work-records）。 */
@Serializable
data class UserWorkRecord(
    @SerialName("object") val obj: String = "work_record",
    @SerialName("play_state") val playState: String? = null,
    @SerialName("playtime_minutes") val playtimeMinutes: Int? = null,
    @SerialName("state_updated_at") val stateUpdatedAt: String = "",
    @SerialName("user_rating") val userRating: WorkRecordRating? = null,
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
)

@Serializable
data class WorkRecordRating(
    val id: String = "",
    val overall: Int = 0,
)

/** 游戏库各状态计数（服务端只返回五个大类）。 */
@Serializable
data class WorkStateCounts(
    val wish: Int = 0,
    val doing: Int = 0,
    val done: Int = 0,
    @SerialName("on_hold") val onHold: Int = 0,
    val dropped: Int = 0,
)

/** 游戏库响应：游标分页 + 状态计数。 */
@Serializable
data class WorkRecordPage(
    @SerialName("object") val obj: String = "list",
    val items: List<UserWorkRecord> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("state_counts") val stateCounts: WorkStateCounts = WorkStateCounts(),
)

/** 用户收藏夹摘要（/users/{id}/collections）。 */
@Serializable
data class CollectionSummary(
    @SerialName("object") val obj: String = "collection",
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("preview_covers") val previewCovers: List<ImageInfo> = emptyList(),
    val visibility: String = "private",
    val owner: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: CollectionViewer? = null,
)

@Serializable
data class CollectionViewer(
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("has_work") val hasWork: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
)

/** 关注/粉丝条目：followers 用 follower，following 用 followee。 */
@Serializable
data class FollowEdge(
    @SerialName("object") val obj: String = "follow",
    val follower: UserRef? = null,
    val followee: UserRef? = null,
    @SerialName("followed_at") val followedAt: String = "",
) {
    val user: UserRef? get() = follower ?: followee
}

/** 评分摘要（/ratings?author_id=）。 */
@Serializable
data class RatingSummary(
    @SerialName("object") val obj: String = "rating",
    val id: String = "",
    val work: WorkRef? = null,
    val author: UserRef = UserRef(),
    val recommend: String = "neutral",
    val overall: Int = 0,
    @SerialName("game_types") val gameTypes: List<String> = emptyList(),
    @SerialName("play_status") val playStatus: String? = null,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("short_summary") val shortSummary: String = "",
    @SerialName("aspect_scores") val aspectScores: AspectScores = AspectScores(),
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("work_summary") val workSummary: WorkSummary? = null,
)

/** 八维评分；没填的维度为 null。 */
@Serializable
data class AspectScores(
    val art: Int? = null,
    val character: Int? = null,
    val music: Int? = null,
    @SerialName("replay_value") val replayValue: Int? = null,
    val route: Int? = null,
    val story: Int? = null,
    val system: Int? = null,
    val voice: Int? = null,
)

/** 用户发布的资源摘要（/users/{id}/galgame-resources）。 */
@Serializable
data class GalgameResourceSummary(
    @SerialName("object") val obj: String = "galgame_resource",
    val id: String = "",
    val title: String = "",
    val size: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    @SerialName("resource_platforms") val resourcePlatforms: List<String> = emptyList(),
    @SerialName("resource_languages") val resourceLanguages: List<String> = emptyList(),
    val state: String = "valid",
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val work: WorkRef? = null,
)

/** 题库摘要（/quizzes?author_id=）。 */
@Serializable
data class QuizSummary(
    @SerialName("object") val obj: String = "quiz",
    val id: String = "",
    val prompt: ContentDocument? = null,
    val author: UserRef = UserRef(),
    @SerialName("quiz_type") val quizType: String = "single",
    @SerialName("quiz_category") val quizCategory: String = "other",
    val difficulty: Int = 0,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("answer_count") val answerCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("quality_average") val qualityAverage: Double? = null,
    @SerialName("quality_count") val qualityCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** 工具集摘要（/users/{id}/toolsets）。 */
@Serializable
data class ToolsetSummary(
    @SerialName("object") val obj: String = "toolset",
    val id: String = "",
    val title: String = "",
    val aliases: List<String> = emptyList(),
    @SerialName("homepage_urls") val homepageUrls: List<String> = emptyList(),
    @SerialName("toolset_type") val toolsetType: String = "others",
    val platform: String = "windows",
    @SerialName("interface_language") val interfaceLanguage: String = "zh-cn",
    @SerialName("release_channel") val releaseChannel: String = "stable",
    @SerialName("practicality_average") val practicalityAverage: Double? = null,
    @SerialName("practicality_count") val practicalityCount: Int = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** 游玩时长条目（/me/playtimes）。 */
@Serializable
data class WorkPlaytime(
    @SerialName("object") val obj: String = "work_playtime",
    @SerialName("play_state") val playState: String? = null,
    val minutes: Int = 0,
    @SerialName("client_count") val clientCount: Int = 0,
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
)

/** 本人游玩时长汇总。 */
@Serializable
data class PlaytimePage(
    @SerialName("object") val obj: String = "list",
    val items: List<WorkPlaytime> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Int = 0,
    @SerialName("total_relation") val totalRelation: String? = null,
    @SerialName("total_minutes") val totalMinutes: Int = 0,
    @SerialName("finished_work_count") val finishedWorkCount: Int = 0,
    @SerialName("is_truncated") val isTruncated: Boolean = false,
)

/** 改名价格（GET /account-prices，公开）。 */
@Serializable
data class AccountPrices(
    @SerialName("object") val obj: String = "account_prices",
    @SerialName("rename_cost") val renameCost: Int? = null,
)

/** 资料修改响应（PATCH /me/profile）。 */
@Serializable
data class ProfileUpdate(
    @SerialName("object") val obj: String = "user",
    val id: String = "",
    val name: String? = null,
    val bio: String? = null,
    val avatar: ImageInfo? = null,
)

/** 主页可见性（GET/PUT /me/profile-settings）。 */
@Serializable
data class ProfileSettings(
    @SerialName("object") val obj: String = "profile_settings",
    @SerialName("is_sign_in_required") val isSignInRequired: Boolean = false,
)

/** 游玩记录可见性（GET/PATCH /me/privacy-settings）。 */
@Serializable
data class PrivacySettings(
    @SerialName("object") val obj: String = "privacy_settings",
    @SerialName("is_play_records_public") val isPlayRecordsPublic: Boolean = true,
    @SerialName("changed_at") val changedAt: String? = null,
)

/** 动态隐藏（GET/PUT /me/activity-settings）。 */
@Serializable
data class ActivitySettings(
    @SerialName("object") val obj: String = "activity_settings",
    @SerialName("is_hidden") val isHidden: Boolean = false,
)

/** 签到结果（POST /me/check-ins）。 */
@Serializable
data class CheckInResult(
    @SerialName("object") val obj: String = "check_in",
    @SerialName("check_in_date") val checkInDate: String = "",
    val moemoepoint: Int = 0,
    @SerialName("moemoepoint_awarded") val moemoepointAwarded: Int = 0,
)
