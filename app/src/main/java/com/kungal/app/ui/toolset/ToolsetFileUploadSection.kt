package com.kungal.app.ui.toolset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 工具资源文件上传区（BP-03 U6）：今日额度、选择压缩包、上传进度、
 * 未完成上传续传、已完成文件复用。
 */
@Composable
fun ToolsetFileUploadSection(
    state: ToolsetUploadState,
    onPick: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onResume: (ToolsetUploadRecord) -> Unit,
    onUseFinished: (ToolsetUploadRecord) -> Unit,
    onRemove: (ToolsetUploadRecord) -> Unit,
    onClearPicked: () -> Unit,
    onRefreshQuota: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuotaSummary(state, onRefreshQuota)
        if (state.artifactId != null) {
            CompletedPanel(state = state, onClear = onClearPicked)
        } else {
            PickPanel(state = state, onPick = onPick, onStart = onStart, onCancel = onCancel, onClear = onClearPicked)
        }

        val pending = state.records.filter {
            !it.completed && it.id != state.uploadId && it.id != state.resumedRecord?.id
        }
        if (pending.isNotEmpty() && !state.uploadInProgress) {
            Text(
                "未完成的上传",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            pending.forEach { record ->
                UploadRecordRow(
                    record = record,
                    actionLabel = "继续上传",
                    onAction = { onResume(record) },
                    onRemove = { onRemove(record) },
                )
            }
        }

        val finished = state.records.filter { it.completed && it.id != state.artifactId }
        if (finished.isNotEmpty()) {
            Text(
                "已上传完成的文件",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "此前已传完的文件可以直接填写信息发布；如果发布时提示失效，需要重新上传。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            finished.forEach { record ->
                UploadRecordRow(
                    record = record,
                    actionLabel = "使用",
                    onAction = { onUseFinished(record) },
                    onRemove = { onRemove(record) },
                )
            }
        }
    }
}

@Composable
private fun QuotaSummary(state: ToolsetUploadState, onRefreshQuota: () -> Unit) {
    val quota = state.quota
    when {
        state.quotaLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text("正在读取今日上传额度…", style = MaterialTheme.typography.labelSmall)
        }
        quota != null -> {
            Text(
                "今日已用 ${formatUploadMb(quota.usedToday)} / ${formatUploadMb(quota.dailyBudget)} MB" +
                    " · 剩余 ${formatUploadMb(quota.dailyRemaining)} MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = {
                    if (quota.dailyBudget <= 0) 0f
                    else (quota.usedToday.toFloat() / quota.dailyBudget).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(
                "单文件上限 ${formatUploadMb(quota.maxSingleFile)} MB · 仅支持 .7z / .zip / .rar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.quotaError ?: "读取上传额度失败",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefreshQuota) { Text("重试") }
        }
    }
}

@Composable
private fun PickPanel(
    state: ToolsetUploadState,
    onPick: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    val picked = state.picked
    Text(
        "对象存储 (S3)：站内托管，直链不限速下载。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (picked == null) {
        FilledTonalButton(onClick = onPick, enabled = !state.uploadInProgress) {
            Text("选择压缩包")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        picked.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    formatToolsetFileSize(picked.size) +
                        " · " +
                        if (picked.size > MAX_SMALL_FILE_SIZE) "分片上传" else "单次上传",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.resumedRecord != null && state.status == ToolsetUploadStatus.IDLE) {
                    Text(
                        "检测到未完成的上传，将从断点继续（已上传 ${state.resumedRecord.progress}%）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (state.uploadInProgress || state.progress > 0) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
                Text(
                    statusText(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        state.error != null -> MaterialTheme.colorScheme.error
                        state.status == ToolsetUploadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.uploadInProgress) {
                        TextButton(onClick = onCancel) { Text("取消上传") }
                    } else {
                        Button(onClick = onStart) {
                            Text(
                                when {
                                    state.status == ToolsetUploadStatus.COMPLETED -> "重新上传"
                                    state.resumedRecord != null -> "继续上传"
                                    else -> "开始上传"
                                },
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = onPick) { Text("更换文件") }
                        TextButton(onClick = onClear) { Text("移除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedPanel(state: ToolsetUploadState, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    state.artifactName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                formatToolsetFileSize(state.artifactSize) + " · 上传完成",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "填写下方的解压码和备注后即可发布。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onClear) { Text("移除文件") }
        }
    }
}

@Composable
private fun UploadRecordRow(
    record: ToolsetUploadRecord,
    actionLabel: String,
    onAction: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                record.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatToolsetFileSize(record.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!record.completed) {
            LinearProgressIndicator(
                progress = { record.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Text(
                "已上传 ${record.progress}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onAction) { Text(actionLabel) }
            TextButton(onClick = onRemove) {
                Text(if (record.completed) "移除记录" else "彻底删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun statusText(state: ToolsetUploadState): String = when {
    state.error != null -> state.error
    state.status == ToolsetUploadStatus.INITIALIZING -> "正在初始化上传…"
    state.status == ToolsetUploadStatus.UPLOADING -> "正在上传 ${state.progress}%"
    state.status == ToolsetUploadStatus.COMPLETING -> "正在完成上传…"
    state.status == ToolsetUploadStatus.COMPLETED -> "上传完成"
    state.resumedRecord != null -> "已上传 ${state.resumedRecord.progress}%，等待继续"
    else -> "等待开始上传"
}
