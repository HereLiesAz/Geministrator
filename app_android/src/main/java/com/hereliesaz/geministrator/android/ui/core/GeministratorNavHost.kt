package com.hereliesaz.geministrator.android.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.material3.Text
import androidx.navigation.compose.composable
import com.hereliesaz.geministrator.android.ui.file.FileSaveScreen
import com.hereliesaz.geministrator.android.ui.main.MainSessionView
import com.hereliesaz.geministrator.android.ui.main.MainViewModel
import com.hereliesaz.geministrator.android.ui.navigation.HistoryScreen
import com.hereliesaz.geministrator.android.ui.navigation.SettingsScreen
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.session.DiffScreen
import com.hereliesaz.geministrator.android.ui.session.SessionLogScreen
import com.hereliesaz.geministrator.android.ui.settings.PromptEditorScreen
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel

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
            MainSessionView(mainViewModel, projectViewModel, navController)
        }
        composable("save_file") {
            FileSaveScreen()
        }
        composable("settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateToPrompts = { navController.navigate("prompts") }
            )
        }
        composable("history") {
            HistoryScreen(onSessionClick = { sessionId ->
                navController.navigate("session_log/$sessionId")
            })
        }
        composable("session_log/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                SessionLogScreen(sessionId = sessionId)
            }
        }
        composable("diff/{filePath}") { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath")
            if (filePath != null) {
                DiffScreen(filePath = filePath)
            }
        }
        composable("prompts") {
            PromptEditorScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}