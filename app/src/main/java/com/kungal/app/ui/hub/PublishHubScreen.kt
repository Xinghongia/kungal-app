package com.kungal.app.ui.hub

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.SettingsGroup

private val publishSections = listOf(
    HubSection(
        title = "发布",
        entries = listOf(
            HubEntry("发布话题", "讨论、求助、资讯与随笔", Icons.Outlined.Topic, "/edit/topic", "BP-01"),
            HubEntry("发布 Galgame", "投稿作品条目", Icons.Outlined.SportsEsports, "/edit/galgame/publish", "BP-02"),
            HubEntry("发布 Gal 工具", "分享工具与工具资源", Icons.Outlined.Build, "/edit/toolset/create", "BP-03"),
            HubEntry("发布 Gal 情报", "发布资讯", Icons.Outlined.Newspaper, "/edit/news", "BP-05"),
            HubEntry("发布 Galgame 习题", "出题", Icons.Outlined.Quiz, "/edit/galgame/quiz", "BP-05"),
        ),
    ),
)

/** 发布入口页：对齐官方「发布」分组；各发布器按领域蓝图交付。 */
@Composable
fun PublishHubScreen(
    onBack: () -> Unit,
    onOpen: (HubEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "发布",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        HubContent(
            sections = publishSections,
            onOpen = onOpen,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
