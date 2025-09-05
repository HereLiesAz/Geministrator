package com.hereliesaz.geministrator.android.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.android.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.android.ui.navigation.GeministratorNavRail
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel

@Composable
fun MainScreen(projectViewModel: ProjectViewModel) {
    val navController = rememberNavController()
    var currentDestination by remember { mutableStateOf("sessions") }

    Row(Modifier.fillMaxSize()) {
        GeministratorNavRail(
            onNavigate = { destination ->
                currentDestination = destination
                navController.navigate(destination)
            },
            currentDestination = currentDestination
        )
        GeministratorNavHost(
            navController = navController,
            projectViewModel = projectViewModel
        )
    }
}

@Composable
fun MainSessionView(
    mainViewModel: MainViewModel = viewModel(),
    projectViewModel: ProjectViewModel,
    navController: NavController
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val sessions = uiState.sessions
    val selectedIndex = uiState.selectedSessionIndex

    NewSessionDialog(
        showSheet = uiState.showNewSessionDialog,
        onDismiss = { mainViewModel.onDismissNewSessionDialog() },
        onConfirm = { prompt -> mainViewModel.startSession(prompt, projectViewModel) }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mainViewModel.onShowNewSessionDialog() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Session")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (sessions.isNotEmpty()) {
                TabRow(selectedTabIndex = selectedIndex) {
                    sessions.forEachIndexed { index, session ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { mainViewModel.selectSession(index) },
                            text = { Text(text = session.title) }
                        )
                    }
                }
                val selectedSession = sessions[selectedIndex]
                SessionScreen(
                    sessionViewModel = selectedSession.viewModel,
                    navController = navController
                )
            }
        }
    }
}