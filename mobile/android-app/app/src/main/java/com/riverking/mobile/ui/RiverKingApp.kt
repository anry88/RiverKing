package com.riverking.mobile.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riverking.mobile.auth.GoogleSignInManager
import kotlinx.coroutines.launch

private enum class MainTab(val title: String) {
    FISHING("Fishing"),
    TOURNAMENTS("Tournaments"),
    RATINGS("Ratings"),
    GUIDE("Guide"),
}

@Composable
fun RiverKingApp(
    viewModel: RiverKingViewModel,
    googleSignInManager: GoogleSignInManager,
    activity: ComponentActivity,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                    state.me == null -> AuthScreen(
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
                    state.me.needsNickname -> NicknameScreen(
                        nickname = state.nickname,
                        busy = state.working,
                        onNicknameChange = viewModel::updateNickname,
                        onSave = viewModel::saveNickname,
                    )
                    else -> MainShell(
                        state = state,
                        onLogout = viewModel::logout,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    state: RiverKingUiState,
    onLogout: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FISHING) }
    val me = state.me ?: return

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RiverKing") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {},
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = me.username ?: "Unnamed angler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard("Coins", me.coins.toString(), Modifier.weight(1f))
                    StatCard("Today", "${me.todayWeight} kg", Modifier.weight(1f))
                }
            }
            item {
                StatCard(
                    title = selectedTab.title,
                    value = when (selectedTab) {
                        MainTab.FISHING -> "Use the shared /api/me, /api/cast and /api/hook flows here."
                        MainTab.TOURNAMENTS -> "Tournament surfaces will reuse the existing backend endpoints."
                        MainTab.RATINGS -> "Ratings will consume the same leaderboard contracts as the Mini App."
                        MainTab.GUIDE -> "Guide and achievements stay on the common API."
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
