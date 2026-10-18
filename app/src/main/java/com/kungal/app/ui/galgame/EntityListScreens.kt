package com.kungal.app.ui.galgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.core.model.CharacterSummary
import com.kungal.core.model.CompanySummary
import com.kungal.core.model.Engine
import com.kungal.core.model.SeriesSummary
import com.kungal.core.model.TagSummary
import com.kungal.core.model.TraitSummary

private fun zhName(displayName: String, localized: Map<String, com.kungal.core.model.LocalizedText>): String =
    localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() } ?: displayName

@Composable
private fun EntityListScaffold(
    title: String,
    subtitle: String,
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
        Column(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
fun TagListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    val includeNsfw = nsfwDisplay.allowsNsfw
    EntityListScaffold("Galgame 标签", "按标签浏览资料库", onBack) {
        PagedListContent(
            key = Triple("tags", query, includeNsfw),
            load = { page, _ ->
                val result = app.entityApi.tags(
                    q = query.ifBlank { null },
                    page = page,
                    limit = 24,
                    includeNsfw = includeNsfw,
                )
                PageResult(
                    result.items,
                    ended = result.items.size < 24 || page * 24 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到标签",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 24,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索标签名称",
                )
            },
        ) { tag ->
            EntityListRow(
                title = zhName(tag.displayName, tag.localized),
                subtitle = buildString {
                    append(if (tag.tagKind == "content") "内容标签" else "属性标签")
                    append(" · 含 ${tag.descendantCount} 个子标签")
                    if (tag.isSexual) append(" · 成人")
                },
                trailing = "${tag.catalogWorkCount} 部",
                onClick = { onOpenTag(tag.id) },
            )
        }
    }
}

private val CompanyKindOptions = listOf(
    "game_brand" to "游戏品牌",
    "bunko" to "文库",
    "publisher" to "发行商",
    "anime_studio" to "动画工作室",
    "doujin_circle" to "同人社团",
    "group" to "团体",
)

@Composable
fun CompanyListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenCompany: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<String?>(null) }
    EntityListScaffold("Galgame 会社", "按制作会社浏览作品", onBack) {
        PagedListContent(
            key = Triple("companies", query, kind),
            load = { page, _ ->
                val result = app.entityApi.companies(
                    q = query.ifBlank { null },
                    companyKind = kind,
                    page = page,
                    limit = 24,
                )
                PageResult(
                    result.items,
                    ended = result.items.size < 24 || page * 24 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到会社",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 24,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索会社名称",
                )
                FilterChips(
                    options = listOf("" to "全部") + CompanyKindOptions,
                    selected = kind.orEmpty(),
                    onSelect = { kind = it.ifBlank { null } },
                )
            },
        ) { company ->
            EntityListRow(
                title = zhName(company.displayName, company.localized),
                subtitle = CompanyKindOptions.firstOrNull { it.first == company.companyKind }?.second
                    ?: company.companyKind,
                trailing = "${company.catalogWorkCount} 部",
                onClick = { onOpenCompany(company.id) },
                leading = {
                    KungalImage(
                        url = company.logo?.url,
                        contentDescription = null,
                        modifier = Modifier.width(40.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp)),
                    )
                },
            )
        }
    }
}

@Composable
fun EngineListScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenEngine: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    EntityListScaffold("Galgame 引擎", "按游戏引擎浏览作品", onBack) {
        PagedListContent(
            key = Pair("engines", query),
            load = { page, _ ->
                val result = app.entityApi.engines(q = query.ifBlank { null }, page = page, limit = 24)
                PageResult(
                    result.items,
                    ended = result.items.size < 24 || page * 24 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到引擎",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 24,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索引擎名称",
                )
            },
        ) { engine ->
            EntityListRow(
                title = zhName(engine.displayName, engine.localized),
                subtitle = engine.description.take(80).ifBlank { null },
                trailing = "${engine.catalogWorkCount} 部",
                onClick = { onOpenEngine(engine.id) },
            )
        }
    }
}

@Composable
fun SeriesListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenSeries: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    EntityListScaffold("Galgame 系列", "按系列浏览作品", onBack) {
        PagedListContent(
            key = Triple("series", query, nsfwDisplay.allowsNsfw),
            load = { page, _ ->
                val result = app.entityApi.seriesList(
                    q = query.ifBlank { null },
                    page = page,
                    limit = 24,
                    includeNsfw = nsfwDisplay.allowsNsfw,
                )
                PageResult(
                    result.items,
                    ended = result.items.size < 24 || page * 24 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到系列",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 24,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索系列名称",
                )
            },
        ) { series ->
            SeriesListRow(series, onClick = { onOpenSeries(series.id) })
        }
    }
}

@Composable
private fun SeriesListRow(series: SeriesSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        EntityListRow(
            title = zhName(series.displayName, series.localized),
            subtitle = "本站收录 ${series.listedWorkCount} 部 · 资料库 ${series.catalogWorkCount} 部",
            trailing = null,
            onClick = onClick,
        )
        if (series.sampleWorks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                series.sampleWorks.take(5).forEach { sample ->
                    KungalImage(
                        url = sample.cover?.url ?: sample.banner?.url,
                        contentDescription = sample.displayName,
                        modifier = Modifier
                            .width(52.dp)
                            .aspectRatio(5f / 7f)
                            .clip(RoundedCornerShape(4.dp)),
                        fallback = { NoCoverArtwork() },
                    )
                }
            }
        }
    }
}

private val GenderOptions = listOf(
    "female" to "女",
    "male" to "男",
    "other" to "其他",
)

private val CharacterSortOptions = listOf(
    "popularity_desc" to "人气",
    "id_desc" to "最新",
    "relevance_desc" to "相关度",
)

@Composable
fun CharacterListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    initialTraitId: String,
    onBack: () -> Unit,
    onOpenCharacter: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var genders by rememberSaveable { mutableStateOf(setOf<String>()) }
    var sort by rememberSaveable { mutableStateOf("popularity_desc") }
    val includeNsfw = nsfwDisplay.allowsNsfw
    EntityListScaffold("Galgame 角色", "按名字、性别与特质浏览角色", onBack) {
        PagedListContent(
            key = listOf("characters", query, genders, sort, initialTraitId, includeNsfw),
            load = { page, _ ->
                val effectiveSort = if (sort == "relevance_desc" && query.isBlank()) {
                    "popularity_desc"
                } else {
                    sort
                }
                val result = app.entityApi.characters(
                    q = query.ifBlank { null },
                    traitIds = initialTraitId.takeIf { it.isNotBlank() }?.let { listOf(it) }
                        ?: emptyList(),
                    genders = genders.toList(),
                    sort = effectiveSort,
                    page = page,
                    limit = 24,
                    includeNsfw = includeNsfw,
                )
                PageResult(
                    result.items,
                    ended = result.items.size < 24 || page * 24 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到角色",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 24,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索角色名",
                )
                FilterChips(
                    options = listOf("" to "全部性别") + GenderOptions,
                    selected = genders.firstOrNull().orEmpty(),
                    onSelect = { value ->
                        genders = if (value.isBlank()) emptySet() else setOf(value)
                    },
                )
                FilterChips(
                    options = CharacterSortOptions.filter { it.first != "relevance_desc" || query.isNotBlank() },
                    selected = sort,
                    onSelect = { sort = it },
                )
                if (initialTraitId.isNotBlank()) {
                    Text(
                        "已按特质筛选；点开角色可查看该特质的详情",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            },
        ) { character ->
            CharacterListRow(character, nsfwDisplay, onOpenCharacter)
        }
    }
}

@Composable
private fun CharacterListRow(
    character: CharacterSummary,
    nsfwDisplay: NsfwDisplayMode,
    onOpenCharacter: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCharacter(character.id) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (nsfwDisplay.blursMedia && character.image?.sexual == "explicit") {
            com.kungal.app.ui.components.NsfwCoverImage(
                url = character.image?.url,
                modifier = Modifier.width(52.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)),
            )
        } else {
            KungalImage(
                url = character.image?.url,
                contentDescription = character.displayName,
                modifier = Modifier.width(52.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                zhName(character.displayName, character.localized),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "出演 ${character.catalogWorkCount} 部作品" +
                    if (character.matchedTraits.isNotEmpty()) {
                        " · " + character.matchedTraits.joinToString(" / ") {
                            zhName(it.displayName, it.localized)
                        }
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.TextButton(onClick = { onOpenCharacter(character.id) }) {
            Text("详情")
        }
    }
}

@Composable
fun TraitListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTrait: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    val includeNsfw = nsfwDisplay.allowsNsfw
    EntityListScaffold("Galgame 特质", "角色的发色、性格等特征分组", onBack) {
        PagedListContent(
            key = Triple("root-traits", query, includeNsfw),
            load = { page, _ ->
                val result = app.entityApi.traits(
                    q = query.ifBlank { null },
                    page = page,
                    limit = 50,
                    includeNsfw = includeNsfw,
                )
                PageResult(
                    result.items,
                    ended = result.items.size < 50 || page * 50 >= result.total,
                    total = result.total,
                )
            },
            emptyText = "没有找到特质",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            pagination = true,
            pageSize = 50,
            header = {
                EntitySearchRow(
                    value = draft,
                    onValueChange = { draft = it },
                    onSearch = { query = draft.trim() },
                    placeholder = "搜索特质",
                )
                if (query.isBlank()) {
                    Text(
                        "以下为特质根分组；点开可查看其下特质与相关角色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            },
        ) { trait ->
            EntityListRow(
                title = zhName(trait.displayName, trait.localized),
                subtitle = "子特质 ${trait.childCount} 个 · 相关角色 ${trait.characterCount} 人",
                trailing = null,
                onClick = { onOpenTrait(trait.id) },
            )
        }
    }
}
