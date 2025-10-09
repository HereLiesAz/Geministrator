package com.hereliesaz.geministrator.ui.jules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadActivities() }) {
                        Text("Retry")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = Modifier.padding(16.dp),
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

@Composable
fun ActivityItem(activity: Activity) {
    // TODO: Create a more sophisticated representation of an activity
    Text(
        text = activity.name,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
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