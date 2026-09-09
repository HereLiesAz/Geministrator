package com.hereliesaz.geministrator

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi
import com.hereliesaz.geministrator.workflow.GitHubActionsExecutorIntegration
import com.hereliesaz.geministrator.workflow.GitHubRestActionsClient
import com.hereliesaz.geministrator.workflow.GitHubTokenProvider
import com.hereliesaz.geministrator.workflow.TaskExecutorIntegrationRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

fun main() {
    val julesKey = System.getenv("JULES_API_KEY")?.takeIf(String::isNotBlank)
    val providers = julesKey?.let { key ->
        listOf(
            JulesProvider(
                JulesRestApi(
                    JulesApiKeyProvider { key },
                ),
            ),
        )
    }.orEmpty()

    val githubToken = System.getenv("GITHUB_TOKEN")?.takeIf(String::isNotBlank)
    val githubClient = githubToken?.let {
        HttpClient(CIO)
    }
    val executorIntegrations = if (githubToken != null && githubClient != null) {
        TaskExecutorIntegrationRegistry(
            listOf(
                GitHubActionsExecutorIntegration(
                    GitHubRestActionsClient(
                        httpClient = githubClient,
                        tokenProvider = GitHubTokenProvider { githubToken },
                    ),
                ),
            ),
        )
    } else {
        TaskExecutorIntegrationRegistry.Empty
    }

    try {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "The Haive",
                state = rememberWindowState(width = 1180.dp, height = 760.dp),
            ) {
                App(
                    providers = providers,
                    executorIntegrations = executorIntegrations,
                )
            }
        }
    } finally {
        githubClient?.close()
    }
}
