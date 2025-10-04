package com.hereliesaz.geministrator.android.ui.navigation

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
            id = "settings",
            text = "Settings",
            onClick = { onNavigate("settings") }
        )
    }
}