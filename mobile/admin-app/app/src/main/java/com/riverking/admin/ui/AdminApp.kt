package com.riverking.admin.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riverking.admin.network.AdminApiClient

@Composable
fun AdminApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val apiClient = remember { AdminApiClient() }
    val connectedServerName = remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { url, token ->
                    apiClient.baseUrl = url
                    apiClient.token = token
                    // Resolve server name from saved servers
                    val servers = loadServers(context)
                    connectedServerName.value = servers.find { it.url == url }?.name ?: url
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                serverName = connectedServerName.value,
                onNavigateToTournaments = { navController.navigate("tournaments") },
                onNavigateToEvents = { navController.navigate("events") },
                onNavigateToDiscounts = { navController.navigate("discounts") },
                onNavigateToBroadcast = { navController.navigate("broadcast") },
                onSwitchServer = {
                    apiClient.token = ""
                    setActiveServerIndex(context, -1)
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onLogout = {
                    apiClient.token = ""
                    setActiveServerIndex(context, -1)
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("tournaments") {
            TournamentsScreen(
                apiClient = apiClient,
                onBack = { navController.popBackStack() }
            )
        }
        composable("events") {
            EventsScreen(
                apiClient = apiClient,
                onBack = { navController.popBackStack() }
            )
        }
        composable("discounts") {
            DiscountsScreen(
                apiClient = apiClient,
                onBack = { navController.popBackStack() }
            )
        }
        composable("broadcast") {
            BroadcastScreen(
                apiClient = apiClient,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
