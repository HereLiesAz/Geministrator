package com.hereliesaz.geministrator.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hereliesaz.geministrator.ui.ide.IdeScreen
import com.hereliesaz.geministrator.ui.jules.SessionScreen
import com.hereliesaz.geministrator.ui.jules.SourceSelectionScreen
import com.hereliesaz.geministrator.ui.settings.SettingsScreen
import com.hereliesaz.geministrator.ui.terminal.CmdScreen

@Composable
fun GeministratorNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    setLoading: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "explorer",
        modifier = modifier
    ) {
        composable("explorer") {
            SourceSelectionScreen(
                onSessionCreated = { sessionId, roles ->
                    navController.navigate("session/$sessionId?roles=$roles")
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
            SessionScreen(setLoading = setLoading)
        }
        composable("ide") {
            IdeScreen(setLoading = setLoading)
        }
        composable("settings") {
            SettingsScreen(
                setLoading = setLoading,
                onLogout = {
                    navController.navigate("explorer") {
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
        composable("cmd") {
            CmdScreen(setLoading = setLoading)
        }
    }
}
