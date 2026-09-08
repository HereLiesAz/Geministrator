package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hereliesaz.geministrator.providers.AgentProvider
import kotlinx.coroutines.flow.collectLatest

@Composable
fun App(
    providers: Collection<AgentProvider> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    var runtimeState by remember { mutableStateOf<ApplicationRuntimeState>(ApplicationRuntimeState.Loading) }

    LaunchedEffect(providers) {
        val runtime = ApplicationRuntime.create(providers = providers, scope = scope)
        runtime.state.collectLatest { runtimeState = it }
    }

    GeministratorTheme {
        var destination by remember { mutableStateOf(ControlRoomDestination.Overview) }
        var selectedTaskId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(runtimeState) {
            val live = runtimeState as? ApplicationRuntimeState.Live
            if (live == null) {
                selectedTaskId = null
            } else if (selectedTaskId != null && live.presentation.definition.tasks.none { it.id.value == selectedTaskId }) {
                selectedTaskId = null
            }
        }

        Scaffold { paddingValues ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                ControlRoom(
                    destination = destination,
                    onDestinationSelected = { destination = it },
                    selectedTaskId = selectedTaskId,
                    onTaskSelected = { taskId ->
                        selectedTaskId = if (selectedTaskId == taskId) null else taskId
                    },
                    compact = maxWidth < ControlRoomBreakpoints.Wide,
                    contentPadding = paddingValues,
                    runtimeState = runtimeState,
                )
            }
        }
    }
}

@Composable
private fun GeministratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GeministratorColors,
        content = content,
    )
}
