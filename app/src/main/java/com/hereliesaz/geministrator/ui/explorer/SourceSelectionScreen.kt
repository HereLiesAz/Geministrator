package com.hereliesaz.geministrator.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.geministrator.view.CreateSessionDialog

@Composable
fun SourceSelectionScreen(
    onSessionCreated: (String) -> Unit,
    viewModel: SourceSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading sources...")
        }
    } else if (uiState.error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Error: ${uiState.error}")
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(uiState.sources) { source ->
                Text(
                    text = source.name,
                    modifier = Modifier.clickable {
                        // TODO: Show session creation dialog
                    }
                )
            }
        }
    }
}
