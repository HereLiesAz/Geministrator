package com.hereliesaz.geministrator.ui.navigation

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.AzNavRail

@Composable
fun GeministratorNavRail(
    onNavigate: (String) -> Unit
) {
    AzNavRail {
        azRailItem(
            id = "explorer",
            text = "Explorer",
            onClick = { onNavigate("explorer") }
        )
        azRailItem(
            id = "geministrator",
            text = "Geministrator",
            onClick = { onNavigate("geministrator") }
        )
        azRailItem(
            id = "settings",
            text = "Settings",
            onClick = { onNavigate("settings") }
        )
    }
}