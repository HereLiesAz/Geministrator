package com.hereliesaz.geministrator.android.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToPrompts: () -> Unit,
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val themeOptions = listOf("Light", "Dark", "System")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("API Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.apiKey,
            onValueChange = { settingsViewModel.onApiKeyChange(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Theme", style = MaterialTheme.typography.titleMedium)
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

        Text("Customization", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onNavigateToPrompts,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View & Edit Agent Prompts")
        }


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