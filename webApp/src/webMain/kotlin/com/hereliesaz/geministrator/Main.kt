package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi
import com.hereliesaz.geministrator.workflow.GitHubActionsExecutorIntegration
import com.hereliesaz.geministrator.workflow.GitHubRestActionsClient
import com.hereliesaz.geministrator.workflow.GitHubTokenProvider
import com.hereliesaz.geministrator.workflow.TaskExecutorIntegrationRegistry
import kotlinx.browser.window

private const val JULES_API_KEY_STORAGE_KEY = "haive.julesApiKey"
private const val GITHUB_TOKEN_STORAGE_KEY = "haive.githubToken"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        var julesKey by remember { mutableStateOf(window.localStorage.getItem(JULES_API_KEY_STORAGE_KEY)) }
        var continueWithoutJules by remember { mutableStateOf(false) }

        val reconfigure: (String) -> Unit = {
            window.localStorage.removeItem(JULES_API_KEY_STORAGE_KEY)
            julesKey = null
            continueWithoutJules = false
        }

        when {
            julesKey != null -> App(
                providers = configuredWebProviders(julesKey),
                executorIntegrations = configuredWebExecutorIntegrations(
                    githubToken = window.localStorage.getItem(GITHUB_TOKEN_STORAGE_KEY),
                ),
                onReconfigureProvider = reconfigure,
            )
            continueWithoutJules -> App(
                providers = emptyList(),
                executorIntegrations = configuredWebExecutorIntegrations(
                    githubToken = window.localStorage.getItem(GITHUB_TOKEN_STORAGE_KEY),
                ),
                onReconfigureProvider = reconfigure,
            )
            else -> WebJulesCredentialSetup(
                onSave = { key ->
                    window.localStorage.setItem(JULES_API_KEY_STORAGE_KEY, key)
                    julesKey = key
                },
                onContinueWithoutJules = { continueWithoutJules = true },
            )
        }
    }
}

@Composable
private fun WebJulesCredentialSetup(
    onSave: (String) -> Unit,
    onContinueWithoutJules: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("CONNECT JULES", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Enter your Jules API key. The key is stored in browser localStorage and never enters workflow persistence.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; errorMessage = null },
                    label = { Text("Jules API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                )
                Button(
                    onClick = {
                        val clean = apiKey.trim()
                        if (clean.isEmpty()) {
                            errorMessage = "Jules API key is required"
                        } else {
                            onSave(clean)
                        }
                    },
                    enabled = apiKey.isNotBlank(),
                ) {
                    Text("SAVE + CONTINUE")
                }
                OutlinedButton(onClick = onContinueWithoutJules) {
                    Text("CONTINUE WITHOUT JULES")
                }
            }
        }
    }
}

internal fun configuredWebProviders(julesApiKey: String?): List<AgentProvider> {
    val apiKey = julesApiKey?.trim()?.takeIf(String::isNotEmpty) ?: return emptyList()
    return listOf(
        JulesProvider(
            JulesRestApi(
                JulesApiKeyProvider { apiKey },
            ),
        ),
    )
}

internal fun configuredWebExecutorIntegrations(githubToken: String?): TaskExecutorIntegrationRegistry {
    val token = githubToken?.trim()?.takeIf(String::isNotEmpty)
        ?: return TaskExecutorIntegrationRegistry.Empty
    return TaskExecutorIntegrationRegistry(
        listOf(
            GitHubActionsExecutorIntegration(
                GitHubRestActionsClient(
                    tokenProvider = GitHubTokenProvider { token },
                ),
            ),
        ),
    )
}
