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

        setContent {
            App(providers = providers)
        }
    }
}
