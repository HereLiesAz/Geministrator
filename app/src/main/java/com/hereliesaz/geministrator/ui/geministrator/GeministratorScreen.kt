package com.hereliesaz.geministrator.ui.geministrator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.data.model.geministrator.DelegatedTask

@Composable
fun GeministratorScreen(
    geministratorViewModel: GeministratorViewModel = viewModel()
) {
    val uiState by geministratorViewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is GeministratorUiState.Success -> {
                    PlanInputSection(
                        onPlanCreate = { geministratorViewModel.createPlan(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    state.plan?.let { plan ->
                        PlanDisplay(plan.steps)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AvailableAgents(state.availableAgents)
                }
                is GeministratorUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanInputSection(
    onPlanCreate: (String) -> Unit
) {
    var userInput by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Describe the task...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onPlanCreate(userInput) },
            enabled = userInput.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Create Plan")
        }
    }
}

@Composable
private fun PlanDisplay(tasks: List<DelegatedTask>) {
    Text("Current Plan", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            TaskCard(task)
        }
    }
}

@Composable
private fun TaskCard(task: DelegatedTask) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.description,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = task.status,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AvailableAgents(agents: List<String>) {
    Text("Available Agents", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(8.dp))
    if (agents.isEmpty()) {
        Text("No agents enabled in settings.")
    } else {
        LazyColumn {
            items(agents) { agentName ->
                Text(
                    text = "• $agentName",
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                )
            }
        }
    }
}
