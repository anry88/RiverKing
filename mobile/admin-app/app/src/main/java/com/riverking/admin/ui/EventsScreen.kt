package com.riverking.admin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.AdminCatalogDTO
import com.riverking.admin.network.AdminEventFishDTO
import com.riverking.admin.network.AdminEventPrizeDTO
import com.riverking.admin.network.CatalogOptionDTO
import com.riverking.admin.network.EventCastAreaDTO
import com.riverking.admin.network.SpecialEventDTO
import com.riverking.admin.network.SpecialEventReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val EventDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(apiClient: AdminApiClient, onBack: () -> Unit) {
    var events by remember { mutableStateOf<List<SpecialEventDTO>>(emptyList()) }
    var catalog by remember { mutableStateOf<AdminCatalogDTO?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SpecialEventDTO?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun reload() {
        scope.launch {
            loading = true
            try {
                catalog = apiClient.getCatalog()
                events = apiClient.getEvents()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Load failed: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Special Events") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = "Add") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .riverBackdrop()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                item { Text("Loading...") }
            }
            items(events) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = {
                        editing = event
                        showEditor = true
                    },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(event.nameRu, style = MaterialTheme.typography.titleMedium)
                        Text("${formatEventDate(event.startTime)} -> ${formatEventDate(event.endTime)}")
                        Text("Fish: ${event.fish.size}; image: ${event.imagePath ?: "-"}")
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    apiClient.deleteEvent(event.id)
                                    snackbarHostState.showSnackbar("Event deleted")
                                    reload()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Delete failed: ${e.message}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    val loadedCatalog = catalog
    if (showEditor && loadedCatalog != null) {
        EventEditorDialog(
            apiClient = apiClient,
            catalog = loadedCatalog,
            event = editing,
            onDismiss = { showEditor = false },
            onSaved = {
                showEditor = false
                reload()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditorDialog(
    apiClient: AdminApiClient,
    catalog: AdminCatalogDTO,
    event: SpecialEventDTO?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var nameRu by remember(event?.id) { mutableStateOf(event?.nameRu.orEmpty()) }
    var nameEn by remember(event?.id) { mutableStateOf(event?.nameEn.orEmpty()) }
    var imagePath by remember(event?.id) { mutableStateOf(event?.imagePath.orEmpty()) }
    var startDate by remember(event?.id) { mutableStateOf(event?.startTime?.let(::formatEventDate) ?: LocalDate.now(ZoneOffset.UTC).format(EventDateFormatter)) }
    var endDate by remember(event?.id) { mutableStateOf(event?.endTime?.let(::formatEventDate) ?: LocalDate.now(ZoneOffset.UTC).plusDays(7).format(EventDateFormatter)) }
    var minX by remember(event?.id) { mutableStateOf((event?.castArea?.minX ?: 0.14).toString()) }
    var maxX by remember(event?.id) { mutableStateOf((event?.castArea?.maxX ?: 0.86).toString()) }
    var farY by remember(event?.id) { mutableStateOf((event?.castArea?.farY ?: 0.47).toString()) }
    var nearY by remember(event?.id) { mutableStateOf((event?.castArea?.nearY ?: 0.78).toString()) }
    var fishRows by remember(event?.id) {
        mutableStateOf(event?.fish?.takeIf { it.isNotEmpty() } ?: listOf(AdminEventFishDTO(catalog.eventFish.firstOrNull()?.id?.toLongOrNull() ?: 1L, 1.0)))
    }
    var weightPlaces by remember(event?.id) { mutableStateOf((event?.weightPrizes?.prizePlaces ?: 3).toString()) }
    var countPlaces by remember(event?.id) { mutableStateOf((event?.countPrizes?.prizePlaces ?: 3).toString()) }
    var fishPlaces by remember(event?.id) { mutableStateOf((event?.fishPrizes?.prizePlaces ?: 3).toString()) }
    var weightPrizesJson by remember(event?.id) { mutableStateOf(event?.weightPrizes?.prizesJson ?: defaultEventPrizeJson()) }
    var countPrizesJson by remember(event?.id) { mutableStateOf(event?.countPrizes?.prizesJson ?: defaultEventPrizeJson()) }
    var fishPrizesJson by remember(event?.id) { mutableStateOf(event?.fishPrizes?.prizesJson ?: defaultEventPrizeJson()) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("empty file")
                imagePath = apiClient.uploadEventImage("event.webp", bytes).imagePath
            } catch (e: Exception) {
                error = "Upload failed: ${e.message}"
            } finally {
                uploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
        title = { Text(if (event == null) "Create Event" else "Event #${event.id}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(nameRu, { nameRu = it }, label = { Text("Location name RU") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nameEn, { nameEn = it }, label = { Text("Location name EN") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(imagePath, { imagePath = it }, label = { Text("Image path") }, colors = riverTextFieldColors(), modifier = Modifier.weight(1f))
                    Button(onClick = { imagePicker.launch("image/*") }, enabled = !uploading) { Text(if (uploading) "..." else "Upload") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text("Start dd.mm.yyyy") }, colors = riverTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(endDate, { endDate = it }, label = { Text("End dd.mm.yyyy") }, colors = riverTextFieldColors(), modifier = Modifier.weight(1f))
                }
                Text("Cast area (normalized 0..1)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField("minX", minX, { minX = it }, Modifier.weight(1f))
                    NumberField("maxX", maxX, { maxX = it }, Modifier.weight(1f))
                    NumberField("farY", farY, { farY = it }, Modifier.weight(1f))
                    NumberField("nearY", nearY, { nearY = it }, Modifier.weight(1f))
                }
                Text("Fish pool")
                fishRows.forEachIndexed { index, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        EventOptionDropdown(
                            label = "Fish",
                            options = catalog.eventFish,
                            selectedId = row.fishId.toString(),
                            onSelected = { selected ->
                                fishRows = fishRows.toMutableList().also {
                                    it[index] = row.copy(fishId = selected.toLongOrNull() ?: row.fishId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        NumberField("Weight", row.weight.toString(), { value ->
                            fishRows = fishRows.toMutableList().also {
                                it[index] = row.copy(weight = value.toDoubleOrNull() ?: row.weight)
                            }
                        }, Modifier.width(110.dp))
                        IconButton(onClick = { fishRows = fishRows.toMutableList().also { it.removeAt(index) } }, enabled = fishRows.size > 1) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove fish")
                        }
                    }
                }
                Button(onClick = {
                    val first = catalog.eventFish.firstOrNull()?.id?.toLongOrNull() ?: 1L
                    fishRows = fishRows + AdminEventFishDTO(first, 1.0)
                }) { Text("Add fish") }
                EventPrizeBlock("Total weight prizes", weightPlaces, { weightPlaces = it }, weightPrizesJson, { weightPrizesJson = it })
                EventPrizeBlock("Fish count prizes", countPlaces, { countPlaces = it }, countPrizesJson, { countPrizesJson = it })
                EventPrizeBlock("Personal fish prizes", fishPlaces, { fishPlaces = it }, fishPrizesJson, { fishPrizesJson = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = parseEventDateMillis(startDate)
                val end = parseEventDateMillis(endDate)
                val castArea = EventCastAreaDTO(
                    minX.toDoubleOrNull() ?: -1.0,
                    maxX.toDoubleOrNull() ?: -1.0,
                    farY.toDoubleOrNull() ?: -1.0,
                    nearY.toDoubleOrNull() ?: -1.0,
                )
                if (nameRu.isBlank() || nameEn.isBlank() || start == null || end == null) {
                    error = "Names and valid dates are required"
                    return@TextButton
                }
                val req = SpecialEventReq(
                    nameRu = nameRu.trim(),
                    nameEn = nameEn.trim(),
                    startTime = start,
                    endTime = end,
                    imagePath = imagePath.ifBlank { null },
                    castArea = castArea,
                    fish = fishRows,
                    weightPrizes = AdminEventPrizeDTO(weightPlaces.toIntOrNull() ?: 0, weightPrizesJson),
                    countPrizes = AdminEventPrizeDTO(countPlaces.toIntOrNull() ?: 0, countPrizesJson),
                    fishPrizes = AdminEventPrizeDTO(fishPlaces.toIntOrNull() ?: 0, fishPrizesJson),
                )
                scope.launch {
                    try {
                        if (event == null) apiClient.createEvent(req) else apiClient.updateEvent(event.id, req)
                        onSaved()
                    } catch (e: Exception) {
                        error = "Save failed: ${e.message}"
                    }
                }
            }) { Text(if (event == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = riverTextFieldColors(),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun EventPrizeBlock(
    title: String,
    places: String,
    onPlaces: (String) -> Unit,
    prizesJson: String,
    onPrizesJson: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    NumberField("Places", places, onPlaces, Modifier.fillMaxWidth())
    OutlinedTextField(
        value = prizesJson,
        onValueChange = onPrizesJson,
        label = { Text("""Prize JSON [{"pack":"coins","qty":1000,"coins":1000}]""") },
        colors = riverTextFieldColors(),
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventOptionDropdown(
    label: String,
    options: List<CatalogOptionDTO>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.label ?: selectedId,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = riverTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun parseEventDateMillis(value: String): Long? =
    runCatching { LocalDate.parse(value.trim(), EventDateFormatter).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()

private fun formatEventDate(millis: Long): String =
    runCatching { java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(EventDateFormatter) }.getOrElse { millis.toString() }

private fun defaultEventPrizeJson(): String = """[{"pack":"coins","qty":1000,"coins":1000}]"""
