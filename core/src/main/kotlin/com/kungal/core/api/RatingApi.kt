package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.RatingCreateBody
import com.kungal.core.model.RatingDetail
import com.kungal.core.model.RatingEngagement
import com.kungal.core.model.RatingPatchBody
import com.kungal.core.model.RatingSummary
import com.kungal.core.net.PagedList

/** 作品评分（BP-02 U4）：列表筛选、详情、创建/修改/删除与点赞。 */
class RatingApi(private val client: KungalClient) {

    /** sort: created|view|overall × _asc/_desc。 */
    suspend fun list(
        page: Int = 1,
        limit: Int = 24,
        sort: String? = "created_desc",
        workId: String? = null,
        authorId: String? = null,
        spoilerLevel: String? = null,
        playStatus: String? = null,
        gameType: String? = null,
        includeNsfw: Boolean = false,
    ): PagedList<RatingSummary> = client.get(
        path = "/ratings",
        query = buildMap {
            put("page", page)
            put("limit", limit)
            put("sort", sort)
            put("work_id", workId)
            put("author_id", authorId)
            put("spoiler_level", spoilerLevel)
            put("play_status", playStatus)
            put("game_type", gameType)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PagedList.serializer(RatingSummary.serializer()),
    )

    suspend fun get(ratingId: String): RatingDetail = client.get(
        path = "/ratings/$ratingId",
        deserializer = RatingDetail.serializer(),
    )

    suspend fun create(body: RatingCreateBody, idempotencyKey: String): RatingDetail =
        client.postJson(
            path = "/ratings",
            body = KungalJson.encodeToString(RatingCreateBody.serializer(), body),
            idempotencyKey = idempotencyKey,
            deserializer = RatingDetail.serializer(),
        )

    suspend fun update(ratingId: String, body: RatingPatchBody): RatingDetail = client.patchJson(
        path = "/ratings/$ratingId",
        body = KungalJson.encodeToString(RatingPatchBody.serializer(), body),
        deserializer = RatingDetail.serializer(),
    )

    suspend fun delete(ratingId: String) {
        client.deleteNoBody("/ratings/$ratingId")
    }

    suspend fun setLike(ratingId: String, liked: Boolean): RatingEngagement {
        val path = "/ratings/$ratingId/like"
        return if (liked) {
            client.put(path, RatingEngagement.serializer())
        } else {
            client.delete(path, RatingEngagement.serializer())
        }
    }
}
