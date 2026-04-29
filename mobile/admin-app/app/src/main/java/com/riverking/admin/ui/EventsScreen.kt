package com.riverking.admin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.Alignment
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
import com.riverking.admin.network.PrizeOptionDTO
import com.riverking.admin.network.SpecialEventDTO
import com.riverking.admin.network.SpecialEventReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverTextFieldColors
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val EVENT_MAX_PRIZE_PLACES = 1000
private const val EVENT_COIN_PRIZE_ID = "coins"
private const val EVENT_IMAGE_MAX_DIMENSION = 1800
private const val EVENT_IMAGE_WEBP_QUALITY = 88
private const val EVENT_IMAGE_DIRECT_UPLOAD_LIMIT_BYTES = 900 * 1024
private val EventDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val EventJson = Json { ignoreUnknownKeys = true }

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
        },
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
    val prizeOptions = remember(catalog.tournamentPrizes) { eventPrizeOptions(catalog.tournamentPrizes) }
    val today = remember { LocalDate.now(ZoneOffset.UTC) }
    var nameRu by remember(event?.id) { mutableStateOf(event?.nameRu.orEmpty()) }
    var nameEn by remember(event?.id) { mutableStateOf(event?.nameEn.orEmpty()) }
    var imagePath by remember(event?.id) { mutableStateOf(event?.imagePath.orEmpty()) }
    var startDate by remember(event?.id) {
        mutableStateOf(event?.startTime?.let(::formatEventDate) ?: today.format(EventDateFormatter))
    }
    var endDate by remember(event?.id) {
        mutableStateOf(event?.endTime?.let(::formatEventDate) ?: today.plusDays(7).format(EventDateFormatter))
    }
    var fishRows by remember(event?.id) {
        mutableStateOf(
            event?.fish?.takeIf { it.isNotEmpty() }
                ?: listOf(AdminEventFishDTO(catalog.eventFish.firstOrNull()?.id?.toLongOrNull() ?: 1L, 1.0))
        )
    }
    var weightPlaces by remember(event?.id) { mutableStateOf((event?.weightPrizes?.prizePlaces ?: 3).toString()) }
    var countPlaces by remember(event?.id) { mutableStateOf((event?.countPrizes?.prizePlaces ?: 3).toString()) }
    var fishPlaces by remember(event?.id) { mutableStateOf((event?.fishPrizes?.prizePlaces ?: 3).toString()) }
    var weightPrizes by remember(event?.id, prizeOptions) {
        mutableStateOf(parseEventPrizes(event?.weightPrizes?.prizesJson, prizeOptions))
    }
    var countPrizes by remember(event?.id, prizeOptions) {
        mutableStateOf(parseEventPrizes(event?.countPrizes?.prizesJson, prizeOptions))
    }
    var fishPrizes by remember(event?.id, prizeOptions) {
        mutableStateOf(parseEventPrizes(event?.fishPrizes?.prizesJson, prizeOptions))
    }
    var error by remember(event?.id) { mutableStateOf<String?>(null) }
    var uploading by remember(event?.id) { mutableStateOf(false) }

    val weightPlaceCount = eventPrizePlaces(weightPlaces)
    val countPlaceCount = eventPrizePlaces(countPlaces)
    val fishPlaceCount = eventPrizePlaces(fishPlaces)

    LaunchedEffect(weightPlaceCount, prizeOptions) {
        weightPrizes = adjustEventPrizes(weightPrizes, weightPlaceCount, prizeOptions)
    }
    LaunchedEffect(countPlaceCount, prizeOptions) {
        countPrizes = adjustEventPrizes(countPrizes, countPlaceCount, prizeOptions)
    }
    LaunchedEffect(fishPlaceCount, prizeOptions) {
        fishPrizes = adjustEventPrizes(fishPrizes, fishPlaceCount, prizeOptions)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            try {
                val selection = prepareEventImageUpload(context, uri)
                val uploaded = apiClient.uploadEventImage(selection.fileName, selection.bytes)
                imagePath = uploaded.imagePath
                error = null
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(nameRu, { nameRu = it }, label = { Text("Location name RU") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nameEn, { nameEn = it }, label = { Text("Location name EN") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = imagePath,
                        onValueChange = { imagePath = it },
                        label = { Text("Image") },
                        colors = riverTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { imagePicker.launch("image/*") }, enabled = !uploading) {
                        Text(if (uploading) "..." else "Upload")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text("Start dd.mm.yyyy") }, colors = riverTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(endDate, { endDate = it }, label = { Text("End dd.mm.yyyy") }, colors = riverTextFieldColors(), modifier = Modifier.weight(1f))
                }
                Text("Fish pool", style = MaterialTheme.typography.titleMedium)
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
                        EventDecimalField("Weight", row.weight.toString(), { value ->
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

                EventPrizeSection(
                    title = "Total weight prizes",
                    places = weightPlaces,
                    onPlaces = { weightPlaces = sanitizeEventPrizePlacesInput(it) },
                    prizes = weightPrizes,
                    onPrizes = { weightPrizes = it },
                    options = prizeOptions,
                )
                EventPrizeSection(
                    title = "Fish count prizes",
                    places = countPlaces,
                    onPlaces = { countPlaces = sanitizeEventPrizePlacesInput(it) },
                    prizes = countPrizes,
                    onPrizes = { countPrizes = it },
                    options = prizeOptions,
                )
                EventPrizeSection(
                    title = "Personal fish prizes",
                    places = fishPlaces,
                    onPlaces = { fishPlaces = sanitizeEventPrizePlacesInput(it) },
                    prizes = fishPrizes,
                    onPrizes = { fishPrizes = it },
                    options = prizeOptions,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = parseEventDateMillis(startDate)
                val end = parseEventDateMillis(endDate)
                when {
                    nameRu.isBlank() || nameEn.isBlank() -> error = "Names are required"
                    start == null || end == null -> error = "Use date format dd.mm.yyyy"
                    end <= start -> error = "End date must be after start date"
                    fishRows.isEmpty() || fishRows.any { it.fishId <= 0L || it.weight <= 0.0 } -> error = "Fish and weights are required"
                    else -> {
                        error = null
                        val req = SpecialEventReq(
                            nameRu = nameRu.trim(),
                            nameEn = nameEn.trim(),
                            startTime = start,
                            endTime = end,
                            imagePath = imagePath.ifBlank { null },
                            castZone = event?.castZone,
                            fish = fishRows,
                            weightPrizes = eventPrizeConfig(weightPlaceCount, weightPrizes),
                            countPrizes = eventPrizeConfig(countPlaceCount, countPrizes),
                            fishPrizes = eventPrizeConfig(fishPlaceCount, fishPrizes),
                        )
                        scope.launch {
                            try {
                                if (event == null) apiClient.createEvent(req) else apiClient.updateEvent(event.id, req)
                                onSaved()
                            } catch (e: Exception) {
                                error = "Save failed: ${e.message}"
                            }
                        }
                    }
                }
            }) { Text(if (event == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EventDecimalField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
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
private fun EventPrizeSection(
    title: String,
    places: String,
    onPlaces: (String) -> Unit,
    prizes: List<PrizeSpec>,
    onPrizes: (List<PrizeSpec>) -> Unit,
    options: List<PrizeOptionDTO>,
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = places,
        onValueChange = onPlaces,
        label = { Text("Prize places") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = riverTextFieldColors(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    prizes.forEachIndexed { index, prize ->
        EventPrizeEditorCard(
            place = index + 1,
            prize = prize,
            options = options,
            onChange = { updated ->
                onPrizes(prizes.toMutableList().also { it[index] = updated })
            },
        )
    }
}

@Composable
private fun EventPrizeEditorCard(
    place: Int,
    prize: PrizeSpec,
    options: List<PrizeOptionDTO>,
    onChange: (PrizeSpec) -> Unit,
) {
    val amount = prize.coins ?: prize.qty
    val step = if (prize.pack == EVENT_COIN_PRIZE_ID) 100 else 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Place $place", color = MaterialTheme.colorScheme.onSurfaceVariant)
            EventPrizeOptionDropdown(
                options = options,
                selectedId = prize.pack.ifBlank { EVENT_COIN_PRIZE_ID },
                onSelected = { onChange(eventDefaultPrize(it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Qty:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { onChange(prize.withEventAmount((amount - step).coerceAtLeast(1))) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                OutlinedTextField(
                    value = amount.toString(),
                    onValueChange = { raw ->
                        raw.filter(Char::isDigit).toIntOrNull()?.takeIf { it > 0 }?.let {
                            onChange(prize.withEventAmount(it))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.width(100.dp),
                )
                IconButton(onClick = { onChange(prize.withEventAmount(amount + step)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPrizeOptionDropdown(
    options: List<PrizeOptionDTO>,
    selectedId: String,
    onSelected: (PrizeOptionDTO) -> Unit,
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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
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

private data class EventImageUpload(
    val fileName: String,
    val bytes: ByteArray,
    val preview: Bitmap,
)

private fun prepareEventImageUpload(context: Context, uri: Uri): EventImageUpload {
    val resolver = context.contentResolver
    val originalBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("empty file")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("bad image")

    val preview = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size) ?: error("bad image")
    if (originalBytes.size <= EVENT_IMAGE_DIRECT_UPLOAD_LIMIT_BYTES) {
        return EventImageUpload(
            fileName = eventUploadFileName(uri, context, fallback = "event.png"),
            bytes = originalBytes,
            preview = preview,
        )
    }
    preview.recycle()

    val sample = eventImageSampleSize(bounds.outWidth, bounds.outHeight)
    val decoded = BitmapFactory.decodeByteArray(
        originalBytes,
        0,
        originalBytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    ) ?: error("bad image")
    val scaled = scaleEventBitmap(decoded)
    if (scaled !== decoded) decoded.recycle()
    val output = ByteArrayOutputStream()
    @Suppress("DEPRECATION")
    if (!scaled.compress(Bitmap.CompressFormat.WEBP, EVENT_IMAGE_WEBP_QUALITY, output)) {
        error("image compression failed")
    }
    return EventImageUpload(
        fileName = "event.webp",
        bytes = output.toByteArray(),
        preview = scaled,
    )
}

private fun eventUploadFileName(uri: Uri, context: Context, fallback: String): String {
    val type = context.contentResolver.getType(uri).orEmpty().lowercase()
    val extension = when {
        type.contains("webp") -> "webp"
        type.contains("jpeg") || type.contains("jpg") -> "jpg"
        type.contains("png") -> "png"
        else -> fallback.substringAfterLast('.', "png")
    }
    return "event.$extension"
}

private fun eventImageSampleSize(width: Int, height: Int): Int {
    var sample = 1
    while (width / sample > EVENT_IMAGE_MAX_DIMENSION * 2 || height / sample > EVENT_IMAGE_MAX_DIMENSION * 2) {
        sample *= 2
    }
    return sample
}

private fun scaleEventBitmap(bitmap: Bitmap): Bitmap {
    val maxSide = maxOf(bitmap.width, bitmap.height)
    if (maxSide <= EVENT_IMAGE_MAX_DIMENSION) return bitmap
    val ratio = EVENT_IMAGE_MAX_DIMENSION.toFloat() / maxSide.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
        (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
        true,
    )
}

private fun eventPrizeOptions(options: List<PrizeOptionDTO>): List<PrizeOptionDTO> =
    options.ifEmpty { listOf(PrizeOptionDTO(EVENT_COIN_PRIZE_ID, "Coins", defaultQty = 1000, coins = true)) }

private fun eventDefaultPrize(option: PrizeOptionDTO): PrizeSpec =
    if (option.coins || option.id == EVENT_COIN_PRIZE_ID) {
        PrizeSpec(pack = EVENT_COIN_PRIZE_ID, qty = option.defaultQty, coins = option.defaultQty)
    } else {
        PrizeSpec(pack = option.id, qty = option.defaultQty, coins = null)
    }

private fun PrizeSpec.withEventAmount(amount: Int): PrizeSpec =
    if (pack == EVENT_COIN_PRIZE_ID) {
        copy(qty = amount, coins = amount)
    } else {
        copy(qty = amount, coins = null)
    }

private fun normalizeEventPrize(prize: PrizeSpec): PrizeSpec =
    if (prize.pack == EVENT_COIN_PRIZE_ID || prize.coins != null) {
        val amount = (prize.coins ?: prize.qty).coerceAtLeast(1)
        PrizeSpec(pack = EVENT_COIN_PRIZE_ID, qty = amount, coins = amount)
    } else {
        prize.copy(qty = prize.qty.coerceAtLeast(1), coins = null)
    }

private fun parseEventPrizes(raw: String?, options: List<PrizeOptionDTO>): List<PrizeSpec> {
    val fallback = listOf(eventDefaultPrize(options.first()))
    if (raw.isNullOrBlank()) return fallback
    return runCatching {
        EventJson.decodeFromString<List<PrizeSpec>>(raw)
            .ifEmpty { fallback }
            .map { prize ->
                when {
                    prize.pack == EVENT_COIN_PRIZE_ID || prize.coins != null -> {
                        val amount = (prize.coins ?: prize.qty).coerceAtLeast(1)
                        PrizeSpec(pack = EVENT_COIN_PRIZE_ID, qty = amount, coins = amount)
                    }
                    prize.pack.isBlank() -> eventDefaultPrize(options.first())
                    else -> prize.copy(qty = prize.qty.coerceAtLeast(1), coins = null)
                }
            }
    }.getOrElse { fallback }
}

private fun adjustEventPrizes(
    current: List<PrizeSpec>,
    prizePlaces: Int,
    options: List<PrizeOptionDTO>,
): List<PrizeSpec> = when {
    prizePlaces > current.size -> current + List(prizePlaces - current.size) { eventDefaultPrize(options.first()) }
    prizePlaces < current.size -> current.take(prizePlaces)
    else -> current
}

private fun eventPrizeConfig(places: Int, prizes: List<PrizeSpec>): AdminEventPrizeDTO =
    AdminEventPrizeDTO(
        prizePlaces = places,
        prizesJson = EventJson.encodeToString(prizes.take(places).map(::normalizeEventPrize)),
    )

private fun sanitizeEventPrizePlacesInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(EVENT_MAX_PRIZE_PLACES.toString().length)
    val places = digits.toIntOrNull() ?: return digits
    return if (places > EVENT_MAX_PRIZE_PLACES) EVENT_MAX_PRIZE_PLACES.toString() else digits
}

private fun eventPrizePlaces(value: String): Int =
    value.toIntOrNull()?.coerceIn(0, EVENT_MAX_PRIZE_PLACES) ?: 0

private fun parseEventDateMillis(value: String): Long? =
    runCatching { LocalDate.parse(value.trim(), EventDateFormatter).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()

private fun formatEventDate(millis: Long): String =
    runCatching { java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(EventDateFormatter) }.getOrElse { millis.toString() }
