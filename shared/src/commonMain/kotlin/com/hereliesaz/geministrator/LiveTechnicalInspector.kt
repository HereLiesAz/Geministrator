package com.hereliesaz.geministrator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.geministrator.domain.TaskDefinitionId

@Composable
internal fun TechnicalInspector(
    selectedTaskId: String,
    liveWorkflow: LiveWorkflowPresentation?,
    modifier: Modifier = Modifier,
) {
    if (liveWorkflow == null) {
        TechnicalInspector(selectedTaskId = selectedTaskId, modifier = modifier)
        return
    }

    val taskId = TaskDefinitionId(selectedTaskId)
    val task = liveWorkflow.definition.tasks.firstOrNull { it.id == taskId }
    val taskRun = liveWorkflow.run.taskRuns[taskId]
    if (task == null || taskRun == null) {
        TechnicalInspector(selectedTaskId = selectedTaskId, modifier = modifier)
        return
    }

    val role = liveWorkflow.roles.firstOrNull { it.id == taskRun.assignedRoleId }
    Column(
        modifier = modifier
            .background(Azphalt.Ink)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("INSPECTOR", style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text((role?.name ?: taskRun.assignedRoleId.value).uppercase(), style = AzphaltType.section, color = Azphalt.White)
        Text(task.objective, style = AzphaltType.body, color = Azphalt.White)
        LiveInspectorLine("STATE", taskRun.status.name)
        LiveInspectorLine("STAFFING", taskRun.assignedProviderId?.value ?: "Unstaffed")
        LiveInspectorLine("ATTEMPT", taskRun.attempt.toString())
        LiveInspectorLine("PROVIDER RUN", taskRun.providerRunId?.value ?: "—")
        taskRun.progress?.let { progress ->
            LiveInspectorLine("PROGRESS", "${(progress * 100f).toInt()}%")
        }
        taskRun.progressMessage?.takeIf(String::isNotBlank)?.let {
            LiveInspectorLine("NOW", it)
        }
        taskRun.blockingReason?.let {
            LiveInspectorLine("BLOCKED", it.message)
        }
        LiveInspectorLine("ARTIFACTS", taskRun.artifacts.size.toString())
        AzphaltPill("Message agent", "message-$selectedTaskId", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LiveInspectorLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text(value, style = AzphaltType.body, color = Azphalt.White)
    }
}
