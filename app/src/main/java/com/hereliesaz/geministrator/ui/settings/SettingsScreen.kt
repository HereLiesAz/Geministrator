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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    setLoading: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigateToRoles: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val themeOptions = listOf("Light", "Dark", "System")
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    LaunchedEffect(key1 = Unit) {
        settingsViewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSaveConfirmation -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.LaunchUrl -> {
                    context.startActivity(event.intent)
                }
                is UiEvent.NavigateToLogin -> {
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
                value = uiState.githubRepository,
                onValueChange = { settingsViewModel.onGithubRepositoryChange(it) },
                label = { Text("GitHub Repository") },
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
            var expanded by remember { mutableStateOf(false) }
            val geminiModels = listOf("gemini-2.5-flash", "gemini-1.5-pro-latest", "gemini-1.0-pro")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.geminiModelName,
                    onValueChange = {},
                    label = { Text("Gemini Model Name") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    geminiModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                settingsViewModel.onGeminiModelNameChange(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Integrations", style = MaterialTheme.typography.titleLarge)
            if (uiState.username == null) {
                Button(
                    onClick = { settingsViewModel.signInWithGitHub(context as android.app.Activity) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with GitHub")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { settingsViewModel.signInWithGoogle(context as android.app.Activity) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Signed in as: ${uiState.username}")
                    Button(onClick = { settingsViewModel.signOut() }) {
                        Text("Sign out")
                    }
                }
            }

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
        }
    }
}