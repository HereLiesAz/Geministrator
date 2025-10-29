package com.hereliesaz.geministrator.ui.explorer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hereliesaz.geministrator.ui.project.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    projectViewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
) {
    val projectState by projectViewModel.uiState.collectAsStateWithLifecycle()
    val fileContent = projectState.localCachePath?.let {
        projectViewModel.readFile(filePath).getOrNull()
    } ?: "Error: Could not read file."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = filePath, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(text = fileContent)
        }
    }
}