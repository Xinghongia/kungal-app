package com.kungal.app.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.content.documentPlainText
import com.kungal.app.ui.galgame.ResourceEditorSheet
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.CollectionSummary
import com.kungal.core.model.GalgameResourceSummary
import com.kungal.core.model.QuizSummary
import com.kungal.core.model.RatingSummary
import com.kungal.core.model.ToolsetSummary
import com.kungal.core.model.UserCommentItem
import com.kungal.core.model.UserReplyItem
import com.kungal.core.model.UserTopicItem
import com.kungal.core.model.UserWorkRecord
import com.kungal.core.model.WorkPlaytime
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ---------- 动态概览 ----------

private data class OverviewData(
    val topics: List<UserTopicItem> = emptyList(),
    val replies: List<UserReplyItem> = emptyList(),
    val comments: List<UserCommentItem> = emptyList(),
    val works: List<WorkSummary> = emptyList(),
    val ratings: List<RatingSummary> = emptyList(),
    val resources: List<GalgameResourceSummary> = emptyList(),
) {
    val isEmpty: Boolean
        get() = topics.isEmpty() && replies.isEmpty() && comments.isEmpty() &&
            works.isEmpty() && ratings.isEmpty() && resources.isEmpty()
}

/** 动态：与网站一致，六类内容各取最近 4 条，可跳到对应标签看全部。 */
@Composable
fun ActivityOverviewTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onSelectTab: (UserTab) -> Unit,
) {
    var data by remember(userId, includeNsfw) { mutableStateOf<OverviewData?>(null) }
    var error by remember(userId, includeNsfw) { mutableStateOf<String?>(null) }
    var reload by remember(userId, includeNsfw) { mutableStateOf(0) }

    LaunchedEffect(userId, includeNsfw, reload) {
        data = null
        error = null
        runCatching {
            coroutineScope {
                val topics = async { runCatching { app.userApi.topics(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                val replies = async { runCatching { app.userApi.replies(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                val comments = async { runCatching { app.userApi.comments(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                val works = async { runCatching { app.userApi.works(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                val ratings = async { runCatching { app.userApi.ratings(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                val resources = async { runCatching { app.userApi.resources(userId, limit = 4, includeNsfw = includeNsfw).items }.getOrDefault(emptyList()) }
                OverviewData(
                    topics = topics.await(),
                    replies = replies.await(),
                    comments = comments.await(),
                    works = works.await(),
                    ratings = ratings.await(),
                    resources = resources.await(),
                )
            }
        }.onSuccess { data = it }
            .onFailure { error = friendlyError(it) }
    }

    when {
        error != null -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") { header() }
            item(key = "error") { ErrorBox(error!!, onRetry = { reload += 1 }) }
        }
        data?.isEmpty == true -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") { header() }
            item(key = "empty") { EmptyBox("这只笨蛋萝莉还没有任何动态") }
        }
        else -> {
            val d = data
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "header") { header() }
                if (d == null) {
                    item(key = "loading") { LoadingBox() }
                    return@LazyColumn
                }
                if (d.topics.isNotEmpty()) {
                    item { SectionHeader("发布的话题", onMore = { onSelectTab(UserTab.TOPIC) }) }
                    items(d.topics) { topic ->
                        SimpleContentRow(
                            title = topic.title,
                            subtitle = relativeTime(topic.createdAt),
                            onClick = { onOpenTopic(topic.id) },
                        )
                    }
                }
                if (d.replies.isNotEmpty()) {
                    item { SectionHeader("发布的回复", onMore = { onSelectTab(UserTab.TOPIC) }) }
                    items(d.replies) { reply ->
                        SimpleContentRow(
                            title = reply.excerpt.ifBlank { "回复 #${reply.floor}" },
                            subtitle = "#${reply.floor} · ${reply.topicTitle}",
                            onClick = { onOpenTopic(reply.topicId) },
                        )
                    }
                }
                if (d.comments.isNotEmpty()) {
                    item { SectionHeader("发布的评论", onMore = { onSelectTab(UserTab.TOPIC) }) }
                    items(d.comments) { comment ->
                        SimpleContentRow(
                            title = comment.excerpt,
                            subtitle = comment.topicTitle,
                            onClick = { onOpenTopic(comment.topicId) },
                        )
                    }
                }
                if (d.works.isNotEmpty()) {
                    item { SectionHeader("发布的作品", onMore = { onSelectTab(UserTab.GALGAME) }) }
                    items(d.works, key = { "work-${it.id}" }) { work ->
                        WorkRow(
                            work = work,
                            blurNsfw = blurNsfw,
                            onClick = { onOpenWeb(siteUrl("/galgame/${work.id}")) },
                        )
                    }
                }
                if (d.ratings.isNotEmpty()) {
                    item { SectionHeader("发布的评分", onMore = { onSelectTab(UserTab.RATING) }) }
                    items(d.ratings, key = { "rating-${it.id}" }) { rating ->
                        RatingRow(
                            rating = rating,
                            blurNsfw = blurNsfw,
                            onClick = { onOpenWeb(siteUrl("/galgame-rating/${rating.id}")) },
                        )
                    }
                }
                if (d.resources.isNotEmpty()) {
                    item { SectionHeader("发布的资源", onMore = { onSelectTab(UserTab.RESOURCE) }) }
                    items(d.resources, key = { "resource-${it.id}" }) { resource ->
                        ResourceRow(
                            resource = resource,
                            blurNsfw = blurNsfw,
                            onClick = { onOpenWeb(siteUrl("/galgame/resource/${resource.id}")) },
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ---------- 游戏库 ----------

@Composable
fun LibraryTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var filter by remember(userId) { mutableStateOf("all") }
    val options = listOf(
        "all" to "全部",
        "wish" to "想玩",
        "doing" to "在玩",
        "done" to "玩过",
        "on_hold" to "搁置",
        "dropped" to "抛弃",
    )
    PagedListContent(
        key = Triple(userId, filter, includeNsfw),
        load = { _, cursor ->
            val page = app.userApi.workRecords(
                userId = userId,
                states = if (filter == "all") emptyList() else listOf(filter),
                cursor = cursor,
                includeNsfw = includeNsfw,
            )
            PageResult(page.items, page.nextCursor, ended = page.nextCursor == null)
        },
        emptyText = "TA 还没有公开的游玩记录",
        modifier = Modifier.fillMaxSize(),
        header = {
            header()
            FilterChips(options = options, selected = filter, onSelect = { filter = it })
        },
        keyOf = { it.workSummary.id },
    ) { record -> WorkRecordRow(record, blurNsfw, onOpenWeb) }
}

@Composable
private fun WorkRecordRow(record: UserWorkRecord, blurNsfw: Boolean, onOpenWeb: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenWeb(siteUrl("/galgame/${record.workSummary.id}")) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCoverImage(
            url = record.workSummary.cover?.url,
            isNsfw = record.workSummary.isNsfw,
            blurNsfw = blurNsfw,
            modifier = Modifier.size(48.dp, 66.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                record.workSummary.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(playStateLabel(record.playState))
                record.userRating?.let { Chip("评分 ${it.overall}") }
            }
            val minutes = record.playtimeMinutes ?: 0
            if (minutes > 0) {
                Text(
                    "游玩 ${formatMinutes(minutes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- 收藏 ----------

@Composable
fun CollectionTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var mode by remember(userId) { mutableStateOf("folders") }
    val modeChips: @Composable () -> Unit = {
        header()
        FilterChips(
            options = listOf(
                "folders" to "收藏夹",
                "topics" to "话题",
                "toolsets" to "工具集",
            ),
            selected = mode,
            onSelect = { mode = it },
        )
    }
    when (mode) {
        "folders" -> PagedListContent(
            key = Triple(userId, "folders", includeNsfw),
            load = { page, _ ->
                val result = app.userApi.collections(userId, page = page, includeNsfw = includeNsfw)
                PageResult(result.items, ended = result.items.size < 24)
            },
            emptyText = "TA 还没有公开的收藏夹",
            modifier = Modifier.fillMaxSize(),
            header = modeChips,
            keyOf = { it.id },
        ) { collection -> CollectionRow(collection, onOpenWeb) }
        "toolsets" -> PagedListContent(
            key = Pair(userId, "fav-toolsets"),
            load = { page, _ ->
                val result = app.userApi.toolsets(userId, relation = "favorited", page = page)
                PageResult(result.items, ended = result.items.size < 24)
            },
            emptyText = "TA 还没有收藏工具集",
            modifier = Modifier.fillMaxSize(),
            header = modeChips,
            keyOf = { it.id },
        ) { toolset ->
            ToolsetRow(toolset, onClick = { onOpenWeb(siteUrl("/toolset/${toolset.id}")) })
        }
        else -> PagedListContent(
            key = Triple(userId, "fav-topics", includeNsfw),
            load = { page, _ ->
                val result = app.userApi.topics(
                    userId, relation = "favorited", page = page, limit = 50, includeNsfw = includeNsfw,
                )
                PageResult(result.items, ended = result.items.size < 50)
            },
            emptyText = "TA 还没有收藏话题",
            modifier = Modifier.fillMaxSize(),
            header = modeChips,
            keyOf = { it.id },
        ) { topic ->
            SimpleContentRow(
                title = topic.title,
                subtitle = relativeTime(topic.createdAt),
                onClick = { onOpenTopic(topic.id) },
            )
        }
    }
}

@Composable
private fun CollectionRow(collection: CollectionSummary, onOpenWeb: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenWeb(siteUrl("/collection/${collection.id}")) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val covers = collection.previewCovers.take(3)
            if (covers.isEmpty()) {
                KungalImage(
                    url = null,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp, 48.dp).clip(RoundedCornerShape(6.dp)),
                )
            } else {
                covers.forEach { cover ->
                    KungalImage(
                        url = cover.url,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp, 48.dp).clip(RoundedCornerShape(5.dp)),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    collection.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (collection.isDefault) {
                    Spacer(Modifier.width(6.dp))
                    Chip("默认")
                }
            }
            Text(
                "${collection.itemCount} 部作品",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- 话题 / 回复 / 评论 ----------

@Composable
fun TopicsTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenTopic: (String) -> Unit,
) {
    var kind by remember(userId) { mutableStateOf("topic") }
    var relation by remember(userId) { mutableStateOf("authored") }
    val kindLabel = when (kind) {
        "reply" -> "回复"
        "comment" -> "评论"
        else -> "话题"
    }
    val headerContent: @Composable () -> Unit = {
        header()
        FilterChips(
            options = listOf("topic" to "话题", "reply" to "回复", "comment" to "评论"),
            selected = kind,
            onSelect = {
                kind = it
                relation = "authored"
            },
        )
        FilterChips(
            options = if (kind == "topic") {
                listOf(
                    "authored" to "发布",
                    "liked" to "喜欢",
                    "upvoted" to "推",
                    "favorited" to "收藏",
                    "hidden" to "隐藏",
                )
            } else {
                listOf("authored" to "发布", "received" to "收到", "liked" to "点赞")
            },
            selected = relation,
            onSelect = { relation = it },
        )
    }
    when (kind) {
        "topic" -> PagedListContent(
            key = Triple(userId, relation, includeNsfw),
            load = { page, _ ->
                val result = app.userApi.topics(
                    userId, relation = relation, page = page, limit = 50, includeNsfw = includeNsfw,
                )
                PageResult(result.items, ended = result.items.size < 50)
            },
            emptyText = "这里还没有话题",
            modifier = Modifier.fillMaxSize(),
            header = headerContent,
            keyOf = { it.id },
        ) { topic ->
            SimpleContentRow(
                title = topic.title,
                subtitle = relativeTime(topic.createdAt),
                onClick = { onOpenTopic(topic.id) },
            )
        }
        "reply" -> PagedListContent(
            key = Triple(userId, "reply-$relation", includeNsfw),
            load = { page, _ ->
                val result = app.userApi.replies(
                    userId, relation = relation, page = page, limit = 50, includeNsfw = includeNsfw,
                )
                PageResult(result.items, ended = result.items.size < 50)
            },
            emptyText = "这里还没有回复",
            modifier = Modifier.fillMaxSize(),
            header = headerContent,
            keyOf = { it.id },
        ) { reply ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTopic(reply.topicId) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    reply.excerpt.ifBlank { "回复 #${reply.floor}" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${reply.floor} · ${reply.topicTitle} · ${relativeTime(reply.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        else -> PagedListContent(
            key = Triple(userId, "comment-$relation", includeNsfw),
            load = { page, _ ->
                val result = app.userApi.comments(
                    userId, relation = relation, page = page, limit = 50, includeNsfw = includeNsfw,
                )
                PageResult(result.items, ended = result.items.size < 50)
            },
            emptyText = "这里还没有$kindLabel",
            modifier = Modifier.fillMaxSize(),
            header = headerContent,
            keyOf = { it.id },
        ) { comment ->
            SimpleContentRow(
                title = comment.excerpt,
                subtitle = "${comment.topicTitle} · ${relativeTime(comment.createdAt)}",
                onClick = { onOpenTopic(comment.topicId) },
            )
        }
    }
}

// ---------- Galgame 作品 ----------

@Composable
fun WorksTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var relation by remember(userId) { mutableStateOf("published") }
    PagedListContent(
        key = Triple(userId, relation, includeNsfw),
        load = { page, _ ->
            val result = app.userApi.works(
                userId, relation = relation, page = page, limit = 24, includeNsfw = includeNsfw,
            )
            PageResult(result.items, ended = result.items.size < 24)
        },
        emptyText = "这里还没有作品",
        modifier = Modifier.fillMaxSize(),
        header = {
            header()
            FilterChips(
                options = listOf("published" to "发布", "contributed" to "贡献", "liked" to "点赞"),
                selected = relation,
                onSelect = { relation = it },
            )
        },
        keyOf = { it.id },
    ) { work ->
        WorkRow(
            work = work,
            blurNsfw = blurNsfw,
            onClick = { onOpenWeb(siteUrl("/galgame/${work.id}")) },
        )
    }
}

@Composable
private fun WorkRow(work: WorkSummary, blurNsfw: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCoverImage(
            url = work.cover?.url,
            isNsfw = work.isNsfw,
            blurNsfw = blurNsfw,
            modifier = Modifier.size(48.dp, 66.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                work.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            work.maker?.displayName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                work.ratingScore?.let { Chip("%.1f 分".format(it)) }
                work.releaseDate?.take(7)?.let { Chip(it) }
                if (work.isNsfw) Chip("NSFW")
            }
        }
    }
}

// ---------- 评分 ----------

@Composable
fun RatingsTab(
    app: KungalApp,
    userId: String,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    PagedListContent(
        key = Pair(userId, includeNsfw),
        load = { page, _ ->
            val result = app.userApi.ratings(userId, page = page, limit = 24, includeNsfw = includeNsfw)
            PageResult(result.items, ended = result.items.size < 24)
        },
        emptyText = "TA 还没有发布评分",
        modifier = Modifier.fillMaxSize(),
        header = header,
        keyOf = { it.id },
    ) { rating ->
        RatingRow(
            rating = rating,
            blurNsfw = blurNsfw,
            onClick = { onOpenWeb(siteUrl("/galgame-rating/${rating.id}")) },
        )
    }
}

@Composable
private fun RatingRow(rating: RatingSummary, blurNsfw: Boolean, onClick: () -> Unit) {
    val workName = rating.work?.displayName ?: rating.workSummary?.displayName ?: "作品 #${rating.work?.id.orEmpty()}"
    val cover = rating.work?.cover?.url ?: rating.workSummary?.cover?.url
    val isNsfw = rating.work?.isNsfw == true || rating.workSummary?.isNsfw == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCoverImage(
            url = cover,
            isNsfw = isNsfw,
            blurNsfw = blurNsfw,
            modifier = Modifier.size(44.dp, 60.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    workName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${rating.overall} 分",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(ratingRecommendLabel(rating.recommend))
                rating.playStatus?.let { Chip(playStateLabel(it)) }
            }
            if (rating.shortSummary.isNotBlank()) {
                Text(
                    rating.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------- 资源 ----------

@Composable
fun ResourcesTab(
    app: KungalApp,
    userId: String,
    isSelf: Boolean,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    initialState: String = "valid",
    onOpenResource: (String) -> Unit,
) {
    var relation by remember(userId) { mutableStateOf("published") }
    var state by remember(userId, initialState) { mutableStateOf(initialState) }
    var reload by remember(userId) { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<GalgameResourceSummary?>(null) }
    var confirmValid by remember { mutableStateOf<GalgameResourceSummary?>(null) }
    var savingValid by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canManage = isSelf && relation == "published"

    fun refresh() {
        reload += 1
    }

    fun markValid(resource: GalgameResourceSummary) {
        if (savingValid) return
        savingValid = true
        scope.launch {
            runCatching {
                app.resourceApi.patch(
                    resource.id,
                    kotlinx.serialization.json.buildJsonObject {
                        put("state", kotlinx.serialization.json.JsonPrimitive("valid"))
                    },
                )
            }.onSuccess {
                AppMessages.show("已重新标记为有效")
                refresh()
            }.onFailure { AppMessages.show(friendlyError(it)) }
            savingValid = false
        }
    }

    PagedListContent(
        key = listOf(userId, relation, state, includeNsfw, reload),
        load = { page, _ ->
            val result = app.userApi.resources(
                userId,
                relation = relation,
                state = if (relation == "published") state else null,
                page = page,
                limit = 50,
                includeNsfw = includeNsfw,
            )
            PageResult(result.items, ended = result.items.size < 50)
        },
        emptyText = "这里还没有资源",
        modifier = Modifier.fillMaxSize(),
        header = {
            header()
            FilterChips(
                options = listOf("published" to "发布", "liked" to "点赞"),
                selected = relation,
                onSelect = { relation = it },
            )
            if (relation == "published") {
                FilterChips(
                    options = listOf("valid" to "有效", "expired" to "过期"),
                    selected = state,
                    onSelect = { state = it },
                )
            }
        },
        keyOf = { it.id },
    ) { resource ->
        ResourceRow(
            resource = resource,
            blurNsfw = blurNsfw,
            onClick = { onOpenResource(resource.id) },
            actions = if (canManage) {
                {
                    if (resource.state == "expired") {
                        TextButton(
                            onClick = { confirmValid = resource },
                            enabled = !savingValid,
                        ) { Text("标记为有效", style = MaterialTheme.typography.labelMedium) }
                    }
                    TextButton(onClick = { editing = resource }) {
                        Text(
                            if (state == "expired") "更改链接并标记为有效" else "更改链接",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            } else {
                null
            },
        )
    }

    editing?.let { resource ->
        ResourceEditorSheet(
            app = app,
            workId = resource.work?.id.orEmpty(),
            resourceId = resource.id,
            onDismiss = { editing = null },
            onSaved = {
                editing = null
                // 失效列表里改完链接按网页行为顺带标记有效
                if (state == "expired") markValid(resource) else refresh()
            },
        )
    }

    confirmValid?.let { resource ->
        AlertDialog(
            onDismissRequest = { confirmValid = null },
            title = { Text("重新标记有效") },
            text = { Text("资源链接修复后，或核实过链接依然可用时，可以重新标记为有效。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmValid = null
                    markValid(resource)
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { confirmValid = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ResourceRow(
    resource: GalgameResourceSummary,
    blurNsfw: Boolean,
    onClick: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkCoverImage(
                url = resource.work?.cover?.url,
                isNsfw = resource.work?.isNsfw == true,
                blurNsfw = blurNsfw,
                modifier = Modifier.size(44.dp, 60.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    resource.title.ifBlank { resource.work?.displayName ?: "资源 #${resource.id}" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (resource.size.isNotBlank()) Chip(resource.size)
                    if (resource.state == "expired") Chip("已过期")
                    resource.resourcePlatforms.take(2).forEach { Chip(PlatformLabels[it] ?: it) }
                }
                Text(
                    "下载 ${resource.downloadCount} · 点赞 ${resource.likeCount} · ${relativeTime(resource.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actions != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                actions()
            }
        }
    }
}

// ---------- 题库 ----------

@Composable
fun QuizTab(
    app: KungalApp,
    userId: String,
    isSelf: Boolean,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var mode by remember(userId) { mutableStateOf("authored") }
    PagedListContent(
        key = Triple(userId, mode, isSelf),
        load = { page, _ ->
            val result = if (mode == "answered" && isSelf) {
                app.userApi.answeredQuizzes(page = page)
            } else {
                app.userApi.quizzes(userId, page = page)
            }
            PageResult(result.items, ended = result.items.size < 50)
        },
        emptyText = if (mode == "answered") "还没有答题记录" else "TA 还没有出过题",
        modifier = Modifier.fillMaxSize(),
        header = {
            header()
            if (isSelf) {
                FilterChips(
                    options = listOf("authored" to "出题", "answered" to "答题"),
                    selected = mode,
                    onSelect = { mode = it },
                )
            }
        },
        keyOf = { it.id },
    ) { quiz -> QuizRow(quiz, onClick = { onOpenWeb(siteUrl("/galgame-quiz/${quiz.id}")) }) }
}

@Composable
private fun QuizRow(quiz: QuizSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            documentPlainText(quiz.prompt).ifBlank { "题目 #${quiz.id}" },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(QuizTypeLabels[quiz.quizType] ?: quiz.quizType)
            Chip(QuizCategoryLabels[quiz.quizCategory] ?: quiz.quizCategory)
            Chip("难度 ${quiz.difficulty}")
        }
        Text(
            "${quiz.answerCount} 人作答 · 正确 ${quiz.correctCount} · ${relativeTime(quiz.createdAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 工具集 ----------

@Composable
fun ToolsetTab(
    app: KungalApp,
    userId: String,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var relation by remember(userId) { mutableStateOf("authored") }
    PagedListContent(
        key = Pair(userId, relation),
        load = { page, _ ->
            val result = app.userApi.toolsets(userId, relation = relation, page = page)
            PageResult(result.items, ended = result.items.size < 24)
        },
        emptyText = "这里还没有工具集",
        modifier = Modifier.fillMaxSize(),
        header = {
            header()
            FilterChips(
                options = listOf("authored" to "发布", "favorited" to "收藏"),
                selected = relation,
                onSelect = { relation = it },
            )
        },
        keyOf = { it.id },
    ) { toolset -> ToolsetRow(toolset, onClick = { onOpenWeb(siteUrl("/toolset/${toolset.id}")) }) }
}

@Composable
private fun ToolsetRow(toolset: ToolsetSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            toolset.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(PlatformLabels[toolset.platform] ?: toolset.platform)
            Chip(ToolsetTypeLabels[toolset.toolsetType] ?: toolset.toolsetType)
            toolset.practicalityAverage?.let { Chip("实用度 %.1f".format(it)) }
        }
        Text(
            "下载 ${toolset.downloadCount} · 浏览 ${toolset.viewCount} · ${relativeTime(toolset.updatedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 游玩时长（仅本人） ----------

@Composable
fun PlaytimeTab(
    app: KungalApp,
    header: @Composable () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var items by remember { mutableStateOf<List<WorkPlaytime>>(emptyList()) }
    var totalMinutes by remember { mutableStateOf(0) }
    var totalWorks by remember { mutableStateOf(0) }
    var finishedCount by remember { mutableStateOf(0) }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching { app.userApi.playtimes() }
            .onSuccess { page ->
                items = page.items
                cursor = page.nextCursor
                totalMinutes = page.totalMinutes
                totalWorks = page.total
                finishedCount = page.finishedWorkCount
                loading = false
            }
            .onFailure {
                error = friendlyError(it)
                loading = false
            }
    }

    when {
        loading && items.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") { header() }
            item(key = "loading") { LoadingBox() }
        }
        error != null && items.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") { header() }
            item(key = "error") { ErrorBox(error!!, onRetry = { reload += 1 }) }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") { header() }
            item(key = "stats") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        PlaytimeStat("总时长", formatMinutes(totalMinutes), Modifier.weight(1f))
                        PlaytimeStat("作品数", totalWorks.toString(), Modifier.weight(1f))
                        PlaytimeStat("通关", finishedCount.toString(), Modifier.weight(1f))
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyBox("还没有游玩记录。在网页端或桌面端上报时长后会显示在这里。")
                }
            }
            items(items, key = { it.workSummary.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWeb(siteUrl("/galgame/${item.workSummary.id}")) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KungalImage(
                        url = item.workSummary.cover?.url,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp, 60.dp).clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.workSummary.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${formatMinutes(item.minutes)} · ${playStateLabel(item.playState)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (cursor != null) {
                item {
                    TextButton(
                        onClick = {
                            val next = cursor ?: return@TextButton
                            loading = true
                            // 追加一页；失败时保留已有数据并提示
                            scope.launch {
                                runCatching { app.userApi.playtimes(cursor = next) }
                                    .onSuccess { page ->
                                        items = items + page.items
                                        cursor = page.nextCursor
                                        totalMinutes = page.totalMinutes
                                        totalWorks = page.total
                                        finishedCount = page.finishedWorkCount
                                        loading = false
                                    }
                                    .onFailure {
                                        error = friendlyError(it)
                                        loading = false
                                    }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (loading) "加载中…" else "加载更多") }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlaytimeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 分钟 → 「x 小时 y 分钟」。 */
fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0 分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest 分钟"
        rest == 0 -> "$hours 小时"
        else -> "$hours 小时 $rest 分钟"
    }
}

/** 作品封面；NSFW 且设置为「模糊」时打码，点击可揭示。 */
@Composable
private fun WorkCoverImage(
    url: String?,
    isNsfw: Boolean,
    blurNsfw: Boolean,
    modifier: Modifier,
) {
    if (isNsfw && blurNsfw) {
        Box(modifier) { NsfwCoverImage(url = url, modifier = Modifier.fillMaxSize()) }
    } else {
        KungalImage(url = url, contentDescription = null, modifier = modifier)
    }
}
