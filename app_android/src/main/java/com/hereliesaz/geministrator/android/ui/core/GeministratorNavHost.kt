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
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.settings.SettingsScreen
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun GeministratorNavHost(
    navController: NavHostController,
    projectViewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "explorer",
        modifier = modifier
    ) {
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
                onNavigateToPrompts = { }
            )
        }
    }
}