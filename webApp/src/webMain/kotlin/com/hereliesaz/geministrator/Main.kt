package com.hereliesaz.geministrator

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val providers = listOf(
        JulesProvider(
            JulesRestApi(
                JulesApiKeyProvider {
                    error("Jules provider is registered but a web API-key source has not been configured")
                },
            ),
        ),
    )
    ComposeViewport(viewportContainerId = "webApp") {
        App(providers = providers)
    }
}
