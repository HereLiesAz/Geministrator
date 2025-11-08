package com.hereliesaz.geministrator.ui.jules

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jules.apiclient.Source

@Composable
fun CreateSessionDialog(
    source: Source,
    viewModel: JulesViewModel,
    onDismiss: () -> Unit,
    onSessionCreated: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Session for ${source.name}") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") }
                )
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Initial Prompt") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createSession(source.id, title, prompt, onSessionCreated)
                },
                enabled = title.isNotBlank() && prompt.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
