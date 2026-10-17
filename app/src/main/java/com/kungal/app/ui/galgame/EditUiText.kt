package com.kungal.app.ui.galgame

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/** 编辑表单字段的中文标签；与网页 `galgameEdit.ts` 的分组名一致。 */
fun editFieldLabel(key: String): String = when (key) {
    "catalog.work.titles" -> "标题与别名"
    "catalog.work.titles.suppressed" -> "隐藏的标题"
    "catalog.work.display_name" -> "条目名"
    "catalog.work.intros" -> "介绍"
    "catalog.work.olang" -> "原始语言"
    "catalog.work.content_rating" -> "年龄分级"
    "catalog.work.display_nsfw" -> "展示素材为 NSFW"
    "catalog.work.series_ids" -> "所属系列"
    "catalog.work.tag_ids" -> "标签"
    "catalog.work.labels" -> "会社"
    "catalog.work.engine_ids" -> "引擎"
    "catalog.work.roster" -> "出演名单"
    "catalog.work.roster.suppressed" -> "隐藏的角色"
    "catalog.work.credits" -> "本站署名"
    "catalog.work.credits.suppressed" -> "隐藏的上游署名"
    "catalog.work.links" -> "相关链接"
    "catalog.work.covers" -> "封面"
    "catalog.work.screenshots" -> "画廊"
    "catalog.work.release_date" -> "发售日期"
    "catalog.work.release_date_precision" -> "发售日期精度"
    else -> key.removePrefix("catalog.work.").removePrefix("galgame.game.")
}

/** 编辑表单的分组与顺序（对应网页的七个分组）。 */
enum class EditGroup(val label: String) {
    TITLES("标题"),
    INTRO("介绍"),
    BASIC("基本信息"),
    RELATIONS("关联条目"),
    CAST("出演与署名"),
    EXTRAS("链接"),
    IMAGES("图片"),
    OTHER("其他"),
}

fun editGroupOf(key: String): EditGroup = when (key) {
    "catalog.work.titles", "catalog.work.titles.suppressed",
    "catalog.work.display_name" -> EditGroup.TITLES
    "catalog.work.intros" -> EditGroup.INTRO
    "catalog.work.olang", "catalog.work.content_rating", "catalog.work.display_nsfw",
    "catalog.work.release_date", "catalog.work.release_date_precision" -> EditGroup.BASIC
    "catalog.work.series_ids", "catalog.work.tag_ids", "catalog.work.labels",
    "catalog.work.engine_ids" -> EditGroup.RELATIONS
    "catalog.work.roster", "catalog.work.roster.suppressed",
    "catalog.work.credits", "catalog.work.credits.suppressed" -> EditGroup.CAST
    "catalog.work.links" -> EditGroup.EXTRAS
    "catalog.work.covers", "catalog.work.screenshots" -> EditGroup.IMAGES
    else -> EditGroup.OTHER
}

val EditLanguageOptions = listOf(
    "" to "无语言",
    "ja" to "日语",
    "zh-Hans" to "简体中文",
    "zh-Hant" to "繁体中文",
    "en" to "英语",
)

val EditOfficialLanguageOptions = listOf(
    "ja" to "日语",
    "zh-Hans" to "简体中文",
    "zh-Hant" to "繁体中文",
    "en" to "英语",
)

fun editLanguageLabel(tag: String): String =
    EditLanguageOptions.firstOrNull { it.first == tag }?.second ?: tag

val EditContentRatingOptions = listOf(
    0 to "全年龄",
    1 to "敏感",
    2 to "R18",
)

val EditTitleKindOptions = listOf(
    0 to "官方名",
    1 to "别名",
    2 to "缩写",
)

fun editTitleKindLabel(kind: Int): String =
    EditTitleKindOptions.firstOrNull { it.first == kind }?.second ?: "官方名"

val EditLabelKindOptions = listOf(
    0 to "同人社团",
    1 to "发行商",
    2 to "开发商",
    3 to "品牌",
)

fun editLabelKindLabel(kind: Int): String =
    EditLabelKindOptions.firstOrNull { it.first == kind }?.second ?: "开发商"

val EditRosterKindOptions = listOf(
    0 to "未分类",
    1 to "主角",
    2 to "配角",
    3 to "出场",
)

fun editRosterKindLabel(kind: Int): String =
    EditRosterKindOptions.firstOrNull { it.first == kind }?.second ?: "未分类"

val EditSpoilerOptions = listOf(
    0 to "无剧透",
    1 to "轻微",
    2 to "严重",
)

fun editSpoilerLevelLabel(level: Int): String =
    EditSpoilerOptions.firstOrNull { it.first == level }?.second ?: "无剧透"

val EditSexualOptions = listOf(
    0 to "无性内容",
    1 to "性暗示",
    2 to "露骨",
)

fun editSexualLabel(level: Int): String =
    EditSexualOptions.firstOrNull { it.first == level }?.second ?: "无性内容"

val EditViolenceOptions = listOf(
    0 to "无暴力",
    1 to "暴力",
    2 to "血腥",
)

fun editViolenceLabel(level: Int): String =
    EditViolenceOptions.firstOrNull { it.first == level }?.second ?: "无暴力"

/** 本站署名的职务词表（与网页 `CreditsField.vue` 一致，值为 catalog 的 role id）。 */
val EditCreditRoles = listOf(
    247 to "脚本",
    184 to "原画",
    145 to "人设",
    209 to "音乐",
    173 to "导演",
    1 to "声优",
    286 to "演唱",
    199 to "作词",
    158 to "作曲",
    115 to "编曲",
    3 to "翻译",
    4 to "编辑",
    5 to "QA",
    2 to "其他",
)

fun editCreditRoleLabel(roleId: Int): String =
    EditCreditRoles.firstOrNull { it.first == roleId }?.second ?: "职务 #$roleId"

/** 图片哈希 → 内容寻址 CDN 地址。 */
fun editImageUrl(hash: String): String =
    "https://image.kungal.iloveren.link/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$hash.webp"

// ---------- JsonElement 读写 ----------

fun JsonElement?.asText(fallback: String = ""): String = when (this) {
    is JsonPrimitive -> content
    else -> fallback
}

fun JsonElement?.asInt(default: Int = 0): Int = when (this) {
    is JsonPrimitive -> intOrNull ?: default
    else -> default
}

fun JsonElement?.asBool(default: Boolean = false): Boolean = when (this) {
    is JsonPrimitive -> booleanOrNull ?: when (content.lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> default
    }
    else -> default
}

fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

fun JsonElement?.asArrayOrEmpty(): List<JsonElement> =
    (this as? JsonArray)?.toList() ?: emptyList()

fun JsonObject.string(key: String): String = this[key].asText()

fun JsonObject.int(key: String): Int = this[key].asInt()

fun JsonObject.bool(key: String): Boolean = this[key].asBool()

fun JsonObject.stringList(key: String): List<String> =
    this[key].asArrayOrEmpty().map { it.asText() }.filter { it.isNotBlank() }

fun JsonObject.array(key: String): List<JsonElement> = this[key].asArrayOrEmpty()

fun JsonObject.objectOrNull(key: String): JsonObject? = this[key].asObjectOrNull()

/** 年龄分级既可能是 int 编码（0/1/2），也可能是 token（all_ages/sensitive/r18）。 */
fun contentRatingCode(value: JsonElement?): Int {
    val primitive = value as? JsonPrimitive ?: return -1
    primitive.intOrNull?.let { return it }
    return when (primitive.content) {
        "all_ages" -> 0
        "sensitive" -> 1
        "r18" -> 2
        else -> -1
    }
}

/** 字段值的中文摘要（版本差异、提案卡片用）；null 表示该版本未设置。 */
fun editValueSummary(key: String, value: JsonElement?): String {
    if (value == null || value is JsonNull) return "（未设置）"
    if (value !is JsonArray) {
        return when (key) {
            "catalog.work.display_nsfw" -> if (value.asBool()) "是" else "否"
            "catalog.work.content_rating" -> {
                val code = contentRatingCode(value)
                EditContentRatingOptions.firstOrNull { it.first == code }?.second
                    ?: value.asText()
            }
            "catalog.work.olang" -> editLanguageLabel(value.asText())
            else -> value.asText().ifBlank { "（空）" }.let {
                if (it.length > 200) it.take(200) + "…" else it
            }
        }
    }
    val rows = value.asArrayOrEmpty()
    if (rows.isEmpty()) return "（空）"
    return when (key) {
        "catalog.work.titles" -> rows.joinToString("、") { row ->
            val obj = row.asObjectOrNull() ?: return@joinToString row.asText()
            "${obj.string("title")}（${editLanguageLabel(obj.string("lang"))}·${editTitleKindLabel(obj.int("kind"))}）"
        }
        "catalog.work.intros" -> rows.joinToString("、") { row ->
            val obj = row.asObjectOrNull() ?: return@joinToString ""
            "${editLanguageLabel(obj.string("lang"))}：${obj.string("intro").take(60)}"
        }
        "catalog.work.covers" -> rows.mapIndexed { i, row ->
            val obj = row.asObjectOrNull() ?: return@mapIndexed "#${i + 1}"
            val hash = obj.string("image_hash")
            "#${i + 1} ${obj.string("kind").ifBlank { "未标类型" }} (${hash.take(8)}…)"
        }.joinToString("、")
        "catalog.work.screenshots" -> "${rows.size} 张"
        "catalog.work.roster" -> rows.joinToString("、") { row ->
            val obj = row.asObjectOrNull() ?: return@joinToString ""
            "#${obj.int("character_id")} ${editRosterKindLabel(obj.int("kind"))}"
        }
        "catalog.work.credits" -> rows.joinToString("、") { row ->
            val obj = row.asObjectOrNull() ?: return@joinToString ""
            "${editCreditRoleLabel(obj.int("role_id"))} #${obj.int("credit_name_id")}"
        }
        "catalog.work.labels" -> rows.joinToString("、") { row ->
            val obj = row.asObjectOrNull() ?: return@joinToString ""
            "#${obj.int("label_id")} ${editLabelKindLabel(obj.int("kind"))}"
        }
        else -> if (rows.all { it is JsonPrimitive }) {
            rows.joinToString("、") { it.asText() }
        } else {
            "${rows.size} 项"
        }
    }
}
