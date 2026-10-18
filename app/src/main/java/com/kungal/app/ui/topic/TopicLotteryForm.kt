package com.kungal.app.ui.topic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.KungalImage
import com.kungal.core.model.Lottery
import com.kungal.core.model.LotteryCreate
import com.kungal.core.model.LotteryPatch
import com.kungal.core.model.LotteryPrize
import com.kungal.core.model.LotteryPrizeInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.util.UUID

private data class PrizeImageRow(
    val hash: String,
    val url: String,
    val markedAdult: Boolean,
    val gradedExplicit: Boolean,
)

private class PrizeFormState(
    title: String = "",
    description: String = "",
    delivery: String = "offline",
    pointMode: String = "fixed",
    pointAmount: String = "",
    slots: String = "1",
    codes: String = "",
) {
    var title by mutableStateOf(title)
    var description by mutableStateOf(description)
    var delivery by mutableStateOf(delivery)
    var pointMode by mutableStateOf(pointMode)
    var pointAmount by mutableStateOf(pointAmount)
    var slots by mutableStateOf(slots)
    var codes by mutableStateOf(codes)
    var uploading by mutableStateOf(false)
    val images = mutableStateListOf<PrizeImageRow>()

    fun toInput(): LotteryPrizeInput = LotteryPrizeInput(
        title = title.trim(),
        description = description,
        imageHashes = images.map { it.hash },
        adultImageHashes = images.filter { it.markedAdult || it.gradedExplicit }.map { it.hash },
        delivery = delivery,
        slotCount = slots.toIntOrNull() ?: 1,
        codes = if (delivery == "code") {
            codes.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            null
        },
        pointMode = if (delivery == "point") pointMode else null,
        pointAmount = if (delivery == "point") pointAmount.toIntOrNull() else null,
    )

    companion object {
        fun from(prize: LotteryPrize): PrizeFormState {
            val state = PrizeFormState(
                title = prize.title,
                description = prize.description,
                delivery = prize.delivery,
                pointMode = prize.pointMode ?: "fixed",
                pointAmount = prize.pointAmount?.toString().orEmpty(),
                slots = prize.slotCount.toString(),
                codes = "",
            )
            prize.images.forEach { image ->
                state.images.add(
                    PrizeImageRow(
                        hash = image.hash,
                        url = image.image?.url.orEmpty(),
                        markedAdult = image.isMarkedAdult,
                        gradedExplicit = image.isGradedExplicit,
                    ),
                )
            }
            return state
        }
    }
}

private class LotteryFormModel(initial: Lottery?) {
    var title by mutableStateOf(initial?.title.orEmpty())
    var description by mutableStateOf(initial?.description.orEmpty())
    var entryMode by mutableStateOf(initial?.entryMode ?: "signup")
    var floorRule by mutableStateOf(initial?.floorRule.orEmpty())
    var drawMode by mutableStateOf(initial?.drawMode ?: "deadline")
    var drawThreshold by mutableStateOf(initial?.drawThreshold?.toString().orEmpty())
    var closesAt by mutableStateOf(isoToLocalDate(initial?.closesAt))
    var minAccountAgeDays by mutableStateOf((initial?.minAccountAgeDays ?: 3).toString())
    var minMoemoepoint by mutableStateOf((initial?.minMoemoepoint ?: 20).toString())
    var showEntrants by mutableStateOf(initial?.isEntryListPublic ?: true)
    val prizes = mutableStateListOf<PrizeFormState>().apply {
        initial?.prizes?.forEach { add(PrizeFormState.from(it)) }
        if (isEmpty()) add(PrizeFormState())
    }

    val totalSlots: Int get() = prizes.sumOf { it.slots.toIntOrNull() ?: 0 }

    fun validate(includePrizes: Boolean, initial: Lottery? = null): String? {
        if (title.trim().isEmpty()) return "抽奖标题不能为空"
        if (title.trim().length > 100) return "抽奖标题最多 100 个字符"
        if (description.length > 1000) return "抽奖说明最多 1000 个字符"
        if (entryMode == "floor" && drawMode == "threshold") {
            return "楼层抽奖不能使用满员开奖"
        }
        if (entryMode == "floor") {
            val rule = floorRule.trim()
            if (rule.isEmpty()) return "请填写中奖楼层"
            if (!rule.startsWith("every:")) {
                val floors = rule.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                if (floors.size != totalSlots) {
                    return "中奖楼层数量需要与总名额一致（当前总名额 $totalSlots）"
                }
                if (floors.any { (it.toIntOrNull() ?: 0) <= 0 }) {
                    return "中奖楼层格式不正确，示例：8,18,28"
                }
            } else if ((rule.removePrefix("every:").toIntOrNull() ?: 0) <= 0) {
                return "每 N 楼的格式不正确，示例：every:10"
            }
        }
        if (drawMode == "threshold") {
            val threshold = drawThreshold.toIntOrNull()
            if (threshold == null || threshold < 1) return "请填写满多少人开奖"
            if (threshold < totalSlots) {
                return "开奖人数不能少于总名额（当前总名额 $totalSlots）"
            }
        }
        if (drawMode == "deadline") {
            val date = closesAt ?: return "请选择开奖时间"
            // 编辑一个快到点的抽奖时，未改动的过去时间要原样保留，不能被表单拦下。
            val unchanged = initial != null &&
                initial.drawMode == "deadline" &&
                date == isoToLocalDate(initial.closesAt)
            if (!unchanged && localDateDeadline(date).isBefore(Instant.now())) {
                return "开奖时间必须晚于现在"
            }
        }
        val age = minAccountAgeDays.toIntOrNull()
        if (age == null || age !in 0..3650) return "注册天数门槛需要在 0–3650 之间"
        val points = minMoemoepoint.toIntOrNull()
        if (points == null || points !in 0..1_000_000) return "萌萌点门槛需要在 0–1000000 之间"
        if (includePrizes) {
            if (prizes.isEmpty()) return "至少需要一个奖项"
            if (prizes.size > 10) return "最多只能有 10 个奖项"
            if (totalSlots !in 1..500) return "总名额需要在 1–500 之间"
            var pointBudget = 0
            prizes.forEachIndexed { index, prize ->
                if (prize.title.trim().isEmpty()) return "奖项 ${index + 1} 名称不能为空"
                if (prize.title.trim().length > 100) return "奖项 ${index + 1} 名称最多 100 个字符"
                if (prize.description.length > 500) return "奖项 ${index + 1} 描述最多 500 个字符"
                val slots = prize.slots.toIntOrNull()
                if (slots == null || slots !in 1..500) {
                    return "奖项 ${index + 1} 的名额需要在 1–500 之间"
                }
                if (prize.images.size > 9) return "单个奖项最多 9 张图片"
                when (prize.delivery) {
                    "code" -> {
                        val codes = prize.codes.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        if (codes.size != slots) {
                            return "奖项「${prize.title.trim()}」有 $slots 个名额，需要正好 $slots 个兑换码"
                        }
                        if (codes.any { it.length > 200 }) {
                            return "奖项 ${index + 1} 的兑换码最长 200 个字符"
                        }
                    }
                    "point" -> {
                        val amount = prize.pointAmount.toIntOrNull()
                        if (amount == null || amount !in 1..100_000) {
                            return "奖项 ${index + 1} 的萌萌点数量需要在 1–100000 之间"
                        }
                        if (prize.pointMode !in listOf("fixed", "split", "random")) {
                            return "奖项 ${index + 1} 的萌萌点发放方式不正确"
                        }
                        if (entryMode == "floor" && prize.pointMode == "random") {
                            return "楼层抽奖不能使用拼手气奖池"
                        }
                        if (prize.pointMode != "fixed" && amount < slots) {
                            return "奖项「${prize.title.trim()}」的奖池至少需要 $slots 萌萌点，每个名额至少分到 1 点"
                        }
                        pointBudget += if (prize.pointMode == "fixed") amount * slots else amount
                    }
                }
            }
            if (pointBudget > 100_000) return "萌萌点奖池总额不能超过 100000"
        }
        return null
    }

    fun create(): LotteryCreate = LotteryCreate(
        title = title.trim(),
        description = description,
        entryMode = entryMode,
        floorRule = floorRule.trim().takeIf { entryMode == "floor" },
        drawMode = drawMode,
        drawThreshold = drawThreshold.toIntOrNull().takeIf { drawMode == "threshold" },
        closesAt = closesAt?.let(::localDateToIso),
        minAccountAgeDays = minAccountAgeDays.toIntOrNull() ?: 0,
        minMoemoepoint = minMoemoepoint.toIntOrNull() ?: 0,
        isEntryListPublic = showEntrants,
        prizes = prizes.map { it.toInput() },
    )

    fun patch(initial: Lottery, rewritePrizes: Boolean): LotteryPatch? {
        var patch = LotteryPatch()
        if (title.trim() != initial.title) patch = patch.copy(title = title.trim())
        if (description != initial.description) patch = patch.copy(description = description)
        if (entryMode != initial.entryMode) patch = patch.copy(entryMode = entryMode)
        if (entryMode == "floor" && floorRule.trim() != initial.floorRule.orEmpty()) {
            patch = patch.copy(floorRule = floorRule.trim())
        }
        if (drawMode != initial.drawMode) patch = patch.copy(drawMode = drawMode)
        if (drawMode == "threshold") {
            val threshold = drawThreshold.toIntOrNull()
            if (threshold != initial.drawThreshold) patch = patch.copy(drawThreshold = threshold)
        }
        val initialDate = isoToLocalDate(initial.closesAt)
        if (closesAt != initialDate) {
            patch = patch.copy(
                closesAt = closesAt?.let { JsonPrimitive(localDateToIso(it)) } ?: JsonNull,
            )
        }
        val age = minAccountAgeDays.toIntOrNull() ?: 0
        if (age != initial.minAccountAgeDays) patch = patch.copy(minAccountAgeDays = age)
        val points = minMoemoepoint.toIntOrNull() ?: 0
        if (points != initial.minMoemoepoint) patch = patch.copy(minMoemoepoint = points)
        if (showEntrants != initial.isEntryListPublic) {
            patch = patch.copy(isEntryListPublic = showEntrants)
        }
        if (rewritePrizes) patch = patch.copy(prizes = prizes.map { it.toInput() })
        return patch.takeIf { it != LotteryPatch() }
    }
}

@Composable
fun LotteryFormDialog(
    app: KungalApp,
    initial: Lottery?,
    onDismiss: () -> Unit,
    onCreate: suspend (LotteryCreate) -> String?,
    onUpdate: suspend (String, LotteryPatch) -> String?,
) {
    val scope = rememberCoroutineScope()
    val model = remember(initial?.id) { LotteryFormModel(initial) }
    var rewritePrizes by remember(initial?.id) { mutableStateOf(initial == null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val hasEntries = (initial?.entryCount ?: 0) > 0

    fun submit() {
        if (busy) return
        val includePrizes = initial == null || rewritePrizes
        val problem = model.validate(includePrizes, initial)
        if (problem != null) {
            error = problem
            return
        }
        error = null
        scope.launch {
            busy = true
            val failure = if (initial == null) {
                onCreate(model.create())
            } else {
                val patch = model.patch(initial, rewritePrizes)
                if (patch == null) null else onUpdate(initial.id, patch)
            }
            busy = false
            if (failure == null) onDismiss() else error = failure
        }
    }

    MiniAppFormDialog(
        title = if (initial == null) "创建抽奖" else "编辑抽奖",
        confirmText = if (initial == null) "发布抽奖" else "保存更改",
        busy = busy,
        onDismiss = onDismiss,
        onConfirm = ::submit,
    ) {
        Text(
            "每个话题最多 10 个抽奖，每个抽奖最多 10 个奖项。本站只负责产生中奖名单，不担保实物履约，也不代收收货地址。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = model.title,
            onValueChange = { model.title = it.take(100) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("抽奖标题") },
            singleLine = true,
        )
        OutlinedTextField(
            value = model.description,
            onValueChange = { model.description = it.take(1000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("抽奖说明（可选）") },
            minLines = 2,
            maxLines = 4,
        )

        FormSectionLabel("参与方式")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormChoiceChip("报名参与", model.entryMode == "signup", enabled = !hasEntries) {
                model.entryMode = "signup"
            }
            FormChoiceChip("回帖参与", model.entryMode == "reply", enabled = !hasEntries) {
                model.entryMode = "reply"
            }
            FormChoiceChip("楼层抽奖", model.entryMode == "floor", enabled = !hasEntries) {
                model.entryMode = "floor"
                if (model.drawMode == "threshold") model.drawMode = "deadline"
            }
        }
        Text(
            when (model.entryMode) {
                "reply" -> "需要先在本话题回复才能参与。"
                "floor" -> "由楼层号直接决定，无需报名；中奖楼层数需要与总名额一致。"
                else -> "点一下就能参加。"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasEntries) {
            Text(
                "已经有人参与后，参与方式、楼层规则与奖项不可再修改。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        FormSectionLabel("开奖方式")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormChoiceChip("到点自动开奖", model.drawMode == "deadline") {
                model.drawMode = "deadline"
            }
            FormChoiceChip("楼主手动开奖", model.drawMode == "manual") {
                model.drawMode = "manual"
            }
            FormChoiceChip(
                "满员自动开奖",
                model.drawMode == "threshold",
                enabled = model.entryMode != "floor",
            ) { model.drawMode = "threshold" }
        }
        if (model.entryMode == "floor") {
            OutlinedTextField(
                value = model.floorRule,
                onValueChange = { model.floorRule = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("中奖楼层") },
                placeholder = { Text("8,18,28 或 every:10（每 10 楼）") },
                supportingText = { Text("楼层数量需要与总名额一致，当前总名额 ${model.totalSlots}") },
                enabled = !hasEntries,
                singleLine = true,
            )
        }
        if (model.drawMode == "deadline") {
            MiniAppDateField(
                label = "开奖时间",
                value = model.closesAt,
                onChange = { model.closesAt = it },
                allowClear = false,
            )
        }
        if (model.drawMode == "threshold") {
            FormNumberField(
                label = "满多少人开奖",
                value = model.drawThreshold,
                onValueChange = { model.drawThreshold = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "满员开奖人数不能少于总名额（当前 ${model.totalSlots}）。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormNumberField(
                label = "注册满（天）",
                value = model.minAccountAgeDays,
                onValueChange = { model.minAccountAgeDays = it },
                modifier = Modifier.weight(1f),
            )
            FormNumberField(
                label = "萌萌点不低于",
                value = model.minMoemoepoint,
                onValueChange = { model.minMoemoepoint = it },
                modifier = Modifier.weight(1f),
            )
        }
        FormSwitchRow(
            label = "公开参与名单",
            checked = model.showEntrants,
            onCheckedChange = { model.showEntrants = it },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            FormSectionLabel("奖项设置（共 ${model.totalSlots} 个名额）")
            Spacer(Modifier.weight(1f))
            if (initial != null) {
                Text("修改奖项", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.Switch(
                    checked = rewritePrizes,
                    onCheckedChange = { rewritePrizes = it },
                    enabled = !hasEntries,
                )
            }
        }
        if (initial != null && !rewritePrizes) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "奖项保持不变。打开「修改奖项」会整体重写奖项列表，已托管的兑换码会被清空，需要重新填写。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    initial.prizes.forEach { prize ->
                        Text(
                            "${prize.title} · ${prize.slotCount} 名 · ${deliveryLabel(prize.delivery)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            model.prizes.forEachIndexed { index, prize ->
                PrizeEditor(
                    app = app,
                    index = index,
                    prize = prize,
                    floorEntry = model.entryMode == "floor",
                    canRemove = model.prizes.size > 1,
                    onRemove = { model.prizes.removeAt(index) },
                )
            }
            TextButton(
                onClick = { if (model.prizes.size < 10) model.prizes.add(PrizeFormState()) },
                enabled = model.prizes.size < 10,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("增加奖项")
            }
        }

        FormErrorText(error)
    }
}

@Composable
private fun PrizeEditor(
    app: KungalApp,
    index: Int,
    prize: PrizeFormState,
    floorEntry: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            prize.uploading = true
            uploadError = null
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null) {
                uploadError = "无法读取这张图片"
                prize.uploading = false
                return@launch
            }
            if (bytes.size > 10 * 1024 * 1024) {
                uploadError = "图片不能超过 10 MiB"
                prize.uploading = false
                return@launch
            }
            val mime = context.contentResolver.getType(uri) ?: "image/png"
            val ext = extensionFor(mime)
            runCatching {
                app.imageApi.upload(
                    bytes = bytes,
                    fileName = "prize.$ext",
                    contentType = mime,
                    purpose = "content",
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess { image ->
                val explicit = image.sexual == "explicit" || image.sexual == "adult"
                prize.images.add(
                    PrizeImageRow(
                        hash = image.hash,
                        url = image.url,
                        markedAdult = explicit,
                        gradedExplicit = explicit,
                    ),
                )
            }.onFailure { e ->
                uploadError = friendlyError(e)
            }
            prize.uploading = false
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "奖项 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove, enabled = canRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除奖项")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prize.title,
                    onValueChange = { prize.title = it.take(100) },
                    modifier = Modifier.weight(1f),
                    label = { Text("奖品名称") },
                    singleLine = true,
                )
                FormNumberField(
                    label = "名额",
                    value = prize.slots,
                    onValueChange = { prize.slots = it },
                    modifier = Modifier.width(100.dp),
                )
            }
            OutlinedTextField(
                value = prize.description,
                onValueChange = { prize.description = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("奖品描述（可选）") },
                minLines = 2,
                maxLines = 3,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    prize.images.forEachIndexed { imageIndex, image ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                KungalImage(
                                    url = image.url.takeIf(String::isNotBlank),
                                    contentDescription = prize.title,
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                if (image.markedAdult || image.gradedExplicit) {
                                    Text(
                                        "R18",
                                        modifier = Modifier.padding(2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.FilterChip(
                                    selected = image.markedAdult || image.gradedExplicit,
                                    onClick = {
                                        prize.images[imageIndex] =
                                            image.copy(markedAdult = !image.markedAdult)
                                    },
                                    enabled = !image.gradedExplicit,
                                    label = { Text("R18") },
                                )
                                IconButton(
                                    onClick = { prize.images.removeAt(imageIndex) },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "移除图片")
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { picker.launch("image/*") },
                    enabled = !prize.uploading && prize.images.size < 9,
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (prize.uploading) "上传中…" else "添加图片（最多 9 张）")
                }
            }

            FormSectionLabel("发放方式")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormChoiceChip("楼主私聊发放", prize.delivery == "offline") { prize.delivery = "offline" }
                FormChoiceChip("系统托管兑换码", prize.delivery == "code") { prize.delivery = "code" }
                FormChoiceChip("自动发放萌萌点", prize.delivery == "point") { prize.delivery = "point" }
            }

            if (prize.delivery == "code") {
                OutlinedTextField(
                    value = prize.codes,
                    onValueChange = { prize.codes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("兑换码（每行一个）") },
                    placeholder = { Text("CODE-1\nCODE-2") },
                    minLines = 4,
                    maxLines = 8,
                )
                val count = prize.codes.lines().count { it.trim().isNotEmpty() }
                Text(
                    "已填写 $count 个，需要 ${prize.slots.toIntOrNull() ?: 0} 个。兑换码提交后加密托管，页面上不会再显示，只有中奖者本人能揭示。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prize.delivery == "point") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FormChoiceChip("每人固定", prize.pointMode == "fixed") { prize.pointMode = "fixed" }
                    FormChoiceChip("奖池均分", prize.pointMode == "split") { prize.pointMode = "split" }
                    FormChoiceChip(
                        "奖池拼手气",
                        prize.pointMode == "random",
                        enabled = !floorEntry,
                    ) { prize.pointMode = "random" }
                }
                FormNumberField(
                    label = if (prize.pointMode == "fixed") "每人获得（萌萌点）" else "奖池总额（萌萌点）",
                    value = prize.pointAmount,
                    onValueChange = { prize.pointAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    pointHint(prize, floorEntry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FormErrorText(uploadError)
        }
    }
}

private fun pointHint(prize: PrizeFormState, floorEntry: Boolean): String {
    val amount = prize.pointAmount.toIntOrNull() ?: 0
    val slots = prize.slots.toIntOrNull() ?: 0
    if (amount <= 0 || slots <= 0) return ""
    return when (prize.pointMode) {
        "split" -> "奖池 $amount 点均分，名额全中时每人约 ${amount / slots} 点；中奖人数不足时每人分得更多。"
        "random" -> if (floorEntry) {
            "楼层抽奖不能使用拼手气奖池。"
        } else {
            "奖池 $amount 点按开奖随机数拼手气分配，每人至少 1 点。"
        }
        else -> "$slots 个名额 × $amount 点 = 共 ${amount * slots} 萌萌点。"
    }
}

private fun deliveryLabel(delivery: String): String = when (delivery) {
    "code" -> "兑换码"
    "point" -> "萌萌点"
    else -> "线下发放"
}

private fun extensionFor(mime: String): String = when (mime) {
    "image/jpeg" -> "jpg"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    "image/avif" -> "avif"
    else -> "png"
}
