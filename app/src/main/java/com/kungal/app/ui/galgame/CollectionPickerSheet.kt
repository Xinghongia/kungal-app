package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SheetMessageHost
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.CollectionChoice
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 收藏夹选择器（作品页心形按钮）：勾选/取消所属收藏夹，可新建收藏夹。
 * 保存时只提交有变化的条目，成功后回填页面心形状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    app: KungalApp,
    workId: String,
    onDismiss: () -> Unit,
    onSaved: (favorited: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var collections by remember { mutableStateOf<List<CollectionChoice>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var held by remember { mutableStateOf<Set<String>>(emptySet()) }
    var saving by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }

    fun apply(items: List<CollectionChoice>, preserve: Boolean) {
        collections = items
        val nextHeld = items.filter { it.viewer?.hasWork == true }.map { it.id }.toSet()
        held = nextHeld
        if (!preserve) selected = nextHeld
    }

    fun load(preserve: Boolean = false) {
        scope.launch {
            loading = true
            error = null
            runCatching { app.collectionApi.choicesForWork(workId) }
                .onSuccess { page ->
                    apply(page.items, preserve)
                    loading = false
                }
                .onFailure { e ->
                    error = friendlyError(e)
                    loading = false
                }
        }
    }

    LaunchedEffect(workId) { load() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SheetMessageHost()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("收藏到收藏夹", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { createOpen = true }) { Text("新建") }
            }

            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!, onRetry = { load() })
                collections.isEmpty() -> Text(
                    "还没有收藏夹，点击右上角「新建」创建一个吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    items(collections, key = { it.id }) { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (collection.id in selected) {
                                        selected - collection.id
                                    } else {
                                        selected + collection.id
                                    }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = collection.id in selected,
                                onCheckedChange = {
                                    selected = if (collection.id in selected) {
                                        selected - collection.id
                                    } else {
                                        selected + collection.id
                                    }
                                },
                            )
                            Icon(
                                if (collection.visibility == "private") Icons.Outlined.Lock
                                else Icons.Outlined.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                collection.title.ifBlank { "未命名收藏夹" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                collection.itemCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        scope.launch {
                            val diffs = collections.filter { (it.id in selected) != (it.id in held) }
                            var failure: Throwable? = null
                            for (collection in diffs) {
                                val result = runCatching {
                                    app.collectionApi.setWork(
                                        collectionId = collection.id,
                                        workId = workId,
                                        member = collection.id in selected,
                                    )
                                }
                                if (result.isFailure) {
                                    failure = result.exceptionOrNull()
                                    break
                                }
                            }
                            saving = false
                            if (failure != null) {
                                AppMessages.show(friendlyError(failure!!))
                                load()
                                return@launch
                            }
                            onSaved(selected.isNotEmpty())
                            onDismiss()
                        }
                    },
                    enabled = !loading && error == null,
                ) {
                    Text(if (saving) "保存中…" else "保存")
                }
            }
        }
    }

    if (createOpen) {
        CreateCollectionDialog(
            app = app,
            onDismiss = { createOpen = false },
            onCreated = { newId ->
                createOpen = false
                scope.launch {
                    runCatching { app.collectionApi.choicesForWork(workId) }
                        .onSuccess { page ->
                            apply(page.items, preserve = true)
                            if (newId != null) selected = selected + newId
                        }
                        .onFailure { AppMessages.show(friendlyError(it)) }
                }
            },
        )
    }
}

@Composable
private fun CreateCollectionDialog(
    app: KungalApp,
    onDismiss: () -> Unit,
    onCreated: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("新建收藏夹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("公开 · 所有人可见", modifier = Modifier.weight(1f))
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                if (failure != null) {
                    Text(
                        failure!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (submitting) return@TextButton
                    val name = title.trim()
                    if (name.isEmpty()) {
                        failure = "请填写收藏夹名称"
                        return@TextButton
                    }
                    submitting = true
                    failure = null
                    scope.launch {
                        runCatching {
                            app.collectionApi.create(
                                title = name,
                                visibility = if (isPublic) "public" else "private",
                                description = description.trim().ifBlank { null },
                                idempotencyKey = UUID.randomUUID().toString(),
                            )
                        }.onSuccess {
                            submitting = false
                            onCreated(it.id)
                        }.onFailure { e ->
                            submitting = false
                            failure = friendlyError(e)
                        }
                    }
                },
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
    )
}
