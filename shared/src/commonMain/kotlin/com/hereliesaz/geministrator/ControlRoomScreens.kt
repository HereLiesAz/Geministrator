package com.hereliesaz.geministrator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun TechnicalInspector(selectedTaskId: String, modifier: Modifier = Modifier) {
    val node = ActiveWorkflow.firstOrNull { it.id == selectedTaskId } ?: ActiveWorkflow.first()
    Column(
        modifier = modifier.background(Azphalt.Ink).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("INSPECTOR", style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text(node.position.uppercase(), style = AzphaltType.section, color = Azphalt.White)
        Text(node.assignment, style = AzphaltType.body, color = Azphalt.White)
        InspectorLine("STATE", node.state.label)
        InspectorLine("STAFFING", node.staffing ?: "Unstaffed")
        InspectorLine("ATTEMPT", if (node.id == "implementation") "1 of 2" else "1")
        InspectorLine("PROVIDER RUN", if (node.staffing != null) "sessions/9b2e" else "—")
        InspectorLine("PROMPT REUSE", if (node.staffing != null) "Session scoped" else "—")
        InspectorLine("CACHE HIT", "Not reported")
        AzphaltPill("Message agent", "message", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InspectorLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = AzphaltType.eyebrow, color = Azphalt.Yellow)
        Text(value, style = AzphaltType.body, color = Azphalt.White)
    }
}

@Composable
internal fun CompanyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("COMPANY", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        listOf(
            "Executive" to listOf("Orchestrator"),
            "Product" to listOf("Product Manager", "Researcher", "UX Designer"),
            "Engineering" to listOf("Architect", "EPA Representative", "Implementation Engineer"),
            "Assurance" to listOf("Crash Test Dummy", "QA Engineer", "Adversarial Reviewer", "Code Reviewer", "Recovery Engineer"),
            "Delivery" to listOf("Release Engineer"),
        ).forEach { (department, roles) ->
            SectionLabel(department)
            roles.forEach { role ->
                AzphaltRecord(
                    seed = role,
                    eyebrow = department,
                    title = role,
                    endCap = if (role == "Implementation Engineer") "Working" else "Available",
                    body = when (role) {
                        "Crash Test Dummy" -> "Author tests · cannot verify or approve"
                        "EPA Representative" -> "Select environment · cannot implement or verify"
                        "Implementation Engineer" -> "Implement · cannot certify own work"
                        else -> "Company position"
                    },
                )
            }
        }
    }
}

@Composable
internal fun InboxScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("INBOX", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        DecisionRecord("infra-release", "Failure escalation", "Infra · Release", "Release failed after 3 attempts", true)
        DecisionRecord("market-security", "Security risk", "Marketplace · Security", "Expanded OAuth scope challenged", true)
        DecisionRecord("foo-integration", "Upcoming", "Foo · Authentication", "Integration approval after independent review", false)
    }
}

@Composable
private fun DecisionRecord(seed: String, kind: String, title: String, body: String, actionable: Boolean) {
    AzphaltRecord(seed, kind, title, body, if (actionable) "Needs you" else "Later", well = if (actionable) {
        {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzphaltPill("Approve", "$seed-approve", onClick = {})
                AzphaltPill("Reject", "$seed-reject", onClick = {})
            }
        }
    } else null)
}

@Composable
internal fun WorkflowTemplateScreen(modifier: Modifier = Modifier) {
    val entrance = remember { AzphaltEntrance.roll() }
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("WORKFLOWS", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        listOf(
            Triple("Standard Feature", "Product → Architecture → Tests → Implementation → QA → Review → Release", "Default"),
            Triple("Bug Fix", "Diagnosis → Contract → Fix → Regression → QA → Review", "Template"),
            Triple("Research Spike", "Product → Research → Architecture → Decision", "Template"),
        ).forEachIndexed { index, item ->
            AzphaltRecord(
                "workflow-$index",
                "Workflow",
                item.first,
                item.second,
                item.third,
                modifier = Modifier.azphaltEntrance(entrance, index, 3),
            )
        }
    }
}

private data class ArtifactEntry(
    val id: String,
    val name: String,
    val detail: String,
    val children: List<ArtifactEntry> = emptyList(),
)

private val ArtifactTree = listOf(
    ArtifactEntry("spec", "Specification", "Approved product and architecture inputs", listOf(
        ArtifactEntry("requirements", "Requirements", "Product Manager · approved"),
        ArtifactEntry("architecture", "Architecture", "Architect · approved"),
    )),
    ArtifactEntry("pretests", "Pre-code verification", "Crash Test Dummy", listOf(
        ArtifactEntry("acceptance", "Acceptance test plan", "4 scenarios"),
        ArtifactEntry("contract", "Contract tests", "7 contracts"),
        ArtifactEntry("failure", "Failure scenarios", "5 cases"),
    )),
    ArtifactEntry("environment", "Environment", "EPA Representative", listOf(
        ArtifactEntry("runtime", "Runtime", "JDK 17 · ephemeral"),
        ArtifactEntry("network", "Network", "Restricted"),
    )),
    ArtifactEntry("implementation-artifacts", "Implementation", "Jules · active", listOf(
        ArtifactEntry("changes", "Code change", "Pending completion"),
        ArtifactEntry("command", "Command output", "3 recent commands"),
    )),
)

@Composable
internal fun ArtifactFileManagerScreen(modifier: Modifier = Modifier) {
    var openId by remember { mutableStateOf<String?>("spec") }
    var previewId by remember { mutableStateOf<String?>(null) }
    val rootEntrance = remember { AzphaltEntrance.roll() }
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ARTIFACTS", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AzphaltPill("Browse", "artifact-browse", selected = true, onClick = {})
            AzphaltPill("Search", "artifact-search", onClick = {})
            AzphaltPill("Storage", "artifact-storage", onClick = {})
        }
        ArtifactTree.forEachIndexed { rootIndex, entry ->
            val open = openId == entry.id
            val siblingFraction by animateFloatAsState(
                targetValue = if (openId == null || open) 1f else 0.42f,
                label = "artifact-sibling-yield-${entry.id}",
            )
            AzphaltRecord(
                seed = entry.id,
                eyebrow = if (entry.children.isEmpty()) "Artifact" else "Collection",
                title = entry.name,
                body = entry.detail,
                endCap = if (open) "Open" else entry.children.size.takeIf { it > 0 }?.toString(),
                selected = open,
                onClick = {
                    previewId = null
                    openId = if (open) null else entry.id
                },
                modifier = Modifier
                    .fillMaxWidth(siblingFraction)
                    .azphaltEntrance(rootEntrance, rootIndex, ArtifactTree.size),
                well = if (open && entry.children.isNotEmpty()) {
                    {
                        val childEntrance = remember(entry.id) { AzphaltEntrance.childBand() }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            entry.children.forEachIndexed { childIndex, child ->
                                val childSelected = previewId == child.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .azphaltEntrance(childEntrance, childIndex, entry.children.size)
                                        .azphaltSelectedTransform(childSelected)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (childSelected) Azphalt.Yellow else Azphalt.hue(child.id))
                                        .clickable { previewId = if (childSelected) null else child.id }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(child.name.uppercase(), style = AzphaltType.capsule, color = if (childSelected) Azphalt.Ink else Azphalt.hue(child.id).contrastingText)
                                    Text(child.detail.uppercase(), style = AzphaltType.endCap, color = if (childSelected) Azphalt.Ink else Azphalt.hue(child.id).contrastingText)
                                }
                                AzphaltChildBand(visible = childSelected) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F0F0F)).padding(12.dp),
                                    ) {
                                        Text("${child.name}\n${child.detail}\nsource: ${entry.name}\nstatus: available", style = AzphaltType.body, color = Azphalt.White)
                                    }
                                }
                            }
                        }
                    }
                } else null,
            )
        }
    }
}

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SETTINGS", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        ProviderRecord("Jules", "Connected", "1 / 3 active", "Secure device storage / gateway")
        ProviderRecord("Codex", "Not configured", "0 active", "No credential")
        ProviderRecord("Claude", "Not configured", "0 active", "No credential")
    }
}

@Composable
private fun ProviderRecord(name: String, state: String, activity: String, auth: String) {
    AzphaltRecord(
        seed = "provider-$name",
        eyebrow = "Provider",
        title = name,
        body = "$activity · $auth",
        endCap = state,
        well = {
            Text("PROMPT REUSE", style = AzphaltType.eyebrow, color = Azphalt.Yellow)
            Text(if (name == "Jules") "Session scoped" else "Provider default", style = AzphaltType.body, color = Azphalt.White)
        },
    )
}

@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
}
