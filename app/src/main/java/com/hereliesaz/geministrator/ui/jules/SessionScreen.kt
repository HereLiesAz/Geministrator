package com.hereliesaz.geministrator.ui.jules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jules.apiclient.Activity
import com.jules.apiclient.AgentResponseActivity
import com.jules.apiclient.PlanActivity
import com.jules.apiclient.ToolCallActivity
import com.jules.apiclient.ToolOutputActivity
import com.jules.apiclient.UserMessageActivity
import androidx.compose.material3.Card

@Composable
fun SessionScreen() {
    val viewModel: SessionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadActivities()
    }

    Scaffold(
        bottomBar = {
            SendMessageBar(
                onSendMessage = { viewModel.sendMessage(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadActivities() }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiInteraction(
                        geminiResponse = uiState.geminiResponse,
                        onAskGemini = { viewModel.askGemini(it) }
                    )
                    DecompositionSection(
                        uiState = uiState,
                        onDecompose = { viewModel.decomposeTask(it) }
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp),
                        reverseLayout = true
                    ) {
                        items(uiState.activities.reversed()) { activity ->
                            ActivityItem(activity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecompositionSection(
    uiState: SessionUiState,
    onDecompose: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Decompose Task", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter a high-level task") },
                modifier = Modifier.weight(1f),
                maxLines = 5
            )
            IconButton(
                onClick = {
                    onDecompose(text)
                    text = ""
                },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Decompose task"
                )
            }
        }
        if (uiState.subTasks.isNotEmpty()) {
            Text("Sub-tasks:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(uiState.subTasks) { subTask ->
                    Text(subTask, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (activity) {
                is UserMessageActivity -> {
                    Text("You", style = MaterialTheme.typography.titleMedium)
                    Text(activity.prompt)
                }
                is AgentResponseActivity -> {
                    Text("Agent", style = MaterialTheme.typography.titleMedium)
                    Text(activity.response)
                }
                is ToolCallActivity -> {
                    Text("Tool Call: ${activity.toolName}", style = MaterialTheme.typography.titleMedium)
                    Text(activity.args)
                }
                is ToolOutputActivity -> {
                    Text("Tool Output: ${activity.toolName}", style = MaterialTheme.typography.titleMedium)
                    Text(activity.output)
                }
                is PlanActivity -> {
                    Text("Plan", style = MaterialTheme.typography.titleMedium)
                    Text(activity.plan)
                }
            }
        }
    }
}

@Composable
fun SendMessageBar(
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Send a message to Jules") },
            modifier = Modifier.weight(1f),
            maxLines = 5
        )
        IconButton(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            enabled = text.isNotBlank()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message"
            )
        }
    }
}

@Composable
fun GeminiInteraction(
    geminiResponse: String?,
    onAskGemini: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Ask Gemini", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Send a message to Gemini") },
                modifier = Modifier.weight(1f),
                maxLines = 5
            )
            IconButton(
                onClick = {
                    onAskGemini(text)
                    text = ""
                },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message to Gemini"
                )
            }
        }
        geminiResponse?.let {
            Text("Gemini Response:", style = MaterialTheme.typography.titleMedium)
            Text(it)
        }
    }
}