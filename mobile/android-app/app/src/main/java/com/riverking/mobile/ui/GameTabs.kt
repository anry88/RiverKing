package com.riverking.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riverking.mobile.auth.AchievementDto
import com.riverking.mobile.auth.CatchDto
import com.riverking.mobile.auth.CurrentTournamentDto
import com.riverking.mobile.auth.GuideLocationDto
import com.riverking.mobile.auth.GuideRodDto
import com.riverking.mobile.auth.PrizeDto
import com.riverking.mobile.auth.QuestDto
import com.riverking.mobile.auth.RecentDto
import com.riverking.mobile.auth.TournamentDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MainTab(val title: String) {
    FISHING("Fishing"),
    TOURNAMENTS("Tournaments"),
    RATINGS("Ratings"),
    GUIDE("Guide"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    state: RiverKingUiState,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit,
    onClaimDaily: () -> Unit,
    onQuickCast: () -> Unit,
    onSelectLocation: (Long) -> Unit,
    onSelectLure: (Long) -> Unit,
    onSelectRod: (Long) -> Unit,
    onLoadTournaments: (Boolean) -> Unit,
    onClaimPrize: (Long) -> Unit,
    onLoadRatings: (RatingsScope, Boolean) -> Unit,
    onLoadGuide: (Boolean) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.FISHING) }
    val me = state.me ?: return

    LaunchedEffect(selectedTab, me.id) {
        when (selectedTab) {
            MainTab.FISHING -> Unit
            MainTab.TOURNAMENTS -> onLoadTournaments(false)
            MainTab.RATINGS -> onLoadRatings(state.ratings.scope, false)
            MainTab.GUIDE -> onLoadGuide(false)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(me.username ?: "RiverKing") },
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
                        icon = { Text(tab.title.first().toString()) },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.FISHING -> FishingTab(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRefreshProfile = onRefreshProfile,
                onClaimDaily = onClaimDaily,
                onQuickCast = onQuickCast,
                onSelectLocation = onSelectLocation,
                onSelectLure = onSelectLure,
                onSelectRod = onSelectRod,
            )
            MainTab.TOURNAMENTS -> TournamentsTab(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRefresh = { onLoadTournaments(true) },
                onClaimPrize = onClaimPrize,
            )
            MainTab.RATINGS -> RatingsTab(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onLoadRatings = onLoadRatings,
            )
            MainTab.GUIDE -> GuideTab(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRefresh = { onLoadGuide(true) },
            )
        }
    }
}

@Composable
private fun FishingTab(
    state: RiverKingUiState,
    modifier: Modifier = Modifier,
    onRefreshProfile: () -> Unit,
    onClaimDaily: () -> Unit,
    onQuickCast: () -> Unit,
    onSelectLocation: (Long) -> Unit,
    onSelectLure: (Long) -> Unit,
    onSelectRod: (Long) -> Unit,
) {
    val me = state.me ?: return
    val busy = state.fishing.busy || state.profileRefreshing

    LazyColumn(
        modifier = modifier.padding(16.dp),
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
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Coins", me.coins.toString(), Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Today", me.todayWeight.asKg(), Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Total", me.totalWeight.asKg(), Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Streak", "${me.dailyStreak} days", Modifier.fillMaxWidth())
                }
            }
        }
        item {
            SectionCard("Cast") {
                Text("Current water: ${me.locations.firstOrNull { it.id == me.locationId }?.name ?: "Unknown"}")
                Text("Auto fish: ${if (me.autoFish) "enabled" else "off"}")
                Spacer12()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onQuickCast, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(if (state.fishing.busy) "Casting..." else "Quick cast")
                    }
                    OutlinedButton(onClick = onRefreshProfile, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text("Refresh")
                    }
                }
                Spacer12()
                OutlinedButton(
                    onClick = onClaimDaily,
                    enabled = !busy && me.dailyAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (me.dailyAvailable) "Claim daily bait" else "Daily reward already claimed")
                }
            }
        }
        if (state.fishing.lastCast != null || state.fishing.lastStart != null) {
            item {
                SectionCard("Last result") {
                    state.fishing.lastCast?.let { cast ->
                        if (cast.caught && cast.catch != null) {
                            Text("${cast.catch.fish} • ${cast.catch.weight.asKg()}", fontWeight = FontWeight.SemiBold)
                            Text("${cast.catch.location} • ${cast.catch.rarity}")
                        } else {
                            Text("The fish escaped this time.")
                        }
                        if (cast.coins > 0) {
                            Text("Coins earned: ${cast.coins}")
                        }
                        if (cast.unlockedLocations.isNotEmpty()) {
                            Text("Unlocked waters: ${cast.unlockedLocations.joinToString()}")
                        }
                        if (cast.unlockedRods.isNotEmpty()) {
                            Text("Unlocked rods: ${cast.unlockedRods.joinToString()}")
                        }
                    }
                    state.fishing.lastStart?.recommendedRodName?.let { rodName ->
                        Spacer12()
                        Text("Recommended rod: $rodName")
                    }
                    state.fishing.lastStart?.newLureName?.let { lureName ->
                        Text("Backend switched bait to: $lureName")
                    }
                }
            }
        }
        item {
            SectionCard("Locations") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(me.locations, key = { it.id }) { location ->
                        FilterChip(
                            selected = me.locationId == location.id,
                            onClick = { onSelectLocation(location.id) },
                            enabled = !busy && location.unlocked,
                            label = {
                                Text(
                                    if (location.unlocked) location.name else "${location.name} • ${location.unlockKg.asKg()}",
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            SectionCard("Rods") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(me.rods, key = { it.id }) { rod ->
                        FilterChip(
                            selected = me.currentRodId == rod.id,
                            onClick = { onSelectRod(rod.id) },
                            enabled = !busy && rod.unlocked,
                            label = {
                                Text(
                                    if (rod.unlocked) rod.name else "${rod.name} • unlock ${rod.unlockKg.asKg()}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            SectionCard("Baits") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(me.lures, key = { it.id }) { lure ->
                        FilterChip(
                            selected = me.currentLureId == lure.id,
                            onClick = { onSelectLure(lure.id) },
                            enabled = !busy && lure.qty > 0,
                            label = {
                                Text(
                                    "${lure.displayName} • x${lure.qty}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
        if (me.recent.isNotEmpty()) {
            item {
                SectionCard("Recent catches") {
                    me.recent.forEachIndexed { index, recent ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        RecentCatchRow(recent)
                    }
                }
            }
        }
        if (me.dailyRewards.isNotEmpty()) {
            item {
                SectionCard("Daily bait plan") {
                    me.dailyRewards.forEachIndexed { index, rewards ->
                        Text("Day ${index + 1}", fontWeight = FontWeight.SemiBold)
                        Text(rewards.joinToString { "${it.name} x${it.qty}" })
                        if (index != me.dailyRewards.lastIndex) {
                            Spacer12()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentsTab(
    state: RiverKingUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onClaimPrize: (Long) -> Unit,
) {
    val data = state.tournaments
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Live", if (data.current == null) "No" else "Yes", Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    StatCard("Pending prizes", data.prizes.size.toString(), Modifier.fillMaxWidth())
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !data.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (data.loading) "Refreshing..." else "Refresh tournaments")
            }
        }
        item {
            SectionCard("Current tournament") {
                TournamentCurrentBlock(data.current)
            }
        }
        if (data.prizes.isNotEmpty()) {
            item {
                SectionCard("Unclaimed prizes") {
                    data.prizes.forEachIndexed { index, prize ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        PrizeRow(
                            prize = prize,
                            enabled = !data.loading,
                            onClaim = { onClaimPrize(prize.id) },
                        )
                    }
                }
            }
        }
        if (data.upcoming.isNotEmpty()) {
            item {
                SectionCard("Upcoming") {
                    data.upcoming.forEachIndexed { index, tournament ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        TournamentSummaryRow(tournament)
                    }
                }
            }
        }
        if (data.past.isNotEmpty()) {
            item {
                SectionCard("Recent finals") {
                    data.past.forEachIndexed { index, tournament ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        TournamentSummaryRow(tournament)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingsTab(
    state: RiverKingUiState,
    modifier: Modifier = Modifier,
    onLoadRatings: (RatingsScope, Boolean) -> Unit,
) {
    val data = state.ratings
    val me = state.me
    val currentLocationName = me?.locations?.firstOrNull { it.id == me.locationId }?.name ?: "current water"
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Leaderboard scope") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = data.scope == RatingsScope.ALL,
                        onClick = { onLoadRatings(RatingsScope.ALL, true) },
                        label = { Text("All waters") },
                    )
                    FilterChip(
                        selected = data.scope == RatingsScope.CURRENT,
                        onClick = { onLoadRatings(RatingsScope.CURRENT, true) },
                        label = { Text(currentLocationName) },
                    )
                }
                Spacer12()
                OutlinedButton(
                    onClick = { onLoadRatings(data.scope, true) },
                    enabled = !data.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (data.loading) "Refreshing..." else "Refresh ratings")
                }
            }
        }
        item {
            SectionCard("Your best catches") {
                CatchListBlock(
                    catches = data.personal,
                    emptyLabel = if (data.loaded) "No entries yet." else "Open this tab to load your results.",
                )
            }
        }
        item {
            SectionCard("Global leaders") {
                CatchListBlock(
                    catches = data.global,
                    emptyLabel = if (data.loaded) "No global entries yet." else "Open this tab to load the leaderboard.",
                )
            }
        }
    }
}

@Composable
private fun GuideTab(
    state: RiverKingUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    val data = state.guide
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !data.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (data.loading) "Refreshing..." else "Refresh guide")
            }
        }
        data.quests?.let { quests ->
            item {
                SectionCard("Daily quests") {
                    QuestBlock(quests.daily)
                }
            }
            item {
                SectionCard("Weekly quests") {
                    QuestBlock(quests.weekly)
                }
            }
        }
        if (data.achievements.isNotEmpty()) {
            item {
                SectionCard("Achievements") {
                    data.achievements.forEachIndexed { index, achievement ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        AchievementRow(achievement)
                    }
                }
            }
        }
        data.guide?.let { guide ->
            if (guide.locations.isNotEmpty()) {
                item {
                    SectionCard("Waters") {
                        guide.locations.forEachIndexed { index, location ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            GuideLocationRow(location)
                        }
                    }
                }
            }
            if (guide.rods.isNotEmpty()) {
                item {
                    SectionCard("Rod roadmap") {
                        guide.rods.forEachIndexed { index, rod ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            GuideRodRow(rod)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCurrentBlock(current: CurrentTournamentDto?) {
    if (current == null) {
        Text("No tournament is running right now.")
        return
    }
    TournamentSummaryRow(current.tournament)
    if (current.mine != null) {
        Spacer12()
        Text("Your rank: #${current.mine.rank} • ${current.mine.value}")
    }
    if (current.leaderboard.isNotEmpty()) {
        Spacer12()
        current.leaderboard.take(5).forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            Text("#${entry.rank} • ${entry.user ?: "Unknown"} • ${entry.value}")
            entry.fish?.let { Text(it) }
        }
    }
}

@Composable
private fun TournamentSummaryRow(tournament: TournamentDto) {
    Text(tournament.name, fontWeight = FontWeight.SemiBold)
    Text("${formatEpoch(tournament.startTime)} → ${formatEpoch(tournament.endTime)}")
    val descriptors = listOfNotNull(
        tournament.location,
        tournament.fish,
        tournament.fishRarity,
        "metric: ${tournament.metric}",
    )
    Text(descriptors.joinToString(" • "))
}

@Composable
private fun PrizeRow(
    prize: PrizeDto,
    enabled: Boolean,
    onClaim: () -> Unit,
) {
    Text(
        buildString {
            append("#")
            append(prize.rank)
            append(" • ")
            append(prize.source.replaceFirstChar { it.uppercase() })
        },
        fontWeight = FontWeight.SemiBold,
    )
    Text(prizeLabel(prize))
    Spacer12()
    OutlinedButton(onClick = onClaim, enabled = enabled) {
        Text("Claim")
    }
}

@Composable
private fun CatchListBlock(
    catches: List<CatchDto>,
    emptyLabel: String,
) {
    if (catches.isEmpty()) {
        Text(emptyLabel)
        return
    }
    catches.take(10).forEachIndexed { index, catch ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        Text("#${catch.rank ?: (index + 1)} • ${catch.fish}", fontWeight = FontWeight.SemiBold)
        Text("${catch.weight.asKg()} • ${catch.location}")
        catch.user?.let { Text(it) }
    }
}

@Composable
private fun QuestBlock(quests: List<QuestDto>) {
    if (quests.isEmpty()) {
        Text("No quests available.")
        return
    }
    quests.forEachIndexed { index, quest ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        Text(quest.name, fontWeight = FontWeight.SemiBold)
        Text(quest.description)
        Text("Progress: ${quest.progress}/${quest.target} • Reward: ${quest.rewardCoins} coins")
    }
}

@Composable
private fun AchievementRow(achievement: AchievementDto) {
    Text(achievement.name, fontWeight = FontWeight.SemiBold)
    Text(achievement.description)
    Text("Progress: ${achievement.progressLabel}/${achievement.targetLabel}")
    Text(
        if (achievement.claimable) "Claimable now" else "Level: ${achievement.level}",
        color = if (achievement.claimable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GuideLocationRow(location: GuideLocationDto) {
    Text(location.name, fontWeight = FontWeight.SemiBold)
    Text("Fish: ${location.fish.joinToString { it.name }}")
    Text("Best lures: ${location.lures.joinToString()}")
}

@Composable
private fun GuideRodRow(rod: GuideRodDto) {
    Text(rod.name, fontWeight = FontWeight.SemiBold)
    Text("Unlock: ${rod.unlockKg.asKg()}")
    val bonuses = listOfNotNull(
        rod.bonusWater?.let { "water: $it" },
        rod.bonusPredator?.let { if (it) "predator bonus" else "peaceful bonus" },
    )
    if (bonuses.isNotEmpty()) {
        Text(bonuses.joinToString(" • "))
    }
}

@Composable
private fun RecentCatchRow(recent: RecentDto) {
    Text(recent.fish, fontWeight = FontWeight.SemiBold)
    Text("${recent.weight.asKg()} • ${recent.location}")
    Text("${recent.rarity} • ${recent.at}")
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer12()
                content()
            },
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Spacer12() {
    Spacer(modifier = Modifier.height(12.dp))
}

private fun Double.asKg(): String = String.format(Locale.US, "%.2f kg", this)

private fun formatEpoch(seconds: Long): String =
    Instant.ofEpochSecond(seconds)
        .atZone(ZoneId.of("Europe/Belgrade"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

private fun prizeLabel(prize: PrizeDto): String = when {
    prize.coins != null -> "${prize.coins} coins"
    prize.packageId.equals("coins", ignoreCase = true) -> "${prize.qty} coins"
    else -> "${prize.qty} × ${prize.packageId}"
}
