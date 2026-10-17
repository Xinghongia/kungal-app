package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.EditAmendment
import com.kungal.core.model.EditForm
import com.kungal.core.model.EditProposal
import com.kungal.core.model.EditProposalSummary
import com.kungal.core.model.EditRevision
import com.kungal.core.model.EditRevisionDiff
import com.kungal.core.model.EditRevert
import com.kungal.core.model.UploadedImage
import com.kungal.core.model.WorkEditProposalCreateResult
import com.kungal.core.model.WorkEditProposalCreateResultSerializer
import com.kungal.core.net.ApiResponse
import com.kungal.core.net.PageList
import com.kungal.core.net.PagedList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Galgame 资料编辑（BP-02 U5）：编辑表单、提案、审阅、修订历史与回滚、编辑图片上传。
 * 所有写操作带 Idempotency-Key；审阅/修正用 ETag 作为 If-Match。
 */
class EditApi(private val client: KungalClient) {

    @Serializable
    private data class ProposalCreateBody(
        val patch: JsonObject,
        val note: String? = null,
    )

    @Serializable
    private data class ProposalUpdateBody(
        val state: String,
        val note: String? = null,
    )

    @Serializable
    private data class AmendmentBody(
        val set: JsonObject = JsonObject(emptyMap()),
        val unset: List<String> = emptyList(),
        val note: String? = null,
    )

    @Serializable
    private data class RevertBody(
        @SerialName("to_seq") val toSeq: Int,
        val note: String? = null,
    )

    /** 当前字段值、可编辑字段模式与词汇表。需要登录；隐藏/未知作品为 404。 */
    suspend fun form(workId: String): EditForm = client.get(
        path = "/works/$workId/edit-form",
        deserializer = EditForm.serializer(),
    )

    /** 一部作品的提案（游标，最新在前）。state 默认 open。 */
    suspend fun proposals(
        workId: String,
        state: String = "open",
        cursor: String? = null,
        limit: Int = 50,
    ): PageList<EditProposalSummary> = client.get(
        path = "/works/$workId/edit-proposals",
        query = buildMap {
            put("state", state)
            put("limit", limit)
            if (cursor != null) put("cursor", cursor)
        },
        deserializer = PageList.serializer(EditProposalSummary.serializer()),
    )

    /** 我的提案（游标）；work_id / state 可选收窄。 */
    suspend fun myProposals(
        workId: String? = null,
        state: String? = null,
        cursor: String? = null,
        limit: Int = 50,
    ): PageList<EditProposalSummary> = client.get(
        path = "/me/edit-proposals",
        query = buildMap {
            put("work_id", workId)
            put("state", state)
            put("limit", limit)
            if (cursor != null) put("cursor", cursor)
        },
        deserializer = PageList.serializer(EditProposalSummary.serializer()),
    )

    /** 全体审阅队列；需要 galgame.edit_proposal.review 权限。 */
    suspend fun reviewQueue(
        state: String = "open",
        cursor: String? = null,
        limit: Int = 50,
    ): PageList<EditProposalSummary> = client.get(
        path = "/edit-proposals",
        query = buildMap {
            put("state", state)
            put("limit", limit)
            if (cursor != null) put("cursor", cursor)
        },
        deserializer = PageList.serializer(EditProposalSummary.serializer()),
    )

    /** 提案详情（含 patch / effective_patch / amendments）；ETag 用于后续 If-Match。 */
    suspend fun proposal(proposalId: String): ApiResponse<EditProposal> = client.getWithResponse(
        path = "/edit-proposals/$proposalId",
        deserializer = EditProposal.serializer(),
    )

    /** 提交编辑提案；可信账号或创建者可能立即 merged（响应带 revision）。 */
    suspend fun createProposal(
        workId: String,
        patch: JsonObject,
        note: String?,
        idempotencyKey: String,
    ): WorkEditProposalCreateResult {
        val body = KungalJson.encodeToString(
            ProposalCreateBody.serializer(),
            ProposalCreateBody(patch = patch, note = note),
        )
        val response = client.postJsonWithResponse(
            path = "/works/$workId/edit-proposals",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = WorkEditProposalCreateResultSerializer,
        )
        return response.body
    }

    /** 修正待审提案：set / unset 至少一项；返回新修正与其 ETag。 */
    suspend fun amendProposal(
        proposalId: String,
        set: JsonObject,
        unset: List<String>,
        note: String?,
        ifMatch: String?,
        idempotencyKey: String,
    ): ApiResponse<EditAmendment> {
        val body = KungalJson.encodeToString(
            AmendmentBody.serializer(),
            AmendmentBody(set = set, unset = unset, note = note),
        )
        return client.postJsonWithResponse(
            path = "/edit-proposals/$proposalId/amendments",
            body = body,
            headers = ifMatchHeaders(ifMatch),
            idempotencyKey = idempotencyKey,
            deserializer = EditAmendment.serializer(),
        )
    }

    /** 合并 / 拒绝 / 撤回提案。declined 必须带说明；返回提案回读结果与最新 ETag。 */
    suspend fun updateProposal(
        proposalId: String,
        state: String,
        note: String? = null,
        ifMatch: String? = null,
    ): ApiResponse<EditProposal> {
        val body = KungalJson.encodeToString(
            ProposalUpdateBody.serializer(),
            ProposalUpdateBody(state = state, note = note),
        )
        return client.patchJsonWithResponse(
            path = "/edit-proposals/$proposalId",
            body = body,
            headers = ifMatchHeaders(ifMatch),
            deserializer = EditProposal.serializer(),
        )
    }

    /** 修订链（页码分页，最新在前）。 */
    suspend fun revisions(
        workId: String,
        page: Int = 1,
        limit: Int = 50,
    ): PagedList<EditRevision> = client.get(
        path = "/works/$workId/edit-revisions",
        query = mapOf("page" to page, "limit" to limit),
        deserializer = PagedList.serializer(EditRevision.serializer()),
    )

    /** 两个修订版本之间的字段级差异。 */
    suspend fun diff(workId: String, fromSeq: Int, toSeq: Int): EditRevisionDiff = client.get(
        path = "/works/$workId/edit-revisions/diff",
        query = mapOf("from_seq" to fromSeq, "to_seq" to toSeq),
        deserializer = EditRevisionDiff.serializer(),
    )

    /** 回滚到某修订：可信账号可能立即生成新修订，否则进入审核。 */
    suspend fun revert(
        workId: String,
        toSeq: Int,
        note: String?,
        idempotencyKey: String,
    ): EditRevert {
        val body = KungalJson.encodeToString(
            RevertBody.serializer(),
            RevertBody(toSeq = toSeq, note = note),
        )
        return client.postJson(
            path = "/works/$workId/edit-reverts",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = EditRevert.serializer(),
        )
    }

    /** 上传封面/画廊图片供编辑提案引用（multipart，≤10 MiB；不计每日图片额度）。 */
    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        preset: String,
        idempotencyKey: String,
    ): UploadedImage = client.postMultipart(
        path = "/work-edit-images",
        fileBytes = bytes,
        fileName = fileName,
        contentType = contentType,
        fields = mapOf("preset" to preset),
        idempotencyKey = idempotencyKey,
        deserializer = UploadedImage.serializer(),
    )

    private fun ifMatchHeaders(etag: String?): Map<String, String> =
        if (etag.isNullOrBlank()) emptyMap() else mapOf("If-Match" to etag)
}
