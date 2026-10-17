package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.Toolset
import com.kungal.core.model.ToolsetDownload
import com.kungal.core.model.ToolsetEngagement
import com.kungal.core.model.ToolsetPracticality
import com.kungal.core.model.ToolsetResource
import com.kungal.core.model.ToolsetResourceSource
import com.kungal.core.model.ToolsetSource
import com.kungal.core.model.ToolsetSummary
import com.kungal.core.model.ToolsetUpload
import com.kungal.core.model.ToolsetUploadCompletePart
import com.kungal.core.model.ToolsetUploadCreate
import com.kungal.core.net.PagedList
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** 工具集列表筛选（GET /toolsets）。字段与 OpenAPI 参数一一对应。 */
data class ToolsetListQuery(
    val page: Int = 1,
    val limit: Int = 24,
    /** resource_updated_desc / created_asc / view_desc / title_asc … */
    val sort: String = "resource_updated_desc",
    val toolsetType: String? = null,
    val interfaceLanguage: String? = null,
    val platform: String? = null,
    val releaseChannel: String? = null,
    val q: String? = null,
) {
    fun toQuery(): Map<String, Any?> = mapOf(
        "page" to page,
        "limit" to limit,
        "sort" to sort,
        "toolset_type" to toolsetType,
        "interface_language" to interfaceLanguage,
        "platform" to platform,
        "release_channel" to releaseChannel,
        "q" to q,
    )
}

/**
 * 工具集（BP-03 U4/U5/U6）：列表、详情、收藏、实用性评分、创建/编辑/删除、
 * 工具资源管理与下载、S3 上传会话。
 * 写操作按官方契约带 Idempotency-Key；详情每次读取会让浏览数 +1。
 */
class ToolsetApi(private val client: KungalClient) {

    suspend fun list(query: ToolsetListQuery = ToolsetListQuery()): PagedList<ToolsetSummary> =
        client.get(
            path = "/toolsets",
            query = query.toQuery(),
            deserializer = PagedList.serializer(ToolsetSummary.serializer()),
        )

    suspend fun toolset(toolsetId: String): Toolset = client.get(
        path = "/toolsets/$toolsetId",
        deserializer = Toolset.serializer(),
    )

    suspend fun source(toolsetId: String): ToolsetSource = client.get(
        path = "/toolsets/$toolsetId/source",
        deserializer = ToolsetSource.serializer(),
    )

    /** 创建工具集（幂等键必需）。 */
    suspend fun create(body: JsonObject, idempotencyKey: String): Toolset =
        client.postJson(
            path = "/toolsets",
            body = KungalJson.encodeToString(JsonObject.serializer(), body),
            idempotencyKey = idempotencyKey,
            deserializer = Toolset.serializer(),
        )

    /** 编辑工具集；只发送变化字段。 */
    suspend fun patch(toolsetId: String, body: JsonObject): Toolset =
        client.patchJson(
            path = "/toolsets/$toolsetId",
            body = KungalJson.encodeToString(JsonObject.serializer(), body),
            deserializer = Toolset.serializer(),
        )

    /** 删除工具集（扣除 3 萌萌点）。 */
    suspend fun delete(toolsetId: String) = client.deleteNoBody("/toolsets/$toolsetId")

    suspend fun setFavorite(toolsetId: String, favorited: Boolean): ToolsetEngagement =
        if (favorited) {
            client.put(
                path = "/toolsets/$toolsetId/favorite",
                deserializer = ToolsetEngagement.serializer(),
            )
        } else {
            client.delete(
                path = "/toolsets/$toolsetId/favorite",
                deserializer = ToolsetEngagement.serializer(),
            )
        }

    /** 实用性评分 1–5；重复提交会覆盖。 */
    suspend fun ratePracticality(toolsetId: String, rating: Int): ToolsetPracticality =
        client.putJson(
            path = "/toolsets/$toolsetId/practicality",
            body = KungalJson.encodeToString(
                JsonObject.serializer(),
                buildJsonObject { put("rating", JsonPrimitive(rating)) },
            ),
            deserializer = ToolsetPracticality.serializer(),
        )

    // ---------- 工具资源 ----------

    suspend fun resource(toolsetId: String, resourceId: String): ToolsetResource = client.get(
        path = "/toolsets/$toolsetId/resources/$resourceId",
        deserializer = ToolsetResource.serializer(),
    )

    suspend fun resourceSource(toolsetId: String, resourceId: String): ToolsetResourceSource =
        client.get(
            path = "/toolsets/$toolsetId/resources/$resourceId/source",
            deserializer = ToolsetResourceSource.serializer(),
        )

    /** 新增工具资源（file 需 artifact_id，link 需 link_url + size_label）。 */
    suspend fun addResource(
        toolsetId: String,
        body: JsonObject,
        idempotencyKey: String,
    ): ToolsetResource = client.postJson(
        path = "/toolsets/$toolsetId/resources",
        body = KungalJson.encodeToString(JsonObject.serializer(), body),
        idempotencyKey = idempotencyKey,
        deserializer = ToolsetResource.serializer(),
    )

    suspend fun patchResource(
        toolsetId: String,
        resourceId: String,
        body: JsonObject,
    ): ToolsetResource = client.patchJson(
        path = "/toolsets/$toolsetId/resources/$resourceId",
        body = KungalJson.encodeToString(JsonObject.serializer(), body),
        deserializer = ToolsetResource.serializer(),
    )

    suspend fun deleteResource(toolsetId: String, resourceId: String) =
        client.deleteNoBody("/toolsets/$toolsetId/resources/$resourceId")

    /** 领取工具资源下载：file 返回预签名 URL（有有效期），link 返回原链接。 */
    suspend fun downloadResource(toolsetId: String, resourceId: String): ToolsetDownload =
        client.postNoBodyJson(
            path = "/toolsets/$toolsetId/resources/$resourceId/downloads",
            deserializer = ToolsetDownload.serializer(),
        )

    // ---------- S3 上传 ----------

    /** 初始化上传会话（幂等键可选）。 */
    suspend fun createUpload(
        toolsetId: String,
        request: ToolsetUploadCreate,
        idempotencyKey: String? = null,
    ): ToolsetUpload = client.postJson(
        path = "/toolsets/$toolsetId/uploads",
        body = KungalJson.encodeToString(ToolsetUploadCreate.serializer(), request),
        idempotencyKey = idempotencyKey,
        deserializer = ToolsetUpload.serializer(),
    )

    /** 续传查询（返回已传分片与缺失分片的预签名地址）。 */
    suspend fun upload(toolsetId: String, uploadId: String): ToolsetUpload = client.get(
        path = "/toolsets/$toolsetId/uploads/$uploadId",
        deserializer = ToolsetUpload.serializer(),
    )

    /** 完成上传：单文件直接 state=completed；分片需带每片 ETag。 */
    suspend fun completeUpload(
        toolsetId: String,
        uploadId: String,
        parts: List<ToolsetUploadCompletePart> = emptyList(),
    ): ToolsetUpload = client.patchJson(
        path = "/toolsets/$toolsetId/uploads/$uploadId",
        body = KungalJson.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("state", JsonPrimitive("completed"))
                if (parts.isNotEmpty()) {
                    put(
                        "parts",
                        buildJsonArray {
                            parts.forEach { part ->
                                add(
                                    buildJsonObject {
                                        put("part_number", JsonPrimitive(part.partNumber))
                                        put("etag", JsonPrimitive(part.etag))
                                    },
                                )
                            }
                        },
                    )
                }
            },
        ),
        deserializer = ToolsetUpload.serializer(),
    )

    /** 中止上传会话。 */
    suspend fun abortUpload(toolsetId: String, uploadId: String) =
        client.deleteNoBody("/toolsets/$toolsetId/uploads/$uploadId")
}
