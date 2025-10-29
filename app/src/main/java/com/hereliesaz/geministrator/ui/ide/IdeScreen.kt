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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun IdeScreen(
    setLoading: (Boolean) -> Unit,
    viewModel: IdeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = uiState.fileContent ?: "",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.onRunClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Run")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.onCommitClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Commit")
            }
        }
    }

    if (uiState.showCommitDialog) {
        CommitDialog(
            commitMessage = uiState.commitMessage,
            onCommitMessageChanged = { viewModel.onCommitMessageChanged(it) },
            onCommit = { viewModel.onCommitConfirm() },
            onDismiss = { viewModel.onCommitDialogDismiss() }
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
