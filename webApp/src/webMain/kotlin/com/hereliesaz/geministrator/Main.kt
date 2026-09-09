package com.hereliesaz.geministrator

import androidx.compose.ui.ExperimentalComposeUiApi
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
    val providers = configuredWebProviders(
        julesApiKey = window.localStorage.getItem(JULES_API_KEY_STORAGE_KEY),
    )
    val executorIntegrations = configuredWebExecutorIntegrations(
        githubToken = window.localStorage.getItem(GITHUB_TOKEN_STORAGE_KEY),
    )

    ComposeViewport(viewportContainerId = "webApp") {
        App(
            providers = providers,
            executorIntegrations = executorIntegrations,
        )
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
