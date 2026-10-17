package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 列表页顶部的筛选胶囊：和 Galgame 资料库一致，点开后从底部弹出选项，
 * 不用锚点下拉菜单。
 */
@Composable
fun FilterPill(
    label: String,
    active: Boolean,
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = icon,
        trailingIcon = {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        },
    )
}

/**
 * 底部弹出的单/多选筛选面板。multi = false 时点一项即关闭；
 * includeAll 会额外列出「全部」并在选中时调用 onClear。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterOptionSheet(
    title: String,
    options: List<FilterOption>,
    selected: Set<String>,
    multi: Boolean,
    includeAll: Boolean = true,
    onToggle: (String) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (onClear != null) {
                    TextButton(onClick = onClear) { Text("清空") }
                }
                TextButton(onClick = onDismiss) { Text("完成") }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                if (!multi && includeAll) {
                    item {
                        FilterOptionRow(
                            label = "全部",
                            selected = selected.isEmpty(),
                            onClick = {
                                onClear?.invoke()
                                onDismiss()
                            },
                        )
                    }
                }
                items(options, key = { it.value }) { option ->
                    FilterOptionRow(
                        label = option.label,
                        selected = option.value in selected,
                        onClick = {
                            onToggle(option.value)
                            if (!multi) onDismiss()
                        },
                    )
                }
                item { Spacer(Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun FilterOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
