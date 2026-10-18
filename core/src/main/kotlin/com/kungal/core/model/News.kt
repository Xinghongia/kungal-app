package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 情报条目（GET /news-items、/news-items/{id}）。
 * [content] 只有详情端点返回；列表端点里是空文档。
 */
@Serializable
data class NewsItem(
    @SerialName("object") val obj: String = "news_item",
    val id: String = "",
    @SerialName("news_source") val newsSource: String = "",
    /** news = 情报摘要，column = 专栏长文。 */
    val lane: String = "news",
    val title: String = "",
    val preview: String = "",
    val banner: ImageInfo? = null,
    @SerialName("source_url") val sourceUrl: String = "",
    /** true 表示本站自带正文（用户投稿），false 表示转载、需去原文阅读。 */
    @SerialName("has_body") val hasBody: Boolean = false,
    val submitter: UserRef? = null,
    @SerialName("published_at") val publishedAt: String = "",
    val content: ContentDocument = ContentDocument(),
)

/** 情报来源（GET /news-sources），含网页要求的署名文案。 */
@Serializable
data class NewsSource(
    @SerialName("object") val obj: String = "news_source",
    val key: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("homepage_url") val homepageUrl: String = "",
    @SerialName("column_url") val columnUrl: String = "",
    val attribution: String = "",
    @SerialName("forum_account") val forumAccount: UserRef? = null,
)

/** 来源目录的信封（order 由站点给出）。 */
@Serializable
data class NewsSourceList(
    @SerialName("object") val obj: String = "list",
    val items: List<NewsSource> = emptyList(),
)

/**
 * 情报流的一页（GET /news-items）：游标分页；
 * 只有 include_total=true 时才有 [total]。
 */
@Serializable
data class NewsFeedPage(
    @SerialName("object") val obj: String = "list",
    val items: List<NewsItem> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Int? = null,
)

@Serializable
data class NewsYearCount(
    val year: Int = 0,
    val count: Int = 0,
)

@Serializable
data class NewsMonthCount(
    val month: Int = 0,
    val count: Int = 0,
)

/** 归档索引（GET /news-archive）：年份倒序；指定 year 时附带该年各月计数。 */
@Serializable
data class NewsArchive(
    @SerialName("object") val obj: String = "news_archive",
    val years: List<NewsYearCount> = emptyList(),
    val months: List<NewsMonthCount> = emptyList(),
)

@Serializable
data class NewsDayCount(
    val day: Int = 0,
    val count: Int = 0,
)

/** 单月概览（GET /news-archive/{year}/{month}）：整月条数与逐日计数。 */
@Serializable
data class NewsMonth(
    @SerialName("object") val obj: String = "news_month",
    val year: Int = 0,
    val month: Int = 0,
    @SerialName("item_count") val itemCount: Int = 0,
    val days: List<NewsDayCount> = emptyList(),
)
