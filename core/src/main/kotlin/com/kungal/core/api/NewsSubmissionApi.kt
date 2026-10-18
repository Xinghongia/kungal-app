package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.NewsSubmission
import com.kungal.core.model.NewsSubmissionCreate
import com.kungal.core.model.NewsSubmissionPatch
import com.kungal.core.model.UploadedImage
import com.kungal.core.net.PageList

/**
 * 情报投稿（BP-05 U6，网页 /edit/news 与 /news/mine）。全部端点需登录：
 * 创建带 Idempotency-Key；封面为 multipart 上传，返回 image（hash 用于提交）。
 * 撤回 = 把 state 单独 PATCH 成 withdrawn，只有 published 状态可以撤回。
 */
class NewsSubmissionApi(private val client: KungalClient) {

    suspend fun mySubmissions(
        cursor: String? = null,
        limit: Int = 20,
    ): PageList<NewsSubmission> = client.get(
        path = "/me/news-submissions",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
        },
        deserializer = PageList.serializer(NewsSubmission.serializer()),
    )

    suspend fun submission(id: String): NewsSubmission = client.get(
        path = "/me/news-submissions/$id",
        deserializer = NewsSubmission.serializer(),
    )

    suspend fun create(body: NewsSubmissionCreate, idempotencyKey: String): NewsSubmission =
        client.postJson(
            path = "/me/news-submissions",
            body = KungalJson.encodeToString(NewsSubmissionCreate.serializer(), body),
            idempotencyKey = idempotencyKey,
            deserializer = NewsSubmission.serializer(),
        )

    suspend fun update(id: String, body: NewsSubmissionPatch): NewsSubmission = client.patchJson(
        path = "/me/news-submissions/$id",
        body = KungalJson.encodeToString(NewsSubmissionPatch.serializer(), body),
        deserializer = NewsSubmission.serializer(),
    )

    suspend fun withdraw(id: String): NewsSubmission =
        update(id, NewsSubmissionPatch(state = "withdrawn"))

    /** 封面上传（JPEG/PNG/WebP，≤4,000,000 字节）；返回记录里的 hash 用于投稿。 */
    suspend fun uploadBanner(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        idempotencyKey: String,
    ): UploadedImage = client.postMultipart(
        path = "/me/news-submission-images",
        fileBytes = bytes,
        fileName = fileName,
        contentType = contentType,
        idempotencyKey = idempotencyKey,
        deserializer = UploadedImage.serializer(),
    )
}
