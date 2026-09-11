package com.hereliesaz.haive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun JulesCredentialSetup(
    onSave: (String) -> Unit,
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
                    "Enter your Jules API key. The Haive encrypts it with Android Keystore before storing it on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        errorMessage = null
                    },
                    label = { Text("Jules API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { message ->
                        { Text(message) }
                    },
                )
                Button(
                    onClick = {
                        val clean = apiKey.trim()
                        if (clean.isEmpty()) {
                            errorMessage = "Jules API key is required"
                        } else {
                            runCatching { onSave(clean) }
                                .onFailure { failure ->
                                    errorMessage = failure.message ?: "Unable to store Jules API key"
                                }
                        }
                    },
                    enabled = apiKey.isNotBlank(),
                ) {
                    Text("SAVE + CONTINUE")
                }
            }
        }
    }
}
