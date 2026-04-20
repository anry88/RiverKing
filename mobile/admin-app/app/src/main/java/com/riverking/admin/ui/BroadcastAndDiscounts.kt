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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.AdminCatalogDTO
import com.riverking.admin.network.BroadcastReq
import com.riverking.admin.network.DiscountDTO
import com.riverking.admin.network.DiscountPackageDTO
import com.riverking.admin.network.DiscountReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val AdminDiscountDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(apiClient: AdminApiClient, onBack: () -> Unit) {
    var textRu by remember { mutableStateOf("") }
    var textEn by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().riverBackdrop().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = textRu,
                    onValueChange = { textRu = it },
                    label = { Text("Message (Russian)") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = riverTextFieldColors()
                )
                OutlinedTextField(
                    value = textEn,
                    onValueChange = { textEn = it },
                    label = { Text("Message (English)") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = riverTextFieldColors()
                )
                Button(
                    onClick = {
                        scope.launch {
                            isSending = true
                            try {
                                val resp = apiClient.sendBroadcast(BroadcastReq(textRu, textEn))
                                snackbarHostState.showSnackbar("✅ Broadcast queued to ${resp.count} users")
                                textRu = ""
                                textEn = ""
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Error: ${e.message}")
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverPrimaryButtonColors(),
                    enabled = !isSending && (textRu.isNotBlank() || textEn.isNotBlank())
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isSending) "Sending..." else "Send Broadcast")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountsScreen(apiClient: AdminApiClient, onBack: () -> Unit) {
    var discounts by remember { mutableStateOf<List<DiscountDTO>>(emptyList()) }
    var catalog by remember { mutableStateOf<AdminCatalogDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadDiscounts() {
        scope.launch {
            try {
                discounts = apiClient.getDiscounts()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadCatalog() {
        scope.launch {
            try {
                catalog = apiClient.getCatalog()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Catalog failed: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCatalog()
        loadDiscounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discounts") },
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
            FloatingActionButton(onClick = {
                if (catalog == null) {
                    scope.launch { snackbarHostState.showSnackbar("Catalog is still loading") }
                } else {
                    showCreateDialog = true
                }
            }) {
                Icon(Icons.Default.Add, "Add")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().riverBackdrop().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { isLoading = true; loadDiscounts() }) { Text("Retry") }
                }
            } else if (discounts.isEmpty()) {
                Text(
                    "No active discounts",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(discounts) { discount ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val pack = catalog?.discountPackages?.firstOrNull { it.id == discount.packageId }
                                    Text(text = pack?.name ?: discount.packageId, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    pack?.let {
                                        Text(text = it.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(text = "Price: ${discount.price}⭐", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${formatAdminDiscountDate(discount.startDate)} → ${formatAdminDiscountDate(discount.endDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteDiscount(discount.packageId)
                                            snackbarHostState.showSnackbar("✅ Discount removed")
                                            loadDiscounts()
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("❌ Error: ${e.message}")
                                        }
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

    val loadedCatalog = catalog
    if (showCreateDialog && loadedCatalog != null) {
        CreateDiscountDialog(
            apiClient = apiClient,
            catalog = loadedCatalog,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                scope.launch { snackbarHostState.showSnackbar("✅ Discount created") }
                loadDiscounts()
                loadCatalog()
            },
            onError = { message ->
                scope.launch { snackbarHostState.showSnackbar("❌ Error: $message") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDiscountDialog(
    apiClient: AdminApiClient,
    catalog: AdminCatalogDTO,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now(ZoneOffset.UTC) }
    var selectedPackageId by remember(catalog) { mutableStateOf(catalog.discountPackages.firstOrNull()?.id.orEmpty()) }
    val selectedPackage = catalog.discountPackages.firstOrNull { it.id == selectedPackageId }
    var price by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today.format(AdminDiscountDateFormatter)) }
    var end by remember { mutableStateOf(today.plusDays(1).format(AdminDiscountDateFormatter)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedPackageId) {
        selectedPackage?.let { price = it.currentPrice.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Discount") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiscountPackageDropdown(
                    packages = catalog.discountPackages,
                    selectedId = selectedPackageId,
                    onSelected = { selectedPackageId = it.id }
                )
                selectedPackage?.let { pack ->
                    Text(
                        text = buildString {
                            append("Current price: ${pack.currentPrice}⭐")
                            if (pack.basePrice != pack.currentPrice) {
                                append(" (base ${pack.basePrice}⭐)")
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (pack.activeDiscountPrice != null) {
                        Text(
                            text = "Active discount: ${pack.activeDiscountPrice}⭐ (${pack.activeDiscountStart} → ${pack.activeDiscountEnd})",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit) },
                    label = { Text("Discount price (stars)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start (dd.mm.yyyy)") },
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End (dd.mm.yyyy)") },
                    colors = riverTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pack = selectedPackage
                    val discountPrice = price.toIntOrNull()
                    val startDate = parseAdminDiscountDate(start)
                    val endDate = parseAdminDiscountDate(end)
                    when {
                        pack == null -> errorMessage = "Select a package"
                        discountPrice == null || discountPrice <= 0 -> errorMessage = "Enter a positive price"
                        startDate == null || endDate == null -> errorMessage = "Use date format dd.mm.yyyy"
                        !endDate.isAfter(startDate) -> errorMessage = "End date must be after start date"
                        else -> {
                            errorMessage = null
                            scope.launch {
                                try {
                                    apiClient.createDiscount(DiscountReq(pack.id, discountPrice, start, end))
                                    onCreated()
                                } catch (e: Exception) {
                                    onError(e.message ?: "unknown")
                                }
                            }
                        }
                    }
                },
                enabled = catalog.discountPackages.isNotEmpty()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscountPackageDropdown(
    packages: List<DiscountPackageDTO>,
    selectedId: String,
    onSelected: (DiscountPackageDTO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = packages.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${it.currentPrice}⭐" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Package / rod / subscription") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = riverTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            packages.forEach { pack ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(pack.name)
                            Text(
                                "${pack.category} · current ${pack.currentPrice}⭐",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelected(pack)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun parseAdminDiscountDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), AdminDiscountDateFormatter) }.getOrNull()

private fun formatAdminDiscountDate(value: String): String =
    runCatching { LocalDate.parse(value).format(AdminDiscountDateFormatter) }.getOrElse { value }
