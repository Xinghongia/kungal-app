package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 站点待办（GET /todos）。 */
@Serializable
data class Todo(
    @SerialName("object") val obj: String = "todo",
    val id: String = "",
    val project: String = "",
    val state: String = "",
    val text: String = "",
    val author: UserRef = UserRef(),
    val claimer: UserRef? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: TodoViewer? = null,
)

/** 当前用户对待办的操作权限；匿名或 Bearer 登录时为 null。 */
@Serializable
data class TodoViewer(
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_claim") val canClaim: Boolean = false,
    @SerialName("can_complete") val canComplete: Boolean = false,
    @SerialName("can_discard") val canDiscard: Boolean = false,
    @SerialName("can_release") val canRelease: Boolean = false,
    @SerialName("can_reopen") val canReopen: Boolean = false,
)

/** 待办列表的一页；只有 include_total=true 时才有 [total]。 */
@Serializable
data class TodoPage(
    @SerialName("object") val obj: String = "list",
    val items: List<Todo> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Int? = null,
)
