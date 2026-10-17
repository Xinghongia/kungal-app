package com.kungal.app.ui.points

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.MoemoepointEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PointsViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val balance: Int = 0,
        val items: List<MoemoepointEntry> = emptyList(),
        val nextCursor: String? = null,
        val loadingMore: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val me = async { app.meApi.me() }
                    val page = async { app.meApi.moemoepointEntries(limit = 30) }
                    me.await().moemoepoint to page.await()
                }
            }.onSuccess { (balance, page) ->
                _state.update {
                    it.copy(
                        loading = false,
                        balance = balance,
                        items = page.items,
                        nextCursor = page.nextCursor,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (current.loadingMore || current.loading) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching { app.meApi.moemoepointEntries(cursor = cursor, limit = 30) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loadingMore = false,
                            items = it.items + page.items,
                            nextCursor = page.nextCursor,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loadingMore = false, error = friendlyError(e)) }
                }
        }
    }
}

/** 萌萌点：余额、规则说明与收支流水（游标分页）。 */
@Composable
fun PointsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val vm: PointsViewModel = viewModel { PointsViewModel(app) }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "萌萌点",
                subtitle = "余额与收支流水",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && state.items.isEmpty() -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && state.items.isEmpty() -> ErrorBox(
                state.error!!,
                onRetry = vm::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    BalanceCard(balance = state.balance)
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("萌萌点规则", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "获取：每日签到、帖子被点赞/被推、优质内容奖励等。\n" +
                                    "消耗：推话题、改名、购买商店物品、下载收费资源等。\n" +
                                    "具体金额与规则以站点为准，流水记录与网页完全一致。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Text(
                        "收支流水",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 4.dp),
                    )
                }
                if (state.items.isEmpty()) {
                    item {
                        Text(
                            "还没有萌萌点记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                        )
                    }
                }
                items(state.items, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = {
                            entry.refPath?.takeIf { it.isNotBlank() }?.let {
                                onOpenWeb("https://www.kungal.com$it")
                            }
                        },
                    )
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            state.loadingMore -> LoadingBox()
                            state.nextCursor != null -> TextButton(onClick = vm::loadMore) { Text("加载更多") }
                            state.items.isNotEmpty() -> Text(
                                "没有更多了",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "当前萌萌点",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    balance.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: MoemoepointEntry, onClick: () -> Unit) {
    val positive = entry.delta >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (entry.refPath != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entryReasonLabel(entry), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (entry.source != "this_site") {
                    Chip(if (entry.source == "account_center") "账号中心" else "其他站点")
                }
                Text(
                    fullDateTime(entry.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = if (positive) "+${entry.delta}" else entry.delta.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

/** 站点把 reason 存成机器码，这里按网页 MoemoepointLog 的规则转成中文。 */
private val reasonLabels = mapOf(
    "daily_checkin" to "每日签到",
    "liked" to "内容被点赞",
    "content_approved" to "内容被采纳",
    "content_removed" to "内容被移除",
    "admin_grant" to "管理员发放",
    "admin_deduct" to "管理员扣除",
    "migration" to "初始迁移",
    "register_gift" to "注册礼物",
    "name_change" to "修改用户名",
)

private val refKindLabels = mapOf(
    "galgame" to "Galgame",
    "galgame_pr" to "Galgame 修订",
    "galgame_comment" to "游戏评论",
    "galgame_rating" to "游戏评分",
    "galgame_resource" to "游戏资源",
    "galgame_quiz" to "题目",
    "toolset" to "工具集",
    "toolset_resource" to "工具资源",
    "topic" to "话题",
    "topic_comment" to "话题评论",
    "topic_reply" to "回复",
)

private val behaviorLabels = mapOf(
    "content_approved:galgame_quiz_answer" to "答对了题目",
    "content_approved:topic_upvote" to "话题被推荐",
    "content_removed:topic_upvote" to "推话题消耗",
    "content_removed:topic_lottery_escrow" to "抽奖奖池托管",
    "content_approved:topic_lottery_escrow" to "抽奖奖池退回",
    "content_approved:topic_lottery" to "抽奖中奖",
)

private fun entryReasonLabel(entry: MoemoepointEntry): String {
    val kind = entry.ref.substringBefore(':')
    behaviorLabels["${entry.reason}:$kind"]?.let { return it }
    val kindLabel = refKindLabels[kind]
    if (kindLabel != null) {
        if (entry.reason == "content_approved") {
            return if (entry.delta < 0) "$kindLabel 被移除" else "创建了新的 $kindLabel"
        }
        if (entry.reason == "liked") {
            return "$kindLabel${if (entry.delta < 0) "取消点赞" else "被点赞"}"
        }
        if (entry.reason == "content_removed") {
            return "$kindLabel 被移除"
        }
    }
    return reasonLabels[entry.reason] ?: entry.reason.ifBlank { "萌萌点变动" }
}
