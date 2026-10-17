package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.WorkSubmission
import com.kungal.core.model.WorkSubmissionCandidate
import com.kungal.core.model.WorkSubmissionCreate
import com.kungal.core.model.WorkSubmissionCreated
import com.kungal.core.model.WorkSubmissionList
import com.kungal.core.model.WorkSubmissionPatch
import com.kungal.core.net.ApiResponse
import com.kungal.core.net.PageList

/**
 * Galgame 投稿（BP-02 U6）：发布前的候选搜索、新建申请、我的提交与状态流转。
 * 候选搜索与投稿接口都需要登录；新建带 Idempotency-Key，状态变更用 ETag 作为 If-Match。
 */
class SubmissionApi(private val client: KungalClient) {

    /** 按名称搜索资料库：返回已有作品与本站认领状态。q 必填（≤200）。 */
    suspend fun candidates(
        query: String,
        includeNsfw: Boolean,
        cursor: String? = null,
        limit: Int = 12,
    ): PageList<WorkSubmissionCandidate> = client.get(
        path = "/work-submission-candidates",
        query = buildMap {
            put("q", query)
            put("include_nsfw", includeNsfw)
            put("limit", limit)
            if (cursor != null) put("cursor", cursor)
        },
        deserializer = PageList.serializer(WorkSubmissionCandidate.serializer()),
    )

    /** 我的投稿（最新活动在前）；states 为空表示全部，多个状态用逗号传递。 */
    suspend fun mySubmissions(
        states: List<String> = emptyList(),
        cursor: String? = null,
        limit: Int = 20,
        includeTotal: Boolean = false,
    ): WorkSubmissionList = client.get(
        path = "/me/work-submissions",
        query = buildMap {
            if (states.isNotEmpty()) put("state", states.joinToString(","))
            put("limit", limit)
            if (includeTotal) put("include_total", true)
            if (cursor != null) put("cursor", cursor)
        },
        deserializer = WorkSubmissionList.serializer(),
    )

    /** 新建申请；409 DUPLICATE_SUSPECTS 时作者确认后用 is_duplicate_confirmed=true 重发。 */
    suspend fun create(
        request: WorkSubmissionCreate,
        idempotencyKey: String,
    ): ApiResponse<WorkSubmissionCreated> {
        val body = KungalJson.encodeToString(WorkSubmissionCreate.serializer(), request)
        return client.postJsonWithResponse(
            path = "/work-submissions",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = WorkSubmissionCreated.serializer(),
        )
    }

    /** 投稿详情；ETag 是后续状态变更 / 删除的 If-Match。 */
    suspend fun submission(workId: String): ApiResponse<WorkSubmission> = client.getWithResponse(
        path = "/work-submissions/$workId",
        deserializer = WorkSubmission.serializer(),
    )

    /** 作者送审（pending）或撤回（draft）；审阅者用 live / declined / hidden / unban。 */
    suspend fun updateState(
        workId: String,
        state: String,
        note: String? = null,
        ifMatch: String? = null,
    ): ApiResponse<WorkSubmission> {
        val body = KungalJson.encodeToString(
            WorkSubmissionPatch.serializer(),
            WorkSubmissionPatch(state = state, note = note),
        )
        return client.patchJsonWithResponse(
            path = "/work-submissions/$workId",
            body = body,
            headers = ifMatchHeaders(ifMatch),
            deserializer = WorkSubmission.serializer(),
        )
    }

    /** 删除草稿（只有 draft 可删，其余先撤回）。 */
    suspend fun deleteDraft(workId: String, ifMatch: String? = null) {
        client.deleteNoBody("/work-submissions/$workId", ifMatchHeaders(ifMatch))
    }

    private fun ifMatchHeaders(etag: String?): Map<String, String> =
        if (etag.isNullOrBlank()) emptyMap() else mapOf("If-Match" to etag)
}
