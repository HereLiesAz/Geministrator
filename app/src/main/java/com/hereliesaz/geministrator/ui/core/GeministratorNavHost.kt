package com.hereliesaz.geministrator.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        startDestination = "source_selection",
        modifier = modifier
    ) {
        composable("source_selection") {
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
            SessionScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}