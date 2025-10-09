package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.ui.navigation.GeministratorNavRail
import com.hereliesaz.geministrator.ui.project.ProjectViewModel

@Composable
fun MainScreen(projectViewModel: ProjectViewModel) {
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
            projectViewModel = projectViewModel,
            modifier = Modifier.weight(1f)
        )
    }
}