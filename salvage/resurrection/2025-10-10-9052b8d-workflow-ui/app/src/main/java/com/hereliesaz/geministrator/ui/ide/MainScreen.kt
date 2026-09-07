package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.ui.navigation.GeministratorNavRail

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Row(modifier = Modifier.fillMaxSize()) {
        GeministratorNavRail(
            onNavigate = { destination ->
                navController.navigate(destination) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        GeministratorNavHost(
            navController = navController,
            modifier = Modifier.weight(1f)
        )
    }
}