package com.riverking.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.AchievementClaimDto
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.AppUpdateInfoDto
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.CatchStatsDto
import com.riverking.mobile.auth.CastResultDto
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.ClubChatMessageDto
import com.riverking.mobile.auth.ClubDetailsDto
import com.riverking.mobile.auth.ClubSummaryDto
import com.riverking.mobile.auth.CurrentTournamentDto
import com.riverking.mobile.auth.GuideDto
import com.riverking.mobile.auth.GuideFishDto
import com.riverking.mobile.auth.HookResultDto
import com.riverking.mobile.auth.HookedFishDto
import com.riverking.mobile.auth.MeResponseDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.QuestListDto
import com.riverking.mobile.auth.ReferralInfoDto
import com.riverking.mobile.auth.ReferralRewardDto
import com.riverking.mobile.auth.ShopCategoryDto
import com.riverking.mobile.auth.StartCastResultDto
import com.riverking.mobile.auth.TelegramLinkPollResult
import com.riverking.mobile.auth.TelegramLinkStartDto
import com.riverking.mobile.auth.TelegramLoginPollResult
import com.riverking.mobile.auth.TournamentDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AuthMode {
    LOGIN,
    REGISTER,
}

enum class FishingPhase {
    READY,
    COOLDOWN,
    WAITING_BITE,
    BITING,
    TAP_CHALLENGE,
    RESOLVING,
}

data class FishingCastSpot(
    val xRoll: Float,
    val yRoll: Float,
    val proMode: Boolean = false,
    val castDurationMillis: Int = CAST_ANIMATION_DEFAULT_MILLIS,
)

enum class RatingsMode(val apiValue: String) {
    PERSONAL("personal"),
    GLOBAL("global"),
}

enum class RatingsPeriod(val apiValue: String) {
    TODAY("today"),
    YESTERDAY("yesterday"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ALL("all"),
}

enum class RatingsOrder(val apiValue: String) {
    DESC("desc"),
    ASC("asc"),
}

sealed interface PlayPurchaseSyncResult {
    data object Completed : PlayPurchaseSyncResult

    data object Duplicate : PlayPurchaseSyncResult

    data class Failed(
        val code: String,
        val message: String,
    ) : PlayPurchaseSyncResult
}

data class FishingUiState(
    val phase: FishingPhase = FishingPhase.READY,
    val phaseTimeLeftMillis: Long = 0L,
    val tapCount: Int = 0,
    val tapGoal: Int = TAP_CHALLENGE_GOAL,
    val tapDurationMillis: Long = TAP_CHALLENGE_MILLIS,
    val hookedFish: HookedFishDto? = null,
    val struggleIntensity: Double = 0.0,
    val autoCastEnabled: Boolean = false,
    val castWaitSeconds: Int = 0,
    val castSpot: FishingCastSpot? = null,
    val lastStart: StartCastResultDto? = null,
    val lastCast: CastResultDto? = null,
    val lastCatchWasNewFish: Boolean = false,
    val lastEscape: Boolean = false,
)

data class TournamentsUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val current: CurrentTournamentDto? = null,
    val upcoming: List<TournamentDto> = emptyList(),
    val past: List<TournamentDto> = emptyList(),
    val prizes: List<PrizeDto> = emptyList(),
    val selectedTournamentId: Long? = null,
    val selectedTournament: CurrentTournamentDto? = null,
    val selectedTournamentLoading: Boolean = false,
)

data class RatingsUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val mode: RatingsMode = RatingsMode.GLOBAL,
    val period: RatingsPeriod = RatingsPeriod.TODAY,
    val order: RatingsOrder = RatingsOrder.DESC,
    val locationId: String = "all",
    val fishId: String = "all",
    val fishOptions: List<GuideFishDto> = emptyList(),
    val entries: List<CatchDto> = emptyList(),
)

data class GuideUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val guide: GuideDto? = null,
    val achievements: List<AchievementDto> = emptyList(),
    val quests: QuestListDto? = null,
)

data class ClubUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val club: ClubDetailsDto? = null,
    val searchResults: List<ClubSummaryDto> = emptyList(),
    val searchLoading: Boolean = false,
    val chat: List<ClubChatMessageDto> = emptyList(),
    val chatLoading: Boolean = false,
    val chatOlderLoading: Boolean = false,
    val chatHasMore: Boolean = true,
    val chatSending: Boolean = false,
)

data class ShopUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val categories: List<ShopCategoryDto> = emptyList(),
)

data class ReferralUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val referrals: ReferralInfoDto? = null,
    val rewards: List<ReferralRewardDto> = emptyList(),
)

data class CatchStatsUiState(
    val loading: Boolean = false,
    val period: String = "all",
    val stats: CatchStatsDto? = null,
)

data class RiverKingUiState(
    val loading: Boolean = true,
    val me: MeResponseDto? = null,
    val authProvider: String? = null,
    val login: String = "",
    val password: String = "",
    val nickname: String = "",
    val authMode: AuthMode = AuthMode.LOGIN,
    val working: Boolean = false,
    val telegramLoginPending: Boolean = false,
    val telegramLinkPending: Boolean = false,
    val pendingExternalUrl: String? = null,
    val profileRefreshing: Boolean = false,
    val error: String? = null,
    val appUpdate: AppUpdateInfoDto? = null,
    val selectedCatch: CatchDto? = null,
    val selectedCatchCard: ByteArray? = null,
    val catchLoading: Boolean = false,
    val lastAchievementReward: AchievementClaimDto? = null,
    val fishing: FishingUiState = FishingUiState(),
    val tournaments: TournamentsUiState = TournamentsUiState(),
    val ratings: RatingsUiState = RatingsUiState(),
    val guide: GuideUiState = GuideUiState(),
    val club: ClubUiState = ClubUiState(),
    val shop: ShopUiState = ShopUiState(),
    val referrals: ReferralUiState = ReferralUiState(),
    val catchStats: CatchStatsUiState = CatchStatsUiState(),
)

class RiverKingViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RiverKingUiState())
    val state: StateFlow<RiverKingUiState> = _state.asStateFlow()

    private var waitJob: Job? = null
    private var biteJob: Job? = null
    private var tapJob: Job? = null
    private var cooldownJob: Job? = null
    private var appUpdateJob: Job? = null
    private var telegramLoginJob: Job? = null
    private var telegramLinkJob: Job? = null
    private var postCatchProgressRefreshJob: Job? = null
    private var dismissedRecommendedUpdateCode: Int? = null
    private var hookReactionSeconds: Double = FAIL_REACTION_SECONDS
    private var biteStartedAtMillis: Long = 0L

    init {
        bootstrap()
    }

    override fun onCleared() {
        cancelFishingJobs()
        cancelTelegramJobs()
        postCatchProgressRefreshJob?.cancel()
        appUpdateJob?.cancel()
        super.onCleared()
    }

    fun updateLogin(value: String) {
        repository.rememberLoginDraft(value)
        _state.update { it.copy(login = value) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun updateNickname(value: String) {
        _state.update { it.copy(nickname = value) }
    }

    fun toggleAuthMode() {
        _state.update {
            it.copy(
                authMode = if (it.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                error = null,
            )
        }
    }

    fun submitPasswordAuth() {
        val login = state.value.login.trim()
        val password = state.value.password
        if (login.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Login and password are required") }
            return
        }
        if (state.value.authMode == AuthMode.REGISTER) {
            when {
                !REGISTER_LOGIN_REGEX.matches(login.lowercase()) -> {
                    _state.update { it.copy(error = "Use 3-32 lowercase letters, digits, dot, underscore, or dash") }
                    return
                }
                password.length !in 8..128 -> {
                    _state.update { it.copy(error = "Password must be between 8 and 128 characters") }
                    return
                }
            }
        }
        repository.rememberLoginDraft(login)

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = when (state.value.authMode) {
                    AuthMode.LOGIN -> repository.loginPassword(login, password)
                    AuthMode.REGISTER -> repository.registerPassword(login, password)
                }
                onAuthenticated(me)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.loginGoogle(idToken)
                onAuthenticated(me)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun startTelegramLogin() {
        if (state.value.working || state.value.telegramLoginPending) return
        telegramLoginJob?.cancel()
        telegramLoginJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    working = true,
                    error = null,
                    telegramLoginPending = false,
                )
            }
            try {
                val started = repository.startTelegramLogin()
                repository.rememberPendingTelegramLogin(started.sessionToken)
                _state.update {
                    it.copy(
                        working = false,
                        telegramLoginPending = true,
                        pendingExternalUrl = started.telegramLink,
                    )
                }
                awaitTelegramLogin(started.sessionToken)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        telegramLoginPending = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun startTelegramLink() {
        val me = state.value.me ?: return
        if (state.value.working || state.value.telegramLinkPending || me.telegramLinked) return
        telegramLinkJob?.cancel()
        telegramLinkJob = viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val started = repository.startTelegramLink()
                repository.rememberPendingTelegramLink(started.sessionToken)
                _state.update {
                    it.copy(
                        working = false,
                        telegramLinkPending = true,
                        pendingExternalUrl = started.telegramLink,
                    )
                }
                awaitTelegramLink(started.sessionToken)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        telegramLinkPending = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun saveNickname() {
        val nickname = state.value.nickname.trim()
        if (nickname.isBlank()) {
            _state.update { it.copy(error = "Nickname is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.updateNickname(nickname)
                applyProfileUpdate(me)
                _state.update { it.copy(working = false) }
                warmupPlayerSurfaces()
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun changeLanguage(language: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.changeLanguage(language)
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        ratings = it.ratings.copy(loaded = false),
                        guide = it.guide.copy(loaded = false),
                        shop = it.shop.copy(loaded = false),
                        tournaments = it.tournaments.copy(loaded = false, selectedTournament = null),
                    )
                }
                warmupPlayerSurfaces()
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun refreshProfile() {
        if (state.value.profileRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(profileRefreshing = true, error = null) }
            try {
                val me = repository.refreshProfile()
                applyProfileUpdate(me)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        profileRefreshing = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun claimDaily() {
        if (state.value.fishing.phase == FishingPhase.RESOLVING) return
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.claimDaily()
                applyProfileUpdate(me)
                _state.update { it.copy(working = false) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun toggleAutoCast() {
        _state.update {
            val enabled = !it.fishing.autoCastEnabled
            it.copy(fishing = it.fishing.copy(autoCastEnabled = enabled))
        }
        val shouldStart = state.value.fishing.autoCastEnabled &&
            state.value.me?.autoFish == true &&
            state.value.fishing.phase == FishingPhase.READY
        if (shouldStart) {
            beginCast(auto = true)
        }
    }

    fun beginCast(auto: Boolean = false, visualSpot: FishingCastSpot? = null) {
        val current = state.value
        val me = current.me ?: return
        if (current.fishing.phase != FishingPhase.READY) return
        val currentLure = me.lures.firstOrNull { it.id == me.currentLureId }
        if (currentLure == null || currentLure.qty <= 0) {
            _state.update { it.copy(error = "No bait available") }
            return
        }

        val castSpot = visualSpot ?: randomFishingCastSpot()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    fishing = it.fishing.copy(
                        phase = FishingPhase.RESOLVING,
                        phaseTimeLeftMillis = 0L,
                        castSpot = castSpot,
                        lastCast = null,
                        lastEscape = false,
                        tapCount = 0,
                        hookedFish = null,
                        tapGoal = TAP_CHALLENGE_GOAL,
                        tapDurationMillis = TAP_CHALLENGE_MILLIS,
                        struggleIntensity = 0.0,
                    ),
                    error = null,
                )
            }
            try {
                val start = repository.startCast()
                val waitSeconds = Random.nextInt(5, 31)
                val nextLureId = start.currentLureId ?: me.currentLureId
                _state.update { currentState ->
                    currentState.copy(
                        me = currentState.me?.let { profile ->
                            profile.copy(
                                currentLureId = nextLureId,
                                lures = profile.lures.map { lure ->
                                    if (lure.id == me.currentLureId) {
                                        lure.copy(qty = (lure.qty - 1).coerceAtLeast(0))
                                    } else {
                                        lure
                                    }
                                },
                            )
                        },
                        fishing = currentState.fishing.copy(
                            phase = FishingPhase.WAITING_BITE,
                            phaseTimeLeftMillis = waitSeconds * 1_000L,
                            castWaitSeconds = waitSeconds,
                            castSpot = castSpot,
                            lastStart = start,
                            lastCast = null,
                            lastCatchWasNewFish = false,
                            lastEscape = false,
                        ),
                    )
                }
                launchWaitCountdown(waitSeconds)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        fishing = it.fishing.copy(
                            phase = FishingPhase.READY,
                            phaseTimeLeftMillis = 0L,
                            castSpot = null,
                        ),
                        error = message,
                    )
                }
            }
        }
    }

    fun hookFish() {
        if (state.value.fishing.phase != FishingPhase.BITING) return
        performHook(auto = false)
    }

    fun registerTap() {
        if (state.value.fishing.phase != FishingPhase.TAP_CHALLENGE) return
        val nextCount = state.value.fishing.tapCount + 1
        _state.update { it.copy(fishing = it.fishing.copy(tapCount = nextCount)) }
        if (nextCount >= state.value.fishing.tapGoal) {
            finalizeCatch(success = true)
        }
    }

    fun dismissCatchDetails() {
        _state.update { it.copy(selectedCatch = null, selectedCatchCard = null, catchLoading = false) }
    }

    fun openCatchDetails(catch: CatchDto) {
        val catchId = catch.id
        if (catchId <= 0L) {
            _state.update { it.copy(selectedCatch = catch, selectedCatchCard = null) }
            return
        }
        viewModelScope.launch {
            val canShare = state.value.me?.id != null && catch.userId == state.value.me?.id
            _state.update { it.copy(catchLoading = true, selectedCatch = catch, selectedCatchCard = null) }
            val (detailed, cardBytes) = coroutineScope {
                val detailsDeferred = async { runCatching { repository.catchDetails(catchId) }.getOrDefault(catch) }
                val cardDeferred = async {
                    if (canShare) runCatching { repository.catchCard(catchId) }.getOrNull() else null
                }
                detailsDeferred.await() to cardDeferred.await()
            }
            _state.update { current ->
                if (current.selectedCatch?.id != catchId) {
                    current
                } else {
                    val merged = detailed.copy(
                        userId = detailed.userId ?: catch.userId,
                        fishId = detailed.fishId ?: catch.fishId,
                        user = detailed.user ?: catch.user,
                        at = detailed.at ?: catch.at,
                        rank = detailed.rank ?: catch.rank,
                        prizeCoins = detailed.prizeCoins ?: catch.prizeCoins,
                        rarity = detailed.rarity.ifBlank { catch.rarity },
                    )
                    current.copy(
                        selectedCatch = merged,
                        selectedCatchCard = cardBytes,
                        catchLoading = false,
                    )
                }
            }
        }
    }

    fun loadTournaments(force: Boolean = false) {
        if (state.value.me == null) return
        if (state.value.tournaments.loading) return
        if (!force && state.value.tournaments.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(tournaments = it.tournaments.copy(loading = true), error = null) }
            try {
                val data = coroutineScope {
                    val current = async { repository.loadCurrentTournament() }
                    val upcoming = async { repository.loadUpcomingTournaments() }
                    val past = async { repository.loadPastTournaments() }
                    val prizes = async { repository.loadPendingPrizes() }
                    TournamentsUiState(
                        loaded = true,
                        loading = false,
                        current = current.await(),
                        upcoming = upcoming.await(),
                        past = past.await(),
                        prizes = prizes.await(),
                        selectedTournamentId = state.value.tournaments.selectedTournamentId,
                        selectedTournament = state.value.tournaments.selectedTournament,
                    )
                }
                _state.update { it.copy(tournaments = data, error = null) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun openTournament(tournamentId: Long) {
        if (state.value.tournaments.selectedTournamentLoading) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    tournaments = it.tournaments.copy(
                        selectedTournamentId = tournamentId,
                        selectedTournamentLoading = true,
                    ),
                    error = null,
                )
            }
            try {
                val tournament = repository.loadTournament(tournamentId)
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(
                            selectedTournamentId = tournamentId,
                            selectedTournament = tournament,
                            selectedTournamentLoading = false,
                        ),
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(selectedTournamentLoading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun closeTournamentDetails() {
        _state.update {
            it.copy(
                tournaments = it.tournaments.copy(
                    selectedTournament = null,
                    selectedTournamentId = null,
                    selectedTournamentLoading = false,
                )
            )
        }
    }

    fun claimPrize(prizeId: Long) {
        if (state.value.tournaments.loading) return
        viewModelScope.launch {
            _state.update { it.copy(tournaments = it.tournaments.copy(loading = true), error = null) }
            try {
                val me = repository.claimPrize(prizeId)
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(loaded = false, loading = false),
                        error = null,
                    )
                }
                loadTournaments(force = true)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun setRatingsMode(mode: RatingsMode) {
        _state.update { it.copy(ratings = it.ratings.copy(mode = mode)) }
        loadRatings(force = true)
    }

    fun setRatingsPeriod(period: RatingsPeriod) {
        _state.update { it.copy(ratings = it.ratings.copy(period = period)) }
        loadRatings(force = true)
    }

    fun setRatingsOrder(order: RatingsOrder) {
        _state.update { it.copy(ratings = it.ratings.copy(order = order)) }
        loadRatings(force = true)
    }

    fun setRatingsLocation(locationId: String) {
        _state.update {
            val currentRatings = it.ratings
            val nextFishId = if (isFishFilterValidForLocation(currentRatings.fishOptions, currentRatings.fishId, locationId)) {
                currentRatings.fishId
            } else {
                "all"
            }
            it.copy(ratings = currentRatings.copy(locationId = locationId, fishId = nextFishId))
        }
        loadRatings(force = true)
    }

    fun setRatingsFish(fishId: String) {
        _state.update { it.copy(ratings = it.ratings.copy(fishId = fishId)) }
        loadRatings(force = true)
    }

    fun loadRatings(force: Boolean = false) {
        val me = state.value.me ?: return
        if (state.value.ratings.loading) return
        if (!force && state.value.ratings.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(ratings = it.ratings.copy(loading = true), error = null) }
            try {
                val guide = if (state.value.ratings.fishOptions.isEmpty()) repository.loadGuide() else null
                val ratingsState = state.value.ratings
                val fishOptions = guide?.fish ?: ratingsState.fishOptions
                val normalizedFishId = if (isFishFilterValidForLocation(fishOptions, ratingsState.fishId, ratingsState.locationId)) {
                    ratingsState.fishId
                } else {
                    "all"
                }
                val usingSpecies = normalizedFishId != "all"
                val filter = if (usingSpecies) "species" else "location"
                val id = if (usingSpecies) normalizedFishId else ratingsState.locationId
                val raw = repository.loadRatings(
                    mode = ratingsState.mode.apiValue,
                    filter = filter,
                    id = id,
                    period = ratingsState.period.apiValue,
                    order = ratingsState.order.apiValue,
                )
                val filtered = if (usingSpecies && ratingsState.locationId != "all") {
                    val locationName = me.locations.firstOrNull { it.id.toString() == ratingsState.locationId }?.name
                    if (locationName == null) raw else raw.filter { it.location == locationName }
                } else {
                    raw
                }
                _state.update {
                    it.copy(
                        ratings = it.ratings.copy(
                            loaded = true,
                            loading = false,
                            fishId = normalizedFishId,
                            entries = filtered,
                            fishOptions = fishOptions,
                        ),
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        ratings = it.ratings.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    private fun isFishFilterValidForLocation(
        fishOptions: List<GuideFishDto>,
        fishId: String,
        locationId: String,
    ): Boolean {
        if (fishId == "all" || locationId == "all") return true
        val locationLong = locationId.toLongOrNull() ?: return true
        val fish = fishOptions.firstOrNull { it.id.toString() == fishId } ?: return true
        return fish.locationIds.contains(locationLong)
    }

    fun loadGuide(force: Boolean = false) {
        if (state.value.me == null) return
        if (state.value.guide.loading) return
        if (!force && state.value.guide.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(guide = it.guide.copy(loading = true), error = null) }
            try {
                val data = coroutineScope {
                    val guide = async { repository.loadGuide() }
                    val achievements = async { repository.loadAchievements() }
                    val quests = async { repository.loadQuests() }
                    GuideUiState(
                        loaded = true,
                        loading = false,
                        guide = guide.await(),
                        achievements = achievements.await(),
                        quests = quests.await(),
                    )
                }
                _state.update {
                    it.copy(
                        guide = data,
                        ratings = it.ratings.copy(
                            fishOptions = data.guide?.fish ?: it.ratings.fishOptions,
                        ),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        guide = it.guide.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun claimAchievement(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val reward = repository.claimAchievement(code)
                val me = repository.refreshProfile()
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        lastAchievementReward = reward,
                        guide = it.guide.copy(loaded = false),
                    )
                }
                loadGuide(force = true)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        working = false,
                        error = message,
                    )
                }
            }
        }
    }

    fun dismissAchievementReward() {
        _state.update { it.copy(lastAchievementReward = null) }
    }

    fun loadCatchStats(period: String) {
        if (state.value.me == null) return
        viewModelScope.launch {
            _state.update {
                it.copy(catchStats = it.catchStats.copy(loading = true, period = period))
            }
            try {
                val stats = repository.loadCatchStats(period)
                _state.update {
                    it.copy(catchStats = CatchStatsUiState(loading = false, period = period, stats = stats))
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        catchStats = it.catchStats.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun loadClub(force: Boolean = false) {
        if (state.value.me == null) return
        if (state.value.club.loading) return
        if (!force && state.value.club.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(club = it.club.copy(loading = true), error = null) }
            try {
                val club = repository.loadClub()
                _state.update {
                    it.copy(
                        club = it.club.copy(
                            loaded = true,
                            loading = false,
                            club = club,
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        club = it.club.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun loadClubChat() {
        if (state.value.club.chatLoading) return
        viewModelScope.launch {
            _state.update { it.copy(club = it.club.copy(chatLoading = true), error = null) }
            try {
                val messages = repository.loadClubChat()
                _state.update {
                    it.copy(
                        club = it.club.copy(
                            chat = messages,
                            chatLoading = false,
                            chatHasMore = messages.size >= 100,
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        club = it.club.copy(chatLoading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun loadOlderClubChat() {
        val clubState = state.value.club
        if (clubState.chatOlderLoading || clubState.chatLoading || !clubState.chatHasMore) return
        val oldestId = clubState.chat.firstOrNull()?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(club = it.club.copy(chatOlderLoading = true), error = null) }
            try {
                val older = repository.loadClubChat(beforeId = oldestId, limit = 20)
                _state.update { current ->
                    val existingIds = current.club.chat.map { it.id }.toSet()
                    current.copy(
                        club = current.club.copy(
                            chat = older.filterNot { it.id in existingIds } + current.club.chat,
                            chatOlderLoading = false,
                            chatHasMore = older.size >= 20,
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        club = it.club.copy(chatOlderLoading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun sendClubChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || state.value.club.chatSending) return
        viewModelScope.launch {
            _state.update { it.copy(club = it.club.copy(chatSending = true), error = null) }
            try {
                val message = repository.sendClubChatMessage(trimmed)
                _state.update { current ->
                    val exists = current.club.chat.any { it.id == message.id }
                    current.copy(
                        club = current.club.copy(
                            chat = if (exists) current.club.chat else current.club.chat + message,
                            chatSending = false,
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        club = it.club.copy(chatSending = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun searchClubs(query: String?) {
        if (state.value.club.searchLoading) return
        viewModelScope.launch {
            _state.update { it.copy(club = it.club.copy(searchLoading = true), error = null) }
            try {
                val results = repository.searchClubs(query)
                _state.update {
                    it.copy(
                        club = it.club.copy(
                            searchResults = results,
                            searchLoading = false,
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        club = it.club.copy(searchLoading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun createClub(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val club = repository.createClub(name)
                val me = repository.refreshProfile()
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        club = it.club.copy(
                            loaded = true,
                            club = club,
                            searchResults = emptyList(),
                        ),
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun joinClub(clubId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val club = repository.joinClub(clubId)
                val me = repository.refreshProfile()
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        club = it.club.copy(
                            loaded = true,
                            club = club,
                        ),
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun updateClubInfo(info: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val club = repository.updateClubInfo(info)
                _state.update { it.copy(working = false, club = it.club.copy(club = club)) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun updateClubSettings(minJoinWeightKg: Double, recruitingOpen: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val club = repository.updateClubSettings(minJoinWeightKg, recruitingOpen)
                _state.update { it.copy(working = false, club = it.club.copy(club = club)) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun leaveClub() {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                repository.leaveClub()
                _state.update {
                    it.copy(
                        working = false,
                        club = ClubUiState(),
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun clubMemberAction(memberId: Long, action: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val club = repository.clubMemberAction(memberId, action)
                _state.update { it.copy(working = false, club = it.club.copy(club = club)) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun loadShop(force: Boolean = false) {
        if (state.value.me == null) return
        if (state.value.shop.loading) return
        if (!force && state.value.shop.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(shop = it.shop.copy(loading = true), error = null) }
            try {
                val data = ShopUiState(
                    loaded = true,
                    loading = false,
                    categories = repository.loadShop(),
                )
                _state.update { it.copy(shop = data) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        shop = it.shop.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun loadReferrals(force: Boolean = false) {
        if (state.value.me == null) return
        if (state.value.referrals.loading) return
        if (!force && state.value.referrals.loaded) return
        viewModelScope.launch {
            _state.update { it.copy(referrals = it.referrals.copy(loading = true), error = null) }
            try {
                val data = coroutineScope {
                    val referrals = async { repository.loadReferrals() }
                    val rewards = async { repository.loadReferralRewards() }
                    ReferralUiState(
                        loaded = true,
                        loading = false,
                        referrals = referrals.await(),
                        rewards = rewards.await(),
                    )
                }
                _state.update { it.copy(referrals = data) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        referrals = it.referrals.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun generateReferral() {
        viewModelScope.launch {
            _state.update { it.copy(referrals = it.referrals.copy(loading = true), error = null) }
            try {
                val link = repository.generateReferral()
                _state.update {
                    val current = it.referrals.referrals
                    it.copy(
                        referrals = it.referrals.copy(
                            loaded = true,
                            loading = false,
                            referrals = ReferralInfoDto(
                                token = link.token,
                                invited = current?.invited ?: emptyList(),
                                link = link.link,
                                telegramLink = link.telegramLink,
                                androidShareText = link.androidShareText,
                                webFallbackLink = link.webFallbackLink,
                            )
                        )
                    )
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update {
                    it.copy(
                        referrals = it.referrals.copy(loading = false),
                        error = message,
                    )
                }
            }
        }
    }

    fun claimReferralRewards() {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.claimReferralRewards()
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        referrals = it.referrals.copy(loaded = false),
                    )
                }
                loadReferrals(force = true)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun buyShopWithCoins(packId: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.buyShopWithCoins(packId)
                applyProfileUpdate(me)
                _state.update { it.copy(working = false, shop = it.shop.copy(loaded = false)) }
                loadShop(force = true)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    suspend fun syncPlayPurchase(
        packId: String,
        purchaseToken: String,
        orderId: String? = null,
        purchaseTimeMillis: Long? = null,
    ): PlayPurchaseSyncResult {
        _state.update { it.copy(working = true, error = null) }
        return try {
            val me = repository.completePlayPurchase(
                packId = packId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTimeMillis = purchaseTimeMillis,
            )
            applyProfileUpdate(me)
            _state.update { it.copy(working = false, shop = it.shop.copy(loaded = false)) }
            loadShop(force = true)
            PlayPurchaseSyncResult.Completed
        } catch (error: Throwable) {
            val message = describeError(error)
            _state.update { it.copy(working = false) }
            when (message) {
                "duplicate_purchase" -> PlayPurchaseSyncResult.Duplicate
                else -> PlayPurchaseSyncResult.Failed(
                    code = message ?: "app_update_required",
                    message = message ?: "",
                )
            }
        }
    }

    fun completePlayPurchase(
        packId: String,
        purchaseToken: String,
        orderId: String? = null,
        purchaseTimeMillis: Long? = null,
    ) {
        viewModelScope.launch {
            when (
                val result = syncPlayPurchase(
                    packId = packId,
                    purchaseToken = purchaseToken,
                    orderId = orderId,
                    purchaseTimeMillis = purchaseTimeMillis,
                )
            ) {
                PlayPurchaseSyncResult.Completed,
                PlayPurchaseSyncResult.Duplicate,
                -> Unit
                is PlayPurchaseSyncResult.Failed -> showError(result.message)
            }
        }
    }

    fun logout() {
        cancelFishingJobs()
        cancelTelegramJobs()
        viewModelScope.launch {
            repository.logout()
            _state.value = RiverKingUiState(
                loading = false,
                login = repository.lastLoginDraft(),
            )
        }
    }

    fun deleteAccount() {
        cancelFishingJobs()
        cancelTelegramJobs()
        val deletedMessage = if (state.value.me?.language == "ru") "Аккаунт удалён" else "Account deleted"
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                repository.deleteAccount()
                _state.value = RiverKingUiState(
                    loading = false,
                    login = repository.lastLoginDraft(),
                    error = deletedMessage,
                )
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun openSupport() = queueExternalUrl(BuildConfig.SUPPORT_URL)

    fun openPrivacyPolicy() = queueExternalUrl(BuildConfig.PRIVACY_POLICY_URL)

    fun openAccountDeletionHelp() = queueExternalUrl(BuildConfig.ACCOUNT_DELETION_URL)

    fun refreshAppUpdateStatus() {
        if (appUpdateJob?.isActive == true) return
        appUpdateJob = viewModelScope.launch {
            try {
                applyAppUpdate(repository.checkAppUpdate())
            } catch (_: Throwable) {
                // Update checks are best-effort during resume; API calls still enforce mandatory upgrades.
            }
        }
    }

    fun dismissAppUpdate() {
        val update = state.value.appUpdate ?: return
        if (update.isMandatory) return
        dismissedRecommendedUpdateCode = update.latestVersionCode
        _state.update { it.copy(appUpdate = null) }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    fun consumePendingExternalUrl() {
        _state.update { it.copy(pendingExternalUrl = null) }
    }

    fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun dismissCatchError() {
        _state.update { it.copy(selectedCatch = null, selectedCatchCard = null, catchLoading = false) }
    }

    fun isGoogleEnabled(): Boolean = BuildConfig.GOOGLE_AUTH_ENABLED

    fun isPlayFlavor(): Boolean = BuildConfig.DISTRIBUTION_CHANNEL == "play"

    fun currentAccessToken(): String? = repository.currentAccessToken()

    suspend fun downloadCatchCard(catchId: Long): ByteArray = repository.catchCard(catchId)

    private fun queueExternalUrl(url: String) {
        val target = url.trim().takeIf { it.isNotEmpty() } ?: return
        _state.update { it.copy(pendingExternalUrl = target) }
    }

    private suspend fun describeError(error: Throwable): String? {
        repository.appUpdateFrom(error)?.let { update ->
            applyAppUpdate(update)
            return null
        }
        return repository.describeError(error)
    }

    private fun applyAppUpdate(update: AppUpdateInfoDto) {
        if (!update.isUpdateAvailable) {
            _state.update { it.copy(appUpdate = null) }
            return
        }
        if (!update.isMandatory && dismissedRecommendedUpdateCode == update.latestVersionCode) {
            return
        }
        _state.update {
            it.copy(
                loading = false,
                working = false,
                profileRefreshing = false,
                appUpdate = update,
                error = null,
            )
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val rememberedLogin = repository.lastLoginDraft()
            val update = try {
                repository.checkAppUpdate()
            } catch (_: Throwable) {
                null
            }
            if (update?.isMandatory == true) {
                _state.value = RiverKingUiState(
                    loading = false,
                    login = rememberedLogin,
                    appUpdate = update,
                )
                return@launch
            }
            val me = try {
                repository.restoreProfile()
            } catch (error: Throwable) {
                repository.appUpdateFrom(error)?.let { requiredUpdate ->
                    _state.value = RiverKingUiState(
                        loading = false,
                        login = rememberedLogin,
                        appUpdate = requiredUpdate,
                    )
                    return@launch
                }
                null
            }
            val pendingTelegramLogin = if (me == null) {
                repository.pendingTelegramLogin()
            } else {
                repository.clearPendingTelegramLogin()
                null
            }
            val pendingTelegramLink = if (me != null) {
                repository.pendingTelegramLink()
            } else {
                repository.clearPendingTelegramLink()
                null
            }
            _state.value = RiverKingUiState(
                loading = false,
                me = me,
                authProvider = repository.currentAuthProvider(),
                login = rememberedLogin,
                nickname = me?.username.orEmpty(),
                telegramLoginPending = pendingTelegramLogin != null,
                telegramLinkPending = pendingTelegramLink != null,
                appUpdate = update?.takeIf { it.isUpdateAvailable },
                ratings = RatingsUiState(locationId = me?.locationId?.toString() ?: "all"),
            )
            if (me != null) {
                warmupPlayerSurfaces()
            }
            when {
                pendingTelegramLogin != null -> resumeTelegramLogin(pendingTelegramLogin)
                me != null && pendingTelegramLink != null -> resumeTelegramLink(pendingTelegramLink)
            }
        }
    }

    private fun onAuthenticated(me: MeResponseDto) {
        _state.update {
            RiverKingUiState(
                loading = false,
                me = me,
                authProvider = repository.currentAuthProvider(),
                nickname = me.username.orEmpty(),
                appUpdate = state.value.appUpdate,
                ratings = RatingsUiState(locationId = me.locationId.toString()),
                fishing = FishingUiState(autoCastEnabled = false),
            )
        }
        warmupPlayerSurfaces()
    }

    fun resumeTelegramLogin(sessionToken: String) {
        if (state.value.me != null) {
            repository.clearPendingTelegramLogin()
            return
        }
        repository.rememberPendingTelegramLogin(sessionToken)
        telegramLoginJob?.cancel()
        telegramLoginJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = false,
                    working = false,
                    telegramLoginPending = true,
                    error = null,
                )
            }
            awaitTelegramLogin(sessionToken)
        }
    }

    fun resumeTelegramLink(sessionToken: String) {
        if (state.value.me == null) {
            repository.clearPendingTelegramLink()
            return
        }
        repository.rememberPendingTelegramLink(sessionToken)
        telegramLinkJob?.cancel()
        telegramLinkJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = false,
                    working = false,
                    telegramLinkPending = true,
                    error = null,
                )
            }
            awaitTelegramLink(sessionToken)
        }
    }

    private suspend fun awaitTelegramLogin(sessionToken: String) {
        while (true) {
            val result = try {
                repository.pollTelegramLogin(sessionToken)
            } catch (error: Throwable) {
                if (repository.isTransientNetworkError(error)) {
                    delay(TELEGRAM_POLL_INTERVAL_MS)
                    continue
                }
                repository.clearPendingTelegramLogin()
                val message = describeError(error)
                _state.update {
                    it.copy(
                        telegramLoginPending = false,
                        working = false,
                        error = message,
                    )
                }
                return
            }
            when (result) {
                TelegramLoginPollResult.Pending -> delay(TELEGRAM_POLL_INTERVAL_MS)
                TelegramLoginPollResult.Expired -> {
                    repository.clearPendingTelegramLogin()
                    _state.update {
                        it.copy(
                            telegramLoginPending = false,
                            working = false,
                            error = "Telegram sign-in expired",
                        )
                    }
                    return
                }
                is TelegramLoginPollResult.Failed -> {
                    repository.clearPendingTelegramLogin()
                    _state.update {
                        it.copy(
                            telegramLoginPending = false,
                            working = false,
                            error = result.error,
                        )
                    }
                    return
                }
                is TelegramLoginPollResult.Authorized -> {
                    repository.clearPendingTelegramLogin()
                    onAuthenticated(result.me)
                    return
                }
            }
        }
    }

    private suspend fun awaitTelegramLink(sessionToken: String) {
        while (true) {
            val result = try {
                repository.pollTelegramLink(sessionToken)
            } catch (error: Throwable) {
                if (repository.isTransientNetworkError(error)) {
                    delay(TELEGRAM_POLL_INTERVAL_MS)
                    continue
                }
                repository.clearPendingTelegramLink()
                val message = describeError(error)
                _state.update {
                    it.copy(
                        telegramLinkPending = false,
                        error = message,
                    )
                }
                return
            }
            when (result) {
                TelegramLinkPollResult.Pending -> delay(TELEGRAM_POLL_INTERVAL_MS)
                TelegramLinkPollResult.Expired -> {
                    repository.clearPendingTelegramLink()
                    _state.update {
                        it.copy(
                            telegramLinkPending = false,
                            error = "Telegram link expired",
                        )
                    }
                    return
                }
                is TelegramLinkPollResult.Failed -> {
                    repository.clearPendingTelegramLink()
                    _state.update {
                        it.copy(
                            telegramLinkPending = false,
                            error = result.error,
                        )
                    }
                    return
                }
                is TelegramLinkPollResult.Completed -> {
                    repository.clearPendingTelegramLink()
                    applyProfileUpdate(result.me)
                    _state.update { it.copy(telegramLinkPending = false) }
                    return
                }
            }
        }
    }

    private fun applyProfileUpdate(me: MeResponseDto) {
        _state.update { current ->
            current.copy(
                me = me,
                authProvider = current.authProvider ?: repository.currentAuthProvider(),
                nickname = if (current.me == null || current.nickname == current.me.username.orEmpty()) {
                    me.username.orEmpty()
                } else {
                    current.nickname
                },
                profileRefreshing = false,
                fishing = current.fishing.copy(autoCastEnabled = current.fishing.autoCastEnabled && me.autoFish),
                ratings = current.ratings.copy(
                    locationId = normalizeSelectedLocation(current.ratings.locationId, me),
                ),
            )
        }
    }

    private fun normalizeSelectedLocation(currentLocationId: String, me: MeResponseDto): String {
        if (currentLocationId == "all") return currentLocationId
        if (me.locations.any { it.id.toString() == currentLocationId }) return currentLocationId
        return me.locationId.toString()
    }

    private fun warmupPlayerSurfaces() {
        loadGuide(force = true)
        loadShop(force = true)
        if (shouldExposeReferralPanel(state.value)) {
            loadReferrals(force = true)
        }
        loadClub(force = true)
        loadTournaments(force = true)
        loadRatings(force = true)
    }

    private fun shouldExposeReferralPanel(state: RiverKingUiState): Boolean {
        val me = state.me ?: return false
        val authProvider = state.authProvider
        return when (authProvider) {
            AuthRepository.AUTH_PROVIDER_TELEGRAM -> true
            AuthRepository.AUTH_PROVIDER_GOOGLE -> true
            AuthRepository.AUTH_PROVIDER_PASSWORD -> false
            null -> me.telegramLinked || me.authProviders.contains(AuthRepository.AUTH_PROVIDER_GOOGLE)
            else -> false
        }
    }

    private fun launchWaitCountdown(waitSeconds: Int) {
        waitJob?.cancel()
        waitJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val totalMillis = waitSeconds * 1_000L
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = (totalMillis - elapsed).coerceAtLeast(0L)
                _state.update { it.copy(fishing = it.fishing.copy(phaseTimeLeftMillis = remaining)) }
                if (remaining <= 0L) break
                delay(100L)
            }
            enterBitePhase()
        }
    }

    private fun enterBitePhase() {
        biteStartedAtMillis = System.currentTimeMillis()
        _state.update {
            it.copy(
                fishing = it.fishing.copy(
                    phase = FishingPhase.BITING,
                    phaseTimeLeftMillis = BITE_WINDOW_MILLIS,
                )
            )
        }
        biteJob?.cancel()
        biteJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - biteStartedAtMillis
                val remaining = (BITE_WINDOW_MILLIS - elapsed).coerceAtLeast(0L)
                _state.update { it.copy(fishing = it.fishing.copy(phaseTimeLeftMillis = remaining)) }
                if (remaining <= 0L) break
                delay(50L)
            }
            performHook(auto = true)
        }
    }

    private fun performHook(auto: Boolean) {
        if (state.value.fishing.phase != FishingPhase.BITING) return
        biteJob?.cancel()
        val wait = state.value.fishing.castWaitSeconds
        val reaction = if (auto) {
            FAIL_REACTION_SECONDS
        } else {
            ((System.currentTimeMillis() - biteStartedAtMillis).coerceAtLeast(0L) / 1_000.0)
        }
        viewModelScope.launch {
            _state.update { it.copy(fishing = it.fishing.copy(phase = FishingPhase.RESOLVING)) }
            try {
                val result = repository.hook(wait = wait, reaction = reaction)
                _state.update {
                    it.copy(
                        me = it.me?.copy(autoFish = result.autoFish),
                        fishing = it.fishing.copy(
                            autoCastEnabled = it.fishing.autoCastEnabled && result.autoFish,
                            lastEscape = false,
                        ),
                    )
                }
                if (!result.success) {
                    _state.update {
                        it.copy(
                            fishing = it.fishing.copy(
                                lastCast = null,
                                lastCatchWasNewFish = false,
                                lastEscape = true,
                            )
                        )
                    }
                    startCooldown(triggerAutoCast = false)
                    return@launch
                }
                hookReactionSeconds = reaction
                startTapChallenge(result)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(error = message) }
                startCooldown(triggerAutoCast = false)
            }
        }
    }

    private fun startTapChallenge(result: HookResultDto) {
        val challenge = result.challenge
        val tapGoal = challenge?.tapGoal?.coerceAtLeast(1) ?: TAP_CHALLENGE_GOAL
        val durationMillis = challenge?.durationMs?.coerceAtLeast(1_000)?.toLong() ?: TAP_CHALLENGE_MILLIS
        val intensity = challenge?.struggleIntensity?.coerceIn(0.0, 1.0) ?: 0.0
        tapJob?.cancel()
        _state.update {
            it.copy(
                fishing = it.fishing.copy(
                    phase = FishingPhase.TAP_CHALLENGE,
                    phaseTimeLeftMillis = durationMillis,
                    tapCount = 0,
                    tapGoal = tapGoal,
                    tapDurationMillis = durationMillis,
                    hookedFish = result.hookedFish,
                    struggleIntensity = intensity,
                )
            )
        }
        val startedAt = System.currentTimeMillis()
        tapJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = (durationMillis - elapsed).coerceAtLeast(0L)
                _state.update { it.copy(fishing = it.fishing.copy(phaseTimeLeftMillis = remaining)) }
                if (remaining <= 0L) break
                delay(50L)
            }
            finalizeCatch(success = false)
        }
    }

    private fun finalizeCatch(success: Boolean) {
        if (state.value.fishing.phase != FishingPhase.TAP_CHALLENGE) return
        tapJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(fishing = it.fishing.copy(phase = FishingPhase.RESOLVING)) }
            try {
                val previousCaughtFishIds = state.value.me?.caughtFishIds.orEmpty()
                val cast = repository.cast(
                    wait = state.value.fishing.castWaitSeconds,
                    reaction = hookReactionSeconds,
                    success = success,
                )
                val isNewFish = cast.catch?.fishId?.let { fishId -> fishId !in previousCaughtFishIds } == true
                val me = repository.refreshProfile()
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        fishing = it.fishing.copy(
                            lastCast = cast,
                            lastCatchWasNewFish = isNewFish,
                            lastEscape = false,
                        ),
                    )
                }
                if (cast.caught) {
                    refreshPostCatchProgress(
                        refreshQuests = cast.questProgressChanged || cast.questUpdates.isNotEmpty(),
                        refreshAchievements = cast.achievements.isNotEmpty(),
                    )
                }
                startCooldown(triggerAutoCast = cast.caught && me.autoFish && state.value.fishing.autoCastEnabled)
                val caught = cast.catch
                if (caught != null) {
                    delay(CATCH_DETAILS_OPEN_DELAY_MILLIS)
                    _state.update { current ->
                        if (current.fishing.lastCast?.catch?.id == caught.id && current.selectedCatch == null) {
                            current.copy(selectedCatch = caught)
                        } else {
                            current
                        }
                    }
                }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(error = message) }
                startCooldown(triggerAutoCast = false)
            }
        }
    }

    private fun refreshPostCatchProgress(refreshQuests: Boolean, refreshAchievements: Boolean) {
        if (!refreshQuests && !refreshAchievements) return
        postCatchProgressRefreshJob?.cancel()
        postCatchProgressRefreshJob = viewModelScope.launch {
            try {
                val (quests, achievements) = coroutineScope {
                    val quests = if (refreshQuests) async { repository.loadQuests() } else null
                    val achievements = if (refreshAchievements) async { repository.loadAchievements() } else null
                    quests?.await() to achievements?.await()
                }
                _state.update { current ->
                    current.copy(
                        guide = current.guide.copy(
                            quests = quests ?: current.guide.quests,
                            achievements = achievements ?: current.guide.achievements,
                        ),
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val message = describeError(error)
                _state.update { it.copy(error = message) }
            }
        }
    }

    private fun startCooldown(triggerAutoCast: Boolean) {
        waitJob?.cancel()
        biteJob?.cancel()
        tapJob?.cancel()
        cooldownJob?.cancel()
        _state.update {
            it.copy(
                fishing = it.fishing.copy(
                    phase = FishingPhase.COOLDOWN,
                    phaseTimeLeftMillis = CAST_READY_DELAY_MILLIS,
                    tapCount = 0,
                    tapGoal = TAP_CHALLENGE_GOAL,
                    tapDurationMillis = TAP_CHALLENGE_MILLIS,
                    hookedFish = null,
                    struggleIntensity = 0.0,
                    castSpot = null,
                )
            )
        }
        val startedAt = System.currentTimeMillis()
        cooldownJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = (CAST_READY_DELAY_MILLIS - elapsed).coerceAtLeast(0L)
                _state.update { it.copy(fishing = it.fishing.copy(phaseTimeLeftMillis = remaining)) }
                if (remaining <= 0L) break
                delay(50L)
            }
            _state.update {
                it.copy(
                    fishing = it.fishing.copy(
                        phase = FishingPhase.READY,
                        phaseTimeLeftMillis = 0L,
                        tapCount = 0,
                        tapGoal = TAP_CHALLENGE_GOAL,
                        tapDurationMillis = TAP_CHALLENGE_MILLIS,
                        hookedFish = null,
                        struggleIntensity = 0.0,
                    )
                )
            }
            if (triggerAutoCast && state.value.fishing.autoCastEnabled && state.value.me?.autoFish == true) {
                beginCast(auto = true)
            }
        }
    }

    private fun cancelFishingJobs() {
        waitJob?.cancel()
        biteJob?.cancel()
        tapJob?.cancel()
        cooldownJob?.cancel()
        waitJob = null
        biteJob = null
        tapJob = null
        cooldownJob = null
    }

    private fun cancelTelegramJobs() {
        telegramLoginJob?.cancel()
        telegramLinkJob?.cancel()
        telegramLoginJob = null
        telegramLinkJob = null
    }

    fun selectLocation(locationId: Long) {
        if (state.value.fishing.phase == FishingPhase.RESOLVING) return
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.changeLocation(locationId)
                applyProfileUpdate(me)
                _state.update {
                    it.copy(
                        working = false,
                        ratings = it.ratings.copy(loaded = false, locationId = me.locationId.toString()),
                    )
                }
                loadRatings(force = true)
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun selectLure(lureId: Long) {
        if (state.value.fishing.phase == FishingPhase.RESOLVING) return
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.changeLure(lureId)
                applyProfileUpdate(me)
                _state.update { it.copy(working = false) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }

    fun selectRod(rodId: Long) {
        if (state.value.fishing.phase == FishingPhase.RESOLVING) return
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = repository.changeRod(rodId)
                applyProfileUpdate(me)
                _state.update { it.copy(working = false) }
            } catch (error: Throwable) {
                val message = describeError(error)
                _state.update { it.copy(working = false, error = message) }
            }
        }
    }
}

private const val TAP_CHALLENGE_GOAL = 10
private const val TAP_CHALLENGE_MILLIS = 5_000L
private const val BITE_WINDOW_MILLIS = 5_000L
private const val CAST_READY_DELAY_MILLIS = 3_000L
private const val CAST_ANIMATION_DEFAULT_MILLIS = 560
private const val CATCH_DETAILS_OPEN_DELAY_MILLIS = 1_150L
private const val TELEGRAM_POLL_INTERVAL_MS = 2_000L
private const val FAIL_REACTION_SECONDS = 9.99
private val REGISTER_LOGIN_REGEX = Regex("^[a-z0-9._-]{3,32}$")

private fun randomFishingCastSpot(): FishingCastSpot =
    FishingCastSpot(
        xRoll = Random.nextFloat(),
        yRoll = Random.nextFloat(),
    )
