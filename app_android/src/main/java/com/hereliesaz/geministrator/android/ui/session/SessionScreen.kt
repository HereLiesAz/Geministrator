package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.components.MarkdownText
import com.hereliesaz.geministrator.android.ui.components.ShimmerPlaceholder

@Composable
fun SessionScreen(sessionViewModel: SessionViewModel = viewModel()) {
    val uiState by sessionViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.logEntries.size) {
        if (uiState.logEntries.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logEntries.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = uiState.logEntries, key = { it.id }) { entry ->
                LogEntryItem(entry)
            }
        }

        StatusFooter(status = uiState.status)
    }
}

@Composable
fun LogEntryItem(entry: LogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                text = "${entry.agent.name}:",
                color = entry.agent.color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " ${entry.message}",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (entry.content != null) {
            MarkdownText(text = entry.content)
        }

        if (entry.isWorking) {
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}

@Composable
private fun StatusFooter(status: WorkflowStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            WorkflowStatus.RUNNING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    Text("  Workflow in progress...", modifier = Modifier.padding(start = 8.dp))
                }
            }

            WorkflowStatus.SUCCESS -> Text("Workflow Completed Successfully")
            WorkflowStatus.FAILURE -> Text(
                "Workflow Failed",
                color = MaterialTheme.colorScheme.error
            )

            WorkflowStatus.AWAITING_INPUT -> Text(
                "Awaiting user input...",
                color = MaterialTheme.colorScheme.primary
            )

            WorkflowStatus.IDLE -> Text("Session ready.")
        }
    }
}