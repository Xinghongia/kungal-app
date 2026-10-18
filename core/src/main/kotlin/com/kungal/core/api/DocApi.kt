package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.Doc
import com.kungal.core.net.PageList

/** 帮助文档（/docs）。 */
class DocApi(private val client: KungalClient) {

    suspend fun list(
        cursor: String? = null,
        limit: Int = 40,
        sort: String? = null,
        docCategory: String? = null,
        isPinned: Boolean? = null,
    ): PageList<Doc> = client.get(
        path = "/docs",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("sort", sort)
            put("doc_category", docCategory)
            put("is_pinned", isPinned)
        },
        deserializer = PageList.serializer(Doc.serializer()),
    )

    /** 文档详情：`/docs/{slug}`，正文是块节点树。 */
    suspend fun detail(slug: String): Doc = client.get(
        path = "/docs/$slug",
        deserializer = Doc.serializer(),
    )
}
