package com.hereliesaz.geministrator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowBand
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowEdge
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowMap
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowNode
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowState
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun

private fun WorkState.toH2g2State(): H2g2WorkflowState = when (this) {
    WorkState.Complete -> H2g2WorkflowState.Complete
    WorkState.Working -> H2g2WorkflowState.Active
    WorkState.Waiting -> H2g2WorkflowState.Pending
    WorkState.Gate -> H2g2WorkflowState.Gate
    WorkState.Blocked -> H2g2WorkflowState.Blocked
}

private val WorkflowNodes = ActiveWorkflow.associate { work ->
    work.id to H2g2WorkflowNode(
        id = work.id,
        label = work.position,
        subtitle = work.assignment,
        hueSeed = work.position,
        motionSeed = work.position,
        motion = roleMotion(work.position),
        state = work.state.toH2g2State(),
        progress = work.progress,
        injected = work.injectedReason != null,
        detail = buildString {
            work.staffing?.let { append("Staffed by $it") }
            work.detail?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
            work.injectedReason?.let {
                if (isNotEmpty()) append(" · ")
                append("Auto-assigned: $it")
            }
        }.ifBlank { null },
    )
}

private fun node(id: String): H2g2WorkflowNode = checkNotNull(WorkflowNodes[id])

private val GeministratorBands = listOf(
    H2g2WorkflowBand(listOf(node("product"))),
    H2g2WorkflowBand(listOf(node("architect"))),
    H2g2WorkflowBand(listOf(node("pre-code"), node("epa"))),
    H2g2WorkflowBand(listOf(node("implementation"))),
    H2g2WorkflowBand(listOf(node("post-code"))),
    H2g2WorkflowBand(listOf(node("qa"))),
    H2g2WorkflowBand(listOf(node("review"))),
    H2g2WorkflowBand(listOf(node("release"))),
)

private val GeministratorEdges = listOf(
    H2g2WorkflowEdge("product", "architect"),
    H2g2WorkflowEdge("architect", "pre-code"),
    H2g2WorkflowEdge("architect", "epa"),
    H2g2WorkflowEdge("pre-code", "implementation"),
    H2g2WorkflowEdge("epa", "implementation"),
    H2g2WorkflowEdge("implementation", "post-code"),
    H2g2WorkflowEdge("post-code", "qa"),
    H2g2WorkflowEdge("qa", "review"),
    H2g2WorkflowEdge("review", "release"),
)

/** Current sample-run surface while the control room is still bootstrapped from [ActiveWorkflow]. */
@Composable
internal fun GeministratorWorkflowMindMap(
    selectedTaskId: String?,
    onTaskSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    H2g2WorkflowMap(
        bands = GeministratorBands,
        edges = GeministratorEdges,
        selectedId = selectedTaskId,
        onNodeSelected = { onTaskSelected(it.id) },
        modifier = modifier,
    )
}

/**
 * Live runtime entry point. The rendered map is a projection of workflow/run state rather than a
 * parallel UI model, so provider assignment, blocking, retries and progress can move the same nodes.
 */
@Composable
internal fun GeministratorWorkflowMindMap(
    definition: WorkflowDefinition,
    run: WorkflowRun,
    roles: Collection<RoleDefinition>,
    selectedTaskId: String?,
    onTaskSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val projection = remember(definition, run, roles) {
        projectWorkflowMindMap(definition, run, roles)
    }
    H2g2WorkflowMap(
        bands = projection.bands,
        edges = projection.edges,
        selectedId = selectedTaskId,
        onNodeSelected = { onTaskSelected(it.id) },
        modifier = modifier,
    )
}
