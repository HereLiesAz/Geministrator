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
import androidx.compose.runtime.LaunchedEffect
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunStatus

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
internal fun CompanyScreen(runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Loading, modifier: Modifier = Modifier) {
    val liveWorkflow = (runtimeState as? ApplicationRuntimeState.Live)?.presentation
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("COMPANY", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        if (liveWorkflow != null) {
            val activeRoleIds = liveWorkflow.run.taskRuns.values
                .filter { it.status in setOf(TaskRunStatus.Running, TaskRunStatus.Planning, TaskRunStatus.AwaitingApproval, TaskRunStatus.Verifying) }
                .mapNotNull { it.assignedRoleId }
                .toSet()
            val byDept = liveWorkflow.roles.groupBy { it.department() }
            listOf("Executive", "Product", "Engineering", "Assurance", "Delivery", "Custom").forEach { dept ->
                val roles = byDept[dept] ?: return@forEach
                SectionLabel(dept)
                roles.forEach { role ->
                    AzphaltRecord(
                        seed = role.id.value,
                        eyebrow = dept,
                        title = role.name,
                        endCap = if (role.id in activeRoleIds) "Working" else "Available",
                        body = role.description,
                    )
                }
            }
        } else {
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
}

@Composable
internal fun InboxScreen(
    runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Loading,
    onApproveTask: (String) -> Unit = {},
    onRejectPlan: (String) -> Unit = {},
    onResolveEscalation: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val liveWorkflow = (runtimeState as? ApplicationRuntimeState.Live)?.presentation
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("INBOX", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        if (liveWorkflow != null) {
            val pending = liveWorkflow.run.taskRuns.values.filter {
                it.status == TaskRunStatus.AwaitingApproval || it.status == TaskRunStatus.Escalated
            }
            if (pending.isEmpty()) {
                Text("No pending decisions.", style = AzphaltType.body, color = Azphalt.currentGround.onPage)
            } else {
                pending.forEach { taskRun ->
                    val task = liveWorkflow.definition.tasks.firstOrNull { it.id == taskRun.taskDefinitionId }
                    val taskId = taskRun.taskDefinitionId.value
                    val isPlanApproval = taskRun.status == TaskRunStatus.AwaitingApproval
                    AzphaltRecord(
                        seed = taskId,
                        eyebrow = if (isPlanApproval) "Plan Approval" else "Failure Escalation",
                        title = task?.name ?: taskId,
                        body = if (isPlanApproval) {
                            taskRun.progressMessage?.takeIf(String::isNotBlank) ?: (task?.objective ?: "")
                        } else {
                            taskRun.blockingReason?.let { "${it.code} · ${it.message}" } ?: "Task failed"
                        },
                        endCap = "Needs you",
                        well = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isPlanApproval) {
                                    AzphaltPill("Approve", "$taskId-approve", onClick = { onApproveTask(taskId) })
                                    if (taskRun.assignedProviderId != null) {
                                        AzphaltPill("Reject", "$taskId-reject", onClick = { onRejectPlan(taskId) })
                                    }
                                } else {
                                    AzphaltPill("Retry", "$taskId-retry", onClick = { onResolveEscalation(taskId, true) })
                                    AzphaltPill("Stop", "$taskId-stop", onClick = { onResolveEscalation(taskId, false) })
                                }
                            }
                        },
                    )
                }
            }
        } else {
            DecisionRecord("infra-release", "Failure escalation", "Infra · Release", "Release failed after 3 attempts", true)
            DecisionRecord("market-security", "Security risk", "Marketplace · Security", "Expanded OAuth scope challenged", true)
            DecisionRecord("foo-integration", "Upcoming", "Foo · Authentication", "Integration approval after independent review", false)
        }
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
internal fun RunsScreen(
    runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Loading,
    onLoadRunHistory: suspend () -> List<Pair<Project, List<WorkflowRun>>> = { emptyList() },
    onSwitchRun: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var history by remember { mutableStateOf<List<Pair<Project, List<WorkflowRun>>>?>(null) }
    LaunchedEffect(runtimeState) {
        history = onLoadRunHistory()
    }
    val liveRunId = (runtimeState as? ApplicationRuntimeState.Live)?.presentation?.run?.id?.value
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("RUNS", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        val entries = history
        if (entries == null) {
            Text("Loading…", style = AzphaltType.body, color = Azphalt.currentGround.onPage)
        } else if (entries.isEmpty()) {
            Text("No projects yet.", style = AzphaltType.body, color = Azphalt.currentGround.onPage)
        } else {
            entries.forEach { (project, runs) ->
                SectionLabel(project.name)
                if (runs.isEmpty()) {
                    Text("No runs.", style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
                } else {
                    runs.forEach { run ->
                        val isActive = run.id.value == liveRunId
                        AzphaltRecord(
                            seed = run.id.value,
                            eyebrow = run.status.name,
                            title = run.objective.take(60).let { if (run.objective.length > 60) "$it…" else it },
                            body = "Run · ${run.id.value.takeLast(8)}",
                            endCap = if (isActive) "Active" else runStatusEndCap(run.status),
                            selected = isActive,
                            onClick = { if (!isActive) onSwitchRun(run.id.value) },
                        )
                    }
                }
            }
        }
    }
}

private fun runStatusEndCap(status: WorkflowRunStatus): String = when (status) {
    WorkflowRunStatus.Completed -> "Done"
    WorkflowRunStatus.Failed -> "Failed"
    WorkflowRunStatus.Cancelled -> "Cancelled"
    WorkflowRunStatus.Running -> "Running"
    WorkflowRunStatus.AwaitingHuman -> "Waiting"
    WorkflowRunStatus.Created -> "Created"
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
internal fun ArtifactFileManagerScreen(runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Loading, modifier: Modifier = Modifier) {
    val liveWorkflow = (runtimeState as? ApplicationRuntimeState.Live)?.presentation
    val displayTree: List<ArtifactEntry> = if (liveWorkflow != null) {
        liveWorkflow.definition.tasks.mapNotNull { task ->
            val taskRun = liveWorkflow.run.taskRuns[task.id] ?: return@mapNotNull null
            if (taskRun.artifacts.isEmpty()) return@mapNotNull null
            ArtifactEntry(
                id = task.id.value,
                name = task.name,
                detail = taskRun.status.name,
                children = taskRun.artifacts.map { artifact ->
                    ArtifactEntry(
                        id = artifact.id.value,
                        name = artifact.label,
                        detail = artifact.kind.name,
                    )
                },
            )
        }.takeIf { it.isNotEmpty() } ?: ArtifactTree
    } else ArtifactTree

    var openId by remember(displayTree) { mutableStateOf(displayTree.firstOrNull()?.id) }
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
        if (liveWorkflow != null && displayTree === ArtifactTree) {
            Text("No artifacts produced yet.", style = AzphaltType.body, color = Azphalt.currentGround.onPage)
        }
        displayTree.forEachIndexed { rootIndex, entry ->
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
                    .azphaltEntrance(rootEntrance, rootIndex, displayTree.size),
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
internal fun SettingsScreen(
    connectedProviderIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SETTINGS", style = AzphaltType.hero, color = Azphalt.currentGround.onPage)
        SectionLabel("Providers")
        if (connectedProviderIds.isNotEmpty()) {
            connectedProviderIds.forEach { providerId ->
                ProviderRecord(providerId, "Connected", "Credential present")
            }
        } else {
            ProviderRecord("Jules", "Not configured", "No credential")
            ProviderRecord("Codex", "Not configured", "No credential")
            ProviderRecord("Claude", "Not configured", "No credential")
        }
    }
}

@Composable
private fun ProviderRecord(name: String, state: String, auth: String) {
    AzphaltRecord(
        seed = "provider-$name",
        eyebrow = "Provider",
        title = name,
        body = auth,
        endCap = state,
    )
}

@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), style = AzphaltType.eyebrow, color = Azphalt.currentGround.onPage)
}

private fun RoleDefinition.department(): String = when (id.value) {
    "orchestrator" -> "Executive"
    "product-manager", "researcher", "ux-designer" -> "Product"
    "architect", "epa-representative", "implementation-engineer" -> "Engineering"
    "crash-test-dummy", "qa-engineer", "adversarial-reviewer", "code-reviewer", "recovery-engineer" -> "Assurance"
    "release-engineer" -> "Delivery"
    else -> "Custom"
}
