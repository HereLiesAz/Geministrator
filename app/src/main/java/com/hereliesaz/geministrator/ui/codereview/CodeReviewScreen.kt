package com.hereliesaz.geministrator.ui.codereview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

@Composable
fun CodeReviewScreen(sessionId: String) {
    val viewModel: CodeReviewViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var prNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = owner,
            onValueChange = { owner = it },
            label = { Text("Owner") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = repo,
            onValueChange = { repo = it },
            label = { Text("Repo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = prNumber,
            onValueChange = { prNumber = it },
            label = { Text("PR Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.reviewPullRequest(owner, repo, prNumber.toInt(), sessionId, "user") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Review")
        }
        if (uiState.isLoading) {
            Text("Reviewing...")
        }
        uiState.error?.let {
            Text("Error: $it")
        }
        uiState.reviewResult?.let {
            Text("Review Result: $it")
        }
    }
}
