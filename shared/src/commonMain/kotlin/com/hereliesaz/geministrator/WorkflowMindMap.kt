package com.hereliesaz.geministrator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowMap
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun

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
