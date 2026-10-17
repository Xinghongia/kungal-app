package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.MoyuPatchList
import com.kungal.core.model.ResourceDownload
import com.kungal.core.model.ResourceEngagement
import com.kungal.core.model.ResourceExpiryReport
import com.kungal.core.model.ResourceRelocation
import com.kungal.core.model.ResourceSource
import com.kungal.core.net.ApiResponse
import com.kungal.core.net.PagedList
import kotlinx.serialization.json.JsonObject

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

    /** 领取下载链接（下载数 +1）；匿名同样可用。 */
    suspend fun download(resourceId: String): ResourceDownload = client.postNoBodyJson(
        path = "/galgame-resources/$resourceId/downloads",
        deserializer = ResourceDownload.serializer(),
    )

    /** 点赞 / 取消点赞；不能给自己点赞。 */
    suspend fun setLike(resourceId: String, liked: Boolean): ResourceEngagement =
        if (liked) {
            client.put(
                path = "/galgame-resources/$resourceId/like",
                deserializer = ResourceEngagement.serializer(),
            )
        } else {
            client.delete(
                path = "/galgame-resources/$resourceId/like",
                deserializer = ResourceEngagement.serializer(),
            )
        }

    /** 报告失效；服务端会先核验链接。必须带幂等键。 */
    suspend fun reportExpiry(
        resourceId: String,
        idempotencyKey: String,
    ): ResourceExpiryReport = client.postNoBodyJson(
        path = "/galgame-resources/$resourceId/expiry-reports",
        idempotencyKey = idempotencyKey,
        deserializer = ResourceExpiryReport.serializer(),
    )

    /** 编辑源数据（登录且可编辑）。 */
    suspend fun source(resourceId: String): ResourceSource = client.get(
        path = "/galgame-resources/$resourceId/source",
        deserializer = ResourceSource.serializer(),
    )

    /**
     * 发布资源到词条（幂等键必需）。201 响应带 Location/ETag。
     * 请求体按 OpenAPI 字段构造；`resource_runtimes` 仅在有运行环境轴的类型下发送。
     */
    suspend fun create(
        workId: String,
        body: JsonObject,
        idempotencyKey: String,
    ): ApiResponse<GalgameResource> = client.postJsonWithResponse(
        path = "/works/$workId/resources",
        body = KungalJson.encodeToString(JsonObject.serializer(), body),
        idempotencyKey = idempotencyKey,
        deserializer = GalgameResource.serializer(),
    )

    /** 编辑资源；只发送变化字段（`version_label: null` 表示清空）。 */
    suspend fun patch(resourceId: String, body: JsonObject): GalgameResource =
        client.patchJson(
            path = "/galgame-resources/$resourceId",
            body = KungalJson.encodeToString(JsonObject.serializer(), body),
            deserializer = GalgameResource.serializer(),
        )

    /** 删除资源（会扣除发布者 3 萌萌点）。 */
    suspend fun delete(resourceId: String) = client.deleteNoBody("/galgame-resources/$resourceId")

    /** moyu 上的补丁页与补丁资源（只读，游标分页）。 */
    suspend fun moyuPatches(workId: String, cursor: String? = null): MoyuPatchList = client.get(
        path = "/works/$workId/moyu-patches",
        query = mapOf("cursor" to cursor),
        deserializer = MoyuPatchList.serializer(),
    )
}
