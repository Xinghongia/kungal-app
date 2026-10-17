package com.kungal.app.ui.galgame

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ---------- 标题与别名 ----------

@Composable
fun TitlesEditor(
    value: JsonElement?,
    onChange: (JsonArray) -> Unit,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }
    val officials = rows.withIndex().filter { it.value.int("kind") == 0 }
    val others = rows.withIndex().filter { it.value.int("kind") != 0 }

    fun emit(newRows: List<JsonObject>) = onChange(JsonArray(newRows))

    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        emit(newRows.map { JsonObject(it) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "官方名 · 每种语言一条（至少一条）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (officials.isEmpty()) {
            Text(
                "还没有官方名——catalog 要求至少保留一条。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        officials.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EditStringSelect(
                    options = EditOfficialLanguageOptions,
                    selected = entry.value.string("lang"),
                    onSelect = { lang -> updateAt(entry.index) { it["lang"] = kotlinx.serialization.json.JsonPrimitive(lang) } },
                )
                OutlinedTextField(
                    value = entry.value.string("title"),
                    onValueChange = { text ->
                        updateAt(entry.index) { it["title"] = kotlinx.serialization.json.JsonPrimitive(text) }
                    },
                    placeholder = { Text("官方标题") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val newRows = rows.toMutableList().also { it.removeAt(entry.index) }
                        emit(newRows)
                    },
                    enabled = officials.size > 1,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "删除")
                }
            }
        }
        TextButton(
            onClick = {
                emit(
                    rows + buildJsonObject {
                        put("lang", "ja")
                        put("title", "")
                        put("kind", 0)
                    },
                )
            },
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("添加官方名")
        }

        Text(
            "别名与缩写（${others.size}）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        others.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EditStringSelect(
                    options = EditLanguageOptions,
                    selected = entry.value.string("lang"),
                    onSelect = { lang -> updateAt(entry.index) { it["lang"] = kotlinx.serialization.json.JsonPrimitive(lang) } },
                )
                OutlinedTextField(
                    value = entry.value.string("title"),
                    onValueChange = { text ->
                        updateAt(entry.index) { it["title"] = kotlinx.serialization.json.JsonPrimitive(text) }
                    },
                    placeholder = { Text("别名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                EditIntSelect(
                    options = EditTitleKindOptions.drop(1),
                    selected = entry.value.int("kind"),
                    onSelect = { kind -> updateAt(entry.index) { it["kind"] = kotlinx.serialization.json.JsonPrimitive(kind) } },
                )
                IconButton(onClick = { emit(rows.toMutableList().also { it.removeAt(entry.index) }) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "删除")
                }
            }
        }
        AliasAdder { title ->
            emit(
                rows + buildJsonObject {
                    put("lang", "")
                    put("title", title)
                    put("kind", 1)
                },
            )
        }
    }
}

@Composable
private fun AliasAdder(onAdd: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("输入别名后添加") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    onAdd(text)
                    draft = ""
                }
            },
            enabled = draft.isNotBlank(),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("添加")
        }
    }
}

// ---------- 介绍 ----------

@Composable
fun IntrosEditor(
    value: JsonElement?,
    onChange: (JsonArray) -> Unit,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }
    val byLang = rows.associateBy { it.string("lang") }

    fun setText(lang: String, text: String) {
        val next = buildList {
            addAll(rows.filterNot { it.string("lang") == lang })
            if (text.trim().isNotEmpty()) {
                add(buildJsonObject {
                    put("lang", lang)
                    put("intro", text)
                })
            }
        }
        onChange(JsonArray(next))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditOfficialLanguageOptions.forEach { (lang, label) ->
            Text(label, style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = byLang[lang]?.string("intro") ?: "",
                onValueChange = { setText(lang, it) },
                placeholder = { Text("支持 Markdown；留空表示不提交该语言") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- 实体引用（系列 / 标签 / 引擎） ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RefIdsEditor(
    values: List<String>,
    nameOf: (String) -> String,
    onChange: (JsonArray) -> Unit,
    pickerTitle: String,
    addLabel: String,
    search: suspend (String) -> List<PickerItem>,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (values.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                values.forEach { id ->
                    AssistChip(
                        onClick = { },
                        label = { Text(nameOf(id)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onChange(JsonArray(values.filter { it != id }.map { idToJson(it) })) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "移除", modifier = Modifier.size(14.dp))
                            }
                        },
                    )
                }
            }
        }
        TextButton(onClick = { showPicker = true }) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(addLabel)
        }
    }
    if (showPicker) {
        EntityPickerDialog(
            title = pickerTitle,
            search = search,
            onPick = { item ->
                if (item.id !in values) {
                    onChange(JsonArray((values + item.id).map { idToJson(it) }))
                }
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** catalog 的引用 id 以数字发送（与网页 entity-picker 的 Number(id) 一致）。 */
private fun idToJson(id: String): kotlinx.serialization.json.JsonElement =
    id.toLongOrNull()?.let { kotlinx.serialization.json.JsonPrimitive(it) }
        ?: kotlinx.serialization.json.JsonPrimitive(id)

// ---------- 会社 ----------

@Composable
fun LabelsEditor(
    value: JsonElement?,
    names: EditNames,
    onChange: (JsonArray) -> Unit,
    search: suspend (String) -> List<PickerItem>,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }
    var showPicker by remember { mutableStateOf(false) }

    fun emit(newRows: List<JsonObject>) = onChange(JsonArray(newRows))
    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        emit(newRows.map { JsonObject(it) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    names.company(row.string("label_id")),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                EditIntSelect(
                    options = EditLabelKindOptions,
                    selected = row.int("kind"),
                    onSelect = { kind -> updateAt(index) { it["kind"] = kotlinx.serialization.json.JsonPrimitive(kind) } },
                )
                IconButton(onClick = { emit(rows.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "移除")
                }
            }
        }
        TextButton(onClick = { showPicker = true }) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("搜索会社名称添加")
        }
    }
    if (showPicker) {
        EntityPickerDialog(
            title = "选择会社",
            search = search,
            onPick = { item ->
                if (rows.none { it.string("label_id") == item.id }) {
                    emit(
                        rows + buildJsonObject {
                            put("label_id", item.id.toLongOrNull() ?: 0L)
                            put("kind", 2)
                        },
                    )
                }
            },
            onDismiss = { showPicker = false },
        )
    }
}

// ---------- 出演名单 ----------

@Composable
fun RosterEditor(
    value: JsonElement?,
    suppressed: List<String>,
    names: EditNames,
    onChange: (JsonArray) -> Unit,
    onSuppressedChange: (JsonArray) -> Unit,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }
    val hidden = suppressed.toSet()

    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        onChange(JsonArray(newRows.map { JsonObject(it) }))
    }

    fun toggleHidden(characterId: String) {
        val key = "roster:$characterId"
        val next = if (key in hidden) suppressed - key else suppressed + key
        onSuppressedChange(JsonArray(next.map { kotlinx.serialization.json.JsonPrimitive(it) }))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rows.isEmpty()) {
            Text(
                "这部作品还没有出演名单。出演边由上游导入，不能在这里新建。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        rows.forEachIndexed { index, row ->
            val characterId = row.string("character_id")
            val isHidden = "roster:$characterId" in hidden
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    names.character(characterId),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (isHidden) TextDecoration.LineThrough else null,
                    color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                EditIntSelect(
                    options = EditRosterKindOptions,
                    selected = row.int("kind"),
                    onSelect = { kind -> updateAt(index) { it["kind"] = kotlinx.serialization.json.JsonPrimitive(kind) } },
                    modifier = Modifier.size(width = 88.dp, height = 40.dp),
                )
                EditIntSelect(
                    options = EditSpoilerOptions,
                    selected = row.int("spoiler"),
                    onSelect = { level -> updateAt(index) { it["spoiler"] = kotlinx.serialization.json.JsonPrimitive(level) } },
                    modifier = Modifier.size(width = 88.dp, height = 40.dp),
                )
                TextButton(onClick = { toggleHidden(characterId) }) {
                    Text(if (isHidden) "取消隐藏" else "隐藏")
                }
            }
        }
    }
}

// ---------- 本站署名 ----------

@Composable
fun CreditsEditor(
    value: JsonElement?,
    names: EditNames,
    onChange: (JsonArray) -> Unit,
    searchStaff: suspend (String) -> List<PickerItem>,
    searchCharacters: suspend (String) -> List<PickerItem>,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }
    var addRole by remember { mutableStateOf<Int?>(null) }
    var characterRow by remember { mutableStateOf<Int?>(null) }
    var staffRow by remember { mutableStateOf<Int?>(null) }

    fun emit(newRows: List<JsonObject>) = onChange(JsonArray(newRows))
    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        emit(newRows.map { JsonObject(it) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EditIntSelect(
                        options = EditCreditRoles,
                        selected = row.int("role_id"),
                        onSelect = { role -> updateAt(index) { it["role_id"] = kotlinx.serialization.json.JsonPrimitive(role) } },
                    )
                    TextButton(onClick = { staffRow = index }, modifier = Modifier.weight(1f)) {
                        Text(names.staffOf(row.string("credit_name_id")))
                    }
                    IconButton(onClick = { emit(rows.toMutableList().also { it.removeAt(index) }) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "删除")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val characterId = row.string("character_id")
                    TextButton(onClick = { characterRow = index }, modifier = Modifier.weight(1f)) {
                        Text(
                            if (characterId.isBlank()) "配音角色（可选）"
                            else names.character(characterId),
                        )
                    }
                    if (characterId.isNotBlank()) {
                        IconButton(onClick = { updateAt(index) { it.remove("character_id") } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "清除角色")
                        }
                    }
                    OutlinedTextField(
                        value = row.string("note"),
                        onValueChange = { note ->
                            updateAt(index) {
                                if (note.isBlank()) it.remove("note")
                                else it["note"] = kotlinx.serialization.json.JsonPrimitive(note)
                            }
                        },
                        placeholder = { Text("备注（可选）") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }
        }
        TextButton(onClick = { addRole = 247 }) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("添加本站署名")
        }
    }

    addRole?.let {
        EntityPickerDialog(
            title = "选择 Staff 名义",
            search = searchStaff,
            onPick = { item ->
                emit(
                    rows + buildJsonObject {
                        put("role_id", it)
                        put("credit_name_id", item.id.toLongOrNull() ?: 0L)
                    },
                )
                addRole = null
            },
            onDismiss = { addRole = null },
        )
    }
    staffRow?.let { index ->
        EntityPickerDialog(
            title = "选择 Staff 名义",
            search = searchStaff,
            onPick = { item ->
                updateAt(index) { it["credit_name_id"] = kotlinx.serialization.json.JsonPrimitive(item.id.toLongOrNull() ?: 0L) }
                staffRow = null
            },
            onDismiss = { staffRow = null },
        )
    }
    characterRow?.let { index ->
        EntityPickerDialog(
            title = "配音角色（可选）",
            search = searchCharacters,
            onPick = { item ->
                updateAt(index) { it["character_id"] = kotlinx.serialization.json.JsonPrimitive(item.id.toLongOrNull() ?: 0L) }
                characterRow = null
            },
            onDismiss = { characterRow = null },
        )
    }
}
