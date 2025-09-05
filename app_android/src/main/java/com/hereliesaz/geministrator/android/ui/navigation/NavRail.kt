package com.hereliesaz.geministrator.android.ui.navigation

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.AzNavRail

@Composable
fun GeministratorNavRail(
    onNavigate: (String) -> Unit,
    currentDestination: String
) {
    AzNavRail {
        azRailItem(
            id = "sessions",
            text = "Sessions",
            onClick = { onNavigate("sessions") }
        )
        azRailItem(
            id = "explorer",
            text = "Explorer",
            onClick = { onNavigate("explorer") }
        )
        azRailItem(
            id = "settings",
            text = "Settings",
            onClick = { onNavigate("settings") }
        )
        azRailItem(
            id = "history",
            text = "History",
            onClick = { onNavigate("history") }
        )
    }
}