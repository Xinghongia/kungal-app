package com.kungal.app.ui.friendlinks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.website.websiteVisitUrl
import com.kungal.core.model.FriendLink

private const val FRIEND_LINK_PAGE_SIZE = 100
private const val FRIEND_LINK_MAX_PAGES = 20

private val FriendLinkCategoryOrder = listOf(
    "official" to "官方网站",
    "galgame" to "Galgame 网站",
    "others" to "其它网站",
)

private suspend fun loadAllFriendLinks(app: KungalApp): List<FriendLink> {
    val all = mutableListOf<FriendLink>()
    var cursor: String? = null
    var page = 0
    do {
        val result = app.friendLinkApi.list(cursor = cursor, limit = FRIEND_LINK_PAGE_SIZE)
        all += result.items
        cursor = result.nextCursor
        page++
    } while (cursor != null && page < FRIEND_LINK_MAX_PAGES)
    return all
}

/**
 * 友情链接（BP-05 U9，对应网页 /friend-links）：
 * 按官方网站 / Galgame 网站 / 其它网站分组，已下线站点带标记；点卡片打开外链。
 */
@Composable
fun FriendLinksScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenContact: () -> Unit,
) {
    var links by remember { mutableStateOf<List<FriendLink>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        if (!refreshing) loading = true
        error = null
        runCatching { loadAllFriendLinks(app) }
            .onSuccess { links = it }
            .onFailure {
                if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
            }
        loading = false
        refreshing = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "友情链接",
                subtitle = "与我们一起维护 Galgame 社区的伙伴",
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
            else -> KungalPullToRefresh(
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
                    contentPadding = PaddingValues(bottom = 28.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "note") {
                        Text(
                            "我们非常重视合作；如果下列网站出现任何付费行为，请随时联系我们撤销友情链接。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    if (links.isEmpty()) {
                        item(key = "empty") { EmptyBox("暂无友情链接") }
                    }
                    FriendLinkCategoryOrder.forEach { (key, label) ->
                        val group = links.filter { it.friendLinkCategory == key }
                        if (group.isNotEmpty()) {
                            item(key = "cat-$key") {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            items(group.size, key = { group[it].id }) { index ->
                                val link = group[index]
                                FriendLinkCard(
                                    link = link,
                                    onClick = { onOpenUrl(websiteVisitUrl(link.url)) },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                    item(key = "join") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenContact,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text("加入我们")
                            }
                            Text(
                                "要加入我们，请加入我们的群组并提供您的网站链接。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendLinkCard(link: FriendLink, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (link.state == "down") {
                    Text(
                        "已下线",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
            if (link.description.isNotBlank()) {
                Text(
                    if (link.description.length > 107) {
                        link.description.take(107) + "..."
                    } else {
                        link.description
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            link.banner?.url?.let { url ->
                KungalImage(
                    url = url,
                    contentDescription = link.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}
