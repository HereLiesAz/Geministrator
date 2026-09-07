package com.hereliesaz.geministrator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RunControlRoom(
    modifier: Modifier,
    selectedTaskId: String,
    onTaskSelected: (String) -> Unit,
    compact: Boolean,
) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ExecutiveHeader(compact)
        AttentionStrip(compact)
        SectionHeading("COMPANY EXECUTION", "Work moving through positions, gates, and handoffs")
        WorkflowRail(selectedTaskId, onTaskSelected)
        SectionHeading("RECENT COMPANY ACTIVITY", "Narrative by default; provider telemetry lives in the inspector")
        EventTimeline()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ExecutiveHeader(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ADD AUTHENTICATION", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Foo · Standard Feature Workflow", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill("RUNNING", WorkState.Working)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Metric("4/9", "positions complete")
            Metric("1", "working")
            Metric("4", "blocked / waiting")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}) { Text("Pause") }
            OutlinedButton(onClick = {}) { Text("Cancel") }
        }
    }
}

@Composable
private fun AttentionStrip(compact: Boolean) {
    Surface(color = Color(0xFF2B2112), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (compact) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OWNER ATTENTION", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE7C27D))
                Text("No decisions required right now", fontWeight = FontWeight.Bold)
                Text("Implementation is underway. The next human gate is integration approval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OWNER ATTENTION", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE7C27D), fontWeight = FontWeight.Bold)
                Text("No decisions required right now", fontWeight = FontWeight.Bold)
                Text("Next gate: integration approval", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkflowRail(selectedTaskId: String, onTaskSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ActiveWorkflow.forEachIndexed { index, node ->
            WorkOrderNode(node, node.id == selectedTaskId) { onTaskSelected(node.id) }
            if (index != ActiveWorkflow.lastIndex) {
                Box(
                    modifier = Modifier.padding(start = 27.dp).width(2.dp).height(16.dp).background(MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}

@Composable
private fun WorkOrderNode(node: WorkNode, selected: Boolean, onClick: () -> Unit) {
    val borderColor = when (node.state) {
        WorkState.Working -> MaterialTheme.colorScheme.primary
        WorkState.Gate -> Color(0xFFE7C27D)
        WorkState.Complete -> Color(0xFF5B8F6B)
        WorkState.Waiting, WorkState.Blocked -> MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier.fillMaxWidth().border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else borderColor, RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(node.position.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    Text(node.assignment, style = MaterialTheme.typography.titleMedium)
                }
                StatusPill(node.state.label, node.state)
            }
            node.staffing?.let {
                Text("Staffed by $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            node.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            node.injectedReason?.let {
                Surface(color = Color(0xFF24201B), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "AUTO-ASSIGNED · $it",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD7B77A),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, state: WorkState) {
    val color = when (state) {
        WorkState.Complete -> Color(0xFF8ED6A3)
        WorkState.Working -> MaterialTheme.colorScheme.primary
        WorkState.Gate -> Color(0xFFE7C27D)
        WorkState.Waiting -> Color(0xFFC7C8D0)
        WorkState.Blocked -> Color(0xFF8B8E96)
    }
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EventTimeline() {
    val events = listOf(
        "15:34" to "Implementation began · Implementation Engineer staffed by Jules",
        "15:32" to "EPA delivered execution environment specification",
        "15:29" to "Pre-code verification contract approved",
        "15:23" to "Crash Test Dummy produced four specification-derived test artifacts",
        "15:18" to "Architect plan approved",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        events.forEach { (time, description) ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp))
                Text(description, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun TechnicalInspector(selectedTaskId: String, modifier: Modifier = Modifier) {
    val node = ActiveWorkflow.firstOrNull { it.id == selectedTaskId } ?: ActiveWorkflow.first()
    Surface(modifier = modifier, color = Color(0xFF0E1014)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("INSPECTOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(node.position, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(node.assignment, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider()
            InspectorField("POSITION", node.position)
            InspectorField("STATE", node.state.label)
            InspectorField("STAFFING", node.staffing ?: "Unstaffed")
            InspectorField("ATTEMPT", if (node.id == "implementation") "1 of 2" else "1")
            InspectorField("PROVIDER RUN", if (node.staffing != null) "sessions/9b2e" else "—")
            InspectorField("PROMPT REUSE", if (node.staffing != null) "Session scoped" else "—")
            InspectorField("CACHE HIT", "Provider does not report")
            Divider()
            Text("AUTHORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (node.position) {
                "Implementation Engineer" -> AuthorityBlock(listOf("Implement"), listOf("Verify", "Approve own work", "Approve release"))
                "Crash Test Dummy" -> AuthorityBlock(listOf("Author tests"), listOf("Implement", "Verify", "Approve results"))
                "EPA Representative" -> AuthorityBlock(listOf("Select environment"), listOf("Implement", "Verify"))
                else -> AuthorityBlock(listOf("Perform assigned role"), listOf("Self-certify downstream work"))
            }
            Divider()
            Text("TECHNICAL DETAIL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Raw provider/session telemetry belongs here rather than in the main workflow view.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Message staffed agent") }
        }
    }
}

@Composable
private fun InspectorField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AuthorityBlock(can: List<String>, cannot: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        can.forEach { Text("CAN · $it", color = Color(0xFF8ED6A3), style = MaterialTheme.typography.bodySmall) }
        cannot.forEach { Text("CANNOT · $it", color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun CompanyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("COMPANY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Positions, authority, staffing preferences, and handoffs—not a list of bots.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OrgDepartment("EXECUTIVE", listOf("Orchestrator"))
        OrgDepartment("PRODUCT", listOf("Product Manager", "Researcher", "UX Designer"))
        OrgDepartment("ENGINEERING", listOf("Architect", "EPA Representative", "Implementation Engineer"))
        OrgDepartment("ASSURANCE", listOf("Crash Test Dummy", "QA Engineer", "Adversarial Reviewer", "Code Reviewer", "Recovery Engineer"))
        OrgDepartment("DELIVERY", listOf("Release Engineer"))
    }
}

@Composable
private fun OrgDepartment(title: String, roles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        roles.forEach { role ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(role, fontWeight = FontWeight.Bold)
                    Text(if (role == "Implementation Engineer") "WORKING" else "AVAILABLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun InboxScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("INBOX", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Only work that needs an owner decision belongs here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        DecisionCard("Infra · Release", "FAILURE ESCALATION", "Release failed after 3 attempts. Recovery Engineer requests a human decision.")
        DecisionCard("Marketplace · Security", "SECURITY RISK", "Adversarial Reviewer objects to expanded OAuth scope.")
        DecisionCard("Foo · Authentication", "UPCOMING", "Integration approval will become available after independent review.", actionable = false)
    }
}

@Composable
private fun DecisionCard(project: String, kind: String, reason: String, actionable: Boolean = true) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(kind, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE7C27D), fontWeight = FontWeight.Black)
            Text(project, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionable) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Approve") }
                    OutlinedButton(onClick = {}) { Text("Reject") }
                }
            }
        }
    }
}

@Composable
internal fun WorkflowTemplateScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("WORKFLOWS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Templates describe how work moves through the company.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            "Standard Feature" to "Product → Architecture → pre-code tests → Implementation → post-code tests → QA → Review → Release",
            "Bug Fix" to "Diagnosis → verification contract → fix → regression tests → QA → Review",
            "Research Spike" to "Product → Research → Architecture → decision",
        ).forEach { (name, path) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Provider staffing, concurrency, and secure connection status.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProviderPanel("Jules", "CONNECTED", "1 / 3 active", "Session-scoped prompt reuse", "Credential source: secure device storage / gateway")
        ProviderPanel("Codex", "NOT CONFIGURED", "0 active", "Future provider", "No credential configured")
        ProviderPanel("Claude", "NOT CONFIGURED", "0 active", "Future provider", "No credential configured")
    }
}

@Composable
private fun ProviderPanel(name: String, status: String, activity: String, caching: String, auth: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelSmall, color = if (status == "CONNECTED") Color(0xFF8ED6A3) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(activity, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(caching, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(auth, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
