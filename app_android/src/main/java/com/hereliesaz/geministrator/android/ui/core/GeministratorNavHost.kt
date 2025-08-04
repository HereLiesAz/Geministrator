package com.hereliesaz.geministrator.android.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hereliesaz.geministrator.android.ui.file.FileSaveScreen
import com.hereliesaz.geministrator.android.ui.main.MainSessionView
import com.hereliesaz.geministrator.android.ui.main.MainViewModel
import com.hereliesaz.geministrator.android.ui.navigation.HistoryScreen
import com.hereliesaz.geministrator.android.ui.navigation.SettingsScreen
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel

@Composable
fun GeministratorNavHost(
    navController: NavHostController,
    projectViewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val mainViewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "sessions",
        modifier = modifier
    ) {
        composable("sessions") {
            MainSessionView(mainViewModel, projectViewModel)
        }
        composable("save_file") {
            FileSaveScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
        composable("history") {
            HistoryScreen()
        }
    }
}