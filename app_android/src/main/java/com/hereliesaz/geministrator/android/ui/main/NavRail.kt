package com.hereliesaz.geministrator.android.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable

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
        selected = currentDestination == "save_file",
        onClick = { onNavigate("save_file") },
        icon = { Icon(Icons.Default.Save, contentDescription = "Save File") }
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