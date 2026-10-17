package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.ResourceRelocation
import com.kungal.core.net.PagedList

/**
 * 资源列表筛选（GET /galgame-resources）。网页只暴露 q 与 include_nsfw；
 * state/sort 是官方接口自带能力，供后续页面使用。
 */
data class ResourceListQuery(
    val page: Int = 1,
    val limit: Int = 50,
    val q: String? = null,
    val includeNsfw: Boolean = false,
    val state: String? = null,
    val sort: String? = null,
) {
    fun toQuery(): Map<String, Any?> = mapOf(
        "page" to page,
        "limit" to limit,
        "q" to q,
        "include_nsfw" to includeNsfw,
        "state" to state,
        "sort" to sort,
    )
}

/** Galgame 下载资源（BP-03）。 */
class ResourceApi(private val client: KungalClient) {

    /** 全站资源列表（匿名可读）。 */
    suspend fun list(query: ResourceListQuery = ResourceListQuery()): PagedList<GalgameResource> =
        client.get(
            path = "/galgame-resources",
            query = query.toQuery(),
            deserializer = PagedList.serializer(GalgameResource.serializer()),
        )

    /** 某个词条下的资源列表（匿名可读）。网页按 100/页拉全。 */
    suspend fun workResources(
        workId: String,
        page: Int = 1,
        limit: Int = 100,
        state: String? = null,
    ): PagedList<GalgameResource> = client.get(
        path = "/works/$workId/resources",
        query = mapOf("page" to page, "limit" to limit, "state" to state),
        deserializer = PagedList.serializer(GalgameResource.serializer()),
    )

    /** 资源详情（匿名可读）。 */
    suspend fun resource(resourceId: String): GalgameResource = client.get(
        path = "/galgame-resources/$resourceId",
        deserializer = GalgameResource.serializer(),
    )

    /** 资源迁移查询：详情 404 时使用。 */
    suspend fun relocation(resourceId: String): ResourceRelocation = client.get(
        path = "/galgame-resource-relocations/$resourceId",
        deserializer = ResourceRelocation.serializer(),
    )
}
