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
import com.hereliesaz.geministrator.ui.terminal.TerminalScreen

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
                    navController.navigate("explorer") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("explorer") {
            SourceSelectionScreen(
                onSessionCreated = { sessionId, roles ->
                    val encodedRoles = java.net.URLEncoder.encode(roles, "UTF-8")
                    navController.navigate("session/$sessionId?roles=$encodedRoles")
                }
            )
        }
        composable(
            route = "session/{sessionId}?roles={roles}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("roles") { type = NavType.StringType; nullable = true }
            )
        ) {
            SessionScreen()
        }
        composable("ide") {
            com.hereliesaz.geministrator.ui.ide.IdeScreen()
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
        composable("terminal") {
            TerminalScreen()
        }
    }
}
