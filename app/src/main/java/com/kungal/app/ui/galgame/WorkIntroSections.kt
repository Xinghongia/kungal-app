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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.core.model.CatalogIntro
import com.kungal.core.model.Work
import com.kungal.core.model.WorkCharacter
import com.kungal.core.model.WorkCreditGroup

private val TAG_KINDS = listOf(
    "content" to "内容",
    "meta" to "属性",
    "sexual" to "成人内容",
)

private val TAG_SPOILERS = listOf(
    "none" to "无剧透",
    "minor" to "轻度剧透",
    "major" to "严重剧透",
)

/** 词条「游戏介绍」标签页的全部区块，按网页顺序排列。 */
fun LazyListScope.workIntroItems(
    work: Work,
    blurMedia: Boolean,
    allowNsfw: Boolean,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenImage: (String) -> Unit,
) {
    item(key = "intro") { IntroSection(work.intros) }
    item(key = "links") { LinksSection(work, onOpenWeb) }
    item(key = "info") { InfoSection(work, onOpenWeb) }
    if (work.tags.isNotEmpty()) {
        item(key = "tags") { TagsSection(work.tags, allowNsfw, onOpenWeb) }
    }
    if (work.credits.isNotEmpty()) {
        item(key = "staff") { StaffSection(work.credits, onOpenWeb) }
    }
    if (work.roster.isNotEmpty()) {
        item(key = "roster") {
            RosterSection(
                roster = work.roster,
                workIsNsfw = work.isNsfw,
                blurMedia = blurMedia,
                allowNsfw = allowNsfw,
                onOpenWeb = onOpenWeb,
                onOpenImage = onOpenImage,
            )
        }
    }
    if (work.screenshots.isNotEmpty()) {
        item(key = "gallery") {
            GallerySection(
                work = work,
                blurMedia = blurMedia,
                allowNsfw = allowNsfw,
                onOpenImage = onOpenImage,
            )
        }
    }
    if (work.series.isNotEmpty()) {
        item(key = "series") { SeriesSection(work, onOpenWeb) }
    }
    if (work.relatedWorks.isNotEmpty()) {
        item(key = "related") { RelatedSection(work, onOpenWork) }
    }
    if (work.creator != null || work.contributors.isNotEmpty()) {
        item(key = "contributors") { ContributorsSection(work, onOpenUser) }
    }
    if (work.externalRatings.isNotEmpty()) {
        item(key = "external-ratings") { ExternalRatingsSection(work) }
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun IntroSection(intros: List<CatalogIntro>) {
    var locale by rememberSaveable(intros) {
        mutableStateOf(pickIntro(intros)?.locale.orEmpty())
    }
    val current = intros.firstOrNull { it.locale == locale } ?: pickIntro(intros)
    SectionCard(title = "游戏介绍") {
        if (intros.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                intros.forEach { intro ->
                    FilterChip(
                        selected = current?.locale == intro.locale,
                        onClick = { locale = intro.locale },
                        label = { Text(introLanguageLabel(intro.locale)) },
                    )
                }
            }
        }
        if (current == null) {
            Text(
                "暂无简介，欢迎贡献",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val source = introSourceLabel(current.dataSource)
            if (source != null || current.isMachine) {
                Text(
                    text = buildString {
                        if (source != null) append("来源: $source")
                        if (current.isMachine) {
                            if (isNotEmpty()) append(" · ")
                            append("本段简介由机器翻译生成，与原文可能有出入")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = markdownToPlainText(current.value),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinksSection(work: Work, onOpenWeb: (String) -> Unit) {
    val links = identityLinks(work.externalRefs) +
        work.links.map { ExternalLink(catalogLinkLabel(it.site, it.url), it.url) }
    if (links.isEmpty()) return
    SectionCard(title = "相关链接") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            links.forEach { link ->
                TextButton(
                    onClick = { onOpenWeb(link.url) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                ) {
                    Text(link.label, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(work: Work, onOpenWeb: (String) -> Unit) {
    SectionCard(title = "作品信息") {
        if (work.companies.isNotEmpty()) {
            InfoRow("会社") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    work.companies.forEach { company ->
                        AssistChip(
                            onClick = { onOpenWeb("/galgame/official/${company.id}") },
                            label = {
                                Text(
                                    buildString {
                                        append(company.localized["zh-Hans"]?.value ?: company.displayName)
                                        if (company.attributionRoles.isNotEmpty()) {
                                            append(" · ")
                                            append(company.attributionRoles.joinToString("/") {
                                                attributionRoleLabel(it)
                                            })
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
        if (work.series.isNotEmpty()) {
            InfoRow("所属系列") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    work.series.forEach { series ->
                        AssistChip(
                            onClick = { onOpenWeb("/galgame/series/${series.id}") },
                            label = {
                                Text(
                                    series.localized["zh-Hans"]?.value ?: series.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
        if (work.engines.isNotEmpty()) {
            InfoRow("游戏引擎") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    work.engines.forEach { engine ->
                        AssistChip(
                            onClick = { onOpenWeb("/galgame/engine/${engine.id}") },
                            label = {
                                Text(
                                    engine.localized["zh-Hans"]?.value ?: engine.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
        InfoRow("原语言") { Chip(originalLanguageLabel(work.originalLanguage)) }
        InfoRow("发售日期") {
            Text(
                releaseDateText(work.releaseDate, work.releaseDatePrecision),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        InfoRow("年龄限制") { Chip(contentRatingLabel(work.contentRating)) }
        if (work.playtimes.isNotEmpty()) {
            InfoRow("游玩时长") {
                Column {
                    work.playtimes.forEach { row ->
                        Text(
                            "${row.site}: ${formatMinutes(row.minutes)}（${row.voteCount} 人记录）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp).padding(top = 6.dp),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<com.kungal.core.model.WorkTag>,
    allowNsfw: Boolean,
    onOpenWeb: (String) -> Unit,
) {
    var kinds by rememberSaveable { mutableStateOf(setOf("content", "meta")) }
    var spoilers by rememberSaveable { mutableStateOf(setOf("none")) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val filtered = tags.filter { tag ->
        val kind = if (tag.isSexual) "sexual" else tag.tagKind
        kind in kinds && tag.spoiler in spoilers
    }.sortedBy { it.id.toLongOrNull() ?: 0L }
    val visible = if (expanded) filtered else filtered.take(40)

    SectionCard(
        title = "标签",
        subtitle = "默认只显示无剧透标签；剧透标签由你决定是否展开",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TAG_KINDS.forEach { (value, label) ->
                if (value == "sexual" && !allowNsfw) return@forEach
                FilterChip(
                    selected = value in kinds,
                    onClick = {
                        kinds = if (value in kinds) kinds - value else kinds + value
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TAG_SPOILERS.forEach { (value, label) ->
                FilterChip(
                    selected = value in spoilers,
                    onClick = {
                        spoilers = if (value in spoilers) spoilers - value else spoilers + value
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        if (filtered.isEmpty()) {
            Text(
                "没有符合筛选条件的标签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                visible.forEach { tag ->
                    AssistChip(
                        onClick = { onOpenWeb("/galgame/tag/${tag.id}") },
                        label = {
                            Text(
                                buildString {
                                    append(tag.localized["zh-Hans"]?.value ?: tag.displayName)
                                    if (tag.spoiler != "none") append(" · 剧透")
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
            if (filtered.size > visible.size) {
                TextButton(onClick = { expanded = true }) {
                    Text("显示其余 ${filtered.size - visible.size} 个标签")
                }
            }
        }
    }
}

@Composable
private fun StaffSection(credits: List<WorkCreditGroup>, onOpenWeb: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) credits else credits.take(6)
    SectionCard(
        title = "制作人员",
        subtitle = "剧本、原画、音乐、声优等署名，来自 NextMoe 目录的署名图谱",
    ) {
        visible.forEach { group ->
            Row {
                Text(
                    group.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp).padding(top = 4.dp),
                )
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.people.forEach { person ->
                        TextButton(
                            onClick = { onOpenWeb("/galgame/staff/${person.id}") },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                buildString {
                                    append(person.localized["zh-Hans"]?.value ?: person.displayName)
                                    if (person.voicedCharacters.isNotEmpty()) {
                                        append("（")
                                        append(person.voicedCharacters.joinToString(" / "))
                                        append("）")
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        if (credits.size > 6) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起制作人员" else "展开其余 ${credits.size - 6} 项职位")
            }
        }
    }
}

@Composable
private fun RosterSection(
    roster: List<WorkCharacter>,
    workIsNsfw: Boolean,
    blurMedia: Boolean,
    allowNsfw: Boolean,
    onOpenWeb: (String) -> Unit,
    onOpenImage: (String) -> Unit,
) {
    SectionCard(title = "出演角色", subtitle = "主要角色优先；点击角色查看详情") {
        roster.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { character ->
                    CharacterCard(
                        character = character,
                        workIsNsfw = workIsNsfw,
                        blurMedia = blurMedia,
                        allowNsfw = allowNsfw,
                        onOpenWeb = onOpenWeb,
                        onOpenImage = onOpenImage,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: WorkCharacter,
    workIsNsfw: Boolean,
    blurMedia: Boolean,
    allowNsfw: Boolean,
    onOpenWeb: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = character.image
    val explicit = image?.sexual == "explicit"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onOpenWeb("/galgame/character/${character.id}") }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (image == null) {
                NoCoverArtwork(Modifier.fillMaxSize())
            } else if (!allowNsfw && explicit) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "成人向图片已隐藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (blurMedia && (workIsNsfw || explicit)) {
                NsfwCoverImage(
                    url = image.url,
                    modifier = Modifier.fillMaxSize().clickable { onOpenImage(image.url) },
                )
            } else {
                CharacterImage(
                    url = image.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { onOpenImage(image.url) },
                )
            }
        }
        Text(
            text = character.localized["zh-Hans"]?.value ?: character.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(characterKindLabel(character.characterKind))
            if (character.spoiler != "none") {
                Spacer(Modifier.width(4.dp))
                Chip(spoilerLabel(character.spoiler))
            }
        }
        if (character.voices.isNotEmpty()) {
            Text(
                text = "CV: " + character.voices.joinToString(" / ") {
                    it.localized["zh-Hans"]?.value ?: it.displayName
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GallerySection(
    work: Work,
    blurMedia: Boolean,
    allowNsfw: Boolean,
    onOpenImage: (String) -> Unit,
) {
    val shots = work.screenshots.filter { shot -> shot.image != null }
    val visible = shots.filter { allowNsfw || it.image?.sexual != "explicit" }
    val hidden = shots.size - visible.size
    SectionCard(
        title = "画廊",
        subtitle = if (hidden > 0) "有 $hidden 张成人向图片已按内容设置隐藏" else null,
    ) {
        visible.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { shot ->
                    val image = shot.image ?: return@forEach
                    val url = image.url
                    Column(
                        modifier = Modifier.weight(1f).clickable { onOpenImage(url) },
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val explicit = image.sexual == "explicit"
                        if (blurMedia && (work.isNsfw || explicit)) {
                            NsfwCoverImage(
                                url = url,
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                            )
                        } else {
                            KungalImage(
                                url = url,
                                contentDescription = shot.caption.ifBlank { null },
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        if (shot.caption.isNotBlank()) {
                            Text(
                                shot.caption,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SeriesSection(work: Work, onOpenWeb: (String) -> Unit) {
    SectionCard(title = "所属系列") {
        work.series.forEach { series ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenWeb("/galgame/series/${series.id}") }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    series.localized["zh-Hans"]?.value ?: series.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "本站收录 ${series.listedWorkCount} 部 · 资料库共 ${series.catalogWorkCount} 部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (series.sampleWorks.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        series.sampleWorks.take(4).forEach { sample ->
                            KungalImage(
                                url = sample.cover?.url ?: sample.banner?.url,
                                contentDescription = sample.displayName,
                                modifier = Modifier
                                    .width(48.dp)
                                    .aspectRatio(5f / 7f)
                                    .clip(RoundedCornerShape(4.dp)),
                                fallback = { NoCoverArtwork() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedSection(
    work: Work,
    onOpenWork: (String) -> Unit,
) {
    SectionCard(title = "关联作品") {
        work.relatedWorks.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { related ->
                    val isGalgame = related.medium == "galgame"
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isGalgame) Modifier.clickable { onOpenWork(related.id) }
                                else Modifier,
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        KungalImage(
                            url = related.cover?.url,
                            contentDescription = related.displayName,
                            modifier = Modifier
                                .width(44.dp)
                                .aspectRatio(5f / 7f)
                                .clip(RoundedCornerShape(4.dp)),
                            fallback = { NoCoverArtwork() },
                        )
                        Column {
                            Text(
                                related.localized["zh-Hans"]?.value ?: related.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                related.relations.firstOrNull()?.phrase ?: related.medium,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContributorsSection(work: Work, onOpenUser: (String) -> Unit) {
    SectionCard(
        title = "贡献者",
        subtitle = "本游戏项目的资料贡献者（不计资源发布）",
    ) {
        val creator = work.creator
        if (creator != null) {
            ContributorRow(creator.name ?: creator.id, creator.avatar?.url, "创建本游戏", onOpenUser, creator.id)
        }
        work.contributors.forEach { user ->
            ContributorRow(user.name ?: user.id, user.avatar?.url, "资料贡献", onOpenUser, user.id)
        }
    }
}

@Composable
private fun ContributorRow(
    name: String,
    avatar: String?,
    role: String,
    onOpenUser: (String) -> Unit,
    userId: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenUser(userId) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(url = avatar, seed = name, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.weight(1f))
        Text(
            role,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExternalRatingsSection(work: Work) {
    SectionCard(
        title = "外部评分",
        subtitle = "来自 VNDB / Bangumi / 批评空间 / DLsite 的评分，各来源刻度不同，不跨来源混算",
    ) {
        work.externalRatings.forEach { rating ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    externalRatingLabel(rating.site),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(78.dp),
                )
                Text(
                    String.format(java.util.Locale.US, "%.2f", rating.ratingValue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${rating.voteCount} 人评分",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rating.sourceRank != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "第 ${rating.sourceRank} 名",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun releaseDateText(date: String?, precision: String?): String = when {
    date.isNullOrBlank() -> "未定 (TBA)"
    precision == "year" -> date.take(4)
    precision == "month" -> date.take(7)
    else -> date
}
