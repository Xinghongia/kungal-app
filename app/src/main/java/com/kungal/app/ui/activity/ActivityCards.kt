package com.kungal.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.galgame.GalgameLanguageOptions
import com.kungal.app.ui.galgame.GalgamePlatformOptions
import com.kungal.app.ui.galgame.GalgameResourceTypeOptions
import com.kungal.app.ui.galgame.labelOf
import com.kungal.app.ui.galgame.playStateLabel
import com.kungal.app.ui.galgame.platformShortLabel
import com.kungal.app.ui.galgame.ratingSpoilerLabel
import com.kungal.app.ui.galgame.recommendLabel
import com.kungal.app.ui.galgame.workTitleOf
import com.kungal.app.ui.quiz.quizDifficultyLabel
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.FeedActivity
import com.kungal.core.model.WorkRef

/**
 * 动态卡片（网页 components/activity/Card.vue）：左侧发起人头像，
 * 右侧按 activity_type 显示话题、回复、Galgame、资源、题目等详情块。
 * 整张卡点按跳到 activity.path 对应的原生页面。
 */
@Composable
fun ActivityCard(
    activity: FeedActivity,
    blurCover: Boolean,
    onOpenPath: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onOpenPath(activity.path) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            activity.performer?.let { performer ->
                AvatarImage(
                    url = performer.avatar?.url,
                    seed = performer.name,
                    modifier = Modifier.size(38.dp).clickable { onOpenUser(performer.id) },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ActivityHeader(activity, onOpenUser)
                ActivityBody(activity, blurCover)
            }
        }
    }
}

@Composable
private fun ActivityHeader(activity: FeedActivity, onOpenUser: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val performer = activity.performer
        Text(
            performer?.name?.takeIf { it.isNotBlank() } ?: "站点",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (performer != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(
                if (performer != null) {
                    Modifier.clickable { onOpenUser(performer.id) }
                } else {
                    Modifier
                },
            ),
        )
        Text(
            relativeTime(activity.occurredAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        ActivityTypeChip(activityTypeLabel(activity.activityType))
    }
}

@Composable
private fun ActivityTypeChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

@Composable
private fun ActivityBody(activity: FeedActivity, blurCover: Boolean) {
    when (activity.activityType) {
        "topic_creation", "topic_upvote" -> TopicActivityBody(activity, blurCover)
        "topic_reply_creation", "best_answer_set" -> ReplyActivityBody(activity)
        "topic_comment_creation" -> CommentActivityBody(activity)
        "galgame_creation" -> GalgameCreationBody(activity, blurCover)
        "galgame_edit", "galgame_pr_creation" -> GalgameEditBody(activity, blurCover)
        "galgame_rating_creation" -> GalgameRatingBody(activity, blurCover)
        "galgame_resource_creation" -> GalgameResourceBody(activity, blurCover)
        "galgame_comment_creation" -> GalgameCommentBody(activity, blurCover)
        "galgame_quiz_creation" -> QuizActivityBody(activity)
        "galgame_website_creation" -> SimpleNoteBody("创建了网站", activity.excerptMarkdown)
        "toolset_creation" -> SimpleNoteBody("创建了工具", activity.excerptMarkdown)
        "toolset_resource_creation" ->
            SimpleNoteBody("在工具《${activity.toolset?.title.orEmpty()}》发布了资源", activity.excerptMarkdown)
        "todo_creation" -> {
            val state = activity.todo?.state.orEmpty()
            SimpleNoteBody(
                "待办事项${if (state.isNotBlank()) " · ${todoStateLabel(state)}" else ""}",
                activity.excerptMarkdown,
            )
        }
        "update_log_creation" -> {
            val version = activity.updateLog?.releaseVersion.orEmpty()
            SimpleNoteBody(
                "更新日志${if (version.isNotBlank()) " · $version" else ""}",
                activity.excerptMarkdown,
            )
        }
        else -> {
            val summary = activitySummaryText(activity)
            if (summary.isNotBlank()) {
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TopicActivityBody(activity: FeedActivity, blurCover: Boolean) {
    val topic = activity.topic ?: return
    if (activity.activityType == "topic_upvote") {
        val blurb = activityInlineText(activity.excerptMarkdown)
        if (blurb.isNotBlank()) {
            Text(
                "推了这个话题，$blurb",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
    Text(
        topic.title.ifBlank { activityInlineText(activity.excerptMarkdown) },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    val excerpt = activity.topicDigest?.excerptMarkdown.orEmpty()
    if (excerpt.isNotBlank()) {
        Text(
            activityBodyText(excerpt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
    topic.coverImages.firstOrNull()?.url?.let { url ->
        if (blurCover && topic.isNsfw) {
            NsfwCoverImage(
                url = url,
                modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)),
            )
        } else {
            KungalImage(
                url = url,
                contentDescription = topic.title,
                modifier = Modifier.fillMaxWidth().height(130.dp),
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActivityCountChip("浏览 ${topic.viewCount}")
        ActivityCountChip("回复 ${topic.replyCount}")
        val favorites = activity.topicDigest?.favoriteCount ?: 0
        if (favorites > 0) ActivityCountChip("收藏 $favorites")
    }
}

@Composable
private fun ReplyActivityBody(activity: FeedActivity) {
    val reply = activity.reply ?: return
    if (activity.activityType == "best_answer_set") {
        Text(
            "采纳了最佳答案",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
    QuotedBox(reply.quoted?.floor, reply.quoted?.excerptMarkdown)
    val content = reply.content
    if (content != null && content.children.isNotEmpty()) {
        ContentDocumentView(content)
    } else {
        val text = activityBodyText(activity.excerptMarkdown)
        if (text.isNotBlank()) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
    ActivityTopicFooter(reply.topicTitle, reply.floor)
}

@Composable
private fun CommentActivityBody(activity: FeedActivity) {
    val comment = activity.comment ?: return
    QuotedBox(comment.quoted?.floor, comment.quoted?.excerptMarkdown)
    val text = activityBodyText(activity.excerptMarkdown)
    if (text.isNotBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
    ActivityTopicFooter(comment.topicTitle, null)
}

@Composable
private fun QuotedBox(floor: Int?, excerptMarkdown: String?) {
    val excerpt = activityInlineText(excerptMarkdown.orEmpty())
    if (floor == null || excerpt.isBlank()) return
    Text(
        "回复 #$floor：$excerpt",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActivityTopicFooter(topicTitle: String, floor: Int?) {
    if (topicTitle.isBlank()) return
    Text(
        buildString {
            if (floor != null) append("#$floor · ")
            append(topicTitle)
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun GalgameCreationBody(activity: FeedActivity, blurCover: Boolean) {
    val work = activity.work ?: return
    WorkHeader(work, blurCover)
    Text(
        "创建了一个新的 Galgame，已经有 ${activity.workStats?.resourceCount ?: 0} 个下载资源",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        activity.workStats?.let {
            ActivityCountChip("点赞 ${it.likeCount}")
            ActivityCountChip("收藏 ${it.favoriteCount}")
        }
    }
    WorkDigestLine(activity)
}

@Composable
private fun GalgameEditBody(activity: FeedActivity, blurCover: Boolean) {
    val work = activity.work ?: return
    Text(
        if (activity.activityType == "galgame_pr_creation") {
            "提出了《${workTitleOf(work)}》的更新请求"
        } else {
            "编辑了《${workTitleOf(work)}》"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    WorkHeader(work, blurCover)
    activity.workRevision?.revisionNumber?.let { number ->
        ActivityCountChip("修订 #$number")
    }
    if (activity.activityType == "galgame_pr_creation") {
        Text(
            "该更新请求需要被审核",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
    WorkDigestLine(activity)
}

@Composable
private fun GalgameRatingBody(activity: FeedActivity, blurCover: Boolean) {
    val work = activity.work ?: return
    val rating = activity.galgameRating ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WorkCover(work, blurCover, Modifier.width(52.dp).aspectRatio(5f / 7f))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                workTitleOf(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "评分 ${rating.overall} · ${recommendLabel(rating.recommend)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            rating.playStatus?.let { status ->
                Text(
                    playStateLabel(status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rating.spoilerLevel != "none") {
                Text(
                    ratingSpoilerLabel(rating.spoilerLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (rating.shortSummary.isNotBlank()) {
                Text(
                    rating.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GalgameResourceBody(activity: FeedActivity, blurCover: Boolean) {
    val work = activity.work ?: return
    val resource = activity.resource ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WorkCover(work, blurCover, Modifier.width(52.dp).aspectRatio(5f / 7f))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                workTitleOf(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip(labelOf(GalgameResourceTypeOptions, resource.resourceType))
                resource.resourcePlatforms.take(2).forEach { Chip(platformShortLabel(it)) }
                resource.resourceLanguages.take(2).forEach {
                    Chip(labelOf(GalgameLanguageOptions, it))
                }
            }
            if (resource.size.isNotBlank()) {
                Text(
                    resource.size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            resource.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GalgameCommentBody(activity: FeedActivity, blurCover: Boolean) {
    val work = activity.work ?: return
    val text = activityBodyText(activity.excerptMarkdown)
    if (text.isNotBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WorkCover(work, blurCover, Modifier.size(24.dp))
        Text(
            workTitleOf(work),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuizActivityBody(activity: FeedActivity) {
    val quiz = activity.quiz ?: return
    Text(
        "出了一道题目，${quizDifficultyLabel(quiz.difficulty)}·难度${quiz.difficulty}，" +
            "已经有 ${quiz.answerCount} 人作答。",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val question = activityBodyText(activity.excerptMarkdown)
    if (question.isNotBlank()) {
        Text(
            question,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val description = activityBodyText(quiz.descriptionExcerpt)
    if (description.isNotBlank()) {
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SimpleNoteBody(label: String, markdown: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val text = activityBodyText(markdown)
    if (text.isNotBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkHeader(work: WorkRef, blurCover: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCover(work, blurCover, Modifier.width(46.dp).aspectRatio(5f / 7f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                workTitleOf(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            work.latin?.takeIf { it.isNotBlank() }?.let { latin ->
                Text(
                    latin,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WorkDigestLine(activity: FeedActivity) {
    val digest = activity.workDigest ?: return
    val line = buildList {
        if (digest.developerNames.isNotEmpty()) add(digest.developerNames.joinToString("、"))
        digest.release?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" · ")
    if (line.isNotBlank()) {
        Text(
            line,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    digest.introExcerpt?.takeIf { it.isNotBlank() }?.let { intro ->
        Text(
            activityBodyText(intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkCover(work: WorkRef, blurCover: Boolean, modifier: Modifier) {
    val url = work.cover?.url
    if (blurCover && work.isNsfw) {
        NsfwCoverImage(url = url, modifier = modifier.clip(RoundedCornerShape(8.dp)))
    } else {
        KungalImage(
            url = url,
            contentDescription = workTitleOf(work),
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        )
    }
}

@Composable
private fun ActivityCountChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}
