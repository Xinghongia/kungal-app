package com.kungal.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * v1 列表响应的统一信封：{object:"list", items:[...], next_cursor?}。
 * next_cursor 为不透明键集游标，仅在还有后续页时出现。
 */
@Serializable
data class PageList<T>(
    @SerialName("object") val obj: String = "list",
    val items: List<T> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
) {
    val hasMore: Boolean get() = nextCursor != null
}
