package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.Section
import com.kungal.core.net.PageList

/** 版块接口（GET /sections，可按分类过滤）。 */
class SectionApi(private val client: KungalClient) {

    suspend fun list(category: String? = null): PageList<Section> = client.get(
        path = "/sections",
        query = mapOf("category" to category),
        deserializer = PageList.serializer(Section.serializer()),
    )
}
