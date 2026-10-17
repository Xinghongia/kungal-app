package com.kungal.app.ui.galgame

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
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

private enum class RatingSheet { SORT, SPOILER, PLAY_STATE, GAME_TYPE }

/** 评分排序字段；方向由旁边的升/降序按钮控制，与资源资料库的筛选条一致。 */
private val RatingSortFields = listOf(
    FilterOption("created", "最新"),
    FilterOption("view", "浏览最多"),
    FilterOption("overall", "评分最高"),
)

@Composable
fun RatingListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenRating: (String) -> Unit,
) {
    var sortField by rememberSaveable { mutableStateOf("created") }
    var sortOrder by rememberSaveable { mutableStateOf("desc") }
    var spoiler by rememberSaveable { mutableStateOf<String?>(null) }
    var playStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var gameType by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<RatingSheet?>(null) }
    val includeNsfw = nsfwDisplay.allowsNsfw
    val sort = "${sortField}_$sortOrder"
    val hasFilter = spoiler != null || playStatus != null || gameType != null ||
        sortField != "created" || sortOrder != "desc"

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
                    FilterPill(
                        label = labelOf(RatingSortFields, sortField),
                        active = true,
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        onClick = { sheet = RatingSheet.SORT },
                    )
                    FilterPill(
                        label = if (sortOrder == "desc") "降序" else "升序",
                        active = false,
                        icon = {
                            Icon(
                                if (sortOrder == "desc") Icons.Outlined.ArrowDownward
                                else Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        onClick = { sortOrder = if (sortOrder == "desc") "asc" else "desc" },
                    )
                    FilterPill(
                        label = spoiler?.let(::ratingSpoilerLabel) ?: "剧透程度",
                        active = spoiler != null,
                        onClick = { sheet = RatingSheet.SPOILER },
                    )
                    FilterPill(
                        label = playStatus?.let(::playStateLabel) ?: "游玩状态",
                        active = playStatus != null,
                        onClick = { sheet = RatingSheet.PLAY_STATE },
                    )
                    FilterPill(
                        label = gameType?.let(::gameTypeLabel) ?: "游戏类型",
                        active = gameType != null,
                        onClick = { sheet = RatingSheet.GAME_TYPE },
                    )
                    if (hasFilter) {
                        FilterPill(
                            label = "清空",
                            active = false,
                            icon = {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            onClick = {
                                sortField = "created"
                                sortOrder = "desc"
                                spoiler = null
                                playStatus = null
                                gameType = null
                            },
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

    when (sheet) {
        RatingSheet.SORT -> FilterOptionSheet(
            title = "排序",
            options = RatingSortFields,
            selected = setOf(sortField),
            multi = false,
            includeAll = false,
            onToggle = { sortField = it },
            onClear = null,
            onDismiss = { sheet = null },
        )
        RatingSheet.SPOILER -> FilterOptionSheet(
            title = "剧透程度",
            options = RatingSpoilerLabels.map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(spoiler),
            multi = false,
            onToggle = { spoiler = it },
            onClear = { spoiler = null },
            onDismiss = { sheet = null },
        )
        RatingSheet.PLAY_STATE -> FilterOptionSheet(
            title = "游玩状态",
            options = RatingPlayStateLabels
                .filter { it.first != "done" }
                .map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(playStatus),
            multi = false,
            onToggle = { playStatus = it },
            onClear = { playStatus = null },
            onDismiss = { sheet = null },
        )
        RatingSheet.GAME_TYPE -> FilterOptionSheet(
            title = "游戏类型",
            options = RatingGameTypeLabels.map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(gameType),
            multi = false,
            onToggle = { gameType = it },
            onClear = { gameType = null },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}
