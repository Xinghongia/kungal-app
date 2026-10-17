package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.core.api.EntityWorkQuery
import com.kungal.core.net.PagedList
import com.kungal.core.model.WorkSummary

data class EntityWorksFilters(
    val sortField: String = "time",
    val sortOrder: String = "desc",
    val platform: String? = null,
    val language: String? = null,
    val resourceType: String? = null,
    val gameType: String? = null,
    val via: String? = null,
) {
    fun toQuery(includeNsfw: Boolean, page: Int = 1, limit: Int = 24): EntityWorkQuery {
        val emulator = platform == "emulator"
        return EntityWorkQuery(
            page = page,
            limit = limit,
            sort = workSortToken(sortField, sortOrder),
            resourceType = resourceType,
            resourcePlatform = platform?.takeIf { it != "emulator" },
            resourceRuntime = if (emulator) "emulator" else null,
            resourceLanguage = language,
            gameType = gameType,
            includeNsfw = includeNsfw,
            via = via,
        )
    }
}

/**
 * 实体页的作品列表：头部（实体资料）+ 单值筛选条 + 作品行 + 页码分页。
 * 实体接口的资源轴只接受单值，与网页实体页的普通筛选一致。
 */
@Composable
fun EntityWorksList(
    entityKey: String,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    load: suspend (EntityWorkQuery) -> PagedList<WorkSummary>,
    onOpenWork: (String) -> Unit,
    header: @Composable () -> Unit,
    showVia: Boolean = false,
    rowTrailing: (@Composable (WorkSummary) -> Unit)? = null,
) {
    var filters by remember(entityKey) {
        mutableStateOf(EntityWorksFilters())
    }
    PagedListContent(
        key = Triple(entityKey, filters, includeNsfw),
        load = { page, _ ->
            val result = load(filters.toQuery(includeNsfw, page = page))
            PageResult(
                items = result.items,
                ended = result.items.isEmpty() || result.items.size < 24 ||
                    (result.total > 0 && page * 24 >= result.total),
            )
        },
        emptyText = "没有找到符合条件的作品",
        keyOf = { it.id },
        header = {
            header()
            EntityFilterRow(filters, showVia = showVia, onChange = { filters = it })
        },
    ) { work ->
        EntityWorkRow(
            work = work,
            blurCover = blurNsfw,
            onClick = { onOpenWork(work.id) },
            trailing = rowTrailing?.let { slot -> { slot(work) } },
        )
    }
}

@Composable
fun EntityFilterRow(
    filters: EntityWorksFilters,
    showVia: Boolean,
    onChange: (EntityWorksFilters) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sortLabel = labelOf(GalgameResourceSortFields, filters.sortField)
        EntityFilterMenu(
            label = sortLabel,
            options = GalgameResourceSortFields,
            selected = filters.sortField,
            onSelect = { value -> value?.let { onChange(filters.copy(sortField = it)) } },
        )
        IconButton(onClick = {
            onChange(filters.copy(sortOrder = if (filters.sortOrder == "desc") "asc" else "desc"))
        }) {
            Icon(
                if (filters.sortOrder == "desc") Icons.Outlined.ArrowDownward
                else Icons.Outlined.ArrowUpward,
                contentDescription = if (filters.sortOrder == "desc") "降序" else "升序",
                modifier = Modifier.size(18.dp),
            )
        }
        EntityFilterMenu(
            label = filters.platform?.let { labelOf(GalgamePlatformOptions, it) } ?: "平台",
            options = GalgamePlatformOptions,
            selected = filters.platform,
            onSelect = { onChange(filters.copy(platform = it)) },
            allLabel = "全部平台",
        )
        EntityFilterMenu(
            label = filters.language?.let { labelOf(GalgameLanguageOptions, it) } ?: "语言",
            options = GalgameLanguageOptions,
            selected = filters.language,
            onSelect = { onChange(filters.copy(language = it)) },
            allLabel = "全部语言",
        )
        EntityFilterMenu(
            label = filters.resourceType?.let { labelOf(GalgameResourceTypeOptions, it) } ?: "资源类型",
            options = GalgameResourceTypeOptions,
            selected = filters.resourceType,
            onSelect = { onChange(filters.copy(resourceType = it)) },
            allLabel = "全部类型",
        )
        EntityFilterMenu(
            label = labelOf(GalgameGameTypeOptions, filters.gameType ?: "uncategorized")
                .takeIf { filters.gameType != null } ?: "游戏类型",
            options = GalgameGameTypeOptions,
            selected = filters.gameType,
            onSelect = { onChange(filters.copy(gameType = it)) },
            allLabel = "全部作品",
        )
        if (showVia) {
            EntityFilterMenu(
                label = when (filters.via) {
                    "own" -> "仅自身"
                    "imprint" -> "仅品牌"
                    else -> "作品来源"
                },
                options = listOf(
                    FilterOption("own", "仅自身作品"),
                    FilterOption("imprint", "仅旗下品牌作品"),
                ),
                selected = filters.via,
                onSelect = { onChange(filters.copy(via = it)) },
                allLabel = "自身与品牌",
            )
        }
    }
}

@Composable
fun EntityFilterMenu(
    label: String,
    options: List<FilterOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
    allLabel: String = "全部",
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    open = false
                    onSelect(null)
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        open = false
                        onSelect(option.value)
                    },
                )
            }
        }
    }
}

/** 目录列表的搜索行：显式点搜索/回车才发起请求，避免逐字请求。 */
@Composable
fun EntitySearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        TextButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("搜索")
        }
    }
}

/** 目录列表的通用标签行（标题 + 数量 + 副标题）。 */
@Composable
fun EntityListRow(
    title: String,
    subtitle: String?,
    trailing: String?,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
