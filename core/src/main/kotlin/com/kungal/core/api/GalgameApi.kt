package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.MyCoverVoteList
import com.kungal.core.model.MyWorkList
import com.kungal.core.model.PlaytimePutBody
import com.kungal.core.model.TagBlock
import com.kungal.core.model.TagBlockList
import com.kungal.core.model.Work
import com.kungal.core.model.WorkCollectedMonths
import com.kungal.core.model.WorkCoverEngagement
import com.kungal.core.model.WorkEngagement
import com.kungal.core.model.WorkRecord
import com.kungal.core.model.WorkRecordPutBody
import com.kungal.core.model.WorkSummary
import com.kungal.core.model.WorkViewerPlaytime
import com.kungal.core.net.PagedList

/**
 * 资源资料库的筛选条件（GET /works）。资源轴是复数值（平台/语言/模拟器）；
 * 其余为单值。字段含义与 OpenAPI 参数一一对应。
 */
data class WorkListQuery(
    val page: Int = 1,
    val limit: Int = 24,
    val sort: String = "resource_updated_desc",
    val resourceType: String? = null,
    val resourcePlatforms: List<String> = emptyList(),
    val resourceLanguages: List<String> = emptyList(),
    val resourceRuntimes: List<String> = emptyList(),
    val gameType: String? = null,
    val resourceProviders: List<String> = emptyList(),
    val excludedSoleProviders: List<String> = emptyList(),
    val releasedFrom: String? = null,
    val releasedTo: String? = null,
    val releasedMonths: List<Int> = emptyList(),
    val collectedFrom: String? = null,
    val collectedTo: String? = null,
    val collectedMonths: List<Int> = emptyList(),
    val minRating: Double? = null,
    val minRatingCount: Int? = null,
    val includeNsfw: Boolean = false,
    val includeResourceless: Boolean = false,
    val includeAllOriginalLanguages: Boolean = false,
    val excludeTagIds: List<String> = emptyList(),
    val excludeTagTrees: List<String> = emptyList(),
) {
    fun toQuery(): Map<String, Any?> = buildMap {
        put("page", page)
        put("limit", limit)
        put("sort", sort)
        put("resource_type", resourceType)
        if (resourcePlatforms.isNotEmpty()) put("resource_platforms", resourcePlatforms)
        if (resourceLanguages.isNotEmpty()) put("resource_languages", resourceLanguages)
        if (resourceRuntimes.isNotEmpty()) put("resource_runtimes", resourceRuntimes)
        put("game_type", gameType)
        if (resourceProviders.isNotEmpty()) put("resource_providers", resourceProviders)
        if (excludedSoleProviders.isNotEmpty()) put("excluded_sole_providers", excludedSoleProviders)
        put("released_from", releasedFrom)
        put("released_to", releasedTo)
        if (releasedMonths.isNotEmpty()) put("released_months", releasedMonths)
        put("collected_from", collectedFrom)
        put("collected_to", collectedTo)
        if (collectedMonths.isNotEmpty()) put("collected_months", collectedMonths)
        put("min_rating", minRating)
        put("min_rating_count", minRatingCount)
        put("include_nsfw", includeNsfw)
        if (includeResourceless) put("include_resourceless", true)
        if (includeAllOriginalLanguages) put("include_all_original_languages", true)
        if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
        if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
    }
}

/** 信息资料库的筛选条件（GET /library-works）：全量作品，含无资源条目。 */
data class LibraryWorkQuery(
    val page: Int = 1,
    val limit: Int = 24,
    val q: String? = null,
    val sort: String = "popularity_desc",
    val releasedFrom: String? = null,
    val releasedTo: String? = null,
    val includeNsfw: Boolean = false,
    val includeAllOriginalLanguages: Boolean = false,
    val excludeTagIds: List<String> = emptyList(),
    val excludeTagTrees: List<String> = emptyList(),
) {
    fun toQuery(): Map<String, Any?> = buildMap {
        put("page", page)
        put("limit", limit)
        put("q", q)
        put("sort", sort)
        put("released_from", releasedFrom)
        put("released_to", releasedTo)
        put("include_nsfw", includeNsfw)
        if (includeAllOriginalLanguages) put("include_all_original_languages", true)
        if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
        if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
    }
}

/**
 * Galgame 资料库只读接口（BP-02 U1）：
 * 资源资料库 /works、信息资料库 /library-works、收录月份、本人标签屏蔽。
 */
class GalgameApi(private val client: KungalClient) {

    suspend fun works(query: WorkListQuery): PagedList<WorkSummary> = client.get(
        path = "/works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    suspend fun libraryWorks(query: LibraryWorkQuery): PagedList<WorkSummary> = client.get(
        path = "/library-works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    suspend fun collectedMonths(includeNsfw: Boolean = false): WorkCollectedMonths = client.get(
        path = "/works/collected-months",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = WorkCollectedMonths.serializer(),
    )

    /** 词条详情；合并条目会抛 409 ENTITY_MERGED（problem.current_id 指向新 id）。 */
    suspend fun work(workId: String, includeNsfw: Boolean = false): Work = client.get(
        path = "/works/$workId",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = Work.serializer(),
    )

    /** 点赞/取消点赞作品。 */
    suspend fun setWorkLike(workId: String, liked: Boolean): WorkEngagement {
        val path = "/works/$workId/like"
        return if (liked) {
            client.put(path, WorkEngagement.serializer())
        } else {
            client.delete(path, WorkEngagement.serializer())
        }
    }

    /** 封面投票/撤票：一人一票，投别的封面会移动选票。 */
    suspend fun setCoverVote(
        workId: String,
        coverId: String,
        voted: Boolean,
    ): WorkCoverEngagement {
        val path = "/works/$workId/covers/$coverId/vote"
        return if (voted) {
            client.put(path, WorkCoverEngagement.serializer())
        } else {
            client.delete(path, WorkCoverEngagement.serializer())
        }
    }

    /** 批量读取本人对若干作品的封面投票。 */
    suspend fun myCoverVotes(workIds: List<String>): MyCoverVoteList = client.get(
        path = "/me/cover-votes",
        query = mapOf("work_ids" to workIds),
        deserializer = MyCoverVoteList.serializer(),
    )

    /** 批量读取本人对作品的点赞/收藏夹/游玩记录（1–100 个 id）。 */
    suspend fun myWorkStates(workIds: List<String>): MyWorkList = client.get(
        path = "/me/works",
        query = mapOf("work_ids" to workIds),
        deserializer = MyWorkList.serializer(),
    )

    /** 本人对一部作品的评分、游玩状态与本站游玩时长。 */
    suspend fun workRecord(workId: String): WorkRecord = client.get(
        path = "/works/$workId/record",
        deserializer = WorkRecord.serializer(),
    )

    /** 整体替换记录：漏发的部分会被删除（评分/时长/状态）。 */
    suspend fun putWorkRecord(workId: String, body: WorkRecordPutBody): WorkRecord =
        client.putJson(
            path = "/works/$workId/record",
            body = KungalJson.encodeToString(WorkRecordPutBody.serializer(), body),
            deserializer = WorkRecord.serializer(),
        )

    suspend fun putPlaytime(workId: String, body: PlaytimePutBody): WorkViewerPlaytime =
        client.putJson(
            path = "/works/$workId/playtime",
            body = KungalJson.encodeToString(PlaytimePutBody.serializer(), body),
            deserializer = WorkViewerPlaytime.serializer(),
        )

    suspend fun deletePlaytime(workId: String): WorkViewerPlaytime = client.delete(
        path = "/works/$workId/playtime",
        deserializer = WorkViewerPlaytime.serializer(),
    )

    /** 登录用户的标签屏蔽；列表请求据此追加 exclude_tag_ids / exclude_tag_trees。 */
    suspend fun tagBlocks(): List<TagBlock> {
        val all = mutableListOf<TagBlock>()
        var cursor: String? = null
        do {
            val page: TagBlockList = client.get(
                path = "/me/blocked-tags",
                query = if (cursor == null) emptyMap<String, Any?>() else mapOf("cursor" to cursor),
                deserializer = TagBlockList.serializer(),
            )
            all += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        return all
    }
}
