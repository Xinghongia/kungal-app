package com.kungal.app.ui.messages

import com.kungal.core.model.FollowedWall
import com.kungal.core.model.Notification

/**
 * 通知类型与文案：类型词汇、中文标签、网页同款折叠文案（「等 N 人…」）、
 * 以及关注墙的类型/标题/网页地址。网页来源 `constants/notification.ts` 与
 * `components/message/utils/getMessageI18n.ts`。
 */
object NotificationCatalog {

    data class Category(
        val key: String,
        val label: String,
        val types: List<String>,
    )

    /** 通知偏好分组；私信 chat 只在偏好里出现，不是通知列表类型。 */
    val categories: List<Category> = listOf(
        Category("interaction", "互动", listOf("upvoted", "liked", "favorited", "mentioned")),
        Category("follow", "关注", listOf("user_followed", "followee_activity_published")),
        Category(
            "reply", "回复评论",
            listOf(
                "replied", "commented", "followed_thread_activity", "subscribed_topic_replied",
                "best_answer_chosen", "reply_pinned", "quiz_answered",
            ),
        ),
        Category(
            "review", "内容审核",
            listOf(
                "edit_requested", "edit_merged", "edit_declined",
                "resource_link_reported", "resource_relocated",
            ),
        ),
        Category(
            "miniapp", "话题小程序",
            listOf("lottery_won", "lottery_drawn", "lottery_code_expired", "poll_closed"),
        ),
        Category("system", "系统", listOf("system_notice")),
    )

    val preferenceGroups: List<Category> = categories + Category("chat", "私信", listOf("chat"))

    /** 通知列表可以筛选的类型（不含 chat）。 */
    val notificationTypes: List<String> = categories.flatMap { it.types }

    private val labels: Map<String, String> = mapOf(
        "upvoted" to "被推荐",
        "liked" to "被点赞",
        "favorited" to "被收藏",
        "mentioned" to "被 @ 提及",
        "user_followed" to "被关注",
        "followee_activity_published" to "关注的人有新发布",
        "replied" to "收到回复",
        "commented" to "收到评论",
        "followed_thread_activity" to "关注的评论区有新评论",
        "subscribed_topic_replied" to "关注的话题有新回复",
        "best_answer_chosen" to "回复被采纳为最佳答案",
        "reply_pinned" to "回复被置顶",
        "quiz_answered" to "题目被回答",
        "edit_requested" to "收到更新请求",
        "edit_merged" to "更新请求被合并",
        "edit_declined" to "更新请求被拒绝",
        "resource_link_reported" to "资源链接被报告过期",
        "resource_relocated" to "资源搬迁到 LetMoe",
        "lottery_won" to "抽奖中奖",
        "lottery_drawn" to "参与的抽奖开奖",
        "lottery_code_expired" to "兑换码过期作废",
        "poll_closed" to "参与的投票截止",
        "system_notice" to "系统通知",
        "chat" to "私信消息",
    )

    fun typeLabel(type: String): String = labels[type] ?: type

    private val actorTemplates: Map<String, String> = mapOf(
        "upvoted" to " 推了您！",
        "liked" to " 点赞了您！",
        "favorited" to " 收藏了您！",
        "replied" to " 回复了您！",
        "commented" to " 评论了您！",
        "mentioned" to " 提到了您！",
        "followed_thread_activity" to " 在您关注的评论区发表了新评论",
        "reply_pinned" to " 置顶了您的回复！",
        "quiz_answered" to " 回答了您的题目！",
        "resource_link_reported" to " 报告了您的资源链接已过期！",
        "edit_requested" to " 向您提出更新请求！",
        "edit_merged" to " 合并了您的更新请求！",
        "edit_declined" to " 拒绝了您的更新请求！",
        "lottery_won" to " 的抽奖开奖了，您中奖了！",
        "lottery_drawn" to " 的抽奖开奖了",
        "lottery_code_expired" to " 的抽奖兑换码已过领取期限",
        "poll_closed" to " 的投票已经截止",
        "user_followed" to " 关注了您！",
        "followee_topic_created" to " 发布了新话题！",
        "followee_activity_published" to " 发布了新内容！",
        "subscribed_topic_replied" to " 在您关注的话题中发表了新回复",
    )

    private val actorlessTemplates: Map<String, String> = mapOf(
        "best_answer_chosen" to "您的回复被标记为最佳答案！",
        "resource_relocated" to "通知您：资源已搬迁到 一起萌·LetMoe",
    )

    /** 通知主文案：演员名 + 折叠后的动作；系统/无演员类型单独成句。 */
    fun messageText(notification: Notification): String {
        val type = notification.notificationType
        if (type == "system_notice") {
            return if (notification.itemCount > 1) {
                "系统通知（${notification.itemCount} 条）"
            } else {
                "系统通知"
            }
        }
        actorlessTemplates[type]?.let { return it }

        val actor = notification.actor.name?.takeIf { it.isNotBlank() } ?: "已注销用户"
        val count = notification.actorCount
        val items = notification.itemCount
        val suffix = when {
            type == "mentioned" && notification.excerptMarkdown.isNotBlank() &&
                notification.origin == "local" -> " 回复了您！"
            count > 1 && type == "liked" -> " 等 $count 人点赞了您！"
            count > 1 && type == "followed_thread_activity" ->
                " 等 $count 人在您关注的评论区发表了 $items 条新评论"
            count > 1 && type == "user_followed" -> " 等 $count 人关注了您！"
            count > 1 && type == "subscribed_topic_replied" ->
                " 等 $count 人在您关注的话题中发表了 $items 条新回复"
            type == "followee_activity_published" && items > 1 ->
                " 发布了 ${if (items >= 100) "100+" else items} 个新内容"
            type == "followed_thread_activity" && items > 1 ->
                " 在您关注的评论区发表了 $items 条新评论"
            type == "subscribed_topic_replied" && items > 1 ->
                " 在您关注的话题中发表了 $items 条新回复"
            else -> actorTemplates[type] ?: " 发来了新通知"
        }
        return actor + suffix
    }

    /** 通知内容预览：user_followed 固定显示「查看 TA 的主页」，其余取脱敏后的正文摘要。 */
    fun preview(notification: Notification): String {
        if (notification.notificationType == "user_followed") return "查看 TA 的主页"
        return plainText(notification.excerptMarkdown).ifBlank { "点击查看详情" }
    }

    /** 把 Markdown 摘要压成单行纯文本（只用于列表预览，不参与渲染）。 */
    fun plainText(markdown: String): String {
        if (markdown.isBlank()) return ""
        var text = markdown
        text = text.replace(Regex("```[\\s\\S]*?```"), "……")
        text = text.replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "[图片]")
        text = text.replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        text = text.replace(Regex("[#>*_`~]+"), "")
        text = text.replace(Regex("[ \\t\\r\\n]+"), " ")
        return text.trim()
    }

    private val wallTypeLabels = mapOf(
        "galgame" to "Galgame",
        "galgame_rating" to "游戏评分",
        "galgame_resource" to "下载资源",
        "galgame_quiz" to "游戏答题",
        "toolset" to "Gal 工具",
        "website" to "网站",
    )

    fun wallTypeLabel(type: String): String = wallTypeLabels[type] ?: type

    fun wallTitle(wall: FollowedWall): String {
        wall.work?.let { work ->
            val name = work.displayName.ifBlank { work.latin.orEmpty() }
            if (name.isNotBlank()) return name
        }
        wall.website?.let { site ->
            if (site.title.isNotBlank()) return site.title
            if (site.host.isNotBlank()) return site.host
        }
        return "${wallTypeLabel(wall.subjectType)} #${wall.subjectId}"
    }

    /** 关注墙对应的网页地址（客户端还没有这些详情页，统一跳浏览器）。 */
    fun wallWebPath(wall: FollowedWall): String = when (wall.subjectType) {
        "galgame" -> "/galgame/${wall.subjectId}"
        "galgame_rating" -> "/galgame-rating/${wall.subjectId}"
        "galgame_resource" -> "/galgame/resource/${wall.subjectId}"
        "galgame_quiz" -> "/galgame-quiz/${wall.subjectId}"
        "toolset" -> "/toolset/${wall.subjectId}"
        "website" -> "/website/${wall.website?.host?.takeIf { it.isNotBlank() } ?: wall.subjectId}"
        else -> "/message/follow"
    }
}
