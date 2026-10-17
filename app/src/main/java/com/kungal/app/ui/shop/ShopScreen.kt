package com.kungal.app.ui.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.ShopInventory
import com.kungal.core.model.ShopItem
import com.kungal.core.model.ShopOffer
import com.kungal.core.model.ShopOwnedItem
import com.kungal.core.model.ShopPurchase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ShopViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val offers: List<ShopOffer> = emptyList(),
        val inventory: ShopInventory? = null,
        val error: String? = null,
        val busyOfferId: String? = null,
        val restockBusyId: String? = null,
        val loadoutBusy: Boolean = false,
        val lastPurchase: ShopPurchase? = null,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load(showLoading = true)
    }

    fun refresh() = load(showLoading = true)

    private fun load(showLoading: Boolean) {
        if (showLoading) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val offers = async {
                        val all = mutableListOf<ShopOffer>()
                        var cursor: String? = null
                        do {
                            val page = app.shopApi.offers(cursor = cursor, limit = 100)
                            all += page.items
                            cursor = page.nextCursor
                        } while (cursor != null && all.size < 500)
                        all
                    }
                    val inventory = async { app.shopApi.inventory() }
                    offers.await() to inventory.await()
                }
            }.onSuccess { (offers, inventory) ->
                _state.update { it.copy(loading = false, offers = offers, inventory = inventory) }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    fun purchase(offer: ShopOffer) {
        if (_state.value.busyOfferId != null) return
        _state.update { it.copy(busyOfferId = offer.id, message = null) }
        viewModelScope.launch {
            runCatching { app.shopApi.purchase(offer.id, UUID.randomUUID().toString()) }
                .onSuccess { result ->
                    _state.update { it.copy(busyOfferId = null, lastPurchase = result) }
                    load(showLoading = false)
                }
                .onFailure { e ->
                    _state.update { it.copy(busyOfferId = null, message = friendlyError(e)) }
                }
        }
    }

    fun toggleRestock(offerId: String, subscribed: Boolean) {
        if (_state.value.restockBusyId != null) return
        _state.update { it.copy(restockBusyId = offerId) }
        viewModelScope.launch {
            runCatching {
                if (subscribed) app.shopApi.unsubscribeRestock(offerId)
                else app.shopApi.subscribeRestock(offerId)
            }.onSuccess {
                _state.update {
                    val ids = it.inventory?.restockAlertOfferIds.orEmpty()
                    val updated = if (subscribed) ids - offerId else ids + offerId
                    it.copy(
                        restockBusyId = null,
                        inventory = it.inventory?.copy(restockAlertOfferIds = updated),
                        message = if (subscribed) "已取消到货提醒" else "补货后会提醒你",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(restockBusyId = null, message = friendlyError(e)) }
            }
        }
    }

    fun setLoadout(item: ShopItem, slot: String, scope: String) {
        if (_state.value.loadoutBusy) return
        _state.update { it.copy(loadoutBusy = true, message = null) }
        viewModelScope.launch {
            runCatching { app.shopApi.setLoadout(scope, slot, item.id) }
                .onSuccess {
                    _state.update { it.copy(loadoutBusy = false, message = "已装备「${item.displayName}」") }
                    load(showLoading = false)
                }
                .onFailure { e ->
                    _state.update { it.copy(loadoutBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun clearLoadout(scope: String, slot: String) {
        if (_state.value.loadoutBusy) return
        _state.update { it.copy(loadoutBusy = true, message = null) }
        viewModelScope.launch {
            runCatching { app.shopApi.clearLoadout(scope, slot) }
                .onSuccess {
                    _state.update { it.copy(loadoutBusy = false, message = "已卸下装扮") }
                    load(showLoading = false)
                }
                .onFailure { e ->
                    _state.update { it.copy(loadoutBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun clearLastPurchase() = _state.update { it.copy(lastPurchase = null) }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

private val shopTabs = listOf("shop" to "商店", "items" to "我的物品", "orders" to "订单")

private fun itemTypeLabel(type: String): String = when (type) {
    "avatar_frame" -> "头像框"
    "profile_background" -> "主页背景"
    "profile_about" -> "个人介绍"
    "redeem_code" -> "兑换码"
    else -> type
}

/** 萌萌点商店：商品购买、我的物品与装扮、订单与兑换码、到货提醒。 */
@Composable
fun ShopScreen(
    app: KungalApp,
    onBack: () -> Unit,
) {
    val vm: ShopViewModel = viewModel { ShopViewModel(app) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf("shop") }
    var buying by remember { mutableStateOf<ShopOffer?>(null) }
    var equipping by remember { mutableStateOf<ShopItem?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    buying?.let { offer ->
        val name = offer.rewards.firstOrNull()?.item?.displayName ?: "商品"
        val balance = state.inventory?.moemoepoint ?: 0
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text("确认购买") },
            text = {
                Text(
                    "花费 ${offer.price} 萌萌点购买「$name」？\n" +
                        "当前余额 $balance，购买后剩余 ${(balance - offer.price).coerceAtLeast(0)}。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        buying = null
                        vm.purchase(offer)
                    },
                    enabled = state.busyOfferId == null,
                ) { Text("购买") }
            },
            dismissButton = { TextButton(onClick = { buying = null }) { Text("取消") } },
        )
    }

    equipping?.let { item ->
        AlertDialog(
            onDismissRequest = { equipping = null },
            title = { Text("选择展示范围") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("「${item.displayName}」要戴到哪里？")
                    Text(
                        "全站展示：所有使用同一账号的站点都能看到；仅本站：只在鲲 Galgame 论坛显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    equipping = null
                    vm.setLoadout(target, target.itemType, "everywhere")
                }) { Text("全站展示") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val target = item
                    equipping = null
                    vm.setLoadout(target, target.itemType, "this_site")
                }) { Text("仅本站") }
            },
        )
    }

    state.lastPurchase?.let { purchase ->
        val codes = purchase.order.redeemCodes
        if (codes.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { vm.clearLastPurchase() },
                title = { Text("购买成功") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("兑换码（点击复制）：")
                        codes.forEach { redeem ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        AndroidActions.copyText(context, "兑换码", redeem.code)
                                    },
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(redeem.code, style = MaterialTheme.typography.bodyMedium)
                                    redeem.expiryDate?.let {
                                        Text(
                                            "有效期至 $it",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { vm.clearLastPurchase() }) { Text("关闭") } },
            )
        } else {
            LaunchedEffect(purchase) {
                snackbar.showSnackbar("购买成功，余额 ${purchase.moemoepoint} 萌萌点")
                vm.clearLastPurchase()
            }
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "萌萌点商店",
                subtitle = "余额 ${state.inventory?.moemoepoint ?: 0} 萌萌点",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterChips(options = shopTabs, selected = tab, onSelect = { tab = it })
            when {
                state.loading && state.inventory == null -> LoadingBox(Modifier.fillMaxSize())
                state.error != null && state.inventory == null -> ErrorBox(
                    state.error!!,
                    onRetry = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> when (tab) {
                    "items" -> MyItemsTab(
                        inventory = state.inventory,
                        busy = state.loadoutBusy,
                        onEquip = { equipping = it },
                        onTakeOff = { scope, slot -> vm.clearLoadout(scope, slot) },
                    )
                    "orders" -> OrdersTab(
                        inventory = state.inventory,
                        onCopy = { code -> AndroidActions.copyText(context, "兑换码", code) },
                    )
                    else -> OffersTab(
                        offers = state.offers,
                        balance = state.inventory?.moemoepoint ?: 0,
                        purchaseCounts = state.inventory?.limitUsage.orEmpty()
                            .associate { it.offerId to it.purchasedCount },
                        subscribedIds = state.inventory?.restockAlertOfferIds.orEmpty().toSet(),
                        busyOfferId = state.busyOfferId,
                        restockBusyId = state.restockBusyId,
                        onBuy = { buying = it },
                        onToggleRestock = { id, subscribed -> vm.toggleRestock(id, subscribed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OffersTab(
    offers: List<ShopOffer>,
    balance: Int,
    purchaseCounts: Map<String, Int>,
    subscribedIds: Set<String>,
    busyOfferId: String?,
    restockBusyId: String?,
    onBuy: (ShopOffer) -> Unit,
    onToggleRestock: (String, Boolean) -> Unit,
) {
    if (offers.isEmpty()) {
        Text(
            "商店暂时没有在售商品",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
        return
    }
    val sections = mutableListOf<Pair<String, List<ShopOffer>>>()
    fun add(title: String, list: List<ShopOffer>) {
        if (list.isNotEmpty()) sections += title to list
    }
    add("本站专区", offers.filter { it.isSiteExclusive })
    val rest = offers.filterNot { it.isSiteExclusive }
    fun typeOf(offer: ShopOffer): String =
        offer.rewards.firstOrNull()?.item?.itemType ?: "other"
    add("福利兑换", rest.filter { typeOf(it) == "redeem_code" })
    add("功能解锁", rest.filter { typeOf(it) == "profile_about" })
    add("装扮", rest.filter { typeOf(it) in setOf("avatar_frame", "profile_background") })
    val known = setOf("redeem_code", "profile_about", "avatar_frame", "profile_background")
    add("其他", rest.filter { typeOf(it) !in known })

    LazyColumn(Modifier.fillMaxSize()) {
        sections.forEach { (title, list) ->
            item(key = "section-$title") {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { offer ->
                OfferCard(
                    offer = offer,
                    balance = balance,
                    purchasedCount = purchaseCounts[offer.id] ?: 0,
                    subscribed = offer.id in subscribedIds,
                    busy = busyOfferId == offer.id,
                    restockBusy = restockBusyId == offer.id,
                    onBuy = { onBuy(offer) },
                    onToggleRestock = { onToggleRestock(offer.id, offer.id in subscribedIds) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun OfferCard(
    offer: ShopOffer,
    balance: Int,
    purchasedCount: Int,
    subscribed: Boolean,
    busy: Boolean,
    restockBusy: Boolean,
    onBuy: () -> Unit,
    onToggleRestock: () -> Unit,
) {
    val item = offer.rewards.firstOrNull()?.item
    val name = item?.displayName ?: "商品 #${offer.id}"
    val limitReached = offer.purchaseLimit?.let { purchasedCount >= it.quantity } == true
    val soldOut = offer.soldOut
    val affordable = balance >= offer.price
    val disabledReason = when {
        soldOut -> "已售罄"
        limitReached -> "已达限购"
        !affordable -> "余额不足"
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val artwork = item?.artwork
            if (artwork != null) {
                KungalImage(
                    url = artwork.animatedUrl?.takeIf { it.isNotBlank() } ?: artwork.staticUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip("${offer.price} 萌萌点")
                    offer.rewards.firstOrNull()?.durationDays?.let { Chip("$it 天") }
                    offer.purchaseLimit?.let {
                        Chip(if (it.period == "month") "每月限购 ${it.quantity}" else "限购 ${it.quantity}")
                    }
                    if (offer.stockCount != null && offer.remainingCount != null) {
                        Chip("库存 ${offer.remainingCount}/${offer.stockCount}")
                    }
                }
                offer.endsAt?.let {
                    Text(
                        "截止 ${it.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (soldOut) {
                    TextButton(
                        onClick = onToggleRestock,
                        enabled = !restockBusy,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(if (subscribed) "已订阅到货提醒（点击取消）" else "到货提醒")
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onBuy,
                enabled = disabledReason == null && !busy,
            ) {
                Text(
                    when {
                        busy -> "购买中…"
                        disabledReason != null -> disabledReason
                        else -> "购买"
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun MyItemsTab(
    inventory: ShopInventory?,
    busy: Boolean,
    onEquip: (ShopItem) -> Unit,
    onTakeOff: (scope: String, slot: String) -> Unit,
) {
    val items = inventory?.items.orEmpty()
    if (items.isEmpty()) {
        Text(
            "还没有购买过物品。购买后的装扮和兑换码会显示在这里。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.item.id + it.acquiredAt }) { owned ->
            OwnedItemRow(
                owned = owned,
                loadout = inventory?.loadout.orEmpty(),
                busy = busy,
                onEquip = onEquip,
                onTakeOff = onTakeOff,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun OwnedItemRow(
    owned: ShopOwnedItem,
    loadout: List<com.kungal.core.model.ShopLoadoutSlot>,
    busy: Boolean,
    onEquip: (ShopItem) -> Unit,
    onTakeOff: (scope: String, slot: String) -> Unit,
) {
    val item = owned.item
    val cosmetic = item.itemType == "avatar_frame" || item.itemType == "profile_background"
    val wornSlot = loadout.firstOrNull { it.wornItemId == item.id }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val artwork = item.artwork
            if (artwork != null) {
                KungalImage(
                    url = artwork.animatedUrl?.takeIf { it.isNotBlank() } ?: artwork.staticUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(itemTypeLabel(item.itemType))
                    if (wornSlot != null) Chip("已装备")
                    if (!owned.isActive) Chip("已过期")
                }
                Text(
                    "获得于 ${relativeTime(owned.acquiredAt)}" +
                        (owned.expiresAt?.let { " · 有效期至 ${it.take(10)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (cosmetic) {
                Spacer(Modifier.width(8.dp))
                if (wornSlot != null) {
                    OutlinedButton(
                        onClick = { onTakeOff(wornSlot.scope, wornSlot.slot) },
                        enabled = !busy,
                    ) { Text("卸下") }
                } else {
                    Button(
                        onClick = { onEquip(item) },
                        enabled = !busy && owned.isActive,
                    ) { Text("装备") }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(
    inventory: ShopInventory?,
    onCopy: (String) -> Unit,
) {
    val orders = inventory?.orders.orEmpty()
    if (orders.isEmpty()) {
        Text(
            "还没有订单",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(orders, key = { it.id }) { order ->
            val names = order.rewards.mapNotNull { it.item.displayName.takeIf { name -> name.isNotBlank() } }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "订单 #${order.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Chip(if (order.state == "refunded") "已退款" else "已完成")
                    }
                    if (names.isNotEmpty()) {
                        Text(
                            names.joinToString("、"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "${order.price} 萌萌点 · ${relativeTime(order.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    order.redeemCodes.forEach { redeem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCopy(redeem.code) },
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(redeem.code, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    "点击复制",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
