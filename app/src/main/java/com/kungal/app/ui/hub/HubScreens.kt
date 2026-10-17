package com.kungal.app.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.GroupTone
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.SettingsGroup

/**
 * 一级入口页的数据模型：官方侧边栏的每个条目 → 应用内的入口。
 * webPath 同时作为占位页的「在浏览器打开网页版」兜底地址。
 */
data class HubEntry(
    val label: String,
    val supporting: String,
    val icon: ImageVector,
    val webPath: String,
    val note: String,
)

data class HubSection(val title: String, val entries: List<HubEntry>)

@Composable
fun HubScreen(
    title: String,
    subtitle: String,
    sections: List<HubSection>,
    onOpen: (HubEntry) -> Unit,
) {
    Scaffold(
        topBar = { KungalTopAppBar(title = title, subtitle = subtitle) },
    ) { padding ->
        HubContent(
            sections = sections,
            onOpen = onOpen,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
fun HubContent(
    sections: List<HubSection>,
    onOpen: (HubEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sections.forEach { section ->
            SettingsGroup(title = section.title) {
                section.entries.forEachIndexed { index, entry ->
                    item(
                        headline = entry.label,
                        supporting = entry.supporting,
                        icon = entry.icon,
                        tone = when (index % 3) {
                            0 -> GroupTone.PRIMARY
                            1 -> GroupTone.SECONDARY
                            else -> GroupTone.TERTIARY
                        },
                        onClick = { onOpen(entry) },
                    )
                }
            }
        }
    }
}

/** Galgame 一级页：对齐官方侧边栏 Galgame 分组的全部条目。 */
val GalgameSections: List<HubSection> = listOf(
    HubSection(
        title = "资料库",
        entries = listOf(
            HubEntry("Galgame", "作品条目浏览", Icons.Outlined.SportsEsports, "/galgame", "BP-02"),
            HubEntry("Galgame 数据库", "结构化资料检索", Icons.Outlined.MenuBook, "/gallib", "BP-02"),
            HubEntry("Galgame 评分", "八维评分与评分榜", Icons.Outlined.Star, "/galgame-rating", "BP-02"),
        ),
    ),
    HubSection(
        title = "发现",
        entries = listOf(
            HubEntry("发售月历", "本月与即将发售的新作", Icons.Outlined.CalendarMonth, "/galgame-calendar", "BP-05"),
            HubEntry("Galgame 情报", "资讯与新作消息", Icons.Outlined.Newspaper, "/news", "BP-05"),
            HubEntry("Galgame 题库", "答题、收藏与评分", Icons.Outlined.Quiz, "/galgame-quiz", "BP-05"),
        ),
    ),
    HubSection(
        title = "资源",
        entries = listOf(
            HubEntry("Gal 资源列表", "汉化、补丁与语音包", Icons.Outlined.Download, "/galgame/resource", "BP-03"),
            HubEntry("Gal 工具资源", "模拟器、翻译器等工具", Icons.Outlined.Build, "/toolset", "BP-03"),
            HubEntry("Gal 网站资料库", "Galgame 相关网站收录", Icons.Outlined.Language, "/website", "BP-05"),
        ),
    ),
    HubSection(
        title = "目录",
        entries = listOf(
            HubEntry("Galgame 会社", "按制作会社浏览", Icons.Outlined.Business, "/galgame/official", "BP-02"),
            HubEntry("Galgame 标签", "按标签浏览", Icons.Outlined.LocalOffer, "/galgame/tag", "BP-02"),
            HubEntry("Galgame 角色", "角色条目", Icons.Outlined.People, "/galgame/character", "BP-02"),
            HubEntry("Galgame 引擎", "按游戏引擎浏览", Icons.Outlined.Memory, "/galgame/engine", "BP-02"),
            HubEntry("Galgame 系列", "系列作品", Icons.Outlined.Layers, "/galgame/series", "BP-02"),
        ),
    ),
)

/** 发现（官方「其他」分组）：排行、动态、更新日志、帮助与链接。 */
val DiscoverSections: List<HubSection> = listOf(
    HubSection(
        title = "排行榜",
        entries = listOf(
            HubEntry("话题排行", "按热度与互动排序", Icons.Outlined.Leaderboard, "/ranking/topic", "BP-05"),
            HubEntry("Galgame 排行", "作品人气排行", Icons.Outlined.SportsEsports, "/ranking/galgame", "BP-05"),
            HubEntry("用户排行", "社区用户排行", Icons.Outlined.People, "/ranking/user", "BP-05"),
        ),
    ),
    HubSection(
        title = "网站动态",
        entries = listOf(
            HubEntry("动态时间线", "全站最新动态", Icons.Outlined.Timeline, "/activity", "BP-05"),
            HubEntry("分类动态", "按分类查看动态", Icons.Outlined.Layers, "/activity/category", "BP-05"),
        ),
    ),
    HubSection(
        title = "更新日志",
        entries = listOf(
            HubEntry("更新历史", "站点功能更新记录", Icons.Outlined.History, "/update/history", "BP-05"),
            HubEntry("待办列表", "站点开发计划", Icons.Outlined.HelpOutline, "/update/todo", "BP-05"),
        ),
    ),
    HubSection(
        title = "帮助与链接",
        entries = listOf(
            HubEntry("Galgame 帮助文档", "站点使用文档", Icons.Outlined.MenuBook, "/doc", "BP-05"),
            HubEntry("友情链接", "合作伙伴站点", Icons.Outlined.Link, "/friend-links", "BP-05"),
            HubEntry("子网站", "鲲 Galgame 生态的其他站点", Icons.Outlined.Apps, "/sites", "BP-05"),
        ),
    ),
)
