package com.kungal.app.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.galgame.WorkCoverImage
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.TopicRankingEntry
import com.kungal.core.model.UserRankingEntry
import com.kungal.core.model.WorkRankingEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** 排行榜三个分栏（与网页 /ranking/{user,topic,galgame} 对应）。 */
private val rankingTabs = listOf(
    "user" to "用户",
    "topic" to "话题",
    "work" to "Galgame",
)

private data class SortOption(val key: String, val label: String)

private val userSorts = listOf(
    SortOption("moemoepoint_desc", "萌萌点"),
    SortOption("topics_desc", "话题数"),
    SortOption("replies_desc", "回复数"),
    SortOption("comments_desc", "评论数"),
    SortOption("resources_desc", "Galgame 资源"),
)

private val topicSorts = listOf(
    SortOption("views_desc", "浏览数"),
    SortOption("replies_desc", "回复数"),
    SortOption("comments_desc", "评论数"),
    SortOption("likes_desc", "点赞数"),
    SortOption("upvotes_desc", "被推数"),
    SortOption("favorites_desc", "收藏数"),
)

private val workSorts = listOf(
    SortOption("views_desc", "浏览数"),
    SortOption("likes_desc", "点赞数"),
    SortOption("favorites_desc", "收藏数"),
    SortOption("resources_desc", "资源数"),
    SortOption("rating_desc", "评分"),
)

class RankingViewModel(
    private val app: KungalApp,
    initialTab: String,
    includeNsfw: Boolean,
) : ViewModel() {

    data class State(
        val tab: String = "user",
        val userSort: String = "moemoepoint_desc",
        val topicSort: String = "views_desc",
        val workSort: String = "views_desc",
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val error: String? = null,
        val users: List<UserRankingEntry> = emptyList(),
        val topics: List<TopicRankingEntry> = emptyList(),
        val works: List<WorkRankingEntry> = emptyList(),
    )

    private val _state = MutableStateFlow(State(tab = initialTab))
    val state: StateFlow<State> = _state.asStateFlow()

    private var nsfw = includeNsfw

    init {
        load()
    }

    fun setIncludeNsfw(value: Boolean) {
        if (value == nsfw) return
        nsfw = value
        if (_state.value.tab == "topic" || _state.value.tab == "work") load()
    }

    fun selectTab(tab: String) {
        if (tab == _state.value.tab) return
        _state.update { it.copy(tab = tab) }
        load()
    }

    fun selectSort(sort: String) {
        when (_state.value.tab) {
            "user" -> if (sort == _state.value.userSort) return else _state.update { it.copy(userSort = sort) }
            "topic" -> if (sort == _state.value.topicSort) return else _state.update { it.copy(topicSort = sort) }
            else -> if (sort == _state.value.workSort) return else _state.update { it.copy(workSort = sort) }
        }
        load()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load(keepList = true)
    }

    private fun load(keepList: Boolean = false) {
        _state.update {
            it.copy(
                loading = !keepList,
                error = null,
                users = if (keepList) it.users else emptyList(),
                topics = if (keepList) it.topics else emptyList(),
                works = if (keepList) it.works else emptyList(),
            )
        }
        viewModelScope.launch {
            val snapshot = _state.value
            runCatching {
                when (snapshot.tab) {
                    "user" -> app.rankingApi.users(sort = snapshot.userSort).items
                        .let { _state.update { s -> s.copy(users = it) } }
                    "topic" -> app.rankingApi.topics(
                        sort = snapshot.topicSort,
                        includeNsfw = nsfw,
                    ).items.let { _state.update { s -> s.copy(topics = it) } }
                    else -> app.rankingApi.works(
                        sort = snapshot.workSort,
                        includeNsfw = nsfw,
                    ).items.let { _state.update { s -> s.copy(works = it) } }
                }
            }.onSuccess {
                _state.update { it.copy(loading = false, refreshing = false, error = null) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = if (keepList && it.hasContent()) null else friendlyError(e),
                    )
                }
            }
        }
    }

    private fun State.hasContent(): Boolean =
        users.isNotEmpty() || topics.isNotEmpty() || works.isNotEmpty()
}

private fun RankingViewModel.State.hasContent(): Boolean =
    users.isNotEmpty() || topics.isNotEmpty() || works.isNotEmpty()

/** 排行榜：用户（萌萌点/话题/回复/评论/资源）、话题（浏览/回复/评论/赞/推/收藏）、Galgame 作品。 */
@Composable
fun RankingScreen(
    app: KungalApp,
    initialTab: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val vm: RankingViewModel = viewModel(key = "ranking") {
        RankingViewModel(app, initialTab, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    val current = rankingTabs.firstOrNull { it.first == state.tab } ?: rankingTabs.first()
    val sorts = when (state.tab) {
        "user" -> userSorts
        "topic" -> topicSorts
        else -> workSorts
    }
    val selectedSort = when (state.tab) {
        "user" -> state.userSort
        "topic" -> state.topicSort
        else -> state.workSort
    }

    LaunchedEffect(nsfwDisplay) { vm.setIncludeNsfw(nsfwDisplay.allowsNsfw) }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "排行榜",
                subtitle = when (state.tab) {
                    "user" -> "萌萌点、话题、回复、评论与资源"
                    "topic" -> "话题的浏览、互动与收藏"
                    else -> "Galgame 作品人气与评分"
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                FilterChips(
                    options = rankingTabs,
                    selected = current.first,
                    onSelect = vm::selectTab,
                )
                FilterChips(
                    options = sorts.map { it.key to it.label },
                    selected = selectedSort,
                    onSelect = vm::selectSort,
                )
                when {
                    state.loading -> LoadingBox(Modifier.padding(vertical = 24.dp))
                    state.error != null && !state.hasContent() -> ErrorBox(
                        message = state.error!!,
                        onRetry = vm::refresh,
                    )
                    !state.hasContent() -> EmptyBox("排行榜暂时没有数据")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when (state.tab) {
                            "user" -> items(state.users, key = { "u-${it.member.id}" }) { entry ->
                                UserRankingRow(
                                    entry = entry,
                                    onClick = {
                                        if (entry.member.id.isNotBlank()) onOpenUser(entry.member.id)
                                    },
                                )
                            }
                            "topic" -> items(
                                state.topics.filter { it.topic != null },
                                key = { "t-${it.topic!!.id}" },
                            ) { entry ->
                                TopicRankingRow(
                                    entry = entry,
                                    onClick = { entry.topic?.let { onOpenTopic(it.id) } },
                                )
                            }
                            else -> items(
                                state.works.filter { it.work != null },
                                key = { "w-${it.work!!.id}" },
                            ) { entry ->
                                WorkRankingRow(
                                    entry = entry,
                                    blurNsfw = nsfwDisplay.blursMedia,
                                    onClick = {
                                        entry.work?.let {
                                            onOpenWeb("https://www.kungal.com/galgame/${it.id}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val color = when (rank) {
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        3 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun UserRankingRow(entry: UserRankingEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(entry.rank)
        Spacer(Modifier.width(10.dp))
        AvatarImage(url = entry.member.avatar?.url, modifier = Modifier.size(42.dp), seed = entry.member.name)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.member.name ?: "已注销用户",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.bio?.takeIf { it.isNotBlank() } ?: "这只笨蛋萝莉居然不写签名! 八嘎!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            metricText(entry.metricValue),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TopicRankingRow(entry: TopicRankingEntry, onClick: () -> Unit) {
    val topic = entry.topic ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(entry.rank)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                topic.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(url = topic.author.avatar?.url, modifier = Modifier.size(18.dp), seed = topic.author.name)
                Spacer(Modifier.width(5.dp))
                Text(
                    topic.author.name ?: "已注销用户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    metricText(entry.metricValue),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WorkRankingRow(
    entry: WorkRankingEntry,
    blurNsfw: Boolean,
    onClick: () -> Unit,
) {
    val work = entry.work ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(entry.rank)
        Spacer(Modifier.width(10.dp))
        WorkCoverImage(
            url = work.cover?.url,
            isNsfw = work.isNsfw,
            blurNsfw = blurNsfw,
            contentDescription = work.displayName,
            modifier = Modifier.width(40.dp).height(56.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                localizedName(work.displayName, work.localized["zh-Hans"]?.value),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.creator?.name ?: "暂无创建者",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            metricText(entry.metricValue),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 优先中文名；没有就用自己的显示名（与网页默认的 catalogNameText 一致）。 */
private fun localizedName(display: String, zh: String?): String =
    zh?.takeIf { it.isNotBlank() } ?: display

private fun metricText(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
