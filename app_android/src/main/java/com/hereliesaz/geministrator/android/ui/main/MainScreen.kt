package com.hereliesaz.geministrator.android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigation.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigation.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.android.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.android.ui.navigation.geministratorNavSuite
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.session.SessionScreen

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun MainScreen(projectViewModel: ProjectViewModel) {
    val navController = rememberNavController()
    val currentDestination = remember { mutableStateOf("sessions") }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            geministratorNavSuite(
                onNavigate = {
                    currentDestination.value = it
                    navController.navigate(it)
                },
                currentDestination = currentDestination.value
            )
        }
    ) {
        Scaffold { innerPadding ->
            GeministratorNavHost(
                navController = navController,
                projectViewModel = projectViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun MainSessionView(mainViewModel: MainViewModel = viewModel(), projectViewModel: ProjectViewModel) {
    val uiState by mainViewModel.uiState.collectAsState()
    val sessions = uiState.sessions
    val selectedIndex = uiState.selectedSessionIndex

    if (uiState.showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { mainViewModel.onDismissNewSessionDialog() },
            onConfirm = { prompt -> mainViewModel.startSession(prompt, projectViewModel) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedIndex) {
            sessions.forEachIndexed { index, session ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { mainViewModel.selectSession(index) },
                    text = { Text(text = session.title) }
                )
            }
            IconButton(onClick = { mainViewModel.onShowNewSessionDialog() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Session")
            }
        }
        if (sessions.isNotEmpty()) {
            val selectedSession = sessions[selectedIndex]
            Box(modifier = Modifier.weight(1f)) {
                SessionScreen(sessionViewModel = selectedSession.viewModel)
            }
        }
    }
}