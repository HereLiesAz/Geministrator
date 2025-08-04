package com.hereliesaz.geministrator.android.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val uiState by settingsViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings")
        OutlinedTextField(
            value = uiState.apiKey,
            onValueChange = { settingsViewModel.onApiKeyChange(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { settingsViewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.apiKey.isNotBlank()
        ) {
            Text("Save Settings")
        }
    }
}