package com.hereliesaz.geministrator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.ui.core.MainViewModel
import com.hereliesaz.geministrator.ui.navigation.GeministratorNavRail
import com.hereliesaz.geministrator.ui.theme.GeministratorTheme
import com.hereliesaz.geministrator.util.TextMateLoader

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var settingsRepository: SettingsRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        TextMateLoader.load(this)
        enableEdgeToEdge()
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = "System")
            GeministratorTheme(themePreference = theme ?: "System") {
                MainScreen(mainViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(mainViewModel: MainViewModel) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GeministratorNavRail(
                isLoading = mainUiState.isLoading,
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
                setLoading = mainViewModel::setLoading
            )
        }
    }
}