package com.riverking.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riverking.admin.network.AdminApiClient

@Composable
fun AdminApp() {
    val navController = rememberNavController()
    val apiClient = remember { AdminApiClient() }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { url, token ->
                    apiClient.baseUrl = url
                    apiClient.token = token
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToTournaments = { navController.navigate("tournaments") },
                onNavigateToDiscounts = { navController.navigate("discounts") },
                onNavigateToBroadcast = { navController.navigate("broadcast") },
                onLogout = {
                    apiClient.token = ""
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
