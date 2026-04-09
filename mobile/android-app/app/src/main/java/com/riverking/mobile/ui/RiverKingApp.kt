package com.riverking.mobile.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riverking.mobile.auth.GoogleSignInManager
import kotlinx.coroutines.launch

@Composable
fun RiverKingApp(
    viewModel: RiverKingViewModel,
    googleSignInManager: GoogleSignInManager,
    activity: ComponentActivity,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentMe = state.me
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.loading -> LoadingScreen()
                    currentMe == null -> AuthScreen(
                        state = state,
                        googleEnabled = viewModel.isGoogleEnabled(),
                        onLoginChange = viewModel::updateLogin,
                        onPasswordChange = viewModel::updatePassword,
                        onToggleMode = viewModel::toggleAuthMode,
                        onSubmit = viewModel::submitPasswordAuth,
                        onGoogleSignIn = {
                            scope.launch {
                                googleSignInManager.requestIdToken(activity)
                                    .onSuccess(viewModel::signInWithGoogle)
                                    .onFailure { error ->
                                        viewModel.showError(error.message ?: "Google sign-in failed")
                                    }
                            }
                        },
                    )
                    currentMe.needsNickname -> NicknameScreen(
                        nickname = state.nickname,
                        busy = state.working,
                        onNicknameChange = viewModel::updateNickname,
                        onSave = viewModel::saveNickname,
                    )
                    else -> MainShell(
                        state = state,
                        onLogout = viewModel::logout,
                        onRefreshProfile = viewModel::refreshProfile,
                        onClaimDaily = viewModel::claimDaily,
                        onQuickCast = viewModel::performQuickCast,
                        onSelectLocation = viewModel::selectLocation,
                        onSelectLure = viewModel::selectLure,
                        onSelectRod = viewModel::selectRod,
                        onLoadTournaments = viewModel::loadTournaments,
                        onClaimPrize = viewModel::claimPrize,
                        onLoadRatings = viewModel::loadRatings,
                        onLoadGuide = viewModel::loadGuide,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("RiverKing", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthScreen(
    state: RiverKingUiState,
    googleEnabled: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("RiverKing", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Android auth uses the shared backend. Mini App auth remains Telegram-only.")
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = state.login,
            onValueChange = onLoginChange,
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.working,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.working,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.authMode == AuthMode.LOGIN) "Sign in" else "Create account")
        }
        TextButton(onClick = onToggleMode, enabled = !state.working) {
            Text(if (state.authMode == AuthMode.LOGIN) "Need an account?" else "Already have an account?")
        }
        if (googleEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGoogleSignIn,
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in with Google")
            }
        }
    }
}

@Composable
private fun NicknameScreen(
    nickname: String,
    busy: Boolean,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Choose your nickname", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}
