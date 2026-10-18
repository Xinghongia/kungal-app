package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.WebsiteCategory
import com.kungal.core.model.WebsiteDetail
import com.kungal.core.model.WebsiteEngagement
import com.kungal.core.model.WebsiteFeedPage
import com.kungal.core.model.WebsiteTag
import com.kungal.core.model.WebsiteTagGroup
import com.kungal.core.net.PageList

/**
 * Galgame 网站资料库（BP-05 U7，网页 /website）。读公开；
 * 点赞/收藏需登录，返回最新计数与本人状态。管理面（/admin 下的接口）不在 App 范围。
 */
class WebsiteApi(private val client: KungalClient) {

    /** 目录列表：游标分页（网页一次拉 100 条再按分类分组）。 */
    suspend fun list(
        cursor: String? = null,
        limit: Int = 100,
        includeTotal: Boolean = false,
        includeNsfw: Boolean = false,
        categoryId: String? = null,
        tagId: String? = null,
    ): WebsiteFeedPage = client.get(
        path = "/websites",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            put("include_total", includeTotal)
            put("include_nsfw", includeNsfw)
            put("website_category_id", categoryId)
            put("website_tag_id", tagId)
        },
        deserializer = WebsiteFeedPage.serializer(),
    )

    suspend fun detail(host: String): WebsiteDetail = client.get(
        path = "/websites/$host",
        deserializer = WebsiteDetail.serializer(),
    )

    suspend fun categories(): List<WebsiteCategory> = client.get(
        path = "/website-categories",
        query = mapOf("limit" to 100),
        deserializer = PageList.serializer(WebsiteCategory.serializer()),
    ).items

    suspend fun category(slug: String): WebsiteCategory = client.get(
        path = "/website-categories/$slug",
        deserializer = WebsiteCategory.serializer(),
    )

    suspend fun tags(): List<WebsiteTag> = client.get(
        path = "/website-tags",
        query = mapOf("limit" to 100),
        deserializer = PageList.serializer(WebsiteTag.serializer()),
    ).items

    suspend fun tag(slug: String): WebsiteTag = client.get(
        path = "/website-tags/$slug",
        deserializer = WebsiteTag.serializer(),
    )

    suspend fun tagGroups(): List<WebsiteTagGroup> = client.get(
        path = "/website-tag-groups",
        query = mapOf("limit" to 100),
        deserializer = PageList.serializer(WebsiteTagGroup.serializer()),
    ).items

    suspend fun setLike(host: String, liked: Boolean): WebsiteEngagement {
        val path = "/websites/$host/like"
        return if (liked) {
            client.put(path, WebsiteEngagement.serializer())
        } else {
            client.delete(path, WebsiteEngagement.serializer())
        }
    }

    suspend fun setFavorite(host: String, favorited: Boolean): WebsiteEngagement {
        val path = "/websites/$host/favorite"
        return if (favorited) {
            client.put(path, WebsiteEngagement.serializer())
        } else {
            client.delete(path, WebsiteEngagement.serializer())
        }
    }
}
