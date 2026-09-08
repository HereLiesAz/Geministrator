package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun App(
    liveWorkflow: LiveWorkflowPresentation? = null,
) {
    GeministratorTheme {
        var destination by remember { mutableStateOf(ControlRoomDestination.Overview) }
        var selectedTaskId by remember { mutableStateOf<String?>(null) }

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
                    liveWorkflow = liveWorkflow,
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
