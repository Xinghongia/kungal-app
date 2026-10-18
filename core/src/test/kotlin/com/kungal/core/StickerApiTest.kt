package com.kungal.core

import com.kungal.core.api.StickerApi
import com.kungal.core.model.stickerContentUrl
import com.kungal.core.model.stickerMarkdown
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StickerApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: StickerApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = KungalClient(baseUrl = server.url("/api/v1").toString().trimEnd('/'))
        api = StickerApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `表情包整包解析与路径`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("ETag", "\"abc\"")
                .body(Fixtures.text("sticker_packs.json"))
                .build(),
        )
        val packs = api.packs()
        assertTrue(packs.items.isNotEmpty())
        val pack = packs.items.first()
        assertTrue(pack.displayName.contains("表情包"))
        assertTrue(pack.stickers.isNotEmpty())
        assertTrue(pack.stickers.first().image!!.hash.isNotBlank())
        assertEquals(packs.items.sumOf { it.stickers.size }, packs.stickers.size)
        assertEquals("/api/v1/sticker-packs", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `正文插入 320 变体 Markdown`() {
        val image = com.kungal.core.model.ImageInfo(
            url = "https://image.kungal.iloveren.link/d5/2a/hash.webp",
            hash = "hash",
        )
        val sticker = com.kungal.core.model.Sticker(
            id = "s1",
            displayName = "鲲 Galgame 表情包 [1] - 1",
            image = image,
        )
        assertEquals(
            "https://image.kungal.iloveren.link/d5/2a/hash_320.webp",
            stickerContentUrl(image),
        )
        assertEquals(
            "![鲲 Galgame 表情包 [1] - 1](https://image.kungal.iloveren.link/d5/2a/hash_320.webp)",
            stickerMarkdown(sticker),
        )
    }
}
