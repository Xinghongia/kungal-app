package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 作品详情（GET /works/{id}）：资料库词条的全部读面字段。 */
@Serializable
data class Work(
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
    val aliases: List<String> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("content_rating") val contentRating: String = "all_ages",
    val intros: List<CatalogIntro> = emptyList(),
    val links: List<CatalogLink> = emptyList(),
    @SerialName("external_refs") val externalRefs: List<WorkExternalRef> = emptyList(),
    val covers: List<WorkCover> = emptyList(),
    val screenshots: List<WorkScreenshot> = emptyList(),
    val companies: List<WorkCompany> = emptyList(),
    val engines: List<Engine> = emptyList(),
    val series: List<SeriesSummary> = emptyList(),
    @SerialName("related_works") val relatedWorks: List<RelatedWork> = emptyList(),
    val tags: List<WorkTag> = emptyList(),
    val credits: List<WorkCreditGroup> = emptyList(),
    val roster: List<WorkCharacter> = emptyList(),
    @SerialName("resource_types") val resourceTypes: List<String> = emptyList(),
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("is_resource_publish_banned") val isResourcePublishBanned: Boolean = false,
    @SerialName("is_resource_hosted_elsewhere") val isResourceHostedElsewhere: Boolean = false,
    val creator: UserRef? = null,
    val contributors: List<UserRef> = emptyList(),
    @SerialName("external_ratings") val externalRatings: List<WorkExternalRating> = emptyList(),
    val playtimes: List<WorkPlaytimeAggregate> = emptyList(),
    val dlsite: DlsiteOffer? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: WorkViewer? = null,
)

@Serializable
data class CatalogIntro(
    val locale: String = "",
    val value: String = "",
    @SerialName("is_machine") val isMachine: Boolean = false,
    @SerialName("data_source") val dataSource: String? = null,
)

@Serializable
data class CatalogLink(
    val site: String = "",
    val url: String = "",
)

@Serializable
data class WorkExternalRef(
    val site: String = "",
    @SerialName("external_id") val externalId: String = "",
)

@Serializable
data class WorkCover(
    @SerialName("object") val obj: String = "work_cover",
    val id: String = "",
    val image: ImageInfo? = null,
    @SerialName("cover_slot") val coverSlot: String = "other",
    val site: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("vote_count") val voteCount: Int = 0,
)

@Serializable
data class WorkScreenshot(
    @SerialName("object") val obj: String = "work_screenshot",
    val image: ImageInfo? = null,
    val caption: String = "",
    val site: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class WorkCompany(
    @SerialName("object") val obj: String = "company",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("company_kind") val companyKind: String = "",
    val logo: ImageInfo? = null,
    val aliases: List<String> = emptyList(),
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    val lang: String? = null,
    val links: List<CatalogLink> = emptyList(),
    @SerialName("attribution_roles") val attributionRoles: List<String> = emptyList(),
)

@Serializable
data class WorkTag(
    @SerialName("object") val obj: String = "tag",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("tag_kind") val tagKind: String = "content",
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    val spoiler: String = "none",
)

@Serializable
data class WorkCreditGroup(
    @SerialName("role_key") val roleKey: String = "",
    @SerialName("display_name") val displayName: String = "",
    val people: List<WorkCreditPerson> = emptyList(),
)

@Serializable
data class WorkCreditPerson(
    @SerialName("object") val obj: String = "credit_name",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val lang: String? = null,
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("voiced_characters") val voicedCharacters: List<String> = emptyList(),
)

@Serializable
data class CreditNameRef(
    @SerialName("object") val obj: String = "credit_name",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val lang: String? = null,
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
)

@Serializable
data class WorkCharacter(
    @SerialName("object") val obj: String = "character",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val image: ImageInfo? = null,
    val figure: ImageInfo? = null,
    @SerialName("character_kind") val characterKind: String = "unknown",
    val spoiler: String = "none",
    val voices: List<CreditNameRef> = emptyList(),
)

@Serializable
data class RelatedWork(
    @SerialName("object") val obj: String = "work",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val cover: ImageInfo? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_date_precision") val releaseDatePrecision: String? = null,
    val medium: String = "galgame",
    val relations: List<WorkRelation> = emptyList(),
)

@Serializable
data class WorkRelation(
    @SerialName("relation_type") val relationType: String = "",
    val phrase: String = "",
)

@Serializable
data class SeriesSummary(
    @SerialName("object") val obj: String = "series",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("has_nsfw_works") val hasNsfwWorks: Boolean? = null,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    @SerialName("listed_work_count") val listedWorkCount: Int = 0,
    @SerialName("sample_works") val sampleWorks: List<SeriesSampleWork> = emptyList(),
)

@Serializable
data class SeriesSampleWork(
    @SerialName("object") val obj: String = "work",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val banner: ImageInfo? = null,
    val cover: ImageInfo? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
)

@Serializable
data class Engine(
    @SerialName("object") val obj: String = "engine",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val aliases: List<String> = emptyList(),
    val description: String = "",
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
)

@Serializable
data class WorkExternalRating(
    @SerialName("object") val obj: String = "work_external_rating",
    val site: String = "",
    @SerialName("rating_value") val ratingValue: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("source_rank") val sourceRank: Int? = null,
    val buckets: List<WorkExternalRatingBucket> = emptyList(),
    val stats: WorkExternalRatingStats? = null,
)

@Serializable
data class WorkExternalRatingBucket(
    val bucket: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
)

@Serializable
data class WorkExternalRatingStats(
    val mean: Double? = null,
    val stdev: Double? = null,
    val lowest: Double? = null,
    val highest: Double? = null,
)

@Serializable
data class WorkPlaytimeAggregate(
    val site: String = "",
    val minutes: Int = 0,
    @SerialName("vote_count") val voteCount: Int = 0,
)

@Serializable
data class DlsiteOffer(
    @SerialName("purchase_url") val purchaseUrl: String = "",
    @SerialName("coupon_url") val couponUrl: String? = null,
    @SerialName("campaign_name") val campaignName: String? = null,
)

@Serializable
data class WorkViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("can_ban_resource_publish") val canBanResourcePublish: Boolean = false,
)

@Serializable
data class WorkEngagement(
    @SerialName("object") val obj: String = "work_engagement",
    @SerialName("work_id") val workId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    val viewer: WorkEngagementViewer? = null,
)

@Serializable
data class WorkEngagementViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
)

@Serializable
data class WorkCoverEngagement(
    @SerialName("object") val obj: String = "work_cover_engagement",
    @SerialName("work_id") val workId: String = "",
    @SerialName("cover_id") val coverId: String = "",
    @SerialName("vote_count") val voteCount: Int = 0,
    val viewer: WorkCoverEngagementViewer? = null,
)

@Serializable
data class WorkCoverEngagementViewer(
    @SerialName("has_voted") val hasVoted: Boolean = false,
)

@Serializable
data class MyCoverVote(
    @SerialName("object") val obj: String = "my_cover_vote",
    @SerialName("work_id") val workId: String = "",
    @SerialName("voted_cover_id") val votedCoverId: String? = null,
)

@Serializable
data class MyCoverVoteList(
    @SerialName("object") val obj: String = "list",
    val items: List<MyCoverVote> = emptyList(),
    val missing: List<String> = emptyList(),
)

/** 本人对作品的状态（GET /me/works 批量读）：点赞、收藏夹归属与游玩记录。 */
@Serializable
data class MyWork(
    @SerialName("object") val obj: String = "my_work",
    @SerialName("work_id") val workId: String = "",
    @SerialName("has_liked") val hasLiked: Boolean = false,
    val library: MyWorkLibrary? = null,
)

/** 本人在一部作品上的游玩时长与状态（catalog 记录，跨 NextMoe 站点共享）。 */
@Serializable
data class WorkViewerPlaytime(
    val minutes: Int = 0,
    @SerialName("play_state") val playState: String? = null,
)

@Serializable
data class MyWorkLibrary(
    @SerialName("collection_ids") val collectionIds: List<String> = emptyList(),
    val playtime: WorkViewerPlaytime? = null,
)

@Serializable
data class MyWorkList(
    @SerialName("object") val obj: String = "list",
    val items: List<MyWork> = emptyList(),
    val missing: List<String> = emptyList(),
)

/** 收藏夹选择项（/me/works/{id}/collections）：比 CollectionSummary 少 owner/封面预览。 */
@Serializable
data class CollectionChoice(
    @SerialName("object") val obj: String = "collection",
    val id: String = "",
    val title: String = "",
    val visibility: String = "private",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: CollectionViewer? = null,
)

@Serializable
data class CollectionWorkEngagement(
    @SerialName("object") val obj: String = "collection_work_engagement",
    @SerialName("collection_id") val collectionId: String = "",
    @SerialName("work_id") val workId: String = "",
    val viewer: CollectionWorkEngagementViewer? = null,
)

@Serializable
data class CollectionWorkEngagementViewer(
    @SerialName("has_work") val hasWork: Boolean = false,
)
