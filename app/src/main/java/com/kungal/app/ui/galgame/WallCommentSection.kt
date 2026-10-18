package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.WallComment
import com.kungal.core.model.WallCommentViewer
import kotlinx.coroutines.launch
import java.util.UUID

private val FlagReasonOptions = listOf(
    "spam" to "垃圾广告",
    "abuse" to "辱骂攻击",
    "off_topic" to "离题",
    "other" to "其他",
    "nsfw_mislabel" to "NSFW 标注错误",
)

/**
 * 通用评论墙（作品 / 评分）：两层展示（根评论 + 回复），支持发帖、回复、点赞、
 * 编辑、删除、举报与审核中状态。评论正文由服务端转成 ContentDocument，用统一渲染器显示。
 */
@Composable
fun WallCommentSection(
    app: KungalApp,
    subjectType: String,
    subjectId: String,
    maxLength: Int,
    title: String = "评论",
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var comments by remember(subjectType, subjectId) { mutableStateOf<List<WallComment>>(emptyList()) }
    var cursor by remember(subjectType, subjectId) { mutableStateOf<String?>(null) }
    var likePending by remember(subjectType, subjectId) { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember(subjectType, subjectId) { mutableStateOf(true) }
    var loadingMore by remember(subjectType, subjectId) { mutableStateOf(false) }
    var error by remember(subjectType, subjectId) { mutableStateOf<String?>(null) }
    var composer by remember(subjectType, subjectId) { mutableStateOf("") }
    var sending by remember(subjectType, subjectId) { mutableStateOf(false) }
    var replyingTo by remember(subjectType, subjectId) { mutableStateOf<WallComment?>(null) }
    var replyText by remember(subjectType, subjectId) { mutableStateOf("") }
    var sendingReply by remember(subjectType, subjectId) { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WallComment?>(null) }
    var editText by remember { mutableStateOf("") }
    var savingEdit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WallComment?>(null) }
    var flagging by remember { mutableStateOf<WallComment?>(null) }
    var flagReason by remember { mutableStateOf("spam") }
    var flagNote by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.wallCommentApi.list(subjectType, subjectId, limit = 20) }
                .onSuccess { page ->
                    comments = page.items
                    cursor = page.nextCursor
                    loading = false
                }
                .onFailure {
                    error = friendlyError(it)
                    loading = false
                }
        }
    }

    fun loadMore() {
        val next = cursor ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            runCatching { app.wallCommentApi.list(subjectType, subjectId, cursor = next, limit = 20) }
                .onSuccess { page ->
                    comments = comments + page.items
                    cursor = page.nextCursor
                    loadingMore = false
                }
                .onFailure {
                    AppMessages.show(friendlyError(it))
                    loadingMore = false
                }
        }
    }

    fun sendReply() {
        val target = replyingTo ?: return
        val text = replyText.trim()
        if (text.isEmpty() || sendingReply) return
        sendingReply = true
        scope.launch {
            runCatching {
                app.wallCommentApi.create(
                    subjectType = subjectType,
                    subjectId = subjectId,
                    contentMarkdown = text,
                    parentCommentId = target.id,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess {
                sendingReply = false
                replyingTo = null
                replyText = ""
                reload()
            }.onFailure {
                sendingReply = false
                AppMessages.show(friendlyError(it))
            }
        }
    }

    LaunchedEffect(subjectType, subjectId) { reload() }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (!app.isLoggedIn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "登录后参与评论",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOpenLogin) { Text("去登录") }
            }
        } else {
            OutlinedTextField(
                value = composer,
                onValueChange = { if (it.length <= maxLength) composer = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                placeholder = { Text("友善地发表你的看法…") },
                minLines = 2,
                supportingText = { Text("${composer.length}/$maxLength") },
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    shape = RoundedCornerShape(50),
                    onClick = {
                        val text = composer.trim()
                        if (text.isEmpty() || sending) return@Button
                        sending = true
                        scope.launch {
                            runCatching {
                                app.wallCommentApi.create(
                                    subjectType = subjectType,
                                    subjectId = subjectId,
                                    contentMarkdown = text,
                                    parentCommentId = null,
                                    idempotencyKey = UUID.randomUUID().toString(),
                                )
                            }.onSuccess {
                                sending = false
                                composer = ""
                                reload()
                            }.onFailure {
                                sending = false
                                AppMessages.show(friendlyError(it))
                            }
                        }
                    },
                    enabled = !sending && composer.isNotBlank(),
                ) { Text(if (sending) "发送中…" else "发送") }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        when {
            loading && comments.isEmpty() -> LoadingBox()
            error != null && comments.isEmpty() -> ErrorBox(error!!, onRetry = { reload() })
            comments.isEmpty() -> Text(
                "还没有评论，来抢沙发吧",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            else -> {
                val replies = comments.filter { it.parentCommentId != null }
                    .groupBy { it.rootCommentId ?: it.parentCommentId.orEmpty() }
                val roots = comments.filter { it.parentCommentId == null }
                roots.forEach { root ->
                    CommentThread(
                        root = root,
                        replies = replies[root.id].orEmpty(),
                        replyingToId = replyingTo?.id,
                        replyText = replyText,
                        sendingReply = sendingReply,
                        maxLength = maxLength,
                        onReplyTextChange = { replyText = it },
                        onCancelReply = { replyingTo = null; replyText = "" },
                        onSubmitReply = { sendReply() },
                        onOpenUser = onOpenUser,
                        onReply = {
                            if (app.isLoggedIn) {
                                replyingTo = it
                                replyText = ""
                            } else {
                                onOpenLogin()
                            }
                        },
                        onLike = { comment ->
                            if (!app.isLoggedIn) {
                                onOpenLogin()
                            } else if (comment.id !in likePending) {
                                val next = comment.viewer?.hasLiked != true
                                likePending = likePending + comment.id
                                // 先切换本地状态，点击立即变色，不等接口
                                comments = comments.map {
                                    if (it.id != comment.id) it else it.copy(
                                        likeCount = (it.likeCount + if (next) 1 else -1)
                                            .coerceAtLeast(0),
                                        viewer = (it.viewer ?: WallCommentViewer())
                                            .copy(hasLiked = next),
                                    )
                                }
                                scope.launch {
                                    runCatching {
                                        app.wallCommentApi.setLike(comment.id, next)
                                    }.onSuccess { updated ->
                                        comments = comments.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                    }.onFailure {
                                        // 失败回滚到点击前的样子
                                        comments = comments.map {
                                            if (it.id == comment.id) comment else it
                                        }
                                        AppMessages.show(friendlyError(it))
                                    }
                                    likePending = likePending - comment.id
                                }
                            }
                        },
                        onEdit = {
                            scope.launch {
                                runCatching { app.wallCommentApi.source(it.id) }
                                    .onSuccess { source ->
                                        editText = source.contentMarkdown
                                        editing = it
                                    }
                                    .onFailure { e -> AppMessages.show(friendlyError(e)) }
                            }
                        },
                        onDelete = { deleting = it },
                        onFlag = { flagging = it },
                    )
                }
                if (cursor != null) {
                    TextButton(
                        onClick = { loadMore() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingMore,
                    ) { Text(if (loadingMore) "加载中…" else "加载更多评论") }
                }
            }
        }
    }

    editing?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!savingEdit) editing = null },
            title = { Text("编辑评论") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { if (it.length <= maxLength) editText = it },
                    minLines = 3,
                    supportingText = { Text("${editText.length}/$maxLength") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (savingEdit || editText.isBlank()) return@TextButton
                        savingEdit = true
                        scope.launch {
                            runCatching { app.wallCommentApi.update(target.id, editText.trim()) }
                                .onSuccess {
                                    savingEdit = false
                                    editing = null
                                    reload()
                                }
                                .onFailure {
                                    savingEdit = false
                                    AppMessages.show(friendlyError(it))
                                }
                        }
                    },
                ) { Text(if (savingEdit) "保存中…" else "保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }, enabled = !savingEdit) { Text("取消") }
            },
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除评论") },
            text = { Text("删除后评论会变成「已删除」墓碑，回复仍会保留。确定删除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        runCatching { app.wallCommentApi.delete(target.id) }
                            .onSuccess { reload() }
                            .onFailure { AppMessages.show(friendlyError(it)) }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }

    flagging?.let { target ->
        AlertDialog(
            onDismissRequest = { flagging = null },
            title = { Text("举报评论") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlagReasonOptions.forEach { (value, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = flagReason == value,
                            onClick = { flagReason = value },
                            label = { Text(label) },
                        )
                    }
                    OutlinedTextField(
                        value = flagNote,
                        onValueChange = { flagNote = it },
                        label = { Text("补充说明（可选）") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val reason = flagReason
                    flagging = null
                    scope.launch {
                        runCatching {
                            app.wallCommentApi.flag(
                                wallCommentId = target.id,
                                reason = reason,
                                note = flagNote.trim().ifBlank { null },
                                idempotencyKey = UUID.randomUUID().toString(),
                            )
                        }.onSuccess { AppMessages.show("已举报，站长会尽快处理") }
                            .onFailure { AppMessages.show(friendlyError(it)) }
                    }
                    flagNote = ""
                }) { Text("提交举报") }
            },
            dismissButton = { TextButton(onClick = { flagging = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CommentThread(
    root: WallComment,
    replies: List<WallComment>,
    replyingToId: String?,
    replyText: String,
    sendingReply: Boolean,
    maxLength: Int,
    onReplyTextChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSubmitReply: () -> Unit,
    onOpenUser: (String) -> Unit,
    onReply: (WallComment) -> Unit,
    onLike: (WallComment) -> Unit,
    onEdit: (WallComment) -> Unit,
    onDelete: (WallComment) -> Unit,
    onFlag: (WallComment) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CommentRow(
            comment = root,
            onOpenUser = onOpenUser,
            onReply = onReply,
            onLike = onLike,
            onEdit = onEdit,
            onDelete = onDelete,
            onFlag = onFlag,
        )
        if (replyingToId == root.id) {
            InlineReplyComposer(
                target = root,
                text = replyText,
                maxLength = maxLength,
                sending = sendingReply,
                indent = 32.dp,
                onTextChange = onReplyTextChange,
                onCancel = onCancelReply,
                onSend = onSubmitReply,
            )
        }
        replies.forEach { reply ->
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(28.dp))
                CommentRow(
                    comment = reply,
                    onOpenUser = onOpenUser,
                    onReply = onReply,
                    onLike = onLike,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onFlag = onFlag,
                )
            }
            if (replyingToId == reply.id) {
                InlineReplyComposer(
                    target = reply,
                    text = replyText,
                    maxLength = maxLength,
                    sending = sendingReply,
                    indent = 60.dp,
                    onTextChange = onReplyTextChange,
                    onCancel = onCancelReply,
                    onSend = onSubmitReply,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }
}

/** 点击「回复」后直接出现在该条评论下方的输入框。 */
@Composable
private fun InlineReplyComposer(
    target: WallComment,
    text: String,
    maxLength: Int,
    sending: Boolean,
    indent: androidx.compose.ui.unit.Dp,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "回复 @${target.author.name ?: target.author.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = onCancel) { Text("取消") }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= maxLength) onTextChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            placeholder = { Text("回复这条评论…", style = MaterialTheme.typography.bodySmall) },
            minLines = 2,
            supportingText = { Text("${text.length}/$maxLength") },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                shape = RoundedCornerShape(50),
                onClick = onSend,
                enabled = !sending && text.isNotBlank(),
            ) { Text(if (sending) "发送中…" else "发送") }
        }
    }
}

@Composable
private fun CommentRow(
    comment: WallComment,
    onOpenUser: (String) -> Unit,
    onReply: (WallComment) -> Unit,
    onLike: (WallComment) -> Unit,
    onEdit: (WallComment) -> Unit,
    onDelete: (WallComment) -> Unit,
    onFlag: (WallComment) -> Unit,
) {
    var menuOpen by remember(comment.id) { mutableStateOf(false) }
    val deleted = comment.state == "deleted"
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(
                url = comment.author.avatar?.url,
                seed = comment.author.name,
                modifier = Modifier.size(26.dp).clickable { onOpenUser(comment.author.id) },
            )
            Spacer(Modifier.width(6.dp))
            Text(
                comment.author.name ?: comment.author.id,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            comment.addressee?.let { target ->
                Text(
                    " 回复 @${target.name ?: target.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (comment.state == "held") {
                Spacer(Modifier.width(6.dp))
                Chip("审核中")
            }
            if (comment.isEditedByModerator) {
                Spacer(Modifier.width(6.dp))
                Chip("管理员编辑")
            } else if (comment.editedAt != null) {
                Spacer(Modifier.width(6.dp))
                Chip("已编辑")
            }
            Spacer(Modifier.weight(1f))
            Text(
                relativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作", modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                DropdownMenuItem(
                    text = { Text("回复") },
                    onClick = { menuOpen = false; onReply(comment) },
                )
                if (comment.viewer?.canEdit == true) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { menuOpen = false; onEdit(comment) },
                    )
                }
                if (comment.viewer?.canDelete == true) {
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = { menuOpen = false; onDelete(comment) },
                    )
                }
                if (comment.viewer?.canFlag == true) {
                    DropdownMenuItem(
                        text = { Text("举报") },
                        onClick = { menuOpen = false; onFlag(comment) },
                    )
                }
            }
        }
        if (deleted) {
            Text(
                "该评论已删除",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp),
            )
        } else {
            Box(Modifier.padding(start = 32.dp)) {
                ContentDocumentView(comment.content)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 32.dp)) {
                val liked = comment.viewer?.hasLiked == true
                TextButton(onClick = { onLike(comment) }) {
                    Icon(
                        if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (liked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        comment.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                TextButton(onClick = { onReply(comment) }) { Text("回复", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
