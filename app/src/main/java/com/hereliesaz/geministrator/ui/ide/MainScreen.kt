package com.hereliesaz.geministrator.ui.ide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor

@Composable
fun IdeScreen(
    setLoading: (Boolean) -> Unit,
    viewModel: IdeViewModel = viewModel()
) {
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
                    subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
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
        Button(
            onClick = { viewModel.onAutocompleteClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Autocomplete")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.onGenerateDocsClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Docs")
        }
    }
}