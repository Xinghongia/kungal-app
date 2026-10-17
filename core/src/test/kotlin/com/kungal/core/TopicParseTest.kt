package com.kungal.core

import com.kungal.core.model.Reply
import com.kungal.core.model.Topic
import com.kungal.core.net.PageList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicParseTest {

    @Test
    fun `话题列表样本字段完整`() {
        val page = KungalJson.decodeFromString<PageList<Topic>>(Fixtures.text("topics_list.json"))

        assertEquals("list", page.obj)
        assertEquals(3, page.items.size)
        assertNotNull(page.nextCursor)
        assertTrue(page.hasMore)

        val first = page.items[0]
        assertEquals("4462", first.id)
        assertEquals("Tensei Games新作福利抽key《女英雄战斗竞技场：堕落竞技场》", first.title)
        assertEquals("published", first.state)
        assertEquals("galgame", first.category)
        assertEquals(listOf("g-news", "g-releases"), first.sections)
        assertEquals(970, first.viewCount)
        assertEquals(14, first.likeCount)
        assertEquals(15, first.replyCount)
        assertTrue(first.miniApps.contains("lottery"))
        assertFalse(first.isNsfw)
        assertEquals("135145", first.author.id)
        assertEquals("TenseiGames", first.author.name)
        assertNotNull(first.author.avatar?.url)
        assertEquals(64, first.coverImages[0].hash.length)
        assertEquals(1440, first.coverImages[0].width)
        assertEquals("suggestive", first.coverImages[0].sexual)
        assertEquals("2026-10-13T04:56:25Z", first.createdAt)
        // 列表页没有正文与 viewer
        assertNull(first.content)
        assertNull(first.viewer)
    }

    @Test
    fun `话题详情样本字段完整`() {
        val topic = KungalJson.decodeFromString<Topic>(Fixtures.text("topic_detail.json"))

        assertEquals("4462", topic.id)
        assertEquals("public", topic.accessScope)
        assertNull(topic.hiddenBy)
        assertEquals(144, topic.authorMoemoepoint)
        assertNotNull(topic.content)
        assertEquals(16, topic.content!!.children.size)

        assertEquals(14, topic.reactions[0].count)
        assertEquals("like", topic.reactions[0].reaction)
        assertEquals(3, topic.reactions[0].reactors.size)
        assertEquals("enwei", topic.reactions[0].reactors[0].name)

        assertEquals(0, topic.dislikeCount)
        assertEquals(970, topic.viewCount)
        assertNull(topic.viewer)
        assertNull(topic.pinnedReply)
        assertNull(topic.bestAnswer)
    }

    @Test
    fun `回复列表样本字段完整`() {
        val page = KungalJson.decodeFromString<PageList<Reply>>(Fixtures.text("replies.json"))

        assertEquals(3, page.items.size)
        assertNotNull(page.nextCursor)

        val floor1 = page.items[0]
        assertEquals("17768", floor1.id)
        assertEquals("4462", floor1.topicId)
        assertEquals(1, floor1.floor)
        assertEquals("宇宇", floor1.author.name)
        assertEquals(1946, floor1.authorMoemoepoint)
        assertNotNull(floor1.content)
        assertFalse(floor1.isPinned)
        assertFalse(floor1.isBestAnswer)
        assertTrue(floor1.comments.isEmpty())
        assertNull(floor1.viewer)

        assertEquals(listOf(1, 2, 3), page.items.map { it.floor })
    }

    @Test
    fun `点赞反应带 viewer 状态时能解析`() {
        // 真实样本里 reactions[].viewer 为 null（匿名）；登录后会有 has_reacted
        val json = """
        {"object":"topic","id":"1","title":"t","reactions":[
          {"reaction":"like","count":2,"reactors":[],"viewer":{"has_reacted":true}}
        ]}
        """.trimIndent()
        val topic = KungalJson.decodeFromString<Topic>(json)
        assertEquals(true, topic.reactions[0].viewer?.hasReacted)
    }

    @Test
    fun `详情内嵌 pinned_reply 与 best_answer 用 Reply 模型解析`() {
        val json = """
        {"object":"topic","id":"1","title":"t",
         "pinned_reply":{"object":"reply","id":"9","topic_id":"1","floor":2,"author":{"id":"5","name":"a"}},
         "best_answer":{"object":"reply","id":"10","topic_id":"1","floor":4,"is_best_answer":true,"author":{"id":"6"}}}
        """.trimIndent()
        val topic = KungalJson.decodeFromString<Topic>(json)
        assertEquals(2, topic.pinnedReply?.floor)
        assertEquals(true, topic.bestAnswer?.isBestAnswer)
    }
}
