package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.core.model.Work
import com.kungal.core.model.WorkCover

private val COVER_SLOT_ORDER = listOf(
    "main", "pkgfront", "dig", "pkgback", "pkgcontent", "pkgside", "pkgmed", "other",
)

/**
 * 全部封面（封面投票）：按封面类型分组；一人一票，投票/撤票立即生效。
 * 与网页一致：内容设置不允许 NSFW 时隐藏 explicit 封面，模糊模式打码。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverGallerySheet(
    work: Work,
    coverCounts: Map<String, Int>,
    votedCoverId: String?,
    votingCoverId: String?,
    allowNsfw: Boolean,
    blurMedia: Boolean,
    onVote: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val withImage = work.covers.filter { it.image != null }
    val visible = withImage.filter { allowNsfw || it.image?.sexual != "explicit" }
    val hiddenCount = withImage.size - visible.size
    val groups = COVER_SLOT_ORDER.mapNotNull { slot ->
        val covers = visible.filter { it.coverSlot == slot }
        if (covers.isEmpty()) null else slot to covers
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("所有封面", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭")
                }
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                if (groups.isEmpty()) {
                    item {
                        Text(
                            "该 Galgame 暂无可用封面",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                groups.forEach { (slot, covers) ->
                    item(key = "slot-$slot") {
                        Text(
                            coverSlotLabel(slot),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                        )
                    }
                    items(covers, key = { it.id }) { cover ->
                        CoverRow(
                            cover = cover,
                            voteCount = coverCounts[cover.id] ?: cover.voteCount,
                            voted = votedCoverId == cover.id,
                            voting = votingCoverId == cover.id,
                            workIsNsfw = work.isNsfw,
                            blurMedia = blurMedia,
                            onVote = { onVote(cover.id) },
                            onOpenImage = onOpenImage,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                if (hiddenCount > 0) {
                    item {
                        Text(
                            "有 $hiddenCount 张成人向封面已按你的内容设置隐藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
                item { Spacer(Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun CoverRow(
    cover: WorkCover,
    voteCount: Int,
    voted: Boolean,
    voting: Boolean,
    workIsNsfw: Boolean,
    blurMedia: Boolean,
    onVote: () -> Unit,
    onOpenImage: (String) -> Unit,
) {
    val image = cover.image ?: return
    val explicit = image.sexual == "explicit"
    val blur = blurMedia && (workIsNsfw || explicit)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.width(104.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (blur) {
                NsfwCoverImage(
                    url = image.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(coverAspect(image.width, image.height))
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                KungalImage(
                    url = image.url,
                    contentDescription = coverSlotLabel(cover.coverSlot),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(coverAspect(image.width, image.height))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenImage(image.url) },
                )
            }
            Text(
                text = "查看大图",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onOpenImage(image.url) }
                    .padding(vertical = 2.dp),
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                coverSlotLabel(cover.coverSlot),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (cover.site.isNotBlank()) {
                Text(
                    "来源: ${cover.site}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onVote,
                enabled = !voting,
            ) {
                Icon(
                    if (voted) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (voted) "已选为最佳封面" else "选为最佳封面",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    voteCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun coverAspect(width: Int?, height: Int?): Float {
    if (width == null || height == null || width <= 0 || height <= 0) return 3f / 4f
    return width.toFloat() / height.toFloat()
}
