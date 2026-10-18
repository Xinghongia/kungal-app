package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.CollectionSummary
import com.kungal.core.model.FollowEdge
import com.kungal.core.model.GalgameResourceSummary
import com.kungal.core.model.PlaytimePage
import com.kungal.core.model.QuizSummary
import com.kungal.core.model.RatingSummary
import com.kungal.core.model.ToolsetSummary
import com.kungal.core.model.UserCommentItem
import com.kungal.core.model.UserProfile
import com.kungal.core.model.UserReplyItem
import com.kungal.core.model.UserResolveList
import com.kungal.core.model.UserTopicItem
import com.kungal.core.model.UserWorkRecord
import com.kungal.core.model.WallCommentList
import com.kungal.core.model.WorkRecordPage
import com.kungal.core.model.WorkSummary
import com.kungal.core.net.PageList
import com.kungal.core.net.PagedList

/**
 * 用户域只读接口：任意用户主页、统计、内容列表、关注/粉丝、评分/资源/题库/工具集、
 * 本人游玩时长。写操作（关注/资料/签到）分别在 SocialApi / MeApi。
 */
class UserApi(private val client: KungalClient) {

    /** 按昵称搜索或按 id 批量解析（GET /users）；q 与 ids 二选一，limit 只对 q 生效。 */
    suspend fun resolveUsers(
        q: String? = null,
        ids: List<String> = emptyList(),
        limit: Int = 8,
    ): UserResolveList {
        require(q != null || ids.isNotEmpty()) { "q 与 ids 必须提供其一" }
        return client.get(
            path = "/users",
            query = buildMap {
                if (ids.isNotEmpty()) {
                    put("ids", ids.take(100))
                } else {
                    put("q", q)
                    put("limit", limit.coerceIn(1, 20))
                }
            },
            deserializer = UserResolveList.serializer(),
        )
    }

    suspend fun profile(userId: String): UserProfile = client.get(
        path = "/users/$userId",
        deserializer = UserProfile.serializer(),
    )

    /** relation: authored / liked / upvoted / favorited / hidden（hidden 由服务端按权限过滤）。 */
    suspend fun topics(
        userId: String,
        relation: String = "authored",
        page: Int = 1,
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PagedList<UserTopicItem> = client.get(
        path = "/users/$userId/topics",
        query = mapOf(
            "relation" to relation,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(UserTopicItem.serializer()),
    )

    /** relation: authored / received / liked。 */
    suspend fun replies(
        userId: String,
        relation: String = "authored",
        page: Int = 1,
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PagedList<UserReplyItem> = client.get(
        path = "/users/$userId/replies",
        query = mapOf(
            "relation" to relation,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(UserReplyItem.serializer()),
    )

    /** relation: authored / received / liked。 */
    suspend fun comments(
        userId: String,
        relation: String = "authored",
        page: Int = 1,
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PagedList<UserCommentItem> = client.get(
        path = "/users/$userId/comments",
        query = mapOf(
            "relation" to relation,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(UserCommentItem.serializer()),
    )

    /** relation: published / contributed / liked。 */
    suspend fun works(
        userId: String,
        relation: String = "published",
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<WorkSummary> = client.get(
        path = "/users/$userId/works",
        query = mapOf(
            "relation" to relation,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(WorkSummary.serializer()),
    )

    suspend fun collections(
        userId: String,
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<CollectionSummary> = client.get(
        path = "/users/$userId/collections",
        query = mapOf("page" to page, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PagedList.serializer(CollectionSummary.serializer()),
    )

    /** 游戏库：游标分页；state 传 PlayState 的服务器分组（wish/doing/done/on_hold/dropped）。 */
    suspend fun workRecords(
        userId: String,
        states: List<String> = emptyList(),
        cursor: String? = null,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): WorkRecordPage = client.get(
        path = "/users/$userId/work-records",
        query = mapOf(
            "state" to states,
            "cursor" to cursor,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = WorkRecordPage.serializer(),
    )

    /** relation: authored / liked；subjectType 传 `galgame` 只看作品留言墙，null 表示全部墙。 */
    suspend fun wallComments(
        userId: String,
        relation: String = "authored",
        subjectType: String? = "galgame",
        cursor: String? = null,
        limit: Int = 24,
    ): WallCommentList = client.get(
        path = "/users/$userId/wall-comments",
        query = mapOf(
            "relation" to relation,
            "subject_type" to subjectType,
            "cursor" to cursor,
            "limit" to limit,
        ),
        deserializer = WallCommentList.serializer(),
    )

    suspend fun followers(userId: String, cursor: String? = null, limit: Int = 30): PageList<FollowEdge> =
        client.get(
            path = "/users/$userId/followers",
            query = mapOf("cursor" to cursor, "limit" to limit),
            deserializer = PageList.serializer(FollowEdge.serializer()),
        )

    suspend fun following(userId: String, cursor: String? = null, limit: Int = 30): PageList<FollowEdge> =
        client.get(
            path = "/users/$userId/following",
            query = mapOf("cursor" to cursor, "limit" to limit),
            deserializer = PageList.serializer(FollowEdge.serializer()),
        )

    /** 用户写过的评分（/ratings?author_id=）。 */
    suspend fun ratings(
        authorId: String,
        page: Int = 1,
        limit: Int = 24,
        includeNsfw: Boolean = false,
    ): PagedList<RatingSummary> = client.get(
        path = "/ratings",
        query = mapOf(
            "author_id" to authorId,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(RatingSummary.serializer()),
    )

    /** relation: published / liked；state: valid / expired（null 表示不限，点赞列表用）。 */
    suspend fun resources(
        userId: String,
        relation: String = "published",
        state: String? = "valid",
        page: Int = 1,
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PagedList<GalgameResourceSummary> = client.get(
        path = "/users/$userId/galgame-resources",
        query = mapOf(
            "relation" to relation,
            "state" to state,
            "page" to page,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PagedList.serializer(GalgameResourceSummary.serializer()),
    )

    /** 用户出的题（/quizzes?author_id=）。 */
    suspend fun quizzes(
        authorId: String,
        page: Int = 1,
        limit: Int = 50,
    ): PagedList<QuizSummary> = client.get(
        path = "/quizzes",
        query = mapOf("author_id" to authorId, "page" to page, "limit" to limit),
        deserializer = PagedList.serializer(QuizSummary.serializer()),
    )

    /** relation: authored / favorited。 */
    suspend fun toolsets(
        userId: String,
        relation: String = "authored",
        page: Int = 1,
        limit: Int = 24,
    ): PagedList<ToolsetSummary> = client.get(
        path = "/users/$userId/toolsets",
        query = mapOf("relation" to relation, "page" to page, "limit" to limit),
        deserializer = PagedList.serializer(ToolsetSummary.serializer()),
    )

    /** 本人作答过的题（/me/answered-quizzes，仅本人可读）。 */
    suspend fun answeredQuizzes(page: Int = 1, limit: Int = 50): PagedList<QuizSummary> = client.get(
        path = "/me/answered-quizzes",
        query = mapOf("page" to page, "limit" to limit),
        deserializer = PagedList.serializer(QuizSummary.serializer()),
    )

    /** 本人游玩时长（/me/playtimes，仅本人可读）。 */
    suspend fun playtimes(cursor: String? = null, limit: Int = 24): PlaytimePage = client.get(
        path = "/me/playtimes",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PlaytimePage.serializer(),
    )
}
