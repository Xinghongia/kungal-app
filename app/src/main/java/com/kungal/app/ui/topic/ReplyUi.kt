package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyComment

/** 楼层回复卡片：正文、反应、楼中楼评论与按权限显示的操作。 */
@Composable
fun ReplyCard(
    reply: Reply,
    busy: Boolean,
    loggedIn: Boolean,
    canPinReply: Boolean,
    canSetBestAnswer: Boolean,
    commentSending: Boolean,
    onToggleReaction: (String) -> Unit,
    onShowHistory: () -> Unit,
    onQuote: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onSetBestAnswer: () -> Unit,
    onClearBestAnswer: () -> Unit,
    onSendComment: (text: String, parentCommentId: String?) -> Unit,
    onToggleCommentLike: (String) -> Unit,
    onEditComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onRequireLogin: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var commentOpen by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ReplyComment?>(null) }
    val viewer = reply.viewer

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = reply.author.avatar?.url, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = reply.author.name ?: reply.author.id,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(6.dp))
                if (reply.isBestAnswer) Chip("最佳答案")
                if (reply.isPinned) Chip("置顶")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "#${reply.floor}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(reply.createdAt) + if (reply.editedAt != null) " · 已编辑" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (viewer?.canEdit == true || viewer?.canDelete == true || viewer?.canLike == true) {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "更多操作",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        DropdownMenuItem(
                            text = { Text("引用回复") },
                            onClick = { menuOpen = false; onQuote() },
                        )
                        if (viewer?.canEdit == true) {
                            DropdownMenuItem(
                                text = { Text("编辑回复") },
                                onClick = { menuOpen = false; onEdit() },
                            )
                        }
                        if (viewer?.canDelete == true) {
                            DropdownMenuItem(
                                text = { Text("删除回复") },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                        if (canPinReply) {
                            DropdownMenuItem(
                                text = { Text(if (reply.isPinned) "取消置顶" else "置顶回复") },
                                onClick = {
                                    menuOpen = false
                                    if (reply.isPinned) onUnpin() else onPin()
                                },
                            )
                        }
                        if (canSetBestAnswer) {
                            DropdownMenuItem(
                                text = { Text(if (reply.isBestAnswer) "取消最佳答案" else "设为最佳答案") },
                                onClick = {
                                    menuOpen = false
                                    if (reply.isBestAnswer) onClearBestAnswer() else onSetBestAnswer()
                                },
                            )
                        }
                    }
                }
            }

            ContentDocumentView(document = reply.content)

            Row(verticalAlignment = Alignment.CenterVertically) {
                QuickReactionRow(
                    reactions = reply.reactions,
                    busy = busy,
                    onToggle = {
                        if (!loggedIn) onRequireLogin() else onToggleReaction(it)
                    },
                )
                IconButton(
                    onClick = {
                        if (!loggedIn) onRequireLogin() else commentOpen = true
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.AddReaction,
                        contentDescription = "表情反应",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onShowHistory, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                    Text("反应历史", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { commentOpen = !commentOpen },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        if (reply.comments.isEmpty()) "评论" else "评论 ${reply.comments.size}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            if (reply.comments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reply.comments.forEach { comment ->
                        CommentRow(
                            comment = comment,
                            loggedIn = loggedIn,
                            onLike = { onToggleCommentLike(comment.id) },
                            onReply = {
                                replyTo = comment
                                commentOpen = true
                            },
                            onEdit = { onEditComment(comment.id) },
                            onDelete = { onDeleteComment(comment.id) },
                            onRequireLogin = onRequireLogin,
                        )
                    }
                }
            }

            if (commentOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                replyTo?.let { "回复 ${it.author.name ?: it.author.id}" } ?: "写下你的评论",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                    )
                    TextButton(
                        enabled = commentText.isNotBlank() && !commentSending,
                        onClick = {
                            if (!loggedIn) {
                                onRequireLogin()
                                return@TextButton
                            }
                            onSendComment(commentText, replyTo?.id)
                            commentText = ""
                            replyTo = null
                        },
                    ) {
                        if (commentSending) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("发送")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: ReplyComment,
    loggedIn: Boolean,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequireLogin: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val liked = comment.viewer?.hasLiked == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(comment.author.avatar?.url, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                comment.author.name ?: comment.author.id,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            comment.inReplyToUser?.let { target ->
                Text(
                    " 回复 ${target.name ?: target.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                relativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (comment.viewer?.canEdit == true || comment.viewer?.canDelete == true) {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "评论操作",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    if (comment.viewer?.canEdit == true) {
                        DropdownMenuItem(
                            text = { Text("编辑评论") },
                            onClick = { menuOpen = false; onEdit() },
                        )
                    }
                    if (comment.viewer?.canDelete == true) {
                        DropdownMenuItem(
                            text = { Text("删除评论") },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
        ContentDocumentView(document = comment.content)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (!loggedIn) onRequireLogin() else onLike()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (liked) "❤️" else "🤍", fontSize = 12.sp)
                Spacer(Modifier.width(3.dp))
                Text(
                    comment.likeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (liked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = { if (!loggedIn) onRequireLogin() else onReply() },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Text("回复", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
