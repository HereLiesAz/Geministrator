package com.hereliesaz.geministrator.android.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hereliesaz.geministrator.android.ui.explorer.FileExplorerScreen
import com.hereliesaz.geministrator.android.ui.explorer.FileViewerScreen
import com.hereliesaz.geministrator.android.ui.history.HistoryDetailScreen
import com.hereliesaz.geministrator.android.ui.main.MainSessionView
import com.hereliesaz.geministrator.android.ui.main.MainViewModel
import com.hereliesaz.geministrator.android.ui.navigation.HistoryScreen
import com.hereliesaz.geministrator.android.ui.navigation.SettingsScreen
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.settings.PromptEditorScreen
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun GeministratorNavHost(
    navController: NavHostController,
    projectViewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "sessions",
        modifier = modifier
    ) {
        composable("sessions") {
            MainSessionView(mainViewModel, projectViewModel)
        }
        composable("explorer") {
            FileExplorerScreen(
                projectViewModel = projectViewModel,
                onNavigateToFile = { encodedFilePath ->
                    navController.navigate("file_viewer/$encodedFilePath")
                }
            )
        }
        composable(
            route = "file_viewer/{filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            // Correctly use getString() to avoid the type mismatch
            val encodedFilePath = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = URLDecoder.decode(encodedFilePath, StandardCharsets.UTF_8.toString())
            FileViewerScreen(
                filePath = filePath,
                projectViewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateToPrompts = { navController.navigate("prompts") }
            )
        }
        composable("history") {
            HistoryScreen(onSessionClick = { sessionId ->
                navController.navigate("history_detail/$sessionId")
            })
        }
        composable(
            route = "history_detail/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1
            HistoryDetailScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("prompts") {
            PromptEditorScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}