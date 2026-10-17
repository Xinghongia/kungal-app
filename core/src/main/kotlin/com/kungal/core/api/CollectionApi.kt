package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.CollectionChoice
import com.kungal.core.model.CollectionSummary
import com.kungal.core.model.CollectionWorkEngagement
import com.kungal.core.net.PagedList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收藏夹（BP-02）：作品侧只做选择/加入/移出与新建；
 * 完整的收藏夹管理（改名、可见性、删除、作品列表）在 U3 扩充。
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
}
