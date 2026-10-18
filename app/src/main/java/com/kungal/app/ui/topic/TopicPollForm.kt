package com.kungal.app.ui.topic

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kungal.core.model.Poll
import com.kungal.core.model.PollCreate
import com.kungal.core.model.PollOptionChanges
import com.kungal.core.model.PollOptionCreate
import com.kungal.core.model.PollOptionUpdate
import com.kungal.core.model.PollPatch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private data class PollOptionRow(
    val id: String?,
    val text: String,
    val deleted: Boolean = false,
)

private val POLL_VISIBILITY = listOf(
    "always" to "任何人可见结果",
    "after_vote" to "投票后可见结果",
    "after_deadline" to "结束后可见结果",
)

@Composable
fun PollFormDialog(
    initial: Poll?,
    onDismiss: () -> Unit,
    onCreate: suspend (PollCreate) -> String?,
    onUpdate: suspend (String, PollPatch) -> String?,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var choiceType by remember(initial?.id) { mutableStateOf(initial?.choiceType ?: "single") }
    var resultVisibility by remember(initial?.id) {
        mutableStateOf(initial?.resultVisibility ?: "always")
    }
    var isAnonymous by remember(initial?.id) { mutableStateOf(initial?.isAnonymous ?: false) }
    var canChangeVote by remember(initial?.id) { mutableStateOf(initial?.canChangeVote ?: true) }
    var closesAt by remember(initial?.id) { mutableStateOf(isoToLocalDate(initial?.closesAt)) }
    var minChoice by remember(initial?.id) {
        mutableStateOf((initial?.minChoice ?: 1).toString())
    }
    var maxChoice by remember(initial?.id) {
        mutableStateOf((initial?.maxChoice ?: initial?.options?.size ?: 2).toString())
    }
    val options = remember(initial?.id) {
        mutableStateListOf<PollOptionRow>().apply {
            initial?.options?.forEach { add(PollOptionRow(it.id, it.text)) }
            if (isEmpty()) {
                add(PollOptionRow(null, ""))
                add(PollOptionRow(null, ""))
            }
        }
    }

    val liveOptions = options.filterNot { it.deleted }
    val multiple = choiceType == "multiple"

    fun submit() {
        if (busy) return
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            error = "投票标题不能为空"
            return
        }
        if (trimmedTitle.length > 100) {
            error = "投票标题最多 100 个字符"
            return
        }
        if (description.length > 500) {
            error = "投票描述最多 500 个字符"
            return
        }
        if (liveOptions.size !in 2..20) {
            error = "投票至少需要 2 个选项，最多 20 个"
            return
        }
        if (liveOptions.any { it.text.trim().isEmpty() }) {
            error = "选项内容不能为空"
            return
        }
        if (liveOptions.any { it.text.trim().length > 100 }) {
            error = "每个选项最多 100 个字符"
            return
        }
        val minValue = minChoice.toIntOrNull() ?: 1
        val maxValue = maxChoice.toIntOrNull() ?: liveOptions.size
        if (multiple) {
            if (minValue < 1 || maxValue < minValue || maxValue > liveOptions.size) {
                error = "多选范围不正确：至少选 1 项，至多选 ${liveOptions.size} 项"
                return
            }
        }
        error = null
        scope.launch {
            busy = true
            val failure = if (initial == null) {
                onCreate(
                    PollCreate(
                        title = trimmedTitle,
                        description = description,
                        choiceType = choiceType,
                        resultVisibility = resultVisibility,
                        options = liveOptions.map { PollOptionCreate(it.text.trim()) },
                        closesAt = closesAt?.let(::localDateToIso),
                        minChoice = if (multiple) minValue else null,
                        maxChoice = if (multiple) maxValue else null,
                        isAnonymous = isAnonymous,
                        canChangeVote = canChangeVote,
                    ),
                )
            } else {
                val changes = buildOptionChanges(initial, options)
                onUpdate(
                    initial.id,
                    PollPatch(
                        title = trimmedTitle,
                        description = description,
                        choiceType = choiceType,
                        minChoice = if (multiple) minValue else 1,
                        maxChoice = if (multiple) maxValue else 1,
                        closesAt = closesAt?.let { JsonPrimitive(localDateToIso(it)) } ?: JsonNull,
                        resultVisibility = resultVisibility,
                        isAnonymous = isAnonymous,
                        canChangeVote = canChangeVote,
                        optionChanges = changes,
                    ),
                )
            }
            busy = false
            if (failure == null) onDismiss() else error = failure
        }
    }

    MiniAppFormDialog(
        title = if (initial == null) "创建投票" else "编辑投票",
        confirmText = if (initial == null) "发布投票" else "保存更改",
        busy = busy,
        onDismiss = onDismiss,
        onConfirm = ::submit,
    ) {
        Text(
            "每个话题最多 30 个投票，每个投票最多 20 个选项。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(100) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("投票标题") },
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("补充描述（可选）") },
            minLines = 2,
            maxLines = 4,
        )

        FormSectionLabel("选项（2–20 个）")
        options.forEachIndexed { index, option ->
            if (!option.deleted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option.text,
                        onValueChange = { options[index] = option.copy(text = it.take(100)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("选项 ${index + 1}") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            options[index] = if (option.id != null) {
                                option.copy(deleted = true)
                            } else {
                                option
                            }
                            if (option.id == null) options.removeAt(index)
                        },
                        enabled = liveOptions.size > 2,
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除选项")
                    }
                }
            }
        }
        TextButton(
            onClick = { options.add(PollOptionRow(null, "")) },
            enabled = liveOptions.size < 20,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("增加选项")
        }

        FormSectionLabel("投票类型")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormChoiceChip("单选", !multiple) { choiceType = "single" }
            FormChoiceChip("多选", multiple) { choiceType = "multiple" }
        }
        if (multiple) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormNumberField(
                    label = "至少选",
                    value = minChoice,
                    onValueChange = { minChoice = it },
                    modifier = Modifier.weight(1f),
                )
                FormNumberField(
                    label = "至多选",
                    value = maxChoice,
                    onValueChange = { maxChoice = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        MiniAppDateField(
            label = "截止日期（可选）",
            value = closesAt,
            onChange = { closesAt = it },
        )

        FormSectionLabel("结果可见性")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            POLL_VISIBILITY.forEach { (value, label) ->
                FormChoiceChip(label, resultVisibility == value) { resultVisibility = value }
            }
        }

        FormSwitchRow(
            label = "匿名投票",
            checked = isAnonymous,
            onCheckedChange = { isAnonymous = it },
            description = "投票后不会公开谁投了哪一项",
        )
        FormSwitchRow(
            label = "允许修改投票",
            checked = canChangeVote,
            onCheckedChange = { canChangeVote = it },
        )

        FormErrorText(error)
    }
}

private fun buildOptionChanges(
    initial: Poll,
    options: List<PollOptionRow>,
): PollOptionChanges? {
    val stored = initial.options.associateBy { it.id }
    val add = options
        .filter { it.id == null && !it.deleted }
        .map { PollOptionCreate(it.text.trim()) }
    val update = options
        .filter { it.id != null && !it.deleted && stored[it.id]?.text != it.text.trim() }
        .map { PollOptionUpdate(it.id!!, it.text.trim()) }
    val remove = options
        .filter { it.id != null && it.deleted }
        .mapNotNull { it.id }
    return if (add.isEmpty() && update.isEmpty() && remove.isEmpty()) {
        null
    } else {
        PollOptionChanges(add = add, update = update, remove = remove)
    }
}
