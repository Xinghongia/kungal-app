package com.kungal.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kungal.core.model.Sticker
import com.kungal.core.model.StickerPack
import com.kungal.core.model.StickerPackList
import com.kungal.core.model.stickerContentUrl

/**
 * 官方表情包选择器（编辑器与私信共用）。
 * [load] 由调用方提供（App 级缓存），只在打开时请求一次。
 */
@Composable
fun StickerPickerDialog(
    load: suspend () -> StickerPackList,
    onPick: (Sticker) -> Unit,
    onDismiss: () -> Unit,
) {
    var packs by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPack by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching { load() }
            .onSuccess {
                packs = it.items
                loading = false
            }
            .onFailure {
                error = it.message ?: "表情包加载失败"
                loading = false
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("表情包", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭")
                    }
                }
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp) }

                    error != null -> Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )

                    packs.isEmpty() -> Text(
                        "暂时没有可用的表情包",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )

                    else -> {
                        val pack = packs.getOrNull(selectedPack) ?: packs.first()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(packs.size) { index ->
                                FilterChip(
                                    selected = index == selectedPack,
                                    onClick = { selectedPack = index },
                                    label = {
                                        Text(
                                            packs[index].displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(pack.stickers, key = { it.id }) { sticker ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable { onPick(sticker) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val image = sticker.image
                                    if (image != null) {
                                        KungalImage(
                                            url = stickerContentUrl(image),
                                            contentDescription = sticker.displayName,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}
