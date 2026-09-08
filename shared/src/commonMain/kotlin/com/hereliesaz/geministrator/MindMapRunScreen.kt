package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowRunStatus

@Composable
internal fun MindMapRunScreen(
    modifier: Modifier,
    selectedTaskId: String?,
    onTaskSelected: (String) -> Unit,
    onLaunchWorkflow: (String, String) -> Unit,
    compact: Boolean,
    runtimeState: ApplicationRuntimeState,
) {
    val liveWorkflow = (runtimeState as? ApplicationRuntimeState.Live)?.presentation
    val activityEntrance = remember { AzphaltEntrance.childBand() }
    var projectName by remember(runtimeState) {
        mutableStateOf((runtimeState as? ApplicationRuntimeState.NoRun)?.project?.name.orEmpty())
    }
    var objective by remember(runtimeState) { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(if (compact) 16.dp else 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (liveWorkflow == null) {
            RuntimeStateRecord(runtimeState, compact)
            if (runtimeState == ApplicationRuntimeState.NoProject || runtimeState is ApplicationRuntimeState.NoRun) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Objective") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onLaunchWorkflow(projectName.trim(), objective.trim()) },
                    enabled = projectName.isNotBlank() && objective.isNotBlank(),
                ) {
                    Text(if (runtimeState is ApplicationRuntimeState.NoRun) "START RUN" else "CREATE PROJECT + START RUN")
                }
            }
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        val run = liveWorkflow.run
        val completed = run.taskRuns.values.count { it.status == TaskRunStatus.Completed }
        val total = run.taskRuns.size

        Text(
            run.objective.uppercase(),
            style = if (compact) AzphaltType.section else AzphaltType.hero,
            color = Azphalt.currentGround.onPage,
        )
        Text(
            liveWorkflow.definition.name.uppercase(),
            style = AzphaltType.eyebrow,
            color = Azphalt.currentGround.onPage,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AzphaltPill(
                run.status.name,
                "run-status",
                endCap = "$completed/$total",
                onClick = {},
            )
        }

        val awaitingHuman = run.status == WorkflowRunStatus.AwaitingHuman
        AzphaltRecord(
            seed = "owner-attention",
            eyebrow = "Owner attention",
            title = if (awaitingHuman) "Decision required" else "No decision required",
            body = if (awaitingHuman) {
                "A workflow gate is waiting for human input."
            } else {
                "Runtime is advancing without human intervention."
            },
            endCap = if (awaitingHuman) "Required" else "Clear",
        )

        Text("COMPANY EXECUTION", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        GeministratorWorkflowMindMap(
            definition = liveWorkflow.definition,
            run = run,
            roles = liveWorkflow.roles,
            selectedTaskId = selectedTaskId,
            onTaskSelected = onTaskSelected,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("COMPANY ACTIVITY", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        val recentTasks = liveWorkflow.definition.tasks
            .mapNotNull { task -> run.taskRuns[task.id]?.let { taskRun -> task to taskRun } }
            .sortedByDescending { (_, taskRun) -> taskRun.status.activityRank() }
            .take(8)
        recentTasks.forEachIndexed { index, (task, taskRun) ->
            val detail = buildString {
                append(taskRun.status.name)
                taskRun.progressMessage?.takeIf(String::isNotBlank)?.let {
                    append(" · ")
                    append(it)
                }
                taskRun.blockingReason?.message?.takeIf(String::isNotBlank)?.let {
                    append(" · ")
                    append(it)
                }
            }
            AzphaltNote(
                seed = "task-activity-${task.id.value}",
                label = task.name,
                value = detail,
                modifier = Modifier.azphaltEntrance(activityEntrance, index, recentTasks.size.coerceAtLeast(1)),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RuntimeStateRecord(state: ApplicationRuntimeState, compact: Boolean) {
    val (title, body) = when (state) {
        ApplicationRuntimeState.Loading -> "LOADING RUNTIME" to "Reading persisted workflow state."
        ApplicationRuntimeState.NoProject -> "NO PROJECT" to "Create a project and define its first objective below."
        is ApplicationRuntimeState.NoRun -> "NO ACTIVE RUN" to "${state.project.name} has no persisted workflow run. Define an objective below to start one."
        is ApplicationRuntimeState.Disconnected -> "RUNTIME DISCONNECTED" to state.message
        is ApplicationRuntimeState.ResumeFailed -> "RESUME FAILED" to state.message
        is ApplicationRuntimeState.Live -> return
    }
    Text(
        title,
        style = if (compact) AzphaltType.section else AzphaltType.hero,
        color = Azphalt.currentGround.onPage,
    )
    AzphaltRecord(
        seed = "runtime-state-$title",
        eyebrow = "Runtime",
        title = title.lowercase().replaceFirstChar(Char::uppercase),
        body = body,
        endCap = null,
    )
}

private fun TaskRunStatus.activityRank(): Int = when (this) {
    TaskRunStatus.Running, TaskRunStatus.Planning, TaskRunStatus.Verifying -> 8
    TaskRunStatus.AwaitingApproval, TaskRunStatus.Escalated -> 7
    TaskRunStatus.Retrying -> 6
    TaskRunStatus.Ready -> 5
    TaskRunStatus.Failed -> 4
    TaskRunStatus.Completed -> 3
    TaskRunStatus.Blocked -> 2
    TaskRunStatus.Created -> 1
    TaskRunStatus.Cancelled -> 0
}
