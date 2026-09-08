package com.hereliesaz.haive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.geministrator.App
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setContent {
            App(providers = providers)
        }
    }
}
