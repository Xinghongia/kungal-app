package com.kungal.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.api.ResourceListQuery
import com.kungal.core.api.ToolsetListQuery
import com.kungal.core.model.CommentSearchHit
import com.kungal.core.model.Engine
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.LocalizedText
import com.kungal.core.model.ReplySearchHit
import com.kungal.core.model.SeriesSummary
import com.kungal.core.model.ToolsetSummary
import com.kungal.core.model.Topic
import com.kungal.core.model.UserSearchHit
import com.kungal.core.model.WallCommentSearchHit
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

enum class SearchTab(val label: String) {
    ALL("全部"),
    WORKS("Galgame"),
    TOPICS("话题"),
    ENTITIES("资料库"),
    RESOURCES("Galgame 资源"),
    USERS("用户"),
    REPLIES("回复"),
    COMMENTS("评论"),
    WALL("游戏评论"),
    TOOLSETS("Gal 工具"),
}

enum class EntityFamily(val label: String) {
    ALL("全部"),
    CHARACTER("角色"),
    COMPANY("会社"),
    STAFF("Staff"),
    TAG("标签"),
    SERIES("系列"),
    ENGINE("引擎"),
}

data class EntityHit(
    val family: EntityFamily,
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val workCount: Int? = null,
)

data class EntityPick(val id: String, val name: String)

data class WorkFilters(
    val sort: String = "relevance_desc",
    val sortLabel: String = "相关度",
    val companyId: String? = null,
    val companyName: String? = null,
    val tags: List<EntityPick> = emptyList(),
    val releasedFrom: String? = null,
    val releasedTo: String? = null,
) {
    val isActive: Boolean
        get() = companyId != null || tags.isNotEmpty() || releasedFrom != null || releasedTo != null
}

data class Lane<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val nextCursor: String? = null,
)

data class Overview(
    val works: List<WorkSummary> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val entities: List<EntityHit> = emptyList(),
    val resources: List<GalgameResource> = emptyList(),
    val users: List<UserSearchHit> = emptyList(),
    val replies: List<ReplySearchHit> = emptyList(),
    val comments: List<CommentSearchHit> = emptyList(),
    val wallComments: List<WallCommentSearchHit> = emptyList(),
    val toolsets: List<ToolsetSummary> = emptyList(),
    val totals: Map<SearchTab, Int> = emptyMap(),
    val anySucceeded: Boolean = false,
)

/**
 * 全站搜索：与网页同一组接口。搜索结果按 tab 各自分页；
 * 「全部」并行拉各分类预览（数量与网页一致），tab 数量来自同一份预览。
 */
class SearchViewModel(
    private val app: KungalApp,
    private val includeNsfw: Boolean,
) : ViewModel() {

    data class State(
        val queryInput: String = "",
        val submittedQuery: String = "",
        val history: List<String> = emptyList(),
        val tab: SearchTab = SearchTab.ALL,
        val overview: Overview? = null,
        val overviewLoading: Boolean = false,
        val overviewError: String? = null,
        val totals: Map<SearchTab, Int> = emptyMap(),
        val works: Lane<WorkSummary> = Lane(),
        val topics: Lane<Topic> = Lane(),
        val replies: Lane<ReplySearchHit> = Lane(),
        val comments: Lane<CommentSearchHit> = Lane(),
        val users: Lane<UserSearchHit> = Lane(),
        val resources: Lane<GalgameResource> = Lane(),
        val toolsets: Lane<ToolsetSummary> = Lane(),
        val wallComments: Lane<WallCommentSearchHit> = Lane(),
        val entities: Lane<EntityHit> = Lane(),
        val entityFamily: EntityFamily = EntityFamily.ALL,
        val workFilters: WorkFilters = WorkFilters(),
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var blockedTagIds: List<String> = emptyList()
    private var blockedTagTrees: List<String> = emptyList()

    init {
        viewModelScope.launch {
            app.searchHistoryStore.flow.collect { history ->
                _state.update { it.copy(history = history) }
            }
        }
        viewModelScope.launch {
            runCatching { app.galgameApi.tagBlocks() }.onSuccess { blocks ->
                blockedTagIds = blocks.filter { it.mode != "tree" }.map { it.id }
                blockedTagTrees = blocks.filter { it.mode == "tree" }.map { it.id }
            }
        }
    }

    fun setQueryInput(value: String) = _state.update { it.copy(queryInput = value) }

    fun useHistory(query: String) {
        _state.update { it.copy(queryInput = query) }
        submit()
    }

    fun submit() {
        val q = _state.value.queryInput.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        _state.update {
            it.copy(
                submittedQuery = q,
                overview = null,
                overviewError = null,
                totals = emptyMap(),
                works = Lane(),
                topics = Lane(),
                replies = Lane(),
                comments = Lane(),
                users = Lane(),
                resources = Lane(),
                toolsets = Lane(),
                wallComments = Lane(),
                entities = Lane(),
                error = null,
            )
        }
        viewModelScope.launch { app.searchHistoryStore.add(q) }
        val tab = _state.value.tab
        searchJob = viewModelScope.launch {
            if (tab == SearchTab.ALL) {
                loadOverview(q)
            } else {
                supervisorScope {
                    launch { loadOverview(q) }
                    launch { loadTab(tab, q) }
                }
            }
        }
    }

    fun selectTab(tab: SearchTab) {
        if (tab == _state.value.tab) return
        _state.update { it.copy(tab = tab, error = null) }
        val q = _state.value.submittedQuery
        if (q.isEmpty()) return
        if (tab == SearchTab.ALL) {
            if (_state.value.overview == null) {
                searchJob = viewModelScope.launch { loadOverview(q) }
            }
        } else if (laneIsEmpty(tab)) {
            searchJob = viewModelScope.launch { loadTab(tab, q) }
        }
    }

    fun setEntityFamily(family: EntityFamily) {
        if (family == _state.value.entityFamily) return
        _state.update { it.copy(entityFamily = family, entities = Lane()) }
        val q = _state.value.submittedQuery
        if (q.isNotEmpty()) {
            searchJob = viewModelScope.launch { loadEntities(q, page = 1, append = false) }
        }
    }

    fun retry() {
        val q = _state.value.submittedQuery
        if (q.isEmpty()) return
        if (_state.value.tab == SearchTab.ALL) {
            searchJob = viewModelScope.launch { loadOverview(q) }
        } else {
            searchJob = viewModelScope.launch { loadTab(_state.value.tab, q) }
        }
    }

    /** 页码分页：打开某一页（替换当前 tab 的结果）。留言墙为游标接口，不支持页码。 */
    fun goToPage(target: Int) {
        val q = _state.value.submittedQuery
        if (q.isEmpty() || target < 1) return
        if (_state.value.tab == SearchTab.WALL) return
        searchJob = viewModelScope.launch {
            when (_state.value.tab) {
                SearchTab.WORKS -> if (_state.value.works.page != target) loadWorks(q, target, append = false)
                SearchTab.TOPICS -> if (_state.value.topics.page != target) loadTopics(q, target, false)
                SearchTab.REPLIES -> if (_state.value.replies.page != target) loadReplies(q, target, false)
                SearchTab.COMMENTS -> if (_state.value.comments.page != target) loadComments(q, target, false)
                SearchTab.USERS -> if (_state.value.users.page != target) loadUsers(q, target, false)
                SearchTab.RESOURCES -> if (_state.value.resources.page != target) loadResources(q, target, false)
                SearchTab.TOOLSETS -> if (_state.value.toolsets.page != target) loadToolsets(q, target, false)
                SearchTab.ENTITIES -> if (_state.value.entities.page != target) loadEntities(q, target, append = false)
                SearchTab.ALL, SearchTab.WALL -> Unit
            }
        }
    }

    fun loadMore() {
        val q = _state.value.submittedQuery
        if (q.isEmpty()) return
        when (_state.value.tab) {
            SearchTab.WORKS -> if (_state.value.works.hasMore) {
                searchJob = viewModelScope.launch {
                    loadWorks(q, _state.value.works.page + 1, append = true)
                }
            }
            SearchTab.TOPICS -> if (_state.value.topics.hasMore) {
                searchJob = viewModelScope.launch { loadTopics(q, _state.value.topics.page + 1, true) }
            }
            SearchTab.REPLIES -> if (_state.value.replies.hasMore) {
                searchJob = viewModelScope.launch { loadReplies(q, _state.value.replies.page + 1, true) }
            }
            SearchTab.COMMENTS -> if (_state.value.comments.hasMore) {
                searchJob = viewModelScope.launch { loadComments(q, _state.value.comments.page + 1, true) }
            }
            SearchTab.USERS -> if (_state.value.users.hasMore) {
                searchJob = viewModelScope.launch { loadUsers(q, _state.value.users.page + 1, true) }
            }
            SearchTab.RESOURCES -> if (_state.value.resources.hasMore) {
                searchJob = viewModelScope.launch { loadResources(q, _state.value.resources.page + 1, true) }
            }
            SearchTab.TOOLSETS -> if (_state.value.toolsets.hasMore) {
                searchJob = viewModelScope.launch { loadToolsets(q, _state.value.toolsets.page + 1, true) }
            }
            SearchTab.WALL -> if (_state.value.wallComments.hasMore) {
                searchJob = viewModelScope.launch {
                    loadWallComments(q, _state.value.wallComments.nextCursor, true)
                }
            }
            SearchTab.ENTITIES -> if (_state.value.entities.hasMore) {
                searchJob = viewModelScope.launch {
                    loadEntities(q, _state.value.entities.page + 1, append = true)
                }
            }
            SearchTab.ALL -> Unit
        }
    }

    // ---------- 作品筛选 ----------

    fun setWorkSort(sort: String, label: String) = updateWorkFilters {
        it.copy(sort = sort, sortLabel = label)
    }

    fun setWorkCompany(company: EntityPick?) = updateWorkFilters {
        it.copy(companyId = company?.id, companyName = company?.name)
    }

    fun toggleWorkTag(tag: EntityPick) = updateWorkFilters { current ->
        val exists = current.tags.any { it.id == tag.id }
        when {
            exists -> current.copy(tags = current.tags.filterNot { it.id == tag.id })
            current.tags.size >= 10 -> current
            else -> current.copy(tags = current.tags + tag)
        }
    }

    fun setWorkYears(from: String?, to: String?) = updateWorkFilters {
        it.copy(
            releasedFrom = from?.takeIf { v -> v.isNotBlank() },
            releasedTo = to?.takeIf { v -> v.isNotBlank() },
        )
    }

    fun clearWorkFilters() = updateWorkFilters {
        WorkFilters(sort = it.sort, sortLabel = it.sortLabel)
    }

    private fun updateWorkFilters(transform: (WorkFilters) -> WorkFilters) {
        _state.update { it.copy(workFilters = transform(it.workFilters)) }
        val q = _state.value.submittedQuery
        if (q.isNotEmpty()) {
            searchJob = viewModelScope.launch { loadWorks(q, page = 1, append = false) }
        }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch { app.searchHistoryStore.remove(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { app.searchHistoryStore.clear() }
    }

    // ---------- 拉取 ----------

    private suspend fun loadOverview(q: String) {
        _state.update { it.copy(overviewLoading = true, overviewError = null) }
        supervisorScope {
            val topics = async { runCatching { app.topicApi.searchTopics(q, 1, 8, includeNsfw) } }
            val works = async {
                runCatching {
                    app.searchApi.works(
                        q = q, limit = 12, includeNsfw = includeNsfw,
                        excludeTagIds = blockedTagIds, excludeTagTrees = blockedTagTrees,
                    )
                }
            }
            val users = async { runCatching { app.searchApi.users(q, limit = 8) } }
            val replies = async { runCatching { app.topicApi.searchReplies(q, 1, 4, includeNsfw) } }
            val comments = async { runCatching { app.topicApi.searchComments(q, 1, 4, includeNsfw) } }
            val resources = async {
                runCatching {
                    app.resourceApi.list(
                        ResourceListQuery(q = q, page = 1, limit = 6, includeNsfw = includeNsfw),
                    )
                }
            }
            val toolsets = async {
                runCatching { app.toolsetApi.list(ToolsetListQuery(q = q, page = 1, limit = 4)) }
            }
            val walls = async {
                if (q.length < 2) null
                else runCatching { app.searchApi.wallComments(q, limit = 4) }
            }
            val entityResults = async { fetchEntityFamilies(q, page = 1, limit = 8) }

            val topicPage = topics.await().getOrNull()
            val workPage = works.await().getOrNull()
            val userPage = users.await().getOrNull()
            val replyPage = replies.await().getOrNull()
            val commentPage = comments.await().getOrNull()
            val resourcePage = resources.await().getOrNull()
            val toolsetPage = toolsets.await().getOrNull()
            val wallPage = walls.await()?.getOrNull()
            val entityGroups = entityResults.await()

            val totals = buildMap {
                topicPage?.let { put(SearchTab.TOPICS, it.total) }
                workPage?.let { put(SearchTab.WORKS, it.total) }
                userPage?.let { put(SearchTab.USERS, it.total) }
                replyPage?.let { put(SearchTab.REPLIES, it.total) }
                commentPage?.let { put(SearchTab.COMMENTS, it.total) }
                resourcePage?.let { put(SearchTab.RESOURCES, it.total) }
                toolsetPage?.let { put(SearchTab.TOOLSETS, it.total) }
                val entityTotal = entityGroups.filter { it.first }.sumOf { it.second.second }
                if (entityGroups.any { it.first }) put(SearchTab.ENTITIES, entityTotal)
            }
            val anySucceeded = listOf(
                topicPage, workPage, userPage, replyPage, commentPage,
                resourcePage, toolsetPage, wallPage,
            ).any { it != null } || entityGroups.any { it.first }

            _state.update {
                it.copy(
                    overviewLoading = false,
                    overviewError = if (anySucceeded) null else "搜索没能完成，请稍后重试",
                    overview = Overview(
                        works = workPage?.items.orEmpty(),
                        topics = topicPage?.items.orEmpty(),
                        entities = entityGroups.flatMap { group -> group.second.first },
                        resources = resourcePage?.items.orEmpty(),
                        users = userPage?.items.orEmpty(),
                        replies = replyPage?.items.orEmpty(),
                        comments = commentPage?.items.orEmpty(),
                        wallComments = wallPage?.items.orEmpty(),
                        toolsets = toolsetPage?.items.orEmpty(),
                        totals = totals,
                        anySucceeded = anySucceeded,
                    ),
                    totals = totals,
                )
            }
        }
    }

    private suspend fun loadTab(tab: SearchTab, q: String) {
        when (tab) {
            SearchTab.WORKS -> loadWorks(q, 1, false)
            SearchTab.TOPICS -> loadTopics(q, 1, false)
            SearchTab.REPLIES -> loadReplies(q, 1, false)
            SearchTab.COMMENTS -> loadComments(q, 1, false)
            SearchTab.USERS -> loadUsers(q, 1, false)
            SearchTab.RESOURCES -> loadResources(q, 1, false)
            SearchTab.TOOLSETS -> loadToolsets(q, 1, false)
            SearchTab.WALL -> loadWallComments(q, null, false)
            SearchTab.ENTITIES -> loadEntities(q, 1, false)
            SearchTab.ALL -> Unit
        }
    }

    private fun setLaneLoading() = _state.update { it.copy(error = null) }

    private suspend fun loadWorks(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(works = it.works.copy(loading = true)) }
        runCatching {
            val f = _state.value.workFilters
            app.searchApi.works(
                q = q, page = page, limit = 24, sort = f.sort,
                companyId = f.companyId,
                tagIds = f.tags.map { it.id },
                releasedFrom = f.releasedFrom,
                releasedTo = f.releasedTo,
                includeNsfw = includeNsfw,
                excludeTagIds = blockedTagIds,
                excludeTagTrees = blockedTagTrees,
            )
        }.onSuccess { data ->
            _state.update {
                it.copy(
                    works = Lane(
                        items = if (append) it.works.items + data.items else data.items,
                        total = data.total,
                        page = page,
                        hasMore = page * 24 < data.total,
                    ),
                )
            }
        }.onFailure { e ->
            _state.update { it.copy(works = it.works.copy(loading = false), error = friendlyError(e)) }
        }
    }

    private suspend fun loadTopics(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(topics = it.topics.copy(loading = true)) }
        runCatching { app.topicApi.searchTopics(q, page, 24, includeNsfw) }
            .onSuccess { data ->
                _state.update { it.copy(topics = nextLane(it.topics, data.items, data.total, page, append)) }
            }
            .onFailure { e ->
                _state.update { it.copy(topics = it.topics.copy(loading = false), error = friendlyError(e)) }
            }
    }

    private suspend fun loadReplies(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(replies = it.replies.copy(loading = true)) }
        runCatching { app.topicApi.searchReplies(q, page, 24, includeNsfw) }
            .onSuccess { data ->
                _state.update { it.copy(replies = nextLane(it.replies, data.items, data.total, page, append)) }
            }
            .onFailure { e ->
                _state.update { it.copy(replies = it.replies.copy(loading = false), error = friendlyError(e)) }
            }
    }

    private suspend fun loadComments(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(comments = it.comments.copy(loading = true)) }
        runCatching { app.topicApi.searchComments(q, page, 24, includeNsfw) }
            .onSuccess { data ->
                _state.update { it.copy(comments = nextLane(it.comments, data.items, data.total, page, append)) }
            }
            .onFailure { e ->
                _state.update { it.copy(comments = it.comments.copy(loading = false), error = friendlyError(e)) }
            }
    }

    private suspend fun loadUsers(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(users = it.users.copy(loading = true)) }
        runCatching { app.searchApi.users(q, page, 24) }
            .onSuccess { data ->
                _state.update { it.copy(users = nextLane(it.users, data.items, data.total, page, append)) }
            }
            .onFailure { e ->
                _state.update { it.copy(users = it.users.copy(loading = false), error = friendlyError(e)) }
            }
    }

    private suspend fun loadResources(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(resources = it.resources.copy(loading = true)) }
        runCatching {
            app.resourceApi.list(ResourceListQuery(q = q, page = page, limit = 24, includeNsfw = includeNsfw))
        }.onSuccess { data ->
            _state.update { it.copy(resources = nextLane(it.resources, data.items, data.total, page, append)) }
        }.onFailure { e ->
            _state.update { it.copy(resources = it.resources.copy(loading = false), error = friendlyError(e)) }
        }
    }

    private suspend fun loadToolsets(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(toolsets = it.toolsets.copy(loading = true)) }
        runCatching { app.toolsetApi.list(ToolsetListQuery(q = q, page = page, limit = 24)) }
            .onSuccess { data ->
                _state.update { it.copy(toolsets = nextLane(it.toolsets, data.items, data.total, page, append)) }
            }
            .onFailure { e ->
                _state.update { it.copy(toolsets = it.toolsets.copy(loading = false), error = friendlyError(e)) }
            }
    }

    private suspend fun loadWallComments(q: String, cursor: String?, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(wallComments = it.wallComments.copy(loading = true)) }
        runCatching { app.searchApi.wallComments(q, cursor = cursor, limit = 24) }
            .onSuccess { data ->
                _state.update {
                    val items = if (append) it.wallComments.items + data.items else data.items
                    it.copy(
                        wallComments = Lane(
                            items = items,
                            total = items.size,
                            page = if (append) it.wallComments.page + 1 else 1,
                            hasMore = data.nextCursor != null,
                            nextCursor = data.nextCursor,
                        ),
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(wallComments = it.wallComments.copy(loading = false), error = friendlyError(e))
                }
            }
    }

    private suspend fun loadEntities(q: String, page: Int, append: Boolean) {
        setLaneLoading()
        _state.update { it.copy(entities = it.entities.copy(loading = true)) }
        val family = _state.value.entityFamily
        if (family == EntityFamily.ALL) {
            val groups = fetchEntityFamilies(q, page = 1, limit = 8)
            val hits = groups.flatMap { it.second.first }
            val total = groups.filter { it.first }.sumOf { it.second.second }
            _state.update {
                it.copy(
                    entities = Lane(items = hits, total = total, page = 1, hasMore = false),
                    totals = it.totals + (SearchTab.ENTITIES to total),
                )
            }
        } else {
            val result = fetchEntityFamily(family, q, page = page, limit = 24)
            if (result == null) {
                _state.update {
                    it.copy(entities = it.entities.copy(loading = false), error = "资料库搜索没能完成，请稍后重试")
                }
            } else {
                val (hits, total) = result
                _state.update {
                    it.copy(
                        entities = Lane(
                            items = if (append) it.entities.items + hits else hits,
                            total = total,
                            page = page,
                            hasMore = page * 24 < total,
                        ),
                    )
                }
            }
        }
    }

    private fun <T> nextLane(
        lane: Lane<T>,
        items: List<T>,
        total: Int,
        page: Int,
        append: Boolean,
    ): Lane<T> = Lane(
        items = if (append) lane.items + items else items,
        total = total,
        page = page,
        hasMore = page * 24 < total,
    )

    private fun laneIsEmpty(tab: SearchTab): Boolean = when (tab) {
        SearchTab.WORKS -> _state.value.works.page == 0
        SearchTab.TOPICS -> _state.value.topics.page == 0
        SearchTab.REPLIES -> _state.value.replies.page == 0
        SearchTab.COMMENTS -> _state.value.comments.page == 0
        SearchTab.USERS -> _state.value.users.page == 0
        SearchTab.RESOURCES -> _state.value.resources.page == 0
        SearchTab.TOOLSETS -> _state.value.toolsets.page == 0
        SearchTab.WALL -> _state.value.wallComments.page == 0
        SearchTab.ENTITIES -> _state.value.entities.page == 0
        SearchTab.ALL -> false
    }

    /** 六类实体并行搜索；返回 (是否成功, (命中的展示项, total))。 */
    private suspend fun fetchEntityFamilies(
        q: String,
        page: Int,
        limit: Int,
    ): List<Pair<Boolean, Pair<List<EntityHit>, Int>>> = supervisorScope {
        val calls = listOf(
            async {
                runCatching { app.entityApi.characters(q = q, page = page, limit = limit, includeNsfw = includeNsfw) }
                    .map { p ->
                        p.items.map {
                            EntityHit(
                                family = EntityFamily.CHARACTER,
                                id = it.id,
                                name = catalogName(it.displayName, it.localized, it.id),
                                imageUrl = it.image?.url,
                                workCount = it.catalogWorkCount,
                            )
                        } to p.total
                    }
            },
            async {
                runCatching { app.entityApi.companies(q = q, page = page, limit = limit) }
                    .map { p ->
                        p.items.map {
                            EntityHit(
                                family = EntityFamily.COMPANY,
                                id = it.id,
                                name = catalogName(it.displayName, it.localized, it.id),
                                imageUrl = it.logo?.url,
                                workCount = it.catalogWorkCount,
                            )
                        } to p.total
                    }
            },
            async {
                runCatching { app.entityApi.creditNames(q = q, page = page, limit = limit) }
                    .map { p ->
                        p.items.map {
                            EntityHit(
                                family = EntityFamily.STAFF,
                                id = it.id,
                                name = catalogName(it.displayName, it.localized, it.id),
                            )
                        } to p.total
                    }
            },
            async {
                runCatching {
                    app.entityApi.tags(q = q, page = page, limit = limit, includeNsfw = includeNsfw)
                }.map { p ->
                    p.items.map {
                        EntityHit(
                            family = EntityFamily.TAG,
                            id = it.id,
                            name = catalogName(it.displayName, it.localized, it.id),
                            workCount = it.catalogWorkCount,
                        )
                    } to p.total
                }
            },
            async {
                runCatching {
                    app.entityApi.seriesList(q = q, page = page, limit = limit, includeNsfw = includeNsfw)
                }.map { p ->
                    p.items.map { series ->
                        EntityHit(
                            family = EntityFamily.SERIES,
                            id = series.id,
                            name = catalogName(series.displayName, series.localized, series.id),
                            workCount = series.catalogWorkCount,
                        )
                    } to p.total
                }
            },
            async {
                runCatching { app.entityApi.engines(q = q, page = page, limit = limit) }
                    .map { p ->
                        p.items.map { engine ->
                            EntityHit(
                                family = EntityFamily.ENGINE,
                                id = engine.id,
                                name = catalogName(engine.displayName, engine.localized, engine.id),
                                workCount = engine.catalogWorkCount,
                            )
                        } to p.total
                    }
            },
        )
        calls.map { call ->
            call.await().fold(
                onSuccess = { true to it },
                onFailure = { false to (emptyList<EntityHit>() to 0) },
            )
        }
    }

    private suspend fun fetchEntityFamily(
        family: EntityFamily,
        q: String,
        page: Int,
        limit: Int,
    ): Pair<List<EntityHit>, Int>? {
        val result = runCatching {
            when (family) {
                EntityFamily.CHARACTER -> {
                    val p = app.entityApi.characters(q = q, page = page, limit = limit, includeNsfw = includeNsfw)
                    p.items.map {
                        EntityHit(
                            EntityFamily.CHARACTER, it.id,
                            catalogName(it.displayName, it.localized, it.id),
                            it.image?.url, it.catalogWorkCount,
                        )
                    } to p.total
                }
                EntityFamily.COMPANY -> {
                    val p = app.entityApi.companies(q = q, page = page, limit = limit)
                    p.items.map {
                        EntityHit(
                            EntityFamily.COMPANY, it.id,
                            catalogName(it.displayName, it.localized, it.id),
                            it.logo?.url, it.catalogWorkCount,
                        )
                    } to p.total
                }
                EntityFamily.STAFF -> {
                    val p = app.entityApi.creditNames(q = q, page = page, limit = limit)
                    p.items.map {
                        EntityHit(
                            EntityFamily.STAFF, it.id,
                            catalogName(it.displayName, it.localized, it.id),
                        )
                    } to p.total
                }
                EntityFamily.TAG -> {
                    val p = app.entityApi.tags(q = q, page = page, limit = limit, includeNsfw = includeNsfw)
                    p.items.map {
                        EntityHit(
                            EntityFamily.TAG, it.id,
                            catalogName(it.displayName, it.localized, it.id),
                            null, it.catalogWorkCount,
                        )
                    } to p.total
                }
                EntityFamily.SERIES -> {
                    val p = app.entityApi.seriesList(q = q, page = page, limit = limit, includeNsfw = includeNsfw)
                    p.items.map { series: SeriesSummary ->
                        EntityHit(
                            EntityFamily.SERIES, series.id,
                            catalogName(series.displayName, series.localized, series.id),
                            null, series.catalogWorkCount,
                        )
                    } to p.total
                }
                EntityFamily.ENGINE -> {
                    val p = app.entityApi.engines(q = q, page = page, limit = limit)
                    p.items.map { engine: Engine ->
                        EntityHit(
                            EntityFamily.ENGINE, engine.id,
                            catalogName(engine.displayName, engine.localized, engine.id),
                            null, engine.catalogWorkCount,
                        )
                    } to p.total
                }
                EntityFamily.ALL -> emptyList<EntityHit>() to 0
            }
        }
        return result.getOrNull()
    }

    private fun catalogName(
        displayName: String,
        localized: Map<String, LocalizedText>,
        id: String,
    ): String = localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: displayName.ifBlank { "#$id" }
}
