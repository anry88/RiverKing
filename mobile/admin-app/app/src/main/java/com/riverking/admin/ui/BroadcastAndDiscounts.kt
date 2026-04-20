package com.riverking.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
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
                            try {
                                apiClient.sendBroadcast(BroadcastReq(textRu, textEn))
                                statusMessage = "Broadcast started successfully!"
                                textRu = ""
                                textEn = ""
                            } catch (e: Exception) {
                                statusMessage = "Error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverPrimaryButtonColors()
                ) {
                    Text("Send Broadcast")
                }
                if (statusMessage.isNotBlank()) {
                    Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
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
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadDiscounts() {
        scope.launch {
            try {
                discounts = apiClient.getDiscounts()
            } catch (e: Exception) {
                // Ignore for now
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
                                Column {
                                    Text(text = discount.packageId, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Price: ${discount.price}⭐", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${discount.startDate} - ${discount.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            apiClient.deleteDiscount(discount.packageId)
                                            loadDiscounts()
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
        var packId by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Discount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = packId, onValueChange = { packId = it }, label = { Text("Package ID") }, colors = riverTextFieldColors())
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Discount Price") }, colors = riverTextFieldColors())
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start Date (yyyy-mm-dd)") }, colors = riverTextFieldColors())
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End Date (yyyy-mm-dd)") }, colors = riverTextFieldColors())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            apiClient.createDiscount(DiscountReq(packId, price.toIntOrNull() ?: 0, start, end))
                            showCreateDialog = false
                            loadDiscounts()
                        } catch (e: Exception) { }
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
