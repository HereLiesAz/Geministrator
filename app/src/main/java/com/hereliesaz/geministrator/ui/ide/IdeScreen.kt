package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.rosemoe.sora.widget.CodeEditor

@Composable
fun IdeScreen(
    setLoading: (Boolean) -> Unit,
    viewModel: IdeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val editor = remember { CodeEditor(context) }

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    uiState.textInsertion?.let {
        LaunchedEffect(it) {
            editor.text.insert(it.line, it.column, it.text)
            viewModel.onTextInsertionConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { 
                editor.apply {
                    subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                        viewModel.onContentChanged(text.toString())
                    }
                }
            },
            update = { codeEditor ->
                if (codeEditor.text.toString() != uiState.fileContent) {
                    codeEditor.setText(uiState.fileContent ?: "")
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { 
                    val cursor = editor.cursor
                    viewModel.onAutocompleteClick(cursor.leftLine, cursor.leftColumn)
                 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Autocomplete")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.onGenerateDocsClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate Docs")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.onRunClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Run")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.onCommitClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Commit")
            }
        }
    }

    if (uiState.showCommitDialog) {
        CommitDialog(
            commitMessage = uiState.commitMessage,
            onCommitMessageChanged = { viewModel.onCommitMessageChanged(it) },
            onCommit = { viewModel.onCommitConfirm() },
            onDismiss = { viewModel.onCommitDialogDismiss() }
        )
    }

    uiState.error?.let { error ->
        Snackbar(
            action = {
                Button(onClick = { viewModel.onErrorShown() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}
