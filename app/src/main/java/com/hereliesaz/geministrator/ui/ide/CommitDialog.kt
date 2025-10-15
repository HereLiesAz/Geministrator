package com.hereliesaz.geministrator.ui.ide

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

@Composable
fun CommitDialog(
    commitMessage: String,
    onCommitMessageChanged: (String) -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit Changes") },
        text = {
            TextField(
                value = commitMessage,
                onValueChange = onCommitMessageChanged,
                label = { Text("Commit Message") }
            )
        },
        confirmButton = {
            Button(onClick = onCommit) {
                Text("Commit")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
