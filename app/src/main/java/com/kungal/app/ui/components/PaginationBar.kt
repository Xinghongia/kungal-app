package com.kungal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 页码分页器，对齐网页底部的 KunPagination：
 * `‹ 1 2 3 4 … 406 ›` + 跳转到指定页。只在页码式接口的列表使用；
 * 游标式接口（话题流、通知、私信等）保持网页的「加载更多」行为。
 */
@Composable
fun PaginationBar(
    page: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (totalPages <= 1) return
    var showJump by remember { mutableStateOf(false) }
    val current = page.coerceIn(1, totalPages)

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = { if (current > 1) onPageChange(current - 1) },
                enabled = enabled && current > 1,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一页",
                )
            }
            pageTokens(current, totalPages).forEach { token ->
                if (token == null) {
                    Text(
                        "…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                } else {
                    PageNumber(
                        number = token,
                        selected = token == current,
                        enabled = enabled,
                        onClick = { if (token != current) onPageChange(token) },
                    )
                }
            }
            IconButton(
                onClick = { if (current < totalPages) onPageChange(current + 1) },
                enabled = enabled && current < totalPages,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一页",
                )
            }
        }
        trailing?.invoke()
        TextButton(onClick = { showJump = true }, enabled = enabled) {
            Text("跳转", style = MaterialTheme.typography.labelMedium)
        }
    }

    if (showJump) {
        PageJumpDialog(
            totalPages = totalPages,
            onJump = { target ->
                showJump = false
                if (target != current) onPageChange(target)
            },
            onDismiss = { showJump = false },
        )
    }
}

/** 总页数计算：total 为服务端总数，pageSize 为每页条数。 */
fun pageCount(total: Int, pageSize: Int): Int =
    if (total <= 0 || pageSize <= 0) 0 else ceil(total.toDouble() / pageSize).toInt()

@Composable
private fun PageNumber(
    number: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(36.dp),
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PageJumpDialog(
    totalPages: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val target = input.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到页数") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value.filter(Char::isDigit).take(6)
                    },
                    label = { Text("页码") },
                    placeholder = { Text("1 - $totalPages") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            target?.takeIf { it in 1..totalPages }?.let(onJump)
                        },
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "共 $totalPages 页",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { target?.takeIf { it in 1..totalPages }?.let(onJump) },
                enabled = target != null && target in 1..totalPages,
            ) { Text("跳转") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 页码序列：首尾固定，当前页前后各两页，间隔用 null 表示省略号。 */
internal fun pageTokens(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (1..total).toList()
    val tokens = mutableListOf<Int?>()
    tokens.add(1)
    val start = max(2, current - 2)
    val end = min(total - 1, current + 2)
    if (start > 2) tokens.add(null)
    for (page in start..end) tokens.add(page)
    if (end < total - 1) tokens.add(null)
    tokens.add(total)
    return tokens
}
