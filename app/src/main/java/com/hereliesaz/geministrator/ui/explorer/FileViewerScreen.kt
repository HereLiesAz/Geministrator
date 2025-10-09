package com.hereliesaz.geministrator.ui.explorer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hereliesaz.geministrator.ui.project.ProjectViewModel
import io.github.rosemoe.sora.lang.textmate.TextMateColorScheme
import io.github.rosemoe.sora.lang.textmate.TextMateLanguage
import io.github.rosemoe.sora.lang.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    projectViewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
) {
    val projectState by projectViewModel.uiState.collectAsStateWithLifecycle()
    val fileContent = projectState.localCachePath?.let {
        projectViewModel.readFile(filePath).getOrNull()
    } ?: "Error: Could not read file."

    val context = LocalContext.current
    val editor = remember { CodeEditor(context) }

    val scopeName = when (filePath.substringAfterLast('.')) {
        "kt", "kts" -> "source.kotlin"
        "java" -> "source.java"
        "json" -> "source.json"
        else -> null
    }

    if (scopeName != null) {
        editor.setEditorLanguage(TextMateLanguage.create(scopeName, true))
        editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    }

    DisposableEffect(editor) {
        onDispose {
            editor.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = filePath, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { editor },
                modifier = Modifier.fillMaxSize()
            ) { codeEditor ->
                codeEditor.setText(fileContent)
            }
        }
    }
}