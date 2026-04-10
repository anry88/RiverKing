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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.riverking.mobile.auth.LocationDto
import com.riverking.mobile.auth.MeResponseDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.PrizeSpecDto
import com.riverking.mobile.auth.QuestDto
import com.riverking.mobile.auth.QuestListDto
import com.riverking.mobile.auth.ReferralInfoDto
import com.riverking.mobile.auth.ReferralRewardDto
import com.riverking.mobile.auth.RodDto
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
    GUIDE(Icons.AutoMirrored.Rounded.MenuBook),
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
    onStartTelegramLink: () -> Unit,
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
                    me = me,
                    onStartTelegramLink = onStartTelegramLink,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderBar(
    me: MeResponseDto,
    strings: RiverStrings,
    achievementBadge: Boolean,
    onLogout: () -> Unit,
    onChangeLanguage: (String) -> Unit,
) {
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }?.name.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = me.username ?: strings.appTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentLocation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onLogout) { Text(strings.logout) }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(title = "🪙", value = me.coins.toString())
            StatPill(title = "kg", value = me.totalWeight.asKgCompact())
            StatPill(title = "🔥", value = me.dailyStreak.toString())
            LanguageToggle(currentLanguage = me.language, onChangeLanguage = onChangeLanguage)
        }
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
                    LoadingStatePanel(strings.loading)
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
                    EmptyStatePanel(strings.noData)
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
                    EmptyStatePanel(strings.noData)
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
                        GuideLocationRow(
                            strings = strings,
                            location = location,
                            ownedLocation = me.locations.firstOrNull { it.id == location.id },
                        )
                    } ?: EmptyStatePanel(strings.noData)
                }
                GuideSection.FISH -> SectionCard(strings.guideFish) {
                    val fishList = guide.guide?.fish.orEmpty()
                        .filter { rarityFilter == "all" || it.rarity == rarityFilter }
                        .filter { !showCaughtOnly || me.caughtFishIds.contains(it.id) }
                    if (fishList.isEmpty()) {
                        EmptyStatePanel(strings.noData)
                    } else {
                        fishList.forEachIndexed { index, fish ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            GuideFishRow(
                                strings = strings,
                                fish = fish,
                                discovered = me.caughtFishIds.contains(fish.id),
                            )
                        }
                    }
                }
                GuideSection.LURES -> SectionCard(strings.guideLures) {
                    guide.guide?.lures?.forEachIndexed { index, lure ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideLureRow(strings = strings, lure = lure)
                    } ?: EmptyStatePanel(strings.noData)
                }
                GuideSection.RODS -> SectionCard(strings.guideRods) {
                    guide.guide?.rods?.forEachIndexed { index, rod ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideRodRow(
                            strings = strings,
                            rod = rod,
                            ownedRod = me.rods.firstOrNull { it.code == rod.code },
                        )
                    } ?: EmptyStatePanel(strings.noData)
                }
                GuideSection.ACHIEVEMENTS -> SectionCard(strings.achievements) {
                    if (guide.achievements.isEmpty()) {
                        EmptyStatePanel(strings.noData)
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
                        EmptyStatePanel(strings.noData)
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
    me: MeResponseDto,
    onStartTelegramLink: () -> Unit,
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
            TelegramAccountCard(
                strings = strings,
                me = me,
                pending = state.telegramLinkPending,
                onStartTelegramLink = onStartTelegramLink,
            )
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
private fun TelegramAccountCard(
    strings: RiverStrings,
    me: MeResponseDto,
    pending: Boolean,
    onStartTelegramLink: () -> Unit,
) {
    SectionCard(strings.telegramAccount) {
        Text(
            if (me.telegramLinked) strings.telegramLinkedTitle else strings.telegramNotLinkedTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        me.telegramUsername?.takeIf { it.isNotBlank() }?.let { username ->
            Spacer(modifier = Modifier.height(8.dp))
            Text("@$username", color = MaterialTheme.colorScheme.secondary)
        }
        if (pending) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings.telegramLinkPending, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!me.telegramLinked) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartTelegramLink,
                enabled = !pending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.linkTelegram)
            }
        }
    }
}

@Composable
private fun HeroLocationCard(
    title: String,
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
                    contentScale = ContentScale.Crop,
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

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = null,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val currentRodCode = state.me?.rods?.firstOrNull { it.id == state.me?.currentRodId }?.code
            val rodUrl = rodAsset(currentRodCode)
            val rodTipAnchor = rodTipAnchorPercentage(currentRodCode)

            val rodImageRatio = 1536f / 1024f
            val rodWFrac = 0.65f
            val rodWidthDp = maxWidth * rodWFrac
            val rodHeightDp = rodWidthDp / rodImageRatio
            val rodLeftDp = maxWidth * 0.10f
            val rodTopDp = maxHeight - rodHeightDp + (maxHeight * 0.15f)

            coil.compose.AsyncImage(
                model = rodUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset(x = rodLeftDp, y = rodTopDp)
                    .width(rodWidthDp)
                    .height(rodHeightDp),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val waterTop = size.height * 0.48f

                val rodBase = Offset(
                    x = rodLeftDp.toPx() + rodWidthDp.toPx() * 0.383f,
                    y = rodTopDp.toPx() + rodHeightDp.toPx() * 0.998f
                )
                val rodTip = Offset(
                    x = rodLeftDp.toPx() + rodWidthDp.toPx() * rodTipAnchor.x,
                    y = rodTopDp.toPx() + rodHeightDp.toPx() * rodTipAnchor.y
                )
                
                val dx = rodTip.x - rodBase.x
                val dy = rodTip.y - rodBase.y
                val rodMid = Offset(rodBase.x + dx * 0.4f, rodBase.y + dy * 0.4f)

                val bobberOffset = if (inWater || phase == FishingPhase.RESOLVING) {
                    kotlin.math.sin(waveShift * (2f * kotlin.math.PI).toFloat()) * size.height * when (phase) {
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
                    y = kotlin.math.min(rodTip.y, bobber.y) + if (inWater) size.height * 0.10f else size.height * 0.18f,
                )
                val linePath = Path().apply {
                    moveTo(rodTip.x, rodTip.y)
                    quadraticTo(control.x, control.y, bobber.x, bobber.y)
                }
                drawPath(
                    path = linePath,
                    color = Color(0xFFF0E8D6),
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
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
        ) {
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
            EmptyStatePanel(strings.noData)
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
                LoadingStatePanel(strings.loading)
            } else if (quests == null) {
                EmptyStatePanel(strings.noData)
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
            EmptyStatePanel(strings.noData)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideLocationRow(
    strings: RiverStrings,
    location: GuideLocationDto,
    ownedLocation: LocationDto?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                locationBackgroundAsset(location.name)?.let { background ->
                    AsyncImage(
                        model = background,
                        contentDescription = location.name,
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
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Black.copy(alpha = 0.70f),
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        location.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CatchDetailChip(guideFishCountLabel(strings, location.fish.size))
                        CatchDetailChip(guideLureCountLabel(strings, location.lures.size))
                        CatchDetailChip(
                            when {
                                ownedLocation?.unlocked == true -> unlockedLabel(strings)
                                ownedLocation != null -> requiresKgLabel(strings, ownedLocation.unlockKg)
                                else -> strings.unavailable
                            }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.guideFish, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    location.fish.forEach { fish ->
                        GuideBadge(
                            label = fish.name,
                            accent = rarityColor(fish.rarity),
                        )
                    }
                }
                Text(strings.guideLures, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    location.lures.forEach { lure ->
                        GuideBadge(
                            label = lure,
                            accent = lureAccentColor(lure),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideFishRow(
    strings: RiverStrings,
    fish: GuideFishDto,
    discovered: Boolean,
) {
    val accent = rarityColor(fish.rarity)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.42f),
                                accent.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (discovered) fish.name.take(1) else "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (discovered) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (discovered) fish.name else "?????",
                            fontWeight = FontWeight.Bold,
                            color = if (discovered) accent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (discovered) strings.rarityLabel(fish.rarity) else catchToLearnLabel(strings),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    GuideBadge(
                        label = if (discovered) discoveredLabel(strings) else hiddenLabel(strings),
                        accent = if (discovered) Color(0xFF7CE38B) else Color(0xFF7A7F87),
                    )
                }
                if (discovered) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        fish.locations.forEach { place ->
                            GuideBadge(label = place, accent = Color(0xFF68D4FF))
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        fish.lures.forEach { lure ->
                            GuideBadge(label = lure, accent = lureAccentColor(lure))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideLureRow(strings: RiverStrings, lure: GuideLureDto) {
    val accent = lureAccentColor(lure.name)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(lure.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${guideFishCountLabel(strings, lure.fish.size)} • ${guideLocationCountLabel(strings, lure.locations.size)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                GuideBadge(label = strings.bait, accent = accent)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lure.fish.forEach { fish ->
                    GuideBadge(label = fish.name, accent = rarityColor(fish.rarity))
                }
            }
            Text(
                lure.locations.joinToString(" • "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GuideRodRow(
    strings: RiverStrings,
    rod: GuideRodDto,
    ownedRod: RodDto?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, Color(0xFFC9A46E).copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = rodImageAsset(rod.code),
                contentDescription = rod.name,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(rod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                GuideBadge(
                    label = when {
                        ownedRod?.unlocked == true -> unlockedLabel(strings)
                        ownedRod?.priceStars != null ->
                            if (strings.login == "Логин") "Откроется на ${rod.unlockKg.asGuideKg()} • ${ownedRod.priceStars}★" else "Unlocks at ${rod.unlockKg.asGuideKg()} • ${ownedRod.priceStars}★"
                        else -> requiresKgLabel(strings, rod.unlockKg)
                    },
                    accent = if (ownedRod?.unlocked == true) Color(0xFF7CE38B) else Color(0xFFC9A46E),
                )
                Text(
                    strings.rodBonusLabel(rod.bonusWater, rod.bonusPredator),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementRow(strings: RiverStrings, achievement: AchievementDto, onClaimAchievement: (String) -> Unit) {
    val ratio = (achievement.progress / achievement.target.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    val accent = if (achievement.claimable) Color(0xFFFFD76A) else Color(0xFF7EA8FF)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = achievementArtAsset(achievement.code, achievement.levelIndex),
                contentDescription = achievement.name,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(achievement.name, fontWeight = FontWeight.Bold)
                        Text(achievement.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    GuideBadge(label = achievement.level, accent = accent)
                }
                Text(
                    "${achievement.progressLabel}/${achievement.targetLabel}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
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
                            .background(accent),
                    )
                }
                if (achievement.claimable) {
                    Button(onClick = { onClaimAchievement(achievement.code) }, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.claim)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBadge(label: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.15f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                    } else {
                        val fishAsset = FISH_ASSET_MAP[catch.fish]?.let { assetUrl(it) }
                        if (fishAsset != null) {
                            AsyncImage(
                                model = fishAsset,
                                contentDescription = catch.fish,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(bottom = 50.dp)
                                    .size(160.dp)
                                    .align(Alignment.Center)
                            )
                        } else if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
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
private fun LoadingStatePanel(text: String, modifier: Modifier = Modifier) {
    StatusPanel(text = text, modifier = modifier, accent = Color(0xFF68D4FF))
}

@Composable
private fun EmptyStatePanel(text: String, modifier: Modifier = Modifier) {
    StatusPanel(text = text, modifier = modifier, accent = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun StatusPanel(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
            )
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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

private fun lureAccentColor(name: String): Color =
    if (name.contains("+")) Color(0xFFFFD76A) else Color(0xFF8FE388)

private fun rodImageAsset(code: String): String = assetUrl(
    when (code) {
        "spark" -> "/app/assets/rods/yellow_rod.png"
        "dew" -> "/app/assets/rods/green_rod.png"
        "stream" -> "/app/assets/rods/blue_rod.png"
        "abyss" -> "/app/assets/rods/black_rod.png"
        "storm" -> "/app/assets/rods/silver_rod.png"
        else -> "/app/assets/rods/yellow_rod.png"
    }
)

private fun achievementArtAsset(code: String, levelIndex: Int): String {
    val suffix = when (levelIndex.coerceIn(0, 4)) {
        0 -> "grey"
        1 -> "bronze"
        2 -> "silver"
        3 -> "gold"
        else -> "platinum"
    }
    val base = if (code.endsWith("_all_fish")) {
        "explorer_${code.removeSuffix("_all_fish")}"
    } else {
        code
    }
    return assetUrl("/app/assets/achievements/${base}_$suffix.png")
}

private fun guideFishCountLabel(strings: RiverStrings, count: Int): String =
    if (strings.login == "Логин") "$count рыб" else "$count fish"

private fun guideLureCountLabel(strings: RiverStrings, count: Int): String =
    if (strings.login == "Логин") "$count наживок" else "$count lures"

private fun guideLocationCountLabel(strings: RiverStrings, count: Int): String =
    if (strings.login == "Логин") "$count локаций" else "$count locations"

private fun unlockedLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Открыто" else "Unlocked"

private fun requiresKgLabel(strings: RiverStrings, unlockKg: Double): String =
    if (strings.login == "Логин") "Откроется на ${unlockKg.asGuideKg()}" else "Unlock at ${unlockKg.asGuideKg()}"

private fun catchToLearnLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Поймайте, чтобы открыть" else "Catch it to reveal details"

private fun discoveredLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Открыто" else "Discovered"

private fun hiddenLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Скрыто" else "Hidden"

private fun Double.asGuideKg(): String = String.format(Locale.US, "%.0f kg", this)

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

private fun rodAsset(code: String?): String {
    val path = when (code) {
        "spark" -> "/app/assets/rods/yellow_rod.png"
        "dew" -> "/app/assets/rods/green_rod.png"
        "stream" -> "/app/assets/rods/blue_rod.png"
        "abyss" -> "/app/assets/rods/black_rod.png"
        "storm" -> "/app/assets/rods/silver_rod.png"
        else -> "/app/assets/rods/yellow_rod.png"
    }
    return assetUrl(path)
}

private fun rodTipAnchorPercentage(code: String?): androidx.compose.ui.geometry.Offset {
    return when (code) {
        "spark" -> androidx.compose.ui.geometry.Offset(0.078f, 0.048f)
        "dew" -> androidx.compose.ui.geometry.Offset(0.13607f, 0.06641f)
        "stream" -> androidx.compose.ui.geometry.Offset(0.155f, 0.07422f)
        "abyss" -> androidx.compose.ui.geometry.Offset(0.14844f, 0.04688f)
        "storm" -> androidx.compose.ui.geometry.Offset(0.125f, 0.07520f)
        else -> androidx.compose.ui.geometry.Offset(0.07878f, 0.04785f)
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

private val FISH_ASSET_MAP = mapOf(
    "Плотва" to "/app/assets/fish/plotva.png",
    "Roach" to "/app/assets/fish/plotva.png",
    "Окунь" to "/app/assets/fish/okun.png",
    "Perch" to "/app/assets/fish/okun.png",
    "Карась" to "/app/assets/fish/karas.png",
    "Crucian Carp" to "/app/assets/fish/karas.png",
    "Лещ" to "/app/assets/fish/lesch.png",
    "Bream" to "/app/assets/fish/lesch.png",
    "Щука" to "/app/assets/fish/schuka.png",
    "Pike" to "/app/assets/fish/schuka.png",
    "Карп" to "/app/assets/fish/karp.png",
    "Carp" to "/app/assets/fish/karp.png",
    "Сом европейский" to "/app/assets/fish/som.png",
    "European Catfish" to "/app/assets/fish/som.png",
    "Осётр европейский" to "/app/assets/fish/osetr.png",
    "European Sturgeon" to "/app/assets/fish/osetr.png",
    "Уклейка" to "/app/assets/fish/ukleyka.png",
    "Bleak" to "/app/assets/fish/ukleyka.png",
    "Линь" to "/app/assets/fish/lin.png",
    "Tench" to "/app/assets/fish/lin.png",
    "Ротан" to "/app/assets/fish/rotan.png",
    "Rotan" to "/app/assets/fish/rotan.png",
    "Судак" to "/app/assets/fish/sudak.png",
    "Zander" to "/app/assets/fish/sudak.png",
    "Чехонь" to "/app/assets/fish/chehon.png",
    "Sabrefish" to "/app/assets/fish/chehon.png",
    "Хариус" to "/app/assets/fish/harius.png",
    "Grayling" to "/app/assets/fish/harius.png",
    "Форель ручьевая" to "/app/assets/fish/forel_ruchevaya.png",
    "Brook Trout" to "/app/assets/fish/forel_ruchevaya.png",
    "Таймень" to "/app/assets/fish/taymen.png",
    "Taimen" to "/app/assets/fish/taymen.png",
    "Налим" to "/app/assets/fish/nalim.png",
    "Burbot" to "/app/assets/fish/nalim.png",
    "Сиг обыкновенный" to "/app/assets/fish/sig.png",
    "Common Whitefish" to "/app/assets/fish/sig.png",
    "Голавль" to "/app/assets/fish/golavl.png",
    "Chub" to "/app/assets/fish/golavl.png",
    "Жерех" to "/app/assets/fish/zhereh.png",
    "Asp" to "/app/assets/fish/zhereh.png",
    "Толстолобик" to "/app/assets/fish/tolstolobik.png",
    "Bighead Carp" to "/app/assets/fish/tolstolobik.png",
    "Амур белый" to "/app/assets/fish/beliy_amur.png",
    "Grass Carp" to "/app/assets/fish/beliy_amur.png",
    "Угорь европейский" to "/app/assets/fish/ugor_evropeyskiy.png",
    "European Eel" to "/app/assets/fish/ugor_evropeyskiy.png",
    "Стерлядь" to "/app/assets/fish/sterlyad.png",
    "Sterlet" to "/app/assets/fish/sterlyad.png",
    "Кефаль-лобан" to "/app/assets/fish/kefal.png",
    "Flathead Grey Mullet" to "/app/assets/fish/kefal.png",
    "Камбала морская" to "/app/assets/fish/kambala.png",
    "Sea Flounder" to "/app/assets/fish/kambala.png",
    "Сельдь" to "/app/assets/fish/seld.png",
    "Herring" to "/app/assets/fish/seld.png",
    "Ставрида" to "/app/assets/fish/stavrida.png",
    "Horse Mackerel" to "/app/assets/fish/stavrida.png",
    "Тихоокеанский клювач" to "/app/assets/fish/klyuvach_pacificocean.png",
    "Pacific Snipefish" to "/app/assets/fish/klyuvach_pacificocean.png",
    "Треска" to "/app/assets/fish/treska.png",
    "Cod" to "/app/assets/fish/treska.png",
    "Сайда" to "/app/assets/fish/sayda.png",
    "Pollock" to "/app/assets/fish/sayda.png",
    "Мерланг" to "/app/assets/fish/merlang.png",
    "Whiting" to "/app/assets/fish/merlang.png",
    "Форель морская" to "/app/assets/fish/morskaya_forel.png",
    "Sea Trout" to "/app/assets/fish/morskaya_forel.png",
    "Палтус" to "/app/assets/fish/paltus.png",
    "Halibut" to "/app/assets/fish/paltus.png",
    "Корюшка" to "/app/assets/fish/koryushka.png",
    "Smelt" to "/app/assets/fish/koryushka.png",
    "Лосось атлантический" to "/app/assets/fish/losos_atlanticheskiy.png",
    "Atlantic Salmon" to "/app/assets/fish/losos_atlanticheskiy.png",
    "Лаврак" to "/app/assets/fish/lavrak.png",
    "Sea Bass" to "/app/assets/fish/lavrak.png",
    "Скумбрия атлантическая" to "/app/assets/fish/skumbriya_atlanticheskaya.png",
    "Atlantic Mackerel" to "/app/assets/fish/skumbriya_atlanticheskaya.png",
    "Горбуша" to "/app/assets/fish/gorbusha.png",
    "Pink Salmon" to "/app/assets/fish/gorbusha.png",
    "Кета" to "/app/assets/fish/keta.png",
    "Chum Salmon" to "/app/assets/fish/keta.png",
    "Белуга" to "/app/assets/fish/beluga.png",
    "Beluga" to "/app/assets/fish/beluga.png",
    "Ёрш" to "/app/assets/fish/yorsh.png",
    "Ruffe" to "/app/assets/fish/yorsh.png",
    "Берш" to "/app/assets/fish/bersh.png",
    "Volga Pikeperch" to "/app/assets/fish/bersh.png",
    "Пескарь" to "/app/assets/fish/peskar.png",
    "Gudgeon" to "/app/assets/fish/peskar.png",
    "Густера" to "/app/assets/fish/gustera.png",
    "Blue Bream" to "/app/assets/fish/gustera.png",
    "Краснопёрка" to "/app/assets/fish/krasnopyorka.png",
    "Rudd" to "/app/assets/fish/krasnopyorka.png",
    "Елец" to "/app/assets/fish/elets.png",
    "Dace" to "/app/assets/fish/elets.png",
    "Верхоплавка" to "/app/assets/fish/verhoplavka.png",
    "Topmouth Gudgeon" to "/app/assets/fish/verhoplavka.png",
    "Вьюн" to "/app/assets/fish/vyun.png",
    "Weather Loach" to "/app/assets/fish/vyun.png",
    "Вобла" to "/app/assets/fish/vobla.png",
    "Caspian Roach" to "/app/assets/fish/vobla.png",
    "Гольян" to "/app/assets/fish/golyan.png",
    "Minnow" to "/app/assets/fish/golyan.png",
    "Язь" to "/app/assets/fish/yaz.png",
    "Ide" to "/app/assets/fish/yaz.png",
    "Бычок-кругляк" to "/app/assets/fish/bychyok.png",
    "Round Goby" to "/app/assets/fish/bychyok.png",
    "Килька" to "/app/assets/fish/kilka.png",
    "Sprat" to "/app/assets/fish/kilka.png",
    "Мойва" to "/app/assets/fish/mojva.png",
    "Capelin" to "/app/assets/fish/mojva.png",
    "Тарань" to "/app/assets/fish/taran.png",
    "Black Sea Roach" to "/app/assets/fish/taran.png",
    "Сардина" to "/app/assets/fish/sardina.png",
    "Sardine" to "/app/assets/fish/sardina.png",
    "Анчоус европейский" to "/app/assets/fish/anchous.png",
    "European Anchovy" to "/app/assets/fish/anchous.png",
    "Дорадо" to "/app/assets/fish/dorado.png",
    "Dorado" to "/app/assets/fish/dorado.png",
    "Ваху" to "/app/assets/fish/vahu.png",
    "Wahoo" to "/app/assets/fish/vahu.png",
    "Парусник" to "/app/assets/fish/parusnik.png",
    "Sailfish" to "/app/assets/fish/parusnik.png",
    "Рыба-меч" to "/app/assets/fish/ryba_mech.png",
    "Swordfish" to "/app/assets/fish/ryba_mech.png",
    "Марлин синий" to "/app/assets/fish/marlin_siniy.png",
    "Blue Marlin" to "/app/assets/fish/marlin_siniy.png",
    "Тунец синеперый" to "/app/assets/fish/tunets_sineperiy.png",
    "Bluefin Tuna" to "/app/assets/fish/tunets_sineperiy.png",
    "Акула мако" to "/app/assets/fish/akula_mako.png",
    "Mako Shark" to "/app/assets/fish/akula_mako.png",
    "Катран обыкновенный" to "/app/assets/fish/katran_simple.png",
    "Spiny Dogfish" to "/app/assets/fish/katran_simple.png",
    "Альбакор" to "/app/assets/fish/albakor.png",
    "Albacore" to "/app/assets/fish/albakor.png",
    "Голец арктический" to "/app/assets/fish/golets_arkticheskiy.png",
    "Arctic Char" to "/app/assets/fish/golets_arkticheskiy.png",
    "Форель кумжа" to "/app/assets/fish/forel_kumzha.png",
    "Brown Trout" to "/app/assets/fish/forel_kumzha.png",
    "Пикша" to "/app/assets/fish/piksha.png",
    "Haddock" to "/app/assets/fish/piksha.png",
    "Тюрбо" to "/app/assets/fish/tyurbo.png",
    "Turbot" to "/app/assets/fish/tyurbo.png",
    "Сайра" to "/app/assets/fish/sayra.png",
    "Pacific Saury" to "/app/assets/fish/sayra.png",
    "Летучая рыба" to "/app/assets/fish/letuchaya_ryba.png",
    "Flying Fish" to "/app/assets/fish/letuchaya_ryba.png",
    "Рыба-луна" to "/app/assets/fish/ryba_luna.png",
    "Ocean Sunfish" to "/app/assets/fish/ryba_luna.png",
    "Сельдяной король" to "/app/assets/fish/seldyanoy_korol.png",
    "Oarfish" to "/app/assets/fish/seldyanoy_korol.png",
    "Паку бурый" to "/app/assets/fish/tambaki.png",
    "Brown Pacu" to "/app/assets/fish/tambaki.png",
    "Паку краснобрюхий" to "/app/assets/fish/paku_krasnobryuhiy.png",
    "Red-bellied Pacu" to "/app/assets/fish/paku_krasnobryuhiy.png",
    "Паку чёрный" to "/app/assets/fish/paku_cherniy.png",
    "Black Pacu" to "/app/assets/fish/paku_cherniy.png",
    "Прохилодус красноштриховый" to "/app/assets/fish/prohilodus.png",
    "Stripetail Prochilodus" to "/app/assets/fish/prohilodus.png",
    "Лепоринус полосатый" to "/app/assets/fish/leporinus_polosatiy.png",
    "Banded Leporinus" to "/app/assets/fish/leporinus_polosatiy.png",
    "Метиннис серебристый" to "/app/assets/fish/metinnis_serebristiy.png",
    "Silver Dollar" to "/app/assets/fish/metinnis_serebristiy.png",
    "Пелядь" to "/app/assets/fish/pelyad.png",
    "Peled Whitefish" to "/app/assets/fish/pelyad.png",
    "Омуль арктический" to "/app/assets/fish/omul_arkticheskiy.png",
    "Arctic Omul" to "/app/assets/fish/omul_arkticheskiy.png",
    "Муксун" to "/app/assets/fish/muksun.png",
    "Muksun Whitefish" to "/app/assets/fish/muksun.png",
    "Анциструс обыкновенный" to "/app/assets/fish/ancistrus.png",
    "Common Bristlenose" to "/app/assets/fish/ancistrus.png",
    "Птеригоплихт парчовый" to "/app/assets/fish/pterigopliht_parchoviy.png",
    "Sailfin Pleco" to "/app/assets/fish/pterigopliht_parchoviy.png",
    "Отоцинклюс широкополосый" to "/app/assets/fish/otocinklyus.png",
    "Banded Otocinclus" to "/app/assets/fish/otocinklyus.png",
    "Карнегиелла мраморная" to "/app/assets/fish/karnegiella_mramornaya.png",
    "Marbled Hatchetfish" to "/app/assets/fish/karnegiella_mramornaya.png",
    "Тетра неоновая" to "/app/assets/fish/tetra_neonovaya.png",
    "Neon Tetra" to "/app/assets/fish/tetra_neonovaya.png",
    "Тернеция чёрная" to "/app/assets/fish/tetra_chernaya.png",
    "Black Skirt Tetra" to "/app/assets/fish/tetra_chernaya.png",
    "Рыба-лист амазонская" to "/app/assets/fish/ryba_list_amazonskaya.png",
    "Amazon Leaf Fish" to "/app/assets/fish/ryba_list_amazonskaya.png",
    "Арапайма" to "/app/assets/fish/arapayma.png",
    "Arapaima" to "/app/assets/fish/arapayma.png",
    "Ленок" to "/app/assets/fish/lenok.png",
    "Lenok Trout" to "/app/assets/fish/lenok.png",
    "Пиранья краснобрюхая" to "/app/assets/fish/piranya_krasnopuzaya.png",
    "Red-bellied Piranha" to "/app/assets/fish/piranya_krasnopuzaya.png",
    "Бикуда" to "/app/assets/fish/bikuda.png",
    "Bicuda" to "/app/assets/fish/bikuda.png",
    "Угорь электрический" to "/app/assets/fish/ugor_elektricheskiy.png",
    "Electric Eel" to "/app/assets/fish/ugor_elektricheskiy.png",
    "Сом краснохвостый" to "/app/assets/fish/krasnohvostiy_som.png",
    "Redtail Catfish" to "/app/assets/fish/krasnohvostiy_som.png",
    "Пимелодус пятнистый" to "/app/assets/fish/pimelodus_pyatnistiy.png",
    "Spotted Pimelodus" to "/app/assets/fish/pimelodus_pyatnistiy.png",
    "Сом веслоносый" to "/app/assets/fish/som_veslonosiy.png",
    "Paddlefish" to "/app/assets/fish/som_veslonosiy.png",
    "Пираиба" to "/app/assets/fish/piraiba.png",
    "Piraiba" to "/app/assets/fish/piraiba.png",
    "Дискус обыкновенный" to "/app/assets/fish/diskus.png",
    "Common Discus" to "/app/assets/fish/diskus.png",
    "Скалярия альтум" to "/app/assets/fish/skalyaria_altum.png",
    "Altum Angelfish" to "/app/assets/fish/skalyaria_altum.png",
    "Скалярия обыкновенная" to "/app/assets/fish/skalyaria_common.png",
    "Freshwater Angelfish" to "/app/assets/fish/skalyaria_common.png",
    "Апистограмма Агассиза" to "/app/assets/fish/apistogramma_agassiza.png",
    "Agassiz's Cichlid" to "/app/assets/fish/apistogramma_agassiza.png",
    "Тетра кардинальная" to "/app/assets/fish/tetra_kardinal.png",
    "Cardinal Tetra" to "/app/assets/fish/tetra_kardinal.png",
    "Тетра лимонная" to "/app/assets/fish/tetra_limonnaya.png",
    "Lemon Tetra" to "/app/assets/fish/tetra_limonnaya.png",
    "Тетра огненная" to "/app/assets/fish/tetra_ognennaya.png",
    "Ember Tetra" to "/app/assets/fish/tetra_ognennaya.png",
    "Тетра пингвин" to "/app/assets/fish/tetra_pingvin.png",
    "Penguin Tetra" to "/app/assets/fish/tetra_pingvin.png",
    "Тетра родостомус" to "/app/assets/fish/tetra_rodostomus.png",
    "Rummy-nose Tetra" to "/app/assets/fish/tetra_rodostomus.png",
    "Тетра чёрный неон" to "/app/assets/fish/tetra_black_neon.png",
    "Black Neon Tetra" to "/app/assets/fish/tetra_black_neon.png",
    "Коридорас панда" to "/app/assets/fish/koridorus_panda.png",
    "Panda Cory" to "/app/assets/fish/koridorus_panda.png",
    "Коридорас Штерба" to "/app/assets/fish/koridoras_shterba.png",
    "Sterba's Corydoras" to "/app/assets/fish/koridoras_shterba.png",
    "Татия леопардовая" to "/app/assets/fish/tatiya_leopardovaya.png",
    "Leopard Tatia" to "/app/assets/fish/tatiya_leopardovaya.png",
    "Торакатум" to "/app/assets/fish/torakatum.png",
    "Hoplo Catfish" to "/app/assets/fish/torakatum.png",
    "Нанностомус трифасциатус" to "/app/assets/fish/nannostomus.png",
    "Three-Stripe Pencilfish" to "/app/assets/fish/nannostomus.png",
    "Нанностомус маргинатус" to "/app/assets/fish/nannostomus_marginatus.png",
    "Dwarf Pencilfish" to "/app/assets/fish/nannostomus_marginatus.png",
    "Рамирези" to "/app/assets/fish/ramirezi.png",
    "Ram Cichlid" to "/app/assets/fish/ramirezi.png",
    "Аравана чёрная" to "/app/assets/fish/aravana_chernaya.png",
    "Black Arowana" to "/app/assets/fish/aravana_chernaya.png",
    "Астронотус глазчатый" to "/app/assets/fish/oskar.png",
    "Oscar Cichlid" to "/app/assets/fish/oskar.png",
    "Аймара" to "/app/assets/fish/aymara.png",
    "Aimara" to "/app/assets/fish/aymara.png",
    "Псевдоплатистома тигровая" to "/app/assets/fish/surubin.png",
    "Tiger Shovelnose" to "/app/assets/fish/surubin.png",
    "Брахиплатистома тигровая" to "/app/assets/fish/brahiplatistoma_tigrovaya.png",
    "Tiger Catfish" to "/app/assets/fish/brahiplatistoma_tigrovaya.png",
    "Пиранья чёрная" to "/app/assets/fish/piranya_chernaya.png",
    "Black Piranha" to "/app/assets/fish/piranya_chernaya.png",
    "Паяра" to "/app/assets/fish/payara.png",
    "Payara" to "/app/assets/fish/payara.png",
    "Мечерот обыкновенный" to "/app/assets/fish/mecherot_common.png",
    "Common Freshwater Barracuda" to "/app/assets/fish/mecherot_common.png",
    "Мечерот пятнистый" to "/app/assets/fish/mecherot_pyatnistiy.png",
    "Spotted Freshwater Barracuda" to "/app/assets/fish/mecherot_pyatnistiy.png",
    "Гимнотус угревидный" to "/app/assets/fish/gimnotus_ugrevidniy.png",
    "Banded Knifefish" to "/app/assets/fish/gimnotus_ugrevidniy.png",
    "Нож-рыба чёрная" to "/app/assets/fish/ryba_nozh_chernaya.png",
    "Black Ghost Knifefish" to "/app/assets/fish/ryba_nozh_chernaya.png",
    "Щучья цихлида" to "/app/assets/fish/schuchya_cihlida.png",
    "Pike Cichlid" to "/app/assets/fish/schuchya_cihlida.png",
    "Павлиний окунь" to "/app/assets/fish/pavliniy_okun.png",
    "Peacock Bass" to "/app/assets/fish/pavliniy_okun.png",
    "Цихлазома мезонаута" to "/app/assets/fish/cihlazoma_mezonauta.png",
    "Flag Cichlid" to "/app/assets/fish/cihlazoma_mezonauta.png",
    "Цихлазома северум" to "/app/assets/fish/cihlazoma_severum.png",
    "Severum Cichlid" to "/app/assets/fish/cihlazoma_severum.png",
    "Молочная рыба" to "/app/assets/fish/molochnaya_ryba.png",
    "Milkfish" to "/app/assets/fish/molochnaya_ryba.png",
    "Кефаль пятнистая" to "/app/assets/fish/kefal_pyatnistaya.png",
    "Spotted Mullet" to "/app/assets/fish/kefal_pyatnistaya.png",
    "Тиляпия мозамбикская" to "/app/assets/fish/tilyapiya_mozambikskaya.png",
    "Mozambique Tilapia" to "/app/assets/fish/tilyapiya_mozambikskaya.png",
    "Анчоус тропический" to "/app/assets/fish/anchous_tropicheskiy.png",
    "Tropical Anchovy" to "/app/assets/fish/anchous_tropicheskiy.png",
    "Сардина индийская" to "/app/assets/fish/sardina_indiyskaya.png",
    "Indian Sardine" to "/app/assets/fish/sardina_indiyskaya.png",
    "Сиган золотистый" to "/app/assets/fish/zolotistiy_shiponog.png",
    "Golden Rabbitfish" to "/app/assets/fish/zolotistiy_shiponog.png",
    "Бычок-пчёлка" to "/app/assets/fish/bychok_mangroviy.png",
    "Bumblebee Goby" to "/app/assets/fish/bychok_mangroviy.png",
    "Баррамунди" to "/app/assets/fish/barramundi.png",
    "Barramundi" to "/app/assets/fish/barramundi.png",
    "Снук" to "/app/assets/fish/snuk.png",
    "Snook" to "/app/assets/fish/snuk.png",
    "Луциан мангровый" to "/app/assets/fish/mangroviy_snapper.png",
    "Mangrove Snapper" to "/app/assets/fish/mangroviy_snapper.png",
    "Тарпон" to "/app/assets/fish/tarpon.png",
    "Tarpon" to "/app/assets/fish/tarpon.png",
    "Сом морской" to "/app/assets/fish/morskoy_som.png",
    "Sea Catfish" to "/app/assets/fish/morskoy_som.png",
    "Сарган морской" to "/app/assets/fish/morskoy_sargan.png",
    "Needlefish" to "/app/assets/fish/morskoy_sargan.png",
    "Каранкс голубой" to "/app/assets/fish/goluboy_trevalli.png",
    "Blue Trevally" to "/app/assets/fish/goluboy_trevalli.png",
    "Рыба-попугай" to "/app/assets/fish/ryba_popugay.png",
    "Parrotfish" to "/app/assets/fish/ryba_popugay.png",
    "Ангел императорский" to "/app/assets/fish/angel_imperatorskiy.png",
    "Emperor Angelfish" to "/app/assets/fish/angel_imperatorskiy.png",
    "Хирург голубой" to "/app/assets/fish/hirurg_goluboy.png",
    "Blue Tang" to "/app/assets/fish/hirurg_goluboy.png",
    "Бабочка нитеносная" to "/app/assets/fish/babochka_klinopolosaya.png",
    "Threadfin Butterflyfish" to "/app/assets/fish/babochka_klinopolosaya.png",
    "Хризиптера синяя" to "/app/assets/fish/damsel_siniy.png",
    "Blue Damselfish" to "/app/assets/fish/damsel_siniy.png",
    "Фузилёр жёлтохвостый" to "/app/assets/fish/fuziler_zheltohvostiy.png",
    "Yellowtail Fusilier" to "/app/assets/fish/fuziler_zheltohvostiy.png",
    "Барабулька тропическая" to "/app/assets/fish/barabulka_tropicheskaya.png",
    "Tropical Goatfish" to "/app/assets/fish/barabulka_tropicheskaya.png",
    "Барракуда большая" to "/app/assets/fish/barrakuda_bolschaya.png",
    "Great Barracuda" to "/app/assets/fish/barrakuda_bolschaya.png",
    "Конгер" to "/app/assets/fish/konger.png",
    "Conger Eel" to "/app/assets/fish/konger.png",
    "Каранкс гигантский" to "/app/assets/fish/gigantskiy_karanks.png",
    "Giant Trevally" to "/app/assets/fish/gigantskiy_karanks.png",
    "Пермит" to "/app/assets/fish/permit.png",
    "Permit" to "/app/assets/fish/permit.png",
    "Альбула" to "/app/assets/fish/kostlyavaya_ryba.png",
    "Bonefish" to "/app/assets/fish/kostlyavaya_ryba.png",
    "Скумбрия испанская" to "/app/assets/fish/ispanskaya_makrel.png",
    "Spanish Mackerel" to "/app/assets/fish/ispanskaya_makrel.png",
    "Группер коралловый" to "/app/assets/fish/koralloviy_grupper.png",
    "Coral Grouper" to "/app/assets/fish/koralloviy_grupper.png",
    "Спинорог-титан" to "/app/assets/fish/spinorog_titan.png",
    "Titan Triggerfish" to "/app/assets/fish/spinorog_titan.png",
    "Карп кои (Кохаку)" to "/app/assets/fish/koi_kohaku.png",
    "Koi (Kohaku)" to "/app/assets/fish/koi_kohaku.png",
    "Карп кои (Тайсё Сансёку)" to "/app/assets/fish/koi_taisho_sanke.png",
    "Koi (Taisho Sanshoku)" to "/app/assets/fish/koi_taisho_sanke.png",
    "Карп кои (Сёва Сансёку)" to "/app/assets/fish/koi_showa_sanshoku.png",
    "Koi (Showa Sanshoku)" to "/app/assets/fish/koi_showa_sanshoku.png",
    "Карп кои (Уцуримоно)" to "/app/assets/fish/koi_utsurimono.png",
    "Koi (Utsurimono)" to "/app/assets/fish/koi_utsurimono.png",
    "Карп кои (Бэкко)" to "/app/assets/fish/koi_bekko.png",
    "Koi (Bekko)" to "/app/assets/fish/koi_bekko.png",
    "Карп кои (Тантё)" to "/app/assets/fish/koi_tancho.png",
    "Koi (Tancho)" to "/app/assets/fish/koi_tancho.png",
    "Карп кои (Асаги)" to "/app/assets/fish/koi_asagi.png",
    "Koi (Asagi)" to "/app/assets/fish/koi_asagi.png",
    "Карп кои (Сюсуй)" to "/app/assets/fish/koi_shusui.png",
    "Koi (Shusui)" to "/app/assets/fish/koi_shusui.png",
    "Карп кои (Коромо)" to "/app/assets/fish/koi_koromo.png",
    "Koi (Koromo)" to "/app/assets/fish/koi_koromo.png",
    "Карп кои (Кингинрин)" to "/app/assets/fish/koi_kinginrin.png",
    "Koi (Kinginrin)" to "/app/assets/fish/koi_kinginrin.png",
    "Карп кои (Каваримоно)" to "/app/assets/fish/koi_kawarimono.png",
    "Koi (Kawarimono)" to "/app/assets/fish/koi_kawarimono.png",
    "Карп кои (Огон)" to "/app/assets/fish/koi_ogon.png",
    "Koi (Ogon)" to "/app/assets/fish/koi_ogon.png",
    "Карп кои (Хикари-моёмоно)" to "/app/assets/fish/koi_hikari_moyomono.png",
    "Koi (Hikari Moyomono)" to "/app/assets/fish/koi_hikari_moyomono.png",
    "Карп кои (Госики)" to "/app/assets/fish/koi_goshiki.png",
    "Koi (Goshiki)" to "/app/assets/fish/koi_goshiki.png",
    "Карп кои (Кумонрю)" to "/app/assets/fish/koi_kumonryu.png",
    "Koi (Kumonryu)" to "/app/assets/fish/koi_kumonryu.png",
    "Карп кои (Дойцу-гои)" to "/app/assets/fish/koi_doitsu.png",
    "Koi (Doitsu-goi)" to "/app/assets/fish/koi_doitsu.png",
    "Амур чёрный" to "/app/assets/fish/cherniy_amur.png",
    "Black Amur" to "/app/assets/fish/cherniy_amur.png",
    "Змееголов северный" to "/app/assets/fish/zmeegolov_severniy.png",
    "Northern Snakehead" to "/app/assets/fish/zmeegolov_severniy.png",
    "Щука амурская" to "/app/assets/fish/amurskaya_schuka.png",
    "Amur Pike" to "/app/assets/fish/amurskaya_schuka.png",
    "Кристивомер" to "/app/assets/fish/kristivomer.png",
    "Lake Trout" to "/app/assets/fish/kristivomer.png",
    "Лосось дунайский" to "/app/assets/fish/dunaiskiy_losos.png",
    "Danube Salmon (Huchen)" to "/app/assets/fish/dunaiskiy_losos.png",
    "Зунгаро" to "/app/assets/fish/zungaro.png",
    "Zungaro Catfish" to "/app/assets/fish/zungaro.png",
    "Скат моторо" to "/app/assets/fish/skat_motoro.png",
    "Motoro Stingray" to "/app/assets/fish/skat_motoro.png",
    "Пеленгас" to "/app/assets/fish/pelengas.png",
    "Pelingas Mullet" to "/app/assets/fish/pelengas.png",
    "Вырезуб" to "/app/assets/fish/vyrezub.png",
    "Black Sea Shemaya" to "/app/assets/fish/vyrezub.png",
    "Кубера" to "/app/assets/fish/kubera.png",
    "Cubera Snapper" to "/app/assets/fish/kubera.png",
    "Мурена европейская" to "/app/assets/fish/murena_european.png",
    "European Moray" to "/app/assets/fish/murena_european.png",
    "Мурена звёздчатая" to "/app/assets/fish/murena_zvezdchataya.png",
    "Starry Moray" to "/app/assets/fish/murena_zvezdchataya.png",
    "Мурена гигантская" to "/app/assets/fish/murena_gigantskaya.png",
    "Giant Moray" to "/app/assets/fish/murena_gigantskaya.png",
    "Зубатка пятнистая" to "/app/assets/fish/zubatka_pyatnistaya.png",
    "Spotted Wolffish" to "/app/assets/fish/zubatka_pyatnistaya.png",
    "Тунец желтоперый" to "/app/assets/fish/tunec_zeltoperiy.png",
    "Yellowfin Tuna" to "/app/assets/fish/tunec_zeltoperiy.png",
    "Снук чёрный" to "/app/assets/fish/chyorniy_snuk.png",
    "Black Snook" to "/app/assets/fish/chyorniy_snuk.png",
    "Рыба-наполеон" to "/app/assets/fish/ryba_napoleon.png",
    "Napoleon Wrasse" to "/app/assets/fish/ryba_napoleon.png",
    "Рыба-клоун" to "/app/assets/fish/ryba_kloun.png",
    "Clownfish" to "/app/assets/fish/ryba_kloun.png",
    "Кефаль мангровая" to "/app/assets/fish/kefal_mangrovaya.png",
    "Mangrove Mullet" to "/app/assets/fish/kefal_mangrovaya.png",
    "Сельдь тихоокеанская" to "/app/assets/fish/seld_tihookeanskaya.png",
    "Pacific Herring" to "/app/assets/fish/seld_tihookeanskaya.png",
    "Хромис рифовый" to "/app/assets/fish/hromis_rifoviy.png",
    "Reef Chromis" to "/app/assets/fish/hromis_rifoviy.png",
    "Дамсел жёлтохвостый" to "/app/assets/fish/damsel_zheltohvostiy.png",
    "Yellowtail Damselfish" to "/app/assets/fish/damsel_zheltohvostiy.png",
    "Морской конёк" to "/app/assets/fish/morskoy_konek.png",
    "Seahorse" to "/app/assets/fish/morskoy_konek.png",
    "Идол мавританский" to "/app/assets/fish/idol_mavritanskiy.png",
    "Moorish Idol" to "/app/assets/fish/idol_mavritanskiy.png",
    "Рыба-бабочка полосатая" to "/app/assets/fish/ryba_babochka_polosataya.png",
    "Striped Butterflyfish" to "/app/assets/fish/ryba_babochka_polosataya.png",
    "Гобиодон голубопятнистый" to "/app/assets/fish/gobiodon_golubopyatnistiy.png",
    "Bluespotted Goby" to "/app/assets/fish/gobiodon_golubopyatnistiy.png",
    "Сиган коричневопятнистый" to "/app/assets/fish/sigan_korichnevopyatnistiy.png",
    "Brown-spotted Rabbitfish" to "/app/assets/fish/sigan_korichnevopyatnistiy.png",
    "Хирург полосатый" to "/app/assets/fish/hirurg_polosatiy.png",
    "Striped Surgeonfish" to "/app/assets/fish/hirurg_polosatiy.png",
    "Луциан серебристо-пятнистый" to "/app/assets/fish/lucian_serebristo-pyatnistiy.png",
    "Silverspot Snapper" to "/app/assets/fish/lucian_serebristo-pyatnistiy.png",
    "Скорпена бородатая" to "/app/assets/fish/skorpena_borodataya.png",
    "Bearded Scorpionfish" to "/app/assets/fish/skorpena_borodataya.png",
    "Барракуда полосатая" to "/app/assets/fish/barrakuda_polosataya.png",
    "Striped Barracuda" to "/app/assets/fish/barrakuda_polosataya.png",
    "Каранкс шестиполосый" to "/app/assets/fish/karanks_polosatiy.png",
    "Sixband Trevally" to "/app/assets/fish/karanks_polosatiy.png",
    "Группер леопардовый коралловый" to "/app/assets/fish/grupper_leopardoviy_koralloviy.png",
    "Leopard Coral Grouper" to "/app/assets/fish/grupper_leopardoviy_koralloviy.png",
    "Иглорыл-агухон" to "/app/assets/fish/igloryl-aguhon.png",
    "Agujon Needlefish" to "/app/assets/fish/igloryl-aguhon.png",
    "Акула рифовая чёрнопёрая" to "/app/assets/fish/akula_rifovaya_chernoperaya.png",
    "Blacktip Reef Shark" to "/app/assets/fish/akula_rifovaya_chernoperaya.png",
    "Губан-чистильщик" to "/app/assets/fish/guban-chistilschik.png",
    "Cleaner Wrasse" to "/app/assets/fish/guban-chistilschik.png",
    "Сержант-майор атлантический" to "/app/assets/fish/sergant-major_atlanticheskiy.png",
    "Atlantic Sergeant Major" to "/app/assets/fish/sergant-major_atlanticheskiy.png",
    "Грамма королевская" to "/app/assets/fish/gramma_korolevskaya.png",
    "Royal Gramma" to "/app/assets/fish/gramma_korolevskaya.png",
    "Ангел королевский" to "/app/assets/fish/angel_korolevskiy.png",
    "Queen Angelfish" to "/app/assets/fish/angel_korolevskiy.png",
    "Мандариновая рыба" to "/app/assets/fish/mandarinivaya_ryba.png",
    "Mandarin Dragonet" to "/app/assets/fish/mandarinivaya_ryba.png",
    "Крылатка зебровая" to "/app/assets/fish/krylaka_zebrovaya.png",
    "Zebra Lionfish" to "/app/assets/fish/krylaka_zebrovaya.png",
    "Рыба-флейта" to "/app/assets/fish/ryba-fleita.png",
    "Trumpetfish" to "/app/assets/fish/ryba-fleita.png",
)
