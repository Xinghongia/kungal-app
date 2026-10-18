package com.kungal.app.ui.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.website.websiteVisitUrl

/**
 * 子网站（BP-05 U9，对应网页 /sites）。
 * 网页数据来自前端常量 `kunSubSites`，没有 API；这里保持同一份静态清单。
 */
private data class SubSite(
    val short: String,
    val name: String,
    val description: String,
    val link: String,
    val github: String? = null,
    val hint: String? = null,
    val icon: ImageVector,
)

private val SubSites = listOf(
    SubSite(
        short = "补丁站",
        name = "鲲 Galgame 补丁站",
        description = "鲲 Galgame 补丁站，提供 Galgame 补丁、整合与相关资源下载。",
        link = "https://www.moyu.moe/",
        hint = "震憾上线",
        icon = Icons.Outlined.Extension,
    ),
    SubSite(
        short = "账号中心",
        name = "NextMoe 账号中心",
        description = "统一 NextMoe 站点集群的所有用户账户，一个账号即可登录旗下的所有网站！",
        link = "https://account.nextmoe.com/",
        icon = Icons.Outlined.Key,
    ),
    SubSite(
        short = "表情包",
        name = "鲲 Galgame 表情包",
        description = "Galgame 表情包网站，鲲 Galgame 表情包下载站，海量 Visual Novel 表情包。",
        link = "https://sticker.kungal.com/",
        github = "https://github.com/KUN1007/kun-galgame-stickers-sveltekit",
        hint = "全网最全",
        icon = Icons.Outlined.Image,
    ),
    SubSite(
        short = "导航站",
        name = "鲲 Galgame 导航",
        description = "完全开源的导航站，可以前往鲲 Galgame 旗下的所有子网站！",
        link = "https://nav.kungal.org/",
        github = "https://github.com/KUN1007/kun-galgame-nav-solid",
        hint = "防失联",
        icon = Icons.Outlined.Navigation,
    ),
    SubSite(
        short = "开发文档",
        name = "鲲 Galgame 论坛开发文档",
        description = "鲲 Galgame 论坛是完全开源免费的！开发文档完全公开！",
        link = "https://docs-kungal.nextmoe.dev/",
        github = "https://github.com/KUN1007/soft.moe",
        icon = Icons.Outlined.Code,
    ),
)

@Composable
fun SitesScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "子网站",
                subtitle = "鲲 Galgame 旗下的其他站点与资源",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "note") {
                Text(
                    "鲲 Galgame 旗下的其他站点与资源，均为我们自己维护，欢迎逛逛~",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            items(SubSites.size, key = { SubSites[it].link }) { index ->
                val site = SubSites[index]
                SubSiteCard(
                    site = site,
                    onClick = { onOpenUrl(websiteVisitUrl(site.link)) },
                    onOpenGithub = site.github?.let { url -> { onOpenUrl(url) } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SubSiteCard(
    site: SubSite,
    onClick: () -> Unit,
    onOpenGithub: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    site.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    site.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                site.hint?.let { hint ->
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                site.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onOpenGithub != null) {
                Text(
                    "GitHub 开源地址",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenGithub)
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}
