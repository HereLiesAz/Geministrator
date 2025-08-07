package com.hereliesaz.geministrator.android.ui.history

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.session.LogEntry
import com.hereliesaz.geministrator.android.ui.session.LogEntryItem
import com.hereliesaz.geministrator.android.ui.theme.Agent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: HistoryDetailViewModel = viewModel(
        factory = HistoryDetailViewModel.provideFactory(application, sessionId)
    )
    val sessionWithLogs by viewModel.sessionWithLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        sessionWithLogs?.let { details ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text("Prompt:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            details.session.initialPrompt,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Status: ${details.session.status}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item { HorizontalDivider() }

                items(details.logs) { logEntity ->
                    // Adapt the entity to the UI model
                    LogEntryItem(
                        entry = LogEntry(
                            message = logEntity.message,
                            agent = Agent.fromString(logEntity.agent),
                            content = logEntity.content
                        )
                    )
                }
            }
        }
    }
}