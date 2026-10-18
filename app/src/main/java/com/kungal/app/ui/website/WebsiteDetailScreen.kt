package com.kungal.app.ui.website

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.galgame.WallCommentSection
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.fullDateTime
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.WebsiteDetail
import com.kungal.core.model.WebsiteEntry
import com.kungal.core.model.WebsiteTag
import com.kungal.core.model.WebsiteTagGroup
import com.kungal.core.model.WebsiteViewer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val WEBSITE_COMMENT_MAX = 1007

private val WebsiteGroupPalette = listOf(
    Color(0xFF3B82F6),
    Color(0xFF22C55E),
    Color(0xFFA855F7),
    Color(0xFF06B6D4),
    Color(0xFF94A3B8),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
)

/**
 * 网站详情（对应网页 /website/{host}）：站点信息、标签与价值精算、点赞/收藏、
 * 访问外链、统计与评论墙。注意详情接口每次读取都会让站点访问量 +1（网页同样）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebsiteDetailScreen(
    app: KungalApp,
    host: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenTag: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var site by remember(host) { mutableStateOf<WebsiteDetail?>(null) }
    var allTags by remember { mutableStateOf<List<WebsiteTag>>(emptyList()) }
    var groups by remember { mutableStateOf<List<WebsiteTagGroup>>(emptyList()) }
    var loading by remember(host) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(host) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var showScoreDetail by remember { mutableStateOf(false) }
    var likePending by remember { mutableStateOf(false) }
    var favoritePending by remember { mutableStateOf(false) }

    LaunchedEffect(host, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching {
            coroutineScope {
                val detail = async { app.websiteApi.detail(host) }
                val tags = async { runCatching { app.websiteApi.tags() }.getOrDefault(emptyList()) }
                val tagGroups = async { runCatching { app.websiteApi.tagGroups() }.getOrDefault(emptyList()) }
                Triple(detail.await(), tags.await(), tagGroups.await())
            }
        }.onSuccess { (detail, tags, tagGroups) ->
            site = detail
            allTags = tags
            groups = tagGroups
        }.onFailure {
            // 下拉刷新失败时保留原内容，只提示；首次加载失败才整页报错。
            if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
        }
        loading = false
        refreshing = false
    }

    fun toggleLike() {
        val current = site ?: return
        if (likePending) return
        if (!app.isLoggedIn) {
            onOpenLogin()
            return
        }
        likePending = true
        val next = !(current.viewer?.hasLiked ?: false)
        scope.launch {
            runCatching { app.websiteApi.setLike(current.host, next) }
                .onSuccess { engagement ->
                    site = current.copy(
                        likeCount = engagement.likeCount,
                        favoriteCount = engagement.favoriteCount,
                        viewer = current.viewer?.copy(
                            hasLiked = engagement.viewer?.hasLiked ?: next,
                            hasFavorited = engagement.viewer?.hasFavorited
                                ?: current.viewer?.hasFavorited ?: false,
                        ) ?: WebsiteViewer(
                            hasLiked = engagement.viewer?.hasLiked ?: next,
                            hasFavorited = engagement.viewer?.hasFavorited ?: false,
                        ),
                    )
                    AppMessages.show(if (next) "点赞网站成功!" else "取消点赞成功!")
                }
                .onFailure { AppMessages.show(friendlyError(it)) }
            likePending = false
        }
    }

    fun toggleFavorite() {
        val current = site ?: return
        if (favoritePending) return
        if (!app.isLoggedIn) {
            onOpenLogin()
            return
        }
        favoritePending = true
        val next = !(current.viewer?.hasFavorited ?: false)
        scope.launch {
            runCatching { app.websiteApi.setFavorite(current.host, next) }
                .onSuccess { engagement ->
                    site = current.copy(
                        likeCount = engagement.likeCount,
                        favoriteCount = engagement.favoriteCount,
                        viewer = current.viewer?.copy(
                            hasLiked = engagement.viewer?.hasLiked ?: current.viewer?.hasLiked ?: false,
                            hasFavorited = engagement.viewer?.hasFavorited ?: next,
                        ) ?: WebsiteViewer(
                            hasLiked = engagement.viewer?.hasLiked ?: false,
                            hasFavorited = engagement.viewer?.hasFavorited ?: next,
                        ),
                    )
                    AppMessages.show(if (next) "收藏网站成功!" else "取消收藏成功!")
                }
                .onFailure { AppMessages.show(friendlyError(it)) }
            favoritePending = false
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "网站详情",
                subtitle = site?.host?.takeIf { it.isNotBlank() } ?: host,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null -> ErrorBox(
                error!!,
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            site == null -> EmptyBox(
                "未找到这个网站",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val current = site!!
                KungalPullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = {
                        if (!refreshing) {
                            refreshing = true
                            reload++
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WebsiteHeaderCard(
                            site = current,
                            allTags = allTags,
                            groups = groups,
                            showScoreDetail = showScoreDetail,
                            onToggleScoreDetail = { showScoreDetail = !showScoreDetail },
                            likePending = likePending,
                            favoritePending = favoritePending,
                            onToggleLike = ::toggleLike,
                            onToggleFavorite = ::toggleFavorite,
                            onVisit = { AndroidActions.openInBrowser(context, websiteVisitUrl(current.host)) },
                            onOpenTag = onOpenTag,
                        )
                        WebsiteInfoCard(
                            site = current,
                            onOpenCategory = onOpenCategory,
                            onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                            onCopyUrl = { url -> AndroidActions.copyText(context, "网站地址", url) },
                        )
                        WebsiteStatsCard(site = current)
                        WallCommentSection(
                            app = app,
                            subjectType = "website",
                            subjectId = current.id,
                            maxLength = WEBSITE_COMMENT_MAX,
                            title = "用户评论",
                            onOpenUser = onOpenUser,
                            onOpenLogin = onOpenLogin,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebsiteHeaderCard(
    site: WebsiteDetail,
    allTags: List<WebsiteTag>,
    groups: List<WebsiteTagGroup>,
    showScoreDetail: Boolean,
    onToggleScoreDetail: () -> Unit,
    likePending: Boolean,
    favoritePending: Boolean,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVisit: () -> Unit,
    onOpenTag: (String) -> Unit,
) {
    val entryLike = WebsiteEntry(
        id = site.id,
        host = site.host,
        title = site.title,
        description = site.description,
        icon = site.icon,
        externalIconUrl = site.externalIconUrl,
        websiteCategory = site.websiteCategory,
        isNsfw = site.isNsfw,
        state = site.state,
        score = site.score,
    )
    DetailSection {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            WebsiteIcon(site = entryLike, modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    site.title.ifBlank { site.host },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${site.viewCount} 次访问 · 更新于 ${relativeTime(site.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    websiteStateBadge(site.state)?.let { Chip(it) }
                    Chip(websiteLanguageLabel(site.language))
                    if (site.isNsfw) Chip("NSFW")
                }
            }
        }

        Text(
            site.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (site.websiteTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                site.websiteTags.forEach { tag ->
                    WebsiteTagChip(tag = tag, onClick = { onOpenTag(tag.slug) })
                }
            }
        }

        WebsiteScoreSection(
            tags = site.websiteTags,
            allTags = allTags,
            groups = groups,
            showDetail = showScoreDetail,
            onToggleDetail = onToggleScoreDetail,
            onOpenTag = onOpenTag,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onToggleLike, enabled = !likePending) {
                val liked = site.viewer?.hasLiked == true
                Icon(
                    if (liked) Icons.Outlined.ThumbUp else Icons.Outlined.ThumbUpOffAlt,
                    contentDescription = "点赞",
                    modifier = Modifier.size(18.dp),
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text("${site.likeCount}")
            }
            TextButton(onClick = onToggleFavorite, enabled = !favoritePending) {
                val favorited = site.viewer?.hasFavorited == true
                Icon(
                    if (favorited) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "收藏",
                    modifier = Modifier.size(18.dp),
                    tint = if (favorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text("${site.favoriteCount}")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onVisit) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("访问网站")
            }
        }
    }
}

@Composable
private fun DetailSection(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** 标签胶囊：颜色按价值分档（网页 WebsiteTag.vue），点击进标签页。 */
@Composable
fun WebsiteTagChip(tag: WebsiteTag, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val (fg, bg) = when (websiteTagTone(tag)) {
        WebsiteTagTone.LEGENDARY -> Color(0xFFB45309) to Color(0xFFF59E0B).copy(alpha = 0.18f)
        WebsiteTagTone.HIGH -> scheme.primary to scheme.primary.copy(alpha = 0.12f)
        WebsiteTagTone.MEDIUM -> scheme.secondary to scheme.secondary.copy(alpha = 0.16f)
        WebsiteTagTone.NEUTRAL -> Color(0xFF16A34A) to Color(0xFF16A34A).copy(alpha = 0.12f)
        WebsiteTagTone.NEGATIVE -> scheme.error to scheme.error.copy(alpha = 0.12f)
        WebsiteTagTone.LOW -> scheme.onSurfaceVariant to scheme.surfaceVariant
    }
    Text(
        "${tag.label.ifBlank { tag.slug }} ${if (tag.level >= 0) "+" else ""}${tag.level}",
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * 「网站价值精算值」：总分 / 理论最高分、分组占比条、图例与可展开的分组明细。
 * 分组明细里每个标签显示价值条（网页 TagVisualization）。
 */
@Composable
private fun WebsiteScoreSection(
    tags: List<WebsiteTag>,
    allTags: List<WebsiteTag>,
    groups: List<WebsiteTagGroup>,
    showDetail: Boolean,
    onToggleDetail: () -> Unit,
    onOpenTag: (String) -> Unit,
) {
    val maxScore = websiteMaxScore(allTags.ifEmpty { tags }, groups)
    val stats = websiteTagGroupStats(tags, groups)
    val totalScore = tags.sumOf { it.level }
    val scoreColor = when (websiteScoreTone(totalScore)) {
        WebsiteScoreTone.TOP -> Color(0xFFF59E0B)
        WebsiteScoreTone.GOOD -> Color(0xFF16A34A)
        WebsiteScoreTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        WebsiteScoreTone.BAD -> MaterialTheme.colorScheme.error
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "网站价值精算值",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$totalScore / $maxScore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor,
            )
        }
        // 分组占比条：按各组分数绝对值分配宽度，正分才填充总进度。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (totalScore > 0) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                    },
                ),
        ) {
            val visible = stats.filter { it.score != 0 }
            visible.forEachIndexed { index, stat ->
                Box(
                    Modifier
                        .weight(kotlin.math.abs(stat.score).toFloat())
                        .fillMaxSize()
                        .background(WebsiteGroupPalette[index % WebsiteGroupPalette.size].copy(alpha = 0.45f)),
                )
            }
        }
        if (stats.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                stats.forEachIndexed { index, stat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(50))
                                .background(WebsiteGroupPalette[index % WebsiteGroupPalette.size].copy(alpha = 0.55f)),
                        )
                        Text(
                            "${stat.label} (${stat.score})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            "评分基于广告程度、资源数量、更新频率等指标，正分表示优质特性，负分表示存在问题。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (stats.isNotEmpty()) {
            TextButton(
                onClick = onToggleDetail,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text(if (showDetail) "收起网站价值分析" else "展开网站价值分析详情")
            }
        }
        if (showDetail) {
            stats.forEach { stat ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stat.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stat.score.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stat.score >= 0) {
                                Color(0xFF16A34A)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    stat.tags.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenTag(tag.slug) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                tag.label.ifBlank { tag.slug },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier
                                    .width(72.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((kotlin.math.abs(tag.level).coerceAtMost(20) / 20f))
                                        .height(5.dp)
                                        .background(
                                            if (tag.level >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                        ),
                                )
                            }
                            Text(
                                "${if (tag.level >= 0) "+" else ""}${tag.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tag.level >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 网站信息卡：分类、运行状态、语言、年龄限制与域名列表。 */
@Composable
private fun WebsiteInfoCard(
    site: WebsiteDetail,
    onOpenCategory: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
) {
    DetailSection {
        Text(
            "网站信息",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        WebsiteInfoRow("分类") {
            Text(
                site.websiteCategory.label.ifBlank { site.websiteCategory.slug },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenCategory(site.websiteCategory.slug) },
            )
        }
        WebsiteInfoRow("运行状态") {
            Text(websiteStateLabel(site.state), style = MaterialTheme.typography.labelMedium)
        }
        WebsiteInfoRow("语言") {
            Text(websiteLanguageLabel(site.language), style = MaterialTheme.typography.labelMedium)
        }
        WebsiteInfoRow("年龄限制") {
            Text(
                if (site.isNsfw) "含有 R18 内容" else "页面默认不含 R18 内容",
                style = MaterialTheme.typography.labelMedium,
                color = if (site.isNsfw) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (site.urls.isNotEmpty()) {
            Text(
                "域名列表",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            site.urls.forEach { url ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        url,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenUrl(url) },
                    )
                    IconButton(onClick = { onCopyUrl(url) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 统计卡：访问量、点赞、收藏、建站时间与索引时间。 */
@Composable
private fun WebsiteStatsCard(site: WebsiteDetail) {
    DetailSection {
        Text(
            "统计信息",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        WebsiteInfoRow("总访问量") { Text("${site.viewCount}", style = MaterialTheme.typography.labelMedium) }
        WebsiteInfoRow("点赞数") { Text("${site.likeCount}", style = MaterialTheme.typography.labelMedium) }
        WebsiteInfoRow("收藏数") { Text("${site.favoriteCount}", style = MaterialTheme.typography.labelMedium) }
        if (site.founded.isNotBlank()) {
            WebsiteInfoRow("网站建立时间") { Text(site.founded, style = MaterialTheme.typography.labelMedium) }
        }
        WebsiteInfoRow("网站索引时间") {
            Text(fullDateTime(site.createdAt), style = MaterialTheme.typography.labelMedium)
        }
    }
}
