package com.riverking.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.BroadcastReq
import com.riverking.admin.network.DiscountDTO
import com.riverking.admin.network.DiscountReq
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) { loadDiscounts() }

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
            FloatingActionButton(onClick = { showCreateDialog = true }) {
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
                                    Text(text = discount.packageId, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Price: ${discount.price}⭐", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${discount.startDate} → ${discount.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showCreateDialog) {
        var packId by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Discount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = packId, onValueChange = { packId = it }, label = { Text("Package ID") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Discount Price (stars)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start (yyyy-mm-dd)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End (yyyy-mm-dd)") }, colors = riverTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.createDiscount(DiscountReq(packId, price.toIntOrNull() ?: 0, start, end))
                                showCreateDialog = false
                                snackbarHostState.showSnackbar("✅ Discount created")
                                loadDiscounts()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Error: ${e.message}")
                            }
                        }
                    },
                    enabled = packId.isNotBlank() && price.isNotBlank() && start.isNotBlank() && end.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
