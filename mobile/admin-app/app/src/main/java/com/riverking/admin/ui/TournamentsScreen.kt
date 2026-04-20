package com.riverking.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.TournamentDTO
import com.riverking.admin.network.TournamentReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class PrizeSpec(
    val pack: String = "",
    val qty: Int = 1,
    val coins: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreen(apiClient: AdminApiClient, onBack: () -> Unit) {
    var tournaments by remember { mutableStateOf<List<TournamentDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadTournaments() {
        scope.launch {
            try {
                tournaments = apiClient.getTournaments()
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadTournaments() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournaments") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().riverBackdrop().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(tournaments) { t ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = t.nameRu, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Metric: ${t.metric} | Places: ${t.prizePlaces}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteTournament(t.id)
                                            loadTournaments()
                                        } catch (e: Exception) { }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTournamentDialog(
            apiClient = apiClient,
            onDismiss = { showCreateDialog = false },
            onCreated = { loadTournaments(); showCreateDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentDialog(
    apiClient: AdminApiClient,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var nameRu by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf("total_weight") }
    var fish by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    // Times
    val now = Instant.now().toEpochMilli()
    var startTimeStr by remember { mutableStateOf(now.toString()) }
    var endTimeStr by remember { mutableStateOf((now + 86400000).toString()) }

    var prizePlacesStr by remember { mutableStateOf("3") }
    val prizePlaces = prizePlacesStr.toIntOrNull() ?: 0

    // Prizes
    var prizes by remember { mutableStateOf(List(prizePlaces) { PrizeSpec(pack = "coins", qty = 100) }) }

    // Re-sync prizes list when places count changes
    LaunchedEffect(prizePlaces) {
        if (prizePlaces > prizes.size) {
            prizes = prizes + List(prizePlaces - prizes.size) { PrizeSpec(pack = "coins", qty = 100) }
        } else if (prizePlaces < prizes.size && prizePlaces >= 0) {
            prizes = prizes.take(prizePlaces)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = { Text("Create Tournament") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nameRu, onValueChange = { nameRu = it }, label = { Text("Name RU") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name EN") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = metric, onValueChange = { metric = it }, label = { Text("Metric (total_weight/count/largest/smallest)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fish, onValueChange = { fish = it }, label = { Text("Fish (optional)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (optional)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startTimeStr, onValueChange = { startTimeStr = it }, label = { Text("Start Time (Millis)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endTimeStr, onValueChange = { endTimeStr = it }, label = { Text("End Time (Millis)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                
                OutlinedTextField(
                    value = prizePlacesStr,
                    onValueChange = { prizePlacesStr = it },
                    label = { Text("Prize Places") },
                    colors = riverTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Prizes configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                prizes.forEachIndexed { index, prize ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Place ${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = prize.pack,
                                onValueChange = { newPack ->
                                    val newPrizes = prizes.toMutableList()
                                    newPrizes[index] = prize.copy(pack = newPack)
                                    prizes = newPrizes
                                },
                                label = { Text("Prize Pack ID") },
                                colors = riverTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Quantity:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val newQty = (prize.qty - 1).coerceAtLeast(1)
                                    val newPrizes = prizes.toMutableList()
                                    newPrizes[index] = prize.copy(qty = newQty)
                                    prizes = newPrizes
                                }) { Icon(Icons.Default.Add, "Decrease") } // Ideally use Remove icon
                                Text("${prize.qty}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = {
                                    val newPrizes = prizes.toMutableList()
                                    newPrizes[index] = prize.copy(qty = prize.qty + 1)
                                    prizes = newPrizes
                                }) { Icon(Icons.Default.Add, "Increase") }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val newPrizes = prizes.toMutableList()
                                    newPrizes[index] = prize.copy(qty = prize.qty + 10)
                                    prizes = newPrizes
                                }) { Text("+10") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    try {
                        val req = TournamentReq(
                            nameRu = nameRu,
                            nameEn = nameEn,
                            startTime = startTimeStr.toLongOrNull() ?: 0L,
                            endTime = endTimeStr.toLongOrNull() ?: 0L,
                            fish = fish.takeIf { it.isNotBlank() },
                            location = location.takeIf { it.isNotBlank() },
                            metric = metric,
                            prizePlaces = prizePlaces,
                            prizesJson = Json.encodeToString(prizes)
                        )
                        apiClient.createTournament(req)
                        onCreated()
                    } catch (e: Exception) { }
                }
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
