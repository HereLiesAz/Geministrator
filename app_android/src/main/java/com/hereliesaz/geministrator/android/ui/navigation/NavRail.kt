package com.hereliesaz.geministrator.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope

@OptIn(
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class
)
fun NavigationSuiteScope.geministratorNavSuite(
    onNavigate: (String) -> Unit,
    currentDestination: String
) {
    item(
        selected = currentDestination == "sessions",
        onClick = { onNavigate("sessions") },
        icon = { Icon(Icons.Default.SmartToy, contentDescription = "Sessions") }
    )
    item(
        selected = currentDestination == "explorer",
        onClick = { onNavigate("explorer") },
        icon = { Icon(Icons.Default.Description, contentDescription = "File Explorer") }
    )
    item(
        selected = currentDestination == "settings",
        onClick = { onNavigate("settings") },
        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )
    item(
        selected = currentDestination == "history",
        onClick = { onNavigate("history") },
        icon = { Icon(Icons.Default.History, contentDescription = "History") }
    )
}