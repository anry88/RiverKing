package com.riverking.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.AdminCatalogDTO
import com.riverking.admin.network.CatalogOptionDTO
import com.riverking.admin.network.PrizeOptionDTO
import com.riverking.admin.network.TournamentDTO
import com.riverking.admin.network.TournamentReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TOURNAMENT_PAGE_SIZE = 10
private const val COIN_PRIZE_ID = "coins"
private val AdminDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val TournamentJson = Json { ignoreUnknownKeys = true }

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
    var catalog by remember { mutableStateOf<AdminCatalogDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPageLoading by remember { mutableStateOf(false) }
    var nextOffset by remember { mutableStateOf(0) }
    var canLoadMore by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TournamentDTO?>(null) }
    var selectedTournament by remember { mutableStateOf<TournamentDTO?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadCatalog() {
        scope.launch {
            try {
                catalog = apiClient.getCatalog()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Catalog failed: ${e.message}")
            }
        }
    }

    fun loadTournaments(reset: Boolean) {
        if (isPageLoading) return
        scope.launch {
            val offset = if (reset) 0 else nextOffset
            if (reset) isLoading = true else isPageLoading = true
            try {
                val page = apiClient.getTournaments(offset = offset, limit = TOURNAMENT_PAGE_SIZE + 1)
                val visible = page.take(TOURNAMENT_PAGE_SIZE)
                tournaments = if (reset) visible else tournaments + visible
                nextOffset = offset + visible.size
                canLoadMore = page.size > TOURNAMENT_PAGE_SIZE
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to load: ${e.message}")
            } finally {
                isLoading = false
                isPageLoading = false
            }
        }
    }

    fun reloadTournaments() {
        nextOffset = 0
        canLoadMore = false
        loadTournaments(reset = true)
    }

    LaunchedEffect(Unit) {
        loadCatalog()
        reloadTournaments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tournaments") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (catalog == null) {
                        scope.launch { snackbarHostState.showSnackbar("Catalog is still loading") }
                    } else {
                        showCreateDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().riverBackdrop().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (tournaments.isEmpty()) {
                Text(
                    text = "No tournaments",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(tournaments, key = { it.id }) { tournament ->
                        TournamentAdminCard(
                            tournament = tournament,
                            onOpenClick = {
                                if (catalog == null) {
                                    scope.launch { snackbarHostState.showSnackbar("Catalog is still loading") }
                                } else {
                                    selectedTournament = tournament
                                }
                            },
                            onDeleteClick = { pendingDelete = tournament }
                        )
                    }
                    if (canLoadMore) {
                        item {
                            Button(
                                onClick = { loadTournaments(reset = false) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                enabled = !isPageLoading,
                                colors = riverPrimaryButtonColors()
                            ) {
                                if (isPageLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isPageLoading) "Loading..." else "Load 10 more")
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    val tournamentToDelete = pendingDelete
    if (tournamentToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete tournament?") },
            text = {
                Text(
                    text = "This will permanently delete \"${tournamentToDelete.nameRu}\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.deleteTournament(tournamentToDelete.id)
                                pendingDelete = null
                                snackbarHostState.showSnackbar("Tournament deleted")
                                reloadTournaments()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Delete failed: ${e.message}")
                            }
                        }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    val loadedCatalog = catalog
    if (showCreateDialog && loadedCatalog != null) {
        TournamentEditorDialog(
            apiClient = apiClient,
            catalog = loadedCatalog,
            tournament = null,
            onDismiss = { showCreateDialog = false },
            onSaved = {
                showCreateDialog = false
                reloadTournaments()
            }
        )
    }

    val tournamentToEdit = selectedTournament
    if (tournamentToEdit != null && loadedCatalog != null) {
        TournamentEditorDialog(
            apiClient = apiClient,
            catalog = loadedCatalog,
            tournament = tournamentToEdit,
            onDismiss = { selectedTournament = null },
            onSaved = {
                selectedTournament = null
                reloadTournaments()
            }
        )
    }
}

@Composable
private fun TournamentAdminCard(
    tournament: TournamentDTO,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onOpenClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = tournament.nameRu,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Metric: ${tournament.metric} | Places: ${tournament.prizePlaces}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatDateMillis(tournament.startTime)} -> ${formatDateMillis(tournament.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentEditorDialog(
    apiClient: AdminApiClient,
    catalog: AdminCatalogDTO,
    tournament: TournamentDTO?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now(ZoneOffset.UTC) }
    val prizeOptions = remember(catalog) {
        catalog.tournamentPrizes.ifEmpty {
            listOf(PrizeOptionDTO(COIN_PRIZE_ID, "Монеты", defaultQty = 1000, coins = true))
        }
    }
    val initialPrizePlaces = tournament?.prizePlaces ?: 3
    val initialPrizes = remember(tournament?.id, prizeOptions) {
        val parsed = tournament?.prizesJson?.let { parseTournamentPrizes(it, prizeOptions) }
            ?: List(initialPrizePlaces) { defaultPrize(prizeOptions.first()) }
        adjustPrizes(parsed, initialPrizePlaces, prizeOptions)
    }
    var nameRu by remember(tournament?.id) { mutableStateOf(tournament?.nameRu.orEmpty()) }
    var nameEn by remember(tournament?.id) { mutableStateOf(tournament?.nameEn.orEmpty()) }
    var metric by remember(tournament?.id) { mutableStateOf(tournament?.metric ?: catalog.metrics.firstOrNull()?.id ?: "total_weight") }
    var fish by remember(tournament?.id) { mutableStateOf(tournament?.fish) }
    var location by remember(tournament?.id) { mutableStateOf(tournament?.location) }
    var startDate by remember(tournament?.id) {
        mutableStateOf(tournament?.startTime?.let(::formatDateMillis) ?: today.format(AdminDateFormatter))
    }
    var endDate by remember(tournament?.id) {
        mutableStateOf(tournament?.endTime?.let(::formatDateMillis) ?: today.plusDays(1).format(AdminDateFormatter))
    }
    var prizePlacesStr by remember(tournament?.id) { mutableStateOf(initialPrizePlaces.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val prizePlaces = prizePlacesStr.toIntOrNull()?.coerceAtLeast(0) ?: 0
    var prizes by remember(tournament?.id, prizeOptions) {
        mutableStateOf(initialPrizes)
    }

    LaunchedEffect(prizePlaces, prizeOptions) {
        prizes = adjustPrizes(prizes, prizePlaces, prizeOptions)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = { Text(if (tournament == null) "Create Tournament" else "Tournament #${tournament.id}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = nameRu, onValueChange = { nameRu = it }, label = { Text("Name RU") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name EN") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                AdminOptionDropdown(
                    label = "Metric",
                    options = catalog.metrics,
                    selectedId = metric,
                    onSelected = { selected -> metric = selected ?: metric }
                )
                AdminOptionDropdown(
                    label = "Fish",
                    options = catalog.fish,
                    selectedId = fish,
                    emptyLabel = "Any fish",
                    onSelected = { fish = it }
                )
                AdminOptionDropdown(
                    label = "Location",
                    options = catalog.locations,
                    selectedId = location,
                    emptyLabel = "Any location",
                    onSelected = { location = it }
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date (dd.mm.yyyy)") },
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date (dd.mm.yyyy)") },
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prizePlacesStr,
                    onValueChange = { prizePlacesStr = it.filter(Char::isDigit).take(2) },
                    label = { Text("Prize Places") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Prizes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                prizes.forEachIndexed { index, prize ->
                    PrizeEditorCard(
                        place = index + 1,
                        prize = prize,
                        options = prizeOptions,
                        onChange = { updated ->
                            prizes = prizes.toMutableList().also { it[index] = updated }
                        }
                    )
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startMillis = parseAdminDateMillis(startDate)
                val endMillis = parseAdminDateMillis(endDate)
                when {
                    nameRu.isBlank() || nameEn.isBlank() -> errorMessage = "Names are required"
                    startMillis == null || endMillis == null -> errorMessage = "Use date format dd.mm.yyyy"
                    endMillis <= startMillis -> errorMessage = "End date must be after start date"
                    prizePlaces <= 0 -> errorMessage = "Prize places must be greater than zero"
                    else -> {
                        errorMessage = null
                        scope.launch {
                            try {
                                val req = TournamentReq(
                                    nameRu = nameRu.trim(),
                                    nameEn = nameEn.trim(),
                                    startTime = startMillis,
                                    endTime = endMillis,
                                    fish = fish,
                                    location = location,
                                    metric = metric,
                                    prizePlaces = prizePlaces,
                                    prizesJson = TournamentJson.encodeToString(prizes.map { normalizePrize(it) })
                                )
                                if (tournament == null) {
                                    apiClient.createTournament(req)
                                } else {
                                    apiClient.updateTournament(tournament.id, req)
                                }
                                onSaved()
                            } catch (e: Exception) {
                                errorMessage = "Save failed: ${e.message}"
                            }
                        }
                    }
                }
            }) { Text(if (tournament == null) "Create" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOptionDropdown(
    label: String,
    options: List<CatalogOptionDTO>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    emptyLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleOptions = remember(options, emptyLabel) {
        if (emptyLabel == null) options else listOf(CatalogOptionDTO("", emptyLabel)) + options
    }
    val selectedLabel = when {
        selectedId.isNullOrBlank() -> emptyLabel.orEmpty()
        else -> options.firstOrNull { it.id == selectedId }?.label ?: selectedId
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = riverTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            visibleOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.id.takeIf { it.isNotBlank() })
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrizeOptionDropdown(
    options: List<PrizeOptionDTO>,
    selectedId: String,
    onSelected: (PrizeOptionDTO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: selectedId
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Prize") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = riverTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PrizeEditorCard(
    place: Int,
    prize: PrizeSpec,
    options: List<PrizeOptionDTO>,
    onChange: (PrizeSpec) -> Unit
) {
    val amount = prize.coins ?: prize.qty
    val step = if (prize.pack == COIN_PRIZE_ID) 100 else 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Place $place", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrizeOptionDropdown(
                options = options,
                selectedId = prize.pack.ifBlank { COIN_PRIZE_ID },
                onSelected = { onChange(defaultPrize(it)) }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Qty:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { onChange(prize.withAmount((amount - step).coerceAtLeast(1))) }) {
                    Icon(Icons.Default.Remove, "Decrease")
                }
                OutlinedTextField(
                    value = amount.toString(),
                    onValueChange = { raw ->
                        raw.filter(Char::isDigit).toIntOrNull()?.takeIf { it > 0 }?.let {
                            onChange(prize.withAmount(it))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                IconButton(onClick = { onChange(prize.withAmount(amount + step)) }) {
                    Icon(Icons.Default.Add, "Increase")
                }
            }
        }
    }
}

private fun defaultPrize(option: PrizeOptionDTO): PrizeSpec =
    if (option.coins || option.id == COIN_PRIZE_ID) {
        PrizeSpec(pack = COIN_PRIZE_ID, qty = option.defaultQty, coins = option.defaultQty)
    } else {
        PrizeSpec(pack = option.id, qty = option.defaultQty, coins = null)
    }

private fun PrizeSpec.withAmount(amount: Int): PrizeSpec =
    if (pack == COIN_PRIZE_ID) {
        copy(qty = amount, coins = amount)
    } else {
        copy(qty = amount, coins = null)
    }

private fun normalizePrize(prize: PrizeSpec): PrizeSpec =
    if (prize.pack == COIN_PRIZE_ID) {
        val amount = prize.coins ?: prize.qty
        PrizeSpec(pack = COIN_PRIZE_ID, qty = amount, coins = amount)
    } else {
        prize.copy(coins = null)
    }

private fun parseTournamentPrizes(raw: String, options: List<PrizeOptionDTO>): List<PrizeSpec> {
    val fallback = listOf(defaultPrize(options.first()))
    return runCatching {
        TournamentJson.decodeFromString<List<PrizeSpec>>(raw)
            .ifEmpty { fallback }
            .map { prize ->
                when {
                    prize.pack == COIN_PRIZE_ID || prize.coins != null -> {
                        val amount = prize.coins ?: prize.qty
                        PrizeSpec(pack = COIN_PRIZE_ID, qty = amount.coerceAtLeast(1), coins = amount.coerceAtLeast(1))
                    }
                    prize.pack.isBlank() -> defaultPrize(options.first())
                    else -> prize.copy(qty = prize.qty.coerceAtLeast(1), coins = null)
                }
            }
    }.getOrElse { fallback }
}

private fun adjustPrizes(
    current: List<PrizeSpec>,
    prizePlaces: Int,
    options: List<PrizeOptionDTO>
): List<PrizeSpec> = when {
    prizePlaces > current.size -> current + List(prizePlaces - current.size) { defaultPrize(options.first()) }
    prizePlaces < current.size -> current.take(prizePlaces)
    else -> current
}

private fun parseAdminDateMillis(value: String): Long? =
    runCatching {
        LocalDate.parse(value.trim(), AdminDateFormatter)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun formatDateMillis(value: Long): String =
    runCatching {
        java.time.Instant.ofEpochMilli(value)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(AdminDateFormatter)
    }.getOrElse { value.toString() }
