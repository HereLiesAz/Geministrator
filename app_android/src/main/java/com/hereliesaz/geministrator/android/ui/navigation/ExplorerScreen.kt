package com.hereliesaz.geministrator.android.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.project.FileNode
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel

@Composable
fun ExplorerScreen(
    projectViewModel: ProjectViewModel = viewModel(),
    onFileClick: (String) -> Unit
) {
    val uiState by projectViewModel.uiState.collectAsState()
    val fileTree = uiState.fileTree

    LaunchedEffect(Unit) {
        projectViewModel.loadFileTree()
    }

    if (fileTree != null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                FileTreeItem(node = fileTree, onFileClick = onFileClick)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No project selected")
        }
    }
}

@Composable
fun FileTreeItem(node: FileNode, onFileClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) {
                    expanded = !expanded
                } else {
                    onFileClick(node.path)
                }
            }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null
        )
        Text(text = node.name, modifier = Modifier.padding(start = 8.dp))
    }

    if (expanded) {
        Column(modifier = Modifier.padding(start = 16.dp)) {
            node.children.forEach { child ->
                FileTreeItem(node = child, onFileClick = onFileClick)
            }
        }
    }
}
