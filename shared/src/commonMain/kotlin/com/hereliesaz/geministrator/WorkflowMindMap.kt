package com.hereliesaz.geministrator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowBand
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowEdge
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowMap
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowMotion
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowNode
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowState

private fun WorkState.toH2g2State(): H2g2WorkflowState = when (this) {
    WorkState.Complete -> H2g2WorkflowState.Complete
    WorkState.Working -> H2g2WorkflowState.Active
    WorkState.Waiting -> H2g2WorkflowState.Pending
    WorkState.Gate -> H2g2WorkflowState.Gate
    WorkState.Blocked -> H2g2WorkflowState.Blocked
}

/**
 * Motion is part of a company position's identity. A role keeps the same physical mannerism across
 * projects and runs, independent of whichever provider happens to staff it.
 */
private fun roleMotion(role: String): H2g2WorkflowMotion = when (role) {
    "Orchestrator" -> H2g2WorkflowMotion.Orbit
    "Product Manager" -> H2g2WorkflowMotion.Nod
    "Researcher" -> H2g2WorkflowMotion.Float
    "Architect" -> H2g2WorkflowMotion.Pendulum
    "EPA Representative" -> H2g2WorkflowMotion.Hover
    "UX Designer" -> H2g2WorkflowMotion.Sway
    "Implementation Engineer" -> H2g2WorkflowMotion.Scoot
    "Crash Test Dummy" -> H2g2WorkflowMotion.Wag
    "QA Engineer" -> H2g2WorkflowMotion.Skitter
    "Adversarial Reviewer" -> H2g2WorkflowMotion.Shimmy
    "Code Reviewer" -> H2g2WorkflowMotion.Tilt
    "Recovery Engineer" -> H2g2WorkflowMotion.Bob
    "Release Engineer" -> H2g2WorkflowMotion.Pulse
    else -> H2g2WorkflowMotion.Breathe
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

@Composable
internal fun GeministratorWorkflowMindMap(
    selectedTaskId: String,
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
