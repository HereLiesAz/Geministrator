package com.hereliesaz.geministrator

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi

fun main() = application {
    val providers = listOf(
        JulesProvider(
            JulesRestApi(
                JulesApiKeyProvider {
                    System.getenv("JULES_API_KEY")
                        ?.takeIf(String::isNotBlank)
                        ?: error("Jules provider is registered but JULES_API_KEY is not configured")
                },
            ),
        ),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "The Haive",
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
    ) {
        App(providers = providers)
    }
}
