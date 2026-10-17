package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.EditProposalSummary
import com.kungal.core.model.proposalStateLabel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private val ProposalStates = listOf(
    "open" to "待审核",
    "merged" to "已合并",
    "declined" to "已拒绝",
    "withdrawn" to "已撤回",
)

private fun proposalTitle(item: EditProposalSummary): String {
    val name = item.workSummary.localized["zh-Hans"]?.value
        ?.takeIf { it.isNotBlank() }
        ?: item.workSummary.displayName
    return name.ifBlank { "Galgame #${item.workId}" }
}

// ---------- 我的提案 ----------

@Composable
fun MyProposalsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenProposal: (String) -> Unit,
) {
    var proposals by remember { mutableStateOf<List<EditProposalSummary>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var withdrawing by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching { app.editApi.myProposals(limit = 50) }
            .onSuccess {
                proposals = it.items
                nextCursor = it.nextCursor
            }
            .onFailure { error = friendlyError(it) }
        loading = false
    }

    fun withdraw(id: String) {
        if (withdrawing != null) return
        withdrawing = id
        scope.launch {
            runCatching {
                val etag = runCatching { app.editApi.proposal(id).etag }.getOrNull()
                app.editApi.updateProposal(id, "withdrawn", ifMatch = etag)
            }.onSuccess {
                com.kungal.app.ui.components.AppMessages.show("提案已撤回")
                reload += 1
            }.onFailure {
                com.kungal.app.ui.components.AppMessages.show(friendlyError(it))
            }
            withdrawing = null
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "我的资料编辑提案",
                subtitle = "你提交的修改与审核结果",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { reload += 1 }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null -> ErrorBox(error!!, onRetry = { reload += 1 }, modifier = Modifier.fillMaxSize().padding(padding))
            proposals.isEmpty() -> EmptyBox("你还没有提交过资料编辑提案", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(proposals.size) { index ->
                    val item = proposals[index]
                    ProposalCard(
                        item = item,
                        showWork = true,
                        onOpenWork = { onOpenWork(item.workId) },
                        onOpen = { onOpenProposal(item.id) },
                        onWithdraw = if (item.viewer?.canWithdraw == true) {
                            { withdraw(item.id) }
                        } else {
                            null
                        },
                        withdrawBusy = withdrawing == item.id,
                    )
                }
                if (nextCursor != null) {
                    item(key = "more") {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = {
                                    val cursor = nextCursor ?: return@TextButton
                                    if (loadingMore) return@TextButton
                                    loadingMore = true
                                    scope.launch {
                                        runCatching { app.editApi.myProposals(cursor = cursor, limit = 50) }
                                            .onSuccess {
                                                proposals = proposals + it.items
                                                nextCursor = it.nextCursor
                                            }
                                            .onFailure {
                                                com.kungal.app.ui.components.AppMessages.show(friendlyError(it))
                                            }
                                        loadingMore = false
                                    }
                                },
                            ) {
                                if (loadingMore) CircularProgressIndicator(Modifier.padding(end = 6.dp))
                                Text(if (loadingMore) "加载中…" else "加载更多")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- 审核队列 ----------

@Composable
fun ReviewQueueScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenProposal: (String) -> Unit,
) {
    var canReview by remember { mutableStateOf<Boolean?>(null) }
    var state by remember { mutableStateOf("open") }
    var proposals by remember { mutableStateOf<List<EditProposalSummary>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        canReview = runCatching { app.meApi.permissions().permissions }
            .getOrNull()?.contains("galgame.edit_proposal.review")
    }
    LaunchedEffect(state, reload, canReview) {
        if (canReview != true) return@LaunchedEffect
        loading = true
        error = null
        runCatching { app.editApi.reviewQueue(state = state, limit = 50) }
            .onSuccess {
                proposals = it.items
                nextCursor = it.nextCursor
            }
            .onFailure { error = friendlyError(it) }
        loading = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "提案审核队列",
                subtitle = "社区提交的资料修改提案",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { reload += 1 }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        when {
            canReview == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            canReview == false -> EmptyBox("需要「审核资料编辑」权限", Modifier.fillMaxSize().padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(selectedTabIndex = ProposalStates.indexOfFirst { it.first == state }, edgePadding = 12.dp) {
                    ProposalStates.forEach { (value, label) ->
                        Tab(
                            selected = state == value,
                            onClick = { state = value },
                            text = { Text(label) },
                        )
                    }
                }
                when {
                    loading -> LoadingBox(Modifier.fillMaxSize())
                    error != null -> ErrorBox(error!!, onRetry = { reload += 1 }, modifier = Modifier.fillMaxSize())
                    proposals.isEmpty() -> EmptyBox("这里还没有提案", Modifier.fillMaxSize())
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(proposals.size) { index ->
                            val item = proposals[index]
                            ProposalCard(
                                item = item,
                                showWork = true,
                                onOpenWork = { onOpenWork(item.workId) },
                                onOpen = { onOpenProposal(item.id) },
                                onWithdraw = null,
                                withdrawBusy = false,
                            )
                        }
                        if (nextCursor != null) {
                            item(key = "more") {
                                Row(
                                    Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    TextButton(
                                        onClick = {
                                            val cursor = nextCursor ?: return@TextButton
                                            if (loadingMore) return@TextButton
                                            loadingMore = true
                                            scope.launch {
                                                runCatching { app.editApi.reviewQueue(state = state, cursor = cursor, limit = 50) }
                                                    .onSuccess {
                                                        proposals = proposals + it.items
                                                        nextCursor = it.nextCursor
                                                    }
                                                    .onFailure {
                                                        com.kungal.app.ui.components.AppMessages.show(friendlyError(it))
                                                    }
                                                loadingMore = false
                                            }
                                        },
                                    ) {
                                        Text(if (loadingMore) "加载中…" else "加载更多")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposalCard(
    item: EditProposalSummary,
    showWork: Boolean,
    onOpenWork: () -> Unit,
    onOpen: () -> Unit,
    onWithdraw: (() -> Unit)?,
    withdrawBusy: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showWork) proposalTitle(item) else "提案 #${item.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (showWork) Modifier.weight(1f).clickable(onClick = onOpenWork) else Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(onClick = onOpen, label = { Text(proposalStateLabel(item.state)) })
            }
            Text(
                "提案 #${item.id} · ${item.proposer.name ?: "用户 #${item.proposer.id}"} · " +
                    relativeTime(item.createdAt) + " · 基线 #${item.baseRevisionSeq}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            item.decider?.let {
                Text(
                    "处理人：${it.name ?: "用户 #${it.id}"}" +
                        (item.decidedAt?.let { time -> " · ${relativeTime(time)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) {
                    Text(if (item.state == "open") "查看提案" else "查看详情")
                }
                onWithdraw?.let {
                    TextButton(onClick = it, enabled = !withdrawBusy) {
                        Text(
                            if (withdrawBusy) "撤回中…" else "撤回",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

// ---------- 提案详情 / 审阅 ----------

@Composable
fun ProposalDetailScreen(
    app: KungalApp,
    proposalId: String,
    nsfwDisplay: com.kungal.app.NsfwDisplayMode,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenWork: (String) -> Unit,
) {
    val vm: ProposalDetailViewModel = viewModel(key = "proposal-$proposalId") {
        ProposalDetailViewModel(app, proposalId, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    var note by remember { mutableStateOf("") }
    var overrides by remember { mutableStateOf<Map<String, JsonElement>>(emptyMap()) }
    var rejected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var confirmDecline by remember { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    var actionStarted by remember { mutableStateOf(false) }

    // 合并 / 拒绝 / 撤回成功后退出（只读查看已关闭的提案时不退出）
    LaunchedEffect(state.proposal?.state, actionStarted) {
        if (actionStarted && state.proposal != null && state.proposal?.state != "open") onDone()
    }

    val proposal = state.proposal
    val effective = proposal?.effectivePatch ?: JsonObject(emptyMap())
    val current = state.form?.fieldValues ?: JsonObject(emptyMap())
    val canDecide = proposal?.viewer?.canDecide == true && proposal.state == "open" && state.form != null
    val orderedKeys = buildList {
        state.form?.fields?.forEach { field -> if (field.key in effective) add(field.key) }
        effective.keys.filter { key -> key !in this }.sorted().forEach { add(it) }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (proposal?.state == "open") "审阅提案" else "提案详情",
                subtitle = proposal?.let { "#${it.id}" },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = { vm.load() }, modifier = Modifier.fillMaxSize().padding(padding))
            proposal == null -> EmptyBox("未找到这个提案", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    proposalTitleOf(state),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f).clickable { onOpenWork(proposal.workId) },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                AssistChip(onClick = { }, label = { Text(proposalStateLabel(proposal.state)) })
                            }
                            Text(
                                "提案人：${proposal.proposer.name ?: "用户 #${proposal.proposer.id}"} · " +
                                    relativeTime(proposal.createdAt) + " · 基线版本 #${proposal.baseRevisionSeq}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            proposal.note?.takeIf { it.isNotBlank() }?.let {
                                Text("提案说明：$it", style = MaterialTheme.typography.bodySmall)
                            }
                            proposal.decider?.let {
                                Text(
                                    "处理人：${it.name ?: "用户 #${it.id}"}" +
                                        (proposal.decidedAt?.let { t -> " · ${relativeTime(t)}" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (proposal.amendments.isNotEmpty()) {
                    item(key = "amendments") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("审核修正记录", style = MaterialTheme.typography.titleSmall)
                                proposal.amendments.forEach { amendment ->
                                    Text(
                                        "#${amendment.seq} · ${amendment.amender.name ?: "用户 #${amendment.amender.id}"} · " +
                                            relativeTime(amendment.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    amendment.note?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "fields-title") {
                    Text(
                        "逐字段审阅（${orderedKeys.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
                if (orderedKeys.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "提案的所有字段都已被移除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(orderedKeys.size) { index ->
                    val key = orderedKeys[index]
                    ProposalFieldReview(
                        fieldKey = key,
                        currentValue = current[key],
                        proposedValue = effective[key],
                        override = overrides[key],
                        editing = editingKey == key,
                        rejected = key in rejected,
                        canDecide = canDecide,
                        field = state.form?.fields?.firstOrNull { it.key == key },
                        form = state.form,
                        names = state.names,
                        onStartEdit = {
                            overrides = overrides + (key to (effective[key] ?: JsonObject(emptyMap())))
                            rejected = rejected - key
                            editingKey = key
                        },
                        onCancelEdit = {
                            editingKey = null
                            overrides = overrides - key
                        },
                        onOverrideChange = { value -> overrides = overrides + (key to value) },
                        onToggleReject = {
                            rejected = if (key in rejected) rejected - key else rejected + key
                            if (key in rejected) {
                                editingKey = null
                                overrides = overrides - key
                            }
                        },
                        guardedOnChangeKey = { otherKey, value ->
                            overrides = overrides + (otherKey to value)
                        },
                        upload = { bytes, name, mime, preset -> vm.uploadImage(bytes, name, mime, preset) },
                        searches = ProposalSearches(app),
                    )
                }
                item(key = "actions") {
                    if (proposal.state == "open") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canDecide) {
                                    OutlinedTextField(
                                        value = note,
                                        onValueChange = { note = it },
                                        label = { Text("审核说明") },
                                        placeholder = { Text("合并备注，或拒绝理由（拒绝时必填）") },
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    state.actionError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val amended = overrides.any { (key, value) -> value != effective[key] } || rejected.isNotEmpty()
                                        if (amended) {
                                            Text(
                                                "将先保存 ${overrides.count { (k, v) -> v != effective[k] } + rejected.size} 处修正，再合并",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        TextButton(onClick = { confirmDecline = true }, enabled = !state.acting) {
                                            Text("拒绝提案", color = MaterialTheme.colorScheme.error)
                                        }
                                        Button(
                                            onClick = {
                                                actionStarted = true
                                                vm.amendAndMerge(note, overrides, rejected)
                                            },
                                            enabled = !state.acting,
                                        ) {
                                            Text(if (amended) "修正并合并" else "合并提案")
                                        }
                                    }
                                } else {
                                    Text(
                                        "只读审阅：只有具备裁决权限的管理员（或该词条创建者）可以合并、修正或拒绝。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (proposal.viewer?.canWithdraw == true) {
                                    TextButton(onClick = { confirmWithdraw = true }, enabled = !state.acting) {
                                        Text("撤回我的提案", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDecline) {
        AlertDialog(
            onDismissRequest = { confirmDecline = false },
            title = { Text("拒绝这个提案？") },
            text = {
                Text(if (note.isBlank()) "拒绝理由将展示给提案人：尚未填写，请先填写审核说明。" else "拒绝理由将展示给提案人：$note")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDecline = false
                        actionStarted = true
                        vm.decline(note)
                    },
                    enabled = note.isNotBlank() && !state.acting,
                ) { Text("确认拒绝") }
            },
            dismissButton = { TextButton(onClick = { confirmDecline = false }) { Text("取消") } },
        )
    }
    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("撤回这个提案？") },
            text = { Text("撤回后提案将关闭，不再出现在待审队列。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmWithdraw = false
                        actionStarted = true
                        vm.withdraw()
                    },
                    enabled = !state.acting,
                ) { Text("确认撤回") }
            },
            dismissButton = { TextButton(onClick = { confirmWithdraw = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ProposalFieldReview(
    fieldKey: String,
    currentValue: JsonElement?,
    proposedValue: JsonElement?,
    override: JsonElement?,
    editing: Boolean,
    rejected: Boolean,
    canDecide: Boolean,
    field: com.kungal.core.model.EditField?,
    form: com.kungal.core.model.EditForm?,
    names: EditNames,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onOverrideChange: (JsonElement) -> Unit,
    onToggleReject: () -> Unit,
    guardedOnChangeKey: (String, JsonElement) -> Unit,
    upload: suspend (ByteArray, String, String, String) -> String?,
    searches: ProposalSearches,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rejected) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(editFieldLabel(fieldKey), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueColumn("当前", editValueSummary(fieldKey, currentValue), Modifier.weight(1f))
                ValueColumn("提案", editValueSummary(fieldKey, override ?: proposedValue), Modifier.weight(1f))
            }
            if (canDecide) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!editing) {
                        TextButton(onClick = onStartEdit, enabled = !rejected) { Text("修正该值") }
                    } else {
                        TextButton(onClick = onCancelEdit) { Text("取消修正") }
                    }
                    TextButton(onClick = onToggleReject) {
                        Text(
                            if (rejected) "恢复该字段" else "拒绝该字段",
                            color = if (rejected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (editing && field != null && form != null) {
                EditFieldContent(
                    field = field,
                    vocabularies = form.vocabularies,
                    value = override,
                    suppressedRoster = form.fieldValues["catalog.work.roster.suppressed"],
                    names = names,
                    onChange = onOverrideChange,
                    onSuppressedChange = { guardedOnChangeKey("catalog.work.roster.suppressed", it) },
                    upload = upload,
                    searchTags = searches.tags,
                    searchCompanies = searches.companies,
                    searchSeries = searches.series,
                    searchEngines = searches.engines,
                    searchCharacters = searches.characters,
                    searchStaff = searches.staff,
                )
            }
        }
    }
}

@Composable
private fun ValueColumn(label: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private class ProposalSearches(private val app: KungalApp) {
    val tags: suspend (String) -> List<PickerItem> = { app.searchTags(it) }
    val companies: suspend (String) -> List<PickerItem> = { app.searchCompanies(it) }
    val series: suspend (String) -> List<PickerItem> = { app.searchSeries(it) }
    val engines: suspend (String) -> List<PickerItem> = { app.searchEngines(it) }
    val characters: suspend (String) -> List<PickerItem> = { app.searchCharacters(it) }
    val staff: suspend (String) -> List<PickerItem> = { app.searchStaff(it) }
}

private fun proposalTitleOf(state: ProposalDetailState): String {
    val proposal = state.proposal ?: return "提案"
    val name = state.work?.let { workTitle(it) }
        ?: proposal.workSummary.localized["zh-Hans"]?.value
        ?: proposal.workSummary.displayName
    return name.ifBlank { "Galgame #${proposal.workId}" }
}
