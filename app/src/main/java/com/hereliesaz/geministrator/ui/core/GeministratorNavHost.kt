package com.hereliesaz.geministrator.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        startDestination = "cmd",
        modifier = modifier
    ) {
        composable("settings") {
            SettingsScreen(
                setLoading = setLoading,
                onNavigateToRoles = { navController.navigate("roles-settings") }
            )
        }
        composable("roles-settings") {
            com.hereliesaz.geministrator.ui.settings.RolesSettingsScreen()
        }
        composable("cmd") {
            CmdScreen(setLoading = setLoading)
        }
    }
}
