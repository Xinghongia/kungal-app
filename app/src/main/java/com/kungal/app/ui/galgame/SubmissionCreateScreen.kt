package com.kungal.app.ui.galgame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import java.time.LocalDate
import java.util.UUID

private val SubmissionSteps = listOf(
    "基本信息",
    "游戏介绍",
    "分级元数据",
    "预览图与提交",
)

/**
 * 新建 Galgame 申请向导（对齐网页 /edit/galgame/create 的四步表单）。
 * 只用于资料库确实没有的作品；已有作品应回到搜索并打开词条发布资源。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionCreateScreen(
    app: KungalApp,
    initialTitle: String,
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onSubmittedLive: (workId: String) -> Unit,
    onSubmittedPending: () -> Unit,
) {
    val vm: SubmissionCreateViewModel = viewModel { SubmissionCreateViewModel(app, initialTitle) }
    val state by vm.state.collectAsState()
    val draft = state.draft
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.submitted) {
        val created = state.submitted ?: return@LaunchedEffect
        vm.consumeSubmitted()
        if (created.state == "live") onSubmittedLive(created.workId) else onSubmittedPending()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "提交 Galgame 申请",
                subtitle = "仅用于资料库未收录的原创 / 同人作品",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            if (app.isLoggedIn) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { vm.previous() },
                            enabled = state.step > 1 && !state.submitting,
                        ) { Text("上一步") }
                        Spacer(Modifier.weight(1f))
                        if (state.step < SubmissionCreateViewModel.LAST_STEP) {
                            Button(onClick = { vm.next() }) { Text("下一步") }
                        } else {
                            Button(
                                onClick = { confirmSubmit = true },
                                enabled = !state.submitting,
                            ) {
                                if (state.submitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (state.submitting) "提交中…" else "提交 Galgame 申请")
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (!app.isLoggedIn) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("登录后才能提交 Galgame 申请", style = MaterialTheme.typography.titleMedium)
                Text(
                    "申请会以你的账号进入审核队列，审核结果通过站内消息通知。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenLogin) { Text("去登录") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SubmissionStepIndicator(step = state.step, onGoTo = vm::goTo)
            HorizontalDivider()

            when (state.step) {
                1 -> SubmissionTitlesStep(
                    draft = draft,
                    onTitleChange = vm::setTitle,
                    onAddAlias = vm::addAlias,
                    onRemoveAlias = vm::removeAlias,
                )
                2 -> SubmissionIntroductionsStep(
                    draft = draft,
                    onIntroChange = vm::setIntroduction,
                )
                3 -> SubmissionMetaStep(
                    draft = draft,
                    onContentLimit = vm::setContentLimit,
                    onAgeLimit = vm::setAgeLimitR18,
                    onOriginalLanguage = vm::setOriginalLanguage,
                    onReleaseDate = vm::setReleaseDate,
                    onReleaseDateTba = vm::setReleaseDateTba,
                )
                else -> SubmissionBannerStep(
                    app = app,
                    bannerHash = draft.bannerHash,
                    onBannerHash = vm::setBannerHash,
                )
            }

            Text(
                "提交后进入审核队列，通过后才会公开；审核期间可在「我的提交」继续修改或撤回。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
        }
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("确定提交 Galgame 申请吗？") },
            text = {
                Text("提交后将进入审核队列，审核通过后才会被公开展示。审核结果会通过站内消息通知你。")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSubmit = false
                    vm.submit()
                }) { Text("提交") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSubmit = false }) { Text("取消") }
            },
        )
    }

    state.duplicateConfirm?.let {
        AlertDialog(
            onDismissRequest = { vm.cancelDuplicate() },
            title = { Text("资料库中已有同名作品") },
            text = {
                Text(
                    "请先回到「发布 Galgame」搜索确认这不是同一部作品——重复条目会被驳回。" +
                        "确实是不同作品（例如同名重制版或同人作品）才继续提交。",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmDuplicate() }) { Text("仍然提交") }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelDuplicate() }) { Text("返回确认") }
            },
        )
    }
}

@Composable
private fun SubmissionStepIndicator(step: Int, onGoTo: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubmissionSteps.forEachIndexed { index, label ->
            val number = index + 1
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onGoTo(number) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                number == step -> MaterialTheme.colorScheme.primary
                                number < step -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            number == step -> MaterialTheme.colorScheme.onPrimary
                            number < step -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (number == step) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (index < SubmissionSteps.lastIndex) {
                HorizontalDivider(Modifier.weight(1f).padding(horizontal = 2.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmissionTitlesStep(
    draft: SubmissionFormDraft,
    onTitleChange: (String, String) -> Unit,
    onAddAlias: (String) -> Unit,
    onRemoveAlias: (String) -> Unit,
) {
    var aliasInput by rememberSaveable { mutableStateOf("") }

    fun addAlias() {
        onAddAlias(aliasInput)
        aliasInput = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("游戏名", "至少填写一种语言，建议尽量补全（没有官方译名的语言可留空）。")
        SubmissionLocales.forEach { locale ->
            OutlinedTextField(
                value = draft.titles[locale.tag].orEmpty(),
                onValueChange = { onTitleChange(locale.tag, it) },
                label = { Text(locale.label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.size(4.dp))
        SectionTitle("别名", "常用译名、罗马音、缩写等，方便别人搜到；名称与别名合计最多 100 个。")
        OutlinedTextField(
            value = aliasInput,
            onValueChange = { aliasInput = it },
            label = { Text("添加别名") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addAlias() }),
            trailingIcon = {
                TextButton(onClick = { addAlias() }) { Text("添加") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.aliases.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.aliases.forEach { alias ->
                    AssistChip(
                        onClick = { onRemoveAlias(alias) },
                        label = { Text(alias) },
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "删除别名",
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmissionIntroductionsStep(
    draft: SubmissionFormDraft,
    onIntroChange: (String, String) -> Unit,
) {
    var selectedTag by rememberSaveable { mutableStateOf("ja") }
    val selected = SubmissionLocales.firstOrNull { it.tag == selectedTag } ?: SubmissionLocales[1]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("游戏介绍", "至少填写一种语言。可参考官方页面、DLsite 或 Bangumi，也可以自行撰写。")
        androidx.compose.material3.ScrollableTabRow(
            selectedTabIndex = SubmissionLocales.indexOf(selected),
            edgePadding = 0.dp,
        ) {
            SubmissionLocales.forEach { locale ->
                Tab(
                    selected = locale.tag == selected.tag,
                    onClick = { selectedTag = locale.tag },
                    text = { Text(locale.label) },
                )
            }
        }
        OutlinedTextField(
            value = draft.introductions[selected.tag].orEmpty(),
            onValueChange = { onIntroChange(selected.tag, it) },
            placeholder = { Text("${selected.label}介绍") },
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "介绍里不要放 R18 图片；露骨的图片一律按 R18 处理。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmissionMetaStep(
    draft: SubmissionFormDraft,
    onContentLimit: (String) -> Unit,
    onAgeLimit: (Boolean) -> Unit,
    onOriginalLanguage: (String) -> Unit,
    onReleaseDate: (String) -> Unit,
    onReleaseDateTba: (Boolean) -> Unit,
) {
    var languageSheet by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            "内容限制",
            "只要游戏名、介绍或封面有不宜在公共场所显示的内容，就选 NSFW；越严越好。",
        )
        SubmissionChoiceRow(
            selected = draft.contentLimit == "sfw",
            title = "SFW（可以在公共场所打开）",
            description = "游戏名、介绍、封面都没有不适宜在公共场所显示的内容",
            onClick = { onContentLimit("sfw") },
        )
        SubmissionChoiceRow(
            selected = draft.contentLimit == "nsfw",
            title = "NSFW（不适合在公共场所打开）",
            description = "游戏名、介绍、封面任意一项露骨，或封面需要打码才能放上来",
            onClick = { onContentLimit("nsfw") },
        )

        HorizontalDivider()
        SectionTitle("游戏基础信息", "默认假设是日语原版、全年龄；与默认不同时请在此调整。")
        Text("年龄分级", style = MaterialTheme.typography.labelLarge)
        SubmissionChoiceRow(
            selected = !draft.ageLimitR18,
            title = "全年龄",
            description = "本游戏不含成人内容",
            onClick = { onAgeLimit(false) },
        )
        SubmissionChoiceRow(
            selected = draft.ageLimitR18,
            title = "R18",
            description = "本游戏含成人内容",
            onClick = { onAgeLimit(true) },
        )

        Text("原始语言", style = MaterialTheme.typography.labelLarge)
        FilterPill(
            label = submissionOriginalLanguageLabel(draft.originalLanguage),
            active = true,
            onClick = { languageSheet = true },
        )

        Text("发售日期（可留空）", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.releaseDate,
                onValueChange = onReleaseDate,
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                enabled = !draft.releaseDateTba,
                trailingIcon = {
                    IconButton(onClick = { datePicker = true }, enabled = !draft.releaseDateTba) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "选择日期")
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.releaseDateTba,
                onCheckedChange = { onReleaseDateTba(it) },
            )
            Spacer(Modifier.width(8.dp))
            Text("发售日期待定（TBA）", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (languageSheet) {
        FilterOptionSheet(
            title = "原始语言",
            options = SubmissionOriginalLanguages.map { FilterOption(it.first, it.second) },
            selected = setOf(draft.originalLanguage),
            multi = false,
            includeAll = false,
            onToggle = onOriginalLanguage,
            onClear = null,
            onDismiss = { languageSheet = false },
        )
    }

    if (datePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(draft.releaseDate).toEpochDay() * 86_400_000L
        }.getOrNull()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        onReleaseDate(LocalDate.ofEpochDay(millis / 86_400_000L).toString())
                    }
                    datePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { datePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmissionChoiceRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubmissionBannerStep(
    app: KungalApp,
    bannerHash: String?,
    onBannerHash: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            "预览图",
            "尽量在游戏内截取原始分辨率，不要拍屏；宽度大于高度为佳，最终会被压缩到不大于 1920 × 1080。",
        )
        if (bannerHash != null) {
            KungalImage(
                url = editImageUrl(bannerHash),
                contentDescription = "预览图",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditImageUploadButton(
                    label = "更换封面",
                    preset = "cover",
                    upload = { bytes, fileName, contentType, preset ->
                        app.editApi.uploadImage(
                            bytes = bytes,
                            fileName = fileName,
                            contentType = contentType,
                            preset = preset,
                            idempotencyKey = UUID.randomUUID().toString(),
                        ).hash
                    },
                    onUploaded = onBannerHash,
                )
                TextButton(onClick = { onBannerHash(null) }) { Text("移除") }
            }
        } else {
            EditImageUploadButton(
                label = "上传预览图",
                preset = "cover",
                upload = { bytes, fileName, contentType, preset ->
                    app.editApi.uploadImage(
                        bytes = bytes,
                        fileName = fileName,
                        contentType = contentType,
                        preset = preset,
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).hash
                },
                onUploaded = onBannerHash,
            )
        }
        Text(
            "预览图不可包含 R18 等敏感内容；封面会作为作品的初始封面。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
