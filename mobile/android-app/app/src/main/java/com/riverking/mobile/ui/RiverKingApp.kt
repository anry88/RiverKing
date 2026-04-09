package com.riverking.mobile.ui

import android.content.Intent
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riverking.mobile.BuildConfig
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.GoogleSignInManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RiverKingApp(
    viewModel: RiverKingViewModel,
    googleSignInManager: GoogleSignInManager,
    activity: ComponentActivity,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentMe = state.me
    val strings = rememberRiverStrings(currentMe?.language)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    RiverTheme {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.loading -> LoadingScreen(strings)
                    currentMe == null -> AuthScreen(
                        state = state,
                        strings = strings,
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
                        strings = strings,
                        nickname = state.nickname,
                        busy = state.working,
                        onNicknameChange = viewModel::updateNickname,
                        onSave = viewModel::saveNickname,
                    )
                    else -> MainShell(
                        state = state,
                        isPlayFlavor = viewModel.isPlayFlavor(),
                        onLogout = viewModel::logout,
                        onRefreshProfile = viewModel::refreshProfile,
                        onChangeLanguage = viewModel::changeLanguage,
                        onClaimDaily = viewModel::claimDaily,
                        onBeginCast = { viewModel.beginCast() },
                        onHookFish = viewModel::hookFish,
                        onTapChallenge = viewModel::registerTap,
                        onToggleAutoCast = viewModel::toggleAutoCast,
                        onSelectLocation = viewModel::selectLocation,
                        onSelectLure = viewModel::selectLure,
                        onSelectRod = viewModel::selectRod,
                        onOpenCatch = viewModel::openCatchDetails,
                        onDismissCatch = viewModel::dismissCatchDetails,
                        onShareCatch = { catch ->
                            scope.launch {
                                shareCatchCard(
                                    activity = activity,
                                    strings = strings,
                                    viewModel = viewModel,
                                    catch = catch,
                                )
                            }
                        },
                        onLoadTournaments = viewModel::loadTournaments,
                        onOpenTournament = viewModel::openTournament,
                        onCloseTournament = viewModel::closeTournamentDetails,
                        onClaimPrize = viewModel::claimPrize,
                        onSetRatingsMode = viewModel::setRatingsMode,
                        onSetRatingsPeriod = viewModel::setRatingsPeriod,
                        onSetRatingsOrder = viewModel::setRatingsOrder,
                        onSetRatingsLocation = viewModel::setRatingsLocation,
                        onSetRatingsFish = viewModel::setRatingsFish,
                        onLoadRatings = viewModel::loadRatings,
                        onLoadGuide = viewModel::loadGuide,
                        onClaimAchievement = viewModel::claimAchievement,
                        onDismissAchievementReward = viewModel::dismissAchievementReward,
                        onLoadClub = viewModel::loadClub,
                        onLoadClubChat = viewModel::loadClubChat,
                        onSearchClubs = viewModel::searchClubs,
                        onCreateClub = viewModel::createClub,
                        onJoinClub = viewModel::joinClub,
                        onUpdateClubInfo = viewModel::updateClubInfo,
                        onUpdateClubSettings = viewModel::updateClubSettings,
                        onLeaveClub = viewModel::leaveClub,
                        onClubMemberAction = viewModel::clubMemberAction,
                        onLoadShop = viewModel::loadShop,
                        onGenerateReferral = viewModel::generateReferral,
                        onClaimReferralRewards = viewModel::claimReferralRewards,
                        onBuyShopWithCoins = viewModel::buyShopWithCoins,
                        onPlayPurchase = { packId ->
                            if (viewModel.isPlayFlavor() && BuildConfig.DEBUG) {
                                val stamp = System.currentTimeMillis()
                                viewModel.completePlayPurchase(
                                    packId = packId,
                                    purchaseToken = "debug-$packId-$stamp",
                                    orderId = "debug-$packId-$stamp",
                                    purchaseTimeMillis = stamp,
                                )
                            } else {
                                viewModel.showError(strings.playPurchaseUnavailable)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(strings: RiverStrings) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(strings.appTitle, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    googleEnabled: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(strings.appTitle, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings.authSubtitle)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = state.login,
                onValueChange = onLoginChange,
                label = { Text(strings.login) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.working,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(strings.password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.working,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSubmit, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.authMode == AuthMode.LOGIN) strings.signIn else strings.createAccount)
            }
            TextButton(onClick = onToggleMode, enabled = !state.working) {
                Text(if (state.authMode == AuthMode.LOGIN) strings.needAccount else strings.alreadyHaveAccount)
            }
            if (googleEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onGoogleSignIn, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.signInGoogle)
                }
            }
        }
    }
}

@Composable
private fun NicknameScreen(
    strings: RiverStrings,
    nickname: String,
    busy: Boolean,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(strings.chooseNickname)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(strings.chooseNickname) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(strings.continueLabel)
            }
        }
    }
}

private suspend fun shareCatchCard(
    activity: ComponentActivity,
    strings: RiverStrings,
    viewModel: RiverKingViewModel,
    catch: CatchDto,
) {
    if (catch.id <= 0L) {
        viewModel.showError(strings.unavailable)
        return
    }
    runCatching {
        val bytes = viewModel.downloadCatchCard(catch.id)
        val dir = File(activity.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "catch-${catch.id}.png")
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        val text = buildString {
            append(catch.fish)
            append(" • ")
            append(String.format(java.util.Locale.US, "%.2f kg", catch.weight))
            append(" • ")
            append(catch.location)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, strings.shareCatch))
    }.onFailure {
        viewModel.showError(it.message ?: strings.unavailable)
    }
}
