package com.riverking.mobile.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.riverking.mobile.auth.LeaderboardEntryDto
import com.riverking.mobile.auth.MeResponseDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.PrizeSpecDto
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
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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

private enum class FishingSheetType {
    DAILY,
    LOCATIONS,
    RODS,
    LURES,
    QUESTS,
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
            cardBytes = state.selectedCatchCard,
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
) {
    val me = state.me ?: return
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }
    var activeSheet by rememberSaveable { mutableStateOf<FishingSheetType?>(null) }
    var lastDailyPromptToken by rememberSaveable(me.id) { mutableStateOf<String?>(null) }
    val dailyPromptToken = remember(me.dailyAvailable, me.dailyStreak, me.dailyRewards) {
        if (!me.dailyAvailable) {
            null
        } else {
            buildString {
                append(me.dailyStreak)
                append(':')
                append(me.dailyRewards.flatten().joinToString("|") { "${it.name}:${it.qty}" })
            }
        }
    }

    LaunchedEffect(dailyPromptToken) {
        if (dailyPromptToken != null && dailyPromptToken != lastDailyPromptToken) {
            lastDailyPromptToken = dailyPromptToken
            activeSheet = FishingSheetType.DAILY
        }
    }

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
                scene = {
                    FishingStageScene(
                        state = state,
                        strings = strings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                },
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
            FishingEquipmentCard(
                strings = strings,
                me = me,
                onOpenLocations = { activeSheet = FishingSheetType.LOCATIONS },
                onOpenRods = { activeSheet = FishingSheetType.RODS },
                onOpenLures = { activeSheet = FishingSheetType.LURES },
            )
        }
        item {
            FishingRewardsCard(
                strings = strings,
                me = me,
                autoCastEnabled = state.fishing.autoCastEnabled,
                onOpenDaily = { activeSheet = FishingSheetType.DAILY },
                onRefreshProfile = onRefreshProfile,
                onToggleAutoCast = onToggleAutoCast,
            )
        }
        item {
            if (state.guide.quests != null) {
                QuestPreviewCard(
                    strings = strings,
                    quests = state.guide.quests,
                    onOpenQuests = { activeSheet = FishingSheetType.QUESTS },
                )
            } else {
                InfoCard {
                    Text(strings.quests, fontWeight = FontWeight.SemiBold)
                    Text(strings.loading, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onLoadGuide(true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.refresh)
                    }
                }
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

    when (activeSheet) {
        FishingSheetType.DAILY -> DailyRewardSheet(
            strings = strings,
            me = me,
            onDismiss = { activeSheet = null },
            onClaim = {
                activeSheet = null
                onClaimDaily()
            },
        )
        FishingSheetType.LOCATIONS -> LocationPickerSheet(
            strings = strings,
            me = me,
            onDismiss = { activeSheet = null },
            onSelect = { locationId ->
                activeSheet = null
                onSelectLocation(locationId)
            },
        )
        FishingSheetType.RODS -> RodPickerSheet(
            strings = strings,
            me = me,
            onDismiss = { activeSheet = null },
            onSelect = { rodId ->
                activeSheet = null
                onSelectRod(rodId)
            },
        )
        FishingSheetType.LURES -> LurePickerSheet(
            strings = strings,
            me = me,
            onDismiss = { activeSheet = null },
            onSelect = { lureId ->
                activeSheet = null
                onSelectLure(lureId)
            },
        )
        FishingSheetType.QUESTS -> QuestSheet(
            strings = strings,
            quests = state.guide.quests,
            loading = state.guide.loading,
            onDismiss = { activeSheet = null },
            onReload = { onLoadGuide(true) },
        )
        null -> Unit
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
                        strings = strings,
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
                        TournamentCard(strings = strings, tournament = tournament, mine = null, onClick = { onOpenTournament(tournament.id) })
                    }
                }
            }
        }
        if (tournaments.past.isNotEmpty()) {
            item {
                SectionCard(strings.pastTournaments) {
                    tournaments.past.forEachIndexed { index, tournament ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        TournamentCard(strings = strings, tournament = tournament, mine = null, onClick = { onOpenTournament(tournament.id) })
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
    val showPrizePreview = ratings.mode == RatingsMode.GLOBAL &&
        ratings.fishId == "all" &&
        ratings.locationId != "all" &&
        ratings.order == RatingsOrder.DESC
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
                    if (showPrizePreview) {
                        Text(
                            if (strings.login == "Логин") "Призовые места подсвечены в строках рейтинга" else "Prize positions are highlighted in the ranking rows",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ratings.entries.forEachIndexed { index, catch ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        RatingsEntryCard(
                            strings = strings,
                            catch = catch,
                            showPrizePreview = showPrizePreview,
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
    scene: @Composable () -> Unit = {},
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
                scene()
                content()
            }
        }
    }
}

@Composable
private fun FishingStageScene(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
) {
    val phase = state.fishing.phase
    val timeLeft = (state.fishing.phaseTimeLeftMillis / 1000.0).coerceAtLeast(0.0)
    val inWater = phase == FishingPhase.WAITING_BITE || phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE
    val infinite = rememberInfiniteTransition(label = "fishing-scene")
    val waveShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2200, easing = LinearEasing)),
        label = "wave-shift",
    )
    val rippleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "ripple-progress",
    )
    val castX by animateFloatAsState(
        targetValue = when (phase) {
            FishingPhase.READY -> 0.18f
            FishingPhase.COOLDOWN -> 0.24f
            FishingPhase.WAITING_BITE -> 0.76f
            FishingPhase.BITING -> 0.78f
            FishingPhase.TAP_CHALLENGE -> 0.79f
            FishingPhase.RESOLVING -> if (state.fishing.lastCast?.caught == true) 0.56f else 0.74f
        },
        animationSpec = tween(durationMillis = 850),
        label = "cast-x",
    )
    val castY by animateFloatAsState(
        targetValue = when (phase) {
            FishingPhase.READY -> 0.60f
            FishingPhase.COOLDOWN -> 0.62f
            FishingPhase.WAITING_BITE -> 0.69f
            FishingPhase.BITING -> 0.66f
            FishingPhase.TAP_CHALLENGE -> 0.64f
            FishingPhase.RESOLVING -> if (state.fishing.lastCast?.caught == true) 0.42f else 0.68f
        },
        animationSpec = tween(durationMillis = 850),
        label = "cast-y",
    )
    val statusText = when (phase) {
        FishingPhase.READY -> strings.castRod
        FishingPhase.COOLDOWN -> "${strings.castCooldown} • ${timeLeft.toInt()}s"
        FishingPhase.WAITING_BITE -> "${strings.waitingBite} • ${timeLeft.toInt()}s"
        FishingPhase.BITING -> "${strings.hook} • ${String.format(Locale.US, "%.1f", timeLeft)}s"
        FishingPhase.TAP_CHALLENGE -> "${strings.tapFast} ${state.fishing.tapCount}/${state.fishing.tapGoal}"
        FishingPhase.RESOLVING -> strings.casting
    }

    Surface(
        modifier = modifier,
        color = Color(0x11141920),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val skyBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x40DFF4FF),
                        Color(0x181B3241),
                        Color(0x10203344),
                    )
                )
                drawRoundRect(brush = skyBrush)

                val waterTop = size.height * 0.48f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x8831B1D5),
                            Color(0xAA0D5778),
                            Color(0xCC07344C),
                        ),
                        startY = waterTop,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, waterTop),
                    size = Size(size.width, size.height - waterTop),
                )

                val shorePath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width * 0.18f, size.height * 0.72f)
                    lineTo(size.width * 0.32f, size.height)
                    close()
                }
                drawPath(
                    path = shorePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xCC3B2E23), Color(0x994A3A2B)),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.32f, size.height),
                    ),
                    style = Fill,
                )

                val rodBase = Offset(size.width * 0.16f, size.height * 0.86f)
                val rodMid = Offset(size.width * 0.20f, size.height * 0.55f)
                val rodTip = Offset(size.width * 0.29f, size.height * 0.22f)
                val bobberOffset = if (inWater || phase == FishingPhase.RESOLVING) {
                    sin(waveShift * (2f * PI).toFloat()) * size.height * when (phase) {
                        FishingPhase.BITING -> 0.028f
                        FishingPhase.TAP_CHALLENGE -> 0.022f
                        else -> 0.015f
                    }
                } else {
                    0f
                }
                val bobber = Offset(size.width * castX, size.height * castY + bobberOffset)
                val control = Offset(
                    x = (rodTip.x + bobber.x) * 0.52f,
                    y = min(rodTip.y, bobber.y) + if (inWater) size.height * 0.10f else size.height * 0.18f,
                )
                val linePath = Path().apply {
                    moveTo(rodBase.x, rodBase.y)
                    quadraticBezierTo(rodMid.x, rodMid.y, rodTip.x, rodTip.y)
                    quadraticBezierTo(control.x, control.y, bobber.x, bobber.y)
                }
                drawPath(
                    path = linePath,
                    color = Color(0xFFF0E8D6),
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = Path().apply {
                        moveTo(rodBase.x, rodBase.y)
                        quadraticBezierTo(rodMid.x, rodMid.y, rodTip.x, rodTip.y)
                    },
                    color = Color(0xFF5F4128),
                    style = Stroke(width = 10f, cap = StrokeCap.Round),
                )

                repeat(if (phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE) 2 else 1) { index ->
                    val progress = ((rippleProgress + index * 0.35f) % 1f)
                    val radius = size.width * (0.04f + progress * 0.08f)
                    drawCircle(
                        color = Color.White.copy(alpha = (0.4f - progress * 0.25f).coerceAtLeast(0f)),
                        radius = radius,
                        center = Offset(bobber.x, max(waterTop + 8f, bobber.y + size.height * 0.02f)),
                        style = Stroke(width = 3f),
                    )
                }

                val bobberRadius = size.width * 0.022f
                drawCircle(color = Color(0xFFE8F0F4), radius = bobberRadius, center = bobber)
                drawCircle(
                    color = Color(0xFFE55B5B),
                    radius = bobberRadius,
                    center = bobber.copy(y = bobber.y - bobberRadius * 0.42f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.32f),
                    radius = bobberRadius * 2.3f,
                    center = bobber,
                )

                if (state.fishing.lastCast?.caught == true && state.fishing.lastCast.catch != null) {
                    val catchColor = rarityColor(state.fishing.lastCast.catch.rarity)
                    drawOval(
                        color = catchColor.copy(alpha = 0.24f),
                        topLeft = Offset(
                            x = bobber.x - size.width * 0.09f,
                            y = bobber.y - size.height * 0.08f,
                        ),
                        size = Size(size.width * 0.12f, size.height * 0.06f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    SceneBadge(text = statusText, accent = phaseAccentColor(phase))
                    state.fishing.lastCast?.let { cast ->
                        if (cast.caught && cast.catch != null) {
                            SceneBadge(
                                text = "${cast.catch.fish} • ${cast.catch.weight.asKgCompact()}",
                                accent = rarityColor(cast.catch.rarity),
                            )
                        } else if (!cast.caught) {
                            SceneBadge(
                                text = strings.fishEscaped,
                                accent = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (phase == FishingPhase.TAP_CHALLENGE || phase == FishingPhase.BITING) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.34f),
                            border = BorderStroke(1.dp, phaseAccentColor(phase).copy(alpha = 0.55f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (phase == FishingPhase.BITING) strings.hook else strings.tapFast,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (phase == FishingPhase.BITING) {
                                        String.format(Locale.US, "%.1fs", timeLeft)
                                    } else {
                                        "${state.fishing.tapCount}/${state.fishing.tapGoal}"
                                    },
                                    color = phaseAccentColor(phase),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneBadge(
    text: String,
    accent: Color,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.30f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
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
                    containerColor = phaseAccentColor(phase),
                    contentColor = if (phase == FishingPhase.TAP_CHALLENGE) Color(0xFF221400) else MaterialTheme.colorScheme.onPrimary,
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
        }
    }
}

@Composable
private fun FishingEquipmentCard(
    strings: RiverStrings,
    me: MeResponseDto,
    onOpenLocations: () -> Unit,
    onOpenRods: () -> Unit,
    onOpenLures: () -> Unit,
) {
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }
    val currentRod = me.rods.firstOrNull { it.id == me.currentRodId }
    val currentLure = me.lures.firstOrNull { it.id == me.currentLureId }
    InfoCard {
        FishingSelectionRow(
            title = strings.water,
            value = currentLocation?.name ?: "—",
            subtitle = strings.chooseLocation,
            accent = Color(0xFF68D4FF),
            onClick = onOpenLocations,
        )
        FishingSelectionRow(
            title = strings.rod,
            value = currentRod?.name ?: "—",
            subtitle = currentRod?.let { strings.rodBonusLabel(it.bonusWater, it.bonusPredator) } ?: strings.chooseRod,
            accent = Color(0xFFC9A46E),
            onClick = onOpenRods,
        )
        FishingSelectionRow(
            title = strings.bait,
            value = currentLure?.displayName ?: "—",
            subtitle = currentLure?.let { "x${it.qty} • ${it.description.ifBlank { strings.chooseBait }}" } ?: strings.chooseBait,
            accent = Color(0xFF8FE388),
            onClick = onOpenLures,
        )
    }
}

@Composable
private fun FishingSelectionRow(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(accent, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringsArrow(), color = accent, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FishingRewardsCard(
    strings: RiverStrings,
    me: MeResponseDto,
    autoCastEnabled: Boolean,
    onOpenDaily: () -> Unit,
    onRefreshProfile: () -> Unit,
    onToggleAutoCast: () -> Unit,
) {
    InfoCard {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDaily),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
            border = BorderStroke(
                1.dp,
                if (me.dailyAvailable) Color(0xFFFFD76A).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.dailyGift, fontWeight = FontWeight.Bold)
                    Text(
                        "${strings.dailyStreakLabel}: ${me.dailyStreak} • " +
                            if (me.dailyAvailable) strings.dailyRewardReady else strings.dailyReturnTomorrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    if (me.dailyAvailable) strings.claimDaily else strings.dailyClaimed,
                    color = if (me.dailyAvailable) Color(0xFFFFD76A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
            }
        }
        OutlinedButton(onClick = onRefreshProfile, modifier = Modifier.fillMaxWidth()) {
            Text(strings.refresh)
        }
        if (me.autoFish) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.autoCast, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (strings.login == "Логин") "Автоловля сама запускает новый заброс" else "Auto-fishing restarts the next cast for you",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = autoCastEnabled,
                    onCheckedChange = { onToggleAutoCast() },
                )
            }
        }
    }
}

@Composable
private fun QuestPreviewCard(
    strings: RiverStrings,
    quests: QuestListDto,
    onOpenQuests: () -> Unit,
) {
    val activeQuests = quests.daily.take(2) + quests.weekly.take(2)
    SectionCard(strings.quests) {
        if (activeQuests.isEmpty()) {
            Text(strings.noData)
        } else {
            activeQuests.forEachIndexed { index, quest ->
                if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                QuestSummaryRow(strings = strings, quest = quest)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOpenQuests, modifier = Modifier.fillMaxWidth()) {
            Text(strings.viewAll)
        }
    }
}

@Composable
private fun QuestSummaryRow(
    strings: RiverStrings,
    quest: QuestDto,
    showTitle: Boolean = true,
    showPeriod: Boolean = true,
) {
    val ratio = (quest.progress.toFloat() / quest.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (showTitle) {
                    Text(quest.name, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "${quest.progress}/${quest.target} • ${strings.questRewardLabel(quest.rewardCoins)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (showPeriod) {
                Text(
                    if (quest.period == "weekly") strings.weeklyQuestsLabel else strings.dailyQuestsLabel,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxSize()
                    .background(if (quest.completed) Color(0xFF7CE38B) else Color(0xFF4EA8DE)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyRewardSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    onDismiss: () -> Unit,
    onClaim: () -> Unit,
) {
    val rewards = me.dailyRewards
    val currentIndex = when {
        rewards.isEmpty() -> -1
        me.dailyAvailable -> min(me.dailyStreak, rewards.lastIndex)
        else -> min(max(me.dailyStreak - 1, 0), rewards.lastIndex)
    }
    val completedThreshold = if (me.dailyAvailable) me.dailyStreak else me.dailyStreak + 1
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(strings.dailyGift, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${strings.dailyStreakLabel}: ${me.dailyStreak}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rewards.chunked(2).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEachIndexed { columnIndex, items ->
                        val absoluteIndex = rowIndex * 2 + columnIndex
                        DailyRewardDayCard(
                            strings = strings,
                            dayIndex = absoluteIndex,
                            rewards = items,
                            isCurrent = absoluteIndex == currentIndex,
                            isCompleted = absoluteIndex < completedThreshold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (me.dailyAvailable) {
                Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.claimDaily)
                }
            } else {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.dailyReturnTomorrow)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DailyRewardDayCard(
    strings: RiverStrings,
    dayIndex: Int,
    rewards: List<com.riverking.mobile.auth.DailyRewardItemDto>,
    isCurrent: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> Color(0x33FFD76A)
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isCurrent -> Color(0xFFFFD76A)
                isCompleted -> Color.White.copy(alpha = 0.10f)
                else -> Color.White.copy(alpha = 0.06f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.dayLabel(dayIndex + 1), fontWeight = FontWeight.Bold)
            rewards.forEach { reward ->
                Text(
                    "${reward.qty} × ${reward.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCurrent) {
                Text(
                    strings.dailyRewardReady,
                    color = Color(0xFFFFD76A),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestSheet(
    strings: RiverStrings,
    quests: QuestListDto?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(strings.quests, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (loading && quests == null) {
                Text(strings.loading, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (quests == null) {
                Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.refresh)
                }
            } else {
                QuestSection(strings.dailyQuestsLabel, quests.daily, strings)
                QuestSection(strings.weeklyQuestsLabel, quests.weekly, strings)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuestSection(
    title: String,
    items: List<QuestDto>,
    strings: RiverStrings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (items.isEmpty()) {
            Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { quest ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(quest.name, fontWeight = FontWeight.Bold)
                        Text(quest.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        QuestSummaryRow(strings = strings, quest = quest, showTitle = false, showPeriod = false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    FishingPickerSheet(title = strings.chooseLocation, onDismiss = onDismiss) {
        me.locations.forEach { location ->
            PickerRow(
                title = location.name,
                subtitle = if (location.unlocked) {
                    strings.currentLabel.takeIf { me.locationId == location.id }
                        ?: location.unlockKg.asKgCompact()
                } else {
                    location.unlockKg.asKgCompact()
                },
                enabled = location.unlocked,
                selected = me.locationId == location.id,
                onClick = { onSelect(location.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RodPickerSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    FishingPickerSheet(title = strings.chooseRod, onDismiss = onDismiss) {
        me.rods.forEach { rod ->
            PickerRow(
                title = rod.name,
                subtitle = buildString {
                    append(strings.rodBonusLabel(rod.bonusWater, rod.bonusPredator))
                    append(" • ")
                    append(
                        if (rod.unlocked) rod.unlockKg.asKgCompact()
                        else rod.unlockKg.asKgCompact()
                    )
                },
                enabled = rod.unlocked,
                selected = me.currentRodId == rod.id,
                onClick = { onSelect(rod.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LurePickerSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    FishingPickerSheet(title = strings.chooseBait, onDismiss = onDismiss) {
        me.lures.forEach { lure ->
            PickerRow(
                title = lure.displayName,
                subtitle = "x${lure.qty} • ${lure.description.ifBlank { lure.name }}",
                enabled = lure.qty > 0,
                selected = me.currentLureId == lure.id,
                onClick = { onSelect(lure.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishingPickerSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                content()
                Spacer(modifier = Modifier.height(12.dp))
            },
        )
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                when {
                    selected -> "•"
                    !enabled -> "×"
                    else -> stringsArrow()
                },
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentCard(
    strings: RiverStrings,
    tournament: TournamentDto,
    mine: LeaderboardEntryDto?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
        border = BorderStroke(
            1.dp,
            if (mine != null) Color(0xFF6FE7B7).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tournament.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatEpoch(tournament.startTime)} → ${formatEpoch(tournament.endTime)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                mine?.let {
                    SceneBadge(
                        text = "#${it.rank} • ${tournamentMetricValueLabel(tournament.metric, it.value)}",
                        accent = Color(0xFF6FE7B7),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatchDetailChip(tournamentFishLabel(strings, tournament))
                CatchDetailChip(tournament.location ?: anyLocationLabel(strings))
                CatchDetailChip(tournamentMetricLabel(strings, tournament.metric))
                CatchDetailChip("${tournament.prizePlaces} ${strings.prizes.lowercase(Locale.getDefault())}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentDialog(
    strings: RiverStrings,
    details: CurrentTournamentDto,
    onDismiss: () -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(details.tournament.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CatchDetailChip(tournamentFishLabel(strings, details.tournament))
                    CatchDetailChip(details.tournament.location ?: anyLocationLabel(strings))
                    CatchDetailChip(tournamentMetricLabel(strings, details.tournament.metric))
                    CatchDetailChip("${details.tournament.prizePlaces} ${strings.prizes.lowercase(Locale.getDefault())}")
                }
                Text(
                    "${formatEpoch(details.tournament.startTime)} → ${formatEpoch(details.tournament.endTime)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                details.mine?.let { mine ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (strings.login == "Логин") "Ваш результат" else "Your result",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TournamentLeaderboardRow(
                            strings = strings,
                            tournament = details.tournament,
                            entry = mine,
                            highlighted = true,
                            onOpenCatch = onOpenCatch,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (strings.login == "Логин") "Лидерборд" else "Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    details.leaderboard.forEach { entry ->
                        TournamentLeaderboardRow(
                            strings = strings,
                            tournament = details.tournament,
                            entry = entry,
                            highlighted = details.mine?.rank == entry.rank,
                            onOpenCatch = onOpenCatch,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.continueLabel)
                }
            }
        }
    }
}

@Composable
private fun RatingsEntryCard(
    strings: RiverStrings,
    catch: CatchDto,
    showPrizePreview: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(
            1.dp,
            if (showPrizePreview && (catch.prizeCoins ?: 0) > 0) Color(0xFFFFD76A).copy(alpha = 0.42f)
            else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            catch.rank?.let { rank ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = rarityColor(catch.rarity).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, rarityColor(catch.rarity).copy(alpha = 0.45f)),
                ) {
                    Text(
                        "#$rank",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    catch.fish,
                    fontWeight = FontWeight.SemiBold,
                    color = rarityColor(catch.rarity),
                )
                Text(
                    listOfNotNull(catch.user, catch.location, catch.at?.let(::formatTimestamp)).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showPrizePreview && (catch.prizeCoins ?: 0) > 0) {
                    Text(
                        "🪙 +${catch.prizeCoins}",
                        color = Color(0xFFFFD76A),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(catch.weight.asKgCompact(), fontWeight = FontWeight.Bold)
                Text(
                    strings.rarityLabel(catch.rarity),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TournamentLeaderboardRow(
    strings: RiverStrings,
    tournament: TournamentDto,
    entry: LeaderboardEntryDto,
    highlighted: Boolean,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val catch = tournamentEntryCatch(tournament, entry)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = catch != null) { catch?.let(onOpenCatch) },
        shape = RoundedCornerShape(20.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.06f),
            ) {
                Text(
                    "#${entry.rank}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(entry.user ?: unknownUserLabel(strings), fontWeight = FontWeight.SemiBold)
                Text(
                    tournamentEntrySubtitle(strings, tournament, entry),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                entry.prize?.let { prize ->
                    PrizeChip(prize = prize)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    tournamentMetricValueLabel(tournament.metric, entry.value),
                    fontWeight = FontWeight.Bold,
                )
                if (highlighted) {
                    Text(
                        if (strings.login == "Логин") "Ваш результат" else "Your result",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrizeChip(prize: PrizeSpecDto) {
    Surface(
        color = Color(0x22FFD76A),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0x55FFD76A)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tournamentPrizeIconModel(prize)?.let { model ->
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            } ?: Text("🪙")
            Text(
                tournamentPrizeLabel(prize),
                color = Color(0xFFFFD76A),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatchDetailsDialog(
    strings: RiverStrings,
    catch: CatchDto,
    cardBytes: ByteArray?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val cardBitmap = remember(cardBytes) {
        cardBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
    }
    val accent = rarityColor(catch.rarity)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                ) {
                    locationBackgroundAsset(catch.location)?.let { background ->
                        AsyncImage(
                            model = background,
                            contentDescription = catch.location,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.20f),
                                        Color.Black.copy(alpha = 0.74f),
                                    )
                                )
                            )
                    )
                    if (cardBitmap != null) {
                        Image(
                            bitmap = cardBitmap,
                            contentDescription = catch.fish,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        color = accent.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            strings.rarityLabel(catch.rarity),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            catch.fish,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            catch.weight.asKgCompact(),
                            color = accent,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            catch.location,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (loading && cardBitmap == null) {
                        Text(strings.loading, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CatchDetailChip(catch.location)
                        catch.user?.takeIf { it.isNotBlank() }?.let { user ->
                            CatchDetailChip(user)
                        }
                        catch.at?.let { CatchDetailChip(formatTimestamp(it)) }
                        catch.rank?.let { CatchDetailChip("#$it") }
                        catch.prizeCoins?.let { CatchDetailChip("$it coins") }
                    }
                    Text(
                        "${strings.rarityLabel(catch.rarity)} • ${catch.weight.asKgCompact()} • ${catch.location}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(strings.continueLabel)
                        }
                        Button(
                            onClick = onShare,
                            enabled = catch.id > 0L,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(strings.shareCatch)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatchDetailChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
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

private fun tournamentFishLabel(strings: RiverStrings, tournament: TournamentDto): String = when {
    !tournament.fish.isNullOrBlank() -> tournament.fish
    !tournament.fishRarity.isNullOrBlank() ->
        if (strings.login == "Логин") "${strings.rarityLabel(tournament.fishRarity)} рыба" else "${strings.rarityLabel(tournament.fishRarity)} fish"
    else -> anyFishLabel(strings)
}

private fun tournamentMetricLabel(strings: RiverStrings, metric: String): String = when (metric) {
    "max_weight" -> if (strings.login == "Логин") "Лучший вес" else "Best weight"
    "min_weight" -> if (strings.login == "Логин") "Минимальный вес" else "Smallest weight"
    "count" -> if (strings.login == "Логин") "Количество" else "Count"
    "total_weight" -> if (strings.login == "Логин") "Суммарный вес" else "Total weight"
    else -> metric
}

private fun tournamentMetricValueLabel(metric: String, value: Double): String = when (metric) {
    "count" -> value.toInt().toString()
    else -> value.asKgCompact()
}

private fun tournamentEntryCatch(tournament: TournamentDto, entry: LeaderboardEntryDto): CatchDto? {
    if (tournament.metric == "count" || tournament.metric == "total_weight") return null
    val catchId = entry.catchId ?: return null
    val fish = entry.fish ?: return null
    val location = entry.location ?: return null
    return CatchDto(
        id = catchId,
        fish = fish,
        weight = entry.value,
        location = location,
        rarity = tournament.fishRarity.orEmpty(),
        user = entry.user,
        userId = entry.userId,
        at = entry.at?.let { Instant.ofEpochSecond(it).toString() },
    )
}

private fun tournamentEntrySubtitle(strings: RiverStrings, tournament: TournamentDto, entry: LeaderboardEntryDto): String =
    when {
        tournament.metric == "count" || tournament.metric == "total_weight" ->
            tournamentMetricLabel(strings, tournament.metric)
        else -> listOfNotNull(
            entry.fish ?: tournamentFishLabel(strings, tournament),
            entry.location,
            entry.at?.let { formatEpoch(it) },
        ).joinToString(" • ").ifBlank { unknownUserLabel(strings) }
    }

private fun tournamentPrizeLabel(prize: PrizeSpecDto): String = when {
    prize.packageId == "coins" || prize.coins != null -> "+${prize.coins ?: prize.qty}"
    prize.qty > 1 -> "${humanizePackId(prize.packageId)} x${prize.qty}"
    else -> humanizePackId(prize.packageId)
}

private fun tournamentPrizeIconModel(prize: PrizeSpecDto): String? = when {
    prize.packageId == "coins" || prize.coins != null -> null
    else -> shopIconAsset(prize.packageId)
}

private fun anyLocationLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Любая локация" else "Any location"

private fun anyFishLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Любая рыба" else "Any fish"

private fun unknownUserLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Неизвестно" else "Unknown"

private fun humanizePackId(packId: String): String = when {
    packId.startsWith("autofish") -> "Autofish"
    else -> packId.replace('_', ' ')
}

private fun phaseAccentColor(phase: FishingPhase): Color = when (phase) {
    FishingPhase.READY -> Color(0xFF4EA8DE)
    FishingPhase.COOLDOWN -> Color(0xFF607D8B)
    FishingPhase.WAITING_BITE -> Color(0xFF68D4FF)
    FishingPhase.BITING -> Color(0xFFFF8A5B)
    FishingPhase.TAP_CHALLENGE -> Color(0xFFFFD76A)
    FishingPhase.RESOLVING -> Color(0xFF9B8CFF)
}

private fun stringsArrow(): String = "›"

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
