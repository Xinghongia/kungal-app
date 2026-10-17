package com.kungal.app.ui.content

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.kungal.app.ui.util.AndroidActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * 全屏图片查看器：双指缩放/拖动，支持保存到本地、复制链接、分享。
 * 图片加载直接用 URL 读取（内容寻址 CDN 不需要会话）。
 */
@Composable
fun FullScreenImageViewer(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale <= 1f) {
                                Offset.Zero
                            } else {
                                offset + pan
                            }
                        }
                    },
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("图片加载失败", color = Color.White)
                    }
                },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Color.White)
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.Black.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                val result = runCatching { saveImage(context, url) }
                                saving = false
                                Toast.makeText(
                                    context,
                                    result.getOrElse { "保存失败：${it.message}" },
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                    ) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Download, contentDescription = "保存到本地", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = { AndroidActions.copyText(context, "图片链接", url) },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "复制链接", tint = Color.White)
                    }
                    IconButton(
                        onClick = { AndroidActions.shareText(context, url) },
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "分享", tint = Color.White)
                    }
                }
            }
        }
    }
}

/** 保存到相册；Android 10+ 走 MediaStore，旧版本保存到应用图片目录。返回提示文本。 */
private suspend fun saveImage(context: android.content.Context, url: String): String {
    val bytes = withContext(Dispatchers.IO) {
        URL(url).openStream().use { it.readBytes() }
    }
    val name = "kungal_${System.currentTimeMillis()}.${extensionOf(url)}"
    val mime = when (extensionOf(url)) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        else -> "image/webp"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/kungal")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return "保存失败：无法创建图片"
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
        return "已保存到相册（Pictures/kungal）"
    }
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "kungal")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, name)
    withContext(Dispatchers.IO) { file.writeBytes(bytes) }
    return "已保存到 ${file.absolutePath}"
}

private fun extensionOf(url: String): String {
    val clean = url.substringBefore('?').substringBefore('#')
    val ext = clean.substringAfterLast('.', "").lowercase()
    return if (ext in setOf("png", "jpg", "jpeg", "gif", "webp")) ext else "webp"
}
