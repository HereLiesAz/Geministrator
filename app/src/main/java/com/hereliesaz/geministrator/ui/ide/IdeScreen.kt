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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepositoryImpl
import com.jules.apiclient.JulesApiClient
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun IdeScreen(
    setLoading: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val settingsRepository = SettingsRepositoryImpl(context)
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return IdeViewModel(
                androidx.lifecycle.SavedStateHandle(),
                settingsRepository,
                null,
                null
            ) as T
        }
    }
    val viewModel: IdeViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                CodeEditor(context).apply {
                    viewModel.onEditorAttached(this)
                    subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                        viewModel.onContentChanged(text.toString())
                    }
                }
            },
            update = { editor ->
                if (editor.text.toString() != uiState.fileContent) {
                    editor.setText(uiState.fileContent)
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.onAutocompleteClick() },
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