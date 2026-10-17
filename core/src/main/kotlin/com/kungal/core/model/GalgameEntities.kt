package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 目录实体（BP-02 U3）：标签 / 会社 / 引擎 / 系列 / 角色 / Staff / 特质。 */

@Serializable
data class CatalogName(
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
)

@Serializable
data class TagRef(
    @SerialName("object") val obj: String = "tag",
    val id: String = "",
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
)

@Serializable
data class TagSummary(
    @SerialName("object") val obj: String = "tag",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("tag_kind") val tagKind: String = "content",
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    @SerialName("descendant_count") val descendantCount: Int = 0,
)

@Serializable
data class TagNode(
    @SerialName("object") val obj: String = "tag",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("parent_ids") val parentIds: List<String> = emptyList(),
)

@Serializable
data class TagNodeList(
    @SerialName("object") val obj: String = "list",
    val items: List<TagNode> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class TagDetail(
    @SerialName("object") val obj: String = "tag",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("tag_kind") val tagKind: String = "content",
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    @SerialName("is_hidden") val isHidden: Boolean = false,
    val intros: List<CatalogIntro> = emptyList(),
    @SerialName("parent_tags") val parentTags: List<TagRef> = emptyList(),
    val subtags: List<TagSummary> = emptyList(),
    @SerialName("descendant_count") val descendantCount: Int = 0,
    val ancestors: List<TagNode> = emptyList(),
)

@Serializable
data class CompanySummary(
    @SerialName("object") val obj: String = "company",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("company_kind") val companyKind: String = "",
    val logo: ImageInfo? = null,
    val aliases: List<String> = emptyList(),
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
)

@Serializable
data class CompanyDetail(
    @SerialName("object") val obj: String = "company",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("company_kind") val companyKind: String = "",
    val logo: ImageInfo? = null,
    val aliases: List<String> = emptyList(),
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    val lang: String? = null,
    val links: List<CatalogLink> = emptyList(),
    val intros: List<CatalogIntro> = emptyList(),
)

@Serializable
data class CompanyWork(
    @SerialName("object") val obj: String = "company_work",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    @SerialName("via_company") val viaCompany: CompanyRef? = null,
)

@Serializable
data class CompanyGraph(
    @SerialName("object") val obj: String = "company_graph",
    @SerialName("company_id") val companyId: String = "",
    val nodes: List<CompanyGraphNode> = emptyList(),
    val edges: List<CompanyGraphEdge> = emptyList(),
)

@Serializable
data class CompanyGraphNode(
    @SerialName("object") val obj: String = "company",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val logo: ImageInfo? = null,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
)

@Serializable
data class CompanyGraphEdge(
    @SerialName("from_company_id") val fromCompanyId: String = "",
    @SerialName("to_company_id") val toCompanyId: String = "",
    val relation: String = "",
)

@Serializable
data class CharacterSummary(
    @SerialName("object") val obj: String = "character",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val image: ImageInfo? = null,
    @SerialName("catalog_work_count") val catalogWorkCount: Int = 0,
    @SerialName("matched_traits") val matchedTraits: List<TraitRef> = emptyList(),
)

@Serializable
data class CharacterDetail(
    @SerialName("object") val obj: String = "character",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val lang: String? = null,
    val image: ImageInfo? = null,
    val figure: ImageInfo? = null,
    val intros: List<CatalogIntro> = emptyList(),
    val traits: List<CharacterTrait> = emptyList(),
    val links: List<CatalogLink> = emptyList(),
)

@Serializable
data class CharacterTrait(
    @SerialName("object") val obj: String = "trait",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("trait_group") val traitGroup: CatalogName = CatalogName(),
    @SerialName("trait_group_id") val traitGroupId: String = "",
    val spoiler: String = "none",
    @SerialName("is_lie") val isLie: Boolean = false,
    @SerialName("is_sexual") val isSexual: Boolean = false,
)

@Serializable
data class Appearance(
    @SerialName("object") val obj: String = "appearance",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    val voices: List<CreditNameRef> = emptyList(),
)

@Serializable
data class AppearanceList(
    @SerialName("object") val obj: String = "list",
    val items: List<Appearance> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class CreditNameDetail(
    @SerialName("object") val obj: String = "credit_name",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    val lang: String? = null,
    val photo: ImageInfo? = null,
    val gender: String? = null,
    @SerialName("birth_year") val birthYear: Int? = null,
    @SerialName("birth_month") val birthMonth: Int? = null,
    @SerialName("birth_day") val birthDay: Int? = null,
    val intros: List<CatalogIntro> = emptyList(),
    val links: List<CatalogLink> = emptyList(),
    val siblings: List<CreditNameRef> = emptyList(),
)

@Serializable
data class Credit(
    @SerialName("object") val obj: String = "credit",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    @SerialName("credit_roles") val creditRoles: List<CreditRole> = emptyList(),
    val characters: List<CreditCharacter> = emptyList(),
)

@Serializable
data class CreditRole(
    @SerialName("role_key") val roleKey: String = "",
    @SerialName("display_name") val displayName: String = "",
)

@Serializable
data class CreditCharacter(
    @SerialName("character_id") val characterId: String? = null,
    @SerialName("display_name") val displayName: String = "",
)

@Serializable
data class CreditList(
    @SerialName("object") val obj: String = "list",
    val items: List<Credit> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class TraitRef(
    @SerialName("object") val obj: String = "trait",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("trait_group") val traitGroup: CatalogName = CatalogName(),
    @SerialName("trait_group_id") val traitGroupId: String = "",
)

@Serializable
data class TraitSummary(
    @SerialName("object") val obj: String = "trait",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("trait_group") val traitGroup: CatalogName = CatalogName(),
    @SerialName("trait_group_id") val traitGroupId: String = "",
    val parents: List<TraitRef> = emptyList(),
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("is_searchable") val isSearchable: Boolean = true,
    @SerialName("character_count") val characterCount: Int = 0,
    @SerialName("child_count") val childCount: Int = 0,
)

@Serializable
data class TraitDetail(
    @SerialName("object") val obj: String = "trait",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val latin: String? = null,
    val localized: Map<String, LocalizedText> = emptyMap(),
    @SerialName("trait_group") val traitGroup: CatalogName = CatalogName(),
    @SerialName("trait_group_id") val traitGroupId: String = "",
    val parents: List<TraitRef> = emptyList(),
    @SerialName("is_sexual") val isSexual: Boolean = false,
    @SerialName("is_searchable") val isSearchable: Boolean = true,
    @SerialName("character_count") val characterCount: Int = 0,
    @SerialName("child_count") val childCount: Int = 0,
    val aliases: List<String> = emptyList(),
    val description: String = "",
    val intros: List<CatalogIntro> = emptyList(),
    val subtraits: List<TraitSummary> = emptyList(),
)

/** 收藏夹编辑请求；只发送非 null 字段。 */
@Serializable
data class CollectionPatchBody(
    val title: String? = null,
    val description: String? = null,
    val visibility: String? = null,
    @SerialName("is_default") val isDefault: Boolean? = null,
)
