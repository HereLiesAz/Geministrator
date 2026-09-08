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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MindMapRunScreen(
    modifier: Modifier,
    selectedTaskId: String?,
    onTaskSelected: (String) -> Unit,
    compact: Boolean,
    liveWorkflow: LiveWorkflowPresentation? = null,
) {
    val activityEntrance = remember { AzphaltEntrance.childBand() }
    val title = liveWorkflow?.run?.objective ?: "ADD AUTHENTICATION"
    val subtitle = liveWorkflow?.definition?.name?.uppercase() ?: "FOO · STANDARD FEATURE"
    val completed = liveWorkflow?.run?.taskRuns?.values?.count { it.status == com.hereliesaz.geministrator.domain.TaskRunStatus.Completed }
    val total = liveWorkflow?.run?.taskRuns?.size

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(if (compact) 16.dp else 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            title.uppercase(),
            style = if (compact) AzphaltType.section else AzphaltType.hero,
            color = Azphalt.currentGround.onPage,
        )
        Text(
            subtitle,
            style = AzphaltType.eyebrow,
            color = Azphalt.currentGround.onPage,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AzphaltPill("Pause", "pause", onClick = {})
            AzphaltPill(
                "Running",
                "run",
                endCap = if (completed != null && total != null) "$completed/$total" else "4/9",
                onClick = {},
            )
            AzphaltPill("Cancel", "cancel", onClick = {})
        }
        AzphaltRecord(
            seed = "owner-attention",
            eyebrow = "Owner attention",
            title = "No decision required",
            body = "Next gate: integration approval",
            endCap = "Clear",
        )

        Text("COMPANY EXECUTION", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        if (liveWorkflow != null) {
            GeministratorWorkflowMindMap(
                definition = liveWorkflow.definition,
                run = liveWorkflow.run,
                roles = liveWorkflow.roles,
                selectedTaskId = selectedTaskId,
                onTaskSelected = onTaskSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            GeministratorWorkflowMindMap(
                selectedTaskId = selectedTaskId,
                onTaskSelected = onTaskSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text("COMPANY ACTIVITY", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        listOf(
            "15:34" to "Implementation began",
            "15:32" to "EPA delivered environment",
            "15:29" to "Verification contract approved",
            "15:23" to "Pre-code tests authored",
            "15:18" to "Architecture approved",
        ).forEachIndexed { index, event ->
            AzphaltNote(
                seed = "event-$index",
                label = event.first,
                value = event.second,
                modifier = Modifier.azphaltEntrance(activityEntrance, index, 5),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
