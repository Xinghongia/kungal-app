package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.WallComment
import com.kungal.core.model.WallCommentCreateBody
import com.kungal.core.model.WallCommentFlagBody
import com.kungal.core.model.WallCommentList
import com.kungal.core.model.WallCommentPatchBody
import com.kungal.core.model.WallCommentSource

/**
 * 通用评论墙（works / galgame_rating / galgame_resource / galgame_quiz / toolset / website）。
 * 外层按游标分页；客户端按 root_comment_id 分两层展示。
 */
class WallCommentApi(private val client: KungalClient) {

    suspend fun list(
        subjectType: String,
        subjectId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): WallCommentList = client.get(
        path = "/wall-comments",
        query = buildMap {
            put("subject_type", subjectType)
            put("subject_id", subjectId)
            put("cursor", cursor)
            put("limit", limit)
        },
        deserializer = WallCommentList.serializer(),
    )

    suspend fun create(
        subjectType: String,
        subjectId: String,
        contentMarkdown: String,
        parentCommentId: String? = null,
        idempotencyKey: String,
    ): WallComment = client.postJson(
        path = "/wall-comments",
        body = KungalJson.encodeToString(
            WallCommentCreateBody.serializer(),
            WallCommentCreateBody(
                subjectType = subjectType,
                subjectId = subjectId,
                parentCommentId = parentCommentId,
                contentMarkdown = contentMarkdown,
            ),
        ),
        idempotencyKey = idempotencyKey,
        deserializer = WallComment.serializer(),
    )

    suspend fun update(wallCommentId: String, contentMarkdown: String): WallComment =
        client.patchJson(
            path = "/wall-comments/$wallCommentId",
            body = KungalJson.encodeToString(
                WallCommentPatchBody.serializer(),
                WallCommentPatchBody(contentMarkdown),
            ),
            deserializer = WallComment.serializer(),
        )

    suspend fun delete(wallCommentId: String) {
        client.deleteNoBody("/wall-comments/$wallCommentId")
    }

    suspend fun setLike(wallCommentId: String, liked: Boolean): WallComment {
        val path = "/wall-comments/$wallCommentId/like"
        return if (liked) {
            client.put(path, WallComment.serializer())
        } else {
            client.delete(path, WallComment.serializer())
        }
    }

    suspend fun flag(
        wallCommentId: String,
        reason: String,
        note: String? = null,
        idempotencyKey: String? = null,
    ) {
        client.postJsonNoContent(
            path = "/wall-comments/$wallCommentId/flags",
            body = KungalJson.encodeToString(
                WallCommentFlagBody.serializer(),
                WallCommentFlagBody(flagReason = reason, note = note),
            ),
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun source(wallCommentId: String): WallCommentSource = client.get(
        path = "/wall-comments/$wallCommentId/source",
        deserializer = WallCommentSource.serializer(),
    )
}
