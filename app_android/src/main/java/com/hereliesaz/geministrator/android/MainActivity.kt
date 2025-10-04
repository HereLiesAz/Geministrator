package com.hereliesaz.geministrator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hereliesaz.geministrator.android.ui.ide.MainScreen
import com.hereliesaz.geministrator.android.ui.project.ProjectSetupScreen
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel
import com.hereliesaz.geministrator.android.ui.theme.GeministratorTheme

class MainActivity : ComponentActivity() {
    private val projectViewModel: ProjectViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val projectState by projectViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()

            GeministratorTheme(themePreference = settingsState.theme) {
                if (projectState.projectUri == null && projectState.localCachePath == null) {
                    ProjectSetupScreen(projectViewModel)
                } else {
                    MainScreen(projectViewModel = projectViewModel)
                }
            }
        }
    }
}