package com.kungal.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** 编辑表单（GET /works/{work_id}/edit-form）：当前值 + 可编辑字段模式 + 受控词汇表。 */
@Serializable
data class EditForm(
    @SerialName("object") val obj: String = "edit_form",
    @SerialName("work_id") val workId: String = "",
    @SerialName("field_values") val fieldValues: JsonObject = JsonObject(emptyMap()),
    val fields: List<EditField> = emptyList(),
    val vocabularies: List<EditVocabulary> = emptyList(),
)

@Serializable
data class EditField(
    val key: String = "",
    @SerialName("field_type") val fieldType: String = "text",
    @SerialName("diff_hint") val diffHint: String = "inline",
    @SerialName("is_deprecated") val isDeprecated: Boolean = false,
    @SerialName("max_elements") val maxElements: Int = 0,
    @SerialName("max_suppressed") val maxSuppressed: Int = 0,
    val vocabulary: String = "",
    val encoding: String? = null,
    val base: Int = 0,
    @SerialName("is_nullable") val isNullable: Boolean = false,
    val element: EditFieldElement? = null,
)

@Serializable
data class EditFieldElement(
    @SerialName("element_type") val elementType: String = "",
    val members: List<EditFieldElementMember> = emptyList(),
)

@Serializable
data class EditFieldElementMember(
    val key: String = "",
    @SerialName("member_type") val memberType: String = "",
    val vocabulary: String = "",
    val base: Int = 0,
    @SerialName("is_nullable") val isNullable: Boolean = false,
)

@Serializable
data class EditVocabulary(
    @SerialName("object") val obj: String = "vocabulary",
    val vocabulary: String = "",
    @SerialName("is_closed") val isClosed: Boolean = true,
    val values: List<EditVocabularyValue> = emptyList(),
)

@Serializable
data class EditVocabularyValue(
    val value: String = "",
    @SerialName("display_name") val displayName: String = "",
    val description: String = "",
)

@Serializable
data class EditProposalViewer(
    @SerialName("is_proposer") val isProposer: Boolean = false,
    @SerialName("can_withdraw") val canWithdraw: Boolean = false,
    @SerialName("can_decide") val canDecide: Boolean = false,
    @SerialName("can_amend") val canAmend: Boolean = false,
)

/** 提案摘要（列表项）：不含 patch，patch 只在详情里。 */
@Serializable
data class EditProposalSummary(
    @SerialName("object") val obj: String = "edit_proposal",
    val id: String = "",
    val state: String = "open",
    @SerialName("work_id") val workId: String = "",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    val note: String? = null,
    val proposer: UserRef = UserRef(),
    val decider: UserRef? = null,
    @SerialName("base_revision_seq") val baseRevisionSeq: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("decided_at") val decidedAt: String? = null,
    val viewer: EditProposalViewer? = null,
)

@Serializable
data class EditAmendment(
    @SerialName("object") val obj: String = "edit_amendment",
    val id: String = "",
    val seq: Int = 0,
    val amender: UserRef = UserRef(),
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

/** 提案详情：摘要 + patch（提案原值）+ effective_patch（折叠修正后的值）+ 修正链。 */
@Serializable
data class EditProposal(
    @SerialName("object") val obj: String = "edit_proposal",
    val id: String = "",
    val state: String = "open",
    @SerialName("work_id") val workId: String = "",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    val note: String? = null,
    val proposer: UserRef = UserRef(),
    val decider: UserRef? = null,
    @SerialName("base_revision_seq") val baseRevisionSeq: Int = 0,
    val patch: JsonObject = JsonObject(emptyMap()),
    @SerialName("effective_patch") val effectivePatch: JsonObject = JsonObject(emptyMap()),
    val amendments: List<EditAmendment> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("decided_at") val decidedAt: String? = null,
    val viewer: EditProposalViewer? = null,
)

/**
 * POST /works/{id}/edit-proposals 的响应：Go 端把 EditProposal 内嵌在结果里，
 * JSON 顶层同时带提案字段与 revision，因此单独解码后拆分。
 */
data class WorkEditProposalCreateResult(
    val proposal: EditProposal,
    val revision: EditRevision?,
)

object WorkEditProposalCreateResultSerializer : KSerializer<WorkEditProposalCreateResult> {
    override val descriptor = buildClassSerialDescriptor("WorkEditProposalCreateResult")

    override fun deserialize(decoder: Decoder): WorkEditProposalCreateResult {
        val input = decoder as? JsonDecoder ?: error("只支持 JSON")
        val obj = input.decodeJsonElement().jsonObject
        val proposal = input.json.decodeFromJsonElement(EditProposal.serializer(), obj)
        val revision = obj["revision"]
            ?.takeUnless { it is JsonNull }
            ?.let { input.json.decodeFromJsonElement(EditRevision.serializer(), it) }
        return WorkEditProposalCreateResult(proposal, revision)
    }

    override fun serialize(encoder: Encoder, value: WorkEditProposalCreateResult) {
        val merged = encoder as? kotlinx.serialization.json.JsonEncoder ?: error("只支持 JSON")
        val base = merged.json.encodeToJsonElement(EditProposal.serializer(), value.proposal).jsonObject
        val out = JsonObject(
            base + mapOf(
                "revision" to (value.revision?.let {
                    merged.json.encodeToJsonElement(EditRevision.serializer(), it)
                } ?: JsonNull),
            ),
        )
        merged.encodeJsonElement(out)
    }
}

@Serializable
data class EditRevision(
    @SerialName("object") val obj: String = "edit_revision",
    val id: String = "",
    val seq: Int = 0,
    @SerialName("revision_action") val revisionAction: String = "created",
    @SerialName("changed_fields") val changedFields: List<String> = emptyList(),
    val actor: UserRef = UserRef(),
    @SerialName("last_amender") val lastAmender: UserRef? = null,
    @SerialName("proposal_id") val proposalId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class EditFieldChange(
    val key: String = "",
    /** 任意 JSON 值；null 表示该版本未设置。 */
    val from: JsonElement? = null,
    val to: JsonElement? = null,
)

@Serializable
data class EditRevisionDiff(
    @SerialName("object") val obj: String = "edit_revision_diff",
    @SerialName("from_seq") val fromSeq: Int = 1,
    @SerialName("to_seq") val toSeq: Int = 1,
    @SerialName("field_changes") val fieldChanges: List<EditFieldChange> = emptyList(),
)

@Serializable
data class EditRevert(
    @SerialName("object") val obj: String = "edit_revert",
    val proposal: EditProposal = EditProposal(),
    val revision: EditRevision? = null,
)

/** GET /me/permissions：当前账号持有的权限键。 */
@Serializable
data class PermissionList(
    val permissions: List<String> = emptyList(),
)

/** 提案状态的中文标签。 */
fun proposalStateLabel(state: String): String = when (state) {
    "open" -> "待审核"
    "merged" -> "已合并"
    "declined" -> "已拒绝"
    "withdrawn" -> "已撤回"
    else -> state
}

/** 修订来源的中文标签。 */
fun revisionActionLabel(action: String): String = when (action) {
    "created" -> "创建"
    "merged" -> "合并"
    "direct" -> "直接编辑"
    "reverted" -> "回滚"
    else -> action
}
