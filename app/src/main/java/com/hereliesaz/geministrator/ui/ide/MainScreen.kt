package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.ui.navigation.GeministratorNavRail

@Composable
fun IdeScreen() {
    // TODO: Implement a real ViewModel for this screen
    val navController = rememberNavController()

    Row(modifier = Modifier.fillMaxSize()) {
        GeministratorNavRail(
            isLoading = false, // TODO: Replace with actual loading state from ViewModel
            onNavigate = { destination ->
                navController.navigate(destination) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        GeministratorNavHost(
            navController = navController,
            modifier = Modifier.weight(1f),
            setLoading = {} // TODO: Replace with actual setLoading from ViewModel
        )
    }
}