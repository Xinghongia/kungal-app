package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.CommentCreate
import com.kungal.core.model.CommentPatch
import com.kungal.core.model.CommentSearchHit
import com.kungal.core.model.FollowingActivityGroup
import com.kungal.core.model.FollowingActivitySummary
import com.kungal.core.model.FollowingReadMarker
import com.kungal.core.model.Lottery
import com.kungal.core.model.LotteryCodeReveal
import com.kungal.core.model.LotteryEntry
import com.kungal.core.model.PinReplyBody
import com.kungal.core.model.Poll
import com.kungal.core.model.PollCreate
import com.kungal.core.model.PollVote
import com.kungal.core.model.PollVoteBody
import com.kungal.core.model.ReactionRecord
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyComment
import com.kungal.core.model.ReplyCreate
import com.kungal.core.model.ReplyEngagement
import com.kungal.core.model.ReplyPatch
import com.kungal.core.model.ReplySearchHit
import com.kungal.core.model.ReplySource
import com.kungal.core.model.SubscriptionSet
import com.kungal.core.model.SubscribedTopic
import com.kungal.core.model.Topic
import com.kungal.core.model.TopicCreate
import com.kungal.core.model.TopicDraft
import com.kungal.core.model.TopicDraftCreate
import com.kungal.core.model.TopicDraftSummary
import com.kungal.core.model.TopicEngagement
import com.kungal.core.model.TopicPatch
import com.kungal.core.model.TopicSource
import com.kungal.core.model.TopicStates
import com.kungal.core.model.TopicSubscription
import com.kungal.core.model.TopicUpvote
import com.kungal.core.model.UpvoteCreate
import com.kungal.core.net.PageList
import com.kungal.core.net.PagedList

/**
 * 话题与讨论域的全部读写接口（topics / replies / comments / reactions / favorites /
 * subscriptions / upvotes / drafts / polls / lotteries / following activities / search）。
 * 写操作按站点要求带 Idempotency-Key。
 */
class TopicApi(private val client: KungalClient) {

    private inline fun <reified T> encode(value: T): String = KungalJson.encodeToString(
        kotlinx.serialization.serializer(),
        value,
    )

    // ---------- 话题列表与详情 ----------

    suspend fun list(
        cursor: String? = null,
        limit: Int = 20,
        sort: String? = null,
        category: String? = null,
        section: String? = null,
        includeNsfw: Boolean = false,
    ): PageList<Topic> = client.get(
        path = "/topics",
        query = mapOf(
            "cursor" to cursor,
            "limit" to limit,
            "sort" to sort,
            "category" to category,
            "section" to section,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PageList.serializer(Topic.serializer()),
    )

    suspend fun detail(topicId: String): Topic = client.get(
        path = "/topics/$topicId",
        deserializer = Topic.serializer(),
    )

    /** 浏览计数（与网页打开话题时一致）；失败可静默。 */
    suspend fun markView(topicId: String) {
        client.postNoBody("/topics/$topicId/views")
    }

    // ---------- 回复 ----------

    suspend fun replies(
        topicId: String,
        cursor: String? = null,
        limit: Int = 20,
        sort: String = "floor_asc",
        fromFloor: Int? = null,
    ): PageList<Reply> = client.get(
        path = "/topics/$topicId/replies",
        query = mapOf(
            "cursor" to cursor,
            "limit" to limit,
            "sort" to sort,
            "from_floor" to fromFloor,
        ),
        deserializer = PageList.serializer(Reply.serializer()),
    )

    /** 发表回复。idempotencyKey 必填（服务端强制），同一 key 24h 内重放返回首次结果。 */
    suspend fun createReply(topicId: String, markdown: String, idempotencyKey: String): Reply {
        val body = KungalJson.encodeToString(ReplyCreate.serializer(), ReplyCreate(markdown))
        return client.postJson(
            path = "/topics/$topicId/replies",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = Reply.serializer(),
        )
    }

    suspend fun reply(replyId: String): Reply = client.get(
        path = "/replies/$replyId",
        deserializer = Reply.serializer(),
    )

    /** 编辑回复前读取 Markdown 源文。 */
    suspend fun replySource(replyId: String): ReplySource = client.get(
        path = "/replies/$replyId/source",
        deserializer = ReplySource.serializer(),
    )

    suspend fun updateReply(replyId: String, markdown: String): Reply = client.patchJson(
        path = "/replies/$replyId",
        body = KungalJson.encodeToString(ReplyPatch.serializer(), ReplyPatch(markdown)),
        deserializer = Reply.serializer(),
    )

    suspend fun deleteReply(replyId: String) {
        client.deleteNoBody("/replies/$replyId")
    }

    /** 置顶回复；返回更新后的话题。 */
    suspend fun pinReply(topicId: String, replyId: String): Topic = client.putJson(
        path = "/topics/$topicId/pinned-reply",
        body = KungalJson.encodeToString(PinReplyBody.serializer(), PinReplyBody(replyId)),
        deserializer = Topic.serializer(),
    )

    suspend fun unpinReply(topicId: String): Topic = client.delete(
        path = "/topics/$topicId/pinned-reply",
        deserializer = Topic.serializer(),
    )

    suspend fun setBestAnswer(topicId: String, replyId: String): Topic = client.putJson(
        path = "/topics/$topicId/best-answer",
        body = KungalJson.encodeToString(PinReplyBody.serializer(), PinReplyBody(replyId)),
        deserializer = Topic.serializer(),
    )

    suspend fun clearBestAnswer(topicId: String): Topic = client.delete(
        path = "/topics/$topicId/best-answer",
        deserializer = Topic.serializer(),
    )

    // ---------- 楼中楼评论 ----------

    suspend fun createComment(
        replyId: String,
        text: String,
        parentCommentId: String? = null,
        idempotencyKey: String,
    ): ReplyComment = client.postJson(
        path = "/replies/$replyId/comments",
        body = KungalJson.encodeToString(
            CommentCreate.serializer(),
            CommentCreate(text = text, parentCommentId = parentCommentId),
        ),
        idempotencyKey = idempotencyKey,
        deserializer = ReplyComment.serializer(),
    )

    suspend fun comment(commentId: String): ReplyComment = client.get(
        path = "/comments/$commentId",
        deserializer = ReplyComment.serializer(),
    )

    suspend fun commentSource(commentId: String): com.kungal.core.model.CommentSource = client.get(
        path = "/comments/$commentId/source",
        deserializer = com.kungal.core.model.CommentSource.serializer(),
    )

    suspend fun updateComment(commentId: String, text: String): ReplyComment = client.patchJson(
        path = "/comments/$commentId",
        body = KungalJson.encodeToString(CommentPatch.serializer(), CommentPatch(text)),
        deserializer = ReplyComment.serializer(),
    )

    suspend fun deleteComment(commentId: String) {
        client.deleteNoBody("/comments/$commentId")
    }

    suspend fun likeComment(commentId: String, active: Boolean): ReplyComment = when (active) {
        true -> client.put("/comments/$commentId/like", ReplyComment.serializer())
        false -> client.delete("/comments/$commentId/like", ReplyComment.serializer())
    }

    // ---------- 反应（点赞/点踩 + 表情） ----------

    suspend fun react(topicId: String, reaction: String, active: Boolean): TopicEngagement = when (active) {
        true -> client.put("/topics/$topicId/reactions/$reaction", TopicEngagement.serializer())
        false -> client.delete("/topics/$topicId/reactions/$reaction", TopicEngagement.serializer())
    }

    suspend fun reactReply(replyId: String, reaction: String, active: Boolean): ReplyEngagement = when (active) {
        true -> client.put("/replies/$replyId/reactions/$reaction", ReplyEngagement.serializer())
        false -> client.delete("/replies/$replyId/reactions/$reaction", ReplyEngagement.serializer())
    }

    /** 反应历史：谁在什么时候点了什么（话题或回复共用）。 */
    suspend fun topicReactionHistory(
        topicId: String,
        cursor: String? = null,
        limit: Int = 30,
    ): PageList<ReactionRecord> = client.get(
        path = "/topics/$topicId/reactions",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(ReactionRecord.serializer()),
    )

    suspend fun replyReactionHistory(
        replyId: String,
        cursor: String? = null,
        limit: Int = 30,
    ): PageList<ReactionRecord> = client.get(
        path = "/replies/$replyId/reactions",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(ReactionRecord.serializer()),
    )

    // ---------- 收藏 / 订阅 / 推 / 隐藏 ----------

    suspend fun favorite(topicId: String, active: Boolean): TopicEngagement = when (active) {
        true -> client.put("/topics/$topicId/favorite", TopicEngagement.serializer())
        false -> client.delete("/topics/$topicId/favorite", TopicEngagement.serializer())
    }

    suspend fun subscription(topicId: String): TopicSubscription = client.get(
        path = "/topics/$topicId/subscription",
        deserializer = TopicSubscription.serializer(),
    )

    suspend fun setSubscription(topicId: String, level: String): TopicSubscription = client.putJson(
        path = "/topics/$topicId/subscription",
        body = KungalJson.encodeToString(SubscriptionSet.serializer(), SubscriptionSet(level)),
        deserializer = TopicSubscription.serializer(),
    )

    suspend fun markRead(topicId: String, floor: Int): TopicSubscription = client.putJson(
        path = "/topics/$topicId/subscription/read-marker",
        body = """{"floor":$floor}""",
        deserializer = TopicSubscription.serializer(),
    )

    suspend fun subscriptions(
        cursor: String? = null,
        limit: Int = 20,
        includeNsfw: Boolean = false,
        hasUnread: Boolean = false,
    ): PageList<SubscribedTopic> = client.get(
        path = "/me/topic-subscriptions",
        query = mapOf(
            "cursor" to cursor,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
            "has_unread" to hasUnread,
        ),
        deserializer = PageList.serializer(SubscribedTopic.serializer()),
    )

    suspend fun upvote(topicId: String, note: String?, idempotencyKey: String): TopicUpvote =
        client.postJson(
            path = "/topics/$topicId/upvotes",
            body = KungalJson.encodeToString(UpvoteCreate.serializer(), UpvoteCreate(note)),
            idempotencyKey = idempotencyKey,
            deserializer = TopicUpvote.serializer(),
        )

    suspend fun upvotes(
        topicId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): PageList<TopicUpvote> = client.get(
        path = "/topics/$topicId/upvotes",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(TopicUpvote.serializer()),
    )

    /** 批量读取本人对话题的收藏/反应状态（最多 100 个 id）。 */
    suspend fun states(topicIds: List<String>): TopicStates = client.get(
        path = "/me/topic-states",
        query = mapOf("topic_ids" to topicIds.joinToString(",")),
        deserializer = TopicStates.serializer(),
    )

    // ---------- 发帖 / 编辑 / 草稿 ----------

    suspend fun createTopic(payload: TopicCreate, idempotencyKey: String): Topic = client.postJson(
        path = "/topics",
        body = encode(payload),
        idempotencyKey = idempotencyKey,
        deserializer = Topic.serializer(),
    )

    suspend fun topicSource(topicId: String): TopicSource = client.get(
        path = "/topics/$topicId/source",
        deserializer = TopicSource.serializer(),
    )

    suspend fun updateTopic(topicId: String, payload: TopicPatch): Topic = client.patchJson(
        path = "/topics/$topicId",
        body = encode(payload),
        deserializer = Topic.serializer(),
    )

    suspend fun setTopicState(topicId: String, state: String): Topic = client.patchJson(
        path = "/topics/$topicId",
        body = encode(TopicPatch(state = state)),
        deserializer = Topic.serializer(),
    )

    suspend fun createDraft(payload: TopicDraftCreate, idempotencyKey: String): TopicDraft =
        client.postJson(
            path = "/me/topic-drafts",
            body = encode(payload),
            idempotencyKey = idempotencyKey,
            deserializer = TopicDraft.serializer(),
        )

    suspend fun drafts(cursor: String? = null, limit: Int = 20): PageList<TopicDraftSummary> =
        client.get(
            path = "/me/topic-drafts",
            query = mapOf("cursor" to cursor, "limit" to limit),
            deserializer = PageList.serializer(TopicDraftSummary.serializer()),
        )

    suspend fun draft(draftId: String): TopicDraft = client.get(
        path = "/me/topic-drafts/$draftId",
        deserializer = TopicDraft.serializer(),
    )

    suspend fun deleteDraft(draftId: String) {
        client.deleteNoBody("/me/topic-drafts/$draftId")
    }

    // ---------- 投票 ----------

    suspend fun polls(topicId: String): PageList<Poll> = client.get(
        path = "/topics/$topicId/polls",
        deserializer = PageList.serializer(Poll.serializer()),
    )

    suspend fun poll(pollId: String): Poll = client.get(
        path = "/polls/$pollId",
        deserializer = Poll.serializer(),
    )

    suspend fun createPoll(topicId: String, payload: PollCreate, idempotencyKey: String): Poll =
        client.postJson(
            path = "/topics/$topicId/polls",
            body = encode(payload),
            idempotencyKey = idempotencyKey,
            deserializer = Poll.serializer(),
        )

    suspend fun votePoll(pollId: String, optionIds: List<String>): Poll = client.putJson(
        path = "/polls/$pollId/vote",
        body = encode(PollVoteBody(optionIds)),
        deserializer = Poll.serializer(),
    )

    suspend fun clearPollVote(pollId: String): Poll = client.delete(
        path = "/polls/$pollId/vote",
        deserializer = Poll.serializer(),
    )

    suspend fun pollVotes(
        pollId: String,
        cursor: String? = null,
        limit: Int = 30,
    ): PageList<PollVote> = client.get(
        path = "/polls/$pollId/votes",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(PollVote.serializer()),
    )

    // ---------- 抽奖 ----------

    suspend fun lotteries(topicId: String, includeNsfw: Boolean = false): PageList<Lottery> =
        client.get(
            path = "/topics/$topicId/lotteries",
            query = mapOf("include_nsfw" to includeNsfw),
            deserializer = PageList.serializer(Lottery.serializer()),
        )

    suspend fun lottery(lotteryId: String, includeNsfw: Boolean = false): Lottery = client.get(
        path = "/lotteries/$lotteryId",
        query = mapOf("include_nsfw" to includeNsfw),
        deserializer = Lottery.serializer(),
    )

    suspend fun enterLottery(lotteryId: String): Lottery = client.put(
        path = "/lotteries/$lotteryId/entries/me",
        deserializer = Lottery.serializer(),
    )

    suspend fun withdrawLottery(lotteryId: String): Lottery = client.delete(
        path = "/lotteries/$lotteryId/entries/me",
        deserializer = Lottery.serializer(),
    )

    suspend fun lotteryEntries(
        lotteryId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): PageList<LotteryEntry> = client.get(
        path = "/lotteries/$lotteryId/entries",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(LotteryEntry.serializer()),
    )

    suspend fun revealLotteryCode(lotteryId: String): LotteryCodeReveal = client.postNoBodyJson(
        path = "/lotteries/$lotteryId/code-reveals",
        deserializer = LotteryCodeReveal.serializer(),
    )

    // ---------- 关注动态 ----------

    suspend fun followingActivities(
        cursor: String? = null,
        limit: Int = 20,
        includeNsfw: Boolean = false,
    ): PageList<FollowingActivityGroup> = client.get(
        path = "/me/following-activities",
        query = mapOf("cursor" to cursor, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PageList.serializer(FollowingActivityGroup.serializer()),
    )

    suspend fun followingSummary(includeNsfw: Boolean = false): FollowingActivitySummary =
        client.get(
            path = "/me/following-activities/summary",
            query = mapOf("include_nsfw" to includeNsfw),
            deserializer = FollowingActivitySummary.serializer(),
        )

    suspend fun markFollowingSeen(): FollowingReadMarker = client.putJson(
        path = "/me/following-activities/read-marker",
        body = "{}",
        deserializer = FollowingReadMarker.serializer(),
    )

    // ---------- 搜索（话题 / 回复 / 评论） ----------

    suspend fun searchTopics(
        query: String,
        page: Int = 1,
        limit: Int = 20,
        includeNsfw: Boolean = false,
    ): PagedList<Topic> = client.get(
        path = "/search/topics",
        query = mapOf("q" to query, "page" to page, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PagedList.serializer(Topic.serializer()),
    )

    suspend fun searchReplies(
        query: String,
        page: Int = 1,
        limit: Int = 20,
        includeNsfw: Boolean = false,
    ): PagedList<ReplySearchHit> = client.get(
        path = "/search/replies",
        query = mapOf("q" to query, "page" to page, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PagedList.serializer(ReplySearchHit.serializer()),
    )

    suspend fun searchComments(
        query: String,
        page: Int = 1,
        limit: Int = 20,
        includeNsfw: Boolean = false,
    ): PagedList<CommentSearchHit> = client.get(
        path = "/search/comments",
        query = mapOf("q" to query, "page" to page, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PagedList.serializer(CommentSearchHit.serializer()),
    )
}
