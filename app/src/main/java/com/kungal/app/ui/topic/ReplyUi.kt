package com.kungal.app.ui.topic

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.ReactionRecord
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyComment
import com.kungal.core.model.UserFollowState

/**
 * 楼层回复卡片（版式对齐网站）：头部作者/楼层/时间，正文，底部一行
 * 反应 chip + 表态 + 回复 + 评论 + 更多菜单。
 */
@Composable
fun ReplyCard(
    reply: Reply,
    busy: Boolean,
    loggedIn: Boolean,
    isSelf: Boolean,
    canPinReply: Boolean,
    canSetBestAnswer: Boolean,
    commentSending: Boolean,
    followState: UserFollowState?,
    onLoadFollowState: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onLoadHistory: suspend () -> List<ReactionRecord>,
    onOpenAuthor: () -> Unit,
    onQuote: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
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
    var commentReplyTarget by remember { mutableStateOf<ReplyComment?>(null) }
    var commentReplyText by remember { mutableStateOf("") }
    val viewer = reply.viewer
    val hasAnyReaction = reply.reactions.any { it.count > 0 }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenAuthor),
                ) {
                    AvatarImage(url = reply.author.avatar?.url, modifier = Modifier.size(28.dp), seed = reply.author.name)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reply.author.name ?: reply.author.id,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(6.dp))
                            reply.authorMoemoepoint.takeIf { it > 0 }?.let {
                                Text(
                                    "@ $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (reply.isBestAnswer) {
                                Spacer(Modifier.width(4.dp)); Chip("最佳答案")
                            }
                            if (reply.isPinned) {
                                Spacer(Modifier.width(4.dp)); Chip("置顶")
                            }
                        }
                        Text(
                            text = fullDateTime(reply.createdAt) + if (reply.editedAt != null) " · 已编辑" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "#${reply.floor}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ContentDocumentView(document = reply.content)

            // 反应 chip 行
            if (hasAnyReaction) {
                ReactionChips(
                    reactions = reply.reactions,
                    busy = busy,
                    enabled = true,
                    onToggle = { if (!loggedIn) onRequireLogin() else onToggleReaction(it) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // 底部操作行：表态 / 回复 / 评论 / 更多
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReactionPickerButton(
                    reactions = reply.reactions,
                    busy = busy,
                    subjectLabel = "该回复",
                    enabled = true,
                    onToggle = { if (!loggedIn) onRequireLogin() else onToggleReaction(it) },
                    onLoadHistory = onLoadHistory,
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { if (!loggedIn) onRequireLogin() else onQuote() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "回复",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(
                    onClick = {
                        if (!loggedIn) onRequireLogin() else commentOpen = !commentOpen
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reply.comments.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Text("${reply.comments.size}", style = MaterialTheme.typography.labelMedium)
                    }
                }
                // 更多菜单：锚定在按钮所在的 Box 上，避免弹层跑到屏幕角落
                Box {
                    IconButton(
                        onClick = {
                            menuOpen = true
                            onLoadFollowState()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
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
                            text = { Text("分享该回复") },
                            onClick = { menuOpen = false; onShare() },
                        )
                        if (!isSelf && loggedIn) {
                            val following = followState?.isFollowing == true
                            DropdownMenuItem(
                                text = { Text(if (following) "取消关注 TA" else "关注 TA") },
                                onClick = { menuOpen = false; onToggleFollow() },
                            )
                        }
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
                        if (!isSelf && loggedIn) {
                            DropdownMenuItem(
                                text = {
                                    Text("举报", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuOpen = false; onReport() },
                            )
                        }
                    }
                }
            }

            // 楼中楼评论
            if (reply.comments.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reply.comments.forEach { comment ->
                        CommentRow(
                            comment = comment,
                            loggedIn = loggedIn,
                            isSelf = false,
                            onLike = { onToggleCommentLike(comment.id) },
                            onReply = {
                                if (!loggedIn) {
                                    onRequireLogin()
                                } else {
                                    commentReplyTarget = comment
                                    commentReplyText = ""
                                }
                            },
                            onEdit = { onEditComment(comment.id) },
                            onDelete = { onDeleteComment(comment.id) },
                            onRequireLogin = onRequireLogin,
                            onOpenAuthor = onOpenAuthor,
                        )
                        if (commentReplyTarget?.id == comment.id) {
                            InlineCommentReply(
                                target = comment,
                                text = commentReplyText,
                                sending = commentSending,
                                onTextChange = { commentReplyText = it },
                                onCancel = {
                                    commentReplyTarget = null
                                    commentReplyText = ""
                                },
                                onSend = {
                                    if (!loggedIn) {
                                        onRequireLogin()
                                    } else if (commentReplyText.isNotBlank() && !commentSending) {
                                        onSendComment(commentReplyText, comment.id)
                                        commentReplyText = ""
                                        commentReplyTarget = null
                                    }
                                },
                            )
                        }
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
                                "写下你的评论",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                    )
                    TextButton(
                        enabled = commentText.isNotBlank() && !commentSending,
                        onClick = {
                            if (!loggedIn) {
                                onRequireLogin()
                                return@TextButton
                            }
                            onSendComment(commentText, null)
                            commentText = ""
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

/** 楼中楼「回复」：输入框直接出现在被回复的评论下方。 */
@Composable
private fun InlineCommentReply(
    target: ReplyComment,
    text: String,
    sending: Boolean,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 26.dp, top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "回复 ${target.author.name ?: target.author.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onCancel) { Text("取消") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("回复这条评论…", style = MaterialTheme.typography.bodySmall)
                },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
            )
            TextButton(
                enabled = text.isNotBlank() && !sending,
                onClick = onSend,
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("发送")
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: ReplyComment,
    loggedIn: Boolean,
    isSelf: Boolean,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenAuthor: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val liked = comment.viewer?.hasLiked == true
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenAuthor),
            ) {
                AvatarImage(comment.author.avatar?.url, Modifier.size(20.dp), seed = comment.author.name)
                Spacer(Modifier.width(6.dp))
                Text(
                    comment.author.name ?: comment.author.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            comment.inReplyToUser?.let { target ->
                Text(
                    " 回复 ${target.name ?: target.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                fullDateTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (comment.viewer?.canEdit == true || comment.viewer?.canDelete == true) {
                Box {
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
                Text(
                    "回复",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
