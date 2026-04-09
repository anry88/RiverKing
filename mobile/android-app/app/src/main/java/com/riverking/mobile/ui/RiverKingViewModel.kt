package com.riverking.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.CastResultDto
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.CurrentTournamentDto
import com.riverking.mobile.auth.GuideDto
import com.riverking.mobile.auth.MeResponseDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.QuestListDto
import com.riverking.mobile.auth.StartCastResultDto
import com.riverking.mobile.auth.TournamentDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

enum class RatingsScope {
    ALL,
    CURRENT,
}

data class FishingUiState(
    val busy: Boolean = false,
    val lastStart: StartCastResultDto? = null,
    val lastCast: CastResultDto? = null,
)

data class TournamentsUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val current: CurrentTournamentDto? = null,
    val upcoming: List<TournamentDto> = emptyList(),
    val past: List<TournamentDto> = emptyList(),
    val prizes: List<PrizeDto> = emptyList(),
)

data class RatingsUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val scope: RatingsScope = RatingsScope.ALL,
    val personal: List<CatchDto> = emptyList(),
    val global: List<CatchDto> = emptyList(),
)

data class GuideUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val guide: GuideDto? = null,
    val achievements: List<AchievementDto> = emptyList(),
    val quests: QuestListDto? = null,
)

data class RiverKingUiState(
    val loading: Boolean = true,
    val me: MeResponseDto? = null,
    val login: String = "",
    val password: String = "",
    val nickname: String = "",
    val authMode: AuthMode = AuthMode.LOGIN,
    val working: Boolean = false,
    val profileRefreshing: Boolean = false,
    val error: String? = null,
    val fishing: FishingUiState = FishingUiState(),
    val tournaments: TournamentsUiState = TournamentsUiState(),
    val ratings: RatingsUiState = RatingsUiState(),
    val guide: GuideUiState = GuideUiState(),
)

class RiverKingViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RiverKingUiState())
    val state: StateFlow<RiverKingUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun updateLogin(value: String) {
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

        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                val me = when (state.value.authMode) {
                    AuthMode.LOGIN -> repository.loginPassword(login, password)
                    AuthMode.REGISTER -> repository.registerPassword(login, password)
                }
                onAuthenticated(me)
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        working = false,
                        error = repository.describeError(error),
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
                _state.update {
                    it.copy(
                        working = false,
                        error = repository.describeError(error),
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
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        working = false,
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        working = false,
                        error = repository.describeError(error),
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
                _state.update {
                    it.copy(
                        profileRefreshing = false,
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun claimDaily() {
        if (state.value.fishing.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    fishing = it.fishing.copy(busy = true),
                    error = null,
                )
            }
            try {
                val me = repository.claimDaily()
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        fishing = current.fishing.copy(busy = false),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    current.copy(
                        fishing = current.fishing.copy(busy = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun performQuickCast() {
        if (state.value.fishing.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    fishing = it.fishing.copy(busy = true),
                    error = null,
                )
            }
            try {
                val result = repository.performQuickCast()
                _state.update { current ->
                    current.copy(
                        me = result.me,
                        nickname = result.me.username.orEmpty(),
                        fishing = current.fishing.copy(
                            busy = false,
                            lastStart = result.start,
                            lastCast = result.cast,
                        ),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    current.copy(
                        fishing = current.fishing.copy(busy = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun selectLocation(locationId: Long) {
        if (state.value.fishing.busy) return
        viewModelScope.launch {
            _state.update { it.copy(fishing = it.fishing.copy(busy = true), error = null) }
            try {
                val me = repository.changeLocation(locationId)
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        fishing = current.fishing.copy(busy = false),
                        ratings = current.ratings.takeUnless { it.scope == RatingsScope.CURRENT }
                            ?: current.ratings.copy(loaded = false),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    current.copy(
                        fishing = current.fishing.copy(busy = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun selectLure(lureId: Long) {
        if (state.value.fishing.busy) return
        viewModelScope.launch {
            _state.update { it.copy(fishing = it.fishing.copy(busy = true), error = null) }
            try {
                val me = repository.changeLure(lureId)
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        fishing = current.fishing.copy(busy = false),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    current.copy(
                        fishing = current.fishing.copy(busy = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun selectRod(rodId: Long) {
        if (state.value.fishing.busy) return
        viewModelScope.launch {
            _state.update { it.copy(fishing = it.fishing.copy(busy = true), error = null) }
            try {
                val me = repository.changeRod(rodId)
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        fishing = current.fishing.copy(busy = false),
                        error = null,
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    current.copy(
                        fishing = current.fishing.copy(busy = false),
                        error = repository.describeError(error),
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
                    )
                }
                _state.update { it.copy(tournaments = data, error = null) }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(loading = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun claimPrize(prizeId: Long) {
        if (state.value.tournaments.loading) return
        viewModelScope.launch {
            _state.update { it.copy(tournaments = it.tournaments.copy(loading = true), error = null) }
            try {
                val me = repository.claimPrize(prizeId)
                _state.update { current ->
                    current.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        tournaments = current.tournaments.copy(loaded = false, loading = false),
                        error = null,
                    )
                }
                loadTournaments(force = true)
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        tournaments = it.tournaments.copy(loading = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun loadRatings(scope: RatingsScope = state.value.ratings.scope, force: Boolean = false) {
        val me = state.value.me ?: return
        if (state.value.ratings.loading) return
        if (!force && state.value.ratings.loaded && state.value.ratings.scope == scope) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    ratings = it.ratings.copy(
                        loading = true,
                        scope = scope,
                    ),
                    error = null,
                )
            }
            val locationId = when (scope) {
                RatingsScope.ALL -> "all"
                RatingsScope.CURRENT -> me.locationId.toString()
            }
            try {
                val data = coroutineScope {
                    val personal = async { repository.loadPersonalLocationRatings(locationId) }
                    val global = async { repository.loadGlobalLocationRatings(locationId) }
                    RatingsUiState(
                        loaded = true,
                        loading = false,
                        scope = scope,
                        personal = personal.await(),
                        global = global.await(),
                    )
                }
                _state.update { it.copy(ratings = data, error = null) }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        ratings = it.ratings.copy(loading = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
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
                _state.update { it.copy(guide = data, error = null) }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        guide = it.guide.copy(loading = false),
                        error = repository.describeError(error),
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = RiverKingUiState(loading = false)
        }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }

    fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun isGoogleEnabled(): Boolean = BuildConfig.GOOGLE_AUTH_ENABLED

    private fun bootstrap() {
        viewModelScope.launch {
            val me = repository.restoreProfile()
            _state.value = RiverKingUiState(
                loading = false,
                me = me,
                nickname = me?.username.orEmpty(),
            )
        }
    }

    private fun onAuthenticated(me: MeResponseDto) {
        _state.update {
            RiverKingUiState(
                loading = false,
                me = me,
                nickname = me.username.orEmpty(),
            )
        }
    }

    private fun applyProfileUpdate(me: MeResponseDto) {
        _state.update { current ->
            current.copy(
                me = me,
                nickname = if (current.me == null || current.nickname == current.me.username.orEmpty()) {
                    me.username.orEmpty()
                } else {
                    current.nickname
                },
                profileRefreshing = false,
                error = null,
            )
        }
    }
}
