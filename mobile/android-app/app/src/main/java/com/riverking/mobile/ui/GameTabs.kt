package com.riverking.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.AchievementClaimDto
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.ClubDetailsDto
import com.riverking.mobile.auth.ClubMemberDto
import com.riverking.mobile.auth.CurrentTournamentDto
import com.riverking.mobile.auth.GuideFishDto
import com.riverking.mobile.auth.GuideLocationDto
import com.riverking.mobile.auth.GuideLureDto
import com.riverking.mobile.auth.GuideRodDto
import com.riverking.mobile.auth.MeResponseDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.QuestDto
import com.riverking.mobile.auth.QuestListDto
import com.riverking.mobile.auth.ReferralInfoDto
import com.riverking.mobile.auth.ReferralRewardDto
import com.riverking.mobile.auth.ShopPackageDto
import com.riverking.mobile.auth.TournamentDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MainTab(val icon: ImageVector) {
    FISHING(Icons.Rounded.SportsEsports),
    TOURNAMENTS(Icons.Rounded.EmojiEvents),
    RATINGS(Icons.Rounded.Leaderboard),
    GUIDE(Icons.Rounded.MenuBook),
    CLUB(Icons.Rounded.Groups),
    SHOP(Icons.Rounded.ShoppingBag),
}

enum class GuideSection {
    LOCATIONS,
    FISH,
    LURES,
    RODS,
    ACHIEVEMENTS,
}

@Composable
fun MainShell(
    state: RiverKingUiState,
    isPlayFlavor: Boolean,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onClaimDaily: () -> Unit,
    onBeginCast: () -> Unit,
    onHookFish: () -> Unit,
    onTapChallenge: () -> Unit,
    onToggleAutoCast: () -> Unit,
    onSelectLocation: (Long) -> Unit,
    onSelectLure: (Long) -> Unit,
    onSelectRod: (Long) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
    onDismissCatch: () -> Unit,
    onShareCatch: (CatchDto) -> Unit,
    onLoadTournaments: (Boolean) -> Unit,
    onOpenTournament: (Long) -> Unit,
    onCloseTournament: () -> Unit,
    onClaimPrize: (Long) -> Unit,
    onSetRatingsMode: (RatingsMode) -> Unit,
    onSetRatingsPeriod: (RatingsPeriod) -> Unit,
    onSetRatingsOrder: (RatingsOrder) -> Unit,
    onSetRatingsLocation: (String) -> Unit,
    onSetRatingsFish: (String) -> Unit,
    onLoadRatings: (Boolean) -> Unit,
    onLoadGuide: (Boolean) -> Unit,
    onClaimAchievement: (String) -> Unit,
    onDismissAchievementReward: () -> Unit,
    onLoadClub: (Boolean) -> Unit,
    onLoadClubChat: () -> Unit,
    onSearchClubs: (String?) -> Unit,
    onCreateClub: (String) -> Unit,
    onJoinClub: (Long) -> Unit,
    onUpdateClubInfo: (String) -> Unit,
    onUpdateClubSettings: (Double, Boolean) -> Unit,
    onLeaveClub: () -> Unit,
    onClubMemberAction: (Long, String) -> Unit,
    onLoadShop: (Boolean) -> Unit,
    onGenerateReferral: () -> Unit,
    onShareReferral: (ReferralInfoDto) -> Unit,
    onClaimReferralRewards: () -> Unit,
    onBuyShopWithCoins: (String) -> Unit,
    onPlayPurchase: (String) -> Unit,
) {
    val me = state.me ?: return
    val strings = rememberRiverStrings(me.language)
    val clipboard = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FISHING) }

    val achievementBadge = state.guide.achievements.any { it.claimable }
    val tabLabels = remember(strings) {
        mapOf(
            MainTab.FISHING to strings.fishing,
            MainTab.TOURNAMENTS to strings.tournaments,
            MainTab.RATINGS to strings.ratings,
            MainTab.GUIDE to strings.guide,
            MainTab.CLUB to strings.club,
            MainTab.SHOP to strings.shop,
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            HeaderBar(
                me = me,
                strings = strings,
                achievementBadge = achievementBadge,
                onLogout = onLogout,
                onChangeLanguage = onChangeLanguage,
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                MainTab.entries.forEach { tab ->
                    val showBadge = when (tab) {
                        MainTab.SHOP -> me.dailyAvailable
                        MainTab.GUIDE -> achievementBadge
                        else -> false
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (showBadge) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tabLabels.getValue(tab))
                            }
                        },
                        label = { Text(tabLabels.getValue(tab), maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        AppBackdrop {
            when (selectedTab) {
                MainTab.FISHING -> FishingScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onRefreshProfile = onRefreshProfile,
                    onClaimDaily = onClaimDaily,
                    onBeginCast = onBeginCast,
                    onHookFish = onHookFish,
                    onTapChallenge = onTapChallenge,
                    onToggleAutoCast = onToggleAutoCast,
                    onSelectLocation = onSelectLocation,
                    onSelectLure = onSelectLure,
                    onSelectRod = onSelectRod,
                    onOpenCatch = onOpenCatch,
                    onLoadGuide = onLoadGuide,
                    onLoadClub = onLoadClub,
                )
                MainTab.TOURNAMENTS -> TournamentsScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onRefresh = { onLoadTournaments(true) },
                    onOpenTournament = onOpenTournament,
                    onClaimPrize = onClaimPrize,
                    onOpenCatch = onOpenCatch,
                )
                MainTab.RATINGS -> RatingsScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onSetMode = onSetRatingsMode,
                    onSetPeriod = onSetRatingsPeriod,
                    onSetOrder = onSetRatingsOrder,
                    onSetLocation = onSetRatingsLocation,
                    onSetFish = onSetRatingsFish,
                    onRefresh = { onLoadRatings(true) },
                    onOpenCatch = onOpenCatch,
                )
                MainTab.GUIDE -> GuideScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onRefresh = { onLoadGuide(true) },
                    onClaimAchievement = onClaimAchievement,
                )
                MainTab.CLUB -> ClubScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onRefresh = { onLoadClub(true) },
                    onLoadChat = onLoadClubChat,
                    onSearchClubs = onSearchClubs,
                    onCreateClub = onCreateClub,
                    onJoinClub = onJoinClub,
                    onUpdateClubInfo = onUpdateClubInfo,
                    onUpdateClubSettings = onUpdateClubSettings,
                    onLeaveClub = onLeaveClub,
                    onMemberAction = onClubMemberAction,
                )
                MainTab.SHOP -> ShopScreen(
                    state = state,
                    strings = strings,
                    isPlayFlavor = isPlayFlavor,
                    modifier = Modifier.padding(padding),
                    clipboard = clipboard,
                    onRefresh = { onLoadShop(true) },
                    onGenerateReferral = onGenerateReferral,
                    onShareReferral = onShareReferral,
                    onClaimReferralRewards = onClaimReferralRewards,
                    onBuyShopWithCoins = onBuyShopWithCoins,
                    onPlayPurchase = onPlayPurchase,
                )
            }
        }
    }

    state.selectedCatch?.let { catch ->
        CatchDetailsDialog(
            strings = strings,
            catch = catch,
            loading = state.catchLoading,
            onDismiss = onDismissCatch,
            onShare = { onShareCatch(catch) },
        )
    }

    state.lastAchievementReward?.let { reward ->
        AchievementRewardDialog(
            strings = strings,
            reward = reward,
            onDismiss = onDismissAchievementReward,
        )
    }

    state.tournaments.selectedTournament?.let { tournament ->
        TournamentDialog(
            strings = strings,
            details = tournament,
            onDismiss = onCloseTournament,
            onOpenCatch = onOpenCatch,
        )
    }
}

@Composable
private fun AppBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1E26),
                        Color(0xFF122A33),
                        Color(0xFF08131A),
                    )
                )
            ),
        content = content,
    )
}

@Composable
private fun HeaderBar(
    me: MeResponseDto,
    strings: RiverStrings,
    achievementBadge: Boolean,
    onLogout: () -> Unit,
    onChangeLanguage: (String) -> Unit,
) {
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }?.name.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = me.username ?: strings.appTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = currentLocation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatPill(title = "🪙", value = me.coins.toString())
        StatPill(title = "kg", value = me.totalWeight.asKgCompact())
        StatPill(title = "🔥", value = me.dailyStreak.toString())
        LanguageToggle(currentLanguage = me.language, onChangeLanguage = onChangeLanguage)
        OutlinedButton(onClick = onLogout) { Text(strings.logout) }
    }
}

@Composable
private fun FishingScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onRefreshProfile: () -> Unit,
    onClaimDaily: () -> Unit,
    onBeginCast: () -> Unit,
    onHookFish: () -> Unit,
    onTapChallenge: () -> Unit,
    onToggleAutoCast: () -> Unit,
    onSelectLocation: (Long) -> Unit,
    onSelectLure: (Long) -> Unit,
    onSelectRod: (Long) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
    onLoadGuide: (Boolean) -> Unit,
    onLoadClub: (Boolean) -> Unit,
) {
    val me = state.me ?: return
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroLocationCard(
                title = currentLocation?.name ?: strings.water,
                subtitle = when (state.fishing.phase) {
                    FishingPhase.READY -> strings.castRod
                    FishingPhase.COOLDOWN -> strings.castCooldown
                    FishingPhase.WAITING_BITE -> strings.waitingBite
                    FishingPhase.BITING -> strings.hook
                    FishingPhase.TAP_CHALLENGE -> strings.tapFast
                    FishingPhase.RESOLVING -> strings.casting
                },
                backgroundUrl = locationBackgroundAsset(currentLocation?.name),
            ) {
                FishingActionCard(
                    state = state,
                    strings = strings,
                    onBeginCast = onBeginCast,
                    onHookFish = onHookFish,
                    onTapChallenge = onTapChallenge,
                )
            }
        }
        item {
            InfoCard {
                SelectorRow(strings.water, me.locations, me.locationId, onSelectLocation) { location ->
                    if (location.unlocked) location.name else "${location.name} • ${location.unlockKg.asKgCompact()}"
                }
                Spacer(modifier = Modifier.height(8.dp))
                SelectorRow(strings.rod, me.rods, me.currentRodId, onSelectRod) { rod ->
                    if (rod.unlocked) rod.name else "${rod.name} • ${rod.unlockKg.asKgCompact()}"
                }
                Spacer(modifier = Modifier.height(8.dp))
                SelectorRow(strings.bait, me.lures, me.currentLureId, onSelectLure) { lure ->
                    "${lure.displayName} ×${lure.qty}"
                }
            }
        }
        item {
            InfoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onClaimDaily,
                        enabled = me.dailyAvailable,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (me.dailyAvailable) strings.claimDaily else strings.dailyClaimed)
                    }
                    OutlinedButton(
                        onClick = onRefreshProfile,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(strings.refresh)
                    }
                }
                if (me.autoFish) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.autoCast, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = state.fishing.autoCastEnabled,
                            onCheckedChange = { onToggleAutoCast() },
                        )
                    }
                }
            }
        }
        item {
            if (state.guide.quests != null) {
                QuestPreviewCard(strings, state.guide.quests, onLoadGuide, onLoadClub)
            }
        }
        if (me.recent.isNotEmpty()) {
            item {
                SectionCard(strings.recentCatches) {
                    me.recent.forEachIndexed { index, recent ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        CatchRow(
                            title = recent.fish,
                            subtitle = "${recent.location} • ${strings.rarityLabel(recent.rarity)}",
                            value = recent.weight.asKgCompact(),
                            onClick = {
                                onOpenCatch(
                                    CatchDto(
                                        id = recent.id,
                                        fish = recent.fish,
                                        weight = recent.weight,
                                        location = recent.location,
                                        rarity = recent.rarity,
                                        at = recent.at,
                                        user = me.username,
                                        userId = me.id,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentsScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onOpenTournament: (Long) -> Unit,
    onClaimPrize: (Long) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val tournaments = state.tournaments
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill(title = strings.prizes, value = tournaments.prizes.size.toString(), modifier = Modifier.weight(1f))
                StatPill(title = strings.currentTournament, value = if (tournaments.current == null) "0" else "1", modifier = Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(strings.refresh)
            }
        }
        item {
            SectionCard(strings.currentTournament) {
                if (tournaments.current == null) {
                    Text(strings.noData)
                } else {
                    TournamentCard(
                        tournament = tournaments.current.tournament,
                        mine = tournaments.current.mine,
                        onClick = { onOpenTournament(tournaments.current.tournament.id) },
                    )
                    if (tournaments.current.leaderboard.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        tournaments.current.leaderboard.take(5).forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            CatchRow(
                                title = "#${entry.rank} ${entry.user ?: "Unknown"}",
                                subtitle = listOfNotNull(entry.fish, entry.location).joinToString(" • "),
                                value = entry.value.asKgCompact(),
                                onClick = {
                                    if (entry.catchId != null && entry.fish != null && entry.location != null) {
                                        onOpenCatch(
                                            CatchDto(
                                                id = entry.catchId,
                                                fish = entry.fish,
                                                weight = entry.value,
                                                location = entry.location,
                                                rarity = tournaments.current.tournament.fishRarity.orEmpty(),
                                                user = entry.user,
                                                userId = entry.userId,
                                            )
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        if (tournaments.prizes.isNotEmpty()) {
            item {
                SectionCard(strings.prizes) {
                    tournaments.prizes.forEachIndexed { index, prize ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prizeLabel(prize), fontWeight = FontWeight.SemiBold)
                                Text("#${prize.rank} • ${prize.source}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { onClaimPrize(prize.id) }) { Text(strings.claim) }
                        }
                    }
                }
            }
        }
        if (tournaments.upcoming.isNotEmpty()) {
            item {
                SectionCard(strings.upcomingTournaments) {
                    tournaments.upcoming.forEachIndexed { index, tournament ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        TournamentCard(tournament = tournament, mine = null, onClick = { onOpenTournament(tournament.id) })
                    }
                }
            }
        }
        if (tournaments.past.isNotEmpty()) {
            item {
                SectionCard(strings.pastTournaments) {
                    tournaments.past.forEachIndexed { index, tournament ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        TournamentCard(tournament = tournament, mine = null, onClick = { onOpenTournament(tournament.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingsScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onSetMode: (RatingsMode) -> Unit,
    onSetPeriod: (RatingsPeriod) -> Unit,
    onSetOrder: (RatingsOrder) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetFish: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val me = state.me ?: return
    val ratings = state.ratings
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.ratings) {
                HorizontalChipRow {
                    RatingsMode.entries.forEach { mode ->
                        FilterChip(
                            selected = ratings.mode == mode,
                            onClick = { onSetMode(mode) },
                            label = { Text(if (mode == RatingsMode.PERSONAL) strings.personal else strings.global) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalChipRow {
                    RatingsPeriod.entries.forEach { period ->
                        AssistChip(
                            onClick = { onSetPeriod(period) },
                            label = { Text(strings.periodLabel(period)) },
                            colors = chipColors(selected = ratings.period == period),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalChipRow {
                    RatingsOrder.entries.forEach { order ->
                        AssistChip(
                            onClick = { onSetOrder(order) },
                            label = { Text(strings.orderLabel(order)) },
                            colors = chipColors(selected = ratings.order == order),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalChipRow {
                    AssistChip(
                        onClick = { onSetLocation("all") },
                        label = { Text(strings.allLocations) },
                        colors = chipColors(selected = ratings.locationId == "all"),
                    )
                    me.locations.forEach { location ->
                        AssistChip(
                            onClick = { onSetLocation(location.id.toString()) },
                            label = { Text(location.name) },
                            colors = chipColors(selected = ratings.locationId == location.id.toString()),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalChipRow {
                    AssistChip(
                        onClick = { onSetFish("all") },
                        label = { Text(strings.allFish) },
                        colors = chipColors(selected = ratings.fishId == "all"),
                    )
                    ratings.fishOptions.forEach { fish ->
                        AssistChip(
                            onClick = { onSetFish(fish.id.toString()) },
                            label = { Text(fish.name) },
                            colors = chipColors(selected = ratings.fishId == fish.id.toString()),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.refresh)
                }
            }
        }
        item {
            SectionCard(strings.ratings) {
                if (ratings.entries.isEmpty()) {
                    Text(strings.noData)
                } else {
                    ratings.entries.forEachIndexed { index, catch ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        CatchRow(
                            title = buildString {
                                if (catch.rank != null) append("#${catch.rank} ")
                                append(catch.fish)
                            },
                            subtitle = listOfNotNull(catch.user, catch.location, catch.at?.let(::formatTimestamp)).joinToString(" • "),
                            value = catch.weight.asKgCompact(),
                            accent = rarityColor(catch.rarity),
                            onClick = { onOpenCatch(catch) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onClaimAchievement: (String) -> Unit,
) {
    val me = state.me ?: return
    val guide = state.guide
    var section by rememberSaveable { mutableStateOf(GuideSection.LOCATIONS) }
    var rarityFilter by rememberSaveable { mutableStateOf("all") }
    var showCaughtOnly by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.guide) {
                HorizontalChipRow {
                    GuideSection.entries.forEach { tab ->
                        FilterChip(
                            selected = section == tab,
                            onClick = { section = tab },
                            label = {
                                Text(
                                    when (tab) {
                                        GuideSection.LOCATIONS -> strings.guideWaters
                                        GuideSection.FISH -> strings.guideFish
                                        GuideSection.LURES -> strings.guideLures
                                        GuideSection.RODS -> strings.guideRods
                                        GuideSection.ACHIEVEMENTS -> strings.achievements
                                    }
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.refresh)
                }
            }
        }
        if (section == GuideSection.FISH) {
            item {
                SectionCard(strings.guideFish) {
                    HorizontalChipRow {
                        listOf("all", "common", "uncommon", "rare", "epic", "mythic", "legendary").forEach { rarity ->
                            AssistChip(
                                onClick = { rarityFilter = rarity },
                                label = { Text(if (rarity == "all") strings.allFish else strings.rarityLabel(rarity)) },
                                colors = chipColors(selected = rarityFilter == rarity),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (me.language == "ru") "Только пойманные" else "Caught only")
                        Switch(checked = showCaughtOnly, onCheckedChange = { showCaughtOnly = it })
                    }
                }
            }
        }
        item {
            when (section) {
                GuideSection.LOCATIONS -> SectionCard(strings.guideWaters) {
                    guide.guide?.locations?.forEachIndexed { index, location ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideLocationRow(location)
                    } ?: Text(strings.noData)
                }
                GuideSection.FISH -> SectionCard(strings.guideFish) {
                    val fishList = guide.guide?.fish.orEmpty()
                        .filter { rarityFilter == "all" || it.rarity == rarityFilter }
                        .filter { !showCaughtOnly || me.caughtFishIds.contains(it.id) }
                    if (fishList.isEmpty()) {
                        Text(strings.noData)
                    } else {
                        fishList.forEachIndexed { index, fish ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            GuideFishRow(fish = fish, discovered = me.caughtFishIds.contains(fish.id))
                        }
                    }
                }
                GuideSection.LURES -> SectionCard(strings.guideLures) {
                    guide.guide?.lures?.forEachIndexed { index, lure ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideLureRow(lure)
                    } ?: Text(strings.noData)
                }
                GuideSection.RODS -> SectionCard(strings.guideRods) {
                    guide.guide?.rods?.forEachIndexed { index, rod ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideRodRow(rod)
                    } ?: Text(strings.noData)
                }
                GuideSection.ACHIEVEMENTS -> SectionCard(strings.achievements) {
                    if (guide.achievements.isEmpty()) {
                        Text(strings.noData)
                    } else {
                        guide.achievements.forEachIndexed { index, achievement ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            AchievementRow(strings, achievement, onClaimAchievement)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onLoadChat: () -> Unit,
    onSearchClubs: (String?) -> Unit,
    onCreateClub: (String) -> Unit,
    onJoinClub: (Long) -> Unit,
    onUpdateClubInfo: (String) -> Unit,
    onUpdateClubSettings: (Double, Boolean) -> Unit,
    onLeaveClub: () -> Unit,
    onMemberAction: (Long, String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var createName by rememberSaveable { mutableStateOf("") }
    var infoDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(state.club.club?.info.orEmpty()) }
    var minWeightDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(((state.club.club?.minJoinWeightKg ?: 0.0).toInt()).toString()) }
    var recruitingDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(state.club.club?.recruitingOpen ?: true) }
    val club = state.club.club

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text(strings.refresh) }
        }
        if (club != null) {
            item {
                SectionCard(strings.club) {
                    ClubOverview(strings, club)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = infoDraft,
                        onValueChange = { infoDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (club.role == "president" || club.role == "heir") strings.club else strings.guide) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { onUpdateClubInfo(infoDraft) }, modifier = Modifier.weight(1f)) {
                            Text(strings.refresh)
                        }
                        OutlinedButton(onClick = onLeaveClub, modifier = Modifier.weight(1f)) {
                            Text(strings.leaveClub)
                        }
                    }
                    if (club.role == "president" || club.role == "heir") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = minWeightDraft,
                            onValueChange = { minWeightDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (strings.login == "Логин") "Мин. вес для входа" else "Min join weight") },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (strings.login == "Логин") "Открыт для набора" else "Recruiting open")
                            Switch(checked = recruitingDraft, onCheckedChange = { recruitingDraft = it })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onUpdateClubSettings(minWeightDraft.toDoubleOrNull() ?: 0.0, recruitingDraft) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(strings.refresh) }
                    }
                }
            }
            item {
                SectionCard(strings.chat) {
                    OutlinedButton(onClick = onLoadChat, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.chat)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.club.chat.isEmpty()) {
                        Text(strings.noData)
                    } else {
                        state.club.chat.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            Text(localizeClubChatMessage(item.message, strings))
                            Text(
                                formatTimestamp(item.createdAt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(strings.prizes) {
                    ClubWeekSection(strings, club.currentWeek, club.role, onMemberAction)
                }
            }
        }
        item {
            SectionCard(strings.searchClub) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.searchClub) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onSearchClubs(searchQuery) }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.searchClub)
                }
                if (state.club.searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    state.club.searchResults.forEachIndexed { index, summary ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${summary.memberCount}/${summary.capacity} • ${summary.minJoinWeightKg.asKgCompact()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(onClick = { onJoinClub(summary.id) }) { Text(strings.joinClub) }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(strings.createClub) {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.createClub) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onCreateClub(createName) }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.createClub)
                }
            }
        }
    }
}

@Composable
private fun ShopScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    isPlayFlavor: Boolean,
    modifier: Modifier = Modifier,
    clipboard: ClipboardManager,
    onRefresh: () -> Unit,
    onGenerateReferral: () -> Unit,
    onShareReferral: (ReferralInfoDto) -> Unit,
    onClaimReferralRewards: () -> Unit,
    onBuyShopWithCoins: (String) -> Unit,
    onPlayPurchase: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text(strings.refresh) }
        }
        item {
            ReferralCard(
                strings = strings,
                referrals = state.shop.referrals,
                rewards = state.shop.referralRewards,
                clipboard = clipboard,
                onGenerateReferral = onGenerateReferral,
                onShareReferral = onShareReferral,
                onClaimReferralRewards = onClaimReferralRewards,
            )
        }
        items(state.shop.categories, key = { it.id }) { category ->
            SectionCard(category.name) {
                category.packs.forEachIndexed { index, pack ->
                    if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                    ShopPackageRow(
                        strings = strings,
                        pack = pack,
                        isPlayFlavor = isPlayFlavor,
                        onBuyWithCoins = { onBuyShopWithCoins(pack.id) },
                        onPlayPurchase = { onPlayPurchase(pack.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLocationCard(
    title: String,
    subtitle: String,
    backgroundUrl: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (backgroundUrl != null) {
                AsyncImage(
                    model = backgroundUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp)),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
            }
        }
    }
}

@Composable
private fun FishingActionCard(
    state: RiverKingUiState,
    strings: RiverStrings,
    onBeginCast: () -> Unit,
    onHookFish: () -> Unit,
    onTapChallenge: () -> Unit,
) {
    val phase = state.fishing.phase
    val timeLeft = (state.fishing.phaseTimeLeftMillis / 1000.0).coerceAtLeast(0.0)
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = when (phase) {
                    FishingPhase.READY -> strings.castRod
                    FishingPhase.COOLDOWN -> "${strings.castCooldown} • ${timeLeft.toInt()}s"
                    FishingPhase.WAITING_BITE -> "${strings.waitingBite} • ${timeLeft.toInt()}s"
                    FishingPhase.BITING -> "${strings.hook} • ${String.format(Locale.US, "%.1f", timeLeft)}s"
                    FishingPhase.TAP_CHALLENGE -> "${strings.tapFast} ${state.fishing.tapCount}/${state.fishing.tapGoal}"
                    FishingPhase.RESOLVING -> strings.casting
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = when (phase) {
                    FishingPhase.READY -> onBeginCast
                    FishingPhase.BITING -> onHookFish
                    FishingPhase.TAP_CHALLENGE -> onTapChallenge
                    else -> ({})
                },
                enabled = phase == FishingPhase.READY || phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (phase) {
                        FishingPhase.BITING -> Color(0xFFE64F4F)
                        FishingPhase.TAP_CHALLENGE -> Color(0xFFF4C857)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when (phase) {
                        FishingPhase.TAP_CHALLENGE -> Color(0xFF221400)
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                ),
            ) {
                Text(
                    when (phase) {
                        FishingPhase.READY -> strings.castRod
                        FishingPhase.BITING -> strings.hook
                        FishingPhase.TAP_CHALLENGE -> strings.tapFast
                        FishingPhase.RESOLVING -> strings.casting
                        FishingPhase.WAITING_BITE -> strings.waitingBite
                        FishingPhase.COOLDOWN -> strings.castCooldown
                    }
                )
            }
            if (state.fishing.lastCast != null) {
                val cast = state.fishing.lastCast
                if (cast.caught && cast.catch != null) {
                    Text(
                        "${cast.catch.fish} • ${cast.catch.weight.asKgCompact()}",
                        color = rarityColor(cast.catch.rarity),
                        fontWeight = FontWeight.SemiBold,
                    )
                } else if (!cast.caught) {
                    Text(strings.fishEscaped, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun QuestPreviewCard(
    strings: RiverStrings,
    quests: QuestListDto,
    onLoadGuide: (Boolean) -> Unit,
    onLoadClub: (Boolean) -> Unit,
) {
    SectionCard(strings.quests) {
        val activeQuests = quests.daily.take(2) + quests.weekly.take(1)
        if (activeQuests.isEmpty()) {
            Text(strings.noData)
        } else {
            activeQuests.forEachIndexed { index, quest ->
                if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                Text(quest.name, fontWeight = FontWeight.SemiBold)
                Text("${quest.progress}/${quest.target} • ${quest.rewardCoins} coins")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onLoadGuide(true) }, modifier = Modifier.weight(1f)) {
                Text(strings.quests)
            }
            OutlinedButton(onClick = { onLoadClub(true) }, modifier = Modifier.weight(1f)) {
                Text(strings.openClub)
            }
        }
    }
}

@Composable
private fun GuideLocationRow(location: GuideLocationDto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(location.name, fontWeight = FontWeight.SemiBold)
        Text(location.fish.joinToString { "${it.name} (${it.rarity})" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(location.lures.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuideFishRow(fish: GuideFishDto, discovered: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (discovered) fish.name else "?????", fontWeight = FontWeight.SemiBold, color = rarityColor(fish.rarity))
        Text(fish.locations.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(fish.lures.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuideLureRow(lure: GuideLureDto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(lure.name, fontWeight = FontWeight.SemiBold)
        Text(lure.locations.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(lure.fish.joinToString { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuideRodRow(rod: GuideRodDto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rod.name, fontWeight = FontWeight.SemiBold)
        Text(rod.unlockKg.asKgCompact(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val bonuses = listOfNotNull(
            rod.bonusWater?.let { "water: $it" },
            rod.bonusPredator?.let { if (it) "predator bonus" else "peaceful bonus" },
        )
        if (bonuses.isNotEmpty()) {
            Text(bonuses.joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AchievementRow(strings: RiverStrings, achievement: AchievementDto, onClaimAchievement: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(achievement.name, fontWeight = FontWeight.SemiBold)
            Text(achievement.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${achievement.progressLabel}/${achievement.targetLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (achievement.claimable) {
            Button(onClick = { onClaimAchievement(achievement.code) }) {
                Text(strings.claim)
            }
        } else {
            Text(achievement.level, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReferralCard(
    strings: RiverStrings,
    referrals: ReferralInfoDto?,
    rewards: List<ReferralRewardDto>,
    clipboard: ClipboardManager,
    onGenerateReferral: () -> Unit,
    onShareReferral: (ReferralInfoDto) -> Unit,
    onClaimReferralRewards: () -> Unit,
) {
    SectionCard(strings.inviteFriends) {
        Text(referrals?.androidShareText ?: strings.inviteFriends, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onGenerateReferral, modifier = Modifier.weight(1f)) {
                Text(strings.generateLink)
            }
            OutlinedButton(
                onClick = { referrals?.let(onShareReferral) },
                modifier = Modifier.weight(1f),
                enabled = referrals != null,
            ) {
                Text(strings.shareLink)
            }
        }
        OutlinedButton(
            onClick = {
                referrals?.link?.let { clipboard.setText(AnnotatedString(it)) }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = referrals != null,
        ) {
            Text(strings.copyLink)
        }
        if (referrals != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(referrals.link, color = MaterialTheme.colorScheme.secondary)
        }
        if (referrals?.invited?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(referrals.invited.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (rewards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(rewards.joinToString { "${it.name} ×${it.qty}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClaimReferralRewards, modifier = Modifier.fillMaxWidth()) {
                Text(strings.claimRewards)
            }
        }
    }
}

@Composable
private fun ShopPackageRow(
    strings: RiverStrings,
    pack: ShopPackageDto,
    isPlayFlavor: Boolean,
    onBuyWithCoins: () -> Unit,
    onPlayPurchase: () -> Unit,
) {
    val canBuyCoins = pack.coinPrice != null
    val paidUnavailableInDirect = !isPlayFlavor && pack.coinPrice == null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = shopIconAsset(pack.id),
            contentDescription = pack.name,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pack.name, fontWeight = FontWeight.SemiBold)
            Text(pack.desc, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pack.until != null) {
                Text(pack.until, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (pack.originalPrice != null) {
                Text(
                    "${pack.originalPrice}★",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (canBuyCoins) {
                Button(onClick = onBuyWithCoins) {
                    Text("${pack.coinPrice} 🪙")
                }
            }
            if (pack.coinPrice == null) {
                if (paidUnavailableInDirect) {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(strings.unavailable)
                    }
                    Text(strings.shopDisabledDirect, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                } else {
                    Button(onClick = onPlayPurchase) {
                        Text(strings.payWithGoogle)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubOverview(strings: RiverStrings, club: ClubDetailsDto) {
    Text(club.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(strings.roleLabel(club.role), color = MaterialTheme.colorScheme.secondary)
    Spacer(modifier = Modifier.height(4.dp))
    Text(club.info.ifBlank { strings.noData }, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatPill(title = if (strings.login == "Логин") "Участники" else "Members", value = "${club.memberCount}/${club.capacity}", modifier = Modifier.weight(1f))
        StatPill(title = if (strings.login == "Логин") "Взнос" else "Coins", value = club.currentWeek.totalCoins.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ClubWeekSection(
    strings: RiverStrings,
    week: com.riverking.mobile.auth.ClubWeekDto,
    role: String,
    onMemberAction: (Long, String) -> Unit,
) {
    Text(formatWeek(week.weekStart), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    week.members.forEachIndexed { index, member ->
        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
        ClubMemberRow(strings, member, role, onMemberAction)
    }
}

@Composable
private fun ClubMemberRow(
    strings: RiverStrings,
    member: ClubMemberDto,
    actorRole: String,
    onMemberAction: (Long, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                Text(strings.roleLabel(member.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(member.coins.toString(), color = MaterialTheme.colorScheme.secondary)
        }
        if (actorRole == "president" || actorRole == "heir") {
            HorizontalChipRow {
                if (member.role == "novice" || member.role == "veteran") {
                    AssistChip(onClick = { onMemberAction(member.userId, "promote") }, label = { Text("+") })
                    AssistChip(onClick = { onMemberAction(member.userId, "demote") }, label = { Text("-") })
                    AssistChip(onClick = { onMemberAction(member.userId, "kick") }, label = { Text(if (strings.login == "Логин") "Кик" else "Kick") })
                }
                if (actorRole == "president" && member.role == "heir") {
                    AssistChip(
                        onClick = { onMemberAction(member.userId, "appoint-president") },
                        label = { Text(if (strings.login == "Логин") "Назначить" else "Appoint") },
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(tournament: TournamentDto, mine: com.riverking.mobile.auth.LeaderboardEntryDto?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(tournament.name, fontWeight = FontWeight.SemiBold)
        Text("${formatEpoch(tournament.startTime)} → ${formatEpoch(tournament.endTime)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            listOfNotNull(tournament.location, tournament.fish, tournament.fishRarity).joinToString(" • "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        mine?.let {
            Text("#${it.rank} • ${it.value.asKgCompact()}", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun TournamentDialog(
    strings: RiverStrings,
    details: CurrentTournamentDto,
    onDismiss: () -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(details.tournament.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${formatEpoch(details.tournament.startTime)} → ${formatEpoch(details.tournament.endTime)}")
                details.mine?.let { Text("#${it.rank} • ${it.value.asKgCompact()}", color = MaterialTheme.colorScheme.secondary) }
                details.leaderboard.forEach { entry ->
                    CatchRow(
                        title = "#${entry.rank} ${entry.user ?: "Unknown"}",
                        subtitle = listOfNotNull(entry.fish, entry.location).joinToString(" • "),
                        value = entry.value.asKgCompact(),
                        onClick = {
                            if (entry.catchId != null && entry.fish != null && entry.location != null) {
                                onOpenCatch(
                                    CatchDto(
                                        id = entry.catchId,
                                        fish = entry.fish,
                                        weight = entry.value,
                                        location = entry.location,
                                        rarity = details.tournament.fishRarity.orEmpty(),
                                        user = entry.user,
                                        userId = entry.userId,
                                    )
                                )
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.continueLabel) }
        },
    )
}

@Composable
private fun CatchDetailsDialog(
    strings: RiverStrings,
    catch: CatchDto,
    loading: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(catch.fish, color = rarityColor(catch.rarity)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (loading) {
                    Text(strings.loading)
                }
                Text(catch.weight.asKgCompact(), fontWeight = FontWeight.SemiBold)
                Text(catch.location)
                catch.user?.let { Text(it) }
                catch.at?.let { Text(formatTimestamp(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(strings.rarityLabel(catch.rarity), color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onShare, enabled = catch.id > 0L) { Text(strings.shareCatch) }
                TextButton(onClick = onDismiss) { Text(strings.continueLabel) }
            }
        },
    )
}

@Composable
private fun AchievementRewardDialog(
    strings: RiverStrings,
    reward: AchievementClaimDto,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.achievements) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reward.rewards.forEach { item ->
                    Text(
                        when {
                            item.coins != null -> "${item.coins} coins"
                            item.packageId.isBlank() -> "×${item.qty}"
                            else -> "${item.packageId} ×${item.qty}"
                        }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.continueLabel) } },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun StatPill(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LanguageToggle(currentLanguage: String, onChangeLanguage: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(currentLanguage.uppercase(Locale.US)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("EN") },
                onClick = {
                    expanded = false
                    onChangeLanguage("en")
                },
            )
            DropdownMenuItem(
                text = { Text("RU") },
                onClick = {
                    expanded = false
                    onChangeLanguage("ru")
                },
            )
        }
    }
}

@Composable
private fun <T> SelectorRow(
    title: String,
    items: List<T>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    label: (T) -> String,
) where T : Any {
    Text(title, fontWeight = FontWeight.SemiBold)
    HorizontalChipRow {
        items.forEach { item ->
            val itemId = item.javaClass.getMethod("getId").invoke(item) as Long
            val enabled = runCatching { item.javaClass.getMethod("getUnlocked").invoke(item) as Boolean }.getOrDefault(true)
            FilterChip(
                selected = selectedId == itemId,
                onClick = { onSelect(itemId) },
                enabled = enabled,
                label = { Text(label(item), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun HorizontalChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun CatchRow(
    title: String,
    subtitle: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = accent)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun chipColors(selected: Boolean) = AssistChipDefaults.assistChipColors(
    containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    labelColor = MaterialTheme.colorScheme.onSurface,
)

private fun Double.asKgCompact(): String = String.format(Locale.US, "%.2f kg", this)

private fun String?.rarityLabel(strings: RiverStrings): String = strings.rarityLabel(this)

private fun prizeLabel(prize: PrizeDto): String = when {
    prize.coins != null -> "${prize.coins} coins"
    prize.packageId.equals("coins", ignoreCase = true) -> "${prize.qty} coins"
    else -> "${prize.packageId} ×${prize.qty}"
}

private fun rarityColor(rarity: String?): Color = when (rarity) {
    "common" -> Color(0xFFE8EEF2)
    "uncommon" -> Color(0xFF74D77C)
    "rare" -> Color(0xFF58A9FF)
    "epic" -> Color(0xFFC576FF)
    "mythic" -> Color(0xFFFF9E5E)
    "legendary" -> Color(0xFFFFD54F)
    else -> Color.White
}

private fun formatEpoch(seconds: Long): String =
    Instant.ofEpochSecond(seconds)
        .atZone(ZoneId.of("Europe/Belgrade"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

private fun formatTimestamp(value: String): String =
    runCatching {
        Instant.parse(value)
            .atZone(ZoneId.of("Europe/Belgrade"))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    }.getOrElse { value }

private fun formatWeek(value: String): String =
    runCatching {
        Instant.parse("${value}T00:00:00Z")
            .atZone(ZoneId.of("Europe/Belgrade"))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }.getOrElse { value }

private fun localizeClubChatMessage(raw: String, strings: RiverStrings): String {
    if (!raw.trim().startsWith("{")) return raw
    return when {
        raw.contains("clubChatMemberJoined") -> if (strings.login == "Логин") "Новый участник присоединился" else "A new member joined"
        raw.contains("clubChatMemberLeft") -> if (strings.login == "Логин") "Участник покинул клуб" else "A member left the club"
        raw.contains("clubChatMemberKicked") -> if (strings.login == "Логин") "Участник исключён" else "A member was removed"
        raw.contains("clubChatRareCatch") -> if (strings.login == "Логин") "Редкий улов клуба" else "Rare club catch"
        raw.contains("clubChatRatingReward") -> if (strings.login == "Логин") "Награда за рейтинг" else "Ranking reward"
        raw.contains("clubChatPresidentAppointed") -> if (strings.login == "Логин") "Назначен новый президент" else "A new president was appointed"
        else -> raw
    }
}

private fun locationBackgroundAsset(location: String?): String? {
    val path = when (location) {
        "Пруд", "Pond" -> "/app/assets/backgrounds/pond.png"
        "Болото", "Swamp" -> "/app/assets/backgrounds/swamp.png"
        "Река", "River" -> "/app/assets/backgrounds/river.png"
        "Озеро", "Lake" -> "/app/assets/backgrounds/lake.png"
        "Водохранилище", "Reservoir" -> "/app/assets/backgrounds/reservoir.png"
        "Горная река", "Mountain River" -> "/app/assets/backgrounds/mountain_river.png"
        "Дельта реки", "River Delta" -> "/app/assets/backgrounds/river_delta.png"
        "Прибрежье моря", "Sea Coast" -> "/app/assets/backgrounds/sea_coast.png"
        "Фьорд", "Fjord" -> "/app/assets/backgrounds/fjord.png"
        "Открытый океан", "Open Ocean" -> "/app/assets/backgrounds/open_ocean.png"
        "Русло Амазонки", "Amazon Riverbed" -> "/app/assets/backgrounds/amazon_riverbed.png"
        "Игапо, затопленный лес", "Flooded Forest" -> "/app/assets/backgrounds/flooded_forest.png"
        "Мангровые заросли", "Mangroves" -> "/app/assets/backgrounds/mangroves.png"
        "Коралловые отмели", "Coral Flats" -> "/app/assets/backgrounds/coral_flats.png"
        else -> null
    }
    return path?.let(::assetUrl)
}

private fun shopIconAsset(packId: String): String = assetUrl(
    if (packId.startsWith("autofish")) "/app/assets/shop/autofish.png" else "/app/assets/shop/$packId.png"
)

private fun assetUrl(path: String): String = BuildConfig.API_BASE_URL.trimEnd('/') + path
