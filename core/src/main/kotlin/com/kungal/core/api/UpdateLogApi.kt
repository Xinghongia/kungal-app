package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.UpdateLog
import com.kungal.core.net.PageList

/** 站点更新日志（/update-logs）。只读：管理员的增删改不在 App 开放。 */
class UpdateLogApi(private val client: KungalClient) {

    suspend fun list(
        cursor: String? = null,
        limit: Int = 20,
    ): PageList<UpdateLog> = client.get(
        path = "/update-logs",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
        },
        deserializer = PageList.serializer(UpdateLog.serializer()),
    )
}
