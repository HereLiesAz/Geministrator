package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.workflow.TaskExecutorIntegrationRegistry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun App(
    providers: Collection<AgentProvider>,
    executorIntegrations: TaskExecutorIntegrationRegistry = TaskExecutorIntegrationRegistry.Empty,
) {
    val scope = rememberCoroutineScope()
    var runtimeState by remember { mutableStateOf<ApplicationRuntimeState>(ApplicationRuntimeState.Loading) }
    var runtime by remember { mutableStateOf<ApplicationRuntime?>(null) }

    LaunchedEffect(providers, executorIntegrations) {
        runtime?.close()
        runtime = null
        runtimeState = ApplicationRuntimeState.Loading
        try {
            val created = ApplicationRuntime.create(
                providers = providers,
                scope = scope,
                executorIntegrations = executorIntegrations,
            )
            runtime = created
            created.state.collectLatest { runtimeState = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (failure: Throwable) {
            val message = failure.message?.takeIf(String::isNotBlank)
                ?: failure::class.simpleName.orEmpty().ifBlank { "Runtime bootstrap failed" }
            runtimeState = ApplicationRuntimeState.ResumeFailed(message)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runtime?.close()
            runtime = null
        }
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
                    onLaunchWorkflow = { projectName, objective ->
                        val existingProject = (runtimeState as? ApplicationRuntimeState.NoRun)?.project
                        scope.launch {
                            try {
                                runtime?.launchStarterWorkflow(
                                    projectName = projectName,
                                    objective = objective,
                                    existingProject = existingProject,
                                )
                            } catch (failure: Throwable) {
                                runtimeState = failure.toRuntimeFailureState("Workflow launch failed")
                            }
                        }
                    },
                    onApproveTask = { taskId ->
                        scope.launch {
                            try {
                                runtime?.approveTask(TaskDefinitionId(taskId))
                            } catch (failure: Throwable) {
                                runtimeState = failure.toRuntimeFailureState("Approval failed")
                            }
                        }
                    },
                    compact = maxWidth < ControlRoomBreakpoints.Wide,
                    contentPadding = paddingValues,
                    runtimeState = runtimeState,
                )
            }
        }
    }
}

private fun Throwable.toRuntimeFailureState(fallback: String): ApplicationRuntimeState.ResumeFailed {
    val message = message?.takeIf(String::isNotBlank)
        ?: this::class.simpleName.orEmpty().ifBlank { fallback }
    return ApplicationRuntimeState.ResumeFailed(message)
}

@Composable
private fun GeministratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GeministratorColors,
        content = content,
    )
}
