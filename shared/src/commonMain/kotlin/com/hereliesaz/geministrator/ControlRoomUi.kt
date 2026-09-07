package com.hereliesaz.geministrator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object ControlRoomBreakpoints {
    val Wide: Dp = 820.dp
}

enum class ControlRoomDestination(val label: String) {
    Overview("Overview"),
    Runs("Runs"),
    Workflows("Workflows"),
    Company("Company"),
    Artifacts("Artifacts"),
    Inbox("Inbox"),
    Settings("Settings"),
}

internal enum class WorkState(val label: String) {
    Complete("Complete"),
    Working("Working"),
    Waiting("Waiting"),
    Gate("Gate"),
    Blocked("Blocked"),
}

internal data class WorkNode(
    val id: String,
    val position: String,
    val assignment: String,
    val state: WorkState,
    val staffing: String? = null,
    val detail: String? = null,
    val injectedReason: String? = null,
)

internal val ActiveWorkflow = listOf(
    WorkNode("product", "Product Manager", "Define authentication requirements", WorkState.Complete, detail = "Requirements approved"),
    WorkNode("architect", "Architect", "Define authentication architecture", WorkState.Complete, staffing = "Jules", detail = "Plan approved"),
    WorkNode("pre-code", "Crash Test Dummy", "Build pre-code verification contract", WorkState.Complete, staffing = "Jules", detail = "4 verification artifacts", injectedReason = "Pre-code verification policy"),
    WorkNode("epa", "EPA Representative", "Specify execution environment", WorkState.Complete, staffing = "Jules", detail = "Ephemeral · restricted network", injectedReason = "Provider environment policy"),
    WorkNode("implementation", "Implementation Engineer", "Implement authentication", WorkState.Working, staffing = "Jules", detail = "Attempt 1 · active 08:41"),
    WorkNode("post-code", "Crash Test Dummy", "Author regression tests", WorkState.Waiting, staffing = "Jules", detail = "Waiting for implementation", injectedReason = "Post-code test policy"),
    WorkNode("qa", "QA Engineer", "Falsify completion claims", WorkState.Blocked, detail = "Blocked by post-code tests"),
    WorkNode("review", "Code Reviewer", "Review implementation independently", WorkState.Blocked, detail = "Blocked by QA"),
    WorkNode("release", "Release Engineer", "Approve integration and release", WorkState.Blocked, detail = "Blocked by review"),
)

@Composable
fun ControlRoom(
    destination: ControlRoomDestination,
    onDestinationSelected: (ControlRoomDestination) -> Unit,
    selectedTaskId: String,
    onTaskSelected: (String) -> Unit,
    compact: Boolean,
    contentPadding: PaddingValues,
) {
    val ground = Azphalt.currentGround
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ground.page)
            .padding(contentPadding),
    ) {
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                CompactHeader()
                CompactNavigation(destination, onDestinationSelected)
                MainDestination(
                    destination = destination,
                    selectedTaskId = selectedTaskId,
                    onTaskSelected = onTaskSelected,
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                PillNavigation(
                    destination = destination,
                    onDestinationSelected = onDestinationSelected,
                    modifier = Modifier.width(220.dp).fillMaxHeight(),
                )
                MainDestination(
                    destination = destination,
                    selectedTaskId = selectedTaskId,
                    onTaskSelected = onTaskSelected,
                    modifier = Modifier.weight(1f),
                )
                if (destination == ControlRoomDestination.Overview || destination == ControlRoomDestination.Runs) {
                    TechnicalInspector(
                        selectedTaskId = selectedTaskId,
                        modifier = Modifier.width(310.dp).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PillNavigation(
    destination: ControlRoomDestination,
    onDestinationSelected: (ControlRoomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 14.dp, top = 22.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("GEMINISTRATOR", style = AzphaltType.section, color = Azphalt.currentGround.onPage)
        Text("COMPANY OS", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        Spacer(Modifier.height(12.dp))
        ControlRoomDestination.entries.forEachIndexed { index, item ->
            AzphaltPill(
                label = item.label,
                seed = "nav-$index-${item.name}",
                selected = item == destination,
                endCap = when (item) {
                    ControlRoomDestination.Inbox -> "2"
                    ControlRoomDestination.Runs -> "1"
                    else -> null
                },
                onClick = { onDestinationSelected(item) },
                modifier = Modifier.fillMaxWidth(if (item == destination) 0.96f else 0.84f - (index % 3) * 0.03f),
            )
        }
        Spacer(Modifier.weight(1f))
        AzphaltPill(
            label = Azphalt.currentGround.name,
            seed = "ground",
            endCap = "Reroll",
            onClick = { Azphalt.rerollGround() },
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Text("JULES · CONNECTED", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
    }
}

@Composable
private fun CompactHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("GEMINISTRATOR", style = AzphaltType.lead, color = Azphalt.currentGround.onPage)
            Text("COMPANY OS", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
        }
        AzphaltPill("Ground", "compact-ground", endCap = Azphalt.currentGround.name, onClick = { Azphalt.rerollGround() })
    }
}

@Composable
private fun CompactNavigation(
    destination: ControlRoomDestination,
    onDestinationSelected: (ControlRoomDestination) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ControlRoomDestination.entries.forEachIndexed { index, item ->
            AzphaltPill(
                label = item.label,
                seed = "compact-$index-${item.name}",
                selected = item == destination,
                endCap = if (item == ControlRoomDestination.Inbox) "2" else null,
                onClick = { onDestinationSelected(item) },
            )
        }
    }
}

@Composable
private fun MainDestination(
    destination: ControlRoomDestination,
    selectedTaskId: String,
    onTaskSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    when (destination) {
        ControlRoomDestination.Overview,
        ControlRoomDestination.Runs,
        -> RunControlRoom(modifier, selectedTaskId, onTaskSelected, compact)
        ControlRoomDestination.Workflows -> WorkflowTemplateScreen(modifier)
        ControlRoomDestination.Company -> CompanyScreen(modifier)
        ControlRoomDestination.Artifacts -> ArtifactFileManagerScreen(modifier)
        ControlRoomDestination.Inbox -> InboxScreen(modifier)
        ControlRoomDestination.Settings -> SettingsScreen(modifier)
    }
}
