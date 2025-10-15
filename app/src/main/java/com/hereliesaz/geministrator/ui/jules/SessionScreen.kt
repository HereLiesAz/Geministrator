package com.hereliesaz.geministrator.ui.jules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

@Composable
fun SessionScreen(
    setLoading: (Boolean) -> Unit
) {
    val viewModel: SessionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    LaunchedEffect(Unit) {
        viewModel.loadActivities()
    }

    Scaffold(
        bottomBar = {
            SendMessageBar(
                onSendMessage = {
                    if (it.startsWith("/gemini")) {
                        viewModel.askGemini(it.substringAfter("/gemini "))
                    } else if (it.startsWith("/decompose")) {
                        viewModel.decomposeTask(it.substringAfter("/decompose "))
                    } else {
                        viewModel.sendMessage(it)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = true
                ) {
                    val reversedActivities = uiState.activities.reversed()
                    items(reversedActivities) { activity ->
                        ActivityItem(activity = activity)
                    }

                    if (uiState.subTasks.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Sub-tasks", style = MaterialTheme.typography.titleMedium)
                                    uiState.subTasks.forEach {
                                        Text(it)
                                    }
                                }
                            }
                        }
                    }

                    uiState.geminiResponse?.let {
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Gemini Response", style = MaterialTheme.typography.titleMedium)
                                    Text(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: Activity
) {
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
            label = { Text("Send a message") },
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
