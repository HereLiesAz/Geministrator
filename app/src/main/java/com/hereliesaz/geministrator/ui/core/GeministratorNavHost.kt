package com.hereliesaz.geministrator.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hereliesaz.geministrator.android.ui.jules.SessionScreen
import com.hereliesaz.geministrator.android.ui.jules.SourceSelectionScreen
import com.hereliesaz.geministrator.android.ui.settings.SettingsScreen

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
            SettingsScreen()
        }
    }
}