package com.kungal.app.ui.toolset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

private enum class EditorPicker { TYPE, RELEASE, PLATFORM, LANGUAGE }

private data class ToolsetForm(
    val title: String = "",
    val type: String = "emulator",
    val release: String = "stable",
    val platform: String = "windows",
    val language: String = "zh-cn",
    val markdown: String = "",
    val homepageText: String = "",
    val aliasesText: String = "",
)

/**
 * 工具集创建 / 编辑（BP-03 U5）：
 * 标题、类型、版本、平台、语言、Markdown 简介、主页链接、别名。
 * 编辑时先读详情与 /source，只提交变化字段。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsetEditorScreen(
    app: KungalApp,
    toolsetId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val editing = toolsetId != null
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(editing) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf(ToolsetForm()) }
    var original by remember { mutableStateOf<ToolsetForm?>(null) }
    var saving by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<EditorPicker?>(null) }
    var retry by remember { mutableStateOf(0) }

    LaunchedEffect(toolsetId, retry) {
        if (toolsetId == null) return@LaunchedEffect
        loading = true
        loadError = null
        runCatching {
            val detail = app.toolsetApi.toolset(toolsetId)
            val source = app.toolsetApi.source(toolsetId)
            Triple(detail, source, Unit)
        }.onSuccess { (detail, source, _) ->
            val loaded = ToolsetForm(
                title = detail.title,
                type = detail.toolsetType,
                release = detail.releaseChannel,
                platform = detail.platform,
                language = detail.interfaceLanguage,
                markdown = source.contentMarkdown,
                homepageText = detail.homepageUrls.joinToString(", "),
                aliasesText = detail.aliases.joinToString(", "),
            )
            form = loaded
            original = loaded
        }.onFailure { loadError = friendlyError(it) }
        loading = false
    }

    fun parseList(text: String): List<String> =
        text.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (editing) "重新编辑工具信息" else "发布 Galgame 工具",
                subtitle = if (editing) "只提交修改过的字段" else "提交与 Galgame 相关的工具",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            loadError != null -> ErrorBox(
                loadError!!,
                onRetry = { retry += 1 },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "禁止发布游戏资源",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "这里只能发布 Galgame 相关的工具。上传或发布游戏本体及其下载链接会被删除，违规直接永久封禁账号。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { form = form.copy(title = it) },
                    label = { Text("名称") },
                    placeholder = { Text("工具名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectorRow("工具类型", labelOfToolset(ToolsetTypeOptions, form.type)) {
                    picker = EditorPicker.TYPE
                }
                SelectorRow("版本", labelOfToolset(ToolsetReleaseOptions, form.release)) {
                    picker = EditorPicker.RELEASE
                }
                SelectorRow("平台", labelOfToolset(ToolsetPlatformOptions, form.platform)) {
                    picker = EditorPicker.PLATFORM
                }
                SelectorRow("语言", labelOfToolset(ToolsetLanguageOptions, form.language)) {
                    picker = EditorPicker.LANGUAGE
                }
                OutlinedTextField(
                    value = form.markdown,
                    onValueChange = { form = form.copy(markdown = it) },
                    label = { Text("简介（Markdown，最多 2000 字）") },
                    placeholder = { Text("具体说明工具是什么，以及如何使用，越详细越好") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.homepageText,
                    onValueChange = { form = form.copy(homepageText = it) },
                    label = { Text("主页 / 项目链接（可选）") },
                    placeholder = { Text("官网、GitHub 等，多个用英文逗号分隔") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.aliasesText,
                    onValueChange = { form = form.copy(aliasesText = it) },
                    label = { Text("别名（可选，最多 17 个）") },
                    placeholder = { Text("多个用英文逗号分隔") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack, enabled = !saving) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (saving) return@Button
                            val title = form.title.trim()
                            if (title.isEmpty()) {
                                AppMessages.show("请填写工具名称")
                                return@Button
                            }
                            if (title.length > 500) {
                                AppMessages.show("名称过长（最多 500 字）")
                                return@Button
                            }
                            if (form.markdown.length > 2000) {
                                AppMessages.show("简介过长（最多 2000 字）")
                                return@Button
                            }
                            val homepages = parseList(form.homepageText)
                            if (homepages.size > 10 || homepages.any { !it.startsWith("http") || it.length > 500 }) {
                                AppMessages.show("主页链接最多 10 条，且必须是 http/https 地址")
                                return@Button
                            }
                            val aliases = parseList(form.aliasesText)
                            if (aliases.size > 17 || aliases.any { it.length > 500 }) {
                                AppMessages.show("别名最多 17 个，每个最多 500 字")
                                return@Button
                            }
                            saving = true
                            scope.launch {
                                val result = runCatching {
                                    if (editing && toolsetId != null) {
                                        val before = original
                                        val body = buildJsonObject {
                                            if (before == null || title != before.title) {
                                                put("title", JsonPrimitive(title))
                                            }
                                            if (before == null || form.type != before.type) {
                                                put("toolset_type", JsonPrimitive(form.type))
                                            }
                                            if (before == null || form.release != before.release) {
                                                put("release_channel", JsonPrimitive(form.release))
                                            }
                                            if (before == null || form.platform != before.platform) {
                                                put("platform", JsonPrimitive(form.platform))
                                            }
                                            if (before == null || form.language != before.language) {
                                                put("interface_language", JsonPrimitive(form.language))
                                            }
                                            if (before == null || form.markdown != before.markdown) {
                                                put("content_markdown", JsonPrimitive(form.markdown))
                                            }
                                            if (before == null || homepages != parseList(before.homepageText)) {
                                                put(
                                                    "homepage_urls",
                                                    buildJsonArray { homepages.forEach { add(JsonPrimitive(it)) } },
                                                )
                                            }
                                            if (before == null || aliases != parseList(before.aliasesText)) {
                                                put(
                                                    "aliases",
                                                    buildJsonArray { aliases.forEach { add(JsonPrimitive(it)) } },
                                                )
                                            }
                                        }
                                        if (body.isEmpty()) {
                                            null
                                        } else {
                                            app.toolsetApi.patch(toolsetId, body).id
                                        }
                                    } else {
                                        app.toolsetApi.create(
                                            body = buildJsonObject {
                                                put("title", JsonPrimitive(title))
                                                put("toolset_type", JsonPrimitive(form.type))
                                                put("release_channel", JsonPrimitive(form.release))
                                                put("platform", JsonPrimitive(form.platform))
                                                put("interface_language", JsonPrimitive(form.language))
                                                put("content_markdown", JsonPrimitive(form.markdown))
                                                put(
                                                    "homepage_urls",
                                                    buildJsonArray { homepages.forEach { add(JsonPrimitive(it)) } },
                                                )
                                                put(
                                                    "aliases",
                                                    buildJsonArray { aliases.forEach { add(JsonPrimitive(it)) } },
                                                )
                                            },
                                            idempotencyKey = UUID.randomUUID().toString(),
                                        ).id
                                    }
                                }
                                saving = false
                                result.onSuccess { id ->
                                    AppMessages.show(if (editing) "工具信息已更新" else "创建工具成功")
                                    onSaved(id ?: toolsetId.orEmpty())
                                }.onFailure { AppMessages.show(friendlyError(it)) }
                            }
                        },
                        enabled = !saving,
                    ) { Text(if (saving) "保存中…" else if (editing) "保存修改" else "创建工具") }
                }
            }
        }
    }

    val pickerOptions: Pair<String, List<FilterOption>>? = when (picker) {
        EditorPicker.TYPE -> "工具类型" to ToolsetTypeOptions
        EditorPicker.RELEASE -> "版本" to ToolsetReleaseOptions
        EditorPicker.PLATFORM -> "平台" to ToolsetPlatformOptions
        EditorPicker.LANGUAGE -> "语言" to ToolsetLanguageOptions
        null -> null
    }
    pickerOptions?.let { (title, options) ->
        AlertDialog(
            onDismissRequest = { picker = null },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    form = when (picker) {
                                        EditorPicker.TYPE -> form.copy(type = option.value)
                                        EditorPicker.RELEASE -> form.copy(release = option.value)
                                        EditorPicker.PLATFORM -> form.copy(platform = option.value)
                                        EditorPicker.LANGUAGE -> form.copy(language = option.value)
                                        null -> form
                                    }
                                    picker = null
                                }
                                .padding(vertical = 11.dp),
                        ) { Text(option.label, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = null }) { Text("关闭") } },
        )
    }
}

@Composable
private fun SelectorRow(label: String, value: String, onClick: () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
