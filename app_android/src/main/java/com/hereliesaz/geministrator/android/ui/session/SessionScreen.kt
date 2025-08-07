package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.android.ui.components.DiffView
import com.hereliesaz.geministrator.android.ui.components.MarkdownText
import com.hereliesaz.geministrator.android.ui.components.ShimmerPlaceholder

@Composable
fun SessionScreen(sessionViewModel: SessionViewModel = viewModel()) {
    val uiState by sessionViewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Log", "Version Control")

    // Show DiffView Dialog when state is not null
    uiState.diffViewState?.let { diffState ->
        DiffView(
            filePath = diffState.filePath,
            diffContent = diffState.diffContent,
            onDismiss = { sessionViewModel.dismissDiff() }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> LogView(uiState = uiState)
                1 -> VersionControlView(
                    sessionViewModel = sessionViewModel
                )
            }
        }

        StatusFooter(
            status = uiState.status,
            clarificationPrompt = uiState.clarificationPrompt,
            onSubmitClarification = { response ->
                sessionViewModel.submitClarification(response)
            }
        )
    }
}

@Composable
private fun LogView(uiState: SessionUiState) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.logEntries.size) {
        if (uiState.logEntries.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logEntries.size - 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = uiState.logEntries, key = { it.id }) { entry ->
            LogEntryItem(entry)
        }
    }
}

@Composable
fun LogEntryItem(entry: LogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                text = "${entry.agent.name}:",
                color = entry.agent.color,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " ${entry.message}",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (entry.content != null) {
            MarkdownText(text = entry.content)
        }

        if (entry.isWorking) {
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun StatusFooter(
    status: WorkflowStatus,
    clarificationPrompt: String?,
    onSubmitClarification: (String) -> Unit,
) {
    AnimatedContent(
        targetState = status,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        transitionSpec = {
            (fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { height -> height })).togetherWith(
                fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { height -> -height })
            )
        },
        label = "StatusFooterAnimation"
    ) { targetStatus ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (targetStatus) {
                WorkflowStatus.RUNNING -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                WorkflowStatus.SUCCESS -> {
                    Row {
                        Text(
                            "Workflow Completed Successfully",
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                        Button(onClick = { navController.navigate("create_issue") }) {
                            Text("Create GitHub Issue")
                        }
                    }
                }

                WorkflowStatus.FAILURE -> {
                    Text(
                        "Workflow Failed",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }

                WorkflowStatus.AWAITING_INPUT -> {
                    ClarificationInput(
                        prompt = clarificationPrompt ?: "Awaiting input...",
                        onSubmit = onSubmitClarification,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }

                WorkflowStatus.IDLE -> {
                    Text(
                        "Session ready.",
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClarificationInput(
    prompt: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    Column(modifier = modifier) {
        Text(
            text = prompt,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Your response...") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) {
                    onSubmit(text)
                }
            })
        )
    }
}