package com.hereliesaz.geministrator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val GeministratorColors = darkColorScheme(
    primary = Color(0xFFB9C9FF),
    onPrimary = Color(0xFF102046),
    primaryContainer = Color(0xFF1A2A4B),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFC7C8D0),
    secondaryContainer = Color(0xFF292B31),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF1B1E24),
    onSurface = Color(0xFFE8E9ED),
    onSurfaceVariant = Color(0xFFB9BBC4),
    outline = Color(0xFF454952),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5A1F1C),
)

internal object ControlRoomBreakpoints {
    val Wide: Dp = 780.dp
}

enum class ControlRoomDestination(val label: String) {
    Overview("Overview"),
    Runs("Runs"),
    Workflows("Workflows"),
    Company("Company"),
    Inbox("Inbox"),
    Settings("Settings"),
}

internal enum class WorkState(val label: String) {
    Complete("COMPLETE"),
    Working("WORKING"),
    Waiting("WAITING"),
    Gate("GATE"),
    Blocked("BLOCKED"),
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
    WorkNode("epa", "EPA Representative", "Specify execution environment", WorkState.Complete, staffing = "Jules", detail = "Ephemeral · restricted network", injectedReason = "Selected provider requires environment planning"),
    WorkNode("implementation", "Implementation Engineer", "Implement authentication", WorkState.Working, staffing = "Jules", detail = "Attempt 1 · active 08:41"),
    WorkNode("post-code", "Crash Test Dummy", "Author regression tests", WorkState.Waiting, staffing = "Jules", detail = "Waiting for implementation", injectedReason = "Post-code test design policy"),
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
    if (compact) {
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
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
        Row(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            CompanyNavigation(
                destination = destination,
                onDestinationSelected = onDestinationSelected,
                modifier = Modifier.width(194.dp).fillMaxHeight(),
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
                    modifier = Modifier.width(304.dp).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun CompanyNavigation(
    destination: ControlRoomDestination,
    onDestinationSelected: (ControlRoomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = Color(0xFF0C0E12)) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("GEMINISTRATOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("COMPANY OS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            ControlRoomDestination.entries.forEach { item ->
                val selected = item == destination
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDestinationSelected(item) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = item.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text("JULES", style = MaterialTheme.typography.labelLarge)
            Text("CONNECTED · 1/3 ACTIVE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8ED6A3))
        }
    }
}

@Composable
private fun CompactHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0E12)).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("GEMINISTRATOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text("The company that hires agents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompactNavigation(
    destination: ControlRoomDestination,
    onDestinationSelected: (ControlRoomDestination) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            ControlRoomDestination.Overview,
            ControlRoomDestination.Runs,
            ControlRoomDestination.Company,
            ControlRoomDestination.Inbox,
        ).forEach { item ->
            TextButton(onClick = { onDestinationSelected(item) }, modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    color = if (item == destination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        ControlRoomDestination.Inbox -> InboxScreen(modifier)
        ControlRoomDestination.Settings -> SettingsScreen(modifier)
    }
}
