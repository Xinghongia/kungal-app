package com.kungal.core

import com.kungal.core.model.ContentDocument
import com.kungal.core.model.ContentNode
import com.kungal.core.model.ImageNode
import com.kungal.core.model.ListNode
import com.kungal.core.model.ParagraphNode
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyReferenceNode
import com.kungal.core.model.SpoilerNode
import com.kungal.core.model.TextNode
import com.kungal.core.model.Topic
import com.kungal.core.model.UnknownNode
import com.kungal.core.net.PageList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParseTest {

    private fun doc(json: String): ContentDocument = KungalJson.decodeFromString(json)

    @Test
    fun `解析真实话题正文节点树`() {
        val topic = KungalJson.decodeFromString<Topic>(Fixtures.text("topic_detail.json"))
        val children = topic.content!!.children
        assertEquals(16, children.size)
        assertTrue(children.all { it is ParagraphNode })

        val first = children[0] as ParagraphNode
        val text = first.children[0] as TextNode
        assertTrue(text.value.startsWith("大家好"))

        // 段落里混排行内图片节点
        val images = children
            .filterIsInstance<ParagraphNode>()
            .flatMap { it.children }
            .filterIsInstance<ImageNode>()
        assertTrue(images.isNotEmpty())
        assertTrue(images[0].url.startsWith("https://image.kungal.iloveren.link/"))
        assertEquals(64, images[0].image?.hash?.length)
    }

    @Test
    fun `真实回复正文可解析`() {
        val replies = KungalJson.decodeFromString<PageList<Reply>>(Fixtures.text("replies.json"))
        val reply = replies.items[0]
        assertTrue((reply.content?.children?.isNotEmpty()) == true)
        val images = reply.content!!.children
            .filterIsInstance<ParagraphNode>()
            .flatMap { it.children }
            .filterIsInstance<ImageNode>()
        assertTrue(images.isNotEmpty())
    }

    @Test
    fun `全部节点类型都能解析`() {
        val json = """
        {"object":"document","children":[
          {"object":"heading","depth":2,"anchor":"h","children":[{"object":"text","value":"标题"}]},
          {"object":"thematic_break"},
          {"object":"blockquote","children":[{"object":"paragraph","children":[{"object":"text","value":"引用"}]}]},
          {"object":"list","is_ordered":true,"start":1,"is_spread":false,"children":[
            {"object":"list_item","is_checked":true,"children":[{"object":"paragraph","children":[{"object":"text","value":"项"}]}]}
          ]},
          {"object":"code","lang":"kotlin","value":"val a = 1"},
          {"object":"math","value":"E=mc^2"},
          {"object":"table","children":[
            {"object":"table_row","children":[{"object":"table_cell","align":"left","children":[{"object":"text","value":"格"}]}]}
          ]},
          {"object":"spoiler","children":[{"object":"paragraph","children":[{"object":"text","value":"剧透"}]}]},
          {"object":"paragraph","children":[
            {"object":"emphasis","children":[{"object":"text","value":"斜"}]},
            {"object":"strong","children":[{"object":"text","value":"粗"}]},
            {"object":"strikethrough","children":[{"object":"text","value":"删"}]},
            {"object":"inline_code","value":"code"},
            {"object":"inline_math","value":"x^2"},
            {"object":"break"},
            {"object":"link","url":"https://example.com","children":[{"object":"text","value":"链接"}]},
            {"object":"image","url":"https://example.com/i.webp","alt":"图","image":null,"is_sticker":false},
            {"object":"video","url":"https://example.com/v.mp4"},
            {"object":"inline_spoiler","children":[{"object":"text","value":"隐"}]},
            {"object":"mention","mentioned_user":{"object":"user","id":"2","name":"鲲"}},
            {"object":"reply_reference","reply_id":"17768","floor":3}
          ]}
        ]}
        """.trimIndent()

        val d = doc(json)
        assertEquals(9, d.children.size)
        assertTrue(d.children[3] is ListNode)
        assertTrue(d.children[7] is SpoilerNode)

        val inlines = (d.children[8] as ParagraphNode).children
        assertEquals(12, inlines.size)
        assertTrue(inlines.any { it is ImageNode })
        assertTrue(inlines.any { it is ReplyReferenceNode && it.floor == 3 })
    }

    @Test
    fun `未知节点类型落 UnknownNode 而不是抛异常`() {
        val json = """{"object":"document","children":[{"object":"future_widget","payload":123}]}"""
        val node: ContentNode = doc(json).children[0]
        assertTrue(node is UnknownNode)
        assertEquals("future_widget", (node as UnknownNode).objectName)
    }

    @Test
    fun `未知字段被忽略`() {
        val json = """{"object":"document","new_field":true,"children":[
          {"object":"paragraph","extra":"x","children":[{"object":"text","value":"ok","brand_new":1}]}
        ]}"""
        val d = doc(json)
        assertEquals("ok", ((d.children[0] as ParagraphNode).children[0] as TextNode).value)
    }

    @Test
    fun `空的 children 也能解析`() {
        val d = KungalJson.decodeFromString<ContentDocument>("""{"object":"document"}""")
        assertTrue(d.children.isEmpty())
    }
}
