package com.kungal.app.ui.activity

import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.markdownToPlainText
import com.kungal.app.ui.galgame.workTitleOf
import com.kungal.core.model.FeedActivity
import com.kungal.core.model.WorkRef

/** 动态类型 → 中文标签（对齐网页 constants/activity.ts）。 */
val ActivityTypeLabels: Map<String, String> = mapOf(
    "galgame_creation" to "Galgame",
    "galgame_rating_creation" to "Galgame 评分",
    "galgame_rating_comment_creation" to "Galgame 评分评论",
    "topic_creation" to "新话题",
    "topic_upvote" to "话题被推",
    "best_answer_set" to "最佳答案",
    "topic_reply_creation" to "话题回复",
    "topic_comment_creation" to "话题评论",
    "galgame_website_creation" to "Galgame 网站",
    "galgame_resource_creation" to "Galgame 资源",
    "galgame_quiz_creation" to "Galgame 题目",
    "galgame_edit" to "Galgame 编辑",
    "galgame_pr_creation" to "提出更新请求",
    "galgame_comment_creation" to "Galgame 评论",
    "galgame_website_comment_creation" to "Galgame 网站评论",
    "todo_creation" to "待办",
    "update_log_creation" to "更新日志",
    "toolset_creation" to "Galgame 工具",
    "toolset_resource_creation" to "工具资源",
    "toolset_comment_creation" to "工具评论",
    "galgame_resource_comment_creation" to "Galgame 资源评论",
    "galgame_quiz_comment_creation" to "Galgame 题目讨论",
)

/** 分类动态的筛选项分组（顺序与网页一致）。 */
val ActivityTypeGroups: List<Pair<String, List<String>>> = listOf(
    "Galgame" to listOf(
        "galgame_creation",
        "galgame_edit",
        "galgame_pr_creation",
        "galgame_resource_creation",
        "galgame_resource_comment_creation",
        "galgame_quiz_creation",
        "galgame_quiz_comment_creation",
        "galgame_rating_creation",
        "galgame_rating_comment_creation",
        "galgame_comment_creation",
        "galgame_website_creation",
        "galgame_website_comment_creation",
    ),
    "社区" to listOf(
        "topic_creation",
        "topic_reply_creation",
        "topic_comment_creation",
        "topic_upvote",
        "best_answer_set",
    ),
    "工具集" to listOf(
        "toolset_creation",
        "toolset_resource_creation",
        "toolset_comment_creation",
    ),
    "站务" to listOf("todo_creation", "update_log_creation"),
)

fun activityTypeLabel(type: String): String = ActivityTypeLabels[type] ?: type

/** 分类筛选面板的扁平选项：组名 + 类型名，避免 22 项挤在一起没有层次。 */
val ActivityTypeOptions: List<FilterOption> = ActivityTypeGroups.flatMap { (group, types) ->
    types.map { type -> FilterOption(type, "$group · ${activityTypeLabel(type)}") }
}

/** 动态摘要：工作类优先显示作品名，其余用服务端摘录（网页 activitySummaryText）。 */
fun activitySummaryText(activity: FeedActivity): String {
    val name = activity.work?.let(::workTitleOf).orEmpty()
    val text = when (activity.activityType) {
        "galgame_creation" -> name
        "galgame_resource_creation" -> "在《$name》发布了下载资源"
        "galgame_edit" -> "编辑了《$name》"
        "galgame_pr_creation" -> "对《$name》提出了更新请求"
        "galgame_rating_creation" -> {
            val summary = activity.galgameRating?.shortSummary.orEmpty()
            if (summary.isNotBlank()) "$name · $summary" else name
        }
        else -> activityInlineText(activity.excerptMarkdown)
    }
    return text.ifBlank { activityInlineText(activity.excerptMarkdown) }
}

/** 列表摘要压成单行：去掉 Markdown 标记与换行。 */
fun activityInlineText(markdown: String): String =
    markdownToPlainText(markdown).replace(Regex("[ \\t\\r\\n]+"), " ").trim()

/** 正文摘要保留换行（回复/评论/待办正文按网页那样按纯文本展示）。 */
fun activityBodyText(markdown: String): String = markdownToPlainText(markdown).trim()

fun activityWorkName(work: WorkRef?): String = work?.let(::workTitleOf).orEmpty()
