package com.kungal.app.ui.toolset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SheetMessageHost
import com.kungal.app.ui.galgame.parseResourceLinkInput
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ToolsetResource
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

/**
 * 工具资源发布 / 编辑（BP-03 U4/U5，链接型；文件型上传在 U6）。
 * file 资源只能改备注与解压码；link 资源可改链接、体积、提取码、解压码、备注。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsetResourceEditorSheet(
    app: KungalApp,
    toolsetId: String,
    resource: ToolsetResource?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val editing = resource != null
    val isFile = resource?.type == "file"
    var mode by remember { mutableStateOf(if (isFile == true) "file" else "link") }
    var loading by remember { mutableStateOf(editing) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    var linkUrl by remember { mutableStateOf("") }
    var sizeLabel by remember { mutableStateOf("") }
    var extractionCode by remember { mutableStateOf("") }
    var archivePassword by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(resource?.id, retry) {
        if (resource == null) return@LaunchedEffect
        loading = true
        loadError = null
        runCatching { app.toolsetApi.resourceSource(toolsetId, resource.id) }
            .onSuccess { source ->
                linkUrl = source.linkUrl
                sizeLabel = source.sizeLabel
                extractionCode = source.extractionCode
                archivePassword = source.archivePassword
                note = source.note.orEmpty()
            }
            .onFailure { loadError = friendlyError(it) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        if (loading) {
            LoadingBox()
            return@ModalBottomSheet
        }
        if (loadError != null) {
            ErrorBox(loadError!!, onRetry = { retry += 1 })
            return@ModalBottomSheet
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetMessageHost()
            Text(
                if (editing) "编辑工具资源" else "发布工具资源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "只能发布 Galgame 相关工具；禁止发布游戏本体及其下载链接。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )

            if (!editing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("link" to "自定义链接", "file" to "对象存储 (S3)").forEach { (value, label) ->
                        FilterChip(
                            selected = mode == value,
                            onClick = { mode = value },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
            if (mode == "file") {
                Text(
                    "文件上传（单文件 / 分片、断点续传）将在下一批接入；当前可先使用「自定义链接」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (mode == "link") {
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text(if (isFile == true) "下载链接（文件资源不可修改）" else "资源链接") },
                    placeholder = { Text("可直接粘贴分享文本") },
                    enabled = isFile != true,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        val parsed = parseResourceLinkInput(linkUrl)
                        if (!parsed.hasLinks) {
                            AppMessages.show("没有识别到链接")
                            return@TextButton
                        }
                        linkUrl = parsed.links.first()
                        if (parsed.extractionCode.isNotBlank()) extractionCode = parsed.extractionCode
                        if (parsed.archivePassword.isNotBlank()) archivePassword = parsed.archivePassword
                        AppMessages.show("已识别链接")
                    },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("自动识别链接 / 提取码 / 解压码", style = MaterialTheme.typography.labelMedium) }
                OutlinedTextField(
                    value = sizeLabel,
                    onValueChange = { sizeLabel = it },
                    label = { Text("体积（需包含 KB/MB/GB）") },
                    enabled = isFile != true,
                    placeholder = { Text("例如 1007MB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = extractionCode,
                    onValueChange = { extractionCode = it },
                    label = { Text("提取码（可选）") },
                    enabled = isFile != true,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = archivePassword,
                    onValueChange = { archivePassword = it },
                    label = { Text("解压码（可选）") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（建议写明使用方法和注意事项）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (saving) return@Button
                        if (!editing && mode == "file") {
                            AppMessages.show("文件上传将在下一批接入，请先使用自定义链接")
                            return@Button
                        }
                        if (!editing && mode == "link") {
                            if (linkUrl.isBlank()) {
                                AppMessages.show("请填写资源链接")
                                return@Button
                            }
                            if (!Regex("""^(https?|ftps?|magnet|ed2k|thunder):""", RegexOption.IGNORE_CASE)
                                    .containsMatchIn(linkUrl.trim())
                            ) {
                                AppMessages.show("链接格式不正确")
                                return@Button
                            }
                            if (sizeLabel.isBlank() || !Regex("""\d.*(KB|MB|GB)""", RegexOption.IGNORE_CASE)
                                    .containsMatchIn(sizeLabel)
                            ) {
                                AppMessages.show("大小格式不正确，需要包含 KB、MB 或 GB")
                                return@Button
                            }
                        }
                        saving = true
                        scope.launch {
                            val result = runCatching {
                                if (editing && resource != null) {
                                    val body = if (isFile == true) {
                                        buildJsonObject {
                                            if (archivePassword != "") {
                                                put("archive_password", JsonPrimitive(archivePassword))
                                            }
                                            if (note != "") put("note", JsonPrimitive(note))
                                        }
                                    } else {
                                        buildJsonObject {
                                            put("link_url", JsonPrimitive(linkUrl.trim()))
                                            put("size_label", JsonPrimitive(sizeLabel.trim()))
                                            put("extraction_code", JsonPrimitive(extractionCode.trim()))
                                            put("archive_password", JsonPrimitive(archivePassword.trim()))
                                            put("note", JsonPrimitive(note))
                                        }
                                    }
                                    app.toolsetApi.patchResource(toolsetId, resource.id, body)
                                } else {
                                    app.toolsetApi.addResource(
                                        toolsetId = toolsetId,
                                        body = buildJsonObject {
                                            put("toolset_resource_type", JsonPrimitive("link"))
                                            put("link_url", JsonPrimitive(linkUrl.trim()))
                                            put("size_label", JsonPrimitive(sizeLabel.trim()))
                                            if (extractionCode.isNotBlank()) {
                                                put("extraction_code", JsonPrimitive(extractionCode.trim()))
                                            }
                                            if (archivePassword.isNotBlank()) {
                                                put("archive_password", JsonPrimitive(archivePassword.trim()))
                                            }
                                            if (note.isNotBlank()) put("note", JsonPrimitive(note))
                                        },
                                        idempotencyKey = UUID.randomUUID().toString(),
                                    )
                                }
                            }
                            saving = false
                            result.onSuccess {
                                AppMessages.show(if (editing) "更新成功" else "资源发布成功")
                                onSaved()
                            }.onFailure { AppMessages.show(friendlyError(it)) }
                        }
                    },
                    enabled = !saving,
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (editing) "保存" else "提交链接")
                    }
                }
            }
        }
    }
}
