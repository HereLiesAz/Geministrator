package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun IdeScreen(
    setLoading: (Boolean) -> Unit,
    viewModel: IdeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCommitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = uiState.fileContent,
            modifier = Modifier.weight(1f)
        )
        // TODO: This should be the Sora Editor, not a Text composable
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.onRunClicked() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Run")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showCommitDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Commit")
            }
        }
    }

    if (showCommitDialog) {
        var commitMessage by remember { mutableStateOf("") }
        CommitDialog(
            commitMessage = commitMessage,
            onCommitMessageChanged = { commitMessage = it },
            onCommit = {
                viewModel.onCommitClicked(commitMessage)
                showCommitDialog = false
            },
            onDismiss = { showCommitDialog = false }
        )
    }

    uiState.error?.let { error ->
        Snackbar(
            action = {
                Button(onClick = { viewModel.onErrorShown() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}