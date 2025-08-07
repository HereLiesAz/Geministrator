package com.hereliesaz.geministrator.android.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    projectViewModel: ProjectViewModel,
    onNavigateToFile: (String) -> Unit,
) {
    val viewModel: FileExplorerViewModel = viewModel(
        factory = FileExplorerViewModel.provideFactory(projectViewModel)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentPath.relativeTo(uiState.projectRoot).path.ifEmpty { "." },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (uiState.canNavigateUp) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Up"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val error = uiState.error
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = uiState.files, key = { it.absolutePath }) { file ->
                FileListItem(
                    file = file,
                    onFileClicked = { clickedFile ->
                        val relativePath = clickedFile.relativeTo(uiState.projectRoot).path
                        val encodedPath =
                            URLEncoder.encode(relativePath, StandardCharsets.UTF_8.toString())
                        onNavigateToFile(encodedPath)
                    },
                    onDirectoryClicked = { clickedDirectory -> viewModel.navigateTo(clickedDirectory) }
                )
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: File,
    onFileClicked: (File) -> Unit,
    onDirectoryClicked: (File) -> Unit,
) {
    val isDirectory = file.isDirectory
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isDirectory) onDirectoryClicked(file) else onFileClicked(file)
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
        Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
    }
}