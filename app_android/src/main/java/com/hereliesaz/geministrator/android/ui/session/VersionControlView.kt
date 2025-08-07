package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VersionControlView(
    sessionViewModel: SessionViewModel = viewModel(),
    navController: androidx.navigation.NavController
) {
    val uiState by sessionViewModel.uiState.collectAsState()
    val gitStatus = uiState.gitStatus

    LaunchedEffect(Unit) {
        sessionViewModel.getGitStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (gitStatus != null) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (gitStatus.untracked.isNotEmpty()) {
                    item {
                        Text("Untracked Files", style = MaterialTheme.typography.titleMedium)
                    }
                    items(gitStatus.untracked.toList()) { file ->
                        FileItem(
                            file = file,
                            onStage = { /* TODO */ },
                            onUnstage = { /* TODO */ },
                            onFileClick = { navController.navigate("diff/$file") }
                        )
                    }
                }
                if (gitStatus.modified.isNotEmpty()) {
                    item {
                        Text("Modified Files", style = MaterialTheme.typography.titleMedium)
                    }
                    items(gitStatus.modified.toList()) { file ->
                        FileItem(
                            file = file,
                            onStage = { /* TODO */ },
                            onUnstage = { /* TODO */ },
                            onFileClick = { navController.navigate("diff/$file") }
                        )
                    }
                }
            }
            Row {
                Button(onClick = { /* TODO: Stage selected files */ }) {
                    Text("Stage")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* TODO: Unstage selected files */ }) {
                    Text("Unstage")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun FileItem(
    file: String,
    onStage: (String) -> Unit,
    onUnstage: (String) -> Unit,
    onFileClick: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onFileClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { isChecked = it }
        )
        Text(text = file, modifier = Modifier.padding(start = 8.dp))
    }
}
