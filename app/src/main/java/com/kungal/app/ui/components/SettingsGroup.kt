package com.kungal.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 设置式分组卡片（自 saku 项目移植，MIT）：一组多行，首尾行 20dp 大圆角、
 * 中间行 4dp 小圆角（向内收），按下的行圆角展开为大圆角。
 */
private val GroupCorner = 20.dp
private val InnerCorner = 4.dp
private val RowSpacing = 2.dp

enum class GroupTone { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

private typealias RowContent = @Composable RowScope.() -> Unit
private typealias GroupRowBlock = @Composable (index: Int, count: Int) -> Unit

class SettingsGroupScope internal constructor() {
    internal val items = mutableListOf<GroupRowBlock>()

    fun item(
        headline: String,
        supporting: String? = null,
        icon: ImageVector? = null,
        tone: GroupTone = GroupTone.PRIMARY,
        onClick: (() -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        items.add { index, count ->
            GroupRow(index = index, count = count, onClick = onClick) {
                if (icon != null) {
                    GroupIcon(icon, tone)
                }
                Column(Modifier.weight(1f)) {
                    Text(headline, style = MaterialTheme.typography.bodyLarge)
                    if (supporting != null) {
                        Text(
                            supporting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
        }
    }

    fun switchItem(
        headline: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        supporting: String? = null,
        icon: ImageVector? = null,
        tone: GroupTone = GroupTone.PRIMARY,
    ) {
        item(
            headline = headline,
            supporting = supporting,
            icon = icon,
            tone = tone,
            onClick = { onCheckedChange(!checked) },
            trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        )
    }

    fun block(content: @Composable ColumnScope.() -> Unit) {
        items.add { index, count ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = groupShape(index, count),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit,
) {
    val scope = SettingsGroupScope()
    scope.content()
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 8.dp),
        )
        scope.items.forEachIndexed { index, row ->
            row(index, scope.items.size)
            if (index != scope.items.lastIndex) Spacer(Modifier.height(RowSpacing))
        }
    }
}

/**
 * 行首图标：22dp。默认与正文一致的前景色（onSurfaceVariant），
 * 仅需要强调的入口（危险操作/品牌动作）才显式传 tone 使用主题色。
 */
@Composable
fun GroupIcon(
    icon: ImageVector,
    tone: GroupTone = GroupTone.NEUTRAL,
) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = toneColor(tone),
    )
}

@Composable
private fun toneColor(tone: GroupTone): Color = when (tone) {
    GroupTone.PRIMARY -> MaterialTheme.colorScheme.primary
    GroupTone.SECONDARY -> MaterialTheme.colorScheme.secondary
    GroupTone.TERTIARY -> MaterialTheme.colorScheme.tertiary
    GroupTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun GroupRow(
    index: Int,
    count: Int,
    onClick: (() -> Unit)?,
    content: RowContent,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = animatedRowShape(index, count, pressed)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

private fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) GroupCorner else InnerCorner
    val bottom = if (index == count - 1) GroupCorner else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun animatedRowShape(index: Int, count: Int, pressed: Boolean): Shape {
    val targetTop = if (pressed || index == 0) GroupCorner else InnerCorner
    val targetBottom = if (pressed || index == count - 1) GroupCorner else InnerCorner
    val top by animateDpAsState(
        targetValue = targetTop,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "rowTopCorner",
    )
    val bottom by animateDpAsState(
        targetValue = targetBottom,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "rowBottomCorner",
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
