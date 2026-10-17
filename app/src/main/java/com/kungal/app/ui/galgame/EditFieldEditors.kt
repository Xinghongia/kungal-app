package com.kungal.app.ui.galgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** 编辑字段的统一卡片容器：标题、说明与内容。 */
@Composable
fun EditFieldCard(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
fun EditTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
    )
}

/** 下拉选择：值为 Int 的固定词表（标题类型、会社身份、剧透档等）。 */
@Composable
fun EditIntSelect(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }) {
            Text(options.firstOrNull { it.first == selected }?.second ?: "#$selected")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

/** 下拉选择：值为 String 的词表（语言、分级 token 等）。 */
@Composable
fun EditStringSelect(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }) {
            Text(options.firstOrNull { it.first == selected }?.second ?: selected.ifBlank { "未设置" })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
fun EditSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** i18nmap 字段：按语言编辑一条文本（未知语言原样保留）。 */
@Composable
fun EditI18nMapEditor(
    value: JsonElement?,
    onChange: (JsonObject) -> Unit,
) {
    val map = value as? JsonObject ?: JsonObject(emptyMap())
    val languages = (EditLanguageOptions.map { it.first } + map.keys)
        .filter { it.isNotBlank() }
        .distinct()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        languages.forEach { lang ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    editLanguageLabel(lang),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = (map[lang] as? JsonPrimitive)?.content.orEmpty(),
                    onValueChange = { text ->
                        val next = map.toMutableMap()
                        if (text.isBlank()) next.remove(lang) else next[lang] = JsonPrimitive(text)
                        onChange(JsonObject(next))
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 字符串列表编辑：已有项做成可删除的 chip，底部输入框回车/点加号添加。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditStringListEditor(
    values: List<String>,
    onChange: (List<String>) -> Unit,
    placeholder: String,
    addLabel: String = "添加",
) {
    var draft by remember { mutableStateOf("") }
    fun add() {
        val text = draft.trim()
        if (text.isEmpty()) return
        if (text !in values) onChange(values + text)
        draft = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (values.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                values.forEach { value ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                if (value.length > 80) value.take(80) + "…" else value,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { onChange(values - value) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "删除", modifier = Modifier.size(14.dp))
                            }
                        },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            TextButton(onClick = { add() }, enabled = draft.isNotBlank()) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(addLabel)
            }
        }
    }
}
