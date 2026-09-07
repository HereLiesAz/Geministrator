package com.hereliesaz.geministrator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    GeministratorTheme {
        Scaffold { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                if (maxWidth < 720.dp) {
                    CompactShell()
                } else {
                    WideShell()
                }
            }
        }
    }
}

@Composable
private fun GeministratorTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun CompactShell() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header()
        FoundationStatusCard()
        SectionCard(
            title = "Projects",
            body = "Repository-backed projects will live here.",
        )
        SectionCard(
            title = "Workflows",
            body = "DAG workflows, active runs, and dependency state will live here.",
        )
        SectionCard(
            title = "Company",
            body = "Roles, authority, provider assignment, and standing instructions will live here.",
        )
        SectionCard(
            title = "Inbox",
            body = "Only decisions that actually require a human will appear here.",
        )
    }
}

@Composable
private fun WideShell() {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRailPlaceholder()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            FoundationStatusCard()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(
                    title = "Projects",
                    body = "Repository-backed projects and source connections.",
                    modifier = Modifier.weight(1f),
                )
                SectionCard(
                    title = "Workflows",
                    body = "Dependency graphs, active runs, retries, and gates.",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(
                    title = "Company",
                    body = "Role definitions, authority, and provider assignment.",
                    modifier = Modifier.weight(1f),
                )
                SectionCard(
                    title = "Inbox",
                    body = "Human approvals and escalations only.",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavigationRailPlaceholder() {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.width(184.dp).fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Geministrator",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            listOf("Projects", "Workflows", "Company", "Runs", "Inbox", "Settings").forEach { label ->
                TextButton(onClick = {}) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Geministrator",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "The company that hires agents.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoundationStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Multiplatform foundation",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Shared Compose UI is active for Android, Desktop, JavaScript, and WebAssembly targets.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
