package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.AppearanceList
import com.kungal.core.model.CharacterDetail
import com.kungal.core.model.CharacterSummary
import com.kungal.core.model.CompanyDetail
import com.kungal.core.model.CompanyGraph
import com.kungal.core.model.CompanySummary
import com.kungal.core.model.CreditList
import com.kungal.core.model.CreditNameDetail
import com.kungal.core.model.CreditNameRef
import com.kungal.core.model.SeriesDetail
import com.kungal.core.model.SeriesSummary
import com.kungal.core.model.TagBlock
import com.kungal.core.model.TagDetail
import com.kungal.core.model.TagNodeList
import com.kungal.core.model.TagSummary
import com.kungal.core.model.TraitDetail
import com.kungal.core.model.TraitSummary
import com.kungal.core.model.WikiCompanyRedirect
import com.kungal.core.model.WorkSummary
import com.kungal.core.net.KungalApiException
import com.kungal.core.net.PagedList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 实体作品列表查询：实体页与 /works 不同，资源轴只接受单值。 */
data class EntityWorkQuery(
    val page: Int = 1,
    val limit: Int = 24,
    val sort: String = "resource_updated_desc",
    val resourceType: String? = null,
    val resourcePlatform: String? = null,
    val resourceLanguage: String? = null,
    val resourceRuntime: String? = null,
    val gameType: String? = null,
    val includeNsfw: Boolean = false,
    val includeAllOriginalLanguages: Boolean = false,
    val excludeTagIds: List<String> = emptyList(),
    val excludeTagTrees: List<String> = emptyList(),
    val via: String? = null,
) {
    fun toQuery(): Map<String, Any?> = buildMap {
        put("page", page)
        put("limit", limit)
        put("sort", sort)
        put("resource_type", resourceType)
        put("resource_platform", resourcePlatform)
        put("resource_language", resourceLanguage)
        put("resource_runtime", resourceRuntime)
        put("game_type", gameType)
        put("include_nsfw", includeNsfw)
        if (includeAllOriginalLanguages) put("include_all_original_languages", true)
        if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
        if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
        put("via", via)
    }
}

/**
 * Galgame 目录实体（BP-02 U3）：标签 / 会社 / 引擎 / 系列 / 角色 / Staff / 特质。
 * 列表均为页码分页；角色出演与 Staff 参与作品为游标分页。
 */
class GalgameEntityApi(private val client: KungalClient) {

    @Serializable
    private data class TagBlockBody(val mode: String)

    // ---------- 标签 ----------

    suspend fun tags(
        q: String? = null,
        ids: List<String> = emptyList(),
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<TagSummary> = client.get(
        path = "/tags",
        query = buildMap {
            put("q", q)
            if (ids.isNotEmpty()) put("ids", ids)
            put("page", page)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(TagSummary.serializer()),
    )

    suspend fun tag(tagId: String, includeNsfw: Boolean = false): TagDetail = client.get(
        path = "/tags/$tagId",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = TagDetail.serializer(),
    )

    suspend fun tagDescendants(tagId: String, includeNsfw: Boolean = false): TagNodeList = client.get(
        path = "/tags/$tagId/descendants",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = TagNodeList.serializer(),
    )

    suspend fun tagWorks(tagId: String, query: EntityWorkQuery): PagedList<WorkSummary> = client.get(
        path = "/tags/$tagId/works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    /** 多标签联合筛选：只看同时含有 [tagIds]（1–10 个）全部标签的作品。 */
    suspend fun taggedWorks(
        tagIds: List<String>,
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
        includeAllOriginalLanguages: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): PagedList<WorkSummary> = client.get(
        path = "/tagged-works",
        query = buildMap {
            put("tag_ids", tagIds)
            put("page", page)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
            if (includeAllOriginalLanguages) put("include_all_original_languages", true)
            if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
            if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
        },
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    /** 未屏蔽时服务端返回 404；按「未屏蔽」处理。 */
    suspend fun tagBlockOrNull(tagId: String): TagBlock? = try {
        client.get("/me/blocked-tags/$tagId", deserializer = TagBlock.serializer())
    } catch (e: KungalApiException) {
        if (e.httpStatus == 404) null else throw e
    }

    suspend fun setTagBlock(tagId: String, mode: String = "tree"): TagBlock = client.putJson(
        path = "/me/blocked-tags/$tagId",
        body = KungalJson.encodeToString(TagBlockBody.serializer(), TagBlockBody(mode)),
        deserializer = TagBlock.serializer(),
    )

    suspend fun removeTagBlock(tagId: String) {
        client.deleteNoBody("/me/blocked-tags/$tagId")
    }

    // ---------- 会社 ----------

    suspend fun companies(
        q: String? = null,
        ids: List<String> = emptyList(),
        companyKind: String? = null,
        page: Int = 1,
        limit: Int = 24,
    ): PagedList<CompanySummary> = client.get(
        path = "/companies",
        query = buildMap {
            put("q", q)
            if (ids.isNotEmpty()) put("ids", ids)
            put("company_kind", companyKind)
            put("page", page)
            put("limit", limit)
        },
        deserializer = PagedList.serializer(CompanySummary.serializer()),
    )

    suspend fun company(companyId: String): CompanyDetail = client.get(
        path = "/companies/$companyId",
        deserializer = CompanyDetail.serializer(),
    )

    /** 旧 wiki 会社 id 解析（老链接兼容）；不存在时抛 404。 */
    suspend fun companyRedirect(wikiCompanyId: String): WikiCompanyRedirect = client.get(
        path = "/wiki-company-redirects/$wikiCompanyId",
        deserializer = WikiCompanyRedirect.serializer(),
    )

    suspend fun companyWorks(companyId: String, query: EntityWorkQuery): PagedList<WorkSummary> = client.get(
        path = "/companies/$companyId/works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    suspend fun companyGraph(companyId: String): CompanyGraph = client.get(
        path = "/companies/$companyId/graph",
        deserializer = CompanyGraph.serializer(),
    )

    // ---------- 引擎 ----------

    suspend fun engines(q: String? = null, page: Int = 1, limit: Int = 24): PagedList<com.kungal.core.model.Engine> =
        client.get(
            path = "/engines",
            query = buildMap {
                put("q", q)
                put("page", page)
                put("limit", limit)
            },
            deserializer = PagedList.serializer(com.kungal.core.model.Engine.serializer()),
        )

    suspend fun engine(engineId: String): com.kungal.core.model.Engine = client.get(
        path = "/engines/$engineId",
        deserializer = com.kungal.core.model.Engine.serializer(),
    )

    suspend fun engineWorks(engineId: String, query: EntityWorkQuery): PagedList<WorkSummary> = client.get(
        path = "/engines/$engineId/works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    // ---------- 系列 ----------

    suspend fun seriesList(
        q: String? = null,
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<SeriesSummary> = client.get(
        path = "/series",
        query = buildMap {
            put("q", q)
            put("page", page)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(SeriesSummary.serializer()),
    )

    suspend fun series(seriesId: String): SeriesDetail = client.get(
        path = "/series/$seriesId",
        deserializer = SeriesDetail.serializer(),
    )

    suspend fun seriesWorks(seriesId: String, query: EntityWorkQuery): PagedList<WorkSummary> = client.get(
        path = "/series/$seriesId/works",
        query = query.toQuery(),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    // ---------- 角色与特质 ----------

    suspend fun characters(
        q: String? = null,
        traitIds: List<String> = emptyList(),
        traitMatch: String? = null,
        genders: List<String> = emptyList(),
        sort: String? = null,
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<CharacterSummary> = client.get(
        path = "/characters",
        query = buildMap {
            put("q", q)
            if (traitIds.isNotEmpty()) put("trait_ids", traitIds)
            put("trait_match", traitMatch)
            if (genders.isNotEmpty()) put("genders", genders)
            put("sort", sort)
            put("page", page)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(CharacterSummary.serializer()),
    )

    suspend fun character(characterId: String, includeNsfw: Boolean = false): CharacterDetail = client.get(
        path = "/characters/$characterId",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = CharacterDetail.serializer(),
    )

    suspend fun characterAppearances(
        characterId: String,
        cursor: String? = null,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): AppearanceList = client.get(
        path = "/characters/$characterId/appearances",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = AppearanceList.serializer(),
    )

    suspend fun traits(
        q: String? = null,
        ids: List<String> = emptyList(),
        parentId: String? = null,
        page: Int = 1,
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PagedList<TraitSummary> = client.get(
        path = "/traits",
        query = buildMap {
            put("q", q)
            if (ids.isNotEmpty()) put("ids", ids)
            put("parent_id", parentId)
            put("page", page)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(TraitSummary.serializer()),
    )

    suspend fun trait(traitId: String, includeNsfw: Boolean = false): TraitDetail = client.get(
        path = "/traits/$traitId",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = TraitDetail.serializer(),
    )

    // ---------- Staff ----------

    suspend fun creditNames(q: String, page: Int = 1, limit: Int = 24): PagedList<CreditNameRef> =
        client.get(
            path = "/credit-names",
            query = mapOf("q" to q, "page" to page, "limit" to limit),
            deserializer = PagedList.serializer(CreditNameRef.serializer()),
        )

    suspend fun creditName(creditNameId: String): CreditNameDetail = client.get(
        path = "/credit-names/$creditNameId",
        deserializer = CreditNameDetail.serializer(),
    )

    suspend fun creditNameCredits(
        creditNameId: String,
        cursor: String? = null,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): CreditList = client.get(
        path = "/credit-names/$creditNameId/credits",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("include_nsfw", includeNsfw)
        },
        deserializer = CreditList.serializer(),
    )
}
