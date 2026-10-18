package com.kungal.app.ui.website

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.WebsiteCategory
import com.kungal.core.model.WebsiteEntry
import com.kungal.core.model.WebsiteTag

private const val ENTITY_PAGE_SIZE = 100
private const val ENTITY_MAX_PAGES = 20

/** 一次拉全某分类/标签下的全部网站（网页 useWebsiteList 也收集所有页）。 */
private suspend fun loadAll(
    app: KungalApp,
    includeNsfw: Boolean,
    categoryId: String? = null,
    tagId: String? = null,
): List<WebsiteEntry> {
    val all = mutableListOf<WebsiteEntry>()
    var cursor: String? = null
    var page = 0
    do {
        val result = app.websiteApi.list(
            cursor = cursor,
            limit = ENTITY_PAGE_SIZE,
            includeNsfw = includeNsfw,
            categoryId = categoryId,
            tagId = tagId,
        )
        all += result.items
        cursor = result.nextCursor
        page++
    } while (cursor != null && page < ENTITY_MAX_PAGES)
    return websiteSorted(all)
}

/** 分类详情（网页 /website-category/{slug}）：分类下全部网站，按价值降序。 */
@Composable
fun WebsiteCategoryScreen(
    app: KungalApp,
    slug: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWebsite: (String) -> Unit,
) {
    var category by remember(slug) { mutableStateOf<WebsiteCategory?>(null) }
    var sites by remember(slug) { mutableStateOf<List<WebsiteEntry>>(emptyList()) }
    var loading by remember(slug) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(slug, nsfwDisplay.allowsNsfw, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching {
            val data = app.websiteApi.category(slug)
            data to loadAll(app, nsfwDisplay.allowsNsfw, categoryId = data.id)
        }.onSuccess { (data, list) ->
            category = data
            sites = list
        }.onFailure {
            if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
        }
        loading = false
        refreshing = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = category?.label?.ifBlank { slug } ?: "网站分类",
                subtitle = category?.description?.takeIf { it.isNotBlank() },
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
            category == null -> EmptyBox(
                "未找到该网站分类",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val data = category!!
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
                    item(key = "header") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    data.label.ifBlank { data.slug },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "${sites.size} 个 ${data.label.ifBlank { data.slug }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (data.description.isNotBlank()) {
                                Text(
                                    data.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (sites.isEmpty()) {
                        item(key = "empty") {
                            EmptyBox("${data.label.ifBlank { data.slug }} 分类下暂无网站")
                        }
                    } else {
                        items(sites.size, key = { sites[it].id }) { index ->
                            val site = sites[index]
                            WebsiteCard(site = site, onClick = { onOpenWebsite(site.host) })
                        }
                    }
                }
                }
            }
        }
    }
}

/** 标签详情（网页 /website-tag/{slug}）：标签下全部网站，按价值降序。 */
@Composable
fun WebsiteTagScreen(
    app: KungalApp,
    slug: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWebsite: (String) -> Unit,
) {
    var tag by remember(slug) { mutableStateOf<WebsiteTag?>(null) }
    var sites by remember(slug) { mutableStateOf<List<WebsiteEntry>>(emptyList()) }
    var loading by remember(slug) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(slug, nsfwDisplay.allowsNsfw, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching {
            val data = app.websiteApi.tag(slug)
            data to loadAll(app, nsfwDisplay.allowsNsfw, tagId = data.id)
        }.onSuccess { (data, list) ->
            tag = data
            sites = list
        }.onFailure {
            if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
        }
        loading = false
        refreshing = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = tag?.let { "${it.label.ifBlank { it.slug }}的 Galgame 网站" } ?: "网站标签",
                subtitle = tag?.description?.takeIf { it.isNotBlank() },
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
            tag == null -> EmptyBox(
                "未找到该网站标签",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val data = tag!!
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
                    item(key = "header") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "标签价值 ${data.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "${sites.size} 个网站",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (data.description.isNotBlank()) {
                                Text(
                                    data.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (sites.isEmpty()) {
                        item(key = "empty") {
                            EmptyBox("${data.label.ifBlank { data.slug }} 标签下暂无网站")
                        }
                    } else {
                        items(sites.size, key = { sites[it].id }) { index ->
                            val site = sites[index]
                            WebsiteCard(site = site, onClick = { onOpenWebsite(site.host) })
                        }
                    }
                }
                }
            }
        }
    }
}
