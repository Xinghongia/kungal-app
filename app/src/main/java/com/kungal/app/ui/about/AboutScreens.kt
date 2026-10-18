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
import com.kungal.app.isGitHubCacheUsable
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
)

private val AboutProjects = listOf(
    AboutProject(
        repo = "kungal-app",
        name = "kungal-app",
        description = "鲲 Galgame 论坛的第三方安卓客户端（Kotlin + Jetpack Compose），功能对照网页逐项实现。",
        language = "Kotlin",
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
        title = "免责声明",
        intro = "本免责声明（下称“本声明”）适用于 kungal 安卓客户端（下称“本应用”）。" +
            "您在使用本应用前，应当审慎阅读并充分理解本声明的全部内容。您下载、安装、启动或使用本应用，" +
            "即视为您已阅读、理解并同意接受本声明的全部约定。如您不同意本声明的任何内容，请立即停止使用并卸载本应用。",
        sections = listOf(
            "第一条 非官方声明" to
                "1.1 本应用系由个人开发者独立开发并维护的第三方客户端，并非 kungal.com（鲲 Galgame 论坛，" +
                "下称“本网站”）的官方产品，与本网站运营方之间不存在隶属、委托、代理、合作或任何其他形式的关联关系。\n" +
                "1.2 本应用所展示的名称、站标、内容及相关标识，其著作权、商标权及其他相关权利均归本网站及相应权利人所有。" +
                "本应用对上述标识的使用仅用于指代服务来源，不构成对任何权利的取得或主张。\n" +
                "1.3 本应用不代表本网站的立场或观点，本网站亦不对本应用的功能、内容或服务质量承担任何责任。",
            "第二条 数据来源与权限" to
                "2.1 本应用所展示的全部内容均来源于本网站公开提供的接口，未对本网站数据进行任何形式的复制、缓存镜像或再分发。\n" +
                "2.2 本应用严格遵循本网站既有的账号权限、内容分级及剧透提示机制，不实施任何绕过上述机制的技术手段，" +
                "亦不进行全站批量抓取、批量下载或离线镜像。\n" +
                "2.3 本应用内的信息展示与操作权限与本网站网页端保持一致；因账号状态、权限变更或内容调整导致的展示差异，" +
                "以本网站服务器返回结果为准。",
            "第三条 账号与会话" to
                "3.1 本应用的账号体系完全依托本网站的官方账号服务，本应用不存储您的账号密码，亦不参与账号的注册、认证与处罚。\n" +
                "3.2 登录凭据仅保存于本机应用私有目录，您退出登录时即予清除。账号本身的封禁、限制及其他处理措施，" +
                "均由本网站依据其用户协议独立作出，与本应用无关。",
            "第四条 内容与责任限制" to
                "4.1 用户通过本应用发布的话题、回复、评论、投稿及其他内容，由发布者依法承担相应责任，" +
                "本应用仅提供信息传输通道，不对第三方内容的真实性、准确性、完整性或合法性作出任何保证。\n" +
                "4.2 对于因使用或无法使用本应用而产生的任何直接或间接损失，包括但不限于数据丢失、账号受限、" +
                "设备故障或商业利益损失，在法律允许的最大范围内，本应用开发者不承担赔偿责任。\n" +
                "4.3 本应用可能包含指向第三方网站的链接，该等网站的内容及隐私实践由第三方独立负责，" +
                "本应用不对其作出任何担保。",
            "第五条 服务可用性" to
                "5.1 本应用按“现状”及“现有”基础提供，开发者不对其适用性、可靠性、准确性或连续性作出任何明示或默示的保证。\n" +
                "5.2 因本网站接口调整、页面结构变更、网络故障、设备兼容性或不可抗力导致的功能中断或数据异常，" +
                "开发者不承担由此产生的责任，但将尽合理努力予以修复。\n" +
                "5.3 开发者有权在不另行通知的情况下，对本应用的功能、版本及本声明内容进行修改、暂停或终止。",
            "第六条 知识产权" to
                "6.1 本应用的源代码采用项目仓库所载明的开源许可证发布，具体条款以仓库中的许可证文件为准。\n" +
                "6.2 未经相关权利人书面许可，任何人不得将本应用用于商业用途，或以本应用名义从事任何违法或侵权活动。",
            "第七条 声明更新与争议解决" to
                "7.1 本声明可能随本应用版本更新而修订，修订后的声明自发布之日起生效。\n" +
                "7.2 因本声明引起的或与之相关的争议，双方应友好协商解决；协商不成的，依照中华人民共和国法律处理。",
            "第八条 联系方式" to
                "如您对本声明存有疑问、意见或投诉，可通过项目仓库提交 Issue：" +
                "github.com/$GITHUB_USER/kungal-app。",
        ),
        onBack = onBack,
    )
}

/** 隐私协议（我的 → 关于），内容按应用实际行为编写。 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalPage(
        title = "隐私政策",
        intro = "本隐私政策（下称“本政策”）旨在说明 kungal 安卓客户端（下称“本应用”）如何收集、使用、存储和保护您的信息。" +
            "我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。请您在使用本应用前，仔细阅读并理解本政策的全部内容。",
        sections = listOf(
            "一、我们如何收集和使用您的信息" to
                "1.1 本应用的账号信息依托本网站官方账号服务完成，登录凭据（会话 cookie）由您主动登录后生成并保存于本机，" +
                "我们不会收集您的账号密码。\n" +
                "1.2 为向您提供连续一致的使用体验，本应用会在本机保存以下信息：\n" +
                "（1）登录会话信息：用于保持登录状态，存储于应用私有目录；\n" +
                "（2）偏好设置信息：包括主题模式、动态取色、内容分级、默认排序等；\n" +
                "（3）搜索历史信息：最多保存 20 条，可单条删除或一键清空；\n" +
                "（4）工具集上传进度信息：用于断点续传；\n" +
                "（5）GitHub 公开资料缓存：贡献日历与仓库 star 数，保留 24 小时。\n" +
                "上述信息均存储于本机，不会上传至开发者服务器。",
            "二、我们如何共享、转让和公开披露您的信息" to
                "2.1 我们不会向任何第三方出售、出租或以其他方式提供您的个人信息，法律法规另有规定的除外。\n" +
                "2.2 为实现应用功能，本应用需向下列域名发起网络请求：\n" +
                "（1）www.kungal.com：本网站官方接口；\n" +
                "（2）image.kungal.iloveren.link：图片内容分发服务；\n" +
                "（3）sticker.kungal.com：默认头像资源池；\n" +
                "（4）account.nextmoe.com：账号中心，仅在您选择网页登录时由浏览器访问；\n" +
                "（5）github.com 及 avatars.githubusercontent.com：仅在您访问“关于我”页面时，读取公开贡献记录、仓库 star 数与公开头像。\n" +
                "2.3 当您通过本应用打开第三方链接时，相应网站将依据其自身的隐私政策处理您的信息，本应用不对该等处理行为负责。",
            "三、我们如何保护您的信息" to
                "3.1 本应用采取符合业界标准的安全防护措施保护您提供的个人信息，防止数据遭到未经授权的访问、公开披露、使用、修改、损坏或丢失。\n" +
                "3.2 您的登录凭据存储于本机应用私有目录，其他应用无法直接访问；当前版本采用明文存储，" +
                "我们将在后续版本中评估并升级为加密存储。您退出登录或卸载本应用后，相关数据即被清除。\n" +
                "3.3 如发生个人信息安全事件，我们将按照法律法规的要求，及时向您告知相关情况。",
            "四、我们不会收集的信息" to
                "4.1 本应用不集成统计分析、广告推送或崩溃上报类 SDK。\n" +
                "4.2 本应用不会读取您的通讯录、位置、短信、通话记录等敏感权限。\n" +
                "4.3 本应用不会采集设备标识符用于用户追踪或画像分析。",
            "五、您的权利" to
                "5.1 访问与更正：您可在“我的”页面查看账号资料，并在设置页管理外观与浏览偏好。\n" +
                "5.2 删除：您可在搜索页删除单条或全部搜索历史；退出登录将清除本机会话信息；卸载本应用将清除全部本地数据。\n" +
                "5.3 撤回同意：您可随时停止使用本应用，并通过卸载方式撤回此前对本政策的同意。",
            "六、未成年人保护" to
                "6.1 本应用主要面向成年用户。若您为未成年人，请在监护人的陪同和指导下阅读本政策并使用本应用。\n" +
                "6.2 本应用会依照本网站的内容分级机制展示内容，未成年人使用时应遵守相关法律法规及本网站的用户协议。",
            "七、本政策的变更" to
                "7.1 我们可能适时修订本政策的内容。涉及个人信息处理方式的重大变更，我们将通过应用更新日志等合理方式向您告知。\n" +
                "7.2 若您在本政策修订后继续使用本应用，即视为您已充分阅读、理解并同意修订后的政策。",
            "八、联系我们" to
                "如您对本政策或个人信息保护相关事宜存有疑问、意见或建议，可通过“我的 → 关于我”页面中的 GitHub 链接与我们取得联系。",
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
                    text = "生效日期：2026-10-18 · 最近更新：0.8.16",
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
        // 缓存要同时满足两点才直接用：没过期，且覆盖当前全部项目与贡献图。
        // 项目列表或仓库可见性变化后，旧缓存会缺项，必须重新抓一次。
        val cacheUsable = isGitHubCacheUsable(cached, now, AboutProjects.map { it.repo })
        if (reload == 0 && cacheUsable && cached != null) {
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
                Tag(if (project.isFork) "Fork" else "Public")
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
