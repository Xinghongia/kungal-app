package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.UserSearchHit
import com.kungal.core.model.WallCommentSearchHit
import com.kungal.core.model.WorkSummary
import com.kungal.core.net.PageList
import com.kungal.core.net.PagedList

/**
 * 全站搜索（`/search` 接口组）。话题/回复/评论在 [TopicApi]，资料库实体、
 * 资源、工具集复用各自列表接口的 q 参数；这里补用户、作品与留言墙。
 */
class SearchApi(private val client: KungalClient) {

    /** 用户搜索（匿名可读）。 */
    suspend fun users(
        q: String,
        page: Int = 1,
        limit: Int = 24,
    ): PagedList<UserSearchHit> = client.get(
        path = "/search/users",
        query = mapOf("q" to q, "page" to page, "limit" to limit),
        deserializer = PagedList.serializer(UserSearchHit.serializer()),
    )

    /**
     * Galgame 留言墙搜索（匿名可读）。q 至少 2 字，游标分页、无 total；
     * 结果带 subject_path，可直接交给站内路径分发。
     */
    suspend fun wallComments(
        q: String,
        cursor: String? = null,
        limit: Int = 24,
    ): PageList<WallCommentSearchHit> = client.get(
        path = "/search/wall-comments",
        query = mapOf("q" to q, "cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(WallCommentSearchHit.serializer()),
    )

    /** 作品搜索：相关度/热度/发售/资料更新排序 + 会社/标签/发售年份筛选。 */
    suspend fun works(
        q: String,
        page: Int = 1,
        limit: Int = 24,
        sort: String = "relevance_desc",
        companyId: String? = null,
        tagIds: List<String> = emptyList(),
        releasedFrom: String? = null,
        releasedTo: String? = null,
        includeNsfw: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): PagedList<WorkSummary> = client.get(
        path = "/search/works",
        query = buildMap {
            put("q", q)
            put("page", page)
            put("limit", limit)
            put("sort", sort)
            put("company_id", companyId)
            if (tagIds.isNotEmpty()) put("tag_ids", tagIds)
            put("released_from", releasedFrom)
            put("released_to", releasedTo)
            put("include_nsfw", includeNsfw)
            if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
            if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
        },
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )
}
