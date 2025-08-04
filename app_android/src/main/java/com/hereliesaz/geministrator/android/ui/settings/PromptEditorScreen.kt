package com.hereliesaz.geministrator.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptEditorScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Prompts Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.promptsJsonString,
                onValueChange = { settingsViewModel.onPromptsChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("prompts.json") },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { settingsViewModel.resetPrompts() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset to Defaults")
                }
                Button(
                    onClick = { settingsViewModel.savePrompts() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.promptsDirty
                ) {
                    Text("Save Prompts")
                }
            }
        }
    }
}