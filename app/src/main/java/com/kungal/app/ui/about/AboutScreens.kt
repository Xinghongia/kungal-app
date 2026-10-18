package com.kungal.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.GitHubProfileSnapshot
import com.kungal.app.KungalApp
import com.kungal.app.isGitHubCacheFresh
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.api.GitHubPublicApi
import com.kungal.core.model.GitHubContributions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val GITHUB_USER = "Xinghongia"
private const val GITHUB_URL = "https://github.com/$GITHUB_USER"
private const val BLOG_URL = "https://boke.hiromu.top/"
private const val AVATAR_URL = "https://avatars.githubusercontent.com/$GITHUB_USER?s=200"

private data class AboutProject(
    val repo: String,
    val name: String,
    val description: String,
    val language: String,
    val isFork: Boolean = false,
    val comingSoon: Boolean = false,
)

private val AboutProjects = listOf(
    AboutProject(
        repo = "kungal-app",
        name = "kungal-app",
        description = "鲲 Galgame 论坛的第三方安卓客户端（Kotlin + Jetpack Compose），功能对照网页逐项实现。",
        language = "Kotlin",
        comingSoon = true,
    ),
    AboutProject(
        repo = "Kirameku",
        name = "Kirameku",
        description = "基于 Next.js 16 + FastAPI 构建的高颜值前后端分离个人博客。毛玻璃设计、暗色模式、" +
            "说说/杂谈/评论区、GitHub OAuth 登录、RSS 订阅、Markdown 写作、独立后台管理面板，" +
            "简单部署、一站式开箱即用。",
        language = "TypeScript",
    ),
    AboutProject(
        repo = "qoder2api",
        name = "qoder2api",
        description = "Qoder 国际国内双区多账号 OpenAI 兼容网关中枢 —— COSY 签名推理、稳定设备指纹防风控、" +
            "OAuth 设备授权免客户端登录、每日签到与 Pro 福利、后台定时调度器、Web 监控看板，" +
            "支持 Codex / Claude Code 与标准 OpenAI 客户端。",
        language = "Python",
    ),
    AboutProject(
        repo = "saku",
        name = "saku",
        description = "sakuraxy.top 第三方安卓客户端（Kotlin + Jetpack Compose）。",
        language = "Kotlin",
    ),
    AboutProject(
        repo = "grokRegister-cpa",
        name = "grokRegister-cpa",
        description = "Grok 自动注册机 —— 注册成功后自动入库 CLIProxyAPI (CPA)：支持本地 auth 目录与远程 " +
            "Management API 上传。",
        language = "Python",
        isFork = true,
    ),
)

/** 免责说明（我的 → 关于）。 */
@Composable
fun DisclaimerScreen(onBack: () -> Unit) {
    LegalPage(
        title = "免责说明",
        intro = "使用本应用前，请阅读以下说明。继续使用即表示你理解并接受这些内容。",
        sections = listOf(
            "非官方声明" to
                "本应用是 kungal.com（鲲 Galgame 论坛）的第三方客户端，由个人开发维护，" +
                "与站点运营方没有隶属、合作或代理关系，也不代表站点立场。" +
                "应用名称、站点图标与站点内容的相关权利归站点及其原作者所有。",
            "数据与权限" to
                "应用内的内容全部来自站点公开接口，可见范围与可操作性与网页保持一致；" +
                "不绕过登录、权限、内容分级或剧透门禁，也不会批量抓取或离线镜像站点内容。",
            "账号" to
                "登录使用站点官方账号体系，应用不保存你的密码；登录会话保存在应用私有目录，" +
                "退出登录即会清除。账号本身的状态与处罚由站点管理。",
            "内容责任" to
                "用户发布的话题、评论、投稿等内容由发布者负责。应用只提供查看与提交的通道，" +
                "不对第三方内容的准确性、合法性作担保。",
            "服务可用性" to
                "站点接口或页面结构发生变化时，部分功能可能暂时不可用。本应用按“现状”提供，" +
                "不对可用性、连续性作任何明示或暗示的担保。",
            "问题反馈" to
                "发现 bug 或有功能建议，欢迎在 GitHub 仓库提 Issue：" +
                "github.com/$GITHUB_USER/kungal-app。",
        ),
        onBack = onBack,
    )
}

/** 隐私协议（我的 → 关于），内容按应用实际行为编写。 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalPage(
        title = "隐私协议",
        intro = "这份说明按应用的实际行为编写，说明哪些数据保存在本机、会向哪些网站发起请求。",
        sections = listOf(
            "保存在本机的数据" to
                "· 登录会话 cookie（应用私有目录，当前为明文存储；退出登录即清除）\n" +
                "· 偏好设置：主题、动态取色、内容分级、默认排序等\n" +
                "· 搜索历史：最多 20 条，可单条删除或清空\n" +
                "· 工具集上传进度\n" +
                "· GitHub 公开资料缓存：贡献日历与仓库 star 数，保留 24 小时\n" +
                "卸载应用会清除以上全部本地数据。",
            "会访问的网站" to
                "· www.kungal.com：站点官方 API\n" +
                "· image.kungal.iloveren.link：图片 CDN\n" +
                "· sticker.kungal.com：默认头像池\n" +
                "· account.nextmoe.com：账号中心（网页登录时由浏览器访问）\n" +
                "· github.com / avatars.githubusercontent.com：仅“关于我”页面，" +
                "读取公开贡献、仓库 star 数与头像\n" +
                "打开外链时由浏览器访问对应网站，适用该网站自己的隐私政策。",
            "不会收集的数据" to
                "应用没有统计、广告或崩溃上报 SDK；不读取通讯录、位置、短信、通话记录；" +
                "不采集设备标识符用于追踪或画像。",
            "数据共享" to
                "除上述为完成功能而发起的站点请求外，不向第三方出售、出租或共享你的数据。",
            "你的控制" to
                "· 在设置页管理外观与浏览偏好\n" +
                "· 在搜索页删除单条或全部搜索历史\n" +
                "· 退出登录会清除本机会话\n" +
                "· 卸载应用会清除全部本地数据",
            "变更" to
                "本说明会随功能更新调整；涉及数据处理的重大变化会写进应用更新日志。",
        ),
        onBack = onBack,
    )
}

@Composable
private fun LegalPage(
    title: String,
    intro: String,
    sections: List<Pair<String, String>>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = title,
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(sections.size) { index ->
                val (heading, body) = sections[index]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5f,
                        )
                    }
                }
            }
            item {
                Text(
                    text = "本页内容随应用版本更新，最近更新：0.8.14",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 关于我：个人介绍、项目列表与 GitHub 公开贡献图。 */
@Composable
fun AboutMeScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var snapshot by remember { mutableStateOf<GitHubProfileSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        val cached = runCatching { app.githubProfileStore.load() }.getOrNull()
        val now = System.currentTimeMillis()
        if (reload == 0 && cached != null && isGitHubCacheFresh(cached.fetchedAt, now)) {
            snapshot = cached
            loading = false
            return@LaunchedEffect
        }
        val fetched = runCatching { fetchGitHub(app.githubApi) }
        if (fetched.isSuccess) {
            val fresh = fetched.getOrThrow().copy(fetchedAt = now)
            snapshot = fresh
            runCatching { app.githubProfileStore.save(fresh) }
        } else if (cached != null) {
            snapshot = cached
            error = "GitHub 暂时无法访问，显示的是上次缓存。"
        } else {
            error = "GitHub 暂时无法访问，贡献图稍后再试。"
        }
        loading = false
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "关于我",
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ProfileCard(onOpenUrl = onOpenUrl) }
            item {
                SectionTitle("项目")
            }
            items(AboutProjects.size) { index ->
                val project = AboutProjects[index]
                ProjectCard(
                    project = project,
                    stars = snapshot?.stars?.get(project.repo),
                    onClick = { onOpenUrl("$GITHUB_URL/${project.repo}") },
                )
            }
            item {
                SectionTitle("GitHub 贡献")
            }
            item {
                ContributionCard(
                    contributions = snapshot?.contributions,
                    fetchedAt = snapshot?.fetchedAt ?: 0L,
                    loading = loading,
                    error = error,
                    onRetry = { reload += 1 },
                    onOpenGitHub = { onOpenUrl(GITHUB_URL) },
                )
            }
            item {
                Text(
                    text = "贡献图来自 GitHub 公开页面，仅统计公开贡献；数据缓存 24 小时。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private suspend fun fetchGitHub(api: GitHubPublicApi): GitHubProfileSnapshot = coroutineScope {
    val contributions = async {
        runCatching { api.contributions(GITHUB_USER) }.getOrNull()
    }
    val stars = AboutProjects
        .filterNot { it.comingSoon }
        .map { project ->
            async {
                project.repo to runCatching { api.repoStars(GITHUB_USER, project.repo) }.getOrNull()
            }
        }
        .awaitAll()
        .mapNotNull { (repo, count) -> count?.let { repo to it } }
        .toMap()
    GitHubProfileSnapshot(
        contributions = contributions.await(),
        stars = stars,
    )
}

@Composable
private fun ProfileCard(onOpenUrl: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KungalImage(
                    url = AVATAR_URL,
                    contentDescription = "头像",
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    fallbackIcon = true,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "星鸿",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "@$GITHUB_USER",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "独立开发者，喜欢把想法做成能用的东西。持续维护个人博客与几个开源项目；" +
                    "这个 kungal 客户端也是从自用需求开始，一边对照网页一边把功能补齐。",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.5f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkPill("GitHub", onClick = { onOpenUrl(GITHUB_URL) })
                LinkPill("博客", onClick = { onOpenUrl(BLOG_URL) })
            }
        }
    }
}

@Composable
private fun LinkPill(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ProjectCard(project: AboutProject, stars: Int?, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Tag(if (project.comingSoon) "即将开源" else if (project.isFork) "Fork" else "Public")
                Spacer(Modifier.weight(1f))
                if (stars != null) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = "star",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = stars.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.45f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(languageColor(project.language)),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = project.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

private fun languageColor(language: String): Color = when (language) {
    "TypeScript" -> Color(0xFF3178C6)
    "Python" -> Color(0xFF3572A5)
    "Kotlin" -> Color(0xFFA97BFF)
    else -> Color(0xFF8B949E)
}

@Composable
private fun ContributionCard(
    contributions: GitHubContributions?,
    fetchedAt: Long,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        contributions != null -> "近一年 ${contributions.total} 次公开贡献"
                        loading -> "正在读取 GitHub 贡献…"
                        else -> "贡献图暂时不可用"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            when {
                contributions != null -> {
                    ContributionHeatmap(contributions)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "少",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        for (level in 0..4) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(contributionColor(level)),
                            )
                            Spacer(Modifier.width(2.dp))
                        }
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "多",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        if (fetchedAt > 0) {
                            Text(
                                text = "更新于 ${relativeTime(java.time.Instant.ofEpochMilli(fetchedAt).toString())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onRetry) { Text("刷新") }
                }
                else -> {
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        TextButton(onClick = onRetry) { Text("重试") }
                        TextButton(onClick = onOpenGitHub) { Text("在 GitHub 查看") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributionHeatmap(contributions: GitHubContributions) {
    if (contributions.weeks.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        contributions.weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0..6) {
                    val day = week.getOrNull(row)
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (day == null) Color.Transparent else contributionColor(day.level),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun contributionColor(level: Int): Color {
    val palette = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF21262D), Color(0xFF0E4429), Color(0xFF006D32), Color(0xFF26A641), Color(0xFF39D353))
    } else {
        listOf(Color(0xFFEBEDF0), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF30A14E), Color(0xFF216E39))
    }
    return palette[level.coerceIn(0, 4)]
}
