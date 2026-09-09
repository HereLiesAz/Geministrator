package com.hereliesaz.geministrator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.displayName
import com.hereliesaz.geministrator.domain.effectiveExecutor

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

    val role = taskRun.assignedRoleId?.let { roleId -> liveWorkflow.roles.firstOrNull { it.id == roleId } }
    val executor = taskRun.executor ?: task.effectiveExecutor()
    val identity = role?.name ?: executor.displayName()
    Column(
        modifier = modifier
            .background(Azphalt.Ink)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("INSPECTOR", style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text(identity.uppercase(), style = AzphaltType.section, color = Azphalt.White)
        Text(task.objective, style = AzphaltType.body, color = Azphalt.White)
        LiveInspectorLine("STATE", taskRun.status.name)
        LiveInspectorLine("EXECUTOR", executor.displayName())
        LiveInspectorLine("EXECUTOR REF", executor.reference())
        LiveInspectorLine("RESPONSIBLE ROLE", taskRun.assignedRoleId?.value ?: task.roleId?.value ?: "—")
        LiveInspectorLine("PROVIDER", taskRun.assignedProviderId?.value ?: "—")
        LiveInspectorLine("ATTEMPT", taskRun.attempt.toString())
        LiveInspectorLine("PROVIDER RUN", taskRun.providerRunId?.value ?: "—")
        LiveInspectorLine("EXTERNAL RUN", taskRun.externalRunId ?: "—")
        taskRun.progress?.let { progress ->
            LiveInspectorLine("PROGRESS", "${(progress * 100f).toInt()}%")
        }
        taskRun.progressMessage?.takeIf(String::isNotBlank)?.let {
            LiveInspectorLine("NOW", it)
        }
        taskRun.blockingReason?.let {
            LiveInspectorLine("BLOCKED", "${it.code} · ${it.message}")
        }
        LiveInspectorLine("ARTIFACTS", taskRun.artifacts.size.toString())
        taskRun.artifacts.forEachIndexed { index, artifact ->
            val reference = artifact.uri
                ?: artifact.textContent?.takeIf(String::isNotBlank)?.let { "inline evidence" }
                ?: "stored evidence"
            LiveInspectorLine(
                "ARTIFACT ${index + 1} · ${artifact.kind.name}",
                "${artifact.label} · $reference",
            )
        }
        if (taskRun.assignedProviderId != null) {
            AzphaltPill("Message agent", "message-$selectedTaskId", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun TaskExecutor.reference(): String = when (this) {
    is TaskExecutor.RoleAgent -> roleId.value
    is TaskExecutor.GitHubAction -> listOfNotNull(workflow, ref).joinToString(" @ ")
    is TaskExecutor.TestRunner -> command ?: "default runner"
    is TaskExecutor.Deployment -> environment
    is TaskExecutor.RepositoryOperation -> operation
    is TaskExecutor.HumanApproval -> label
    is TaskExecutor.ExternalService -> listOfNotNull(service, operation).joinToString(" · ")
    is TaskExecutor.NestedWorkflow -> workflowDefinitionId.value
}

@Composable
private fun LiveInspectorLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text(value, style = AzphaltType.body, color = Azphalt.White)
    }
}
