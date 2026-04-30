package com.riverking.mobile.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.R
import com.riverking.mobile.auth.AchievementClaimDto
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.AchievementRewardDto
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.CatchStatsDto
import com.riverking.mobile.auth.ClubChatMessageDto
import com.riverking.mobile.auth.ClubDetailsDto
import com.riverking.mobile.auth.ClubMemberDto
import com.riverking.mobile.auth.ClubQuestDto
import com.riverking.mobile.auth.ClubQuestMemberDto
import com.riverking.mobile.auth.CurrentTournamentDto
import com.riverking.mobile.auth.CastZoneDto
import com.riverking.mobile.auth.CastZonePointDto
import com.riverking.mobile.auth.FishBriefDto
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
import com.riverking.mobile.auth.RodDto
import com.riverking.mobile.auth.ShopPackageDto
import com.riverking.mobile.auth.SpecialEventClubEntryDto
import com.riverking.mobile.auth.SpecialEventPersonalEntryDto
import com.riverking.mobile.auth.SpecialEventResponseDto
import com.riverking.mobile.auth.TournamentDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class MainTab(val icon: ImageVector) {
    FISHING(Icons.Rounded.SportsEsports),
    LEADERS(Icons.Rounded.EmojiEvents),
    CATALOG(Icons.AutoMirrored.Rounded.MenuBook),
    CLUB(Icons.Rounded.Groups),
    SHOP(Icons.Rounded.ShoppingBag),
}

private enum class LeaderSection {
    TOURNAMENTS,
    RATINGS,
    ACHIEVEMENTS,
}

private enum class CatalogSection {
    LOCATIONS,
    FISH,
    GEAR,
}

private enum class LocationCatalogKind {
    REGULAR,
    EVENT,
}

private enum class GearSection {
    RODS,
    LURES,
}

private enum class FishingSheetType {
    LOCATIONS,
    RODS,
    LURES,
    QUESTS,
}

private data class BobberVisualState(
    val offset: Float = 0f,
    val xOffset: Float = 0f,
    val tilt: Float = 0f,
    val submerge: Float = 0f,
)

private data class CatchLiftAnimationState(
    val catchId: Long,
    val fish: String,
    val rarity: String,
    val start: Offset,
    val end: Offset,
    val progress: Float = 0f,
)

private data class ProFishingSceneSpec(
    val minX: Float = PRO_CAST_MIN_X,
    val maxX: Float = PRO_CAST_MAX_X,
    val farY: Float = PRO_CAST_FAR_Y,
    val nearY: Float = PRO_CAST_NEAR_Y,
)

private data class ProFishingCastZone(
    val points: List<Offset>,
)

private data class PanViewportSpec(
    val visibleWidth: Float = 1f,
    val maxPan: Float = 0f,
    val centeredLeft: Float = 0f,
    val canPan: Boolean = false,
)

private data class CameraPanBounds(
    val minLeft: Float = 0f,
    val maxLeft: Float = 0f,
)

private class ReadyPanGestureState {
    var primaryPointerId: Int = MotionEvent.INVALID_POINTER_ID
    var startX: Float = 0f
    var startY: Float = 0f
    var lastX: Float = 0f
    var lastY: Float = 0f
    var startedAtMillis: Long = 0L
    var panActive: Boolean = false
    var hadPanGesture: Boolean = false
    var panStartOffsetPx: Float = 0f
    val panStartXByPointer = linkedMapOf<Int, Float>()

    fun reset() {
        primaryPointerId = MotionEvent.INVALID_POINTER_ID
        startX = 0f
        startY = 0f
        lastX = 0f
        lastY = 0f
        startedAtMillis = 0L
        panActive = false
        hadPanGesture = false
        panStartOffsetPx = 0f
        panStartXByPointer.clear()
    }
}

private const val TG_CAST_WATER_TOP = 0.48f
private const val TG_CAST_LEFT_MARGIN = 0.05f
private const val TG_CAST_MIN_DISTANCE_FROM_TIP = 0.05f
private const val TG_CAST_MAX_DISTANCE_FROM_TIP = 0.20f
private const val TG_CAST_MIN_WATER_DEPTH = 0.12f
private const val TG_CAST_WATER_DEPTH_VARIANCE = 0.18f
private const val PRO_CAST_MIN_X = 0.14f
private const val PRO_CAST_MAX_X = 0.86f
private const val PRO_CAST_FAR_Y = 0.47f
private const val PRO_CAST_NEAR_Y = 0.78f
private const val CAST_AREA_EXPAND_MIN_X = 0.014f
private const val CAST_AREA_EXPAND_VIEWPORT_X = 0.06f
private const val CAST_AREA_EXPAND_TOP = 0.008f
private const val CAST_AREA_EXPAND_BOTTOM = 0.02f
private const val CAST_AREA_MIN_VISIBLE_WORLD = 0.08f
private const val CAST_AREA_MIN_VISIBLE_VIEWPORT = 0.28f
private const val PRO_CAST_SHORE_X = 0.44f
private const val PRO_CAST_SHORE_Y = 0.56f
private const val PRO_CAST_MIN_SWIPE_DP = 40f
private const val PAN_EDGE_TAP_FRACTION = 0.25f
private const val PAN_EDGE_TAP_MAX_DISTANCE_DP = 22f
private const val PAN_EDGE_TAP_MAX_DURATION_MILLIS = 260L
private const val PAN_STEP_VIEWPORT_MULTIPLIER = 0.55f
private const val CAST_ANIMATION_MIN_MILLIS = 280
private const val CAST_ANIMATION_MAX_MILLIS = 760
private const val CAST_ANIMATION_DEFAULT_MILLIS = 560
private const val CATCH_LIFT_ANIMATION_MILLIS = 900

private enum class ProFishingNoticeTone {
    ERROR,
    INFO,
}

private data class ProFishingNotice(
    val token: Long,
    val text: String,
    val visible: Boolean,
    val tone: ProFishingNoticeTone = ProFishingNoticeTone.ERROR,
)

@Composable
fun MainShell(
    state: RiverKingUiState,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAccountDeletionHelp: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onClaimDaily: () -> Unit,
    onBeginCast: (FishingCastSpot?) -> Unit,
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
    onLoadEventGuideLocations: (Boolean) -> Unit,
    onClaimAchievement: (String) -> Unit,
    onDismissAchievementReward: () -> Unit,
    onLoadClub: (Boolean) -> Unit,
    onLoadClubChat: () -> Unit,
    onLoadOlderClubChat: () -> Unit,
    onSendClubChatMessage: (String) -> Unit,
    onSearchClubs: (String?) -> Unit,
    onCreateClub: (String) -> Unit,
    onJoinClub: (Long) -> Unit,
    onUpdateClubInfo: (String) -> Unit,
    onUpdateClubSettings: (Double, Boolean) -> Unit,
    onLeaveClub: () -> Unit,
    onClubMemberAction: (Long, String) -> Unit,
    onStartTelegramLink: () -> Unit,
    onLoadShop: (Boolean) -> Unit,
    onLoadReferrals: (Boolean) -> Unit,
    onGenerateReferral: () -> Unit,
    onShareReferral: (String) -> Unit,
    onClaimReferralRewards: () -> Unit,
    onBuyShopWithCoins: (String) -> Unit,
    onPlayPurchase: (String) -> Unit,
    onUpdateNickname: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onLoadCatchStats: (String) -> Unit,
    onShowErrorMessage: suspend (String) -> Unit,
    onConsumeError: () -> Unit,
    onDismissFishingOutcome: () -> Unit,
) {
    val me = state.me ?: return
    val strings = rememberRiverStrings(me.language)
    val clipboard = LocalClipboardManager.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FISHING) }
    var showNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var showCatchStats by rememberSaveable { mutableStateOf(false) }
    var showDailyRewardSheet by rememberSaveable { mutableStateOf(false) }
    var showTelegramAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showReferralSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    var proFishingNotice by remember { mutableStateOf<ProFishingNotice?>(null) }

    val tournamentBadge = state.tournaments.prizes.any { isTournamentPrize(it) || isEventPrize(it) }
    val ratingBadge = state.tournaments.prizes.any(::isRatingPrize)
    val clubBadge = state.tournaments.prizes.any(::isClubPrize)
    val achievementBadge = state.guide.achievements.any { it.claimable }
    val leadersBadge = tournamentBadge || ratingBadge || achievementBadge
    val showReferralMenuItem =
        canShowTelegramReferral(state.authProvider, me) || canShowStoreReferral(state.authProvider, me)
    val tabLabels = remember(strings) {
        mapOf(
            MainTab.FISHING to strings.fishing,
            MainTab.LEADERS to strings.leaders,
            MainTab.CATALOG to strings.catalog,
            MainTab.CLUB to strings.club,
            MainTab.SHOP to strings.shop,
        )
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != MainTab.FISHING) {
            onDismissFishingOutcome()
        }
    }

    LaunchedEffect(state.error, selectedTab, strings) {
        val rawMessage = state.error ?: return@LaunchedEffect
        val message = localizedAppError(strings, rawMessage)
        if (selectedTab == MainTab.FISHING) {
            val token = System.nanoTime()
            proFishingNotice = ProFishingNotice(token = token, text = message, visible = true)
            delay(2500L)
            if (proFishingNotice?.token == token) {
                proFishingNotice = proFishingNotice?.copy(visible = false)
            }
            delay(500L)
            if (proFishingNotice?.token == token) {
                proFishingNotice = null
            }
            onConsumeError()
        } else {
            onShowErrorMessage(message)
            onConsumeError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HeaderBar(
                me = me,
                strings = strings,
                onOpenDaily = { showDailyRewardSheet = true },
                onLogout = onLogout,
                onDeleteAccount = { showDeleteAccountDialog = true },
                onOpenSupport = onOpenSupport,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onChangeLanguage = onChangeLanguage,
                onOpenNicknameChange = {
                    onUpdateNickname(me.username ?: "")
                    showNicknameDialog = true
                },
                onOpenTelegramAccount = {
                    showTelegramAccountSheet = true
                },
                showReferralEntry = showReferralMenuItem,
                onOpenReferrals = {
                    showReferralSheet = true
                    onLoadReferrals(false)
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
            ) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = Color.Transparent,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    MainTab.entries.forEach { tab ->
                        val showBadge = when (tab) {
                            MainTab.LEADERS -> leadersBadge
                            MainTab.CLUB -> clubBadge
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            alwaysShowLabel = true,
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
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tabLabels.getValue(tab),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tabLabels.getValue(tab),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
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
                    proNotice = proFishingNotice,
                    onOpenDaily = { showDailyRewardSheet = true },
                    onBeginCast = onBeginCast,
                    onHookFish = onHookFish,
                    onTapChallenge = onTapChallenge,
                    onToggleAutoCast = onToggleAutoCast,
                    onSelectLocation = onSelectLocation,
                    onSelectLure = onSelectLure,
                    onSelectRod = onSelectRod,
                    onLoadGuide = onLoadGuide,
                )
                MainTab.LEADERS -> LeadersScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onReloadTournaments = onLoadTournaments,
                    onReloadRatings = onLoadRatings,
                    onOpenTournament = onOpenTournament,
                    onClaimPrize = onClaimPrize,
                    onSetMode = onSetRatingsMode,
                    onSetPeriod = onSetRatingsPeriod,
                    onSetOrder = onSetRatingsOrder,
                    onSetLocation = onSetRatingsLocation,
                    onSetFish = onSetRatingsFish,
                    onClaimAchievement = onClaimAchievement,
                    onReloadGuide = onLoadGuide,
                    onOpenCatch = onOpenCatch,
                )
                MainTab.CATALOG -> CatalogScreen(
                    state = state,
                    strings = strings,
                    isPlayFlavor = isPlayFlavor,
                    requestPlayPrice = requestPlayPrice,
                    onLoadEventGuideLocations = onLoadEventGuideLocations,
                    modifier = Modifier.padding(padding),
                )
                MainTab.CLUB -> ClubScreen(
                    state = state,
                    strings = strings,
                    modifier = Modifier.padding(padding),
                    onReloadClub = onLoadClub,
                    onLoadChat = onLoadClubChat,
                    onLoadOlderChat = onLoadOlderClubChat,
                    onSendChatMessage = onSendClubChatMessage,
                    onSearchClubs = onSearchClubs,
                    onCreateClub = onCreateClub,
                    onJoinClub = onJoinClub,
                    onUpdateClubInfo = onUpdateClubInfo,
                    onUpdateClubSettings = onUpdateClubSettings,
                    onLeaveClub = onLeaveClub,
                    onMemberAction = onClubMemberAction,
                    onClaimPrize = onClaimPrize,
                    onOpenCatch = onOpenCatch,
                )
                MainTab.SHOP -> ShopScreen(
                    state = state,
                    strings = strings,
                    isPlayFlavor = isPlayFlavor,
                    requestPlayPrice = requestPlayPrice,
                    modifier = Modifier.padding(padding),
                    onBuyShopWithCoins = onBuyShopWithCoins,
                    onPlayPurchase = onPlayPurchase,
                )
            }
        }
    }

    state.selectedCatch?.let { catch ->
        val fishDiscovered = isFishDiscovered(
            fishId = catch.fishId,
            fishName = catch.fish,
            ownerUserId = catch.userId,
            currentUserId = me.id,
            caughtFishIds = me.caughtFishIds,
            fishGuide = state.guide.guide?.fish,
        )
        CatchDetailsDialog(
            strings = strings,
            catch = catch,
            me = me,
            resolvedRarity = if (fishDiscovered) resolveCatchRarity(catch, state.guide.guide?.fish) else null,
            fishDiscovered = fishDiscovered,
            allowShare = catch.userId != null && catch.userId == me.id,
            loading = state.catchLoading,
            onDismiss = onDismissCatch,
            onShare = { onShareCatch(catch) },
        )
    }

    state.lastAchievementReward?.let { reward ->
        AchievementRewardDialog(
            strings = strings,
            reward = reward,
            shopPacks = state.shop.categories.flatMap { it.packs },
            onDismiss = onDismissAchievementReward,
        )
    }

    state.tournaments.selectedTournament?.let { tournament ->
        TournamentDialog(
            strings = strings,
            details = tournament,
            shopPacks = state.shop.categories.flatMap { it.packs },
            caughtFishIds = me.caughtFishIds,
            fishGuide = state.guide.guide?.fish,
            currentUserId = me.id,
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

    if (showTelegramAccountSheet) {
        TelegramAccountSheet(
            strings = strings,
            me = me,
            pending = state.telegramLinkPending,
            onStartTelegramLink = onStartTelegramLink,
            onDismiss = { showTelegramAccountSheet = false },
        )
    }

    if (showReferralSheet) {
        ReferralSheet(
            strings = strings,
            me = me,
            state = state.referrals,
            sessionAuthProvider = state.authProvider,
            clipboard = clipboard,
            onGenerateReferral = onGenerateReferral,
            onShareReferral = onShareReferral,
            onClaimReferralRewards = onClaimReferralRewards,
            onDismiss = { showReferralSheet = false },
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(strings.deleteAccountTitle) },
            text = { Text(strings.deleteAccountMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    },
                ) {
                    Text(strings.deleteAccountConfirm)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showDeleteAccountDialog = false
                            onOpenAccountDeletionHelp()
                        },
                    ) {
                        Text(strings.deleteAccountWeb)
                    }
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            },
        )
    }
}

@Composable
private fun LeadersScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onReloadTournaments: (Boolean) -> Unit,
    onReloadRatings: (Boolean) -> Unit,
    onOpenTournament: (Long) -> Unit,
    onClaimPrize: (Long) -> Unit,
    onSetMode: (RatingsMode) -> Unit,
    onSetPeriod: (RatingsPeriod) -> Unit,
    onSetOrder: (RatingsOrder) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetFish: (String) -> Unit,
    onClaimAchievement: (String) -> Unit,
    onReloadGuide: (Boolean) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(LeaderSection.TOURNAMENTS) }
    val tournamentsBadge = state.tournaments.prizes.any { isTournamentPrize(it) || isEventPrize(it) }
    val ratingsBadge = state.tournaments.prizes.any(::isRatingPrize)
    val achievementsBadge = state.guide.achievements.any { it.claimable }

    LaunchedEffect(section) {
        when (section) {
            LeaderSection.TOURNAMENTS -> onReloadTournaments(true)
            LeaderSection.RATINGS -> onReloadRatings(true)
            LeaderSection.ACHIEVEMENTS -> onReloadGuide(true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            SectionCard(strings.leaders) {
                SegmentedSelectionBar(
                    items = LeaderSection.entries.toList(),
                    selected = section,
                    onSelect = { section = it },
                    accentFor = {
                        when (it) {
                            LeaderSection.TOURNAMENTS -> RiverTide
                            LeaderSection.RATINGS -> RiverMoss
                            LeaderSection.ACHIEVEMENTS -> RiverAmber
                        }
                    },
                    labelFor = {
                        when (it) {
                            LeaderSection.TOURNAMENTS -> strings.tournaments
                            LeaderSection.RATINGS -> strings.ratings
                            LeaderSection.ACHIEVEMENTS -> strings.achievements
                        }
                    },
                    badgeFor = {
                        when (it) {
                            LeaderSection.TOURNAMENTS -> tournamentsBadge
                            LeaderSection.RATINGS -> ratingsBadge
                            LeaderSection.ACHIEVEMENTS -> achievementsBadge
                        }
                    },
                )
            }
        }
        when (section) {
            LeaderSection.TOURNAMENTS -> TournamentsScreen(
                state = state,
                strings = strings,
                modifier = Modifier.weight(1f),
                onOpenTournament = onOpenTournament,
                onClaimPrize = onClaimPrize,
                onOpenCatch = onOpenCatch,
            )
            LeaderSection.RATINGS -> RatingsScreen(
                state = state,
                strings = strings,
                modifier = Modifier.weight(1f),
                onClaimPrize = onClaimPrize,
                onSetMode = onSetMode,
                onSetPeriod = onSetPeriod,
                onSetOrder = onSetOrder,
                onSetLocation = onSetLocation,
                onSetFish = onSetFish,
                onOpenCatch = onOpenCatch,
            )
            LeaderSection.ACHIEVEMENTS -> AchievementsScreen(
                state = state,
                strings = strings,
                modifier = Modifier.weight(1f),
                onClaimAchievement = onClaimAchievement,
                onReloadGuide = onReloadGuide,
            )
        }
    }
}

@Composable
private fun AchievementsScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    modifier: Modifier = Modifier,
    onClaimAchievement: (String) -> Unit,
    onReloadGuide: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.achievements) {
                when {
                    state.guide.loading && state.guide.achievements.isEmpty() -> LoadingStatePanel(strings.loading)
                    state.guide.achievements.isEmpty() -> {
                        EmptyStatePanel(strings.noData)
                        OutlinedButton(
                            onClick = { onReloadGuide(true) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(strings.refresh)
                        }
                    }
                    else -> {
                        state.guide.achievements.forEachIndexed { index, achievement ->
                            if (index > 0) {
                                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            }
                            AchievementRow(strings, achievement, onClaimAchievement)
                        }
                    }
                }
            }
        }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
    ) {
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
                            value = stats.totalWeight.asKgCompact(strings),
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
                                    text = "${rarity.count} • ${rarity.weight.asKgCompact(strings)}",
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
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverDialogShape,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        title = { Text(strings.changeNickname) },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(strings.chooseNickname) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = riverTextFieldColors(),
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !busy && nickname.isNotBlank(),
                colors = riverPrimaryButtonColors(),
            ) {
                Text(strings.continueLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
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
private fun RiverSheetDragHandle() {
    Surface(
        modifier = Modifier
            .padding(top = 8.dp)
            .width(46.dp)
            .height(6.dp),
        shape = RoundedCornerShape(999.dp),
        color = RiverFog.copy(alpha = 0.32f),
    ) {}
}

@Composable
private fun riverMenuItemColors(destructive: Boolean = false) = MenuDefaults.itemColors(
    textColor = if (destructive) RiverDanger else MaterialTheme.colorScheme.onSurface,
    leadingIconColor = if (destructive) RiverDanger else MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun HeaderBar(
    me: MeResponseDto,
    strings: RiverStrings,
    onOpenDaily: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onOpenNicknameChange: () -> Unit,
    onOpenTelegramAccount: () -> Unit,
    showReferralEntry: Boolean,
    onOpenReferrals: () -> Unit,
    onOpenCatchStats: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
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
                    shape = RoundedCornerShape(24.dp),
                    containerColor = RiverPanelRaised.copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
                    shadowElevation = 18.dp,
                ) {
                    DropdownMenuItem(
                        text = { Text("✏\uFE0F  ${strings.changeNickname}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            onOpenNicknameChange()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("\uD83D\uDCCA  ${strings.statistics}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            onOpenCatchStats()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("\uD83D\uDCF1  ${strings.telegramAccount}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            onOpenTelegramAccount()
                        },
                    )
                    if (showReferralEntry) {
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDC65  ${strings.inviteFriends}") },
                            colors = riverMenuItemColors(),
                            onClick = {
                                menuExpanded = false
                                onOpenReferrals()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("$languageFlag  ${strings.language}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            languagePickerOpen = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("\uD83D\uDEE0\uFE0F  ${strings.support}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            onOpenSupport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("\uD83D\uDD12  ${strings.privacyPolicy}") },
                        colors = riverMenuItemColors(),
                        onClick = {
                            menuExpanded = false
                            onOpenPrivacyPolicy()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(text = strings.logout, fontWeight = FontWeight.SemiBold) },
                        colors = riverMenuItemColors(destructive = true),
                        onClick = {
                            menuExpanded = false
                            onLogout()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = strings.deleteAccount, fontWeight = FontWeight.SemiBold) },
                        colors = riverMenuItemColors(destructive = true),
                        onClick = {
                            menuExpanded = false
                            onDeleteAccount()
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

    if (languagePickerOpen) {
        LanguagePickerDialog(
            strings = strings,
            currentLanguage = me.language,
            onDismiss = { languagePickerOpen = false },
            onSelect = { selectedLanguage ->
                languagePickerOpen = false
                if (selectedLanguage != me.language) {
                    onChangeLanguage(selectedLanguage)
                }
            },
        )
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
    proNotice: ProFishingNotice?,
    onOpenDaily: () -> Unit,
    onBeginCast: (FishingCastSpot?) -> Unit,
    onHookFish: () -> Unit,
    onTapChallenge: () -> Unit,
    onToggleAutoCast: () -> Unit,
    onSelectLocation: (Long) -> Unit,
    onSelectLure: (Long) -> Unit,
    onSelectRod: (Long) -> Unit,
    onLoadGuide: (Boolean) -> Unit,
) {
    val me = state.me ?: return
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }
    var activeSheet by rememberSaveable { mutableStateOf<FishingSheetType?>(null) }
    var lastDailyPromptToken by rememberSaveable(me.id) { mutableStateOf<String?>(null) }
    var escapeNotice by remember { mutableStateOf<ProFishingNotice?>(null) }
    var outcomeNotice by remember { mutableStateOf<ProFishingNotice?>(null) }
    val setupEnabled = !isFishingCastActive(state.fishing.phase)
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

    LaunchedEffect(state.fishing.lastEscape) {
        if (!state.fishing.lastEscape) {
            escapeNotice = null
            return@LaunchedEffect
        }
        outcomeNotice = null
        val token = System.nanoTime()
        escapeNotice = ProFishingNotice(token = token, text = strings.fishEscaped, visible = true)
        delay(2500L)
        if (escapeNotice?.token == token) {
            escapeNotice = escapeNotice?.copy(visible = false)
        }
        delay(500L)
        if (escapeNotice?.token == token) {
            escapeNotice = null
        }
    }

    LaunchedEffect(state.fishing.lastCast?.catch?.id) {
        val text = fishingOutcomeNoticeText(
            strings = strings,
            fishing = state.fishing,
            achievements = state.guide.achievements,
        ) ?: run {
            outcomeNotice = null
            return@LaunchedEffect
        }
        escapeNotice = null
        val token = System.nanoTime()
        outcomeNotice = ProFishingNotice(
            token = token,
            text = text,
            visible = true,
            tone = ProFishingNoticeTone.INFO,
        )
        delay(5200L)
        if (outcomeNotice?.token == token) {
            outcomeNotice = outcomeNotice?.copy(visible = false)
        }
        delay(500L)
        if (outcomeNotice?.token == token) {
            outcomeNotice = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FishingStageScene(
            state = state,
            strings = strings,
            me = me,
            backgroundUrl = currentLocation?.imageUrl ?: locationBackgroundAsset(currentLocation?.name),
            locationName = currentLocation?.name,
            castZone = currentLocation?.castZone,
            hasCustomCastZone = currentLocation?.castZone != null,
            modifier = Modifier.fillMaxSize(),
            proMode = true,
            setupEnabled = setupEnabled,
            autoCastEnabled = state.fishing.autoCastEnabled,
            proNotice = proNotice ?: escapeNotice ?: outcomeNotice,
            onToggleAutoCast = onToggleAutoCast,
            onOpenLocations = { activeSheet = FishingSheetType.LOCATIONS },
            onOpenLures = { activeSheet = FishingSheetType.LURES },
            onOpenRods = { activeSheet = FishingSheetType.RODS },
            onOpenQuests = {
                activeSheet = FishingSheetType.QUESTS
                if (state.guide.quests == null) {
                    onLoadGuide(true)
                }
            },
            onBeginCast = onBeginCast,
            onHookFish = onHookFish,
            onTapChallenge = onTapChallenge,
        )
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
            isClubMember = state.club.club != null,
            clubMembershipKnown = state.club.loaded,
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
    onOpenTournament: (Long) -> Unit,
    onClaimPrize: (Long) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val tournaments = state.tournaments
    val me = state.me
    val shopPacks = state.shop.categories.flatMap { it.packs }
    var tournamentMode by rememberSaveable { mutableStateOf("regular") }
    var eventPeriod by rememberSaveable { mutableStateOf("current") }
    val showingSpecial = tournamentMode == "special"
    val visiblePrizes = tournaments.prizes.filter { prize ->
        if (showingSpecial) isEventPrize(prize) else isTournamentPrize(prize)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !showingSpecial,
                    onClick = { tournamentMode = "regular" },
                    label = { Text(regularTournamentsLabel(strings)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = showingSpecial,
                    onClick = { tournamentMode = "special" },
                    label = { Text(specialTournamentsLabel(strings)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (visiblePrizes.isNotEmpty()) {
            item {
                PendingPrizeCard(
                    strings = strings,
                    prizes = visiblePrizes,
                    onClaimPrize = onClaimPrize,
                )
            }
        }
        if (!showingSpecial) {
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
                                    title = "${entry.rank} ${entry.user ?: "Unknown"}",
                                    subtitle = listOfNotNull(entry.fish, entry.location).joinToString(" • "),
                                    value = entry.value.asKgCompact(strings),
                                    fishName = entry.fish,
                                    fishDiscovered = me?.let {
                                        isFishDiscovered(
                                            fishId = entry.fishId,
                                            fishName = entry.fish,
                                            ownerUserId = entry.userId,
                                            currentUserId = it.id,
                                            caughtFishIds = it.caughtFishIds,
                                            fishGuide = state.guide.guide?.fish,
                                        )
                                    } == true,
                                    fishAccent = rarityColor(tournaments.current.tournament.fishRarity),
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
                                                    fishId = entry.fishId,
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
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = eventPeriod == "previous",
                        onClick = { eventPeriod = "previous" },
                        label = { Text(previousLabel(strings)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = eventPeriod == "current",
                        onClick = { eventPeriod = "current" },
                        label = { Text(currentTournamentPeriodLabel(strings)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                val event = if (eventPeriod == "previous") tournaments.previousEvent else tournaments.currentEvent
                if (event == null) {
                    EmptyStatePanel(eventsEmptyLabel(strings))
                } else {
                    SpecialEventCard(
                        strings = strings,
                        event = event,
                        me = me,
                        shopPacks = shopPacks,
                        onOpenCatch = onOpenCatch,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecialEventCard(
    strings: RiverStrings,
    event: SpecialEventResponseDto,
    me: MeResponseDto?,
    shopPacks: List<ShopPackageDto>,
    onOpenCatch: (CatchDto) -> Unit,
) {
    var selectedBoard by rememberSaveable(event.event.id) { mutableStateOf(EVENT_BOARD_WEIGHT) }
    SectionCard(event.event.name) {
        event.event.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = event.event.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = "${formatEpoch(event.event.startTime)} – ${formatEpoch(event.event.endTime)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionDropdown(
            title = eventLeaderboardLabel(strings),
            selectedLabel = specialEventBoardTitle(strings, selectedBoard),
            options = eventLeaderboardOptions(strings),
            selectedKey = selectedBoard,
            onSelect = { selectedBoard = it },
        )
        Spacer(modifier = Modifier.height(12.dp))
        SpecialEventSelectedBoard(
            strings = strings,
            event = event,
            selectedBoard = selectedBoard,
            me = me,
            shopPacks = shopPacks,
            onOpenCatch = onOpenCatch,
        )
    }
}

@Composable
private fun SpecialEventSelectedBoard(
    strings: RiverStrings,
    event: SpecialEventResponseDto,
    selectedBoard: String,
    me: MeResponseDto?,
    shopPacks: List<ShopPackageDto>,
    onOpenCatch: (CatchDto) -> Unit,
) {
    when (selectedBoard) {
        EVENT_BOARD_COUNT -> SpecialEventClubSection(
            strings = strings,
            title = eventTotalCountLabel(strings),
            rows = event.leaderboards.totalCount,
            mine = event.leaderboards.mineTotalCount,
            countMode = true,
            shopPacks = shopPacks,
            showTitle = false,
        )

        EVENT_BOARD_FISH -> SpecialEventPersonalSection(
            strings = strings,
            event = event,
            rows = event.leaderboards.personalFish,
            mine = event.leaderboards.minePersonalFish,
            me = me,
            shopPacks = shopPacks,
            onOpenCatch = onOpenCatch,
            showTitle = false,
        )

        else -> SpecialEventClubSection(
            strings = strings,
            title = eventTotalWeightLabel(strings),
            rows = event.leaderboards.totalWeight,
            mine = event.leaderboards.mineTotalWeight,
            countMode = false,
            shopPacks = shopPacks,
            showTitle = false,
        )
    }
}

@Composable
private fun SpecialEventClubSection(
    strings: RiverStrings,
    title: String,
    rows: List<SpecialEventClubEntryDto>,
    mine: SpecialEventClubEntryDto?,
    countMode: Boolean,
    shopPacks: List<ShopPackageDto>,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
    }
    if (rows.isEmpty()) {
        EmptyStatePanel("—")
    } else {
        rows.take(10).forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            SpecialEventClubRow(
                strings = strings,
                row = row,
                highlighted = mine?.rank == row.rank,
                countMode = countMode,
                shopPacks = shopPacks,
            )
        }
        if (mine != null && rows.none { it.rank == mine.rank }) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            SpecialEventClubRow(
                strings = strings,
                row = mine,
                highlighted = true,
                countMode = countMode,
                shopPacks = shopPacks,
            )
        }
    }
}

@Composable
private fun SpecialEventClubRow(
    strings: RiverStrings,
    row: SpecialEventClubEntryDto,
    highlighted: Boolean,
    countMode: Boolean,
    shopPacks: List<ShopPackageDto>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) Color(0xFF1F6F4A).copy(alpha = 0.22f) else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("${row.rank} ${row.club}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.prize?.let { prize ->
                PrizeChip(strings = strings, prize = prize, shopPacks = shopPacks)
            }
        }
        Text(if (countMode) row.value.roundToInt().toString() else row.value.asKgCompact(strings))
    }
}

@Composable
private fun SpecialEventPersonalSection(
    strings: RiverStrings,
    event: SpecialEventResponseDto,
    rows: List<SpecialEventPersonalEntryDto>,
    mine: SpecialEventPersonalEntryDto?,
    me: MeResponseDto?,
    shopPacks: List<ShopPackageDto>,
    onOpenCatch: (CatchDto) -> Unit,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        Text(eventTopFishLabel(strings), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
    }
    if (rows.isEmpty()) {
        EmptyStatePanel(strings.noData)
    } else {
        rows.take(10).forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            SpecialEventPersonalRow(
                strings = strings,
                event = event,
                row = row,
                highlighted = mine?.rank == row.rank,
                me = me,
                shopPacks = shopPacks,
                onOpenCatch = onOpenCatch,
            )
        }
        if (mine != null && rows.none { it.rank == mine.rank }) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            SpecialEventPersonalRow(
                strings = strings,
                event = event,
                row = mine,
                highlighted = true,
                me = me,
                shopPacks = shopPacks,
                onOpenCatch = onOpenCatch,
            )
        }
    }
}

@Composable
private fun SpecialEventPersonalRow(
    strings: RiverStrings,
    event: SpecialEventResponseDto,
    row: SpecialEventPersonalEntryDto,
    highlighted: Boolean,
    me: MeResponseDto?,
    shopPacks: List<ShopPackageDto>,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val discovered = me?.caughtFishIds?.contains(row.fishId) == true || me?.id == row.userId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = row.catchId != null) {
                row.catchId?.let {
                    onOpenCatch(
                        CatchDto(
                            id = it,
                            fish = row.fish,
                            weight = row.weight,
                            location = event.event.name,
                            rarity = row.rarity,
                            userId = row.userId,
                            fishId = row.fishId,
                            user = row.user,
                            at = Instant.ofEpochSecond(row.at).toString(),
                        )
                    )
                }
            }
            .background(if (highlighted) Color(0xFF1F6F4A).copy(alpha = 0.22f) else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("${row.rank} ${row.user ?: youLabel(strings)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (discovered) "${row.fish} • ${row.weight.asKgCompact(strings)}" else "??? • ${row.weight.asKgCompact(strings)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            row.prize?.let { prize ->
                PrizeChip(strings = strings, prize = prize, shopPacks = shopPacks)
            }
        }
        Text(row.value.asKgCompact(strings))
    }
}

@Composable
private fun PendingPrizeCard(
    strings: RiverStrings,
    prizes: List<PrizeDto>,
    onClaimPrize: (Long) -> Unit,
) {
    SectionCard(strings.prizes) {
        prizes.forEachIndexed { index, prize ->
            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pendingPrizeLabel(strings, prize), fontWeight = FontWeight.SemiBold)
                    tournamentPrizeDetailsLabel(strings, prize)?.let { details ->
                        Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = { onClaimPrize(prize.id) }) {
                    Text(strings.claim)
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
    onClaimPrize: (Long) -> Unit,
    onSetMode: (RatingsMode) -> Unit,
    onSetPeriod: (RatingsPeriod) -> Unit,
    onSetOrder: (RatingsOrder) -> Unit,
    onSetLocation: (String) -> Unit,
    onSetFish: (String) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val me = state.me ?: return
    val ratings = state.ratings
    val ratingPrizes = state.tournaments.prizes.filter(::isRatingPrize)
    val filteredFishOptions = remember(ratings.fishOptions, ratings.locationId) {
        ratings.fishOptions
            .filter { fish ->
                ratings.locationId == "all" || fish.locationIds.contains(ratings.locationId.toLongOrNull())
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }
    val showPrizePreview = ratings.mode == RatingsMode.GLOBAL &&
        ratings.fishId == "all" &&
        ratings.locationId != "all" &&
        ratings.order == RatingsOrder.DESC
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ratingPrizes.isNotEmpty()) {
            item {
                SectionCard(strings.prizes) {
                    ratingPrizes.forEachIndexed { index, prize ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                pendingPrizeLabel(strings, prize),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = { onClaimPrize(prize.id) }) {
                                Text(strings.claim)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(strings.ratings) {
                SegmentedSelectionBar(
                    items = RatingsMode.entries.toList(),
                    selected = ratings.mode,
                    onSelect = onSetMode,
                    accentFor = { RiverTide },
                    labelFor = {
                        if (it == RatingsMode.PERSONAL) strings.personal else strings.global
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedSelectionBar(
                    items = RatingsOrder.entries.toList(),
                    selected = ratings.order,
                    onSelect = onSetOrder,
                    accentFor = { RiverAmber },
                    labelFor = strings::orderLabel,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SelectionDropdown(
                    title = if (strings.login == "Логин") "Период" else "Period",
                    selectedLabel = strings.periodLabel(ratings.period),
                    options = RatingsPeriod.entries.map { it.apiValue to strings.periodLabel(it) },
                    selectedKey = ratings.period.apiValue,
                    onSelect = { key ->
                        RatingsPeriod.entries.firstOrNull { it.apiValue == key }?.let(onSetPeriod)
                    },
                    accent = RiverAmber,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SelectionDropdown(
                    title = if (strings.login == "Логин") "Локация" else "Location",
                    selectedLabel = me.locations.firstOrNull { it.id.toString() == ratings.locationId }?.name ?: strings.allLocations,
                    options = buildList {
                        add("all" to strings.allLocations)
                        addAll(me.locations.map { it.id.toString() to it.name })
                    },
                    selectedKey = ratings.locationId,
                    onSelect = onSetLocation,
                    accent = RiverTide,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SelectionDropdown(
                    title = if (strings.login == "Логин") "Рыба" else "Fish",
                    selectedLabel = filteredFishOptions.firstOrNull { it.id.toString() == ratings.fishId }?.name ?: strings.allFish,
                    options = buildList {
                        add("all" to strings.allFish)
                        addAll(filteredFishOptions.map { it.id.toString() to it.name })
                    },
                    selectedKey = ratings.fishId,
                    onSelect = onSetFish,
                    accent = RiverMoss,
                )
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
                            fishDiscovered = isFishDiscovered(
                                fishId = catch.fishId,
                                fishName = catch.fish,
                                ownerUserId = catch.userId,
                                currentUserId = me.id,
                                caughtFishIds = me.caughtFishIds,
                                fishGuide = state.guide.guide?.fish,
                            ),
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
private fun CatalogScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
    onLoadEventGuideLocations: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val me = state.me ?: return
    val guide = state.guide
    var section by rememberSaveable { mutableStateOf(CatalogSection.LOCATIONS) }
    var locationKind by rememberSaveable { mutableStateOf(LocationCatalogKind.REGULAR) }
    var gearSection by rememberSaveable { mutableStateOf(GearSection.RODS) }
    var rarityFilter by rememberSaveable { mutableStateOf("all") }
    var showCaughtOnly by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(section, locationKind, guide.eventLocations.size, guide.eventLocationsHasMore, guide.eventLocationsLoading) {
        if (
            section == CatalogSection.LOCATIONS &&
            locationKind == LocationCatalogKind.EVENT &&
            guide.eventLocations.isEmpty() &&
            guide.eventLocationsHasMore &&
            !guide.eventLocationsLoading
        ) {
            onLoadEventGuideLocations(true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            SectionCard(strings.catalog) {
                SegmentedSelectionBar(
                    items = CatalogSection.entries.toList(),
                    selected = section,
                    onSelect = { section = it },
                    accentFor = {
                        when (it) {
                            CatalogSection.LOCATIONS -> RiverTide
                            CatalogSection.FISH -> RiverMoss
                            CatalogSection.GEAR -> RiverAmber
                        }
                    },
                    labelFor = {
                        when (it) {
                            CatalogSection.LOCATIONS -> strings.guideWaters
                            CatalogSection.FISH -> strings.guideFish
                            CatalogSection.GEAR -> strings.gear
                        }
                    },
                )
                if (section == CatalogSection.FISH) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RarityDropdown(
                            strings = strings,
                            selected = rarityFilter,
                            onSelect = { rarityFilter = it },
                            modifier = Modifier.weight(1f),
                        )
                        CaughtOnlyToggleCard(
                            strings = strings,
                            checked = showCaughtOnly,
                            onCheckedChange = { showCaughtOnly = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (section == CatalogSection.LOCATIONS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedSelectionBar(
                        items = LocationCatalogKind.entries.toList(),
                        selected = locationKind,
                        onSelect = { locationKind = it },
                        accentFor = {
                            when (it) {
                                LocationCatalogKind.REGULAR -> RiverTide
                                LocationCatalogKind.EVENT -> RiverAmber
                            }
                        },
                        labelFor = {
                            when (it) {
                                LocationCatalogKind.REGULAR -> regularTournamentsLabel(strings)
                                LocationCatalogKind.EVENT -> eventLocationsLabel(strings)
                            }
                        },
                    )
                }
                if (section == CatalogSection.GEAR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedSelectionBar(
                        items = GearSection.entries.toList(),
                        selected = gearSection,
                        onSelect = { gearSection = it },
                        accentFor = {
                            when (it) {
                                GearSection.RODS -> RiverAmber
                                GearSection.LURES -> RiverMoss
                            }
                        },
                        labelFor = {
                            when (it) {
                                GearSection.RODS -> strings.guideRods
                                GearSection.LURES -> strings.guideLures
                            }
                        },
                    )
                }
            }
        }

        when (section) {
            CatalogSection.FISH -> CatalogFishScreen(
                guide = guide,
                me = me,
                strings = strings,
                rarityFilter = rarityFilter,
                showCaughtOnly = showCaughtOnly,
                modifier = Modifier.weight(1f),
            )
            CatalogSection.LOCATIONS -> CatalogLocationsScreen(
                guide = guide,
                me = me,
                strings = strings,
                locationKind = locationKind,
                onLoadMoreEventGuideLocations = { onLoadEventGuideLocations(false) },
                modifier = Modifier.weight(1f),
            )
            CatalogSection.GEAR -> CatalogGearScreen(
                guide = guide,
                me = me,
                strings = strings,
                isPlayFlavor = isPlayFlavor,
                requestPlayPrice = requestPlayPrice,
                section = gearSection,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CatalogLocationsScreen(
    guide: GuideUiState,
    me: MeResponseDto,
    strings: RiverStrings,
    locationKind: LocationCatalogKind,
    onLoadMoreEventGuideLocations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val locations = when (locationKind) {
                LocationCatalogKind.REGULAR -> guide.guide?.locations.orEmpty()
                LocationCatalogKind.EVENT -> guide.eventLocations
            }
            SectionCard(if (locationKind == LocationCatalogKind.EVENT) eventLocationsLabel(strings) else strings.guideWaters) {
                if (locations.isEmpty()) {
                    if (locationKind == LocationCatalogKind.EVENT && guide.eventLocationsLoading) {
                        LoadingStatePanel(strings.loading)
                    } else {
                        EmptyStatePanel(strings.noData)
                    }
                }
                locations.forEachIndexed { index, location ->
                    if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                    GuideLocationRow(
                        strings = strings,
                        location = location,
                        ownedLocation = if (location.isEvent) null else me.locations.firstOrNull { it.id == location.id },
                    )
                }
            }
        }
        if (locationKind == LocationCatalogKind.EVENT && guide.eventLocationsHasMore && guide.eventLocations.isNotEmpty()) {
            item(key = "event-location-loader") {
                LaunchedEffect(guide.eventLocations.size, guide.eventLocationsLoading) {
                    if (!guide.eventLocationsLoading) onLoadMoreEventGuideLocations()
                }
                LoadingStatePanel(strings.loading)
            }
        }
    }
}

@Composable
private fun CatalogFishScreen(
    guide: GuideUiState,
    me: MeResponseDto,
    strings: RiverStrings,
    rarityFilter: String,
    showCaughtOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(strings.guideFish) {
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
        }
    }
}

@Composable
private fun CatalogGearScreen(
    guide: GuideUiState,
    me: MeResponseDto,
    strings: RiverStrings,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
    section: GearSection,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            when (section) {
                GearSection.RODS -> SectionCard(strings.guideRods) {
                    guide.guide?.rods?.forEachIndexed { index, rod ->
                        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        GuideRodRow(
                            strings = strings,
                            rod = rod,
                            ownedRod = me.rods.firstOrNull { it.code == rod.code },
                            isPlayFlavor = isPlayFlavor,
                            requestPlayPrice = requestPlayPrice,
                        )
                    } ?: EmptyStatePanel(strings.noData)
                }
                GearSection.LURES -> SectionCard(strings.guideLures) {
                    val lureGroups = buildGuideLureGroups(
                        strings = strings,
                        lures = guide.guide?.lures.orEmpty(),
                        guideLocations = guide.guide?.locations.orEmpty(),
                    )
                    if (lureGroups.isEmpty()) {
                        EmptyStatePanel(strings.noData)
                    } else {
                        lureGroups.forEachIndexed { index, lureGroup ->
                            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            GuideLureRow(
                                strings = strings,
                                title = lureGroup.title,
                                fishCount = lureGroup.fish.size,
                                locationDetails = lureGroup.locationDetails,
                            )
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
    onReloadClub: (Boolean) -> Unit,
    onLoadChat: () -> Unit,
    onLoadOlderChat: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSearchClubs: (String?) -> Unit,
    onCreateClub: (String) -> Unit,
    onJoinClub: (Long) -> Unit,
    onUpdateClubInfo: (String) -> Unit,
    onUpdateClubSettings: (Double, Boolean) -> Unit,
    onLeaveClub: () -> Unit,
    onMemberAction: (Long, String) -> Unit,
    onClaimPrize: (Long) -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    val me = state.me
    val questsSectionLabel = if (strings.login == "Логин") "Квесты" else "Quests"
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var createName by rememberSaveable { mutableStateOf("") }
    var infoDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(state.club.club?.info.orEmpty()) }
    var minWeightDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(((state.club.club?.minJoinWeightKg ?: 0.0).toInt()).toString()) }
    var recruitingDraft by rememberSaveable(state.club.club?.id) { mutableStateOf(state.club.club?.recruitingOpen ?: true) }
    var selectedSection by rememberSaveable(state.club.club?.id) { mutableStateOf("ratings") }
    var selectedWeek by rememberSaveable(state.club.club?.id) { mutableStateOf("current") }
    var selectedEventBoard by rememberSaveable(state.club.club?.id) { mutableStateOf(EVENT_BOARD_WEIGHT) }
    var selectedQuestCode by rememberSaveable(state.club.club?.id, selectedWeek) { mutableStateOf("") }
    val club = state.club.club
    val canManageClub = club?.role == "president" || club?.role == "heir"
    val weekData = if (selectedWeek == "previous") club?.previousWeek else club?.currentWeek
    val questWeekData = if (selectedWeek == "previous") club?.previousQuestWeek else club?.currentQuestWeek
    val clubEvent = if (selectedWeek == "previous") state.club.previousEvent else state.club.currentEvent
    val selectedQuest = questWeekData?.quests
        ?.firstOrNull { it.code == selectedQuestCode }
        ?: questWeekData?.quests?.firstOrNull()
    val clubPrizes = state.tournaments.prizes.filter(::isClubPrize)
    val shopPacks = state.shop.categories.flatMap { it.packs }
    val createCostCoins = 1_000L
    val minCreateWeightKg = 1_000.0
    val hasCreateWeight = (me?.totalWeight ?: 0.0) >= minCreateWeightKg
    val hasCreateCoins = (me?.coins ?: 0L) >= createCostCoins
    val canCreateClub = hasCreateWeight && hasCreateCoins
    var chatOpen by rememberSaveable(club?.id) { mutableStateOf(false) }
    var chatScrollAction by rememberSaveable(club?.id) { mutableStateOf("bottom") }
    var chatSizeBeforeOlder by rememberSaveable(club?.id) { mutableStateOf(0) }
    val chatListState = rememberLazyListState()

    LaunchedEffect(chatOpen, state.club.chat.size, state.club.chatLoading, state.club.chatOlderLoading) {
        if (!chatOpen || state.club.chatLoading || state.club.chat.isEmpty()) return@LaunchedEffect
        when (chatScrollAction) {
            "older" -> {
                if (!state.club.chatOlderLoading && state.club.chat.size > chatSizeBeforeOlder) {
                    val added = state.club.chat.size - chatSizeBeforeOlder
                    chatListState.scrollToItem(added.coerceAtMost(state.club.chat.lastIndex))
                    chatScrollAction = "idle"
                }
            }
            "bottom" -> {
                chatListState.scrollToItem(state.club.chat.lastIndex)
                chatScrollAction = "idle"
            }
        }
    }

    LaunchedEffect(
        chatOpen,
        chatListState.firstVisibleItemIndex,
        chatListState.firstVisibleItemScrollOffset,
        state.club.chatHasMore,
        state.club.chatOlderLoading,
        state.club.chatLoading,
    ) {
        if (
            chatOpen &&
            state.club.chat.isNotEmpty() &&
            state.club.chatHasMore &&
            !state.club.chatOlderLoading &&
            !state.club.chatLoading &&
            chatScrollAction != "bottom" &&
            chatListState.firstVisibleItemIndex == 0 &&
            chatListState.firstVisibleItemScrollOffset < 24
        ) {
            chatSizeBeforeOlder = state.club.chat.size
            chatScrollAction = "older"
            onLoadOlderChat()
        }
    }

    LaunchedEffect(club?.id, selectedWeek, questWeekData?.weekStart) {
        val quests = questWeekData?.quests.orEmpty()
        selectedQuestCode = when {
            quests.isEmpty() -> ""
            quests.any { it.code == selectedQuestCode } -> selectedQuestCode
            else -> quests.first().code
        }
    }

    LaunchedEffect(selectedSection, selectedWeek, clubEvent?.event?.id) {
        selectedEventBoard = EVENT_BOARD_WEIGHT
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (club != null) 112.dp else 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (club != null) {
                item {
                    SectionCard(strings.club) {
                        ClubOverview(strings, club)
                    }
                }
                if (clubPrizes.isNotEmpty()) {
                    item {
                        PendingPrizeCard(
                            strings = strings,
                            prizes = clubPrizes,
                            onClaimPrize = onClaimPrize,
                        )
                    }
                }
                if (canManageClub) {
                    item {
                        SectionCard(if (strings.login == "Логин") "Настройки клуба" else "Club settings") {
                            OutlinedTextField(
                                value = infoDraft,
                                onValueChange = { infoDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && infoDraft != club.info) {
                                            onUpdateClubInfo(infoDraft)
                                        }
                                    },
                                label = { Text(strings.club) },
                            )
                            OutlinedTextField(
                                value = minWeightDraft,
                                onValueChange = { minWeightDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            val currentWeight = club.minJoinWeightKg
                                            val nextWeight = minWeightDraft.toDoubleOrNull()
                                            if (nextWeight != null && kotlin.math.round(currentWeight) != kotlin.math.round(nextWeight)) {
                                                onUpdateClubSettings(nextWeight, recruitingDraft)
                                            }
                                        }
                                    },
                                label = { Text(if (strings.login == "Логин") "Мин. вес для входа" else "Min join weight") },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(if (strings.login == "Логин") "Открыт для набора" else "Recruiting open")
                                Switch(
                                    checked = recruitingDraft,
                                    onCheckedChange = {
                                        recruitingDraft = it
                                        onUpdateClubSettings(minWeightDraft.toDoubleOrNull() ?: club.minJoinWeightKg, it)
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    SectionCard(
                        when (selectedSection) {
                            "ratings" -> strings.ratings
                            "tournament" -> clubTournamentLabel(strings)
                            else -> questsSectionLabel
                        }
                    ) {
                        SegmentedSelectionBar(
                            items = listOf("ratings", "tournament", "quests"),
                            selected = selectedSection,
                            onSelect = {
                                selectedSection = it
                                onReloadClub(true)
                            },
                            accentFor = { RiverMoss },
                            labelFor = {
                                when (it) {
                                    "ratings" -> strings.ratings
                                    "tournament" -> clubTournamentLabel(strings)
                                    else -> questsSectionLabel
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SegmentedSelectionBar(
                            items = listOf("current", "previous"),
                            selected = selectedWeek,
                            onSelect = {
                                selectedWeek = it
                                onReloadClub(true)
                            },
                            accentFor = { RiverMoss },
                            labelFor = {
                                when (it) {
                                    "current" -> if (selectedSection == "tournament") currentTournamentPeriodLabel(strings) else if (strings.login == "Логин") "Текущая неделя" else "This week"
                                    else -> if (selectedSection == "tournament") previousLabel(strings) else if (strings.login == "Логин") "Прошлая неделя" else "Last week"
                                }
                            },
                        )
                        Text(
                            if (selectedSection == "tournament") {
                                clubEvent?.event?.let { "${formatEpoch(it.startTime)} – ${formatEpoch(it.endTime)}" }.orEmpty()
                            } else {
                                (
                                    if (selectedSection == "quests") questWeekData?.weekStart else weekData?.weekStart
                                )?.let { formatWeekRange(it, strings) }.orEmpty()
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selectedSection == "ratings") {
                            if (weekData == null || weekData.members.isEmpty()) {
                                EmptyStatePanel(
                                    if (strings.login == "Логин") "Пока нет взносов." else "No contributions yet.",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                ClubWeekSection(
                                    strings = strings,
                                    week = weekData,
                                    role = club.role,
                                    allowActions = selectedWeek == "current",
                                    onMemberAction = onMemberAction,
                                )
                            }
                            Text(
                                if (strings.login == "Логин") {
                                    "Итого за неделю: ${weekData?.totalCoins ?: 0} монет"
                                } else {
                                    "Weekly total: ${weekData?.totalCoins ?: 0} coins"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (selectedSection == "tournament") {
                            if (clubEvent == null) {
                                EmptyStatePanel(eventsEmptyLabel(strings), modifier = Modifier.fillMaxWidth())
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                SelectionDropdown(
                                    title = eventLeaderboardLabel(strings),
                                    selectedLabel = specialEventBoardTitle(strings, selectedEventBoard),
                                    options = eventLeaderboardOptions(strings),
                                    selectedKey = selectedEventBoard,
                                    onSelect = { selectedEventBoard = it },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SpecialEventSelectedBoard(
                                    strings = strings,
                                    event = clubEvent,
                                    selectedBoard = selectedEventBoard,
                                    me = me,
                                    shopPacks = shopPacks,
                                    onOpenCatch = onOpenCatch,
                                )
                            }
                        } else {
                            if (questWeekData == null || questWeekData.quests.isEmpty()) {
                                EmptyStatePanel(strings.noData, modifier = Modifier.fillMaxWidth())
                            } else {
                                ClubQuestSelectorRow(
                                    quests = questWeekData.quests,
                                    selectedCode = selectedQuest?.code.orEmpty(),
                                    onSelect = { selectedQuestCode = it },
                                )
                                selectedQuest?.let { quest ->
                                    ClubQuestSummaryCard(strings = strings, quest = quest)
                                    if (quest.members.isEmpty()) {
                                        EmptyStatePanel(strings.noData, modifier = Modifier.fillMaxWidth())
                                    } else {
                                        ClubQuestSection(strings = strings, quest = quest)
                                    }
                                } ?: EmptyStatePanel(strings.noData, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = onLeaveClub,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RiverDanger.copy(alpha = 0.92f),
                            contentColor = RiverDeepNight,
                            disabledContainerColor = RiverDanger.copy(alpha = 0.44f),
                            disabledContentColor = RiverMist.copy(alpha = 0.7f),
                        ),
                    ) {
                        Text(strings.leaveClub)
                    }
                }
            } else {
                if (clubPrizes.isNotEmpty()) {
                    item {
                        PendingPrizeCard(
                            strings = strings,
                            prizes = clubPrizes,
                            onClaimPrize = onClaimPrize,
                        )
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
                                            "${summary.memberCount}/${summary.capacity} • ${summary.minJoinWeightKg.asKgCompact(strings)}",
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
                        Text(
                            if (strings.login == "Логин") "До 20 символов, без ругательств." else "Up to 20 characters, no profanity.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        InfoCard {
                            Text(
                                if (strings.login == "Логин") "Требования для создания" else "Creation requirements",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (strings.login == "Логин") {
                                    "${if (hasCreateWeight) "✓" else "•"} Минимум ${minCreateWeightKg.toInt()} кг общего улова. Сейчас: ${String.format(Locale.US, "%.0f кг", me?.totalWeight ?: 0.0)}"
                                } else {
                                    "${if (hasCreateWeight) "✓" else "•"} At least ${minCreateWeightKg.toInt()} kg total catch. Now: ${String.format(Locale.US, "%.0f kg", me?.totalWeight ?: 0.0)}"
                                },
                                color = if (hasCreateWeight) RiverMoss else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (strings.login == "Логин") {
                                    "${if (hasCreateCoins) "✓" else "•"} Стоимость создания: $createCostCoins монет. Сейчас: ${me?.coins ?: 0}"
                                } else {
                                    "${if (hasCreateCoins) "✓" else "•"} Creation cost: $createCostCoins coins. Now: ${me?.coins ?: 0}"
                                },
                                color = if (hasCreateCoins) RiverMoss else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!hasCreateWeight) {
                            Text(
                                if (strings.login == "Логин") "Нужно наловить минимум 1000 кг рыбы." else "Catch at least 1000 kg of fish.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            if (!hasCreateCoins) {
                                Text(
                                    if (strings.login == "Логин") "Недостаточно монет для создания клуба." else "Not enough coins to create a club.",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            OutlinedTextField(
                                value = createName,
                                onValueChange = { createName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(strings.createClub) },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onCreateClub(createName) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canCreateClub,
                            ) {
                                Text(
                                    if (strings.login == "Логин") "Создать за $createCostCoins монет" else "Create for $createCostCoins coins"
                                )
                            }
                        }
                    }
                }
            }
        }

        if (club != null) {
            Button(
                onClick = {
                    chatOpen = true
                    chatScrollAction = "bottom"
                    onLoadChat()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RiverFoam.copy(alpha = 0.96f),
                    contentColor = RiverDeepNight,
                ),
            ) {
                Text(strings.chat, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (club != null && chatOpen) {
        ClubChatWindow(
            strings = strings,
            messages = state.club.chat,
            loading = state.club.chatLoading,
            olderLoading = state.club.chatOlderLoading,
            sending = state.club.chatSending,
            listState = chatListState,
            onRefresh = {
                chatScrollAction = "bottom"
                onLoadChat()
            },
            onSendMessage = { text ->
                chatScrollAction = "bottom"
                onSendChatMessage(text)
            },
            onDismiss = { chatOpen = false },
        )
    }
}

@Composable
private fun ShopScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onBuyShopWithCoins: (String) -> Unit,
    onPlayPurchase: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.shop.categories, key = { it.id }) { category ->
            SectionCard(category.name) {
                category.packs.forEachIndexed { index, pack ->
                    if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                    ShopPackageRow(
                        strings = strings,
                        pack = pack,
                        isPlayFlavor = isPlayFlavor,
                        requestPlayPrice = requestPlayPrice,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelegramAccountSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    pending: Boolean,
    onStartTelegramLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.telegramAccount,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            TelegramAccountCard(
                strings = strings,
                me = me,
                pending = pending,
                onStartTelegramLink = onStartTelegramLink,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FishingStageScene(
    state: RiverKingUiState,
    strings: RiverStrings,
    me: MeResponseDto,
    backgroundUrl: String?,
    locationName: String?,
    castZone: CastZoneDto? = null,
    hasCustomCastZone: Boolean = false,
    modifier: Modifier = Modifier,
    proMode: Boolean = true,
    setupEnabled: Boolean = false,
    autoCastEnabled: Boolean = false,
    proNotice: ProFishingNotice? = null,
    onToggleAutoCast: () -> Unit = {},
    onOpenLocations: () -> Unit = {},
    onOpenLures: () -> Unit = {},
    onOpenRods: () -> Unit = {},
    onOpenQuests: () -> Unit = {},
    onBeginCast: (FishingCastSpot?) -> Unit = {},
    onHookFish: () -> Unit = {},
    onTapChallenge: () -> Unit = {},
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
    val fightIntensity = state.fishing.struggleIntensity.toFloat().coerceIn(0f, 1f)
    val castSpot = state.fishing.castSpot
    val proSceneZone = remember(castZone) {
        normalizeCastZone(castZone) ?: fallbackCastZone()
    }
    val proSceneSpec = remember(proSceneZone) {
        proSceneZone.bounds()
    }
    var backgroundIntrinsicSize by remember(backgroundUrl) { mutableStateOf(IntSize.Zero) }
    val inWater = when (phase) {
        FishingPhase.WAITING_BITE,
        FishingPhase.BITING,
        FishingPhase.TAP_CHALLENGE,
        -> true
        FishingPhase.RESOLVING -> castSpot != null
        FishingPhase.READY,
        FishingPhase.COOLDOWN,
        -> false
    }
    val infinite = rememberInfiniteTransition(label = "fishing-scene")
    val rippleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "ripple-progress",
    )
    var bobberVisual by remember { mutableStateOf(BobberVisualState()) }
    val shoreSpot = remember(proMode) {
        if (proMode) Offset(PRO_CAST_SHORE_X, PRO_CAST_SHORE_Y) else Offset(0.09f, TG_CAST_WATER_TOP - 0.03f)
    }
    var bobberRel by remember { mutableStateOf(shoreSpot) }
    var castLanded by remember { mutableStateOf(false) }
    var lastLandedRel by remember { mutableStateOf(shoreSpot) }
    var playedCatchAnimationId by remember { mutableStateOf<Long?>(null) }
    var catchLiftAnimation by remember { mutableStateOf<CatchLiftAnimationState?>(null) }
    val hasSplashed = inWater && (castLanded || phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE)
    val showRipple = hasSplashed && (!proMode || phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE)
    val shouldAnimateFloat = if (proMode) {
        phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE
    } else {
        hasSplashed
    }
    val proNoticeAlpha by animateFloatAsState(
        targetValue = if (proNotice?.visible == true) 1f else 0f,
        animationSpec = tween(durationMillis = if (proNotice?.visible == true) 180 else 500),
        label = "pro-fishing-notice-alpha",
    )

    LaunchedEffect(shouldAnimateFloat, phase, proMode, fightIntensity) {
        if (!shouldAnimateFloat) {
            bobberVisual = BobberVisualState()
            return@LaunchedEffect
        }
        var startNanos = 0L
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            if (startNanos == 0L) {
                startNanos = frameNanos
            }
            val elapsedSeconds = (frameNanos - startNanos) / 1_000_000_000f
            val stateMode = when (phase) {
                FishingPhase.BITING -> "biting"
                FishingPhase.TAP_CHALLENGE -> "tapping"
                else -> "idle"
            }
            val basePeriod = when (stateMode) {
                "biting" -> 0.8f
                "tapping" -> 0.65f
                else -> 3f
            }
            val mainWave = sin((elapsedSeconds * (2f * PI.toFloat())) / basePeriod)
            val nextVisual = when (stateMode) {
                "biting" -> {
                    val extraWave = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 0.75f))
                    val sinkOffset = if (proMode) 10f else 0f
                    val offset = sinkOffset + mainWave * 6.5f + extraWave * 1.8f
                    BobberVisualState(
                        offset = offset,
                        tilt = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 0.9f)) * 6.5f,
                        submerge = if (offset > 0f) min(1f, offset / 11f) else 0f,
                    )
                }
                "tapping" -> {
                    val quickWave = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 0.85f))
                    val pullWave = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 1.45f))
                    val snapWave = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 0.42f))
                    val offset = 5f +
                        fightIntensity * 18f +
                        mainWave * (4.2f + fightIntensity * 9f) +
                        quickWave * (1.1f + fightIntensity * 5f)
                    BobberVisualState(
                        offset = offset,
                        xOffset = pullWave * (5f + fightIntensity * 22f) + snapWave * fightIntensity * 7f,
                        tilt = sin((elapsedSeconds * (2f * PI.toFloat())) / (basePeriod * 0.95f)) *
                            (6f + fightIntensity * 15f),
                        submerge = if (offset > 0f) min(1f, offset / (9f + fightIntensity * 9f)) else 0f,
                    )
                }
                else -> {
                    val offset = mainWave * 4f
                    BobberVisualState(
                        offset = offset,
                        tilt = mainWave * 2.5f,
                        submerge = if (offset > 0f) min(1f, offset / 6f) else 0f,
                    )
                }
            }
            bobberVisual = BobberVisualState(
                offset = bobberVisual.offset + (nextVisual.offset - bobberVisual.offset) * 0.18f,
                xOffset = bobberVisual.xOffset + (nextVisual.xOffset - bobberVisual.xOffset) * 0.18f,
                tilt = bobberVisual.tilt + (nextVisual.tilt - bobberVisual.tilt) * 0.18f,
                submerge = bobberVisual.submerge + (nextVisual.submerge - bobberVisual.submerge) * 0.18f,
            )
        }
    }

    Card(
        modifier = modifier,
        shape = if (proMode) RoundedCornerShape(0.dp) else RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (proMode) RiverDeepNight else RiverPanel.copy(alpha = 0.72f),
        ),
        border = if (proMode) null else BorderStroke(1.dp, RiverOutline.copy(alpha = 0.55f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val density = LocalDensity.current
            val sceneWidthPx = with(density) { maxWidth.toPx() }
            val sceneHeightPx = with(density) { maxHeight.toPx() }
            val minSwipePx = with(density) { PRO_CAST_MIN_SWIPE_DP.dp.toPx() }
            val tapMaxDistancePx = with(density) { PAN_EDGE_TAP_MAX_DISTANCE_DP.dp.toPx() }
            val backgroundAspect = remember(backgroundIntrinsicSize, locationName) {
                if (backgroundIntrinsicSize.width > 0 && backgroundIntrinsicSize.height > 0) {
                    backgroundIntrinsicSize.width.toFloat() / backgroundIntrinsicSize.height.toFloat()
                } else {
                    defaultBackgroundAspect(locationName)
                }
            }
            val panViewport = remember(sceneWidthPx, sceneHeightPx, backgroundAspect) {
                computePanViewport(sceneWidthPx, sceneHeightPx, backgroundAspect)
            }
            val panoramicEnabled = proMode && panViewport.canPan
            val backgroundRenderWidthPx = remember(sceneWidthPx, sceneHeightPx, backgroundAspect) {
                max(sceneWidthPx, sceneHeightPx * backgroundAspect)
            }
            val backgroundPanRangePx = remember(sceneWidthPx, backgroundRenderWidthPx) {
                max(0f, backgroundRenderWidthPx - sceneWidthPx)
            }
            val panoramicWorldZone = remember(proSceneZone, panViewport.visibleWidth, hasCustomCastZone) {
                if (hasCustomCastZone) {
                    proSceneZone
                } else {
                    centeredWorldCastZone(proSceneZone, panViewport.visibleWidth)
                }
            }
            val activeCastZone = if (panoramicEnabled) panoramicWorldZone else proSceneZone
            val activeCastArea = activeCastZone.bounds()
            val cameraBounds = remember(panoramicEnabled, activeCastArea, panViewport.visibleWidth) {
                if (panoramicEnabled) {
                    cameraBoundsForArea(activeCastArea, panViewport.visibleWidth)
                } else {
                    CameraPanBounds()
                }
            }
            val locationCameraKey = remember(backgroundUrl, me.locationId, panoramicEnabled) {
                "${me.locationId}:${backgroundUrl.orEmpty()}:${if (panoramicEnabled) "pan" else "static"}"
            }
            var cameraViewLeft by remember(locationCameraKey) {
                mutableStateOf(
                    if (panoramicEnabled) clampCameraViewLeft(panViewport.centeredLeft, cameraBounds) else 0f
                )
            }
            LaunchedEffect(cameraBounds, panoramicEnabled) {
                cameraViewLeft = if (panoramicEnabled) {
                    clampCameraViewLeft(cameraViewLeft, cameraBounds)
                } else {
                    0f
                }
            }
            val cameraViewLeftState = rememberUpdatedState(cameraViewLeft)
            val visibleCastZone = remember(panoramicEnabled, activeCastZone, cameraViewLeft, panViewport.visibleWidth) {
                if (panoramicEnabled) {
                    worldCastZoneToScreen(
                        visibleWorldCastZone(activeCastZone, cameraViewLeft, panViewport.visibleWidth),
                        cameraViewLeft,
                        panViewport.visibleWidth,
                    )
                } else {
                    proSceneZone
                }
            }
            val visibleCastArea = visibleCastZone.bounds()
            val panStep = max(0.06f, panViewport.visibleWidth * PAN_STEP_VIEWPORT_MULTIPLIER)
            val backgroundPanOffsetPx = if (panoramicEnabled && panViewport.maxPan > 0f && backgroundPanRangePx > 0f) {
                (clampCameraViewLeft(cameraViewLeft, cameraBounds) / panViewport.maxPan) * backgroundPanRangePx
            } else {
                0f
            }
            val cameraMinOffsetPx = if (panoramicEnabled && panViewport.maxPan > 0f && backgroundPanRangePx > 0f) {
                (cameraBounds.minLeft / panViewport.maxPan) * backgroundPanRangePx
            } else {
                0f
            }
            val cameraMaxOffsetPx = if (panoramicEnabled && panViewport.maxPan > 0f && backgroundPanRangePx > 0f) {
                (cameraBounds.maxLeft / panViewport.maxPan) * backgroundPanRangePx
            } else {
                0f
            }
            val backgroundPanOffsetState = rememberUpdatedState(backgroundPanOffsetPx)
            if (backgroundUrl != null) {
                val backgroundPainter = rememberAsyncImagePainter(
                    model = backgroundUrl,
                    onSuccess = { result ->
                        val drawable = result.result.drawable
                        backgroundIntrinsicSize = IntSize(
                            width = drawable.intrinsicWidth.coerceAtLeast(0),
                            height = drawable.intrinsicHeight.coerceAtLeast(0),
                        )
                    },
                )
                if (panoramicEnabled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        clipRect {
                            translate(left = -backgroundPanOffsetPx, top = 0f) {
                                with(backgroundPainter) {
                                    draw(
                                        size = Size(
                                            width = backgroundRenderWidthPx,
                                            height = size.height,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Image(
                        painter = backgroundPainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (proMode) 1.06f else 1.22f,
                                scaleY = if (proMode) 1.02f else 1.08f,
                            ),
                    )
                }
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
            val readyPanGestureState = remember(locationCameraKey, phase, panoramicEnabled) {
                ReadyPanGestureState()
            }

            val proGestureModifier = when {
                !proMode -> Modifier
                phase == FishingPhase.READY && panoramicEnabled -> Modifier.pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            readyPanGestureState.reset()
                            readyPanGestureState.primaryPointerId = event.getPointerId(0)
                            readyPanGestureState.startX = event.x
                            readyPanGestureState.startY = event.y
                            readyPanGestureState.lastX = event.x
                            readyPanGestureState.lastY = event.y
                            readyPanGestureState.startedAtMillis =
                                if (event.eventTime > 0L) event.eventTime else SystemClock.uptimeMillis()
                            true
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            if (event.pointerCount >= 2) {
                                readyPanGestureState.panActive = true
                                readyPanGestureState.hadPanGesture = true
                                readyPanGestureState.panStartOffsetPx = backgroundPanOffsetState.value
                                readyPanGestureState.panStartXByPointer.clear()
                                for (index in 0 until event.pointerCount) {
                                    readyPanGestureState.panStartXByPointer[event.getPointerId(index)] =
                                        event.getX(index)
                                }
                            }
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (readyPanGestureState.panActive && event.pointerCount >= 2) {
                                var deltaX = 0f
                                for (index in 0 until event.pointerCount) {
                                    val pointerId = event.getPointerId(index)
                                    val startX = readyPanGestureState.panStartXByPointer[pointerId] ?: continue
                                    val currentDelta = event.getX(index) - startX
                                    if (abs(currentDelta) > abs(deltaX)) {
                                        deltaX = currentDelta
                                    }
                                }
                                val nextOffsetPx = (readyPanGestureState.panStartOffsetPx - deltaX).coerceIn(
                                    cameraMinOffsetPx,
                                    cameraMaxOffsetPx,
                                )
                                cameraViewLeft = if (backgroundPanRangePx > 0f && panViewport.maxPan > 0f) {
                                    clampCameraViewLeft(
                                        (nextOffsetPx / backgroundPanRangePx) * panViewport.maxPan,
                                        cameraBounds,
                                    )
                                } else {
                                    0f
                                }
                            } else {
                                val pointerIndex = event.findPointerIndex(readyPanGestureState.primaryPointerId)
                                if (pointerIndex >= 0) {
                                    readyPanGestureState.lastX = event.getX(pointerIndex)
                                    readyPanGestureState.lastY = event.getY(pointerIndex)
                                }
                            }
                            true
                        }

                        MotionEvent.ACTION_POINTER_UP -> {
                            if (readyPanGestureState.panActive) {
                                val remainingPointers = event.pointerCount - 1
                                if (remainingPointers >= 2) {
                                    readyPanGestureState.panStartOffsetPx = backgroundPanOffsetState.value
                                    readyPanGestureState.panStartXByPointer.clear()
                                    for (index in 0 until event.pointerCount) {
                                        if (index == event.actionIndex) continue
                                        readyPanGestureState.panStartXByPointer[event.getPointerId(index)] =
                                            event.getX(index)
                                    }
                                } else {
                                    readyPanGestureState.panActive = false
                                    readyPanGestureState.panStartXByPointer.clear()
                                }
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!readyPanGestureState.panActive && !readyPanGestureState.hadPanGesture) {
                                val deltaX = event.x - readyPanGestureState.startX
                                val deltaY = event.y - readyPanGestureState.startY
                                val distance = hypot(deltaX, deltaY)
                                val elapsedMillis =
                                    (event.eventTime - readyPanGestureState.startedAtMillis).coerceAtLeast(16L)
                                if (distance >= minSwipePx) {
                                    onBeginCast(
                                        proFishingCastSpotFromSwipe(
                                            swipe = Offset(deltaX, deltaY),
                                            widthPx = sceneWidthPx,
                                            heightPx = sceneHeightPx,
                                            elapsedMillis = elapsedMillis,
                                            sceneZone = visibleCastZone,
                                            worldZone = activeCastZone,
                                            viewLeft = cameraViewLeftState.value,
                                            viewportWidth = panViewport.visibleWidth,
                                        )
                                    )
                                } else if (
                                    distance <= tapMaxDistancePx &&
                                    elapsedMillis <= PAN_EDGE_TAP_MAX_DURATION_MILLIS
                                ) {
                                    val nextViewLeft = when {
                                        readyPanGestureState.lastX <= sceneWidthPx * PAN_EDGE_TAP_FRACTION ->
                                            cameraViewLeftState.value - panStep
                                        readyPanGestureState.lastX >= sceneWidthPx * (1f - PAN_EDGE_TAP_FRACTION) ->
                                            cameraViewLeftState.value + panStep
                                        else -> null
                                    }
                                    if (nextViewLeft != null) {
                                        cameraViewLeft = clampCameraViewLeft(nextViewLeft, cameraBounds)
                                    }
                                }
                            }
                            readyPanGestureState.reset()
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            readyPanGestureState.reset()
                            true
                        }

                        else -> false
                    }
                }
                phase == FishingPhase.READY -> Modifier.pointerInput(
                    phase,
                    sceneWidthPx,
                    sceneHeightPx,
                    visibleCastArea,
                    activeCastArea,
                ) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val pointerPositions = linkedMapOf(firstDown.id to firstDown.position)
                        val startPosition = firstDown.position
                        var lastPosition = firstDown.position
                        var dragTotal = Offset.Zero
                        val startedAtMillis = SystemClock.uptimeMillis()
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (change.id == firstDown.id) {
                                    lastPosition = change.position
                                    dragTotal = change.position - startPosition
                                }
                                if (change.pressed) {
                                    pointerPositions[change.id] = change.position
                                } else {
                                    pointerPositions.remove(change.id)
                                }
                            }
                            if (pointerPositions.isEmpty()) {
                                val distance = hypot(dragTotal.x, dragTotal.y)
                                val elapsedMillis = (SystemClock.uptimeMillis() - startedAtMillis).coerceAtLeast(16L)
                                if (distance >= minSwipePx) {
                                    onBeginCast(
                                        proFishingCastSpotFromSwipe(
                                            swipe = dragTotal,
                                            widthPx = sceneWidthPx,
                                            heightPx = sceneHeightPx,
                                            elapsedMillis = elapsedMillis,
                                            sceneZone = visibleCastZone,
                                            worldZone = activeCastZone,
                                            viewLeft = 0f,
                                            viewportWidth = 1f,
                                        )
                                    )
                                } else if (
                                    distance <= tapMaxDistancePx &&
                                    elapsedMillis <= PAN_EDGE_TAP_MAX_DURATION_MILLIS
                                ) {
                                    val nextViewLeft = when {
                                        lastPosition.x <= sceneWidthPx * PAN_EDGE_TAP_FRACTION ->
                                            cameraViewLeftState.value - panStep
                                        lastPosition.x >= sceneWidthPx * (1f - PAN_EDGE_TAP_FRACTION) ->
                                            cameraViewLeftState.value + panStep
                                        else -> null
                                    }
                                    if (nextViewLeft != null) {
                                        cameraViewLeft = clampCameraViewLeft(nextViewLeft, cameraBounds)
                                    }
                                }
                                break
                            }
                        }
                    }
                }
                phase == FishingPhase.BITING -> Modifier.pointerInput(phase) {
                    detectTapGestures(onTap = { onHookFish() })
                }
                phase == FishingPhase.TAP_CHALLENGE -> Modifier.pointerInput(phase) {
                    detectTapGestures(onTap = { onTapChallenge() })
                }
                else -> Modifier
            }
            Box(modifier = Modifier.matchParentSize().then(proGestureModifier))

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
            val rodBottomOvershoot = if (proMode) (maxHeight.value * -0.28f).dp else 50.dp
            val rodTopDp = maxHeight - rodHeightDp + rodBottomOvershoot
            val rodLeftPx = with(density) { rodLeftDp.toPx() }
            val rodTopPx = with(density) { rodTopDp.toPx() }
            val rodWidthPx = with(density) { rodWidthDp.toPx() }
            val rodHeightPx = with(density) { rodHeightDp.toPx() }
            val isSmallScene = maxWidth < 420.dp
            val catchMaxX = max(32f, sceneWidthPx - 32f)
            val catchMaxY = max(32f, sceneHeightPx - 32f)
            val catchTargetPx = Offset(
                x = (rodLeftPx + rodWidthPx * rodBaseAnchorX - rodWidthPx * if (isSmallScene) 0.22f else 0.16f)
                    .coerceIn(32f, catchMaxX),
                y = (rodTopPx + rodHeightPx * 0.998f - rodHeightPx * if (isSmallScene) 0.26f else 0.20f)
                    .coerceIn(32f, catchMaxY),
            )
            val rodTipRelX = if (maxWidth.value > 0f) {
                (rodLeftDp.value + rodWidthDp.value * rodTipAnchor.x) / maxWidth.value
            } else {
                0.25f
            }
            val activeCastSpot = castSpot
            val activeCastTarget = activeCastSpot?.let {
                if (proMode || it.proMode) {
                    if (panoramicEnabled && it.panoramicAware) {
                        val worldTarget = proStyleCastTarget(it, activeCastZone)
                        worldToScreenRel(worldTarget, cameraViewLeft, panViewport.visibleWidth)
                    } else {
                        proStyleCastTarget(it, if (panoramicEnabled) visibleCastZone else activeCastZone)
                    }
                } else {
                    tgStyleCastTarget(it, rodTipRelX)
                }
            }
            val currentLure = me.lures.firstOrNull { it.id == me.currentLureId }
            val currentLureAsset = lureAsset(currentLure?.name, currentLure?.displayName)
            val bobberRectSizePx = sceneWidthPx * if (proMode) 0.105f else 0.08f
            val bobberRadiusPx = bobberRectSizePx / 2f
            val bobberPx = Offset(
                x = sceneWidthPx * bobberRel.x + bobberVisual.xOffset,
                y = sceneHeightPx * bobberRel.y + bobberVisual.offset,
            )
            val rigLineHeightPx = with(density) { (if (proMode) 36.dp else 27.dp).toPx() }
            val rigHookSizeDp = if (proMode) 18.dp else 14.dp
            val rigBaitSizeDp = if (proMode) 30.dp else 25.dp
            val rigHookSizePx = with(density) { rigHookSizeDp.toPx() }
            val rigBaitSizePx = with(density) { rigBaitSizeDp.toPx() }
            val showRig = !hasSplashed && sceneWidthPx > 0f && sceneHeightPx > 0f

            LaunchedEffect(activeCastTarget, activeCastSpot?.castDurationMillis, shoreSpot) {
                if (activeCastTarget == null) {
                    castLanded = false
                    bobberRel = shoreSpot
                    return@LaunchedEffect
                }

                castLanded = false
                val startRel = bobberRel.takeIf { it.isFinite() } ?: shoreSpot
                val relDistanceY = abs(activeCastTarget.y - startRel.y)
                val arcHeight = max(0.015f, min(0.08f, relDistanceY * 0.75f))
                val durationNanos = (activeCastSpot?.castDurationMillis ?: CAST_ANIMATION_DEFAULT_MILLIS)
                    .coerceIn(CAST_ANIMATION_MIN_MILLIS, CAST_ANIMATION_MAX_MILLIS)
                    .toLong() * 1_000_000L
                var startNanos = 0L

                while (isActive) {
                    val frameNanos = withFrameNanos { it }
                    if (startNanos == 0L) {
                        startNanos = frameNanos
                    }
                    val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                    val eased = easeInOutCubic(progress)
                    val arc = sin(progress * PI.toFloat()) * arcHeight
                    bobberRel = Offset(
                        x = startRel.x + (activeCastTarget.x - startRel.x) * eased,
                        y = startRel.y + (activeCastTarget.y - startRel.y) * eased - arc,
                    )
                    if (progress >= 1f) break
                }

                bobberRel = activeCastTarget
                lastLandedRel = activeCastTarget
                castLanded = true
            }

            val animatedCatch = state.fishing.lastCast?.takeIf { it.caught }?.catch
            LaunchedEffect(animatedCatch?.id) {
                val caught = animatedCatch
                if (caught == null) {
                    catchLiftAnimation = null
                    return@LaunchedEffect
                }
                if (playedCatchAnimationId == caught.id || sceneWidthPx <= 0f || sceneHeightPx <= 0f) {
                    return@LaunchedEffect
                }
                playedCatchAnimationId = caught.id
                val start = Offset(
                    x = sceneWidthPx * lastLandedRel.x,
                    y = sceneHeightPx * lastLandedRel.y,
                )
                catchLiftAnimation = CatchLiftAnimationState(
                    catchId = caught.id,
                    fish = caught.fish,
                    rarity = caught.rarity,
                    start = start,
                    end = catchTargetPx,
                )
                val durationNanos = CATCH_LIFT_ANIMATION_MILLIS * 1_000_000L
                var startNanos = 0L
                while (isActive) {
                    val frameNanos = withFrameNanos { it }
                    if (startNanos == 0L) {
                        startNanos = frameNanos
                    }
                    val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                    val eased = easeOutCubic(progress)
                    catchLiftAnimation = catchLiftAnimation?.takeIf { it.catchId == caught.id }?.copy(progress = eased)
                    if (progress >= 1f) break
                }
                delay(250L)
                if (catchLiftAnimation?.catchId == caught.id) {
                    catchLiftAnimation = null
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val waterTop = size.height * TG_CAST_WATER_TOP
                val bobberBase = Offset(size.width * bobberRel.x + bobberVisual.xOffset, size.height * bobberRel.y)
                val bobberRectSize = size.width * if (proMode) 0.105f else 0.08f
                val bobberRadius = bobberRectSize / 2f
                val visibleAboveWater = bobberRadius * 0.75f
                val waterlineY = (bobberBase.y - bobberRadius + visibleAboveWater).coerceIn(0f, size.height)
                val bobber = bobberBase.copy(y = bobberBase.y + bobberVisual.offset)
                val bobberBottom = bobber.y + bobberRadius + bobberRadius * bobberVisual.submerge * 0.25f
                val shouldClipBobber = hasSplashed && bobberBottom > waterlineY
                val bobberClipBottom = if (shouldClipBobber) waterlineY else size.height

                // Rod tip in pixel coordinates
                val rodTip = Offset(
                    x = rodLeftDp.toPx() + rodWidthDp.toPx() * rodTipAnchor.x,
                    y = rodTopDp.toPx() + rodHeightDp.toPx() * rodTipAnchor.y,
                )

                // Rod line points
                val rodLinePoints = rodLinePointsPercentage(currentRodCode).map {
                    Offset(
                        x = rodLeftDp.toPx() + rodWidthDp.toPx() * it.x,
                        y = rodTopDp.toPx() + rodHeightDp.toPx() * it.y,
                    )
                }

                // Fishing line from last rod point to bobber with natural sag
                val lineOrigin = rodLinePoints.lastOrNull() ?: rodTip
                val lineAttach = bobber.copy(
                    y = if (hasSplashed) min(bobber.y, (waterlineY - 1f).coerceAtLeast(0f)) else bobber.y,
                )
                val dx = lineAttach.x - lineOrigin.x
                val dy = lineAttach.y - lineOrigin.y
                val dist = kotlin.math.hypot(dx, dy)
                val shouldShowSlack = phase == FishingPhase.READY || phase == FishingPhase.COOLDOWN
                val waterLinePath = Path().apply {
                    moveTo(lineOrigin.x, lineOrigin.y)
                    if (shouldShowSlack) {
                        val sag = if (proMode) {
                            min(size.height * 0.06f, max(8f, dist * 0.2f))
                        } else {
                            min(size.height * 0.22f, max(16f, dist * 0.55f))
                        }
                        val baseMidY = lineOrigin.y + dy * 0.5f
                        val control1 = Offset(
                            x = lineOrigin.x + dx * 0.35f,
                            y = baseMidY + sag * 0.45f,
                        )
                        val control2 = Offset(
                            x = lineOrigin.x + dx * 0.75f,
                            y = baseMidY + sag,
                        )
                        cubicTo(control1.x, control1.y, control2.x, control2.y, lineAttach.x, lineAttach.y)
                    } else {
                        val gentleSag = min(size.height * 0.08f, dist * 0.12f)
                        val fightPull = if (phase == FishingPhase.TAP_CHALLENGE) {
                            bobberVisual.xOffset * 0.5f + size.width * 0.035f * fightIntensity *
                                sin((rippleProgress + 0.2f) * 2f * PI.toFloat())
                        } else {
                            0f
                        }
                        val control = Offset(
                            x = lineOrigin.x + dx * 0.5f + fightPull,
                            y = lineOrigin.y + dy * 0.5f + gentleSag + fightIntensity * 18f,
                        )
                        quadraticTo(control.x, control.y, lineAttach.x, lineAttach.y)
                    }
                }
                if (hasSplashed && !proMode) {
                    clipRect(left = 0f, top = 0f, right = size.width, bottom = waterlineY) {
                        drawPath(
                            path = waterLinePath,
                            color = Color.White.copy(alpha = 0.35f),
                            style = Stroke(width = 2f, cap = StrokeCap.Round),
                        )
                    }
                } else {
                    drawPath(
                        path = waterLinePath,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round),
                    )
                }

                if (!hasSplashed) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(bobber.x, bobber.y + bobberRadius * 0.44f),
                        end = Offset(bobber.x, bobber.y + bobberRadius * 0.44f + rigLineHeightPx),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                    )
                }

                // Ripple circles around bobber
                if (showRipple) {
                    repeat(if (phase == FishingPhase.BITING || phase == FishingPhase.TAP_CHALLENGE) 2 else 1) { index ->
                        val progress = ((rippleProgress + index * 0.35f) % 1f)
                        val radius = size.width * (0.04f + progress * (0.08f + fightIntensity * 0.06f))
                        drawCircle(
                            color = Color.White.copy(alpha = (0.4f + fightIntensity * 0.18f - progress * 0.28f).coerceAtLeast(0f)),
                            radius = radius,
                            center = Offset(bobber.x, max(waterTop + 8f, bobber.y + size.height * 0.02f)),
                            style = Stroke(width = 3f),
                        )
                    }
                }

                // Bobber (float)
                if (bobberBitmap != null) {
                    val srcSize = IntSize(bobberBitmap.width, bobberBitmap.height)
                    val dstSize = IntSize(
                        bobberRectSize.toInt().coerceAtLeast(1),
                        (bobberRectSize * bobberBitmap.height / bobberBitmap.width).toInt().coerceAtLeast(1),
                    )
                    rotate(degrees = bobberVisual.tilt, pivot = bobber) {
                        clipRect(left = 0f, top = 0f, right = size.width, bottom = bobberClipBottom) {
                            drawImage(
                                image = bobberBitmap,
                                srcOffset = IntOffset.Zero,
                                srcSize = srcSize,
                                dstOffset = IntOffset(
                                    (bobber.x - dstSize.width / 2f).toInt(),
                                    (bobber.y - dstSize.height / 2f).toInt(),
                                ),
                                dstSize = dstSize,
                            )
                        }
                    }
                } else {
                    rotate(degrees = bobberVisual.tilt, pivot = bobber) {
                        clipRect(left = 0f, top = 0f, right = size.width, bottom = bobberClipBottom) {
                            val fallbackBobberRadius = size.width * 0.022f
                            drawCircle(color = Color(0xFFE8F0F4), radius = fallbackBobberRadius, center = bobber)
                            drawCircle(
                                color = Color(0xFFE55B5B),
                                radius = fallbackBobberRadius,
                                center = bobber.copy(y = bobber.y - fallbackBobberRadius * 0.42f),
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.32f),
                                radius = fallbackBobberRadius * 2.3f,
                                center = bobber,
                            )
                        }
                    }
                }
            }
            if (showRig) {
                val rigTopPx = bobberPx.y + bobberRadiusPx * 0.44f + rigLineHeightPx - rigHookSizePx * 0.18f
                Image(
                    painter = painterResource(R.drawable.fishing_hook),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFFF4EAD6)),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (bobberPx.x - rigHookSizePx * 0.42f).roundToInt(),
                                rigTopPx.roundToInt(),
                            )
                        }
                        .size(rigHookSizeDp),
                )
                if (phase != FishingPhase.COOLDOWN && currentLureAsset != null) {
                    AsyncImage(
                        model = currentLureAsset,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (bobberPx.x - rigBaitSizePx * 0.42f).roundToInt(),
                                    (rigTopPx + rigHookSizePx * 0.35f - rigBaitSizePx * 0.45f).roundToInt(),
                                )
                            }
                            .size(rigBaitSizeDp),
                    )
                }
            }
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
                val rodLinePoints = rodLinePointsPercentage(currentRodCode).map {
                    Offset(
                        x = rodLeftDp.toPx() + rodWidthDp.toPx() * it.x,
                        y = rodTopDp.toPx() + rodHeightDp.toPx() * it.y,
                    )
                }
                if (rodLinePoints.isNotEmpty()) {
                    val rodLinePath = Path().apply {
                        moveTo(rodLinePoints.first().x, rodLinePoints.first().y)
                        for (i in 1 until rodLinePoints.size) {
                            lineTo(rodLinePoints[i].x, rodLinePoints[i].y)
                        }
                    }
                    drawPath(
                        path = rodLinePath,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round),
                    )
                }
            }
            catchLiftAnimation?.let { animation ->
                val fishAsset = fishAssetModel(animation.fish)
                if (fishAsset != null) {
                    val progress = animation.progress
                    val lift = sin(progress * PI.toFloat()) * if (isSmallScene) 26f else 38f
                    val fishSizeDp = if (isSmallScene) 72.dp else 88.dp
                    val fishSizePx = with(density) { fishSizeDp.toPx() }
                    val x = animation.start.x + (animation.end.x - animation.start.x) * progress
                    val y = animation.start.y + (animation.end.y - animation.start.y) * progress - lift
                    val scale = 0.75f + progress * 0.3f
                    val rotation = sin(progress * PI.toFloat()) * if (isSmallScene) 9f else 13f
                    val fadeStart = 0.8f
                    val alpha = if (progress < fadeStart) {
                        1f
                    } else {
                        (1f - (progress - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (x - fishSizePx / 2f).roundToInt(),
                                    (y - fishSizePx / 2f).roundToInt(),
                                )
                            }
                            .size(fishSizeDp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                rotationZ = rotation,
                                alpha = alpha,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            rarityColor(animation.rarity).copy(alpha = 0.42f),
                                            Color.Transparent,
                                        )
                                    ),
                                    CircleShape,
                                )
                        )
                        AsyncImage(
                            model = fishAsset,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            if (proMode) {
                FishingSetupBar(
                    strings = strings,
                    me = me,
                    enabled = setupEnabled,
                    onOpenLocations = onOpenLocations,
                    onOpenLures = onOpenLures,
                    onOpenRods = onOpenRods,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    panelAlpha = 0.68f,
                )
                FishingOverlayAction(
                    label = strings.quests,
                    onClick = onOpenQuests,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 86.dp),
                )
                if (proNotice != null && proNoticeAlpha > 0.01f) {
                    Text(
                        text = proNotice.text,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 28.dp)
                            .padding(bottom = 74.dp)
                            .graphicsLayer(alpha = proNoticeAlpha),
                        color = if (proNotice.tone == ProFishingNoticeTone.ERROR) {
                            Color(0xFFFFD7D0)
                        } else {
                            Color(0xFFC9FBD8)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (me.autoFish) {
                FishingOverlayToggle(
                    label = strings.autoCast,
                    checked = autoCastEnabled,
                    onClick = onToggleAutoCast,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .then(if (proMode) Modifier.navigationBarsPadding() else Modifier)
                        .padding(end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

private fun tgStyleCastTarget(castSpot: FishingCastSpot, rodTipRelX: Float): Offset {
    val minX = max(TG_CAST_LEFT_MARGIN, rodTipRelX - TG_CAST_MAX_DISTANCE_FROM_TIP)
    val maxX = max(minX, rodTipRelX - TG_CAST_MIN_DISTANCE_FROM_TIP)
    return Offset(
        x = minX + castSpot.xRoll * (maxX - minX),
        y = TG_CAST_WATER_TOP + TG_CAST_MIN_WATER_DEPTH + castSpot.yRoll * TG_CAST_WATER_DEPTH_VARIANCE,
    )
}

private fun defaultBackgroundAspect(location: String?): Float = when (location) {
    "Игапо, затопленный лес", "Igapo Flooded Forest", "Flooded Forest",
    "Мангровые заросли", "Mangroves" -> 1f
    else -> 1.5f
}

private fun computePanViewport(widthPx: Float, heightPx: Float, imageAspect: Float): PanViewportSpec {
    if (widthPx <= 0f || heightPx <= 0f || imageAspect <= 0f) {
        return PanViewportSpec()
    }
    val stageAspect = widthPx / heightPx
    if (imageAspect <= stageAspect) {
        return PanViewportSpec()
    }
    val visibleWidth = (stageAspect / imageAspect).coerceIn(0f, 1f)
    val maxPan = max(0f, 1f - visibleWidth)
    return PanViewportSpec(
        visibleWidth = visibleWidth,
        maxPan = maxPan,
        centeredLeft = maxPan / 2f,
        canPan = maxPan > 0.001f,
    )
}

private fun normalizeCastZone(castZone: CastZoneDto?): ProFishingCastZone? {
    val points = castZone?.points.orEmpty().mapNotNull { point ->
        val x = point.x.toFloat()
        val y = point.y.toFloat()
        if (!x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()) {
            Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        } else {
            null
        }
    }
    if (points.size < 3) return null
    val zone = ProFishingCastZone(points)
    if (zone.area() < 0.0004f) return null
    return zone
}

private fun fallbackCastZone(): ProFishingCastZone =
    ProFishingCastZone(
        listOf(
            Offset(0.08f, 0.62f),
            Offset(0.48f, 0.62f),
            Offset(0.48f, 0.94f),
            Offset(0.08f, 0.94f),
        )
    )

private fun rectCastZone(sceneSpec: ProFishingSceneSpec): ProFishingCastZone =
    ProFishingCastZone(
        listOf(
            Offset(sceneSpec.minX, sceneSpec.farY),
            Offset(sceneSpec.maxX, sceneSpec.farY),
            Offset(sceneSpec.maxX, sceneSpec.nearY),
            Offset(sceneSpec.minX, sceneSpec.nearY),
        )
    )

private fun ProFishingCastZone.bounds(): ProFishingSceneSpec {
    if (points.isEmpty()) return ProFishingSceneSpec()
    return ProFishingSceneSpec(
        minX = points.minOf { it.x }.coerceIn(0f, 1f),
        maxX = points.maxOf { it.x }.coerceIn(0f, 1f),
        farY = points.minOf { it.y }.coerceIn(0f, 1f),
        nearY = points.maxOf { it.y }.coerceIn(0f, 1f),
    )
}

private fun ProFishingCastZone.area(): Float {
    if (points.size < 3) return 0f
    var sum = 0f
    points.forEachIndexed { index, point ->
        val next = points[(index + 1) % points.size]
        sum += point.x * next.y - next.x * point.y
    }
    return abs(sum) / 2f
}

private fun screenCastZoneToWorldZone(
    zone: ProFishingCastZone,
    viewportWidth: Float,
    expandX: Float = 0f,
    expandTop: Float = 0f,
    expandBottom: Float = 0f,
): ProFishingCastZone {
    val centeredLeft = max(0f, (1f - viewportWidth) / 2f)
    val points = zone.points.map { point ->
        Offset(
            x = (centeredLeft + point.x * viewportWidth).coerceIn(0f, 1f),
            y = point.y.coerceIn(0f, 1f),
        )
    }
    val expanded = ProFishingCastZone(points).bounds().let { bounds ->
        rectCastZone(
            ProFishingSceneSpec(
                minX = (bounds.minX - expandX).coerceIn(0f, 1f),
                maxX = (bounds.maxX + expandX).coerceIn(0f, 1f),
                farY = (bounds.farY - expandTop).coerceIn(0f, 1f),
                nearY = (bounds.nearY + expandBottom).coerceIn(0f, 1f),
            )
        )
    }
    return if (expandX > 0f || expandTop > 0f || expandBottom > 0f) expanded else ProFishingCastZone(points)
}

private fun centeredWorldCastZone(zone: ProFishingCastZone, viewportWidth: Float): ProFishingCastZone =
    screenCastZoneToWorldZone(
        zone = zone,
        viewportWidth = viewportWidth,
        expandX = max(CAST_AREA_EXPAND_MIN_X, viewportWidth * CAST_AREA_EXPAND_VIEWPORT_X),
        expandTop = CAST_AREA_EXPAND_TOP,
        expandBottom = CAST_AREA_EXPAND_BOTTOM,
    )

private fun visibleWorldCastZone(
    zone: ProFishingCastZone,
    viewLeft: Float,
    viewportWidth: Float,
): ProFishingCastZone {
    val clippedLeft = clipCastZoneVertical(zone.points, viewLeft, keepGreater = true)
    val clipped = clipCastZoneVertical(clippedLeft, viewLeft + viewportWidth, keepGreater = false)
    if (clipped.size >= 3) return ProFishingCastZone(clipped)
    return rectCastZone(visibleWorldCastArea(zone.bounds(), viewLeft, viewportWidth))
}

private fun clipCastZoneVertical(points: List<Offset>, boundaryX: Float, keepGreater: Boolean): List<Offset> {
    if (points.isEmpty()) return emptyList()
    val result = mutableListOf<Offset>()
    fun inside(point: Offset): Boolean = if (keepGreater) point.x >= boundaryX else point.x <= boundaryX
    fun intersection(a: Offset, b: Offset): Offset {
        val dx = b.x - a.x
        if (abs(dx) < 0.000001f) return Offset(boundaryX, a.y)
        val t = ((boundaryX - a.x) / dx).coerceIn(0f, 1f)
        return Offset(boundaryX, a.y + (b.y - a.y) * t)
    }
    points.forEachIndexed { index, current ->
        val previous = points[(index + points.size - 1) % points.size]
        val currentInside = inside(current)
        val previousInside = inside(previous)
        when {
            currentInside && !previousInside -> {
                result += intersection(previous, current)
                result += current
            }
            currentInside -> result += current
            !currentInside && previousInside -> result += intersection(previous, current)
        }
    }
    return result
}

private fun worldCastZoneToScreen(
    zone: ProFishingCastZone,
    viewLeft: Float,
    viewportWidth: Float,
): ProFishingCastZone {
    if (viewportWidth <= 0f) return zone
    return ProFishingCastZone(
        zone.points.map { point ->
            Offset(
                x = ((point.x - viewLeft) / viewportWidth).coerceIn(0f, 1f),
                y = point.y.coerceIn(0f, 1f),
            )
        }
    )
}

private fun legacyScreenAreaToWorldArea(
    sceneSpec: ProFishingSceneSpec,
    viewportWidth: Float,
    expandX: Float = 0f,
    expandTop: Float = 0f,
    expandBottom: Float = 0f,
): ProFishingSceneSpec {
    val centeredLeft = max(0f, (1f - viewportWidth) / 2f)
    val worldMinX = centeredLeft + sceneSpec.minX * viewportWidth
    val worldMaxX = centeredLeft + sceneSpec.maxX * viewportWidth
    return ProFishingSceneSpec(
        minX = (worldMinX - expandX).coerceIn(0f, 1f),
        maxX = (worldMaxX + expandX).coerceIn(0f, 1f),
        farY = (sceneSpec.farY - expandTop).coerceIn(0f, 1f),
        nearY = (sceneSpec.nearY + expandBottom).coerceIn(0f, 1f),
    )
}

private fun centeredWorldCastArea(sceneSpec: ProFishingSceneSpec, viewportWidth: Float): ProFishingSceneSpec =
    legacyScreenAreaToWorldArea(
        sceneSpec = sceneSpec,
        viewportWidth = viewportWidth,
        expandX = max(CAST_AREA_EXPAND_MIN_X, viewportWidth * CAST_AREA_EXPAND_VIEWPORT_X),
        expandTop = CAST_AREA_EXPAND_TOP,
        expandBottom = CAST_AREA_EXPAND_BOTTOM,
    )

private fun cameraBoundsForArea(sceneSpec: ProFishingSceneSpec, viewportWidth: Float): CameraPanBounds {
    val maxPan = max(0f, 1f - viewportWidth)
    if (maxPan <= 0f || viewportWidth <= 0f) {
        return CameraPanBounds()
    }
    val areaWidth = max(0f, sceneSpec.maxX - sceneSpec.minX)
    val minVisible = min(
        areaWidth,
        max(CAST_AREA_MIN_VISIBLE_WORLD, viewportWidth * CAST_AREA_MIN_VISIBLE_VIEWPORT),
    )
    var minLeft = (sceneSpec.minX + minVisible - viewportWidth).coerceIn(0f, maxPan)
    var maxLeft = (sceneSpec.maxX - minVisible).coerceIn(0f, maxPan)
    if (maxLeft < minLeft) {
        val centered = (((sceneSpec.minX + sceneSpec.maxX) / 2f) - viewportWidth / 2f).coerceIn(0f, maxPan)
        minLeft = centered
        maxLeft = centered
    }
    return CameraPanBounds(minLeft = minLeft, maxLeft = maxLeft)
}

private fun clampCameraViewLeft(viewLeft: Float, bounds: CameraPanBounds): Float =
    viewLeft.coerceIn(bounds.minLeft, bounds.maxLeft)

private fun visibleWorldCastArea(
    sceneSpec: ProFishingSceneSpec,
    viewLeft: Float,
    viewportWidth: Float,
): ProFishingSceneSpec {
    val visibleMinX = max(sceneSpec.minX, viewLeft)
    val visibleMaxX = min(sceneSpec.maxX, viewLeft + viewportWidth)
    if (visibleMaxX > visibleMinX) {
        return sceneSpec.copy(minX = visibleMinX, maxX = visibleMaxX)
    }
    val center = ((sceneSpec.minX + sceneSpec.maxX) / 2f).coerceIn(viewLeft, viewLeft + viewportWidth)
    val halfWidth = min(viewportWidth * 0.08f, 0.04f)
    return sceneSpec.copy(
        minX = (center - halfWidth).coerceIn(viewLeft, viewLeft + viewportWidth),
        maxX = (center + halfWidth).coerceIn(viewLeft, viewLeft + viewportWidth),
    )
}

private fun worldAreaToScreenArea(
    sceneSpec: ProFishingSceneSpec,
    viewLeft: Float,
    viewportWidth: Float,
): ProFishingSceneSpec {
    if (viewportWidth <= 0f) return sceneSpec
    return sceneSpec.copy(
        minX = ((sceneSpec.minX - viewLeft) / viewportWidth).coerceIn(0f, 1f),
        maxX = ((sceneSpec.maxX - viewLeft) / viewportWidth).coerceIn(0f, 1f),
    )
}

private fun worldToScreenRel(point: Offset, viewLeft: Float, viewportWidth: Float): Offset {
    if (viewportWidth <= 0f) return point
    return Offset(
        x = (point.x - viewLeft) / viewportWidth,
        y = point.y,
    )
}

private fun proStyleCastTarget(castSpot: FishingCastSpot, zone: ProFishingCastZone): Offset {
    val bounds = zone.bounds()
    val point = Offset(
        x = bounds.minX + castSpot.xRoll.coerceIn(0f, 1f) * (bounds.maxX - bounds.minX),
        y = bounds.farY + castSpot.yRoll.coerceIn(0f, 1f) * (bounds.nearY - bounds.farY),
    )
    return if (pointInCastZone(point, zone)) point else closestPointInCastZone(point, zone)
}

private fun proFishingCastSpotFromSwipe(
    swipe: Offset,
    widthPx: Float,
    heightPx: Float,
    elapsedMillis: Long,
    sceneZone: ProFishingCastZone,
    worldZone: ProFishingCastZone = sceneZone,
    viewLeft: Float = 0f,
    viewportWidth: Float = 1f,
): FishingCastSpot {
    val minSide = min(widthPx, heightPx).coerceAtLeast(1f)
    val distance = hypot(swipe.x, swipe.y)
    val strength = (distance / (minSide * 0.75f)).coerceIn(0f, 1f)
    val nx = if (distance > 0f) swipe.x / distance else 0f
    val ny = if (distance > 0f) swipe.y / distance else 0f
    val sceneSpec = sceneZone.bounds()
    val worldSpec = worldZone.bounds()
    val centerX = (sceneSpec.minX + sceneSpec.maxX) / 2f
    val centerY = (sceneSpec.farY + sceneSpec.nearY) / 2f
    val reachX = (sceneSpec.maxX - sceneSpec.minX) / 2f
    val reachY = (sceneSpec.nearY - sceneSpec.farY) / 2f
    val screenTarget = Offset(
        x = (centerX + nx * strength * reachX).coerceIn(sceneSpec.minX, sceneSpec.maxX),
        y = (centerY + ny * strength * reachY).coerceIn(sceneSpec.farY, sceneSpec.nearY),
    ).let { if (pointInCastZone(it, sceneZone)) it else closestPointInCastZone(it, sceneZone) }
    val targetWorldX = viewLeft + screenTarget.x * viewportWidth
    val worldTarget = Offset(targetWorldX, screenTarget.y)
        .let { if (pointInCastZone(it, worldZone)) it else closestPointInCastZone(it, worldZone) }
    val worldWidth = max(0.0001f, worldSpec.maxX - worldSpec.minX)
    val worldHeight = max(0.0001f, worldSpec.nearY - worldSpec.farY)
    return FishingCastSpot(
        xRoll = ((worldTarget.x - worldSpec.minX) / worldWidth).coerceIn(0f, 1f),
        yRoll = ((worldTarget.y - worldSpec.farY) / worldHeight).coerceIn(0f, 1f),
        proMode = true,
        panoramicAware = viewportWidth < 0.999f && worldZone != sceneZone,
        castDurationMillis = castDurationFromSwipe(distance, elapsedMillis),
    )
}

private fun pointInCastZone(point: Offset, zone: ProFishingCastZone): Boolean {
    val points = zone.points
    if (points.size < 3) return false
    var inside = false
    var j = points.lastIndex
    for (i in points.indices) {
        val pi = points[i]
        val pj = points[j]
        val crosses = (pi.y > point.y) != (pj.y > point.y)
        if (crosses) {
            val xAtY = (pj.x - pi.x) * (point.y - pi.y) / ((pj.y - pi.y).takeIf { abs(it) > 0.000001f } ?: 0.000001f) + pi.x
            if (point.x < xAtY) inside = !inside
        }
        j = i
    }
    return inside
}

private fun closestPointInCastZone(point: Offset, zone: ProFishingCastZone): Offset {
    val points = zone.points
    if (points.isEmpty()) return point
    var closest = points.first()
    var closestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, start ->
        val end = points[(index + 1) % points.size]
        val candidate = closestPointOnSegment(point, start, end)
        val distance = hypot(candidate.x - point.x, candidate.y - point.y)
        if (distance < closestDistance) {
            closestDistance = distance
            closest = candidate
        }
    }
    return Offset(closest.x.coerceIn(0f, 1f), closest.y.coerceIn(0f, 1f))
}

private fun closestPointOnSegment(point: Offset, start: Offset, end: Offset): Offset {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lenSq = dx * dx + dy * dy
    if (lenSq <= 0.000001f) return start
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lenSq).coerceIn(0f, 1f)
    return Offset(start.x + dx * t, start.y + dy * t)
}

private fun proFishingSceneSpec(location: String?): ProFishingSceneSpec = when (location) {
    "Пруд", "Pond" -> ProFishingSceneSpec(minX = 0.05f, maxX = 0.88f, farY = 0.46f, nearY = 0.90f)
    "Болото", "Swamp" -> ProFishingSceneSpec(minX = 0.04f, maxX = 0.86f, farY = 0.48f, nearY = 0.90f)
    "Река", "River" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.84f, farY = 0.46f, nearY = 0.88f)
    "Озеро", "Lake" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.84f, farY = 0.44f, nearY = 0.90f)
    "Водохранилище", "Reservoir" -> ProFishingSceneSpec(minX = 0.04f, maxX = 0.74f, farY = 0.44f, nearY = 0.88f)
    "Горная река", "Mountain River" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.79f, farY = 0.48f, nearY = 0.86f)
    "Дельта реки", "River Delta" -> ProFishingSceneSpec(minX = 0.04f, maxX = 0.82f, farY = 0.44f, nearY = 0.84f)
    "Прибрежье моря", "Sea Coast" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.82f, farY = 0.42f, nearY = 0.90f)
    "Русло Амазонки", "Amazon Riverbed" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.83f, farY = 0.51f, nearY = 0.90f)
    "Игапо, затопленный лес", "Igapo Flooded Forest", "Flooded Forest" -> ProFishingSceneSpec(minX = 0.02f, maxX = 0.98f, farY = 0.48f, nearY = 0.92f)
    "Мангровые заросли", "Mangroves" -> ProFishingSceneSpec(minX = 0.02f, maxX = 0.96f, farY = 0.46f, nearY = 0.92f)
    "Коралловые отмели", "Coral Flats" -> ProFishingSceneSpec(minX = 0.04f, maxX = 0.97f, farY = 0.42f, nearY = 0.90f)
    "Фьорд", "Fjord" -> ProFishingSceneSpec(minX = 0.03f, maxX = 0.97f, farY = 0.53f, nearY = 0.92f)
    "Открытый океан", "Open Ocean" -> ProFishingSceneSpec(minX = 0.02f, maxX = 0.83f, farY = 0.40f, nearY = 0.84f)
    else -> ProFishingSceneSpec(minX = 0.05f, maxX = 0.88f, farY = 0.46f, nearY = 0.88f)
}

private fun castDurationFromSwipe(distancePx: Float, elapsedMillis: Long): Int {
    val elapsed = elapsedMillis.coerceAtLeast(16L).toFloat()
    if (distancePx.isNaN() || distancePx.isInfinite() || distancePx <= 0f) {
        return CAST_ANIMATION_DEFAULT_MILLIS
    }
    val velocity = distancePx / elapsed
    val speed = ((velocity - 0.18f) / 1.55f).coerceIn(0f, 1f)
    return (CAST_ANIMATION_MAX_MILLIS - speed * (CAST_ANIMATION_MAX_MILLIS - CAST_ANIMATION_MIN_MILLIS))
        .roundToInt()
        .coerceIn(CAST_ANIMATION_MIN_MILLIS, CAST_ANIMATION_MAX_MILLIS)
}

private fun easeInOutCubic(progress: Float): Float =
    if (progress < 0.5f) {
        4f * progress * progress * progress
    } else {
        val shifted = -2f * progress + 2f
        1f - (shifted * shifted * shifted) / 2f
    }

private fun easeOutCubic(progress: Float): Float {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    return 1f - remaining * remaining * remaining
}

private fun Offset.isFinite(): Boolean =
    !x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()

@Composable
private fun FishingOverlayToggle(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = RiverPanelRaised.copy(alpha = 0.76f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.58f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (checked) RiverMoss else Color.Transparent)
                    .then(
                        Modifier.background(
                            if (checked) RiverMoss else RiverPanelMuted.copy(alpha = 0.72f),
                            RoundedCornerShape(4.dp),
                        )
                    ),
            )
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FishingOverlayAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = RiverPanelRaised.copy(alpha = 0.76f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.58f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
private fun FishingOutcomeCard(
    strings: RiverStrings,
    me: MeResponseDto,
    fishing: FishingUiState,
    achievements: List<AchievementDto>,
    onOpenCatch: (CatchDto) -> Unit,
) {
    if (fishing.lastEscape) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = RiverPanelRaised.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color(0xFFE1A06C).copy(alpha = 0.55f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFE1A06C), CircleShape)
                )
                Text(
                    text = strings.fishEscaped,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        return
    }

    val cast = fishing.lastCast ?: return
    val catch = cast.catch ?: return
    Surface(
        modifier = Modifier.clickable { onOpenCatch(catch) },
        shape = RoundedCornerShape(24.dp),
        color = RiverPanelRaised.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.catchResultTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (fishing.lastCatchWasNewFish) {
                    GuideBadge(strings.newFishLabel(), RiverAmber)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FishThumbnail(
                    fishName = catch.fish,
                    discovered = true,
                    accent = rarityColor(catch.rarity),
                    size = 64.dp,
                    cornerRadius = 18.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        catch.fish,
                        color = rarityColor(catch.rarity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        catch.weight.asKgCompact(strings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        catch.location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (cast.coins > 0) {
                    Text(
                        strings.coinsEarnedLine(cast.coins),
                        color = Color(0xFFFFD76A),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                cast.unlockedLocations.forEach { location ->
                    Text(
                        strings.locationUnlockedLine(location),
                        color = Color(0xFF7CE38B),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                cast.unlockedRods.forEach { rod ->
                    Text(
                        strings.rodUnlockedLine(rod),
                        color = Color(0xFF7CE38B),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                cast.achievements.forEach { unlock ->
                    Text(
                        strings.achievementUnlockedLine(
                            name = achievementNameForCatch(strings, achievements, unlock.code),
                            level = strings.achievementLevelLabel(unlock.newLevelIndex),
                        ),
                        color = Color(0xFFFFD76A),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                cast.questUpdates.forEach { quest ->
                    Text(
                        strings.questCompletedLine(
                            name = quest.name.ifBlank { quest.code },
                            coins = quest.rewardCoins,
                        ),
                        color = Color(0xFF8EF0BE),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun fishingOutcomeNoticeText(
    strings: RiverStrings,
    fishing: FishingUiState,
    achievements: List<AchievementDto>,
): String? {
    if (fishing.lastEscape) return strings.fishEscaped
    val cast = fishing.lastCast ?: return null
    val catch = cast.catch ?: return null
    val lines = mutableListOf(
        "${strings.catchResultTitle()}: ${catch.fish} • ${catch.weight.asKgCompact(strings)}"
    )
    if (fishing.lastCatchWasNewFish) {
        lines += strings.newFishLabel()
    }
    if (cast.coins > 0) {
        lines += strings.coinsEarnedLine(cast.coins)
    }
    cast.unlockedLocations.forEach { location ->
        lines += strings.locationUnlockedLine(location)
    }
    cast.unlockedRods.forEach { rod ->
        lines += strings.rodUnlockedLine(rod)
    }
    cast.achievements.forEach { unlock ->
        lines += strings.achievementUnlockedLine(
            name = achievementNameForCatch(strings, achievements, unlock.code),
            level = strings.achievementLevelLabel(unlock.newLevelIndex),
        )
    }
    cast.questUpdates.forEach { quest ->
        lines += strings.questCompletedLine(
            name = quest.name.ifBlank { quest.code },
            coins = quest.rewardCoins,
        )
    }
    return lines.take(7).joinToString("\n")
}

@Composable
private fun FishingSetupBar(
    strings: RiverStrings,
    me: MeResponseDto,
    enabled: Boolean,
    onOpenLocations: () -> Unit,
    onOpenLures: () -> Unit,
    onOpenRods: () -> Unit,
    modifier: Modifier = Modifier,
    panelAlpha: Float = 0.94f,
) {
    val currentLocation = me.locations.firstOrNull { it.id == me.locationId }?.name ?: "—"
    val currentLure = me.lures.firstOrNull { it.id == me.currentLureId }?.displayName ?: "—"
    val currentRod = me.rods.firstOrNull { it.id == me.currentRodId }?.name ?: "—"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = RiverPanelMuted.copy(alpha = panelAlpha),
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
                enabled = enabled,
                onClick = onOpenLocations,
            )
            FishingSetupDivider()
            FishingSetupCell(
                label = strings.bait,
                value = currentLure,
                accent = RiverMoss,
                enabled = enabled,
                onClick = onOpenLures,
            )
            FishingSetupDivider()
            FishingSetupCell(
                label = strings.rod,
                value = currentRod,
                accent = RiverAmber,
                enabled = enabled,
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
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer(alpha = if (enabled) 1f else 0.48f)
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
private fun <T> SegmentedSelectionBar(
    modifier: Modifier = Modifier,
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    accentFor: (T) -> Color,
    labelFor: (T) -> String,
    badgeFor: ((T) -> Boolean)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            items.forEachIndexed { index, item ->
                SegmentedSelectionCell(
                    label = labelFor(item),
                    accent = accentFor(item),
                    selected = selected == item,
                    showBadge = badgeFor?.invoke(item) == true,
                    onClick = { onSelect(item) },
                )
                if (index < items.lastIndex) {
                    FishingSetupDivider()
                }
            }
        }
    }
}

@Composable
private fun RowScope.SegmentedSelectionCell(
    label: String,
    accent: Color,
    selected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
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
            Text(
                text = label,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RarityDropdown(
    strings: RiverStrings,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = if (selected == "all") RiverTide else rarityColor(selected)
    val label = if (selected == "all") strings.allFish else strings.rarityLabel(selected)

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(18.dp),
            color = RiverPanelSoft.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accent, CircleShape)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(22.dp),
            containerColor = RiverPanelRaised.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
        ) {
            listOf("all", "common", "uncommon", "rare", "epic", "mythic", "legendary").forEach { rarity ->
                val itemAccent = if (rarity == "all") RiverTide else rarityColor(rarity)
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(itemAccent, CircleShape)
                            )
                            Text(
                                text = if (rarity == "all") strings.allFish else strings.rarityLabel(rarity),
                                color = if (selected == rarity) itemAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected == rarity) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    },
                    colors = riverMenuItemColors(),
                    onClick = {
                        expanded = false
                        onSelect(rarity)
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionDropdown(
    title: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = RiverTide,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(18.dp),
            color = RiverPanelSoft.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(22.dp),
            containerColor = RiverPanelRaised.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (selectedKey == key) accent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selectedKey == key) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = riverMenuItemColors(),
                        onClick = {
                            expanded = false
                            onSelect(key)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaughtOnlyToggleCard(
    strings: RiverStrings,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (checked) RiverMoss else RiverOutline
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(18.dp),
        color = RiverPanelSoft.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (strings.login == "Логин") "Пойманные" else "Caught",
                modifier = Modifier.weight(1f),
                color = if (checked) RiverMoss else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = RiverMoss,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = RiverDeepNight,
                ),
            )
        }
    }
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
    isClubMember: Boolean,
    clubMembershipKnown: Boolean,
    onOpenQuests: () -> Unit,
) {
    SectionCard(strings.quests) {
        val clubFallbackMessage = when {
            quests.club.message != null -> quests.club.message
            clubMembershipKnown && !isClubMember -> strings.clubQuestsLockedMessage
            else -> strings.noData
        }
        val previewItems = questPreviewItems(quests)
        previewItems.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            QuestSummaryRow(strings = strings, quest = item.quest)
        }
        if (!quests.club.available) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(strings.clubQuestsLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(
                clubFallbackMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (previewItems.isEmpty() && quests.club.available) {
            EmptyStatePanel(strings.noData)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOpenQuests, modifier = Modifier.fillMaxWidth()) {
            Text(strings.viewAll)
        }
    }
}

private data class QuestPreviewItem(
    val quest: QuestDto,
)

private fun questPreviewItems(quests: QuestListDto): List<QuestPreviewItem> {
    val sections = listOf(
        quests.daily,
        quests.weekly,
        quests.club.quests,
    )
    val selected = mutableListOf<QuestPreviewItem>()
    val selectedKeys = mutableSetOf<String>()
    fun addQuest(quest: QuestDto): Boolean {
        if (selected.size >= 3) return false
        val key = "${quest.period}:${quest.code}"
        if (!selectedKeys.add(key)) return false
        selected += QuestPreviewItem(quest)
        return true
    }

    sections.forEach { items ->
        items.firstOrNull { !it.completed }?.let(::addQuest)
    }
    sections.forEach { items ->
        items.filterNot { it.completed }.forEach { quest ->
            addQuest(quest)
        }
    }
    if (selected.size >= 3) return selected

    val fallbackSections = sections.takeLast(3 - selected.size)
    fallbackSections.forEach { items ->
        items.firstOrNull { it.completed }?.let(::addQuest)
    }
    return selected
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
                    "${quest.progress}/${quest.target} • ${strings.questRewardLabel(quest.rewardCoins, isClub = quest.period == "club")}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (showPeriod) {
                Text(
                    when (quest.period) {
                        "weekly" -> strings.weeklyQuestsLabel
                        "club" -> strings.clubQuestsLabel
                        else -> strings.dailyQuestsLabel
                    },
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
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
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
                Button(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverPrimaryButtonColors(),
                ) {
                    Text(strings.claimDaily)
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverOutlinedButtonColors(),
                    border = riverOutlineBorder(),
                ) {
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
    isClubMember: Boolean,
    clubMembershipKnown: Boolean,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(strings.quests, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (loading && quests == null) {
                LoadingStatePanel(strings.loading)
            } else if (quests == null) {
                EmptyStatePanel(strings.noData)
                OutlinedButton(
                    onClick = onReload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverOutlinedButtonColors(),
                    border = riverOutlineBorder(),
                ) {
                    Text(strings.refresh)
                }
            } else {
                val clubFallbackMessage = when {
                    quests.club.message != null -> quests.club.message
                    clubMembershipKnown && !isClubMember -> strings.clubQuestsLockedMessage
                    else -> strings.noData
                }
                QuestSection(strings.dailyQuestsLabel, quests.daily, strings)
                QuestSection(strings.weeklyQuestsLabel, quests.weekly, strings)
                if (quests.club.available) {
                    QuestSection(strings.clubQuestsLabel, quests.club.quests, strings)
                } else {
                    QuestInfoSection(strings.clubQuestsLabel, clubFallbackMessage)
                }
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
                    colors = CardDefaults.cardColors(containerColor = RiverPanelSoft.copy(alpha = 0.92f)),
                    border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.72f)),
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

@Composable
private fun QuestInfoSection(
    title: String,
    message: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        InfoCard {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                        ?: if (location.isEvent) specialTournamentsLabel(strings) else location.unlockKg.asKgCompact(strings)
                } else {
                    location.lockedReason ?: if (location.isEvent) eventsEmptyLabel(strings) else location.unlockKg.asKgCompact(strings)
                },
                enabled = location.unlocked,
                selected = me.locationId == location.id,
                special = location.isEvent,
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
                        if (rod.unlocked) rod.unlockKg.asKgCompact(strings)
                        else rod.unlockKg.asKgCompact(strings)
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
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
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
    special: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            special -> RiverAmber.copy(alpha = 0.16f)
            else -> RiverPanelSoft.copy(alpha = 0.88f)
        },
        border = BorderStroke(
            1.dp,
            when {
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                special -> RiverAmber.copy(alpha = 0.72f)
                else -> RiverOutline.copy(alpha = 0.72f)
            },
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
            val backgroundModel = location.imageUrl ?: locationBackgroundAsset(location.name)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                backgroundModel?.let { background ->
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
                                location.isEvent -> specialEventLocationLabel(strings)
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
            FishThumbnail(
                fishName = fish.name,
                discovered = discovered,
                accent = accent,
                modifier = Modifier.size(92.dp),
                size = 92.dp,
                cornerRadius = 24.dp,
            )
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

private data class GuideLureLocationDetails(
    val name: String,
    val fish: List<FishBriefDto>,
)

private data class GuideLureGroup(
    val title: String,
    val fish: List<FishBriefDto>,
    val locationDetails: List<GuideLureLocationDetails>,
)

private fun buildGuideLureGroups(
    strings: RiverStrings,
    lures: List<GuideLureDto>,
    guideLocations: List<GuideLocationDto>,
): List<GuideLureGroup> {
    val grouped = lures.groupBy { lureGuideGroupKey(it.name) }
    val desiredOrder = listOf("fresh_peace", "fresh_predator", "salt_peace", "salt_predator")
    return grouped.entries
        .sortedWith(
            compareBy<Map.Entry<String, List<GuideLureDto>>>(
                { desiredOrder.indexOf(it.key).let { index -> if (index >= 0) index else Int.MAX_VALUE } },
                { it.value.firstOrNull()?.name.orEmpty() },
            )
        )
        .map { (groupKey, members) ->
            val fish = members
                .flatMap { it.fish }
                .distinctBy { it.name }
                .sortedBy { guideRaritySortRank(it.rarity) }
            val memberNames = members.map { it.name }.toSet()
            GuideLureGroup(
                title = lureGuideGroupTitle(strings, groupKey, members.map { it.name }),
                fish = fish,
                locationDetails = buildGuideLureLocationDetails(
                    lureNames = memberNames,
                    fish = fish,
                    guideLocations = guideLocations,
                ),
            )
        }
}

private fun buildGuideLureLocationDetails(
    lureNames: Set<String>,
    fish: List<FishBriefDto>,
    guideLocations: List<GuideLocationDto>,
): List<GuideLureLocationDetails> {
    val fishByName = fish.associateBy { it.name }
    return guideLocations.mapNotNull { location ->
        if (location.lures.none(lureNames::contains)) return@mapNotNull null
        val filteredFish = location.fish
            .mapNotNull { fishByName[it.name] }
            .distinctBy { it.name }
            .sortedBy { guideRaritySortRank(it.rarity) }
        GuideLureLocationDetails(location.name, filteredFish)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideLureRow(
    strings: RiverStrings,
    title: String,
    fishCount: Int,
    locationDetails: List<GuideLureLocationDetails>,
) {
    val accent = RiverMoss
    val locationNames = locationDetails.map { it.name }
    var expandedLocation by rememberSaveable(title) {
        mutableStateOf(locationNames.firstOrNull())
    }
    LaunchedEffect(locationNames) {
        if (expandedLocation !in locationNames) {
            expandedLocation = locationNames.firstOrNull()
        }
    }
    val selectedLocation = locationDetails.firstOrNull { it.name == expandedLocation }
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
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        "${guideFishCountLabel(strings, fishCount)} • ${guideLocationCountLabel(strings, locationDetails.size)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                GuideBadge(label = strings.bait, accent = accent)
            }
            if (locationDetails.isEmpty()) {
                Text(
                    strings.noData,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(locationDetails) { location ->
                        val selected = location.name == expandedLocation
                        Surface(
                            modifier = Modifier
                                .width(184.dp)
                                .height(110.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { expandedLocation = location.name },
                            shape = RoundedCornerShape(22.dp),
                            color = if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.10f),
                            ),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = locationBackgroundAsset(location.name),
                                    contentDescription = location.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.10f),
                                                    Color.Black.copy(alpha = 0.22f),
                                                    Color.Black.copy(alpha = 0.74f),
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        location.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    GuideBadge(
                                        label = guideFishCountLabel(strings, location.fish.size),
                                        accent = if (selected) accent else RiverTide,
                                    )
                                }
                            }
                        }
                    }
                }
                selectedLocation?.let { location ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                location.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                location.fish.forEach { fish ->
                                    GuideBadge(label = fish.name, accent = rarityColor(fish.rarity))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRodRow(
    strings: RiverStrings,
    rod: GuideRodDto,
    ownedRod: RodDto?,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
) {
    var playPrice by remember(ownedRod?.packId, isPlayFlavor) { mutableStateOf<String?>(null) }
    LaunchedEffect(ownedRod?.packId, isPlayFlavor) {
        playPrice = null
        val packId = ownedRod?.packId ?: return@LaunchedEffect
        if (isPlayFlavor) {
            requestPlayPrice(packId) { resolved ->
                playPrice = resolved
            }
        }
    }
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
                        isPlayFlavor && !playPrice.isNullOrBlank() ->
                            if (strings.login == "Логин") {
                                "Откроется на ${rod.unlockKg.asGuideKg(strings)} • $playPrice"
                            } else {
                                "Unlocks at ${rod.unlockKg.asGuideKg(strings)} • $playPrice"
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferralSheet(
    strings: RiverStrings,
    me: MeResponseDto,
    state: ReferralUiState,
    sessionAuthProvider: String?,
    clipboard: ClipboardManager,
    onGenerateReferral: () -> Unit,
    onShareReferral: (String) -> Unit,
    onClaimReferralRewards: () -> Unit,
    onDismiss: () -> Unit,
) {
    val showTelegramVariant = canShowTelegramReferral(sessionAuthProvider, me)
    val showGoogleVariant = canShowStoreReferral(sessionAuthProvider, me)
    val referral = state.referrals

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RiverPanelRaised.copy(alpha = 0.98f),
        shape = RiverSheetShape,
        dragHandle = { RiverSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.inviteFriends,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            when {
                state.loading && referral == null -> LoadingStatePanel(strings.loading)
                !showTelegramVariant && !showGoogleVariant -> EmptyStatePanel(strings.noData)
                else -> {
                    if (referral == null) {
                        SectionCard(strings.inviteFriends) {
                            Text(
                                strings.inviteFriends,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onGenerateReferral,
                                enabled = !state.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(strings.generateLink)
                            }
                        }
                    } else {
                        if (showTelegramVariant) {
                            ReferralVariantCard(
                                title = "Telegram",
                                link = referral.telegramLink,
                                shareBody = referral.androidShareText.ifBlank { referral.telegramLink },
                                clipboard = clipboard,
                                strings = strings,
                                onShareReferral = onShareReferral,
                            )
                        }
                        if (showGoogleVariant) {
                            ReferralVariantCard(
                                title = if (BuildConfig.DISTRIBUTION_CHANNEL == "play") "Google Play" else "itch.io",
                                link = storeReferralLink(referral.token, referral.webFallbackLink),
                                shareBody = storeReferralShareText(strings, referral.token, referral.webFallbackLink),
                                clipboard = clipboard,
                                strings = strings,
                                onShareReferral = onShareReferral,
                            )
                        }
                        OutlinedButton(
                            onClick = onGenerateReferral,
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(strings.generateLink)
                        }
                    }

                    referral?.invited?.takeIf { it.isNotEmpty() }?.let { invited ->
                        SectionCard(strings.inviteFriends) {
                            Text(
                                invited.joinToString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.rewards.isNotEmpty()) {
                        SectionCard(strings.claimRewards) {
                            Text(
                                state.rewards.joinToString { "${it.name} ×${it.qty}" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onClaimReferralRewards,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(strings.claimRewards)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralVariantCard(
    title: String,
    link: String,
    shareBody: String,
    clipboard: ClipboardManager,
    strings: RiverStrings,
    onShareReferral: (String) -> Unit,
) {
    SectionCard(title) {
        Text(
            text = link,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onShareReferral(shareBody) },
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.shareLink)
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(link)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(strings.copyLink)
            }
        }
    }
}

private fun canShowTelegramReferral(
    sessionAuthProvider: String?,
    me: MeResponseDto,
): Boolean = when (sessionAuthProvider) {
    AuthRepository.AUTH_PROVIDER_TELEGRAM -> true
    AuthRepository.AUTH_PROVIDER_GOOGLE,
    AuthRepository.AUTH_PROVIDER_PASSWORD,
    -> false
    null -> me.telegramLinked
    else -> false
}

private fun canShowStoreReferral(
    sessionAuthProvider: String?,
    me: MeResponseDto,
): Boolean = when (sessionAuthProvider) {
    AuthRepository.AUTH_PROVIDER_TELEGRAM -> false
    AuthRepository.AUTH_PROVIDER_GOOGLE,
    AuthRepository.AUTH_PROVIDER_PASSWORD,
    -> true
    null -> me.authProviders.any {
        it == AuthRepository.AUTH_PROVIDER_GOOGLE || it == AuthRepository.AUTH_PROVIDER_PASSWORD
    }
    else -> false
}

private fun storeReferralLink(token: String, webFallbackLink: String): String {
    return if (BuildConfig.DISTRIBUTION_CHANNEL == "play") {
        val separator = if (BuildConfig.PLAY_STORE_URL.contains("?")) "&" else "?"
        "${BuildConfig.PLAY_STORE_URL}$separator" +
            "referrer=${Uri.encode("ref=$token")}"
    } else {
        val base = webFallbackLink.ifBlank { BuildConfig.SUPPORT_URL }
        if (base.contains("://")) {
            "${base.trimEnd('/')}/ref?token=$token"
        } else {
            BuildConfig.ITCH_PROJECT_URL.takeIf { it.isNotBlank() } ?: base
        }
    }
}

private fun storeReferralShareText(strings: RiverStrings, token: String, webFallbackLink: String): String {
    val storeLink = storeReferralLink(token, webFallbackLink)
    val deepLink = "riverking://referral?token=$token"
    return if (BuildConfig.DISTRIBUTION_CHANNEL == "play") {
        if (strings.login == "Логин") {
            "Играй в RiverKing на Android: $storeLink\nЕсли игра уже установлена, открой: $deepLink"
        } else {
            "Play RiverKing on Android: $storeLink\nIf the game is already installed, open: $deepLink"
        }
    } else {
        if (strings.login == "Логин") {
            "Скачай RiverKing для Android: $storeLink\nЕсли игра уже установлена, открой: $deepLink"
        } else {
            "Download RiverKing for Android: $storeLink\nIf the game is already installed, open: $deepLink"
        }
    }
}

@Composable
private fun ShopPackageRow(
    strings: RiverStrings,
    pack: ShopPackageDto,
    isPlayFlavor: Boolean,
    requestPlayPrice: (String, (String?) -> Unit) -> Unit,
    onBuyWithCoins: () -> Unit,
    onPlayPurchase: () -> Unit,
) {
    val canBuyCoins = pack.coinPrice != null
    val paidUnavailableInDirect = !isPlayFlavor && pack.coinPrice == null
    var playPrice by remember(pack.id, isPlayFlavor) { mutableStateOf<String?>(null) }

    LaunchedEffect(pack.id, isPlayFlavor, canBuyCoins) {
        playPrice = null
        if (!canBuyCoins && isPlayFlavor) {
            requestPlayPrice(pack.id) { resolved ->
                playPrice = resolved
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = pack.rodCode?.let(::rodImageAsset) ?: shopIconAsset(pack.id),
                contentDescription = pack.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = pack.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = pack.desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (pack.until != null) {
                    Text(
                        text = pack.until,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Column(
                        modifier = Modifier.widthIn(min = 96.dp, max = 104.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (pack.originalPrice != null) {
                            Text(
                                text = "${pack.originalPrice}★",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        when {
                            canBuyCoins -> {
                                Button(
                                    onClick = onBuyWithCoins,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = "${pack.coinPrice} 🪙",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            paidUnavailableInDirect -> {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = strings.unavailable,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = onPlayPurchase,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = playPrice ?: "Google Play",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubChatWindow(
    strings: RiverStrings,
    messages: List<ClubChatMessageDto>,
    loading: Boolean,
    olderLoading: Boolean,
    sending: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = draft.trim().isNotEmpty() && !sending
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(onClick = onDismiss),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RiverDialogShape,
                colors = CardDefaults.cardColors(containerColor = RiverPanelRaised.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            if (strings.login == "Логин") "Чат клуба" else "Club chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = onRefresh,
                                enabled = !loading,
                                modifier = Modifier.weight(1f),
                                colors = riverOutlinedButtonColors(),
                                border = riverOutlineBorder(),
                            ) {
                                Text(strings.refresh, maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = riverOutlinedButtonColors(),
                                border = riverOutlineBorder(),
                            ) {
                                Text(
                                    if (strings.login == "Логин") "Закрыть" else "Close",
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }

                    when {
                        loading && messages.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        messages.isEmpty() -> {
                            EmptyStatePanel(
                                strings.noData,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (olderLoading) {
                                    item(key = "older-loading") {
                                        Text(
                                            strings.loading,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                items(messages, key = { it.id }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = RiverPanelSoft.copy(alpha = 0.82f)),
                                        border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.6f)),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                formatTimestamp(item.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = buildClubChatMessage(item.message, strings),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(500) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (strings.login == "Логин") "Сообщение" else "Message") },
                            minLines = 1,
                            maxLines = 3,
                            enabled = !sending,
                        )
                        IconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    draft = ""
                                    onSendMessage(text)
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (canSend) RiverFoam else RiverPanelMuted)
                                .padding(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = if (strings.login == "Логин") "Отправить" else "Send",
                                tint = if (canSend) RiverDeepNight else RiverFog,
                            )
                        }
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
    allowActions: Boolean,
    onMemberAction: (Long, String) -> Unit,
) {
    week.members.forEachIndexed { index, member ->
        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
        ClubMemberRow(strings, member, role, allowActions, onMemberAction)
    }
}

@Composable
private fun ClubQuestSelectorRow(
    quests: List<ClubQuestDto>,
    selectedCode: String,
    onSelect: (String) -> Unit,
) {
    HorizontalChipRow {
        quests.forEach { quest ->
            FilterChip(
                selected = quest.code == selectedCode,
                onClick = { onSelect(quest.code) },
                label = {
                    Text(
                        quest.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun ClubQuestSummaryCard(
    strings: RiverStrings,
    quest: ClubQuestDto,
) {
    InfoCard {
        Text(quest.name, fontWeight = FontWeight.SemiBold)
        Text(quest.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatPill(
                title = if (strings.login == "Логин") "Прогресс" else "Progress",
                value = "${quest.progress}/${quest.target}",
                modifier = Modifier.weight(1f),
            )
            StatPill(
                title = if (strings.login == "Логин") "Награда" else "Reward",
                value = quest.rewardCoins.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        if (quest.completed) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (strings.login == "Логин") "Квест выполнен" else "Quest completed",
                color = RiverMoss,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ClubQuestSection(
    strings: RiverStrings,
    quest: ClubQuestDto,
) {
    quest.members.forEachIndexed { index, member ->
        if (index > 0) HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
        ClubQuestMemberRow(strings = strings, member = member)
    }
}

@Composable
private fun ClubQuestMemberRow(
    strings: RiverStrings,
    member: ClubQuestMemberDto,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                Text(strings.roleLabel(member.role), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                member.progress.toString(),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ClubMemberRow(
    strings: RiverStrings,
    member: ClubMemberDto,
    actorRole: String,
    allowActions: Boolean,
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
        if (allowActions && (actorRole == "president" || actorRole == "heir")) {
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
                        text = "${it.rank} • ${tournamentMetricValueLabel(strings, tournament.metric, it.value)}",
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
                    CatchDetailChip(strings.prizePlacesLabel(tournament.prizePlaces))
                }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentDialog(
    strings: RiverStrings,
    details: CurrentTournamentDto,
    shopPacks: List<ShopPackageDto>,
    caughtFishIds: List<Long>,
    fishGuide: List<GuideFishDto>?,
    currentUserId: Long,
    onDismiss: () -> Unit,
    onOpenCatch: (CatchDto) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RiverDialogShape,
            colors = CardDefaults.cardColors(containerColor = RiverPanelRaised.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
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
                    CatchDetailChip(strings.prizePlacesLabel(details.tournament.prizePlaces))
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
                            shopPacks = shopPacks,
                            fishDiscovered = isFishDiscovered(
                                fishId = mine.fishId,
                                fishName = mine.fish,
                                ownerUserId = mine.userId,
                                currentUserId = currentUserId,
                                caughtFishIds = caughtFishIds,
                                fishGuide = fishGuide,
                            ),
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
                            shopPacks = shopPacks,
                            fishDiscovered = isFishDiscovered(
                                fishId = entry.fishId,
                                fishName = entry.fish,
                                ownerUserId = entry.userId,
                                currentUserId = currentUserId,
                                caughtFishIds = caughtFishIds,
                                fishGuide = fishGuide,
                            ),
                            highlighted = details.mine?.rank == entry.rank,
                            onOpenCatch = onOpenCatch,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverOutlinedButtonColors(),
                    border = riverOutlineBorder(),
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
    fishDiscovered: Boolean,
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
                        "$rank",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            FishThumbnail(
                fishName = catch.fish,
                discovered = fishDiscovered,
                accent = rarityColor(catch.rarity),
                size = 48.dp,
                cornerRadius = 16.dp,
            )
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
                Text(catch.weight.asKgCompact(strings), fontWeight = FontWeight.Bold)
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
    shopPacks: List<ShopPackageDto>,
    fishDiscovered: Boolean,
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
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.06f),
                    ) {
                        Text(
                            "${entry.rank}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        entry.user ?: unknownUserLabel(strings),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        tournamentMetricValueLabel(strings, tournament.metric, entry.value),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                entry.fish?.let { fishName ->
                    FishThumbnail(
                        fishName = fishName,
                        discovered = fishDiscovered,
                        accent = rarityColor(tournament.fishRarity),
                        size = 46.dp,
                        cornerRadius = 14.dp,
                    )
                }
                Text(
                    tournamentEntrySubtitle(strings, tournament, entry),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            entry.prize?.let { prize ->
                PrizeChip(strings = strings, prize = prize, shopPacks = shopPacks)
            }
        }
    }
}

@Composable
private fun PrizeChip(
    strings: RiverStrings,
    prize: PrizeSpecDto,
    shopPacks: List<ShopPackageDto>,
) {
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
                tournamentPrizeLabel(strings, prize, shopPacks),
                color = Color(0xFFFFD76A),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatchDetailsDialog(
    strings: RiverStrings,
    catch: CatchDto,
    me: MeResponseDto,
    resolvedRarity: String?,
    fishDiscovered: Boolean,
    allowShare: Boolean,
    loading: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val rarityKey = resolvedRarity?.takeIf { it.isNotBlank() }
    val accent = rarityColor(rarityKey)
    val backgroundModel = remember(me.locations, catch.location) {
        val normalized = catch.location.trim().lowercase()
        me.locations.firstOrNull { location -> location.name.trim().lowercase() == normalized }
            ?.let { location -> location.imageUrl ?: locationBackgroundAsset(location.name) }
            ?: locationBackgroundAsset(catch.location)
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RiverDialogShape,
            colors = CardDefaults.cardColors(containerColor = RiverPanelRaised.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
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
                    backgroundModel?.let { background ->
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
                    val fishAsset = fishAssetModel(catch.fish)
                    if (fishAsset != null) {
                        AsyncImage(
                            model = fishAsset,
                            contentDescription = if (fishDiscovered) catch.fish else null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(bottom = 50.dp)
                                .size(160.dp)
                                .align(Alignment.Center),
                            alpha = if (fishDiscovered) 1f else 0.86f,
                            colorFilter = if (fishDiscovered) {
                                null
                            } else {
                                ColorFilter.tint(
                                    Color.White.copy(alpha = 0.82f),
                                    BlendMode.SrcIn,
                                )
                            },
                        )
                    } else if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    if (!fishDiscovered) {
                        Box(
                            modifier = Modifier.align(Alignment.Center),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = RiverDeepNight.copy(alpha = 0.82f),
                                border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "?",
                                        color = RiverMist,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                }
                            }
                        }
                    }
                    rarityKey?.let {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            color = accent.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                        ) {
                            Text(
                                strings.rarityLabel(it),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            if (fishDiscovered) catch.fish else "???",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            catch.weight.asKgCompact(strings),
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
                        catch.rank?.let { CatchDetailChip("$it") }
                        catch.prizeCoins?.let { CatchDetailChip("$it coins") }
                    }
                    Text(
                        listOfNotNull(
                            rarityKey?.let(strings::rarityLabel),
                            catch.weight.asKgCompact(strings),
                            catch.location,
                        ).joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = riverOutlinedButtonColors(),
                            border = riverOutlineBorder(),
                        ) {
                            Text(strings.continueLabel)
                        }
                        if (allowShare) {
                            Button(
                                onClick = onShare,
                                enabled = catch.id > 0L,
                                modifier = Modifier.weight(1f),
                                colors = riverPrimaryButtonColors(),
                            ) {
                                Text(strings.shareCatch)
                            }
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
    shopPacks: List<ShopPackageDto>,
    onDismiss: () -> Unit,
) {
    val packIndex = remember(shopPacks) { shopPacks.associateBy { it.id } }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RiverDialogShape,
            colors = CardDefaults.cardColors(containerColor = RiverPanelRaised.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = strings.achievements,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (reward.rewards.isEmpty()) {
                    EmptyStatePanel(strings.noData)
                } else {
                    reward.rewards.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        }
                        AchievementRewardCard(
                            strings = strings,
                            reward = item,
                            pack = packIndex[item.pack],
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverPrimaryButtonColors(),
                ) {
                    Text(strings.continueLabel)
                }
            }
        }
    }
}

@Composable
private fun AchievementRewardCard(
    strings: RiverStrings,
    reward: AchievementRewardDto,
    pack: ShopPackageDto?,
) {
    val isCoinsReward = reward.coins != null || reward.pack.equals("coins", ignoreCase = true)
    val accent = if (isCoinsReward) RiverAmber else RiverMoss
    val contentItems = pack?.items.orEmpty().map { it.copy(qty = it.qty * reward.qty.coerceAtLeast(1)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RewardIconCard(
                packId = reward.pack,
                isCoinsReward = isCoinsReward,
                accent = accent,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = achievementRewardTitle(strings, reward, pack),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!isCoinsReward) {
                    RewardMetaChip(
                        text = achievementRewardQuantityLabel(strings, reward.qty),
                        accent = accent,
                    )
                }
            }
        }

        if (isCoinsReward) {
            RewardMetaChip(
                text = pendingPrizeCoinsLabel(strings, reward.coins ?: reward.qty),
                accent = RiverAmber,
            )
        } else if (contentItems.isNotEmpty()) {
            Text(
                text = if (strings.login == "Логин") "Содержимое" else "Contents",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            contentItems.forEach { item ->
                RewardContentRow(name = item.name, qty = item.qty)
            }
        } else {
            pack?.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RewardIconCard(
    packId: String,
    isCoinsReward: Boolean,
    accent: Color,
) {
    Surface(
        modifier = Modifier.size(60.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isCoinsReward) {
                Text(
                    text = "🪙",
                    style = MaterialTheme.typography.headlineSmall,
                )
            } else if (packId.isBlank()) {
                Text(
                    text = "🎁",
                    style = MaterialTheme.typography.headlineSmall,
                )
            } else {
                AsyncImage(
                    model = shopIconAsset(packId),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
            }
        }
    }
}

@Composable
private fun RewardMetaChip(
    text: String,
    accent: Color,
) {
    Surface(
        color = accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RewardContentRow(
    name: String,
    qty: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(lureAccentColor(name), CircleShape)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = "×$qty",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
private fun LanguagePickerDialog(
    strings: RiverStrings,
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        "en" to "English",
        "ru" to "Русский",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.language) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (code, label) ->
                    val selected = currentLanguage == code
                    OutlinedButton(
                        onClick = { onSelect(code) },
                        enabled = !selected,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selected) "✓ $label" else label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

@Composable
private fun LanguageToggle(currentLanguage: String, onChangeLanguage: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(currentLanguage.uppercase(Locale.US)) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = RiverPanelRaised.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
        ) {
            DropdownMenuItem(
                text = { Text("EN") },
                colors = riverMenuItemColors(),
                onClick = {
                    expanded = false
                    onChangeLanguage("en")
                },
            )
            DropdownMenuItem(
                text = { Text("RU") },
                colors = riverMenuItemColors(),
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
    fishName: String? = null,
    fishDiscovered: Boolean = true,
    fishAccent: Color = MaterialTheme.colorScheme.primary,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fishName?.let { name ->
            FishThumbnail(
                fishName = name,
                discovered = fishDiscovered,
                accent = fishAccent,
                modifier = Modifier.size(46.dp),
                size = 46.dp,
                cornerRadius = 16.dp,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = accent)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FishThumbnail(
    fishName: String?,
    discovered: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 16.dp,
) {
    val fishAsset = fishAssetModel(fishName)
    val shape = RoundedCornerShape(cornerRadius)
    val imagePadding = if (size < 64.dp) 6.dp else 10.dp
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = RiverPanelSoft.copy(alpha = 0.94f),
        border = BorderStroke(
            1.dp,
            if (discovered) accent.copy(alpha = 0.34f) else RiverOutline.copy(alpha = 0.82f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (discovered) {
                            listOf(
                                accent.copy(alpha = 0.24f),
                                accent.copy(alpha = 0.08f),
                                RiverPanelSoft.copy(alpha = 0.96f),
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                RiverPanelSoft.copy(alpha = 0.98f),
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (fishAsset != null) {
                AsyncImage(
                    model = fishAsset,
                    contentDescription = if (discovered) fishName else null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(imagePadding),
                    alpha = if (discovered) 1f else 0.84f,
                    colorFilter = if (discovered) {
                        null
                    } else {
                        ColorFilter.tint(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                            BlendMode.SrcIn,
                        )
                    },
                )
            } else {
                Text(
                    text = "🐟",
                    style = if (size < 64.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                )
            }
            if (!discovered) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = RiverDeepNight.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f)),
                ) {
                    Box(
                        modifier = Modifier.size(if (size < 64.dp) 24.dp else 30.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "?",
                            color = RiverMist,
                            fontWeight = FontWeight.Bold,
                            style = if (size < 64.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
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
    "largest" -> if (strings.login == "Логин") "Самая большая" else "Largest"
    "smallest" -> if (strings.login == "Логин") "Самая маленькая" else "Smallest"
    "max_weight" -> if (strings.login == "Логин") "Лучший вес" else "Best weight"
    "min_weight" -> if (strings.login == "Логин") "Минимальный вес" else "Smallest weight"
    "count" -> if (strings.login == "Логин") "Количество" else "Count"
    "total_weight" -> if (strings.login == "Логин") "Суммарный вес" else "Total weight"
    else -> metric
}

private fun tournamentMetricValueLabel(strings: RiverStrings, metric: String, value: Double): String = when (metric) {
    "count" -> value.toInt().toString()
    else -> value.asKgCompact(strings)
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
        fishId = entry.fishId,
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

private fun tournamentPrizeLabel(
    strings: RiverStrings,
    prize: PrizeSpecDto,
    shopPacks: List<ShopPackageDto>,
): String = when {
    prize.packageId == "coins" || prize.coins != null -> "+${prize.coins ?: prize.qty}"
    prize.qty > 1 -> "${tournamentPrizePackName(strings, prize.packageId, shopPacks)} ×${prize.qty}"
    else -> tournamentPrizePackName(strings, prize.packageId, shopPacks)
}

private fun tournamentPrizePackName(
    strings: RiverStrings,
    packageId: String,
    shopPacks: List<ShopPackageDto>,
): String =
    shopPacks.firstOrNull { it.id == packageId }?.name
        ?: achievementRewardPackName(strings, packageId)

private fun achievementRewardPackName(strings: RiverStrings, packId: String): String = when (packId) {
    "fresh_topup_s" -> if (strings.login == "Логин") "Пресное пополнение S" else "Freshwater Top-up S"
    "fresh_stock_m" -> if (strings.login == "Логин") "Пресный запас M" else "Freshwater Stock M"
    "fresh_crate_l" -> if (strings.login == "Логин") "Пресный ящик L" else "Freshwater Crate L"
    "salt_topup_s" -> if (strings.login == "Логин") "Морское пополнение S" else "Saltwater Top-up S"
    "salt_stock_m" -> if (strings.login == "Логин") "Морской запас M" else "Saltwater Stock M"
    "salt_crate_l" -> if (strings.login == "Логин") "Морской ящик L" else "Saltwater Crate L"
    "fresh_boost_s" -> if (strings.login == "Логин") "Пресный буст S" else "Fresh Boost S"
    "fresh_boost_m" -> if (strings.login == "Логин") "Пресный буст M" else "Fresh Boost M"
    "fresh_boost_l" -> if (strings.login == "Логин") "Пресный буст L" else "Fresh Boost L"
    "salt_boost_s" -> if (strings.login == "Логин") "Морской буст S" else "Saltwater Boost S"
    "salt_boost_m" -> if (strings.login == "Логин") "Морской буст M" else "Saltwater Boost M"
    "salt_boost_l" -> if (strings.login == "Логин") "Морской буст L" else "Saltwater Boost L"
    "bundle_starter" -> if (strings.login == "Логин") "Стартовый набор" else "Starter Pack"
    "bundle_pro" -> if (strings.login == "Логин") "Профи рыболов" else "Pro Angler"
    "bundle_whale" -> if (strings.login == "Логин") "Китовый ящик" else "Whale Crate"
    "micro_pred_fresh" -> if (strings.login == "Логин") "Пополнение пресных хищных" else "Predator Top-up"
    "micro_salt_starter" -> if (strings.login == "Логин") "Морской старт" else "Sea Start"
    "micro_salt_pred_refill" -> if (strings.login == "Логин") "Морской хищный запас" else "Saltwater Predator Stock"
    "autofish" -> if (strings.login == "Логин") "Автоловля" else "Auto Catch"
    "autofish_week" -> if (strings.login == "Логин") "Автоловля (неделя)" else "Auto Catch (week)"
    else -> humanizePackId(packId)
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

private fun youLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Ты" else "You"

private fun regularTournamentsLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Регулярные" else "Regular"

private fun specialTournamentsLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Специальные" else "Special"

private const val EVENT_BOARD_WEIGHT = "weight"
private const val EVENT_BOARD_COUNT = "count"
private const val EVENT_BOARD_FISH = "fish"

private fun eventLeaderboardLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Зачет" else "Leaderboard"

private fun eventLeaderboardOptions(strings: RiverStrings): List<Pair<String, String>> = listOf(
    EVENT_BOARD_WEIGHT to eventTotalWeightLabel(strings),
    EVENT_BOARD_COUNT to eventTotalCountLabel(strings),
    EVENT_BOARD_FISH to eventTopFishLabel(strings),
)

private fun specialEventBoardTitle(strings: RiverStrings, board: String): String = when (board) {
    EVENT_BOARD_COUNT -> eventTotalCountLabel(strings)
    EVENT_BOARD_FISH -> eventTopFishLabel(strings)
    else -> eventTotalWeightLabel(strings)
}

private fun eventLocationsLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Ивентовые" else "Events"

private fun specialEventLocationLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Событие" else "Event"

private fun clubTournamentLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Турнир" else "Tournament"

private fun currentTournamentPeriodLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Текущий" else "Current"

private fun previousLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Прошлый" else "Previous"

private fun eventsEmptyLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Нет специального события" else "No special event"

private fun eventTotalWeightLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Клубы: суммарный вес" else "Clubs: total weight"

private fun eventTotalCountLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Клубы: количество рыб" else "Clubs: fish count"

private fun eventTopFishLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Игроки: редкая крупная рыба" else "Players: rare heavy fish"

private fun achievementNameForCatch(
    strings: RiverStrings,
    achievements: List<AchievementDto>,
    code: String,
): String =
    achievements.firstOrNull { it.code == code }?.name
        ?: if (strings.login == "Логин") code.replace('_', ' ') else code.replace('_', ' ')

private fun isFishingCastActive(phase: FishingPhase): Boolean = when (phase) {
    FishingPhase.WAITING_BITE,
    FishingPhase.BITING,
    FishingPhase.TAP_CHALLENGE,
    FishingPhase.RESOLVING,
        -> true
    FishingPhase.READY,
    FishingPhase.COOLDOWN,
        -> false
}

private fun isFishDiscovered(
    fishId: Long?,
    fishName: String?,
    ownerUserId: Long?,
    currentUserId: Long,
    caughtFishIds: Collection<Long>,
    fishGuide: List<GuideFishDto>?,
): Boolean {
    if (ownerUserId != null && ownerUserId == currentUserId) return true
    if (fishId != null) return caughtFishIds.contains(fishId)
    val resolvedFishId = fishGuide?.firstOrNull { it.name == fishName }?.id
    return resolvedFishId != null && caughtFishIds.contains(resolvedFishId)
}

private fun humanizePackId(packId: String): String = when {
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
    val normalizedCode = code.lowercase(Locale.US)
    val base = when {
        normalizedCode == "river_delta_all_fish" -> "explorer_delta_river"
        normalizedCode.endsWith("_all_fish") -> "explorer_${normalizedCode.removeSuffix("_all_fish")}"
        else -> normalizedCode
    }
    return localAsset("achievements/${base}_$suffix.webp")
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
    if (strings.login == "Логин") "Откроется на ${unlockKg.asGuideKg(strings)}" else "Unlock at ${unlockKg.asGuideKg(strings)}"

private fun catchToLearnLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Поймайте, чтобы открыть" else "Catch it to reveal details"

private fun discoveredLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Открыто" else "Discovered"

private fun hiddenLabel(strings: RiverStrings): String =
    if (strings.login == "Логин") "Скрыто" else "Hidden"

private fun Double.asGuideKg(strings: RiverStrings): String =
    String.format(Locale.US, "%.0f %s", this, if (strings.login == "Логин") "кг" else "kg")

private fun phaseAccentColor(phase: FishingPhase): Color = when (phase) {
    FishingPhase.READY -> RiverTide
    FishingPhase.COOLDOWN -> RiverSlate
    FishingPhase.WAITING_BITE -> RiverFoam
    FishingPhase.BITING -> RiverCoral
    FishingPhase.TAP_CHALLENGE -> RiverAmber
    FishingPhase.RESOLVING -> RiverMoss
}

private fun stringsArrow(): String = "›"

private fun Double.asKgCompact(strings: RiverStrings): String =
    String.format(Locale.US, "%.2f %s", this, if (strings.login == "Логин") "кг" else "kg")

private fun String?.rarityLabel(strings: RiverStrings): String = strings.rarityLabel(this)

private fun guideRaritySortRank(rarity: String?): Int = when (rarity) {
    "common" -> 0
    "uncommon" -> 1
    "rare" -> 2
    "epic" -> 3
    "mythic" -> 4
    "legendary" -> 5
    else -> Int.MAX_VALUE
}

private fun lureGuideGroupKey(name: String): String = when (name) {
    "Зерновая крошка", "Grain Crumble", "Луговой червь", "Meadow Worm" -> "fresh_peace"
    "Ручейный малек", "Brook Minnow", "Серебряный живец", "Silver Shiner" -> "fresh_predator"
    "Морская водоросль", "Seaweed Strand", "Неоновый планктон", "Neon Plankton" -> "salt_peace"
    "Кольца кальмара", "Squid Rings", "Королевская креветка", "Royal Shrimp" -> "salt_predator"
    else -> "single:$name"
}

private fun lureGuideGroupTitle(
    strings: RiverStrings,
    groupKey: String,
    memberNames: List<String>,
): String = when (groupKey) {
    "fresh_peace" ->
        if (strings.login == "Логин") "Зерновая крошка / Луговой червь" else "Grain Crumble / Meadow Worm"
    "fresh_predator" ->
        if (strings.login == "Логин") "Ручейный малек / Серебряный живец" else "Brook Minnow / Silver Shiner"
    "salt_peace" ->
        if (strings.login == "Логин") "Морская водоросль / Неоновый планктон" else "Seaweed Strand / Neon Plankton"
    "salt_predator" ->
        if (strings.login == "Логин") "Кольца кальмара / Королевская креветка" else "Squid Rings / Royal Shrimp"
    else -> memberNames.distinct().joinToString(" / ")
}

private fun pendingPrizeLabel(strings: RiverStrings, prize: PrizeDto): String = when {
    prize.coins != null -> pendingPrizeCoinsLabel(strings, prize.coins)
    prize.packageId.equals("coins", ignoreCase = true) -> pendingPrizeCoinsLabel(strings, prize.qty)
    prize.qty > 1 -> "${achievementRewardPackName(strings, prize.packageId)} ×${prize.qty}"
    else -> achievementRewardPackName(strings, prize.packageId)
}

private fun pendingPrizeCoinsLabel(strings: RiverStrings, amount: Int): String =
    if (strings.login == "Логин") "$amount монет" else "$amount coins"

private fun tournamentPrizeDetailsLabel(strings: RiverStrings, prize: PrizeDto): String? = when {
    prize.source == "tournament" -> listOfNotNull(
        prize.rank.takeIf { it > 0 }?.let { tournamentPlaceLabel(strings, it) },
        prize.sourceLabel?.takeIf { it.isNotBlank() },
    ).joinToString(" • ").ifBlank { null }
    prize.source == "club" -> if (strings.login == "Логин") "Награда клуба" else "Club reward"
    else -> prize.sourceLabel?.takeIf { it.isNotBlank() }
}

private fun tournamentPlaceLabel(strings: RiverStrings, rank: Int): String =
    if (strings.login == "Логин") "$rank место" else "${englishOrdinal(rank)} place"

private fun englishOrdinal(value: Int): String {
    val mod100 = value % 100
    val suffix = if (mod100 in 11..13) {
        "th"
    } else {
        when (value % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$value$suffix"
}

private fun isTournamentPrize(prize: PrizeDto): Boolean = prize.source == "tournament"

private fun isRatingPrize(prize: PrizeDto): Boolean = prize.source == "rating"

private fun isClubPrize(prize: PrizeDto): Boolean = prize.source == "club"

private fun isEventPrize(prize: PrizeDto): Boolean = prize.source == "event"

private fun achievementRewardTitle(
    strings: RiverStrings,
    reward: AchievementRewardDto,
    pack: ShopPackageDto?,
): String = when {
    reward.coins != null || reward.pack.equals("coins", ignoreCase = true) ->
        if (strings.login == "Логин") "Монеты" else "Coins"
    reward.pack.isBlank() ->
        if (strings.login == "Логин") "Награда" else "Reward"
    else -> pack?.name ?: achievementRewardPackName(strings, reward.pack)
}

private fun achievementRewardQuantityLabel(strings: RiverStrings, qty: Int): String =
    if (strings.login == "Логин") {
        when {
            qty % 10 == 1 && qty % 100 != 11 -> "$qty пак"
            qty % 10 in 2..4 && qty % 100 !in 12..14 -> "$qty пака"
            else -> "$qty паков"
        }
    } else {
        "$qty pack${if (qty == 1) "" else "s"}"
    }

private fun rarityColor(rarity: String?): Color = when (rarity) {
    "common" -> Color(0xFFE8EEF2)
    "uncommon" -> Color(0xFF74D77C)
    "rare" -> Color(0xFF58A9FF)
    "epic" -> Color(0xFFC576FF)
    "mythic" -> Color(0xFFFF6B76)
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

private fun formatWeekRange(value: String, strings: RiverStrings): String =
    runCatching {
        val start = LocalDate.parse(value)
        val end = start.plusDays(6)
        val formatter = DateTimeFormatter.ofPattern(
            "dd MMM",
            if (strings.login == "Логин") Locale("ru", "RU") else Locale.US,
        )
        "${start.format(formatter)} – ${end.format(formatter)}"
    }.getOrElse { value }

private val clubChatJson = Json { ignoreUnknownKeys = true }

private fun buildClubChatMessage(raw: String, strings: RiverStrings): AnnotatedString {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{")) {
        val payload = runCatching { clubChatJson.parseToJsonElement(trimmed).jsonObject }.getOrNull()
        val key = payload?.get("key")?.jsonPrimitive?.contentOrNull
        val params = payload?.get("params")
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.mapNotNull { (name, value) -> value.jsonPrimitive.contentOrNull?.let { name to it } }
            ?.toMap()
            .orEmpty()
        if (key != null) {
            buildClubChatPayloadMessage(key, params, strings)?.let { return it }
        }
    }
    return highlightPlainClubChatMessage(raw, strings)
}

private fun buildClubChatPayloadMessage(
    key: String,
    params: Map<String, String>,
    strings: RiverStrings,
): AnnotatedString? {
    val isRussian = strings.login == "Логин"

    fun playerName(paramKey: String): String =
        params[paramKey].orEmpty().ifBlank { if (isRussian) "Игрок" else "Player" }

    fun AnnotatedString.Builder.appendCoins(value: String) {
        appendHighlighted(value, RiverAmber)
    }

    fun AnnotatedString.Builder.appendFishName(value: String, rarity: String?) {
        appendHighlighted(value, rarityColor(rarity))
    }

    return when (key) {
        "clubChatMemberMessage" -> buildAnnotatedString {
            val rank = params["rank"] ?: params["role"]
            val sender = params["sender"] ?: params["name"] ?: playerName("sender")
            appendRoleHighlighted(sender, rank)
            append(" ")
            appendRoleHighlighted("(${strings.roleLabel(rank.orEmpty())})", rank)
            append(": ")
            append(params["text"] ?: params["message"].orEmpty())
        }
        "clubChatMemberJoined" -> buildAnnotatedString {
            appendPlayerName(playerName("name"))
            append(if (isRussian) " вступил в клуб." else " joined the club.")
        }
        "clubChatMemberLeft" -> buildAnnotatedString {
            appendPlayerName(playerName("name"))
            append(if (isRussian) " покинул клуб." else " left the club.")
        }
        "clubChatMemberKicked" -> buildAnnotatedString {
            appendPlayerName(playerName("actor"))
            append(if (isRussian) " исключил " else " kicked ")
            appendPlayerName(playerName("target"))
            append(if (isRussian) " из клуба." else " from the club.")
        }
        "clubChatPresidentAppointed" -> buildAnnotatedString {
            appendPlayerName(playerName("actor"))
            append(if (isRussian) " назначил " else " appointed ")
            appendPlayerName(playerName("target"))
            append(if (isRussian) " президентом клуба." else " as club president.")
        }
        "clubChatRolePromoted" -> buildAnnotatedString {
            appendPlayerName(playerName("actor"))
            append(if (isRussian) " повысил " else " promoted ")
            appendPlayerName(playerName("target"))
            append(".")
        }
        "clubChatRoleDemoted" -> buildAnnotatedString {
            appendPlayerName(playerName("actor"))
            append(if (isRussian) " понизил " else " demoted ")
            appendPlayerName(playerName("target"))
            append(".")
        }
        "clubChatRatingReward" -> buildAnnotatedString {
            appendPlayerName(playerName("name"))
            append(if (isRussian) " получил " else " received ")
            appendCoins(params["coins"].orEmpty().ifBlank { "0" })
            append(if (isRussian) " монет за рейтинг." else " coins for rating.")
        }
        "clubChatRareCatch" -> buildAnnotatedString {
            val rarity = params["rarity"]
            appendPlayerName(playerName("name"))
            append(if (isRussian) " поймал " else " caught a ")
            append(clubChatRarityPhrase(rarity, strings))
            append(if (isRussian) " рыбу: " else " fish: ")
            appendFishName(params["fish"].orEmpty(), rarity)
            params["location"]?.takeIf { it.isNotBlank() }?.let { location ->
                append(if (isRussian) " на локации " else " at location ")
                append(location)
                params["weight"]?.takeIf { it.isNotBlank() }?.let { weight ->
                    append(if (isRussian) ", $weight кг" else ", $weight kg")
                }
            }
            append(".")
        }
        else -> null
    }
}

private fun highlightPlainClubChatMessage(raw: String, strings: RiverStrings): AnnotatedString {
    val ruMatch = Regex(
        pattern = "^(.+?)( поймал )(мифическую|легендарную) рыбу: (.+?)(\\.?| на.*)$",
        option = RegexOption.IGNORE_CASE,
    ).matchEntire(raw)
    if (ruMatch != null) {
        val (name, action, rarityLabel, fishName, suffix) = ruMatch.destructured
        val rarity = if (rarityLabel.equals("мифическую", ignoreCase = true)) "mythic" else "legendary"
        return buildAnnotatedString {
            appendPlayerName(name)
            append(action)
            append(rarityLabel)
            append(" рыбу: ")
            appendHighlighted(fishName, rarityColor(rarity))
            append(suffix)
        }
    }

    val enMatch = Regex(
        pattern = "^(.+?)( caught a )(mythic|legendary) fish: (.+?)(\\.?| at.*)$",
        option = RegexOption.IGNORE_CASE,
    ).matchEntire(raw)
    if (enMatch != null) {
        val (name, action, rarityLabel, fishName, suffix) = enMatch.destructured
        return buildAnnotatedString {
            appendPlayerName(name)
            append(action)
            append(rarityLabel)
            append(" fish: ")
            appendHighlighted(fishName, rarityColor(rarityLabel.lowercase(Locale.ROOT)))
            append(suffix)
        }
    }

    return AnnotatedString(raw)
}

private fun AnnotatedString.Builder.appendPlayerName(name: String) {
    appendHighlighted(name, RiverFoam)
}

private fun AnnotatedString.Builder.appendRoleHighlighted(value: String, role: String?) {
    appendHighlighted(value, clubRoleColor(role))
}

private fun AnnotatedString.Builder.appendHighlighted(value: String, color: Color) {
    withStyle(
        SpanStyle(
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    ) {
        append(value)
    }
}

private fun clubRoleColor(role: String?): Color = when (role) {
    "president" -> Color(0xFFFFD76A)
    "heir" -> Color(0xFFFF6B76)
    "veteran" -> Color(0xFF58A9FF)
    "novice" -> Color(0xFF74D77C)
    else -> RiverFoam
}

private fun clubChatRarityPhrase(rarity: String?, strings: RiverStrings): String {
    if (strings.login == "Логин") {
        return when (rarity) {
            "mythic" -> "мифическую"
            "legendary" -> "легендарную"
            else -> rarity.orEmpty()
        }
    }
    return strings.rarityLabel(rarity).lowercase(Locale.ROOT)
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

private fun lureAsset(name: String?, displayName: String?): String? {
    val keys = listOfNotNull(name, displayName)
    val path = keys.firstNotNullOfOrNull { key ->
        when (key) {
            "Пресная мирная", "Зерновая крошка", "Grain Crumble" -> "baits/grain_crumble.webp"
            "Пресная хищная", "Ручейный малек", "Brook Minnow" -> "baits/brook_minnow.webp"
            "Морская мирная", "Морская водоросль", "Seaweed Strand" -> "baits/seaweed_strand.webp"
            "Морская хищная", "Кольца кальмара", "Squid Rings" -> "baits/squid_rings.webp"
            "Пресная мирная+", "Луговой червь", "Meadow Worm" -> "baits/meadow_worm.webp"
            "Пресная хищная+", "Серебряный живец", "Silver Shiner" -> "baits/silver_shiner.webp"
            "Морская мирная+", "Неоновый планктон", "Neon Plankton" -> "baits/neon_plankton.webp"
            "Морская хищная+", "Королевская креветка", "Royal Shrimp" -> "baits/royal_shrimp.webp"
            else -> null
        }
    }
    return path?.let(::localAsset)
}

private fun localizedAppError(strings: RiverStrings, message: String): String {
    val ru = strings.login == "Логин"
    return when (message) {
        "No bait available" -> if (ru) "Нет приманок. Забери ежедневные или купи в магазине." else "No baits. Claim daily ones or buy in the shop."
        "No suitable fish" -> if (ru) "Нет подходящей рыбы на эту наживку." else "No suitable fish for this bait."
        "casting" -> if (ru) "Заброс уже выполняется." else "Cast in progress already."
        "failed" -> if (ru) "Не удалось выполнить действие." else "Action failed."
        else -> message
    }
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
        "Игапо, затопленный лес", "Igapo Flooded Forest", "Flooded Forest" -> "backgrounds/flooded_forest.webp"
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

private fun fishAssetModel(name: String?): String? =
    name?.let(FISH_ASSET_MAP::get)?.let(::localAsset)

private fun resolveCatchRarity(catch: CatchDto, fishGuide: List<GuideFishDto>?): String? =
    catch.rarity.takeIf { it.isNotBlank() }
        ?: fishGuide?.firstOrNull { it.name == catch.fish }?.rarity

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
    "Омуль арктический" to "fish/omul_arcticheskiy.webp",
    "Arctic Omul" to "fish/omul_arcticheskiy.webp",
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
