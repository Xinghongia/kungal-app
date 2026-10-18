package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.FriendLink
import com.kungal.core.net.PageList

/** 友情链接（/friend-links）。 */
class FriendLinkApi(private val client: KungalClient) {

    suspend fun list(
        cursor: String? = null,
        limit: Int = 100,
        category: String? = null,
    ): PageList<FriendLink> = client.get(
        path = "/friend-links",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("friend_link_category", category)
        },
        deserializer = PageList.serializer(FriendLink.serializer()),
    )
}
