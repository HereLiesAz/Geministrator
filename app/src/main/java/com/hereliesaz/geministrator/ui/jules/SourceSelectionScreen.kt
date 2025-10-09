package com.hereliesaz.geministrator.ui.jules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SourceSelectionScreen(
    onSessionCreated: (String) -> Unit
) {
    val viewModel: JulesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState.sources.isEmpty()) {
            viewModel.loadSources()
        }
    }

    if (uiState.showCreateSessionDialog) {
        CreateSessionDialog(
            onDismiss = { viewModel.dismissCreateSessionDialog() },
            onCreateSession = { title, prompt ->
                viewModel.createSession(title, prompt)
            }
        )
    }

    uiState.createdSession?.let {
        LaunchedEffect(it) {
            onSessionCreated(it.id)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.loadSources() }) {
                    Text("Retry")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = Modifier.padding(16.dp)
            ) {
                items(uiState.sources) { source ->
                    Text(
                        text = source.githubRepo?.repo ?: source.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSourceSelected(source) }
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}