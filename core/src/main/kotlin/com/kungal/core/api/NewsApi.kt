package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.NewsArchive
import com.kungal.core.model.NewsFeedPage
import com.kungal.core.model.NewsItem
import com.kungal.core.model.NewsMonth
import com.kungal.core.model.NewsSource
import com.kungal.core.model.NewsSourceList
import com.kungal.core.net.PagedList

/**
 * Galgame 情报（BP-05 U5，网页 /news）。全部端点匿名可读：
 * 列表按游标翻页，月归档按页码翻页；详情只有用户投稿自带正文。
 */
class NewsApi(private val client: KungalClient) {

    /** [lane] = news / column；[year] 与 [month] 需成对使用。 */
    suspend fun items(
        cursor: String? = null,
        limit: Int = 20,
        includeTotal: Boolean = true,
        lane: String? = null,
        source: String? = null,
        year: Int? = null,
        month: Int? = null,
    ): NewsFeedPage = client.get(
        path = "/news-items",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("include_total", includeTotal)
            put("lane", lane)
            put("news_source", source)
            put("year", year)
            if (year != null) put("month", month)
        },
        deserializer = NewsFeedPage.serializer(),
    )

    suspend fun sources(): List<NewsSource> = client.get(
        path = "/news-sources",
        deserializer = NewsSourceList.serializer(),
    ).items

    /** 年份索引；给定 [year] 时同时返回该年各月计数。 */
    suspend fun archive(
        lane: String? = null,
        source: String? = null,
        year: Int? = null,
    ): NewsArchive = client.get(
        path = "/news-archive",
        query = buildMap {
            put("lane", lane)
            put("news_source", source)
            put("year", year)
        },
        deserializer = NewsArchive.serializer(),
    )

    /** 单月概览：整月条数与逐日计数（空日也返回）。 */
    suspend fun month(
        year: Int,
        month: Int,
        lane: String? = null,
        source: String? = null,
    ): NewsMonth = client.get(
        path = "/news-archive/$year/$month",
        query = buildMap {
            put("lane", lane)
            put("news_source", source)
        },
        deserializer = NewsMonth.serializer(),
    )

    /** 单月条目，页码分页（1 起，limit 上限 100）。 */
    suspend fun monthItems(
        year: Int,
        month: Int,
        page: Int = 1,
        limit: Int = 50,
        lane: String? = null,
        source: String? = null,
        day: Int? = null,
    ): PagedList<NewsItem> = client.get(
        path = "/news-archive/$year/$month/items",
        query = buildMap {
            put("page", page)
            put("limit", limit)
            put("lane", lane)
            put("news_source", source)
            put("day", day)
        },
        deserializer = PagedList.serializer(NewsItem.serializer()),
    )

    suspend fun item(id: String): NewsItem = client.get(
        path = "/news-items/$id",
        deserializer = NewsItem.serializer(),
    )
}
