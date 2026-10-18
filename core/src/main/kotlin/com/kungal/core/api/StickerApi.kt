package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.StickerPackList

/** 官方表情包清单（编辑器和私信共用），整包一次取回，页面内可缓存。 */
class StickerApi(private val client: KungalClient) {

    suspend fun packs(): StickerPackList = client.get(
        path = "/sticker-packs",
        deserializer = StickerPackList.serializer(),
    )
}
