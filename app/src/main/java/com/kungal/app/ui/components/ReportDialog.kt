package com.kungal.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kungal.core.model.ReportReason

/** 举报弹窗：原因单选 + 可选备注；原因由调用方从 /report-reasons 加载后传入。 */
@Composable
fun ReportDialog(
    title: String,
    reasons: List<ReportReason>,
    busy: Boolean,
    onSubmit: (reasonKey: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reasonKey by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (reasons.isEmpty()) {
                    Text(
                        "正在加载举报原因…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { reasonKey = reason.key }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = reasonKey == reason.key,
                            onClick = { reasonKey = reason.key },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(reason.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1000) },
                    placeholder = { Text("补充说明（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = reasonKey != null && !busy,
                onClick = { reasonKey?.let { onSubmit(it, note) } },
            ) { Text(if (busy) "提交中…" else "举报") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
