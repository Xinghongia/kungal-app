package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.MoePointBadge
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.content.LocalTextSelectionClearSignal
import com.kungal.app.ui.content.contentPlainText
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.Lottery
import com.kungal.core.model.Poll
import com.kungal.core.model.Reply
import com.kungal.core.model.Topic
import com.kungal.core.model.TopicViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    app: KungalApp,
    topicId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onEditTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val vm: TopicDetailViewModel = viewModel(key = "topic-$topicId") { TopicDetailViewModel(app, topicId) }
    val state by vm.state.collectAsState()
    val session by app.session.collectAsState()
    val currentUserId by app.currentUserId.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuOpen by remember { mutableStateOf(false) }
    var showSubscribe by remember { mutableStateOf(false) }
    var showUpvote by remember { mutableStateOf(false) }
    var hideConfirm by remember { mutableStateOf(false) }
    var deleteReplyTarget by remember { mutableStateOf<String?>(null) }
    var deleteCommentTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var nsfwRevealed by remember(topicId) { mutableStateOf(false) }
    var selectionClearSignal by remember { mutableIntStateOf(0) }
    val miniAppScope = rememberCoroutineScope()
    var pollFormOpen by remember { mutableStateOf(false) }
    var pollToEdit by remember { mutableStateOf<Poll?>(null) }
    var lotteryFormOpen by remember { mutableStateOf(false) }
    var lotteryToEdit by remember { mutableStateOf<Lottery?>(null) }
    var deletePollTarget by remember { mutableStateOf<Poll?>(null) }
    var deleteLotteryTarget by remember { mutableStateOf<Lottery?>(null) }
    var drawLotteryTarget by remember { mutableStateOf<Lottery?>(null) }
    var cancelLotteryTarget by remember { mutableStateOf<Lottery?>(null) }

    // 回复面板：只有点击「回复」/「写回复」时才弹出（与网站一致）
    var replySheetOpen by remember { mutableStateOf(false) }
    var quoteReply by remember { mutableStateOf<Reply?>(null) }
    var sheetText by remember { mutableStateOf("") }

    // 举报弹窗
    var reportTarget by remember { mutableStateOf<Reply?>(null) }
    var reportReasonKey by remember { mutableStateOf<String?>(null) }
    var reportNote by remember { mutableStateOf("") }

    val topic = state.topic
    val nsfwGated = topic?.isNsfw == true && nsfwDisplay == NsfwDisplayMode.HIDE && !nsfwRevealed
    val loggedIn = session != null
    val isTopicAdmin = topic?.let {
        it.author.id == currentUserId || it.viewer?.canEdit == true
    } == true

    fun openReplySheet(target: Reply?) {
        quoteReply = target
        sheetText = ""
        replySheetOpen = true
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    LaunchedEffect(nsfwDisplay) { vm.reloadLotteries() }

    // 回复触底加载
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 2
        }.collect { nearEnd -> if (nearEnd) vm.loadReplies() }
    }

    if (showSubscribe) {
        SubscriptionDialog(
            current = state.subscription?.notificationLevel ?: "normal",
            onSelect = {
                showSubscribe = false
                vm.setSubscription(it)
            },
            onDismiss = { showSubscribe = false },
        )
    }
    if (showUpvote) {
        UpvoteDialog(
            onConfirm = { note ->
                showUpvote = false
                vm.upvote(note) {}
            },
            onDismiss = { showUpvote = false },
        )
    }
    if (hideConfirm) {
        AlertDialog(
            onDismissRequest = { hideConfirm = false },
            title = { Text("隐藏话题") },
            text = { Text("隐藏后话题会从列表与搜索中消失，你自己仍可通过链接访问。确定隐藏吗？") },
            confirmButton = {
                TextButton(onClick = { hideConfirm = false; vm.setHidden(true) }) { Text("隐藏") }
            },
            dismissButton = { TextButton(onClick = { hideConfirm = false }) { Text("取消") } },
        )
    }
    if (pollFormOpen) {
        PollFormDialog(
            initial = pollToEdit,
            onDismiss = { pollFormOpen = false; pollToEdit = null },
            onCreate = { payload -> vm.createPoll(payload) },
            onUpdate = { pollId, patch -> vm.updatePoll(pollId, patch) },
        )
    }
    if (lotteryFormOpen) {
        LotteryFormDialog(
            app = app,
            initial = lotteryToEdit,
            onDismiss = { lotteryFormOpen = false; lotteryToEdit = null },
            onCreate = { payload -> vm.createLottery(payload) },
            onUpdate = { lotteryId, patch -> vm.updateLottery(lotteryId, patch) },
        )
    }
    deletePollTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deletePollTarget = null },
            title = { Text("删除投票") },
            text = { Text("删除投票后，所有投票数据都将丢失，该操作不可恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    deletePollTarget = null
                    miniAppScope.launch { vm.deletePoll(target.id) }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletePollTarget = null }) { Text("取消") } },
        )
    }
    drawLotteryTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { drawLotteryTarget = null },
            title = { Text("立即开奖") },
            text = { Text("开奖后中奖名单立即固定，无法撤销，也不能再修改奖项。确定现在开奖吗？") },
            confirmButton = {
                TextButton(onClick = {
                    drawLotteryTarget = null
                    miniAppScope.launch { vm.drawLottery(target.id) }
                }) { Text("开奖") }
            },
            dismissButton = { TextButton(onClick = { drawLotteryTarget = null }) { Text("取消") } },
        )
    }
    cancelLotteryTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelLotteryTarget = null },
            title = { Text("取消抽奖") },
            text = { Text("取消后将不再开奖，已参与的用户不会获得任何奖品。萌萌点奖池会全额退回给发起人。") },
            confirmButton = {
                TextButton(onClick = {
                    cancelLotteryTarget = null
                    miniAppScope.launch { vm.cancelLottery(target.id) }
                }) { Text("取消抽奖") }
            },
            dismissButton = { TextButton(onClick = { cancelLotteryTarget = null }) { Text("再想想") } },
        )
    }
    deleteLotteryTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteLotteryTarget = null },
            title = { Text("删除抽奖") },
            text = { Text("删除后所有参与记录、中奖名单与托管的兑换码都会一并丢失，该操作不可恢复！未开奖的萌萌点奖池会退回给发起人。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteLotteryTarget = null
                    miniAppScope.launch { vm.deleteLottery(target.id) }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteLotteryTarget = null }) { Text("取消") } },
        )
    }
    state.editingReply?.let { target ->
        EditDialog(
            title = "编辑回复",
            value = target.text,
            saving = state.replySaving,
            onChange = vm::updateEditingReply,
            onConfirm = vm::submitEditReply,
            onDismiss = vm::cancelEditReply,
        )
    }
    state.editingComment?.let { target ->
        EditDialog(
            title = "编辑评论",
            value = target.text,
            saving = state.commentSaving,
            onChange = vm::updateEditingComment,
            onConfirm = vm::submitEditComment,
            onDismiss = vm::cancelEditComment,
        )
    }

    // 举报弹窗（原因从站点 /report-reasons 加载）
    reportTarget?.let { reply ->
        LaunchedEffect(reply.id) { vm.loadReportReasons() }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("举报回复") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.reportReasons.isEmpty()) {
                        Text(
                            "正在加载举报原因…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.reportReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { reportReasonKey = reason.key }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = reportReasonKey == reason.key,
                                onClick = { reportReasonKey = reason.key },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(reason.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportNote,
                        onValueChange = { reportNote = it.take(1000) },
                        placeholder = { Text("补充说明（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reportReasonKey != null && !state.reporting,
                    onClick = {
                        val reason = reportReasonKey ?: return@TextButton
                        vm.reportReply(reply, reason, reportNote, contentPlainText(reply.content))
                        reportTarget = null
                        reportReasonKey = null
                        reportNote = ""
                    },
                ) { Text(if (state.reporting) "提交中…" else "举报") }
            },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("取消") } },
        )
    }

    // 底部回复面板（点「回复」或「写回复」才出现）
    if (replySheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { replySheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            ReplySheet(
                quote = quoteReply,
                text = sheetText,
                sending = state.replySending,
                onTextChange = { sheetText = it },
                onClearQuote = { quoteReply = null },
                onDismiss = { replySheetOpen = false },
                onSend = {
                    val body = sheetText.trim()
                    if (body.isNotEmpty()) {
                        val prefixed = quoteReply?.let {
                            val name = it.author.name ?: it.author.id
                            "> 回复 #${it.floor} @$name\n\n$body"
                        } ?: body
                        vm.sendReply(prefixed)
                        replySheetOpen = false
                    }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("话题", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (topic?.viewer?.canEdit == true) {
                        IconButton(onClick = { onEditTopic(topicId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "编辑话题")
                        }
                    }
                    if (topic?.viewer?.canHide == true || topic?.viewer?.canUnhide == true) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                if (topic?.viewer?.canHide == true) {
                                    DropdownMenuItem(
                                        text = { Text("隐藏话题") },
                                        onClick = { menuOpen = false; hideConfirm = true },
                                    )
                                }
                                if (topic?.viewer?.canUnhide == true) {
                                    DropdownMenuItem(
                                        text = { Text("恢复话题") },
                                        onClick = { menuOpen = false; vm.setHidden(false) },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading && topic == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            topic == null -> ErrorBox(
                state.error ?: "加载失败",
                onRetry = { vm.load() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            nsfwGated -> NsfwGate(
                onReveal = { nsfwRevealed = true },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> CompositionLocalProvider(LocalTextSelectionClearSignal provides selectionClearSignal) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // 点击正文/评论之外的空白区域时取消已选中的文字
                    .pointerInput(Unit) { detectTapGestures { selectionClearSignal++ } },
            ) {
                item { TopicHeader(topic, onOpenUser = onOpenUser) }
                item {
                    ContentDocumentView(
                        document = topic.content,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReactionChips(
                            reactions = topic.reactions,
                            busy = state.reactionBusy,
                            enabled = true,
                            onToggle = {
                                if (!loggedIn) onRequireLogin() else vm.toggleReaction(it)
                            },
                        )
                        ReactionPickerButton(
                            reactions = topic.reactions,
                            busy = state.reactionBusy,
                            subjectLabel = "话题",
                            enabled = true,
                            onToggle = {
                                if (!loggedIn) onRequireLogin() else vm.toggleReaction(it)
                            },
                            onLoadHistory = { app.topicApi.topicReactionHistory(topicId).items },
                        )
                    }
                }
                item {
                    TopicActionBar(
                        topic = topic,
                        subscriptionLevel = state.subscription?.notificationLevel ?: "normal",
                        busy = state.actionBusy,
                        loggedIn = loggedIn,
                        onReply = { if (!loggedIn) onRequireLogin() else openReplySheet(null) },
                        onFavorite = { if (!loggedIn) onRequireLogin() else vm.toggleFavorite() },
                        onSubscribe = { if (!loggedIn) onRequireLogin() else showSubscribe = true },
                        onUpvote = { if (!loggedIn) onRequireLogin() else showUpvote = true },
                    )
                }
                if (state.polls.isNotEmpty()) {
                    item {
                        Text(
                            "投票",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.polls, key = { "poll-${it.id}" }) { poll ->
                        PollCard(
                            poll = poll,
                            onVote = { ids ->
                                if (!loggedIn) onRequireLogin() else vm.votePoll(poll.id, ids)
                            },
                            onClearVote = { vm.clearPollVote(poll.id) },
                            onShowVotes = { app.topicApi.pollVotes(poll.id).items },
                            onEdit = { pollToEdit = poll; pollFormOpen = true },
                            onDelete = { deletePollTarget = poll },
                        )
                    }
                }
                if (state.lotteries.isNotEmpty()) {
                    item {
                        Text(
                            "抽奖",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.lotteries, key = { "lottery-${it.id}" }) { lottery ->
                        LotteryCard(
                            lottery = lottery,
                            onEnter = { if (!loggedIn) onRequireLogin() else vm.enterLottery(lottery.id) },
                            onWithdraw = { vm.withdrawLottery(lottery.id) },
                            loadEntries = { app.topicApi.lotteryEntries(lottery.id).items },
                            onRevealCode = { app.topicApi.revealLotteryCode(lottery.id).code },
                            onEdit = { lotteryToEdit = lottery; lotteryFormOpen = true },
                            onDelete = { deleteLotteryTarget = lottery },
                            onDraw = { drawLotteryTarget = lottery },
                            onCancel = { cancelLotteryTarget = lottery },
                            onSetFulfillment = { winnerId, fulfillment ->
                                vm.setFulfillment(lottery.id, winnerId, fulfillment)
                            },
                            nsfwDisplay = nsfwDisplay,
                        )
                    }
                }
                if (isTopicAdmin) {
                    item {
                        TopicMiniAppPanel(
                            onCreatePoll = {
                                pollToEdit = null
                                pollFormOpen = true
                            },
                            onCreateLottery = {
                                lotteryToEdit = null
                                lotteryFormOpen = true
                            },
                        )
                    }
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "回复（${topic.replyCount}）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                vm.setReplySort(
                                    if (state.replySort == "floor_asc") "floor_desc" else "floor_asc",
                                )
                            },
                        ) {
                            Text(
                                if (state.replySort == "floor_asc") "正序" else "倒序",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                items(state.replies, key = { it.id }) { reply ->
                    ReplyCard(
                        reply = reply,
                        busy = state.replyBusyId == reply.id,
                        loggedIn = loggedIn,
                        isSelf = currentUserId != null && currentUserId == reply.author.id,
                        canPinReply = topic.viewer?.canPinReply == true,
                        canSetBestAnswer = topic.viewer?.canSetBestAnswer == true,
                        commentSending = state.commentSendingReplyId == reply.id,
                        followState = state.followStates[reply.author.id],
                        onLoadFollowState = { vm.loadFollowState(reply.author.id) },
                        onToggleFollow = { vm.toggleFollow(reply.author.id) },
                        onToggleReaction = { vm.toggleReplyReaction(reply.id, it) },
                        onLoadHistory = { app.topicApi.replyReactionHistory(reply.id).items },
                        onOpenAuthor = { onOpenUser(reply.author.id) },
                        onQuote = { openReplySheet(reply) },
                        onShare = {
                            AndroidActions.shareText(
                                context,
                                "${topic.title}: https://www.kungal.com/topic/$topicId?reply=${reply.floor}",
                            )
                        },
                        onReport = {
                            reportTarget = reply
                            reportReasonKey = null
                            reportNote = ""
                        },
                        onEdit = { vm.startEditReply(reply.id) },
                        onDelete = { deleteReplyTarget = reply.id },
                        onPin = { vm.pinReply(reply.id) },
                        onUnpin = { vm.unpinReply() },
                        onSetBestAnswer = { vm.setBestAnswer(reply.id) },
                        onClearBestAnswer = { vm.clearBestAnswer() },
                        onSendComment = { text, parent -> vm.sendComment(reply.id, text, parent) },
                        onToggleCommentLike = { vm.toggleCommentLike(reply.id, it) },
                        onEditComment = { vm.startEditComment(it) },
                        onDeleteComment = { deleteCommentTarget = reply.id to it },
                        onRequireLogin = onRequireLogin,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                item {
                    when {
                        state.loadingReplies -> LoadingBox()
                        state.repliesEndReached && state.replies.isNotEmpty() -> Text(
                            text = "没有更多回复了",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                        state.replies.isEmpty() && !state.loadingReplies -> Text(
                            text = "还没有回复，来抢沙发吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            }
        }
    }

    deleteReplyTarget?.let { replyId ->
        AlertDialog(
            onDismissRequest = { deleteReplyTarget = null },
            title = { Text("删除回复") },
            text = { Text("删除后无法恢复，确定删除这条回复吗？") },
            confirmButton = {
                TextButton(onClick = { deleteReplyTarget = null; vm.deleteReply(replyId) }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteReplyTarget = null }) { Text("取消") } },
        )
    }
    deleteCommentTarget?.let { (replyId, commentId) ->
        AlertDialog(
            onDismissRequest = { deleteCommentTarget = null },
            title = { Text("删除评论") },
            text = { Text("确定删除这条评论吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteCommentTarget = null
                    vm.deleteComment(replyId, commentId)
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteCommentTarget = null }) { Text("取消") } },
        )
    }
}

/** 底部回复面板内容：引用预览 + Markdown 输入 + 发送。 */
@Composable
private fun ReplySheet(
    quote: Reply?,
    text: String,
    sending: Boolean,
    onTextChange: (String) -> Unit,
    onClearQuote: () -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // 等底部面板完成首次布局后再请求焦点，避免 FocusRequester 未初始化
        kotlinx.coroutines.delay(200)
        runCatching {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("发表回复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        quote?.let { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "回复 #${target.floor} @${target.author.name ?: target.author.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClearQuote, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "取消引用", modifier = Modifier.size(16.dp))
                }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("回复（支持 Markdown）") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp).focusRequester(focusRequester),
            maxLines = 10,
            shape = RoundedCornerShape(12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("取消") }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSend, enabled = text.isNotBlank() && !sending) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("发送")
                }
            }
        }
    }
}

@Composable
private fun TopicHeader(topic: Topic, onOpenUser: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topic.isNsfw) Chip("NSFW")
            Chip(com.kungal.app.ui.util.TopicLabels.category(topic.category))
            topic.sections.forEach { Chip(com.kungal.app.ui.util.TopicLabels.section(it)) }
            if (topic.miniApps.contains("lottery")) Chip("抽奖话题")
            if (topic.hasBestAnswer) Chip("已解决")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenUser(topic.author.id) },
            ) {
                AvatarImage(url = topic.author.avatar?.url, modifier = Modifier.size(32.dp), seed = topic.author.name)
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.author.name ?: topic.author.id,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (topic.authorMoemoepoint > 0) {
                            Spacer(Modifier.width(6.dp))
                            MoePointBadge(topic.authorMoemoepoint)
                        }
                    }
                    Text(
                        text = "发布于 " + fullDateTime(topic.createdAt) + if (topic.editedAt != null) "（已编辑）" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = topic.viewCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopicActionBar(
    topic: Topic,
    subscriptionLevel: String,
    busy: Boolean,
    loggedIn: Boolean,
    onReply: () -> Unit,
    onFavorite: () -> Unit,
    onSubscribe: () -> Unit,
    onUpvote: () -> Unit,
) {
    val viewer = topic.viewer ?: TopicViewer()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionChip(
            icon = { Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(16.dp)) },
            label = "回复",
            active = false,
            enabled = !busy && loggedIn,
            onClick = onReply,
        )
        ActionChip(
            icon = {
                Icon(
                    if (viewer.hasFavorited) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    null,
                    Modifier.size(16.dp),
                )
            },
            label = "收藏 ${topic.favoriteCount}",
            active = viewer.hasFavorited,
            enabled = !busy,
            onClick = onFavorite,
        )
        ActionChip(
            icon = { Icon(Icons.Outlined.Notifications, null, Modifier.size(16.dp)) },
            label = "通知：" + notificationLevelLabel(subscriptionLevel),
            active = subscriptionLevel == "watching",
            enabled = !busy && loggedIn,
            onClick = onSubscribe,
        )
        ActionChip(
            icon = { Icon(Icons.Outlined.Whatshot, null, Modifier.size(16.dp)) },
            label = "推 ${topic.upvoteCount}",
            active = viewer.hasUpvoted,
            enabled = !busy && loggedIn && viewer.canUpvote && !viewer.hasUpvoted,
            onClick = onUpvote,
        )
        if (viewer.hasUpvoted) {
            Text(
                "已推",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SubscriptionDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple("watching", "关注", "每条新回复都通知（合并为一条话题通知）"),
        Triple("normal", "正常", "只通知提到你的回复；作为作者时关注模式下也通知"),
        Triple("muted", "屏蔽", "不接收该话题的回复、提及与评论通知"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("话题通知") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (level, label, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(level) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (current == level) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun UpvoteDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确定推这个话题吗？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "推话题会消耗你 10 萌萌点，并给作者增加 5 萌萌点。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(30) },
                    placeholder = { Text("（可选）一句话，说说为什么推它") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
                Text(
                    "${note.length}/30",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(note.ifBlank { null }) }) { Text("确定推") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EditDialog(
    title: String,
    value: String,
    saving: Boolean,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !saving) {
                Text(if (saving) "保存中…" else "保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NsfwGate(onReveal: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "该话题包含成人向内容",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "当前内容设置为「隐藏」。你可以继续查看，或到设置中调整成人向内容的显示方式。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onReveal) { Text("仍然查看") }
    }
}
