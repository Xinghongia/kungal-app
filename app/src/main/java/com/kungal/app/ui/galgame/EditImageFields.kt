package com.kungal.app.ui.galgame

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MAX_EDIT_IMAGE_BYTES = 10 * 1024 * 1024

private fun moveRow(rows: List<JsonObject>, from: Int, to: Int): List<JsonObject> {
    if (to !in rows.indices) return rows
    val next = rows.toMutableList()
    next.add(to, next.removeAt(from))
    return next
}

@Composable
private fun ReorderButtons(
    index: Int,
    lastIndex: Int,
    onMove: (Int) -> Unit,
) {
    Column {
        IconButton(onClick = { onMove(index - 1) }, enabled = index > 0, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.ArrowUpward, contentDescription = "上移", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { onMove(index + 1) }, enabled = index < lastIndex, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.ArrowDownward, contentDescription = "下移", modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * 选图并上传到 /work-edit-images。上传成功回调图片哈希；失败用主题提示。
 * [upload] 由页面提供（需要 app.editApi 与幂等键）。
 */
@Composable
fun EditImageUploadButton(
    label: String,
    preset: String,
    upload: suspend (bytes: ByteArray, fileName: String, contentType: String, preset: String) -> String?,
    onUploaded: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            try {
                val resolver = context.contentResolver
                val contentType = resolver.getType(uri) ?: "image/*"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    AppMessages.show("读取图片失败")
                    return@launch
                }
                if (bytes.size > MAX_EDIT_IMAGE_BYTES) {
                    AppMessages.show("图片超过 10MB，请先压缩")
                    return@launch
                }
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "image"
                runCatching {
                    upload(bytes, fileName, contentType, preset)
                }.onSuccess { hash ->
                    if (hash != null) onUploaded(hash)
                }.onFailure { AppMessages.show(friendlyError(it)) }
            } finally {
                uploading = false
            }
        }
    }
    TextButton(onClick = { launcher.launch("image/*") }, enabled = !uploading) {
        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(if (uploading) "上传中…" else label)
    }
}

@Composable
fun CoversEditor(
    value: JsonElement?,
    onChange: (JsonArray) -> Unit,
    upload: suspend (ByteArray, String, String, String) -> String?,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }

    fun emit(newRows: List<JsonObject>) = onChange(JsonArray(newRows))
    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        emit(newRows.map { JsonObject(it) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            val hash = row.string("image_hash")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KungalImage(
                    url = if (hash.length >= 4) editImageUrl(hash) else null,
                    contentDescription = null,
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(5f / 7f)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = row.string("kind"),
                        onValueChange = { kind ->
                            updateAt(index) { it["kind"] = JsonPrimitive(kind) }
                        },
                        placeholder = { Text("封面类型：main / dig / pkgfront …") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        EditIntSelect(
                            options = EditSexualOptions,
                            selected = row.int("sexual"),
                            onSelect = { level -> updateAt(index) { it["sexual"] = JsonPrimitive(level) } },
                        )
                        EditIntSelect(
                            options = EditViolenceOptions,
                            selected = row.int("violence"),
                            onSelect = { level -> updateAt(index) { it["violence"] = JsonPrimitive(level) } },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(
                            checked = row.bool("portrait_pinned"),
                            onCheckedChange = { pinned ->
                                updateAt(index) { it["portrait_pinned"] = JsonPrimitive(pinned) }
                            },
                        )
                        Text("竖版封面", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = { emit(rows.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "删除封面")
                }
                ReorderButtons(index, rows.lastIndex) { to -> emit(moveRow(rows, index, to)) }
            }
        }
        EditImageUploadButton(
            label = "上传封面",
            preset = "cover",
            upload = upload,
            onUploaded = { hash ->
                if (rows.none { it.string("image_hash") == hash }) {
                    emit(rows + buildJsonObject { put("image_hash", hash) })
                } else {
                    AppMessages.show("已跳过重复图片")
                }
            },
        )
    }
}

@Composable
fun ScreenshotsEditor(
    value: JsonElement?,
    onChange: (JsonArray) -> Unit,
    upload: suspend (ByteArray, String, String, String) -> String?,
) {
    val rows = value.asArrayOrEmpty().map { it.asObjectOrNull() ?: JsonObject(emptyMap()) }

    fun emit(newRows: List<JsonObject>) = onChange(JsonArray(newRows))
    fun updateAt(index: Int, block: (MutableMap<String, JsonElement>) -> Unit) {
        val newRows = rows.map { it.toMutableMap() }
        newRows.getOrNull(index)?.let(block)
        emit(newRows.map { JsonObject(it) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            val hash = row.string("image_hash")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KungalImage(
                    url = if (hash.length >= 4) editImageUrl(hash) else null,
                    contentDescription = null,
                    modifier = Modifier
                        .width(108.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = row.string("caption"),
                        onValueChange = { caption ->
                            updateAt(index) { it["caption"] = JsonPrimitive(caption) }
                        },
                        placeholder = { Text("说明（可留空）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EditIntSelect(
                            options = EditSexualOptions,
                            selected = row.int("sexual"),
                            onSelect = { level -> updateAt(index) { it["sexual"] = JsonPrimitive(level) } },
                        )
                        EditIntSelect(
                            options = EditViolenceOptions,
                            selected = row.int("violence"),
                            onSelect = { level -> updateAt(index) { it["violence"] = JsonPrimitive(level) } },
                        )
                    }
                }
                IconButton(onClick = { emit(rows.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "删除图片")
                }
                ReorderButtons(index, rows.lastIndex) { to -> emit(moveRow(rows, index, to)) }
            }
        }
        EditImageUploadButton(
            label = "上传画廊图片",
            preset = "screenshot",
            upload = upload,
            onUploaded = { hash ->
                if (rows.none { it.string("image_hash") == hash }) {
                    emit(rows + buildJsonObject { put("image_hash", hash) })
                } else {
                    AppMessages.show("已跳过重复图片")
                }
            },
        )
    }
}
