package com.riverking.mobile.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.GoogleSignInManager
import com.riverking.mobile.auth.ReferralInfoDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    val latestStrings = rememberUpdatedState(strings)
    val latestViewModel = rememberUpdatedState(viewModel)
    val playBillingManager = remember(activity, scope) {
        if (viewModel.isPlayFlavor()) {
            PlayBillingManager(
                context = activity.applicationContext,
                scope = scope,
            )
        } else {
            null
        }
    }

    SideEffect {
        playBillingManager?.onNotice = { code ->
            latestViewModel.value.showError(latestStrings.value.playBillingMessage(code))
        }
        playBillingManager?.onSyncPurchase = { packId, purchaseToken, orderId, purchaseTimeMillis ->
            latestViewModel.value.syncPlayPurchase(
                packId = packId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTimeMillis = purchaseTimeMillis,
            )
        }
        playBillingManager?.accountIdProvider = {
            latestViewModel.value.state.value.me?.id?.toString()
        }
        playBillingManager?.isConsumableProduct = { productId ->
            latestViewModel.value.state.value.shop.categories
                .flatMap { it.packs }
                .firstOrNull { it.id == productId }
                ?.rodCode == null
        }
    }

    DisposableEffect(activity, playBillingManager) {
        val manager = playBillingManager
        if (manager == null) {
            onDispose { }
        } else {
            manager.start()
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    manager.syncPurchases()
                }
            }
            activity.lifecycle.addObserver(observer)
            onDispose {
                activity.lifecycle.removeObserver(observer)
                manager.stop()
            }
        }
    }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    LaunchedEffect(state.pendingExternalUrl) {
        val url = state.pendingExternalUrl ?: return@LaunchedEffect
        runCatching { openExternalUrl(activity, url) }
            .onFailure { error ->
                viewModel.showError(error.message ?: strings.unavailable)
            }
        viewModel.consumePendingExternalUrl()
    }

    RiverTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                        onTelegramSignIn = viewModel::startTelegramLogin,
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
                                    cachedCard = state.selectedCatchCard?.takeIf { state.selectedCatch?.id == catch.id },
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
                        onStartTelegramLink = viewModel::startTelegramLink,
                        onLoadShop = viewModel::loadShop,
                        onGenerateReferral = viewModel::generateReferral,
                        onShareReferral = { referral ->
                            shareReferralInvite(
                                activity = activity,
                                referral = referral,
                            )
                        },
                        onClaimReferralRewards = viewModel::claimReferralRewards,
                        onBuyShopWithCoins = viewModel::buyShopWithCoins,
                        onPlayPurchase = { packId ->
                            playBillingManager?.launchPurchase(activity, packId)
                                ?: viewModel.showError(strings.playPurchaseUnavailable)
                        },
                        onUpdateNickname = viewModel::updateNickname,
                        onSaveNickname = viewModel::saveNickname,
                        onLoadCatchStats = viewModel::loadCatchStats,
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
@OptIn(ExperimentalFoundationApi::class)
private fun AuthScreen(
    state: RiverKingUiState,
    strings: RiverStrings,
    googleEnabled: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onTelegramSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    val authBusy = state.working || state.telegramLoginPending
    val scope = rememberCoroutineScope()
    val loginBringIntoViewRequester = remember { BringIntoViewRequester() }
    val passwordBringIntoViewRequester = remember { BringIntoViewRequester() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.authSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.authMode == AuthMode.REGISTER) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.registerRequirements,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onTelegramSignIn, enabled = !authBusy, modifier = Modifier.fillMaxWidth()) {
                Text(strings.signInTelegram)
            }
            if (state.telegramLoginPending) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.telegramLoginPending,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = state.login,
                onValueChange = onLoginChange,
                label = { Text(strings.login) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(loginBringIntoViewRequester, scope),
                enabled = !authBusy,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                colors = riverTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(strings.password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(passwordBringIntoViewRequester, scope),
                enabled = !authBusy,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !authBusy,
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                strings.hidePassword
                            } else {
                                strings.showPassword
                            },
                        )
                    }
                },
                colors = riverTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onSubmit, enabled = !authBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.authMode == AuthMode.LOGIN) strings.signIn else strings.createAccount)
            }
            TextButton(onClick = onToggleMode, enabled = !authBusy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (state.authMode == AuthMode.LOGIN) strings.needAccount else strings.alreadyHaveAccount)
            }
            if (googleEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onGoogleSignIn, enabled = !authBusy, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.signInGoogle)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NicknameScreen(
    strings: RiverStrings,
    nickname: String,
    busy: Boolean,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val nicknameBringIntoViewRequester = remember { BringIntoViewRequester() }
    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = strings.chooseNickname,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(strings.chooseNickname) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(nicknameBringIntoViewRequester, scope),
                enabled = !busy,
                singleLine = true,
                colors = riverTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(strings.continueLabel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AuthBackdrop(
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RiverBackdropBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun riverTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.bringIntoViewOnFocus(
    requester: BringIntoViewRequester,
    scope: CoroutineScope,
): Modifier = this
    .bringIntoViewRequester(requester)
    .onFocusChanged { focusState ->
        if (focusState.isFocused) {
            scope.launch {
                delay(150)
                requester.bringIntoView()
            }
        }
    }

private suspend fun shareCatchCard(
    activity: ComponentActivity,
    strings: RiverStrings,
    viewModel: RiverKingViewModel,
    catch: CatchDto,
    cachedCard: ByteArray? = null,
) {
    if (catch.id <= 0L) {
        viewModel.showError(strings.unavailable)
        return
    }
    runCatching {
        val bytes = cachedCard ?: viewModel.downloadCatchCard(catch.id)
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
            catch.user?.let {
                append('\n')
                append(it)
            }
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

private fun shareReferralInvite(
    activity: ComponentActivity,
    referral: ReferralInfoDto,
) {
    val body = referral.androidShareText.ifBlank { referral.link }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    activity.startActivity(Intent.createChooser(sendIntent, null))
}

private fun openExternalUrl(
    activity: ComponentActivity,
    url: String,
) {
    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val telegramIntent = telegramIntent(url)
    if (telegramIntent != null) {
        runCatching { activity.startActivity(telegramIntent) }
            .recoverCatching { activity.startActivity(fallbackIntent) }
            .getOrThrow()
        return
    }
    activity.startActivity(fallbackIntent)
}

private fun telegramIntent(url: String): Intent? {
    val sourceUri = Uri.parse(url)
    val host = sourceUri.host?.lowercase() ?: return null
    if (host !in setOf("t.me", "www.t.me", "telegram.me", "www.telegram.me")) return null
    val username = sourceUri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val builder = Uri.parse("tg://resolve").buildUpon()
        .appendQueryParameter("domain", username)
    sourceUri.getQueryParameter("start")?.let { builder.appendQueryParameter("start", it) }
    sourceUri.getQueryParameter("startapp")?.let { builder.appendQueryParameter("startapp", it) }
    sourceUri.getQueryParameter("startattach")?.let { builder.appendQueryParameter("startattach", it) }
    return Intent(Intent.ACTION_VIEW, builder.build())
}
