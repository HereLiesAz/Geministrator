package com.hereliesaz.geministrator.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.AzNavRail
import androidx.compose.foundation.background

import androidx.navigation.NavController

@Composable
fun GeministratorNavRail(
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    navController: NavController,
    currentRoute: String?
) {
    AzNavRail(modifier = Modifier.background(Color.Transparent)) {
        azSettings(isLoading = isLoading)
        azRailItem(
            id = "explorer",
            text = "Explorer",
            onClick = { onNavigate("explorer") }
        )
        azRailItem(
            id = "ide",
            text = "IDE",
            onClick = { onNavigate("ide") }
        )
        azRailItem(
            id = "cmd",
            text = "CMD",
            onClick = { onNavigate("cmd") }
        )
        azRailItem(
            id = "code_review",
            text = "Code Review",
            onClick = {
                val sessionId = currentRoute?.substringAfter("session/")?.substringBefore("?")
                if (sessionId != null) {
                    onNavigate("code_review/$sessionId")
                }
            }
        )
        azRailItem(
            id = "settings",
            text = "Settings",
            onClick = { onNavigate("settings") }
        )
    }
}