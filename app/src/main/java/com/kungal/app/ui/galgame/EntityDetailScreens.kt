package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.content.FullScreenImageViewer
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.CharacterDetail
import com.kungal.core.model.CompanyDetail
import com.kungal.core.model.CompanyGraph
import com.kungal.core.model.Credit
import com.kungal.core.model.CreditNameDetail
import com.kungal.core.model.Engine
import com.kungal.core.model.SeriesDetail
import com.kungal.core.model.TagBlock
import com.kungal.core.model.TagDetail
import com.kungal.core.model.TraitDetail
import kotlinx.coroutines.launch

@Composable
private fun EntityDetailScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
private fun DetailCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun EntityIntro(intros: List<com.kungal.core.model.CatalogIntro>) {
    val intro = pickIntro(intros) ?: return
    Text(
        markdownToPlainText(intro.value),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (intro.isMachine) {
        Text(
            "本段简介由机器翻译生成，与原文可能有出入",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LinkChips(
    links: List<com.kungal.core.model.CatalogLink>,
    onOpenWeb: (String) -> Unit,
) {
    if (links.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        links.forEach { link ->
            AssistChip(
                onClick = { onOpenWeb(link.url) },
                label = { Text(catalogLinkLabel(link.site, link.url), style = MaterialTheme.typography.labelSmall) },
                trailingIcon = {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                },
            )
        }
    }
}

// ---------- 标签 ----------

@Composable
fun TagDetailScreen(
    app: KungalApp,
    tagId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val includeNsfw = nsfwDisplay.allowsNsfw
    var tag by remember(tagId) { mutableStateOf<TagDetail?>(null) }
    var error by remember(tagId) { mutableStateOf<String?>(null) }
    var loading by remember(tagId) { mutableStateOf(true) }
    var block by remember(tagId) { mutableStateOf<TagBlock?>(null) }
    var blockDialog by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.entityApi.tag(tagId, includeNsfw) }
                .onSuccess { tag = it; loading = false }
                .onFailure { error = friendlyError(it); loading = false }
        }
    }

    LaunchedEffect(tagId, includeNsfw) {
        load()
        if (app.isLoggedIn) {
            runCatching { app.entityApi.tagBlockOrNull(tagId) }.onSuccess { block = it }
        }
    }

    EntityDetailScaffold(
        title = "Galgame 标签",
        subtitle = tag?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { load() })
            tag == null -> EmptyBox("未找到这个标签")
            else -> EntityWorksList(
                entityKey = "tag-works-$tagId",
                includeNsfw = includeNsfw,
                blurNsfw = nsfwDisplay.blursMedia,
                load = { query -> app.entityApi.tagWorks(tagId, query) },
                onOpenWork = onOpenWork,
                header = {
                    DetailCard(title = tag!!.localized["zh-Hans"]?.value ?: tag!!.displayName) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(if (tag!!.tagKind == "content") "内容标签" else "属性标签")
                            if (tag!!.isSexual) Chip("成人内容")
                            Chip("资料库 ${tag!!.catalogWorkCount} 部")
                            Chip("子标签 ${tag!!.descendantCount} 个")
                        }
                        EntityIntro(tag!!.intros)
                        if (tag!!.ancestors.isNotEmpty()) {
                            Text(
                                "上级标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                tag!!.ancestors.takeLast(4).forEach { ancestor ->
                                    AssistChip(
                                        onClick = { onOpenTag(ancestor.id) },
                                        label = {
                                            Text(
                                                ancestor.localized["zh-Hans"]?.value ?: ancestor.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        if (tag!!.subtags.isNotEmpty()) {
                            Text(
                                "子标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                tag!!.subtags.take(12).forEach { sub ->
                                    AssistChip(
                                        onClick = { onOpenTag(sub.id) },
                                        label = {
                                            Text(
                                                sub.localized["zh-Hans"]?.value ?: sub.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                if (app.isLoggedIn) blockDialog = true else onOpenLogin()
                            }) {
                                Text(
                                    when (block?.mode) {
                                        "tree" -> "已屏蔽（含子标签）"
                                        "exact" -> "已屏蔽（仅此标签）"
                                        else -> "屏蔽此标签"
                                    },
                                )
                            }
                            if (block != null) {
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { app.entityApi.removeTagBlock(tagId) }
                                            .onSuccess { block = null }
                                            .onFailure { AppMessages.show(friendlyError(it)) }
                                    }
                                }) { Text("取消屏蔽") }
                            }
                        }
                        Text(
                            "屏蔽后，标签浏览与作品列表会排除该标签；可在列表中通过筛选继续看到",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }

    if (blockDialog) {
        AlertDialog(
            onDismissRequest = { blockDialog = false },
            title = { Text("屏蔽标签") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("含子标签：屏蔽该标签及其全部下级标签（推荐）")
                    Text("仅此标签：只屏蔽这个标签本身")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    blockDialog = false
                    scope.launch {
                        runCatching { app.entityApi.setTagBlock(tagId, "tree") }
                            .onSuccess { block = it }
                            .onFailure { AppMessages.show(friendlyError(it)) }
                    }
                }) { Text("含子标签") }
            },
            dismissButton = {
                TextButton(onClick = {
                    blockDialog = false
                    scope.launch {
                        runCatching { app.entityApi.setTagBlock(tagId, "exact") }
                            .onSuccess { block = it }
                            .onFailure { AppMessages.show(friendlyError(it)) }
                    }
                }) { Text("仅此标签") }
            },
        )
    }
}

// ---------- 会社 ----------

private val CompanyRelationLabels = mapOf(
    "parent" to "母公司",
    "subsidiary" to "子公司",
    "imprint" to "旗下品牌",
    "imprint_of" to "所属品牌",
    "succeeded_by" to "后继公司",
    "formerly" to "前身",
    "spawned" to "衍生公司",
    "origin" to "起源公司",
)

@Composable
fun CompanyDetailScreen(
    app: KungalApp,
    companyId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenCompany: (String) -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var company by remember(companyId) { mutableStateOf<CompanyDetail?>(null) }
    var graph by remember(companyId) { mutableStateOf<CompanyGraph?>(null) }
    var error by remember(companyId) { mutableStateOf<String?>(null) }
    var loading by remember(companyId) { mutableStateOf(true) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.entityApi.company(companyId) }
                .onSuccess { company = it }
                .onFailure { error = friendlyError(it); loading = false; return@launch }
            runCatching { app.entityApi.companyGraph(companyId) }
                .onSuccess { graph = it }
            loading = false
        }
    }
    LaunchedEffect(companyId) { load() }

    EntityDetailScaffold(
        title = "Galgame 会社",
        subtitle = company?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { load() })
            company == null -> EmptyBox("未找到这个会社")
            else -> EntityWorksList(
                entityKey = "company-works-$companyId",
                includeNsfw = nsfwDisplay.allowsNsfw,
                blurNsfw = nsfwDisplay.blursMedia,
                load = { query -> app.entityApi.companyWorks(companyId, query) },
                onOpenWork = onOpenWork,
                showVia = true,
                header = {
                    DetailCard(title = company!!.localized["zh-Hans"]?.value ?: company!!.displayName) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (company!!.logo != null) {
                                KungalImage(
                                    url = company!!.logo?.url,
                                    contentDescription = null,
                                    modifier = Modifier.width(52.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Chip(company!!.companyKind)
                                if (company!!.aliases.isNotEmpty()) {
                                    Text(
                                        company!!.aliases.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "资料库收录 ${company!!.catalogWorkCount} 部作品",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        EntityIntro(company!!.intros)
                        LinkChips(company!!.links, onOpenWeb)
                        val graphValue = graph
                        val nodes = graphValue?.nodes.orEmpty()
                        val edges = graphValue?.edges
                            .orEmpty()
                            .filter { it.fromCompanyId == companyId }
                        if (edges.isNotEmpty()) {
                            Text(
                                "会社关系",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            edges.forEach { edge ->
                                val target = nodes.firstOrNull { it.id == edge.toCompanyId }
                                if (target != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenCompany(target.id) }
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Chip(CompanyRelationLabels[edge.relation] ?: edge.relation)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            target.localized["zh-Hans"]?.value ?: target.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

// ---------- 引擎 ----------

@Composable
fun EngineDetailScreen(
    app: KungalApp,
    engineId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
) {
    var engine by remember(engineId) { mutableStateOf<Engine?>(null) }
    var error by remember(engineId) { mutableStateOf<String?>(null) }
    var loading by remember(engineId) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(engineId, reload) {
        loading = true
        error = null
        runCatching { app.entityApi.engine(engineId) }
            .onSuccess { engine = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    EntityDetailScaffold(
        title = "Galgame 引擎",
        subtitle = engine?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            engine == null -> EmptyBox("未找到这个引擎")
            else -> EntityWorksList(
                entityKey = "engine-works-$engineId",
                includeNsfw = nsfwDisplay.allowsNsfw,
                blurNsfw = nsfwDisplay.blursMedia,
                load = { query -> app.entityApi.engineWorks(engineId, query) },
                onOpenWork = onOpenWork,
                header = {
                    DetailCard(title = engine!!.localized["zh-Hans"]?.value ?: engine!!.displayName) {
                        Chip("资料库 ${engine!!.catalogWorkCount} 部作品")
                        if (engine!!.aliases.isNotEmpty()) {
                            Text(
                                engine!!.aliases.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (engine!!.description.isNotBlank()) {
                            Text(engine!!.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
            )
        }
    }
}

// ---------- 系列 ----------

@Composable
fun SeriesDetailScreen(
    app: KungalApp,
    seriesId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
) {
    var series by remember(seriesId) { mutableStateOf<SeriesDetail?>(null) }
    var error by remember(seriesId) { mutableStateOf<String?>(null) }
    var loading by remember(seriesId) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(seriesId, reload) {
        loading = true
        runCatching { app.entityApi.series(seriesId) }
            .onSuccess { series = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    EntityDetailScaffold(
        title = "Galgame 系列",
        subtitle = series?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            series == null -> EmptyBox("未找到这个系列")
            else -> EntityWorksList(
                entityKey = "series-works-$seriesId",
                includeNsfw = nsfwDisplay.allowsNsfw,
                blurNsfw = nsfwDisplay.blursMedia,
                load = { query -> app.entityApi.seriesWorks(seriesId, query) },
                onOpenWork = onOpenWork,
                header = {
                    DetailCard(title = series!!.localized["zh-Hans"]?.value ?: series!!.displayName) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip("本站收录 ${series!!.listedWorkCount} 部")
                            Chip("资料库 ${series!!.catalogWorkCount} 部")
                        }
                        EntityIntro(series!!.intros)
                        if (series!!.sampleWorks.isNotEmpty()) {
                            Text(
                                "系列作品",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                series!!.sampleWorks.take(5).forEach { sample ->
                                    KungalImage(
                                        url = sample.cover?.url ?: sample.banner?.url,
                                        contentDescription = sample.displayName,
                                        modifier = Modifier
                                            .width(52.dp)
                                            .aspectRatio(5f / 7f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onOpenWork(sample.id) },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

// ---------- 角色 ----------

@Composable
fun CharacterDetailScreen(
    app: KungalApp,
    characterId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenTrait: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var character by remember(characterId) { mutableStateOf<CharacterDetail?>(null) }
    var error by remember(characterId) { mutableStateOf<String?>(null) }
    var loading by remember(characterId) { mutableStateOf(true) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(characterId, nsfwDisplay.allowsNsfw, reload) {
        loading = true
        error = null
        runCatching { app.entityApi.character(characterId, nsfwDisplay.allowsNsfw) }
            .onSuccess { character = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    EntityDetailScaffold(
        title = "Galgame 角色",
        subtitle = character?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            character == null -> EmptyBox("未找到这个角色")
            else -> PagedListContent(
                key = Triple("appearances", characterId, nsfwDisplay.allowsNsfw),
                load = { _, cursor ->
                    val result = app.entityApi.characterAppearances(
                        characterId = characterId,
                        cursor = cursor,
                        limit = 24,
                        includeNsfw = nsfwDisplay.allowsNsfw,
                    )
                    PageResult(
                        items = result.items,
                        nextCursor = result.nextCursor,
                        ended = result.nextCursor == null,
                    )
                },
                emptyText = "这个角色还没有收录出演作品",
                modifier = Modifier.fillMaxSize(),
                keyOf = { it.workSummary.id },
                header = {
                    DetailCard(title = character!!.localized["zh-Hans"]?.value ?: character!!.displayName) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val art = character!!.figure ?: character!!.image
                            if (art != null) {
                                if (nsfwDisplay.blursMedia && art.sexual == "explicit") {
                                    NsfwCoverImage(
                                        url = art.url,
                                        modifier = Modifier
                                            .width(96.dp)
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    KungalImage(
                                        url = art.url,
                                        contentDescription = character!!.displayName,
                                        modifier = Modifier
                                            .width(96.dp)
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewerUrl = art.url },
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (character!!.latin != null) {
                                    Text(
                                        character!!.latin!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                EntityIntro(character!!.intros)
                                LinkChips(character!!.links, onOpenWeb)
                            }
                        }
                        if (character!!.traits.isNotEmpty()) {
                            Text(
                                "特质",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                character!!.traits.take(16).forEach { trait ->
                                    AssistChip(
                                        onClick = { onOpenTrait(trait.id) },
                                        label = {
                                            Text(
                                                buildString {
                                                    append(trait.localized["zh-Hans"]?.value ?: trait.displayName)
                                                    if (trait.spoiler != "none") append(" · 剧透")
                                                    if (trait.isLie) append(" · 谎言")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        Text(
                            "出演作品",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            ) { appearance ->
                Column {
                    EntityWorkRow(
                        work = appearance.workSummary,
                        blurCover = nsfwDisplay.blursMedia,
                        onClick = { onOpenWork(appearance.workSummary.id) },
                    )
                    if (appearance.voices.isNotEmpty()) {
                        Text(
                            "CV: " + appearance.voices.joinToString(" / ") {
                                it.localized["zh-Hans"]?.value ?: it.displayName
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 80.dp, bottom = 6.dp),
                        )
                    }
                }
            }
        }
    }
    viewerUrl?.let { url -> FullScreenImageViewer(url = url, onDismiss = { viewerUrl = null }) }
}

// ---------- Staff ----------

@Composable
fun StaffDetailScreen(
    app: KungalApp,
    staffId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    var staff by remember(staffId) { mutableStateOf<CreditNameDetail?>(null) }
    var error by remember(staffId) { mutableStateOf<String?>(null) }
    var loading by remember(staffId) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(staffId, reload) {
        loading = true
        error = null
        runCatching { app.entityApi.creditName(staffId) }
            .onSuccess { staff = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    EntityDetailScaffold(
        title = "制作人员",
        subtitle = staff?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            staff == null -> EmptyBox("未找到这位制作人员")
            else -> PagedListContent(
                key = Triple("staff-credits", staffId, nsfwDisplay.allowsNsfw),
                load = { _, cursor ->
                    val result = app.entityApi.creditNameCredits(
                        creditNameId = staffId,
                        cursor = cursor,
                        limit = 24,
                        includeNsfw = nsfwDisplay.allowsNsfw,
                    )
                    PageResult(
                        items = result.items,
                        nextCursor = result.nextCursor,
                        ended = result.nextCursor == null,
                    )
                },
                emptyText = "这位制作人员还没有收录作品",
                modifier = Modifier.fillMaxSize(),
                keyOf = { it.workSummary.id },
                header = {
                    DetailCard(title = staff!!.localized["zh-Hans"]?.value ?: staff!!.displayName) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            staff!!.photo?.let { photo ->
                                KungalImage(
                                    url = photo.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(88.dp)
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (staff!!.gender != null) {
                                    Text(
                                        if (staff!!.gender == "female") "女" else "男",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                birthdayText(staff!!.birthYear, staff!!.birthMonth, staff!!.birthDay)?.let {
                                    Text(
                                        "生日 $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (staff!!.siblings.isNotEmpty()) {
                                    Text(
                                        "其他名义: " + staff!!.siblings.joinToString(" / ") {
                                            it.localized["zh-Hans"]?.value ?: it.displayName
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                EntityIntro(staff!!.intros)
                                LinkChips(staff!!.links, onOpenWeb)
                            }
                        }
                        Text(
                            "参与作品",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            ) { credit ->
                Column {
                    EntityWorkRow(
                        work = credit.workSummary,
                        blurCover = nsfwDisplay.blursMedia,
                        onClick = { onOpenWork(credit.workSummary.id) },
                    )
                    Text(
                        creditLine(credit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 80.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
}

private fun creditLine(credit: Credit): String {
    val roles = credit.creditRoles.joinToString(" / ") { it.displayName }
    val characters = credit.characters.joinToString(" / ") { it.displayName }
    return if (characters.isNotBlank()) "$roles（$characters）" else roles
}

private fun birthdayText(year: Int?, month: Int?, day: Int?): String? {
    if (year == null && month == null && day == null) return null
    val y = year?.toString() ?: "????"
    val m = month?.toString()?.padStart(2, '0') ?: "??"
    val d = day?.toString()?.padStart(2, '0') ?: "??"
    return "$y-$m-$d"
}

// ---------- 特质 ----------

@Composable
fun TraitDetailScreen(
    app: KungalApp,
    traitId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTrait: (String) -> Unit,
    onOpenCharacterList: (String) -> Unit,
    onOpenCharacter: (String) -> Unit,
) {
    var trait by remember(traitId) { mutableStateOf<TraitDetail?>(null) }
    var error by remember(traitId) { mutableStateOf<String?>(null) }
    var loading by remember(traitId) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(traitId, nsfwDisplay.allowsNsfw, reload) {
        loading = true
        error = null
        runCatching { app.entityApi.trait(traitId, nsfwDisplay.allowsNsfw) }
            .onSuccess { trait = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    EntityDetailScaffold(
        title = "Galgame 特质",
        subtitle = trait?.let { it.localized["zh-Hans"]?.value ?: it.displayName },
        onBack = onBack,
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            trait == null -> EmptyBox("未找到这个特质")
            else -> PagedListContent(
                key = Triple("trait-characters", traitId, nsfwDisplay.allowsNsfw),
                load = { page, _ ->
                    val result = app.entityApi.characters(
                        traitIds = listOf(traitId),
                        page = page,
                        limit = 24,
                        includeNsfw = nsfwDisplay.allowsNsfw,
                    )
                    PageResult(result.items, ended = result.items.size < 24 || page * 24 >= result.total)
                },
                emptyText = "没有拥有该特质的角色",
                modifier = Modifier.fillMaxSize(),
                keyOf = { it.id },
                header = {
                    DetailCard(title = "特质") {
                        Text(
                            trait!!.localized["zh-Hans"]?.value ?: trait!!.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(
                                trait!!.traitGroup.localized["zh-Hans"]?.value
                                    ?: trait!!.traitGroup.displayName,
                            )
                            Chip("相关角色 ${trait!!.characterCount} 人")
                            Chip("子特质 ${trait!!.childCount} 个")
                            if (trait!!.isSexual) Chip("成人内容")
                        }
                        if (trait!!.aliases.isNotEmpty()) {
                            Text(
                                "别名: " + trait!!.aliases.joinToString(" / "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (trait!!.description.isNotBlank()) {
                            Text(trait!!.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        EntityIntro(trait!!.intros)
                        if (trait!!.parents.isNotEmpty()) {
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                trait!!.parents.forEach { parent ->
                                    AssistChip(
                                        onClick = { onOpenTrait(parent.id) },
                                        label = {
                                            Text(
                                                parent.localized["zh-Hans"]?.value ?: parent.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        if (trait!!.subtraits.isNotEmpty()) {
                            Text(
                                "下级特质",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                trait!!.subtraits.take(20).forEach { sub ->
                                    AssistChip(
                                        onClick = { onOpenTrait(sub.id) },
                                        label = {
                                            Text(
                                                sub.localized["zh-Hans"]?.value ?: sub.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { onOpenCharacterList(traitId) }) {
                            Text("查看拥有该特质的角色")
                        }
                        Text(
                            "拥有该特质的角色",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            ) { character ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCharacter(character.id) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KungalImage(
                        url = character.image?.url,
                        contentDescription = character.displayName,
                        modifier = Modifier.width(48.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            character.localized["zh-Hans"]?.value ?: character.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "出演 ${character.catalogWorkCount} 部作品",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
