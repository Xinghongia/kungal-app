package com.kungal.app.ui.galgame

import com.kungal.app.KungalApp
import com.kungal.core.model.Work
import kotlinx.serialization.json.JsonObject

/** 编辑界面里需要显示的实体 id → 名称；缺失时退化为 #id。 */
data class EditNames(
    val tags: Map<String, String> = emptyMap(),
    val companies: Map<String, String> = emptyMap(),
    val engines: Map<String, String> = emptyMap(),
    val series: Map<String, String> = emptyMap(),
    val characters: Map<String, String> = emptyMap(),
    val staff: Map<String, String> = emptyMap(),
) {
    fun tag(id: String) = tags[id] ?: "#$id"
    fun company(id: String) = companies[id] ?: "#$id"
    fun engine(id: String) = engines[id] ?: "#$id"
    fun seriesOf(id: String) = series[id] ?: "#$id"
    fun character(id: String) = characters[id] ?: "#$id"
    fun staffOf(id: String) = staff[id] ?: "#$id"
}

data class EditNameIds(
    val tags: Set<String> = emptySet(),
    val companies: Set<String> = emptySet(),
    val engines: Set<String> = emptySet(),
    val series: Set<String> = emptySet(),
    val characters: Set<String> = emptySet(),
    val staff: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = tags.isEmpty() && companies.isEmpty() && engines.isEmpty() &&
            series.isEmpty() && characters.isEmpty() && staff.isEmpty()
}

private fun localizedName(
    displayName: String,
    localized: Map<String, com.kungal.core.model.LocalizedText>,
    id: String,
): String = localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
    ?: displayName.ifBlank { "#$id" }

/** 从词条详情取已知名称（标签、会社、引擎、系列、角色、Staff）。 */
fun namesFromWork(work: Work): EditNames = EditNames(
    tags = work.tags.associate { it.id to localizedName(it.displayName, it.localized, it.id) },
    companies = work.companies.associate { it.id to localizedName(it.displayName, it.localized, it.id) },
    engines = work.engines.associate { it.id to localizedName(it.displayName, it.localized, it.id) },
    series = work.series.associate { it.id to localizedName(it.displayName, it.localized, it.id) },
    characters = work.roster.associate { it.id to localizedName(it.displayName, it.localized, it.id) },
    staff = buildMap {
        for (group in work.credits) {
            for (person in group.people) {
                if (person.id.isNotBlank()) {
                    put(person.id, localizedName(person.displayName, person.localized, person.id))
                }
            }
        }
    },
)

/** 从提案/字段值里收集所有被引用的实体 id。 */
fun editNameIdsFrom(values: JsonObject?): EditNameIds {
    if (values == null) return EditNameIds()
    val tags = mutableSetOf<String>()
    val companies = mutableSetOf<String>()
    val engines = mutableSetOf<String>()
    val series = mutableSetOf<String>()
    val characters = mutableSetOf<String>()
    val staff = mutableSetOf<String>()

    values.stringList("catalog.work.tag_ids").forEach { tags += it }
    values.stringList("catalog.work.engine_ids").forEach { engines += it }
    values.stringList("catalog.work.series_ids").forEach { series += it }
    values.array("catalog.work.labels").forEach { row ->
        row.asObjectOrNull()?.let { obj ->
            obj.string("label_id").takeIf { it.isNotBlank() }?.let { companies += it }
        }
    }
    values.array("catalog.work.roster").forEach { row ->
        row.asObjectOrNull()?.let { obj ->
            obj.string("character_id").takeIf { it.isNotBlank() }?.let { characters += it }
        }
    }
    values.array("catalog.work.credits").forEach { row ->
        row.asObjectOrNull()?.let { obj ->
            obj.string("credit_name_id").takeIf { it.isNotBlank() }?.let { staff += it }
            obj.string("character_id").takeIf { it.isNotBlank() }?.let { characters += it }
        }
    }
    return EditNameIds(
        tags = tags, companies = companies, engines = engines, series = series,
        characters = characters, staff = staff,
    )
}

/**
 * 补齐工作台里缺失的实体名称。标签/会社支持批量 ids；引擎/系列/角色/Staff
 * 只有单条详情，逐个读取（数量由界面上已有的引用决定，通常很小）。
 */
suspend fun resolveEditNames(
    app: KungalApp,
    names: EditNames,
    ids: EditNameIds,
): EditNames {
    var out = names
    val missingTags = ids.tags - out.tags.keys
    if (missingTags.isNotEmpty()) {
        runCatching {
            app.entityApi.tags(ids = missingTags.take(100), limit = 100, includeNsfw = true)
        }.onSuccess { page ->
            out = out.copy(
                tags = out.tags + page.items.associate {
                    it.id to localizedName(it.displayName, it.localized, it.id)
                },
            )
        }
    }
    val missingCompanies = ids.companies - out.companies.keys
    if (missingCompanies.isNotEmpty()) {
        runCatching {
            app.entityApi.companies(ids = missingCompanies.take(100), limit = 100)
        }.onSuccess { page ->
            out = out.copy(
                companies = out.companies + page.items.associate {
                    it.id to localizedName(it.displayName, it.localized, it.id)
                },
            )
        }
    }
    for (id in (ids.engines - out.engines.keys).take(60)) {
        runCatching { app.entityApi.engine(id) }.onSuccess {
            out = out.copy(
                engines = out.engines + (id to localizedName(it.displayName, it.localized, it.id)),
            )
        }
    }
    for (id in (ids.series - out.series.keys).take(60)) {
        runCatching { app.entityApi.series(id) }.onSuccess {
            out = out.copy(
                series = out.series + (id to localizedName(it.displayName, it.localized, it.id)),
            )
        }
    }
    for (id in (ids.characters - out.characters.keys).take(120)) {
        runCatching { app.entityApi.character(id, includeNsfw = true) }.onSuccess {
            out = out.copy(
                characters = out.characters + (id to localizedName(it.displayName, it.localized, it.id)),
            )
        }
    }
    for (id in (ids.staff - out.staff.keys).take(120)) {
        runCatching { app.entityApi.creditName(id) }.onSuccess {
            out = out.copy(staff = out.staff + (id to (it.displayName.ifBlank { "#$id" })))
        }
    }
    return out
}
