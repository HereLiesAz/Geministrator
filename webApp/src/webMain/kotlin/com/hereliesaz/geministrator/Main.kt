package com.hereliesaz.geministrator

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi
import kotlinx.browser.window

private const val JULES_API_KEY_STORAGE_KEY = "haive.julesApiKey"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val providers = configuredWebProviders(
        julesApiKey = window.localStorage.getItem(JULES_API_KEY_STORAGE_KEY),
    )

    ComposeViewport(viewportContainerId = "webApp") {
        App(providers = providers)
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
