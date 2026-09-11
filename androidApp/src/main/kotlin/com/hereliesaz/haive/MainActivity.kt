package com.hereliesaz.haive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hereliesaz.geministrator.App
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.jules.JulesApiKeyProvider
import com.hereliesaz.geministrator.providers.jules.JulesProvider
import com.hereliesaz.geministrator.providers.jules.JulesRestApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credentialStore = AndroidJulesCredentialStore(this)
        val storedKey = credentialStore.read()

        setContent {
            var julesKey by remember { mutableStateOf(storedKey) }
            var continueWithoutJules by remember { mutableStateOf(false) }

            when {
                julesKey != null -> App(providers = configuredAndroidProviders(julesKey))
                continueWithoutJules -> App(providers = emptyList())
                else -> JulesCredentialSetup(
                    onSave = { key ->
                        credentialStore.write(key)
                        julesKey = key
                    },
                    onContinueWithoutJules = {
                        continueWithoutJules = true
                    },
                )
            }
        }
    }
}

internal fun configuredAndroidProviders(julesApiKey: String?): List<AgentProvider> {
    val key = julesApiKey?.trim()?.takeIf(String::isNotEmpty) ?: return emptyList()
    return listOf(
        JulesProvider(
            JulesRestApi(
                JulesApiKeyProvider { key },
            ),
        ),
    )
}
