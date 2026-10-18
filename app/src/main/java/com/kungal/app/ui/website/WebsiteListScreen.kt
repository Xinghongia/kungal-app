package com.kungal.app.ui.website

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.WebsiteCategory
import com.kungal.core.model.WebsiteEntry

private const val WEBSITE_PAGE_SIZE = 100
private const val WEBSITE_MAX_PAGES = 20

/**
 * 网站资料库总览（BP-05 U7，对应网页 /website）：
 * 与网页一致一次拉全量（100/页）后在本地按分类分组、组内按分数排序；
 * 已关站单独存档区；搜索匹配标题/描述/域名；NSFW 站点跟随内容姿态。
 */
@Composable
fun WebsiteListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWebsite: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    var entries by remember { mutableStateOf<List<WebsiteEntry>>(emptyList()) }
    var categories by remember { mutableStateOf<List<WebsiteCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(nsfwDisplay.allowsNsfw, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching {
            categories = app.websiteApi.categories()
            val all = mutableListOf<WebsiteEntry>()
            var cursor: String? = null
            var page = 0
            do {
                val result = app.websiteApi.list(
                    cursor = cursor,
                    limit = WEBSITE_PAGE_SIZE,
                    includeTotal = page == 0,
                    includeNsfw = nsfwDisplay.allowsNsfw,
                )
                if (page == 0) total = result.total ?: result.items.size
                all += result.items
                cursor = result.nextCursor
                page++
            } while (cursor != null && page < WEBSITE_MAX_PAGES)
            all
        }.onSuccess { entries = it }
            .onFailure {
                // 下拉刷新失败时保留原列表，只提示；首次加载失败才整页报错。
                if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
            }
        loading = false
        refreshing = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 网站资料库",
                subtitle = "免费 Galgame 相关网站汇总",
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
            error != null -> ErrorBox(
                error!!,
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val matched = entries.filter { websiteMatches(it, query) }
                val (sections, closed) = websiteSections(matched, categories)
                KungalPullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = {
                        if (!refreshing) {
                            refreshing = true
                            reload++
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("搜索 Galgame 网站") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "清空")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    if (!nsfwDisplay.allowsNsfw) {
                        item(key = "nsfw-note") {
                            Text(
                                "默认只显示 SFW 网站；NSFW 网站请在设置打开内容显示。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            )
                        }
                    }
                    item(key = "count") {
                        Text(
                            "${if (query.isBlank()) total.coerceAtLeast(entries.size) else matched.size} 个网站",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }

                    sections.forEachIndexed { index, section ->
                        item(key = "section-$index") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCategory(section.category.slug) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    section.category.label.ifBlank { section.category.slug },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${section.sites.size} 个网站",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        itemsOf(section.sites, prefix = "section-$index") { site ->
                            WebsiteCard(site = site, onClick = { onOpenWebsite(site.host) })
                        }
                    }

                    if (closed.isNotEmpty()) {
                        item(key = "closed-title") {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                HorizontalDivider(Modifier.padding(bottom = 10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "已关站",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${closed.size} 个网站",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "这些网站已经停止运营，仅作存档保留，链接大概率已经失效。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        itemsOf(closed, prefix = "closed") { site ->
                            WebsiteCard(site = site, onClick = { onOpenWebsite(site.host) })
                        }
                    }

                    if (sections.isEmpty() && closed.isEmpty()) {
                        item(key = "empty") { EmptyBox("没有找到符合条件的网站") }
                    }
                }
                }
            }
        }
    }
}

/** LazyColumn 的小工具：把一组网站按 id 作为 key 铺开。 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsOf(
    sites: List<WebsiteEntry>,
    prefix: String,
    card: @Composable (WebsiteEntry) -> Unit,
) {
    items(sites.size, key = { "$prefix-${sites[it].id}" }) { index ->
        card(sites[index])
    }
}

/** 资料库卡片：图标、标题与状态、域名、简介、价值精算值（网页 Card.vue）。 */
@Composable
fun WebsiteCard(site: WebsiteEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WebsiteIcon(
                    site = site,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            site.title.ifBlank { site.host },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        websiteStateBadge(site.state)?.let { label ->
                            val closed = site.state == "closed"
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (closed) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        (if (closed) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.tertiary).copy(alpha = 0.12f),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                        if (site.isNsfw) {
                            Text(
                                "NSFW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(
                        site.host,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                site.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "网站价值精算值",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    site.score.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (websiteScoreTone(site.score)) {
                        WebsiteScoreTone.TOP -> Color(0xFFF59E0B)
                        WebsiteScoreTone.GOOD -> Color(0xFF16A34A)
                        WebsiteScoreTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
                        WebsiteScoreTone.BAD -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

/** 站点图标：无图时显示地球图标占位；已关站灰显。 */
@Composable
fun WebsiteIcon(
    site: WebsiteEntry,
    modifier: Modifier = Modifier,
) {
    val url = websiteIconUrl(site.icon, site.externalIconUrl)
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    } else {
        Box(modifier) {
            KungalImage(
                url = url,
                contentDescription = site.title,
                modifier = Modifier.fillMaxSize(),
            )
            if (site.state == "closed") {
                Box(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.45f)))
            }
        }
    }
}

/** 详情页的小信息行：左标签右值。 */
@Composable
fun WebsiteInfoRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}
