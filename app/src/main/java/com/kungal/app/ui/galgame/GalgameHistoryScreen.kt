package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.EditRevision
import com.kungal.core.model.EditRevisionDiff
import com.kungal.core.model.Work
import com.kungal.core.model.revisionActionLabel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun GalgameHistoryScreen(
    app: KungalApp,
    workId: String,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenProposal: (String) -> Unit,
) {
    var revisions by remember { mutableStateOf<List<EditRevision>>(emptyList()) }
    var work by remember { mutableStateOf<Work?>(null) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var diff by remember { mutableStateOf<EditRevisionDiff?>(null) }
    var diffLoading by remember { mutableStateOf(false) }
    var diffError by remember { mutableStateOf<String?>(null) }
    var revertTarget by remember { mutableStateOf<Int?>(null) }
    var revertNote by remember { mutableStateOf("") }
    var reverting by remember { mutableStateOf(false) }
    var compareOpen by remember { mutableStateOf(false) }
    var compareFrom by remember { mutableStateOf(1) }
    var compareTo by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workId, reload) {
        loading = true
        error = null
        runCatching { app.editApi.revisions(workId, page = 1, limit = 100) }
            .onSuccess {
                revisions = it.items
                total = it.total
            }
            .onFailure { error = friendlyError(it) }
        work = runCatching { app.galgameApi.work(workId, includeNsfw = true) }.getOrNull()
        loading = false
    }

    fun loadDiff(from: Int, to: Int) {
        if (diffLoading) return
        diffLoading = true
        diffError = null
        diff = null
        scope.launch {
            runCatching { app.editApi.diff(workId, from, to) }
                .onSuccess { diff = it }
                .onFailure { diffError = friendlyError(it) }
            diffLoading = false
        }
    }

    fun revert() {
        val target = revertTarget ?: return
        if (reverting) return
        reverting = true
        scope.launch {
            runCatching {
                app.editApi.revert(
                    workId = workId,
                    toSeq = target,
                    note = revertNote.trim().ifBlank { null },
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess { result ->
                AppMessages.show(
                    if (result.revision != null) "回滚成功（已生成一条新的修订记录）"
                    else "回滚提案已提交，等待审核",
                )
                revertTarget = null
                revertNote = ""
                reload += 1
            }.onFailure { AppMessages.show(friendlyError(it)) }
            reverting = false
        }
    }

    val latestSeq = revisions.firstOrNull()?.seq ?: 0

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "修订历史",
                subtitle = work?.let { workTitle(it) } ?: "#$workId",
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
            revisions.isEmpty() -> EmptyBox("这部作品还没有修订记录", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "header") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "共 $total 条修订 · 最新版本 #$latestSeq",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "完整编辑记录（含旧版百科迁移的历史修订），可任选两个版本对比差异。",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onOpenWork(workId) }) { Text("返回词条") }
                                TextButton(onClick = onOpenEdit) { Text("编辑资料") }
                                TextButton(
                                    onClick = {
                                        compareFrom = revisions.lastOrNull()?.seq ?: 1
                                        compareTo = latestSeq
                                        compareOpen = true
                                    },
                                ) { Text("版本对比") }
                            }
                        }
                    }
                }
                items(revisions.size) { index ->
                    val revision = revisions[index]
                    RevisionRow(
                        revision = revision,
                        canRevert = app.isLoggedIn && revision.seq < latestSeq,
                        onDiffPrevious = if (revision.seq > 1) {
                            { loadDiff(revision.seq - 1, revision.seq) }
                        } else {
                            null
                        },
                        onRevert = { revertTarget = revision.seq },
                        onOpenProposal = revision.proposalId?.let { id -> { onOpenProposal(id) } },
                    )
                }
            }
        }
    }

    if (diff != null || diffLoading || diffError != null) {
        AlertDialog(
            onDismissRequest = { diff = null; diffError = null },
            title = {
                Text(
                    diff?.let { "版本对比 #${it.fromSeq} → #${it.toSeq}" } ?: "版本对比",
                )
            },
            text = {
                when {
                    diffLoading -> CircularProgressIndicator()
                    diffError != null -> Text(diffError!!, color = MaterialTheme.colorScheme.error)
                    diff != null && diff!!.fieldChanges.isEmpty() -> Text("两个版本没有差异")
                    else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        diff!!.fieldChanges.forEach { change ->
                            item(key = change.key) {
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    Text(editFieldLabel(change.key), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "旧：${editValueSummary(change.key, change.from)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "新：${editValueSummary(change.key, change.to)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { diff = null; diffError = null }) { Text("关闭") }
            },
        )
    }

    if (compareOpen) {
        AlertDialog(
            onDismissRequest = { compareOpen = false },
            title = { Text("版本对比") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("从", modifier = Modifier.weight(1f))
                        EditIntSelect(
                            options = revisions.map { it.seq to "#${it.seq} ${revisionActionLabel(it.revisionAction)}" },
                            selected = compareFrom,
                            onSelect = { compareFrom = it },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("到", modifier = Modifier.weight(1f))
                        EditIntSelect(
                            options = revisions.map { it.seq to "#${it.seq} ${revisionActionLabel(it.revisionAction)}" },
                            selected = compareTo,
                            onSelect = { compareTo = it },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        compareOpen = false
                        loadDiff(compareFrom, compareTo)
                    },
                    enabled = compareFrom != compareTo,
                ) { Text("查看差异") }
            },
            dismissButton = { TextButton(onClick = { compareOpen = false }) { Text("取消") } },
        )
    }

    revertTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!reverting) revertTarget = null },
            title = { Text("回滚到版本 #$target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("回滚会把词条恢复到该版本的字段值，并生成一条新的修订记录（历史不会被删除）。",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = revertNote,
                        onValueChange = { revertNote = it },
                        label = { Text("说明（可选）") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { revert() }, enabled = !reverting) {
                    Text(if (reverting) "提交中…" else "确认回滚")
                }
            },
            dismissButton = {
                TextButton(onClick = { revertTarget = null }, enabled = !reverting) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RevisionRow(
    revision: EditRevision,
    canRevert: Boolean,
    onDiffPrevious: (() -> Unit)?,
    onRevert: () -> Unit,
    onOpenProposal: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "#${revision.seq} · ${revisionActionLabel(revision.revisionAction)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    relativeTime(revision.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "编辑者：${revision.actor.name ?: "用户 #${revision.actor.id}"}" +
                    (revision.lastAmender?.let { " · 最后修正：${it.name ?: "用户 #${it.id}"}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (revision.changedFields.isNotEmpty()) {
                Text(
                    revision.changedFields.joinToString("、") { editFieldLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onDiffPrevious?.let { TextButton(onClick = it) { Text("对比上一版") } }
                if (canRevert) {
                    TextButton(onClick = onRevert) {
                        Text("回滚到此版本", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                onOpenProposal?.let { TextButton(onClick = it) { Text("查看提案") } }
            }
        }
    }
}
