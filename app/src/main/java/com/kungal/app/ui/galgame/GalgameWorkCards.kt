package com.kungal.app.ui.galgame

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.R
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.core.model.WorkSummary

/**
 * 作品封面：无封面或图片加载失败时显示官网同款「暂无封面」插图
 * （站点 /galgame-no-cover.webp）；NSFW 且设置为「模糊」时打码。
 */
@Composable
fun WorkCoverImage(
    url: String?,
    isNsfw: Boolean,
    blurNsfw: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (isNsfw && blurNsfw && !url.isNullOrBlank()) {
        NsfwCoverImage(url = url, modifier = modifier)
    } else {
        KungalImage(
            url = url,
            contentDescription = contentDescription,
            modifier = modifier,
            fallback = { NoCoverArtwork() },
        )
    }
}

/** 站点「封面不足」占位图（官方 web 公共资源），按 contain 铺满、底色与网页一致。 */
@Composable
fun NoCoverArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.galgame_no_cover),
            contentDescription = "暂无封面",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/** 资料库网格卡片（两列）：封面 + 标题 + 会社 + 发售 + 评分 + 平台。 */
@Composable
fun GalgameWorkCard(
    work: WorkSummary,
    blurCover: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            WorkCoverImage(
                url = work.cover?.url,
                isNsfw = work.isNsfw,
                blurNsfw = blurCover,
                contentDescription = workTitle(work),
                modifier = Modifier.fillMaxWidth().aspectRatio(5f / 7f),
            )
            if (work.isNsfw) {
                Text(
                    text = "NSFW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = workTitle(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            companyName(work)?.let { company ->
                Text(
                    text = company,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = releaseText(work),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ratingText(work)?.let { score ->
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = score,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val platforms = platformShortLabels(work)
            if (platforms.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    platforms.take(2).forEach { Chip(it) }
                }
            }
        }
    }
}

/** 实体页 / 收藏夹里的作品行：左封面右资料，单列滚动。 */
@Composable
fun EntityWorkRow(
    work: WorkSummary,
    blurCover: Boolean,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCoverImage(
            url = work.cover?.url,
            isNsfw = work.isNsfw,
            blurNsfw = blurCover,
            contentDescription = workTitle(work),
            modifier = Modifier
                .width(58.dp)
                .aspectRatio(5f / 7f)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = workTitle(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            companyName(work)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    releaseText(work),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ratingText(work)?.let { score ->
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        score,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (work.isNsfw) {
                    Spacer(Modifier.width(6.dp))
                    Chip("NSFW")
                }
            }
            val platforms = platformShortLabels(work)
            if (platforms.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    platforms.take(3).forEach { Chip(it) }
                }
            }
        }
        trailing?.invoke()
    }
}
