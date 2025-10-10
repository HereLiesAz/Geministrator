package com.hereliesaz.geministrator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigateToRoles: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val themeOptions = listOf("Light", "Dark", "System")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        settingsViewModel.events.collectLatest { event ->
            when (event) {
                is SettingsViewModel.UiEvent.ShowSaveConfirmation -> {
                    snackbarHostState.showSnackbar("Settings Saved")
                }
                is SettingsViewModel.UiEvent.NavigateToLogin -> {
                    onLogout()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.username?.let { username ->
                Text("User", style = MaterialTheme.typography.titleLarge)
                Text(text = "Logged in as $username")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { settingsViewModel.logout() }) {
                    Text("Logout")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("API Key", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { settingsViewModel.onApiKeyChange(it) },
                label = { Text("Jules API Key") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = uiState.geminiApiKey,
                onValueChange = { settingsViewModel.onGeminiApiKeyChange(it) },
                label = { Text("Gemini API Key") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Gemini Settings", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = uiState.gcpProjectId,
                onValueChange = { settingsViewModel.onGcpProjectIdChange(it) },
                label = { Text("GCP Project ID") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.gcpLocation,
                onValueChange = { settingsViewModel.onGcpLocationChange(it) },
                label = { Text("GCP Location") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.geminiModelName,
                onValueChange = { settingsViewModel.onGeminiModelNameChange(it) },
                label = { Text("Gemini Model Name") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Theme", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth()) {
                themeOptions.forEach { text ->
                    Row(
                        Modifier
                            .selectable(
                                selected = (text == uiState.theme),
                                onClick = { settingsViewModel.onThemeChange(text) }
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == uiState.theme),
                            onClick = { settingsViewModel.onThemeChange(text) }
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToRoles,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage AI Roles")
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { settingsViewModel.saveSettings() },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.apiKey.isNotBlank()
            ) {
                Text("Save Settings")
            }
        }
    }
}