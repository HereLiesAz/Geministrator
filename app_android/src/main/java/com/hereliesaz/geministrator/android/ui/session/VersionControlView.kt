package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VersionControlView(
    sessionViewModel: SessionViewModel,
) {
    val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val gitStatus = uiState.gitStatus

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Version Control Status",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(
                onClick = { sessionViewModel.refreshGitStatus() },
                enabled = !gitStatus.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
            }
        }

        if (gitStatus.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                val unstagedFiles = gitStatus.modified + gitStatus.untracked

                if (unstagedFiles.isEmpty() && gitStatus.added.isEmpty() && gitStatus.removed.isEmpty()) {
                    item {
                        Text(
                            text = "No changes detected. Your working tree is clean.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (unstagedFiles.isNotEmpty()) {
                    item {
                        StatusSectionTitle(
                            title = "Unstaged Changes",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    items(unstagedFiles) { filePath ->
                        SelectableFileItem(
                            filePath = filePath,
                            isSelected = gitStatus.selectedForStaging.contains(filePath),
                            onToggle = { sessionViewModel.toggleFileStaging(filePath) },
                            onClick = { sessionViewModel.showDiffForFile(filePath) }
                        )
                    }
                }
                if (gitStatus.added.isNotEmpty()) {
                    item {
                        StatusSectionTitle(
                            title = "Staged Changes (Added)",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(gitStatus.added) { filePath ->
                        FileItem(
                            filePath = filePath,
                            onClick = { sessionViewModel.showDiffForFile(filePath) }
                        )
                    }
                }
                if (gitStatus.removed.isNotEmpty()) {
                    item {
                        StatusSectionTitle(
                            title = "Staged Changes (Removed)",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(gitStatus.removed) { filePath ->
                        FileItem(
                            filePath = filePath,
                            onClick = { sessionViewModel.showDiffForFile(filePath) }
                        )
                    }
                }
            }

            if (gitStatus.selectedForStaging.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { sessionViewModel.stageSelectedFiles() },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Stage ${gitStatus.selectedForStaging.size} File(s)")
                }
            }
        }
    }
}

@Composable
private fun StatusSectionTitle(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun FileItem(filePath: String, onClick: () -> Unit) {
    Text(
        text = filePath,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, bottom = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SelectableFileItem(
    filePath: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                onValueChange = { onToggle() }, // Corrected typo here
                role = Role.Checkbox
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Text(
            text = filePath,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable(onClick = onClick)
        )
    }
}