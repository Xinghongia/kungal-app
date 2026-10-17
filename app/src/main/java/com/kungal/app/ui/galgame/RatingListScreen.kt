package com.kungal.app.ui.galgame

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent

private val RatingSortOptions = listOf(
    FilterOption("created_desc", "最新"),
    FilterOption("created_asc", "最早"),
    FilterOption("view_desc", "浏览最多"),
    FilterOption("view_asc", "浏览最少"),
    FilterOption("overall_desc", "评分最高"),
    FilterOption("overall_asc", "评分最低"),
)

@Composable
fun RatingListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenRating: (String) -> Unit,
) {
    var sort by rememberSaveable { mutableStateOf("created_desc") }
    var spoiler by rememberSaveable { mutableStateOf<String?>(null) }
    var playStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var gameType by rememberSaveable { mutableStateOf<String?>(null) }
    val includeNsfw = nsfwDisplay.allowsNsfw
    val hasFilter = spoiler != null || playStatus != null || gameType != null || sort != "created_desc"

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 评分",
                subtitle = "按评分、浏览与推荐程度浏览玩家评价",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        PagedListContent(
            key = listOf("ratings", sort, spoiler, playStatus, gameType, includeNsfw),
            load = { page, _ ->
                val result = app.ratingApi.list(
                    page = page,
                    limit = 24,
                    sort = sort,
                    spoilerLevel = spoiler,
                    playStatus = playStatus,
                    gameType = gameType,
                    includeNsfw = includeNsfw,
                )
                PageResult(result.items, ended = result.items.size < 24 || page * 24 >= result.total)
            },
            emptyText = "没有找到符合条件的评分",
            modifier = Modifier.fillMaxSize().padding(padding),
            keyOf = { it.id },
            header = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EntityFilterMenu(
                        label = labelOf(RatingSortOptions, sort),
                        options = RatingSortOptions,
                        selected = sort,
                        onSelect = { value -> value?.let { sort = it } },
                        allLabel = "最新",
                    )
                    EntityFilterMenu(
                        label = spoiler?.let { ratingSpoilerLabel(it) } ?: "剧透程度",
                        options = RatingSpoilerLabels.map { FilterOption(it.first, it.second) },
                        selected = spoiler,
                        onSelect = { spoiler = it },
                        allLabel = "全部剧透程度",
                    )
                    EntityFilterMenu(
                        label = playStatus?.let { playStateLabel(it) } ?: "游玩状态",
                        options = RatingPlayStateLabels
                            .filter { it.first != "done" }
                            .map { FilterOption(it.first, it.second) },
                        selected = playStatus,
                        onSelect = { playStatus = it },
                        allLabel = "全部游玩状态",
                    )
                    EntityFilterMenu(
                        label = gameType?.let { gameTypeLabel(it) } ?: "游戏类型",
                        options = RatingGameTypeLabels.map { FilterOption(it.first, it.second) },
                        selected = gameType,
                        onSelect = { gameType = it },
                        allLabel = "全部游戏类型",
                    )
                    if (hasFilter) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                sort = "created_desc"
                                spoiler = null
                                playStatus = null
                                gameType = null
                            },
                            label = { Text("清空") },
                        )
                    }
                }
            },
        ) { rating ->
            RatingRow(
                rating = rating,
                blurCover = nsfwDisplay.blursMedia,
                onClick = { onOpenRating(rating.id) },
            )
        }
    }
}
