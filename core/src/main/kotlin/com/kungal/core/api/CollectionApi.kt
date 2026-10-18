package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.CollectionAlias
import com.kungal.core.model.CollectionChoice
import com.kungal.core.model.CollectionPatchBody
import com.kungal.core.model.CollectionSummary
import com.kungal.core.model.CollectionWorkEngagement
import com.kungal.core.model.WorkSummary
import com.kungal.core.net.PagedList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收藏夹（BP-02）：作品侧的加入/移出与新建（U2），
 * 以及收藏夹详情、作品列表、改名/可见性/删除与我的收藏夹（U3）。
 */
class CollectionApi(private val client: KungalClient) {

    @Serializable
    private data class CollectionCreateBody(
        val title: String,
        val visibility: String,
        val description: String? = null,
        @SerialName("is_default") val isDefault: Boolean = false,
    )

    suspend fun choicesForWork(
        workId: String,
        page: Int = 1,
        limit: Int = 100,
    ): PagedList<CollectionChoice> = client.get(
        path = "/me/works/$workId/collections",
        query = mapOf("page" to page, "limit" to limit),
        deserializer = PagedList.serializer(CollectionChoice.serializer()),
    )

    suspend fun setWork(
        collectionId: String,
        workId: String,
        member: Boolean,
    ): CollectionWorkEngagement {
        val path = "/collections/$collectionId/works/$workId"
        return if (member) {
            client.put(path, CollectionWorkEngagement.serializer())
        } else {
            client.delete(path, CollectionWorkEngagement.serializer())
        }
    }

    /** 新建收藏夹；visibility 必填（private/public），返回新建的收藏夹。 */
    suspend fun create(
        title: String,
        visibility: String,
        description: String? = null,
        isDefault: Boolean = false,
        idempotencyKey: String,
    ): CollectionSummary = client.postJson(
        path = "/collections",
        body = KungalJson.encodeToString(
            CollectionCreateBody.serializer(),
            CollectionCreateBody(
                title = title,
                visibility = visibility,
                description = description,
                isDefault = isDefault,
            ),
        ),
        idempotencyKey = idempotencyKey,
        deserializer = CollectionSummary.serializer(),
    )

    suspend fun collection(collectionId: String, includeNsfw: Boolean = false): CollectionSummary =
        client.get(
            path = "/collections/$collectionId",
            query = mapOf("include_nsfw" to includeNsfw),
            deserializer = CollectionSummary.serializer(),
        )

    /** 旧论坛收藏夹 id 解析（老链接兼容）；不存在时抛 404。 */
    suspend fun alias(aliasId: String): CollectionAlias = client.get(
        path = "/collection-aliases/$aliasId",
        deserializer = CollectionAlias.serializer(),
    )

    /** sort: favorited_desc / favorited_asc / released_desc / released_asc。 */
    suspend fun works(
        collectionId: String,
        page: Int = 1,
        limit: Int = 24,
        sort: String = "favorited_desc",
        includeNsfw: Boolean = false,
    ): PagedList<WorkSummary> = client.get(
        path = "/collections/$collectionId/works",
        query = mapOf(
            "page" to page,
            "limit" to limit,
            "sort" to sort,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    suspend fun update(collectionId: String, patch: CollectionPatchBody): CollectionSummary =
        client.patchJson(
            path = "/collections/$collectionId",
            body = KungalJson.encodeToString(CollectionPatchBody.serializer(), patch),
            deserializer = CollectionSummary.serializer(),
        )

    suspend fun delete(collectionId: String) {
        client.deleteNoBody("/collections/$collectionId")
    }

    suspend fun myCollections(
        page: Int = 1,
        limit: Int = 24,
        workId: String? = null,
        includeNsfw: Boolean = false,
    ): PagedList<CollectionSummary> = client.get(
        path = "/me/collections",
        query = buildMap {
            put("page", page)
            put("limit", limit)
            put("work_id", workId)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(CollectionSummary.serializer()),
    )
}
