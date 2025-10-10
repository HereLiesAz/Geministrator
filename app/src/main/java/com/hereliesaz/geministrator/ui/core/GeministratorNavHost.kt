package com.hereliesaz.geministrator.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hereliesaz.geministrator.ui.authentication.LoginScreen
import com.hereliesaz.geministrator.ui.jules.SessionScreen
import com.hereliesaz.geministrator.ui.jules.SourceSelectionScreen
import com.hereliesaz.geministrator.ui.settings.SettingsScreen

@Composable
fun GeministratorNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("source_selection") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("source_selection") {
        startDestination = "explorer",
        modifier = modifier
    ) {
        composable("explorer") {
            SourceSelectionScreen(
                onSessionCreated = { sessionId ->
                    navController.navigate("session/$sessionId")
                }
            )
        }
        composable(
            route = "session/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            SessionScreen()
        }
        composable("settings") {
            SettingsScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                onNavigateToRoles = { navController.navigate("roles-settings") }
            )
        }
        composable("cli") {
            com.hereliesaz.geministrator.ui.cli.CliScreen()
        }
        composable("roles-settings") {
            com.hereliesaz.geministrator.ui.settings.RolesSettingsScreen()
        }
    }
}