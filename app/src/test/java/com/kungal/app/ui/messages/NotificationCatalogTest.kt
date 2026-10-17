package com.kungal.app.ui.messages

import com.kungal.core.model.FollowedWall
import com.kungal.core.model.Notification
import com.kungal.core.model.UserRef
import com.kungal.core.model.WebsiteSummary
import com.kungal.core.model.WorkRef
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCatalogTest {

    private fun notification(
        type: String,
        actorCount: Int = 1,
        itemCount: Int = 1,
        excerpt: String = "",
        origin: String = "local",
    ) = Notification(
        id = "1",
        notificationType = type,
        actor = UserRef(id = "2", name = "阿瞳"),
        actorCount = actorCount,
        itemCount = itemCount,
        excerptMarkdown = excerpt,
        origin = origin,
    )

    @Test
    fun `点赞按人数折叠`() {
        assertEquals("阿瞳 点赞了您！", NotificationCatalog.messageText(notification("liked")))
        assertEquals(
            "阿瞳 等 3 人点赞了您！",
            NotificationCatalog.messageText(notification("liked", actorCount = 3)),
        )
    }

    @Test
    fun `评论区与话题回复按人数与条数折叠`() {
        assertEquals(
            "阿瞳 等 4 人在您关注的评论区发表了 6 条新评论",
            NotificationCatalog.messageText(
                notification("followed_thread_activity", actorCount = 4, itemCount = 6),
            ),
        )
        assertEquals(
            "阿瞳 在您关注的评论区发表了 5 条新评论",
            NotificationCatalog.messageText(
                notification("followed_thread_activity", itemCount = 5),
            ),
        )
        assertEquals(
            "阿瞳 等 3 人在您关注的话题中发表了 7 条新回复",
            NotificationCatalog.messageText(
                notification("subscribed_topic_replied", actorCount = 3, itemCount = 7),
            ),
        )
    }

    @Test
    fun `本地点提及有摘要时按回复展示`() {
        assertEquals(
            "阿瞳 回复了您！",
            NotificationCatalog.messageText(
                notification("mentioned", excerpt = "hello", origin = "local"),
            ),
        )
        assertEquals(
            "阿瞳 提到了您！",
            NotificationCatalog.messageText(
                notification("mentioned", excerpt = "hello", origin = "community"),
            ),
        )
    }

    @Test
    fun `系统通知按条数折叠`() {
        assertEquals("系统通知", NotificationCatalog.messageText(notification("system_notice")))
        assertEquals(
            "系统通知（3 条）",
            NotificationCatalog.messageText(notification("system_notice", itemCount = 3)),
        )
    }

    @Test
    fun `无演员的通知单独成句`() {
        assertEquals(
            "您的回复被标记为最佳答案！",
            NotificationCatalog.messageText(notification("best_answer_chosen")),
        )
        assertEquals(
            "通知您：资源已搬迁到 一起萌·LetMoe",
            NotificationCatalog.messageText(notification("resource_relocated")),
        )
    }

    @Test
    fun `关注通知的预览固定为查看主页`() {
        assertEquals(
            "查看 TA 的主页",
            NotificationCatalog.preview(notification("user_followed", excerpt = "**任意文本**")),
        )
        assertEquals(
            "好耶 链接 code",
            NotificationCatalog.preview(
                notification("liked", excerpt = "**好耶** [链接](https://example.com) `code`"),
            ),
        )
    }

    @Test
    fun `关注墙标题与网页地址`() {
        val workWall = FollowedWall(
            subjectType = "galgame",
            subjectId = "77",
            work = WorkRef(id = "77", displayName = "星之梦"),
        )
        assertEquals("星之梦", NotificationCatalog.wallTitle(workWall))
        assertEquals("/galgame/77", NotificationCatalog.wallWebPath(workWall))

        val siteWall = FollowedWall(
            subjectType = "website",
            subjectId = "88",
            website = WebsiteSummary(id = "88", host = "example.com", title = "示例站"),
        )
        assertEquals("示例站", NotificationCatalog.wallTitle(siteWall))
        assertEquals("/website/example.com", NotificationCatalog.wallWebPath(siteWall))

        val fallback = FollowedWall(subjectType = "toolset", subjectId = "9")
        assertEquals("Gal 工具 #9", NotificationCatalog.wallTitle(fallback))
        assertEquals("/toolset/9", NotificationCatalog.wallWebPath(fallback))
    }
}
