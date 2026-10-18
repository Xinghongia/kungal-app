package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.TodoPage

/** 站点待办板（/todos）。只读：认领/完成/编辑等站务写操作不在 App 开放。 */
class TodoApi(private val client: KungalClient) {

    suspend fun list(
        cursor: String? = null,
        limit: Int = 20,
        includeTotal: Boolean = false,
        state: String? = null,
    ): TodoPage = client.get(
        path = "/todos",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("include_total", includeTotal)
            put("state", state)
        },
        deserializer = TodoPage.serializer(),
    )
}
