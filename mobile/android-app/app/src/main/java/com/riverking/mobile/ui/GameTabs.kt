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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.AchievementClaimDto
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.CatchStatsDto
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
    onUpdateNickname: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onLoadCatchStats: (String) -> Unit,
) {
    val me = state.me ?: return
    val strings = rememberRiverStrings(me.language)
    val clipboard = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FISHING) }
    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var showCatchStats by rememberSaveable { mutableStateOf(false) }
    var showDailyRewardSheet by rememberSaveable { mutableStateOf(false) }

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
                onOpenDaily = { showDailyRewardSheet = true },
                onLogout = onLogout,
                onChangeLanguage = onChangeLanguage,
                onOpenNicknameChange = {
                    onUpdateNickname(me.username ?: "")
                    showNicknameDialog = true
                },
                onOpenCatchStats = {
                    showCatchStats = true
                    onLoadCatchStats("all")
                },
            )
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = RiverPanelRaised.copy(alpha = 0.97f),
                border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.72f)),
                shadowElevation = 16.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = Color.Transparent,
                ) {
                    MainTab.entries.forEach { tab ->
                        val showBadge = when (tab) {
                            MainTab.GUIDE -> achievementBadge
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (showBadge) {
                                            Badge(
                                                containerColor = RiverCoral,
                                                contentColor = RiverMist,
                                            )
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
            }
        },
    ) { padding ->
        AppBackdrop {
            when (selectedTab) {
                MainTab.FISHING -> FishingScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onOpenDaily = { showDailyRewardSheet = true },
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
    if (showNicknameDialog) {
        NicknameChangeDialog(
            strings = strings,
            nickname = state.nickname,
            busy = state.working,
            onNicknameChange = onUpdateNickname,
            onSave = {
                onSaveNickname()
                showNicknameDialog = false
            },
            onDismiss = { showNicknameDialog = false },
        )
    }

    if (showCatchStats) {
        CatchStatsSheet(
            state = state.catchStats,
            strings = strings,
            onPeriodChange = onLoadCatchStats,
            onDismiss = { showCatchStats = false },
        )
    }

    if (showDailyRewardSheet) {
        DailyRewardSheet(
            strings = strings,
            me = me,
            onDismiss = { showDailyRewardSheet = false },
            onClaim = {
                showDailyRewardSheet = false
                onClaimDaily()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatchStatsSheet(
    state: CatchStatsUiState,
    strings: RiverStrings,
    onPeriodChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.statistics,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            // Period filter chips
            val periods = listOf("day", "week", "month", "all")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(strings.statsPeriodLabel(period)) },
                    )
                }
            }

            if (state.loading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val stats = state.stats
                if (stats != null) {
                    // Total weight + count row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatPill(
                            title = strings.totalWeight,
                            value = String.format(Locale.US, "%.2f kg", stats.totalWeight),
                            modifier = Modifier.weight(1f),
                        )
                        StatPill(
                            title = strings.totalCount,
                            value = stats.totalCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Rarity breakdown
                    if (stats.byRarity.isNotEmpty()) {
                        HorizontalDivider()
                        stats.byRarity.forEach { rarity ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(rarityColor(rarity.rarity), CircleShape)
                                    )
                                    Text(
                                        text = strings.rarityLabel(rarity.rarity),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                Text(
                                    text = "${rarity.count} • ${String.format(Locale.US, "%.2f kg", rarity.weight)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    EmptyStatePanel(text = strings.noData)
                }
            }
        }
    }
}

@Composable
private fun NicknameChangeDialog(
    strings: RiverStrings,
    nickname: String,
    busy: Boolean,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.changeNickname) },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(strings.chooseNickname) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !busy && nickname.isNotBlank()) {
                Text(strings.continueLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("✕")
            }
        },
    )
}

@Composable
private fun AppBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .riverBackdrop(),
        content = content,
    )
}

@Composable
private fun HeaderBar(
    me: MeResponseDto,
    strings: RiverStrings,
    onOpenDaily: () -> Unit,
    onLogout: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onOpenNicknameChange: () -> Unit,
    onOpenCatchStats: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val languageFlag = if (me.language == "ru") "\uD83C\uDDF7\uD83C\uDDFA" else "\uD83C\uDDEC\uD83C\uDDE7"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Clickable Nickname + Dropdown
            Box {
                Row(
                    modifier = Modifier.clickable { menuExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = me.username ?: strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "▾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("✏\uFE0F  ${strings.changeNickname}") },
                        onClick = {
                            menuExpanded = false
                            onOpenNicknameChange()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("\uD83D\uDCCA  ${strings.statistics}") },
                        onClick = {
                            menuExpanded = false
                            onOpenCatchStats()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("$languageFlag  ${strings.language}") },
                        onClick = {
                            menuExpanded = false
                            val newLang = if (me.language == "ru") "en" else "ru"
                            onChangeLanguage(newLang)
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = strings.logout,
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onLogout()
                        },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DailyStreakChip(me = me, onClick = onOpenDaily)
                HeaderCounterChip(icon = "\uD83E\uDE99", value = me.coins.toString())
            }
        }
    }
}

@Composable
private fun DailyStreakChip(
    me: MeResponseDto,
    onClick: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (me.dailyAvailable) {
                Badge(containerColor = Color(0xFFE04A4A), contentColor = Color.White) {
                    Text("!")
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = RiverPanelMuted.copy(alpha = 0.92f),
            border = BorderStroke(
                1.dp,
                if (me.dailyAvailable) RiverAmber.copy(alpha = 0.72f) else RiverOutline.copy(alpha = 0.72f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "\uD83D\uDD25", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = me.dailyStreak.toString(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HeaderCounterChip(
    icon: String,
    value: String,
) {
    Surface(
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(18.dp),
        color = RiverPanelMuted.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FishingScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onOpenDaily: () -> Unit,
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
            onOpenDaily()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FishingSetupBar(
                strings = strings,
                me = me,
                onOpenLocations = { activeSheet = FishingSheetType.LOCATIONS },
                onOpenLures = { activeSheet = FishingSheetType.LURES },
                onOpenRods = { activeSheet = FishingSheetType.RODS },
            )
        }
        item {
            FishingStageScene(
                state = state,
                backgroundUrl = locationBackgroundAsset(currentLocation?.name),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            )
        }
        item {
            FishingActionCard(
                state = state,
                strings = strings,
                onBeginCast = onBeginCast,
                onHookFish = onHookFish,
                onTapChallenge = onTapChallenge,
            )
        }
        if (me.autoFish) {
            item {
                FishingRewardsCard(
                    strings = strings,
                    me = me,
                    autoCastEnabled = state.fishing.autoCastEnabled,
                    onToggleAutoCast = onToggleAutoCast,
                )
            }
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
private fun FishingStageScene(
    state: RiverKingUiState,
    backgroundUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bobberBitmap = remember(context) {
        try {
            context.assets.open("menu/bobber.webp").use { 
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    val phase = state.fishing.phase
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

    // --- Bobber position (relative fractions matching the TG webapp) ---
    // In the TG webapp the bobber lands to the LEFT of the rod tip,
    // in the water area (waterTop ~0.48, bobber Y ~0.60-0.80).
    // When idle / ready the bobber sits near the shore (left side).
    val castX by animateFloatAsState(
        targetValue = when (phase) {
            FishingPhase.READY -> 0.09f
            FishingPhase.COOLDOWN -> 0.12f
            FishingPhase.WAITING_BITE -> 0.22f
            FishingPhase.BITING -> 0.20f
            FishingPhase.TAP_CHALLENGE -> 0.19f
            FishingPhase.RESOLVING -> if (state.fishing.lastCast?.caught == true) 0.28f else 0.21f
        },
        animationSpec = tween(durationMillis = 850),
        label = "cast-x",
    )
    val castY by animateFloatAsState(
        targetValue = when (phase) {
            FishingPhase.READY -> 0.45f
            FishingPhase.COOLDOWN -> 0.48f
            FishingPhase.WAITING_BITE -> 0.62f
            FishingPhase.BITING -> 0.60f
            FishingPhase.TAP_CHALLENGE -> 0.58f
            FishingPhase.RESOLVING -> if (state.fishing.lastCast?.caught == true) 0.40f else 0.61f
        },
        animationSpec = tween(durationMillis = 850),
        label = "cast-y",
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = RiverPanel.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.55f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            if (backgroundUrl != null) {
                coil.compose.AsyncImage(
                    model = backgroundUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = 1.22f,
                            scaleY = 1.08f,
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RiverDeepNight.copy(alpha = 0.02f),
                                RiverAbyss.copy(alpha = 0.12f),
                                RiverDeepNight.copy(alpha = 0.46f),
                            )
                        )
                    )
            )

            val currentRodCode = state.me?.rods?.firstOrNull { it.id == state.me?.currentRodId }?.code
            val rodUrl = rodAsset(currentRodCode)
            val rodTipAnchor = rodTipAnchorPercentage(currentRodCode)

            // --- Rod sizing matching the TG webapp ---
            // ROD_IMG_SIZE = 1536 x 1024
            val rodImgWidth = 1536f
            val rodImgHeight = 1024f
            // Target fractions (for fitting), then scaled by ROD_SIZE_MULT
            val targetWFrac = 0.70f
            val targetHFrac = 0.92f
            val rodSizeMult = 1.5f
            val rodScaleBase = min(
                (maxWidth.value * targetWFrac) / rodImgWidth,
                (maxHeight.value * targetHFrac) / rodImgHeight,
            )
            val rodScale = rodScaleBase * rodSizeMult
            val rodWidthDp = (rodImgWidth * rodScale).dp
            val rodHeightDp = (rodImgHeight * rodScale).dp

            // --- Rod positioning matching the TG webapp ---
            // ROD_BASE_X_FRACTION = 2/3  — the grip goes at 66% of scene width
            // ROD_BASE_ANCHOR = { x: 0.383, y: 0.998 } — grip point in the rod image
            val rodBaseXFraction = 2f / 3f
            val rodBaseAnchorX = 0.383f
            val baseDesiredX = maxWidth * rodBaseXFraction
            val rodLeftDp = baseDesiredX - rodWidthDp * rodBaseAnchorX

            // Push rod bottom aligned with the scene
            val rodBottomOvershoot = 50.dp
            val rodTopDp = maxHeight - rodHeightDp + rodBottomOvershoot

            coil.compose.AsyncImage(
                model = rodUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset(x = rodLeftDp, y = rodTopDp)
                    .width(rodWidthDp)
                    .height(rodHeightDp),
                contentScale = ContentScale.FillBounds
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val waterTop = size.height * 0.48f

                // Rod tip in pixel coordinates
                val rodTip = Offset(
                    x = rodLeftDp.toPx() + rodWidthDp.toPx() * rodTipAnchor.x,
                    y = rodTopDp.toPx() + rodHeightDp.toPx() * rodTipAnchor.y,
                )

                // Bobber wave animation
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

                // Rod line points
                val rodLinePoints = rodLinePointsPercentage(currentRodCode).map {
                    Offset(
                        x = rodLeftDp.toPx() + rodWidthDp.toPx() * it.x,
                        y = rodTopDp.toPx() + rodHeightDp.toPx() * it.y,
                    )
                }

                // Fishing line from last rod point to bobber with natural sag
                val lineOrigin = rodLinePoints.lastOrNull() ?: rodTip
                val dx = bobber.x - lineOrigin.x
                val dy = bobber.y - lineOrigin.y
                val dist = kotlin.math.hypot(dx, dy)
                val shouldShowSlack = phase == FishingPhase.READY || phase == FishingPhase.COOLDOWN
                val control = if (shouldShowSlack) {
                    // Slack line when idle
                    val sag = min(size.height * 0.22f, max(16f, dist * 0.55f))
                    Offset(
                        x = lineOrigin.x + dx * 0.55f,
                        y = lineOrigin.y + dy * 0.5f + sag,
                    )
                } else {
                    // Taut line when cast
                    val gentleSag = min(size.height * 0.08f, dist * 0.12f)
                    Offset(
                        x = lineOrigin.x + dx * 0.5f,
                        y = lineOrigin.y + dy * 0.5f + gentleSag,
                    )
                }
                val linePath = Path().apply {
                    if (rodLinePoints.isNotEmpty()) {
                        moveTo(rodLinePoints.first().x, rodLinePoints.first().y)
                        for (i in 1 until rodLinePoints.size) {
                            lineTo(rodLinePoints[i].x, rodLinePoints[i].y)
                        }
                    } else {
                        moveTo(lineOrigin.x, lineOrigin.y)
                    }
                    quadraticTo(control.x, control.y, bobber.x, bobber.y)
                }
                drawPath(
                    path = linePath,
                    color = Color.White.copy(alpha = 0.35f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )

                // Ripple circles around bobber
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

                // Bobber (float)
                if (bobberBitmap != null) {
                    val bobberRectSize = size.width * 0.08f
                    val srcSize = IntSize(bobberBitmap.width, bobberBitmap.height)
                    val dstSize = IntSize(bobberRectSize.toInt(), (bobberRectSize * bobberBitmap.height / bobberBitmap.width).toInt())
                    drawImage(
                        image = bobberBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = srcSize,
                        dstOffset = IntOffset(
                            (bobber.x - dstSize.width / 2f).toInt(),
                            (bobber.y - dstSize.height / 2f).toInt()
                        ),
                        dstSize = dstSize
                    )
                } else {
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
        color = RiverPanelSoft.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
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
        color = RiverPanelRaised.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.65f)),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = phaseAccentColor(phase),
                    contentColor = RiverDeepNight,
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
private fun FishingSetupBar(
    strings: RiverStrings,
    me: MeResponseDto,
    onOpenLocations: () -> Unit,
    onOpenLures: () -> Unit,
    onOpenRods: () -> Unit,
) {
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }?.name ?: "—"
    val currentLure = me.lures.firstOrNull { it.id == me.currentLureId }?.displayName ?: "—"
    val currentRod = me.rods.firstOrNull { it.id == me.currentRodId }?.name ?: "—"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = RiverPanelMuted.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FishingSetupCell(
                label = strings.water,
                value = currentLocation,
                accent = RiverTide,
                onClick = onOpenLocations,
            )
            FishingSetupDivider()
            FishingSetupCell(
                label = strings.bait,
                value = currentLure,
                accent = RiverMoss,
                onClick = onOpenLures,
            )
            FishingSetupDivider()
            FishingSetupCell(
                label = strings.rod,
                value = currentRod,
                accent = RiverAmber,
                onClick = onOpenRods,
            )
        }
    }
}

@Composable
private fun RowScope.FishingSetupCell(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FishingSetupDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(RiverOutline.copy(alpha = 0.72f))
    )
}

@Composable
private fun FishingRewardsCard(
    strings: RiverStrings,
    me: MeResponseDto,
    autoCastEnabled: Boolean,
    onToggleAutoCast: () -> Unit,
) {
    if (!me.autoFish) return

    InfoCard {
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
    val todayOrdinal = if (me.dailyAvailable) me.dailyStreak else max(me.dailyStreak - 1, 0)
    val tomorrowOrdinal = todayOrdinal + 1

    val todayItemIndex = if (rewards.isNotEmpty()) min(todayOrdinal, rewards.lastIndex) else -1
    val tomorrowItemIndex = if (rewards.isNotEmpty()) min(tomorrowOrdinal, rewards.lastIndex) else -1
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(strings.dailyGift, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${strings.dailyStreakLabel}: ${me.dailyStreak}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (todayItemIndex != -1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DailyRewardDayCard(
                        strings = strings,
                        dayIndex = todayOrdinal,
                        rewards = rewards[todayItemIndex],
                        isCurrent = me.dailyAvailable,
                        isCompleted = !me.dailyAvailable,
                        modifier = Modifier.weight(1f),
                    )
                    if (tomorrowItemIndex != -1) {
                        DailyRewardDayCard(
                            strings = strings,
                            dayIndex = tomorrowOrdinal,
                            rewards = rewards[tomorrowItemIndex],
                            isCurrent = false,
                            isCompleted = false,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
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
                        val fishAsset = FISH_ASSET_MAP[catch.fish]?.let { localAsset(it) }
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
        color = RiverPanelSoft.copy(alpha = 0.92f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.72f)),
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
        colors = CardDefaults.cardColors(containerColor = RiverPanel.copy(alpha = 0.90f)),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.60f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RiverPanelMuted.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.60f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun LoadingStatePanel(text: String, modifier: Modifier = Modifier) {
    StatusPanel(text = text, modifier = modifier, accent = RiverTide)
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
        color = RiverPanelSoft.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
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
        color = RiverPanelRaised.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.60f)),
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
    containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else RiverPanelSoft.copy(alpha = 0.82f),
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
    if (name.contains("+")) RiverAmber else RiverMoss

private fun rodImageAsset(code: String): String = localAsset(
    when (code) {
        "spark" -> "rods/yellow_rod.webp"
        "dew" -> "rods/green_rod.webp"
        "stream" -> "rods/blue_rod.webp"
        "abyss" -> "rods/black_rod.webp"
        "storm" -> "rods/silver_rod.webp"
        else -> "rods/yellow_rod.webp"
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
    return serverAssetUrl("/app/assets/achievements/${base}_$suffix.png")
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
    FishingPhase.READY -> RiverTide
    FishingPhase.COOLDOWN -> RiverSlate
    FishingPhase.WAITING_BITE -> RiverFoam
    FishingPhase.BITING -> RiverCoral
    FishingPhase.TAP_CHALLENGE -> RiverAmber
    FishingPhase.RESOLVING -> RiverMoss
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
        "spark" -> "rods/yellow_rod.webp"
        "dew" -> "rods/green_rod.webp"
        "stream" -> "rods/blue_rod.webp"
        "abyss" -> "rods/black_rod.webp"
        "storm" -> "rods/silver_rod.webp"
        else -> "rods/yellow_rod.webp"
    }
    return localAsset(path)
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

private fun rodLinePointsPercentage(code: String?): List<androidx.compose.ui.geometry.Offset> {
    return when (code) {
        "spark" -> listOf(
            androidx.compose.ui.geometry.Offset(0.765f, 0.98f),
            androidx.compose.ui.geometry.Offset(0.635f, 0.80f),
            androidx.compose.ui.geometry.Offset(0.495f, 0.61f),
            androidx.compose.ui.geometry.Offset(0.355f, 0.42f),
            androidx.compose.ui.geometry.Offset(0.215f, 0.23f),
            androidx.compose.ui.geometry.Offset(0.078f, 0.048f)
        )
        "dew" -> listOf(
            androidx.compose.ui.geometry.Offset(0.765f, 0.98f),
            androidx.compose.ui.geometry.Offset(0.639f, 0.797f),
            androidx.compose.ui.geometry.Offset(0.513f, 0.614f),
            androidx.compose.ui.geometry.Offset(0.388f, 0.432f),
            androidx.compose.ui.geometry.Offset(0.262f, 0.249f),
            androidx.compose.ui.geometry.Offset(0.13607f, 0.06641f)
        )
        "stream" -> listOf(
            androidx.compose.ui.geometry.Offset(0.72f, 1.1f),
            androidx.compose.ui.geometry.Offset(0.609f, 0.895f),
            androidx.compose.ui.geometry.Offset(0.497f, 0.690f),
            androidx.compose.ui.geometry.Offset(0.386f, 0.485f),
            androidx.compose.ui.geometry.Offset(0.274f, 0.279f),
            androidx.compose.ui.geometry.Offset(0.155f, 0.07422f)
        )
        "abyss" -> listOf(
            androidx.compose.ui.geometry.Offset(0.765f, 0.98f),
            androidx.compose.ui.geometry.Offset(0.642f, 0.793f),
            androidx.compose.ui.geometry.Offset(0.518f, 0.607f),
            androidx.compose.ui.geometry.Offset(0.395f, 0.420f),
            androidx.compose.ui.geometry.Offset(0.272f, 0.234f),
            androidx.compose.ui.geometry.Offset(0.14844f, 0.04688f)
        )
        "storm" -> listOf(
            androidx.compose.ui.geometry.Offset(0.72f, 1.1f),
            androidx.compose.ui.geometry.Offset(0.603f, 0.895f),
            androidx.compose.ui.geometry.Offset(0.486f, 0.690f),
            androidx.compose.ui.geometry.Offset(0.369f, 0.485f),
            androidx.compose.ui.geometry.Offset(0.251f, 0.280f),
            androidx.compose.ui.geometry.Offset(0.125f, 0.07520f)
        )
        else -> listOf(
            androidx.compose.ui.geometry.Offset(0.765f, 0.98f),
            androidx.compose.ui.geometry.Offset(0.635f, 0.80f),
            androidx.compose.ui.geometry.Offset(0.495f, 0.61f),
            androidx.compose.ui.geometry.Offset(0.355f, 0.42f),
            androidx.compose.ui.geometry.Offset(0.215f, 0.23f),
            androidx.compose.ui.geometry.Offset(0.078f, 0.048f)
        )
    }
}

private fun locationBackgroundAsset(location: String?): String? {
    val path = when (location) {
        "Пруд", "Pond" -> "backgrounds/pond.webp"
        "Болото", "Swamp" -> "backgrounds/swamp.webp"
        "Река", "River" -> "backgrounds/river.webp"
        "Озеро", "Lake" -> "backgrounds/lake.webp"
        "Водохранилище", "Reservoir" -> "backgrounds/reservoir.webp"
        "Горная река", "Mountain River" -> "backgrounds/mountain_river.webp"
        "Дельта реки", "River Delta" -> "backgrounds/river_delta.webp"
        "Прибрежье моря", "Sea Coast" -> "backgrounds/sea_coast.webp"
        "Фьорд", "Fjord" -> "backgrounds/fjord.webp"
        "Открытый океан", "Open Ocean" -> "backgrounds/open_ocean.webp"
        "Русло Амазонки", "Amazon Riverbed" -> "backgrounds/amazon_riverbed.webp"
        "Игапо, затопленный лес", "Flooded Forest" -> "backgrounds/flooded_forest.webp"
        "Мангровые заросли", "Mangroves" -> "backgrounds/mangroves.webp"
        "Коралловые отмели", "Coral Flats" -> "backgrounds/coral_flats.webp"
        else -> null
    }
    return path?.let(::localAsset)
}

private fun shopIconAsset(packId: String): String = localAsset(
    if (packId.startsWith("autofish")) "shop/autofish.webp" else "shop/$packId.webp"
)

/** Load a bundled asset from android_asset via file URI (Coil supports this). */
private fun localAsset(relativePath: String): String = "file:///android_asset/$relativePath"

/** Load an asset from the server (used for achievements that are not bundled). */
private fun serverAssetUrl(path: String): String = BuildConfig.API_BASE_URL.trimEnd('/') + path

private val FISH_ASSET_MAP = mapOf(
    "Плотва" to "fish/plotva.webp",
    "Roach" to "fish/plotva.webp",
    "Окунь" to "fish/okun.webp",
    "Perch" to "fish/okun.webp",
    "Карась" to "fish/karas.webp",
    "Crucian Carp" to "fish/karas.webp",
    "Лещ" to "fish/lesch.webp",
    "Bream" to "fish/lesch.webp",
    "Щука" to "fish/schuka.webp",
    "Pike" to "fish/schuka.webp",
    "Карп" to "fish/karp.webp",
    "Carp" to "fish/karp.webp",
    "Сом европейский" to "fish/som.webp",
    "European Catfish" to "fish/som.webp",
    "Осётр европейский" to "fish/osetr.webp",
    "European Sturgeon" to "fish/osetr.webp",
    "Уклейка" to "fish/ukleyka.webp",
    "Bleak" to "fish/ukleyka.webp",
    "Линь" to "fish/lin.webp",
    "Tench" to "fish/lin.webp",
    "Ротан" to "fish/rotan.webp",
    "Rotan" to "fish/rotan.webp",
    "Судак" to "fish/sudak.webp",
    "Zander" to "fish/sudak.webp",
    "Чехонь" to "fish/chehon.webp",
    "Sabrefish" to "fish/chehon.webp",
    "Хариус" to "fish/harius.webp",
    "Grayling" to "fish/harius.webp",
    "Форель ручьевая" to "fish/forel_ruchevaya.webp",
    "Brook Trout" to "fish/forel_ruchevaya.webp",
    "Таймень" to "fish/taymen.webp",
    "Taimen" to "fish/taymen.webp",
    "Налим" to "fish/nalim.webp",
    "Burbot" to "fish/nalim.webp",
    "Сиг обыкновенный" to "fish/sig.webp",
    "Common Whitefish" to "fish/sig.webp",
    "Голавль" to "fish/golavl.webp",
    "Chub" to "fish/golavl.webp",
    "Жерех" to "fish/zhereh.webp",
    "Asp" to "fish/zhereh.webp",
    "Толстолобик" to "fish/tolstolobik.webp",
    "Bighead Carp" to "fish/tolstolobik.webp",
    "Амур белый" to "fish/beliy_amur.webp",
    "Grass Carp" to "fish/beliy_amur.webp",
    "Угорь европейский" to "fish/ugor_evropeyskiy.webp",
    "European Eel" to "fish/ugor_evropeyskiy.webp",
    "Стерлядь" to "fish/sterlyad.webp",
    "Sterlet" to "fish/sterlyad.webp",
    "Кефаль-лобан" to "fish/kefal.webp",
    "Flathead Grey Mullet" to "fish/kefal.webp",
    "Камбала морская" to "fish/kambala.webp",
    "Sea Flounder" to "fish/kambala.webp",
    "Сельдь" to "fish/seld.webp",
    "Herring" to "fish/seld.webp",
    "Ставрида" to "fish/stavrida.webp",
    "Horse Mackerel" to "fish/stavrida.webp",
    "Тихоокеанский клювач" to "fish/klyuvach_pacificocean.webp",
    "Pacific Snipefish" to "fish/klyuvach_pacificocean.webp",
    "Треска" to "fish/treska.webp",
    "Cod" to "fish/treska.webp",
    "Сайда" to "fish/sayda.webp",
    "Pollock" to "fish/sayda.webp",
    "Мерланг" to "fish/merlang.webp",
    "Whiting" to "fish/merlang.webp",
    "Форель морская" to "fish/morskaya_forel.webp",
    "Sea Trout" to "fish/morskaya_forel.webp",
    "Палтус" to "fish/paltus.webp",
    "Halibut" to "fish/paltus.webp",
    "Корюшка" to "fish/koryushka.webp",
    "Smelt" to "fish/koryushka.webp",
    "Лосось атлантический" to "fish/losos_atlanticheskiy.webp",
    "Atlantic Salmon" to "fish/losos_atlanticheskiy.webp",
    "Лаврак" to "fish/lavrak.webp",
    "Sea Bass" to "fish/lavrak.webp",
    "Скумбрия атлантическая" to "fish/skumbriya_atlanticheskaya.webp",
    "Atlantic Mackerel" to "fish/skumbriya_atlanticheskaya.webp",
    "Горбуша" to "fish/gorbusha.webp",
    "Pink Salmon" to "fish/gorbusha.webp",
    "Кета" to "fish/keta.webp",
    "Chum Salmon" to "fish/keta.webp",
    "Белуга" to "fish/beluga.webp",
    "Beluga" to "fish/beluga.webp",
    "Ёрш" to "fish/yorsh.webp",
    "Ruffe" to "fish/yorsh.webp",
    "Берш" to "fish/bersh.webp",
    "Volga Pikeperch" to "fish/bersh.webp",
    "Пескарь" to "fish/peskar.webp",
    "Gudgeon" to "fish/peskar.webp",
    "Густера" to "fish/gustera.webp",
    "Blue Bream" to "fish/gustera.webp",
    "Краснопёрка" to "fish/krasnopyorka.webp",
    "Rudd" to "fish/krasnopyorka.webp",
    "Елец" to "fish/elets.webp",
    "Dace" to "fish/elets.webp",
    "Верхоплавка" to "fish/verhoplavka.webp",
    "Topmouth Gudgeon" to "fish/verhoplavka.webp",
    "Вьюн" to "fish/vyun.webp",
    "Weather Loach" to "fish/vyun.webp",
    "Вобла" to "fish/vobla.webp",
    "Caspian Roach" to "fish/vobla.webp",
    "Гольян" to "fish/golyan.webp",
    "Minnow" to "fish/golyan.webp",
    "Язь" to "fish/yaz.webp",
    "Ide" to "fish/yaz.webp",
    "Бычок-кругляк" to "fish/bychyok.webp",
    "Round Goby" to "fish/bychyok.webp",
    "Килька" to "fish/kilka.webp",
    "Sprat" to "fish/kilka.webp",
    "Мойва" to "fish/mojva.webp",
    "Capelin" to "fish/mojva.webp",
    "Тарань" to "fish/taran.webp",
    "Black Sea Roach" to "fish/taran.webp",
    "Сардина" to "fish/sardina.webp",
    "Sardine" to "fish/sardina.webp",
    "Анчоус европейский" to "fish/anchous.webp",
    "European Anchovy" to "fish/anchous.webp",
    "Дорадо" to "fish/dorado.webp",
    "Dorado" to "fish/dorado.webp",
    "Ваху" to "fish/vahu.webp",
    "Wahoo" to "fish/vahu.webp",
    "Парусник" to "fish/parusnik.webp",
    "Sailfish" to "fish/parusnik.webp",
    "Рыба-меч" to "fish/ryba_mech.webp",
    "Swordfish" to "fish/ryba_mech.webp",
    "Марлин синий" to "fish/marlin_siniy.webp",
    "Blue Marlin" to "fish/marlin_siniy.webp",
    "Тунец синеперый" to "fish/tunets_sineperiy.webp",
    "Bluefin Tuna" to "fish/tunets_sineperiy.webp",
    "Акула мако" to "fish/akula_mako.webp",
    "Mako Shark" to "fish/akula_mako.webp",
    "Катран обыкновенный" to "fish/katran_simple.webp",
    "Spiny Dogfish" to "fish/katran_simple.webp",
    "Альбакор" to "fish/albakor.webp",
    "Albacore" to "fish/albakor.webp",
    "Голец арктический" to "fish/golets_arkticheskiy.webp",
    "Arctic Char" to "fish/golets_arkticheskiy.webp",
    "Форель кумжа" to "fish/forel_kumzha.webp",
    "Brown Trout" to "fish/forel_kumzha.webp",
    "Пикша" to "fish/piksha.webp",
    "Haddock" to "fish/piksha.webp",
    "Тюрбо" to "fish/tyurbo.webp",
    "Turbot" to "fish/tyurbo.webp",
    "Сайра" to "fish/sayra.webp",
    "Pacific Saury" to "fish/sayra.webp",
    "Летучая рыба" to "fish/letuchaya_ryba.webp",
    "Flying Fish" to "fish/letuchaya_ryba.webp",
    "Рыба-луна" to "fish/ryba_luna.webp",
    "Ocean Sunfish" to "fish/ryba_luna.webp",
    "Сельдяной король" to "fish/seldyanoy_korol.webp",
    "Oarfish" to "fish/seldyanoy_korol.webp",
    "Паку бурый" to "fish/tambaki.webp",
    "Brown Pacu" to "fish/tambaki.webp",
    "Паку краснобрюхий" to "fish/paku_krasnobryuhiy.webp",
    "Red-bellied Pacu" to "fish/paku_krasnobryuhiy.webp",
    "Паку чёрный" to "fish/paku_cherniy.webp",
    "Black Pacu" to "fish/paku_cherniy.webp",
    "Прохилодус красноштриховый" to "fish/prohilodus.webp",
    "Stripetail Prochilodus" to "fish/prohilodus.webp",
    "Лепоринус полосатый" to "fish/leporinus_polosatiy.webp",
    "Banded Leporinus" to "fish/leporinus_polosatiy.webp",
    "Метиннис серебристый" to "fish/metinnis_serebristiy.webp",
    "Silver Dollar" to "fish/metinnis_serebristiy.webp",
    "Пелядь" to "fish/pelyad.webp",
    "Peled Whitefish" to "fish/pelyad.webp",
    "Омуль арктический" to "fish/omul_arkticheskiy.webp",
    "Arctic Omul" to "fish/omul_arkticheskiy.webp",
    "Муксун" to "fish/muksun.webp",
    "Muksun Whitefish" to "fish/muksun.webp",
    "Анциструс обыкновенный" to "fish/ancistrus.webp",
    "Common Bristlenose" to "fish/ancistrus.webp",
    "Птеригоплихт парчовый" to "fish/pterigopliht_parchoviy.webp",
    "Sailfin Pleco" to "fish/pterigopliht_parchoviy.webp",
    "Отоцинклюс широкополосый" to "fish/otocinklyus.webp",
    "Banded Otocinclus" to "fish/otocinklyus.webp",
    "Карнегиелла мраморная" to "fish/karnegiella_mramornaya.webp",
    "Marbled Hatchetfish" to "fish/karnegiella_mramornaya.webp",
    "Тетра неоновая" to "fish/tetra_neonovaya.webp",
    "Neon Tetra" to "fish/tetra_neonovaya.webp",
    "Тернеция чёрная" to "fish/tetra_chernaya.webp",
    "Black Skirt Tetra" to "fish/tetra_chernaya.webp",
    "Рыба-лист амазонская" to "fish/ryba_list_amazonskaya.webp",
    "Amazon Leaf Fish" to "fish/ryba_list_amazonskaya.webp",
    "Арапайма" to "fish/arapayma.webp",
    "Arapaima" to "fish/arapayma.webp",
    "Ленок" to "fish/lenok.webp",
    "Lenok Trout" to "fish/lenok.webp",
    "Пиранья краснобрюхая" to "fish/piranya_krasnopuzaya.webp",
    "Red-bellied Piranha" to "fish/piranya_krasnopuzaya.webp",
    "Бикуда" to "fish/bikuda.webp",
    "Bicuda" to "fish/bikuda.webp",
    "Угорь электрический" to "fish/ugor_elektricheskiy.webp",
    "Electric Eel" to "fish/ugor_elektricheskiy.webp",
    "Сом краснохвостый" to "fish/krasnohvostiy_som.webp",
    "Redtail Catfish" to "fish/krasnohvostiy_som.webp",
    "Пимелодус пятнистый" to "fish/pimelodus_pyatnistiy.webp",
    "Spotted Pimelodus" to "fish/pimelodus_pyatnistiy.webp",
    "Сом веслоносый" to "fish/som_veslonosiy.webp",
    "Paddlefish" to "fish/som_veslonosiy.webp",
    "Пираиба" to "fish/piraiba.webp",
    "Piraiba" to "fish/piraiba.webp",
    "Дискус обыкновенный" to "fish/diskus.webp",
    "Common Discus" to "fish/diskus.webp",
    "Скалярия альтум" to "fish/skalyaria_altum.webp",
    "Altum Angelfish" to "fish/skalyaria_altum.webp",
    "Скалярия обыкновенная" to "fish/skalyaria_common.webp",
    "Freshwater Angelfish" to "fish/skalyaria_common.webp",
    "Апистограмма Агассиза" to "fish/apistogramma_agassiza.webp",
    "Agassiz's Cichlid" to "fish/apistogramma_agassiza.webp",
    "Тетра кардинальная" to "fish/tetra_kardinal.webp",
    "Cardinal Tetra" to "fish/tetra_kardinal.webp",
    "Тетра лимонная" to "fish/tetra_limonnaya.webp",
    "Lemon Tetra" to "fish/tetra_limonnaya.webp",
    "Тетра огненная" to "fish/tetra_ognennaya.webp",
    "Ember Tetra" to "fish/tetra_ognennaya.webp",
    "Тетра пингвин" to "fish/tetra_pingvin.webp",
    "Penguin Tetra" to "fish/tetra_pingvin.webp",
    "Тетра родостомус" to "fish/tetra_rodostomus.webp",
    "Rummy-nose Tetra" to "fish/tetra_rodostomus.webp",
    "Тетра чёрный неон" to "fish/tetra_black_neon.webp",
    "Black Neon Tetra" to "fish/tetra_black_neon.webp",
    "Коридорас панда" to "fish/koridorus_panda.webp",
    "Panda Cory" to "fish/koridorus_panda.webp",
    "Коридорас Штерба" to "fish/koridoras_shterba.webp",
    "Sterba's Corydoras" to "fish/koridoras_shterba.webp",
    "Татия леопардовая" to "fish/tatiya_leopardovaya.webp",
    "Leopard Tatia" to "fish/tatiya_leopardovaya.webp",
    "Торакатум" to "fish/torakatum.webp",
    "Hoplo Catfish" to "fish/torakatum.webp",
    "Нанностомус трифасциатус" to "fish/nannostomus.webp",
    "Three-Stripe Pencilfish" to "fish/nannostomus.webp",
    "Нанностомус маргинатус" to "fish/nannostomus_marginatus.webp",
    "Dwarf Pencilfish" to "fish/nannostomus_marginatus.webp",
    "Рамирези" to "fish/ramirezi.webp",
    "Ram Cichlid" to "fish/ramirezi.webp",
    "Аравана чёрная" to "fish/aravana_chernaya.webp",
    "Black Arowana" to "fish/aravana_chernaya.webp",
    "Астронотус глазчатый" to "fish/oskar.webp",
    "Oscar Cichlid" to "fish/oskar.webp",
    "Аймара" to "fish/aymara.webp",
    "Aimara" to "fish/aymara.webp",
    "Псевдоплатистома тигровая" to "fish/surubin.webp",
    "Tiger Shovelnose" to "fish/surubin.webp",
    "Брахиплатистома тигровая" to "fish/brahiplatistoma_tigrovaya.webp",
    "Tiger Catfish" to "fish/brahiplatistoma_tigrovaya.webp",
    "Пиранья чёрная" to "fish/piranya_chernaya.webp",
    "Black Piranha" to "fish/piranya_chernaya.webp",
    "Паяра" to "fish/payara.webp",
    "Payara" to "fish/payara.webp",
    "Мечерот обыкновенный" to "fish/mecherot_common.webp",
    "Common Freshwater Barracuda" to "fish/mecherot_common.webp",
    "Мечерот пятнистый" to "fish/mecherot_pyatnistiy.webp",
    "Spotted Freshwater Barracuda" to "fish/mecherot_pyatnistiy.webp",
    "Гимнотус угревидный" to "fish/gimnotus_ugrevidniy.webp",
    "Banded Knifefish" to "fish/gimnotus_ugrevidniy.webp",
    "Нож-рыба чёрная" to "fish/ryba_nozh_chernaya.webp",
    "Black Ghost Knifefish" to "fish/ryba_nozh_chernaya.webp",
    "Щучья цихлида" to "fish/schuchya_cihlida.webp",
    "Pike Cichlid" to "fish/schuchya_cihlida.webp",
    "Павлиний окунь" to "fish/pavliniy_okun.webp",
    "Peacock Bass" to "fish/pavliniy_okun.webp",
    "Цихлазома мезонаута" to "fish/cihlazoma_mezonauta.webp",
    "Flag Cichlid" to "fish/cihlazoma_mezonauta.webp",
    "Цихлазома северум" to "fish/cihlazoma_severum.webp",
    "Severum Cichlid" to "fish/cihlazoma_severum.webp",
    "Молочная рыба" to "fish/molochnaya_ryba.webp",
    "Milkfish" to "fish/molochnaya_ryba.webp",
    "Кефаль пятнистая" to "fish/kefal_pyatnistaya.webp",
    "Spotted Mullet" to "fish/kefal_pyatnistaya.webp",
    "Тиляпия мозамбикская" to "fish/tilyapiya_mozambikskaya.webp",
    "Mozambique Tilapia" to "fish/tilyapiya_mozambikskaya.webp",
    "Анчоус тропический" to "fish/anchous_tropicheskiy.webp",
    "Tropical Anchovy" to "fish/anchous_tropicheskiy.webp",
    "Сардина индийская" to "fish/sardina_indiyskaya.webp",
    "Indian Sardine" to "fish/sardina_indiyskaya.webp",
    "Сиган золотистый" to "fish/zolotistiy_shiponog.webp",
    "Golden Rabbitfish" to "fish/zolotistiy_shiponog.webp",
    "Бычок-пчёлка" to "fish/bychok_mangroviy.webp",
    "Bumblebee Goby" to "fish/bychok_mangroviy.webp",
    "Баррамунди" to "fish/barramundi.webp",
    "Barramundi" to "fish/barramundi.webp",
    "Снук" to "fish/snuk.webp",
    "Snook" to "fish/snuk.webp",
    "Луциан мангровый" to "fish/mangroviy_snapper.webp",
    "Mangrove Snapper" to "fish/mangroviy_snapper.webp",
    "Тарпон" to "fish/tarpon.webp",
    "Tarpon" to "fish/tarpon.webp",
    "Сом морской" to "fish/morskoy_som.webp",
    "Sea Catfish" to "fish/morskoy_som.webp",
    "Сарган морской" to "fish/morskoy_sargan.webp",
    "Needlefish" to "fish/morskoy_sargan.webp",
    "Каранкс голубой" to "fish/goluboy_trevalli.webp",
    "Blue Trevally" to "fish/goluboy_trevalli.webp",
    "Рыба-попугай" to "fish/ryba_popugay.webp",
    "Parrotfish" to "fish/ryba_popugay.webp",
    "Ангел императорский" to "fish/angel_imperatorskiy.webp",
    "Emperor Angelfish" to "fish/angel_imperatorskiy.webp",
    "Хирург голубой" to "fish/hirurg_goluboy.webp",
    "Blue Tang" to "fish/hirurg_goluboy.webp",
    "Бабочка нитеносная" to "fish/babochka_klinopolosaya.webp",
    "Threadfin Butterflyfish" to "fish/babochka_klinopolosaya.webp",
    "Хризиптера синяя" to "fish/damsel_siniy.webp",
    "Blue Damselfish" to "fish/damsel_siniy.webp",
    "Фузилёр жёлтохвостый" to "fish/fuziler_zheltohvostiy.webp",
    "Yellowtail Fusilier" to "fish/fuziler_zheltohvostiy.webp",
    "Барабулька тропическая" to "fish/barabulka_tropicheskaya.webp",
    "Tropical Goatfish" to "fish/barabulka_tropicheskaya.webp",
    "Барракуда большая" to "fish/barrakuda_bolschaya.webp",
    "Great Barracuda" to "fish/barrakuda_bolschaya.webp",
    "Конгер" to "fish/konger.webp",
    "Conger Eel" to "fish/konger.webp",
    "Каранкс гигантский" to "fish/gigantskiy_karanks.webp",
    "Giant Trevally" to "fish/gigantskiy_karanks.webp",
    "Пермит" to "fish/permit.webp",
    "Permit" to "fish/permit.webp",
    "Альбула" to "fish/kostlyavaya_ryba.webp",
    "Bonefish" to "fish/kostlyavaya_ryba.webp",
    "Скумбрия испанская" to "fish/ispanskaya_makrel.webp",
    "Spanish Mackerel" to "fish/ispanskaya_makrel.webp",
    "Группер коралловый" to "fish/koralloviy_grupper.webp",
    "Coral Grouper" to "fish/koralloviy_grupper.webp",
    "Спинорог-титан" to "fish/spinorog_titan.webp",
    "Titan Triggerfish" to "fish/spinorog_titan.webp",
    "Карп кои (Кохаку)" to "fish/koi_kohaku.webp",
    "Koi (Kohaku)" to "fish/koi_kohaku.webp",
    "Карп кои (Тайсё Сансёку)" to "fish/koi_taisho_sanke.webp",
    "Koi (Taisho Sanshoku)" to "fish/koi_taisho_sanke.webp",
    "Карп кои (Сёва Сансёку)" to "fish/koi_showa_sanshoku.webp",
    "Koi (Showa Sanshoku)" to "fish/koi_showa_sanshoku.webp",
    "Карп кои (Уцуримоно)" to "fish/koi_utsurimono.webp",
    "Koi (Utsurimono)" to "fish/koi_utsurimono.webp",
    "Карп кои (Бэкко)" to "fish/koi_bekko.webp",
    "Koi (Bekko)" to "fish/koi_bekko.webp",
    "Карп кои (Тантё)" to "fish/koi_tancho.webp",
    "Koi (Tancho)" to "fish/koi_tancho.webp",
    "Карп кои (Асаги)" to "fish/koi_asagi.webp",
    "Koi (Asagi)" to "fish/koi_asagi.webp",
    "Карп кои (Сюсуй)" to "fish/koi_shusui.webp",
    "Koi (Shusui)" to "fish/koi_shusui.webp",
    "Карп кои (Коромо)" to "fish/koi_koromo.webp",
    "Koi (Koromo)" to "fish/koi_koromo.webp",
    "Карп кои (Кингинрин)" to "fish/koi_kinginrin.webp",
    "Koi (Kinginrin)" to "fish/koi_kinginrin.webp",
    "Карп кои (Каваримоно)" to "fish/koi_kawarimono.webp",
    "Koi (Kawarimono)" to "fish/koi_kawarimono.webp",
    "Карп кои (Огон)" to "fish/koi_ogon.webp",
    "Koi (Ogon)" to "fish/koi_ogon.webp",
    "Карп кои (Хикари-моёмоно)" to "fish/koi_hikari_moyomono.webp",
    "Koi (Hikari Moyomono)" to "fish/koi_hikari_moyomono.webp",
    "Карп кои (Госики)" to "fish/koi_goshiki.webp",
    "Koi (Goshiki)" to "fish/koi_goshiki.webp",
    "Карп кои (Кумонрю)" to "fish/koi_kumonryu.webp",
    "Koi (Kumonryu)" to "fish/koi_kumonryu.webp",
    "Карп кои (Дойцу-гои)" to "fish/koi_doitsu.webp",
    "Koi (Doitsu-goi)" to "fish/koi_doitsu.webp",
    "Амур чёрный" to "fish/cherniy_amur.webp",
    "Black Amur" to "fish/cherniy_amur.webp",
    "Змееголов северный" to "fish/zmeegolov_severniy.webp",
    "Northern Snakehead" to "fish/zmeegolov_severniy.webp",
    "Щука амурская" to "fish/amurskaya_schuka.webp",
    "Amur Pike" to "fish/amurskaya_schuka.webp",
    "Кристивомер" to "fish/kristivomer.webp",
    "Lake Trout" to "fish/kristivomer.webp",
    "Лосось дунайский" to "fish/dunaiskiy_losos.webp",
    "Danube Salmon (Huchen)" to "fish/dunaiskiy_losos.webp",
    "Зунгаро" to "fish/zungaro.webp",
    "Zungaro Catfish" to "fish/zungaro.webp",
    "Скат моторо" to "fish/skat_motoro.webp",
    "Motoro Stingray" to "fish/skat_motoro.webp",
    "Пеленгас" to "fish/pelengas.webp",
    "Pelingas Mullet" to "fish/pelengas.webp",
    "Вырезуб" to "fish/vyrezub.webp",
    "Black Sea Shemaya" to "fish/vyrezub.webp",
    "Кубера" to "fish/kubera.webp",
    "Cubera Snapper" to "fish/kubera.webp",
    "Мурена европейская" to "fish/murena_european.webp",
    "European Moray" to "fish/murena_european.webp",
    "Мурена звёздчатая" to "fish/murena_zvezdchataya.webp",
    "Starry Moray" to "fish/murena_zvezdchataya.webp",
    "Мурена гигантская" to "fish/murena_gigantskaya.webp",
    "Giant Moray" to "fish/murena_gigantskaya.webp",
    "Зубатка пятнистая" to "fish/zubatka_pyatnistaya.webp",
    "Spotted Wolffish" to "fish/zubatka_pyatnistaya.webp",
    "Тунец желтоперый" to "fish/tunec_zeltoperiy.webp",
    "Yellowfin Tuna" to "fish/tunec_zeltoperiy.webp",
    "Снук чёрный" to "fish/chyorniy_snuk.webp",
    "Black Snook" to "fish/chyorniy_snuk.webp",
    "Рыба-наполеон" to "fish/ryba_napoleon.webp",
    "Napoleon Wrasse" to "fish/ryba_napoleon.webp",
    "Рыба-клоун" to "fish/ryba_kloun.webp",
    "Clownfish" to "fish/ryba_kloun.webp",
    "Кефаль мангровая" to "fish/kefal_mangrovaya.webp",
    "Mangrove Mullet" to "fish/kefal_mangrovaya.webp",
    "Сельдь тихоокеанская" to "fish/seld_tihookeanskaya.webp",
    "Pacific Herring" to "fish/seld_tihookeanskaya.webp",
    "Хромис рифовый" to "fish/hromis_rifoviy.webp",
    "Reef Chromis" to "fish/hromis_rifoviy.webp",
    "Дамсел жёлтохвостый" to "fish/damsel_zheltohvostiy.webp",
    "Yellowtail Damselfish" to "fish/damsel_zheltohvostiy.webp",
    "Морской конёк" to "fish/morskoy_konek.webp",
    "Seahorse" to "fish/morskoy_konek.webp",
    "Идол мавританский" to "fish/idol_mavritanskiy.webp",
    "Moorish Idol" to "fish/idol_mavritanskiy.webp",
    "Рыба-бабочка полосатая" to "fish/ryba_babochka_polosataya.webp",
    "Striped Butterflyfish" to "fish/ryba_babochka_polosataya.webp",
    "Гобиодон голубопятнистый" to "fish/gobiodon_golubopyatnistiy.webp",
    "Bluespotted Goby" to "fish/gobiodon_golubopyatnistiy.webp",
    "Сиган коричневопятнистый" to "fish/sigan_korichnevopyatnistiy.webp",
    "Brown-spotted Rabbitfish" to "fish/sigan_korichnevopyatnistiy.webp",
    "Хирург полосатый" to "fish/hirurg_polosatiy.webp",
    "Striped Surgeonfish" to "fish/hirurg_polosatiy.webp",
    "Луциан серебристо-пятнистый" to "fish/lucian_serebristo-pyatnistiy.webp",
    "Silverspot Snapper" to "fish/lucian_serebristo-pyatnistiy.webp",
    "Скорпена бородатая" to "fish/skorpena_borodataya.webp",
    "Bearded Scorpionfish" to "fish/skorpena_borodataya.webp",
    "Барракуда полосатая" to "fish/barrakuda_polosataya.webp",
    "Striped Barracuda" to "fish/barrakuda_polosataya.webp",
    "Каранкс шестиполосый" to "fish/karanks_polosatiy.webp",
    "Sixband Trevally" to "fish/karanks_polosatiy.webp",
    "Группер леопардовый коралловый" to "fish/grupper_leopardoviy_koralloviy.webp",
    "Leopard Coral Grouper" to "fish/grupper_leopardoviy_koralloviy.webp",
    "Иглорыл-агухон" to "fish/igloryl-aguhon.webp",
    "Agujon Needlefish" to "fish/igloryl-aguhon.webp",
    "Акула рифовая чёрнопёрая" to "fish/akula_rifovaya_chernoperaya.webp",
    "Blacktip Reef Shark" to "fish/akula_rifovaya_chernoperaya.webp",
    "Губан-чистильщик" to "fish/guban-chistilschik.webp",
    "Cleaner Wrasse" to "fish/guban-chistilschik.webp",
    "Сержант-майор атлантический" to "fish/sergant-major_atlanticheskiy.webp",
    "Atlantic Sergeant Major" to "fish/sergant-major_atlanticheskiy.webp",
    "Грамма королевская" to "fish/gramma_korolevskaya.webp",
    "Royal Gramma" to "fish/gramma_korolevskaya.webp",
    "Ангел королевский" to "fish/angel_korolevskiy.webp",
    "Queen Angelfish" to "fish/angel_korolevskiy.webp",
    "Мандариновая рыба" to "fish/mandarinivaya_ryba.webp",
    "Mandarin Dragonet" to "fish/mandarinivaya_ryba.webp",
    "Крылатка зебровая" to "fish/krylaka_zebrovaya.webp",
    "Zebra Lionfish" to "fish/krylaka_zebrovaya.webp",
    "Рыба-флейта" to "fish/ryba-fleita.webp",
    "Trumpetfish" to "fish/ryba-fleita.webp",
)
