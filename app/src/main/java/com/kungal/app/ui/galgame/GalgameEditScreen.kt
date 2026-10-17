package com.kungal.app.ui.galgame

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kungal.core.model.EditField
import com.kungal.core.model.EditForm
import com.kungal.core.model.EditProposalSummary
import com.kungal.core.model.EditVocabulary
import com.kungal.core.model.Work
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun GalgameEditScreen(
    app: KungalApp,
    workId: String,
    nsfwDisplay: com.kungal.app.NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMyProposals: () -> Unit,
    onOpenReviewQueue: () -> Unit,
    onOpenProposal: (String) -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    val vm: GalgameEditViewModel = viewModel(key = "galgame-edit-$workId") {
        GalgameEditViewModel(app, workId, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    val form = state.form
    var note by rememberSaveable { mutableStateOf("") }
    var showNote by rememberSaveable { mutableStateOf(true) }
    val groups = remember(form) { editableGroups(form) }
    var selectedGroup by rememberSaveable { mutableStateOf("") }
    val activeGroup = groups.firstOrNull { it.name == selectedGroup } ?: groups.firstOrNull()
    var lastDirty by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.dirtyCount) {
        if (lastDirty > 0 && state.dirtyCount == 0) note = ""
        lastDirty = state.dirtyCount
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "编辑资料",
                subtitle = state.work?.let { workTitle(it) } ?: "#$workId",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (state.dirtyCount == 0) {
                                vm.load()
                            } else {
                                com.kungal.app.ui.components.AppMessages.show("请先提交或撤销修改再刷新")
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        bottomBar = {
            if (form != null) {
                EditSubmitBar(
                    state = state,
                    note = note,
                    showNote = showNote,
                    onNoteChange = { note = it },
                    onToggleNote = { showNote = !showNote },
                    onReset = {
                        vm.resetChanges()
                        note = ""
                    },
                    onSubmit = { vm.submit(note) },
                )
            }
        },
    ) { padding ->
        when {
            !app.isLoggedIn -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("登录后才能编辑资料", style = MaterialTheme.typography.titleMedium)
                Text(
                    "资料修改会以你的账号提交；未登录时只能查看词条。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenLogin) { Text("去登录") }
            }
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null -> ErrorBox(
                state.error!!,
                onRetry = { vm.load() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            form == null -> EmptyBox("没有可编辑的表单", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    EditHeaderCard(
                        work = state.work,
                        workId = workId,
                        canReviewQueue = state.canReviewQueue,
                        onOpenWork = onOpenWork,
                        onOpenHistory = onOpenHistory,
                        onOpenMyProposals = onOpenMyProposals,
                        onOpenReviewQueue = onOpenReviewQueue,
                    )
                }
                val mine = state.openProposals.filter { it.viewer?.isProposer == true }
                val reviewable = state.openProposals.filter {
                    it.viewer?.isProposer != true && it.viewer?.canDecide == true
                }
                if (state.proposalsError != null) {
                    item(key = "proposals-error") {
                        Text(
                            "无法读取待审提案：${state.proposalsError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (mine.isNotEmpty() || reviewable.isNotEmpty()) {
                    item(key = "proposal-title") {
                        Text(
                            "这部作品的待审提案",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                items(mine.size) { index ->
                    val item = mine[index]
                    OpenProposalCard(
                        item = item,
                        mine = true,
                        submitting = state.submittingProposalId == item.id,
                        onOpen = { onOpenProposal(item.id) },
                        onWithdraw = { vm.withdraw(item.id) },
                    )
                }
                items(reviewable.size) { index ->
                    val item = reviewable[index]
                    OpenProposalCard(
                        item = item,
                        mine = false,
                        submitting = false,
                        onOpen = { onOpenProposal(item.id) },
                        onWithdraw = null,
                    )
                }
                if (groups.isNotEmpty()) {
                    item(key = "group-tabs") {
                        ScrollableTabRow(
                            selectedTabIndex = groups.indexOf(activeGroup).coerceAtLeast(0),
                            edgePadding = 12.dp,
                        ) {
                            groups.forEach { group ->
                                Tab(
                                    selected = group == activeGroup,
                                    onClick = { selectedGroup = group.name },
                                    text = { Text(group.label, style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                        }
                    }
                    val fields = form.fields.filter {
                        editGroupOf(it.key) == activeGroup && it.key != "catalog.work.roster.suppressed"
                    }
                    items(fields.size) { index ->
                        val field = fields[index]
                        EditFieldBlock(
                            field = field,
                            form = form,
                            state = state,
                            onChange = { value -> vm.updateValue(field.key, value) },
                            onChangeKey = { key, value -> vm.updateValue(key, value) },
                            upload = { bytes, name, mime, preset ->
                                vm.uploadImage(bytes, name, mime, preset)
                            },
                            searchTags = { q -> app.searchTags(q) },
                            searchCompanies = { q -> app.searchCompanies(q) },
                            searchSeries = { q -> app.searchSeries(q) },
                            searchEngines = { q -> app.searchEngines(q) },
                            searchCharacters = { q -> app.searchCharacters(q) },
                            searchStaff = { q -> app.searchStaff(q) },
                        )
                    }
                }
            }
        }
    }
}

private fun editableGroups(form: EditForm?): List<EditGroup> {
    if (form == null) return emptyList()
    val present = form.fields.map { editGroupOf(it.key) }.toSet()
    return EditGroup.entries.filter { it in present }
}

@Composable
private fun EditHeaderCard(
    work: Work?,
    workId: String,
    canReviewQueue: Boolean,
    onOpenWork: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMyProposals: () -> Unit,
    onOpenReviewQueue: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                work?.let { workTitle(it) } ?: "Galgame #$workId",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "修改字段后保存：拥有直接编辑权限（管理员 / 词条创建者）时立即生效，否则进入审核队列。" +
                    "审核人可在合并前修正你的提案，双方都会署名。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onOpenWork(workId) }) { Text("返回词条") }
                TextButton(onClick = onOpenHistory) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("修订历史")
                }
                TextButton(onClick = onOpenMyProposals) {
                    Icon(Icons.Outlined.ListAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("我的提案")
                }
                if (canReviewQueue) {
                    TextButton(onClick = onOpenReviewQueue) {
                        Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("审核队列")
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenProposalCard(
    item: EditProposalSummary,
    mine: Boolean,
    submitting: Boolean,
    onOpen: () -> Unit,
    onWithdraw: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (mine) "我的待审提案" else "待审提案") + " #${item.id}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(onClick = onOpen, label = { Text(if (mine) "查看" else "审阅") })
            }
            Text(
                "${item.proposer.name ?: "用户 #${item.proposer.id}"} · 基线版本 #${item.baseRevisionSeq}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (onWithdraw != null && item.viewer?.canWithdraw == true) {
                TextButton(onClick = onWithdraw, enabled = !submitting) {
                    Text(if (submitting) "撤回中…" else "撤回提案", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EditSubmitBar(
    state: GalgameEditState,
    note: String,
    showNote: Boolean,
    onNoteChange: (String) -> Unit,
    onToggleNote: () -> Unit,
    onReset: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showNote) {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("编辑说明（可选）") },
                    placeholder = { Text("说明这次修改的内容与依据，帮助审核人更快处理") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (state.dirtyCount > 0) {
                        Text("已修改 ${state.dirtyCount} 个字段", style = MaterialTheme.typography.labelMedium)
                        Text(
                            state.dirtyKeys.joinToString("、") { editFieldLabel(it) },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text("尚未修改", style = MaterialTheme.typography.labelMedium)
                    }
                    state.submitError?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onToggleNote) { Text(if (showNote) "收起说明" else "编辑说明") }
                TextButton(onClick = onReset, enabled = state.dirtyCount > 0 && !state.submitting) {
                    Text("撤销修改")
                }
                Button(
                    onClick = onSubmit,
                    enabled = state.dirtyCount > 0 && !state.submitting,
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (state.submitting) "提交中…" else "提交修改")
                }
            }
        }
    }
}

@Composable
fun EditFieldContent(
    field: EditField,
    vocabularies: List<EditVocabulary>,
    value: JsonElement?,
    suppressedRoster: JsonElement?,
    names: EditNames,
    onChange: (JsonElement) -> Unit,
    onSuppressedChange: (JsonElement) -> Unit,
    upload: suspend (ByteArray, String, String, String) -> String?,
    searchTags: suspend (String) -> List<PickerItem>,
    searchCompanies: suspend (String) -> List<PickerItem>,
    searchSeries: suspend (String) -> List<PickerItem>,
    searchEngines: suspend (String) -> List<PickerItem>,
    searchCharacters: suspend (String) -> List<PickerItem>,
    searchStaff: suspend (String) -> List<PickerItem>,
) {
    when (field.key) {
            "catalog.work.titles" -> TitlesEditor(value) { onChange(it) }
            "catalog.work.titles.suppressed" -> EditStringListEditor(
                values = value.stringListOf(),
                onChange = { list -> onChange(JsonArray(list.map { JsonPrimitive(it) })) },
                placeholder = "粘贴 title:类型:语言:标题 后添加",
                addLabel = "添加",
            )
            "catalog.work.display_name" -> EditTextField(
                value = value.asText(),
                onChange = { onChange(JsonPrimitive(it)) },
                placeholder = "条目在跨站场合显示的单一名称",
            )
            "catalog.work.intros" -> IntrosEditor(value) { onChange(it) }
            "catalog.work.olang" -> EditStringSelect(
                options = EditOfficialLanguageOptions,
                selected = value.asText(),
                onSelect = { onChange(JsonPrimitive(it)) },
            )
            "catalog.work.content_rating" -> EditIntSelect(
                options = EditContentRatingOptions,
                selected = contentRatingCode(value),
                onSelect = { onChange(JsonPrimitive(it)) },
            )
            "catalog.work.display_nsfw" -> EditSwitchRow(
                checked = value.asBool(),
                onCheckedChange = { onChange(JsonPrimitive(it)) },
                label = "封面 / 画廊 / 简介是否含成人内容",
            )
            "catalog.work.series_ids" -> RefIdsEditor(
                values = value.stringListOf(),
                nameOf = names::seriesOf,
                onChange = { onChange(it) },
                pickerTitle = "搜索系列",
                addLabel = "搜索系列添加",
                search = searchSeries,
            )
            "catalog.work.tag_ids" -> RefIdsEditor(
                values = value.stringListOf(),
                nameOf = names::tag,
                onChange = { onChange(it) },
                pickerTitle = "搜索标签",
                addLabel = "搜索标签添加",
                search = searchTags,
            )
            "catalog.work.labels" -> LabelsEditor(
                value = value,
                names = names,
                onChange = { onChange(it) },
                search = searchCompanies,
            )
            "catalog.work.engine_ids" -> RefIdsEditor(
                values = value.stringListOf(),
                nameOf = names::engine,
                onChange = { onChange(it) },
                pickerTitle = "搜索引擎",
                addLabel = "搜索引擎添加",
                search = searchEngines,
            )
            "catalog.work.roster" -> RosterEditor(
                value = value,
                suppressed = suppressedRoster.stringListOf(),
                names = names,
                onChange = { onChange(it) },
                onSuppressedChange = { onSuppressedChange(it) },
            )
            "catalog.work.credits" -> CreditsEditor(
                value = value,
                names = names,
                onChange = { onChange(it) },
                searchStaff = searchStaff,
                searchCharacters = searchCharacters,
            )
            "catalog.work.credits.suppressed" -> EditStringListEditor(
                values = value.stringListOf(),
                onChange = { list -> onChange(JsonArray(list.map { JsonPrimitive(it) })) },
                placeholder = "粘贴 credit:职务:名义:角色 后添加",
                addLabel = "添加",
            )
            "catalog.work.links" -> EditStringListEditor(
                values = value.stringListOf(),
                onChange = { list -> onChange(JsonArray(list.map { JsonPrimitive(it) })) },
                placeholder = "粘贴 http(s) 链接后添加",
                addLabel = "添加",
            )
            "catalog.work.covers" -> CoversEditor(value, { onChange(it) }, upload)
            "catalog.work.screenshots" -> ScreenshotsEditor(value, { onChange(it) }, upload)
            else -> GenericFieldEditor(field, vocabularies, value, onChange)
        }
}

@Composable
private fun EditFieldBlock(
    field: EditField,
    form: EditForm,
    state: GalgameEditState,
    onChange: (JsonElement) -> Unit,
    onChangeKey: (String, JsonElement) -> Unit,
    upload: suspend (ByteArray, String, String, String) -> String?,
    searchTags: suspend (String) -> List<PickerItem>,
    searchCompanies: suspend (String) -> List<PickerItem>,
    searchSeries: suspend (String) -> List<PickerItem>,
    searchEngines: suspend (String) -> List<PickerItem>,
    searchCharacters: suspend (String) -> List<PickerItem>,
    searchStaff: suspend (String) -> List<PickerItem>,
) {
    EditFieldCard(editFieldLabel(field.key), fieldDescription(field.key)) {
        EditFieldContent(
            field = field,
            vocabularies = form.vocabularies,
            value = state.values[field.key],
            suppressedRoster = state.values["catalog.work.roster.suppressed"],
            names = state.names,
            onChange = onChange,
            onSuppressedChange = { onChangeKey("catalog.work.roster.suppressed", it) },
            upload = upload,
            searchTags = searchTags,
            searchCompanies = searchCompanies,
            searchSeries = searchSeries,
            searchEngines = searchEngines,
            searchCharacters = searchCharacters,
            searchStaff = searchStaff,
        )
    }
}

/**
 * roster 的隐藏键与出演名单是两个 schema 字段；RosterEditor 同时改两者，
 * 第二个字段的回调由 [onChangeKey] 转到对应的 update。
 */
private fun JsonElement?.stringListOf(): List<String> =
    asArrayOrEmpty().mapNotNull { (it as? JsonPrimitive)?.content }.filter { it.isNotBlank() }

private fun fieldDescription(key: String): String? = when (key) {
    "catalog.work.titles" -> "至少要有一条官方名；别名可以留空语言。"
    "catalog.work.titles.suppressed" -> "被隐藏的上游标题仍留在表里，读面不再出现；键由 catalog 派生，不要改格式。"
    "catalog.work.intros" -> "每种语言一条，支持 Markdown；留空表示不提交该语言。"
    "catalog.work.series_ids", "catalog.work.tag_ids", "catalog.work.engine_ids" ->
        "只管理本站补充的部分；VNDB / Bangumi 等上游数据不在这里增删。"
    "catalog.work.labels" -> "搜索会社名称添加，并选择它在本作中的身份。"
    "catalog.work.roster" -> "只能改已有出演边的主配与剧透档；隐藏会从读面拿掉这条边，行仍保留。"
    "catalog.work.credits" -> "只管理本站补充的署名；上游导入的署名不能在这里改。"
    "catalog.work.credits.suppressed" -> "上游署名的行身份键，回显即可、不要自己拼；隐藏后读面不再出现。"
    "catalog.work.covers" -> "「竖版封面」是卡片和列表渲染的那一张；拖拽顺序在网页端调整。"
    "catalog.work.screenshots" -> "点每张图的说明与分级可修改。"
    else -> null
}

@Composable
private fun GenericFieldEditor(
    field: EditField,
    vocabularies: List<EditVocabulary>,
    value: JsonElement?,
    onChange: (JsonElement) -> Unit,
) {
    val vocabulary = vocabularies.firstOrNull { it.vocabulary == field.vocabulary }
    when (field.fieldType) {
        "text" -> EditTextField(value.asText(), { onChange(JsonPrimitive(it)) })
        "i18nmap" -> EditI18nMapEditor(value) { onChange(it) }
        "ref" -> EditTextField(
            value = value.asText(),
            onChange = { text ->
                val id = text.trim()
                if (id.isBlank()) onChange(kotlinx.serialization.json.JsonNull)
                else id.toLongOrNull()?.let { onChange(JsonPrimitive(it)) }
            },
            placeholder = "实体 id",
        )
        "imagehash" -> EditTextField(
            value = value.asText(),
            onChange = { onChange(JsonPrimitive(it.trim())) },
            placeholder = "图片 hash",
        )
        "int" -> EditTextField(
            value = value.asText(),
            onChange = { text -> text.toIntOrNull()?.let { onChange(JsonPrimitive(it)) } },
            placeholder = "整数",
        )
        "date" -> EditTextField(
            value = value.asText(),
            onChange = { onChange(JsonPrimitive(it)) },
            placeholder = "YYYY-MM-DD",
        )
        "enum" -> {
            if (field.encoding == "int") {
                val options = vocabulary?.values?.mapIndexed { index, item ->
                    (field.base + index) to (item.displayName.ifBlank { item.value })
                } ?: EditContentRatingOptions
                EditIntSelect(options, value.asInt(), { onChange(JsonPrimitive(it)) })
            } else {
                val options = vocabulary?.values?.map { it.value to (it.displayName.ifBlank { it.value }) }
                    ?: listOf(value.asText() to value.asText())
                EditStringSelect(options, value.asText(), { onChange(JsonPrimitive(it)) })
            }
        }
        "list" -> {
            when (field.element?.elementType) {
                null, "text", "ref" -> EditStringListEditor(
                    values = value.stringListOf(),
                    onChange = { list -> onChange(JsonArray(list.map { JsonPrimitive(it) })) },
                    placeholder = if (field.element?.elementType == "ref") "输入实体 id 后添加" else "输入后添加",
                    addLabel = "添加",
                )
                else -> Text(
                    editValueSummary(field.key, value) + "（该字段暂未提供原生编辑器，可在网页端修改）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> Text(
            editValueSummary(field.key, value) + "（只读）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 搜索辅助 ----------

private fun nameOf(displayName: String, localized: Map<String, com.kungal.core.model.LocalizedText>, id: String) =
    localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() } ?: displayName.ifBlank { "#$id" }

suspend fun KungalApp.searchTags(q: String): List<PickerItem> =
    runCatching { entityApi.tags(q = q.ifBlank { null }, limit = 20, includeNsfw = true) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), "标签")
        }

suspend fun KungalApp.searchCompanies(q: String): List<PickerItem> =
    runCatching { entityApi.companies(q = q.ifBlank { null }, limit = 20) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), it.companyKind)
        }

suspend fun KungalApp.searchSeries(q: String): List<PickerItem> =
    runCatching { entityApi.seriesList(q = q.ifBlank { null }, limit = 20, includeNsfw = true) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), "系列")
        }

suspend fun KungalApp.searchEngines(q: String): List<PickerItem> =
    runCatching { entityApi.engines(q = q.ifBlank { null }, limit = 50) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), "引擎")
        }

suspend fun KungalApp.searchCharacters(q: String): List<PickerItem> =
    runCatching { entityApi.characters(q = q.ifBlank { null }, limit = 20, includeNsfw = true) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), "角色")
        }

suspend fun KungalApp.searchStaff(q: String): List<PickerItem> =
    runCatching { entityApi.creditNames(q = q, limit = 20) }
        .getOrNull()?.items.orEmpty().map {
            PickerItem(it.id, nameOf(it.displayName, it.localized, it.id), "Staff")
        }
