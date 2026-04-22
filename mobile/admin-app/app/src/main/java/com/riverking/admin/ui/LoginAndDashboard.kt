package com.riverking.admin.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.riverking.admin.ui.theme.riverBackdrop
import com.riverking.admin.ui.theme.riverPrimaryButtonColors
import com.riverking.admin.ui.theme.riverTextFieldColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ServerEntry(
    val name: String,
    val url: String,
    val token: String
)

private val json = Json { ignoreUnknownKeys = true }

fun loadServers(context: Context): List<ServerEntry> {
    val prefs = context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
    val raw = prefs.getString("servers_json", null) ?: return emptyList()
    return try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
}

fun saveServers(context: Context, servers: List<ServerEntry>) {
    context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        .edit()
        .putString("servers_json", json.encodeToString(servers))
        .apply()
}

fun getActiveServerIndex(context: Context): Int {
    return context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        .getInt("active_server_index", -1)
}

fun setActiveServerIndex(context: Context, index: Int) {
    context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        .edit()
        .putInt("active_server_index", index)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    val context = LocalContext.current
    var servers by remember { mutableStateOf(loadServers(context)) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Auto-connect to last active server
    val activeIndex = getActiveServerIndex(context)
    LaunchedEffect(Unit) {
        if (activeIndex in servers.indices) {
            val s = servers[activeIndex]
            onLoginSuccess(s.url, s.token)
        }
    }

    Box(modifier = Modifier.fillMaxSize().riverBackdrop()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "River King Admin",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a server to connect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (servers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No servers added yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add your first server to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        val index = servers.indexOf(server)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                setActiveServerIndex(context, index)
                                onLoginSuccess(server.url, server.token)
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = server.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    val newServers = servers.toMutableList()
                                    newServers.removeAt(index)
                                    servers = newServers
                                    saveServers(context, servers)
                                    if (getActiveServerIndex(context) == index) {
                                        setActiveServerIndex(context, -1)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = riverPrimaryButtonColors()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Server")
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { entry ->
                val newServers = servers + entry
                servers = newServers
                saveServers(context, newServers)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (ServerEntry) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var token by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    placeholder = { Text("e.g. Production") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverTextFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://your-server.com") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverTextFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Admin API Token") },
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Hide token" else "Show token"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = riverTextFieldColors(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(ServerEntry(name = name.ifBlank { url }, url = url.trimEnd('/'), token = token.trim())) },
                enabled = url.isNotBlank() && token.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serverName: String,
    onNavigateToTournaments: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToDiscounts: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onSwitchServer: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard")
                        Text(
                            text = serverName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSwitchServer) {
                        Text("Switch", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onLogout) {
                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().riverBackdrop().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard("Tournaments", "Manage game tournaments", onNavigateToTournaments)
                DashboardCard("Events", "Manage special club events", onNavigateToEvents)
                DashboardCard("Discounts", "Manage shop discounts", onNavigateToDiscounts)
                DashboardCard("Broadcast", "Send messages to users", onNavigateToBroadcast)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
