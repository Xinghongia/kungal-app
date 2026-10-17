package com.kungal.app.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kungal.core.model.BlockquoteNode
import com.kungal.core.model.BreakNode
import com.kungal.core.model.CodeNode
import com.kungal.core.model.ContentDocument
import com.kungal.core.model.ContentNode
import com.kungal.core.model.EmphasisNode
import com.kungal.core.model.HeadingNode
import com.kungal.core.model.ImageNode
import com.kungal.core.model.InlineCodeNode
import com.kungal.core.model.InlineMathNode
import com.kungal.core.model.InlineSpoilerNode
import com.kungal.core.model.LinkNode
import com.kungal.core.model.ListItemNode
import com.kungal.core.model.ListNode
import com.kungal.core.model.MathNode
import com.kungal.core.model.MentionNode
import com.kungal.core.model.ParagraphNode
import com.kungal.core.model.ReplyReferenceNode
import com.kungal.core.model.SpoilerNode
import com.kungal.core.model.StrikethroughNode
import com.kungal.core.model.StrongNode
import com.kungal.core.model.TableCellNode
import com.kungal.core.model.TableNode
import com.kungal.core.model.TableRowNode
import com.kungal.core.model.TextNode
import com.kungal.core.model.ThematicBreakNode
import com.kungal.core.model.VideoNode

/**
 * 正文文档树 → Compose。覆盖站点 blocks.go 定义的全部节点类型；
 * 未知节点（UnknownNode）跳过不渲染。图片点击打开全屏查看器。
 */
@Composable
fun ContentDocumentView(document: ContentDocument?, modifier: Modifier = Modifier) {
    val revealed = remember { mutableStateSetOf<String>() }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    CompositionLocalProvider(LocalImageClicker provides { url -> viewerUrl = url }) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            document?.children?.forEachIndexed { index, node ->
                BlockView(node = node, key = "n$index", revealed = revealed)
            }
        }
    }
    viewerUrl?.let { url ->
        FullScreenImageViewer(url = url, onDismiss = { viewerUrl = null })
    }
}

/** 正文图片点击回调；ContentDocumentView 内部提供。 */
private val LocalImageClicker = compositionLocalOf<(String) -> Unit> { {} }

/** 正文纯文本（举报快照等用途）。 */
fun contentPlainText(document: ContentDocument?): String {
    val builder = StringBuilder()
    fun walk(nodes: List<ContentNode>) {
        for (node in nodes) {
            when (node) {
                is TextNode -> builder.append(node.value)
                is BreakNode -> builder.append('\n')
                is InlineCodeNode -> builder.append(node.value)
                is InlineMathNode -> builder.append(node.value)
                is CodeNode -> builder.append(node.value)
                is EmphasisNode -> walk(node.children)
                is StrongNode -> walk(node.children)
                is StrikethroughNode -> walk(node.children)
                is LinkNode -> walk(node.children)
                is InlineSpoilerNode -> walk(node.children)
                is SpoilerNode -> walk(node.children)
                is ParagraphNode -> walk(node.children)
                is HeadingNode -> walk(node.children)
                is BlockquoteNode -> walk(node.children)
                is ListNode -> walk(node.children)
                is ListItemNode -> walk(node.children)
                is TableNode -> walk(node.children)
                is TableRowNode -> walk(node.children)
                is TableCellNode -> walk(node.children)
                else -> Unit
            }
        }
    }
    walk(document?.children.orEmpty())
    return builder.toString().trim().take(2000)
}

@Composable
private fun BlockView(node: ContentNode, key: String, revealed: MutableSet<String>) {
    when (node) {
        is ParagraphNode -> ParagraphView(node.children, key, revealed)
        is HeadingNode -> HeadingView(node, key)
        is ThematicBreakNode -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        is BlockquoteNode -> BlockquoteView(node, key, revealed)
        is ListNode -> ListView(node, key, revealed)
        is CodeNode -> CodeView(node.value)
        is MathNode -> MathView(node.value)
        is TableNode -> TableView(node)
        is SpoilerNode -> SpoilerBlockView(node, key, revealed)
        is VideoNode -> VideoLink(node.url)
        else -> Unit
    }
}

// ---------- 段落与行内 ----------

private sealed interface ParagraphPart
private class TextRun(val nodes: List<ContentNode>) : ParagraphPart
private class ImagePart(val node: ImageNode) : ParagraphPart
private class VideoPart(val node: VideoNode) : ParagraphPart

private fun groupParts(children: List<ContentNode>): List<ParagraphPart> {
    val parts = mutableListOf<ParagraphPart>()
    val run = mutableListOf<ContentNode>()
    fun flush() {
        if (run.isNotEmpty()) {
            parts += TextRun(run.toList())
            run.clear()
        }
    }
    for (child in children) {
        when (child) {
            is ImageNode -> {
                flush()
                parts += ImagePart(child)
            }
            is VideoNode -> {
                flush()
                parts += VideoPart(child)
            }
            else -> run += child
        }
    }
    flush()
    return parts
}

@Composable
private fun ParagraphView(children: List<ContentNode>, key: String, revealed: MutableSet<String>) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val spoilerColor = MaterialTheme.colorScheme.onSurface
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val parts = remember(children) { groupParts(children) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is TextRun -> {
                    if (part.nodes.isEmpty()) return@forEachIndexed
                    val text = buildAnnotatedString {
                        appendInlines(part.nodes, "$key.$index", revealed, linkColor, codeBg, spoilerColor)
                    }
                    Text(text = text, style = bodyStyle, lineHeight = bodyStyle.fontSize * 1.6f)
                }
                is ImagePart -> ImageBlock(part.node)
                is VideoPart -> VideoLink(part.node.url)
            }
        }
    }
}

@Composable
private fun HeadingView(node: HeadingNode, key: String) {
    val style = when (node.depth) {
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val spoilerColor = MaterialTheme.colorScheme.onSurface
    val revealed = remember { mutableStateSetOf<String>() }
    Text(
        text = buildAnnotatedString {
            appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
        },
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun AnnotatedString.Builder.appendInlines(
    nodes: List<ContentNode>,
    keyPrefix: String,
    revealed: MutableSet<String>,
    linkColor: Color,
    codeBg: Color,
    spoilerColor: Color,
) {
    nodes.forEachIndexed { index, node ->
        val key = "$keyPrefix.$index"
        when (node) {
            is TextNode -> append(node.value)
            is EmphasisNode -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
            }
            is StrongNode -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
            }
            is StrikethroughNode -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
            }
            is InlineCodeNode -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                append(node.value)
            }
            is InlineMathNode -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic),
            ) { append(node.value) }
            is BreakNode -> append("\n")
            is LinkNode -> withLink(
                LinkAnnotation.Url(
                    url = node.url,
                    styles = TextLinkStyles(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    ),
                ),
            ) {
                appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
            }
            is MentionNode -> withStyle(SpanStyle(color = linkColor)) {
                append("@${node.mentionedUser.name ?: node.mentionedUser.id}")
            }
            is ReplyReferenceNode -> withStyle(SpanStyle(color = linkColor)) {
                append("#${node.floor}")
            }
            is InlineSpoilerNode -> {
                if (revealed.contains(key)) {
                    appendInlines(node.children, key, revealed, linkColor, codeBg, spoilerColor)
                } else {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = key,
                            styles = TextLinkStyles(SpanStyle(color = spoilerColor, background = spoilerColor)),
                            linkInteractionListener = { revealed.add(key) },
                        ),
                    ) {
                        append(plainText(node.children).map { if (it == '\n') '\n' else '█' }.joinToString(""))
                    }
                }
            }
            else -> Unit
        }
    }
}

private fun plainText(nodes: List<ContentNode>): String = buildString {
    fun walk(list: List<ContentNode>) {
        for (n in list) {
            when (n) {
                is TextNode -> append(n.value)
                is InlineCodeNode -> append(n.value)
                is InlineMathNode -> append(n.value)
                is EmphasisNode -> walk(n.children)
                is StrongNode -> walk(n.children)
                is StrikethroughNode -> walk(n.children)
                is LinkNode -> walk(n.children)
                is InlineSpoilerNode -> walk(n.children)
                else -> Unit
            }
        }
    }
    walk(nodes)
}

// ---------- 图片 ----------

@Composable
private fun ImageBlock(node: ImageNode) {
    if (node.isSticker) {
        SubcomposeAsyncImage(
            model = node.url,
            contentDescription = node.alt.ifBlank { null },
            modifier = Modifier.size(112.dp),
            contentScale = ContentScale.Fit,
        )
        return
    }
    val onImageClick = LocalImageClicker.current
    val ratio = node.image?.let { info ->
        val w = info.width ?: 0
        val h = info.height ?: 0
        if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null
    }
    Box(
        modifier = Modifier.fillMaxWidth().clickable { onImageClick(node.url) },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = node.url,
            contentDescription = node.alt.ifBlank { null },
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
            loading = {
                val sizeModifier = if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.height(180.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sizeModifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            },
            error = {
                val sizeModifier = if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.height(120.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sizeModifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "图片加载失败",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

// ---------- 引用 / 列表 / 代码 / 公式 / 表格 ----------

@Composable
private fun BlockquoteView(node: BlockquoteNode, key: String, revealed: MutableSet<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            node.children.forEachIndexed { i, child -> BlockView(child, "$key.q$i", revealed) }
        }
    }
}

@Composable
private fun ListView(node: ListNode, key: String, revealed: MutableSet<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(if (node.isSpread) 8.dp else 4.dp)) {
        node.children.forEachIndexed { index, item ->
            if (item !is ListItemNode) return@forEachIndexed
            Row(modifier = Modifier.fillMaxWidth()) {
                val checked = item.isChecked
                if (checked != null) {
                    Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.size(24.dp))
                } else {
                    val marker = if (node.isOrdered) "${(node.start ?: 1) + index}." else "•"
                    Text(
                        text = marker,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item.children.forEachIndexed { i, child -> BlockView(child, "$key.l${index}_$i", revealed) }
                }
            }
        }
    }
}

@Composable
private fun CodeView(value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
private fun MathView(value: String) {
    // 公式渲染（KaTeX）已进 Growth Tracks；当前如实展示 TeX 源码
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}

@Composable
private fun TableView(node: TableNode) {
    val rows = node.children.filterIsInstance<TableRowNode>()
    if (rows.isEmpty()) return
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val spoilerColor = MaterialTheme.colorScheme.onSurface
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            rows.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    val revealed = remember { mutableStateSetOf<String>() }
                    row.children.filterIsInstance<TableCellNode>().forEachIndexed { cellIndex, cell ->
                        val alignment = when (cell.align) {
                            "center" -> TextAlign.Center
                            "right" -> TextAlign.End
                            else -> TextAlign.Start
                        }
                        Text(
                            text = buildAnnotatedString {
                                appendInlines(
                                    cell.children, "r$rowIndex-c$cellIndex", revealed,
                                    linkColor, codeBg, spoilerColor,
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            textAlign = alignment,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
                if (rowIndex == 0) HorizontalDivider()
            }
        }
    }
}

// ---------- 剧透 / 视频 ----------

@Composable
private fun SpoilerBlockView(node: SpoilerNode, key: String, revealed: MutableSet<String>) {
    if (revealed.contains(key)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            node.children.forEachIndexed { i, child -> BlockView(child, "$key.s$i", revealed) }
        }
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { revealed.add(key) },
    ) {
        Text(
            text = "剧透内容，点击显示",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@Composable
private fun VideoLink(url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { uriHandler.openUri(url) }
            .padding(12.dp),
    ) {
        Icon(
            Icons.Outlined.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "视频（点击用浏览器打开）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}
