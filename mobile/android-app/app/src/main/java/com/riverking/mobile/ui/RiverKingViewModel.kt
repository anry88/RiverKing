package com.riverking.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.AuthRepository
import com.riverking.mobile.auth.MeResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

data class RiverKingUiState(
    val loading: Boolean = true,
    val me: MeResponseDto? = null,
    val login: String = "",
    val password: String = "",
    val nickname: String = "",
    val authMode: AuthMode = AuthMode.LOGIN,
    val working: Boolean = false,
    val error: String? = null,
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
            val result = runCatching {
                when (state.value.authMode) {
                    AuthMode.LOGIN -> repository.loginPassword(login, password)
                    AuthMode.REGISTER -> repository.registerPassword(login, password)
                }
            }
            result.onSuccess { me ->
                _state.update {
                    it.copy(
                        me = me,
                        nickname = me.username.orEmpty(),
                        working = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(working = false, error = error.message ?: "Authentication failed") }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            runCatching { repository.loginGoogle(idToken) }
                .onSuccess { me ->
                    _state.update {
                        it.copy(
                            me = me,
                            nickname = me.username.orEmpty(),
                            working = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(working = false, error = error.message ?: "Google sign-in failed") }
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
            runCatching { repository.updateNickname(nickname) }
                .onSuccess { me ->
                    _state.update { it.copy(me = me, working = false, error = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(working = false, error = error.message ?: "Failed to save nickname") }
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
}
