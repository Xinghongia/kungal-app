package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.UploadedImage

/** 图片上传（POST /images，multipart）。服务端限制：单张 ≤10MiB、每人每日 50 张。 */
class ImageApi(private val client: KungalClient) {

    /**
     * 上传一张图片，返回内容寻址记录（hash 用于封面，url 用于正文 Markdown）。
     * idempotencyKey 重试时必须复用同一份字节（multipart boundary 参与指纹）。
     */
    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        purpose: String = "content",
        idempotencyKey: String,
    ): UploadedImage = client.postMultipart(
        path = "/images",
        fileBytes = bytes,
        fileName = fileName,
        contentType = contentType,
        fields = mapOf("purpose" to purpose),
        idempotencyKey = idempotencyKey,
        deserializer = UploadedImage.serializer(),
    )
}
