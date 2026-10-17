package com.kungal.app.ui.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.CreatorApplication
import com.kungal.core.model.CreatorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val status: CreatorStatus? = null,
        val error: String? = null,
        val statement: String = "",
        val submitting: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.meApi.creatorStatus() }
                .onSuccess { status ->
                    _state.update {
                        it.copy(
                            loading = false,
                            status = status,
                            statement = status.application?.statement.orEmpty(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    fun updateStatement(value: String) {
        _state.update { it.copy(statement = value.take(2000)) }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        _state.update { it.copy(submitting = true, message = null) }
        viewModelScope.launch {
            runCatching {
                app.meApi.applyCreator(current.statement.trim(), UUID.randomUUID().toString())
            }.onSuccess { application ->
                _state.update {
                    it.copy(
                        submitting = false,
                        message = if (application.state == "pending") "申请已提交，等待审核" else "申请状态：${application.state}",
                        status = it.status?.copy(application = application, isCreator = application.state == "approved"),
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(submitting = false, message = friendlyError(e)) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/** 创作者申请：资格进度、申请状态与提交。 */
@Composable
fun CreatorScreen(
    app: KungalApp,
    onBack: () -> Unit,
) {
    val vm: CreatorViewModel = viewModel { CreatorViewModel(app) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("提交创作者申请") },
            text = { Text("提交后将由站点审核，结果会通过站内信通知。确定提交吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSubmit = false
                    vm.submit()
                }) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("取消") } },
        )
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "创作者申请",
                subtitle = "资格进度与申请状态",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading && state.status == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && state.status == null -> ErrorBox(
                state.error!!,
                onRetry = vm::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val status = state.status ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (status.isCreator) {
                        StatusBanner(
                            title = "你已经是创作者",
                            description = "可以发布作品、参与资料库贡献。",
                            success = true,
                        )
                    }
                    status.application?.let { application ->
                        ApplicationCard(application)
                    }

                    Text(
                        "资格进度",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "以下任意一项满足即可申请（以站点审核为准）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val e = status.eligibility
                    EligibilityRow("合并的 Pull Request", e.mergedPrCount, e.requiredMergedPrCount)
                    EligibilityRow("已发布 Galgame", e.publishedGalgameCount, e.requiredPublishedGalgameCount)
                    EligibilityRow("长篇评测", e.longReviewCount, e.requiredLongReviewCount)
                    EligibilityRow("萌萌点", e.moemoepoint, e.requiredMoemoepoint)

                    if (!status.isCreator && status.application?.state != "pending") {
                        Text(
                            "申请说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = state.statement,
                            onValueChange = vm::updateStatement,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            placeholder = { Text("可以补充你的作品、贡献或想说的话（可选）") },
                        )
                        Button(
                            onClick = { confirmSubmit = true },
                            enabled = !state.submitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.submitting) "提交中…" else "提交申请") }
                    } else if (status.application?.state == "pending") {
                        Text(
                            "申请正在审核中，审核结果会通过站内信通知。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(title: String, description: String, success: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (success) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Verified,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (success) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (success) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ApplicationCard(application: CreatorApplication) {
    val (label, description) = when (application.state) {
        "pending" -> "审核中" to "提交于 ${fullDateTime(application.createdAt)}"
        "approved" -> "已通过" to "通过于 ${application.reviewedAt?.let { fullDateTime(it) } ?: "—"}"
        "declined" -> "未通过" to (application.declineReason?.takeIf { it.isNotBlank() } ?: "未提供原因")
        else -> application.state to ""
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "最近一次申请：$label",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (application.state == "declined") MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (application.statement.isNotBlank()) {
                Text(
                    "说明：${application.statement}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EligibilityRow(label: String, current: Int, required: Int) {
    val done = current >= required
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "$current / $required",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
