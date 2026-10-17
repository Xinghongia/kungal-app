package com.kungal.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 页码分页的列表信封：{object:"list", items:[...], total, total_relation}。
 * total_relation = "eq" 表示 total 精确；"gte" 表示只给下限（站点对超大结果集的保护）。
 */
@Serializable
data class PagedList<T>(
    @SerialName("object") val obj: String = "list",
    val items: List<T> = emptyList(),
    val total: Int = 0,
    @SerialName("total_relation") val totalRelation: String? = null,
) {
    fun hasMore(page: Int, pageSize: Int): Boolean = page * pageSize < total
}
