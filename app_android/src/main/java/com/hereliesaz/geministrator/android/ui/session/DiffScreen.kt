package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DiffScreen(sessionViewModel: SessionViewModel = viewModel(), filePath: String) {
    val uiState by sessionViewModel.uiState.collectAsState()
    var showSideBySide by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        sessionViewModel.getDiff(filePath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(onClick = { showSideBySide = !showSideBySide }) {
            Text(if (showSideBySide) "Show Inline" else "Show Side-by-Side")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (showSideBySide) {
            SideBySideDiffView(diff = uiState.diff)
        } else {
            val annotatedString = buildAnnotatedString {
                uiState.diff.lines().forEach { line ->
                    when {
                        line.startsWith("+") -> withStyle(style = SpanStyle(color = Color.Green)) {
                            append(line)
                        }
                        line.startsWith("-") -> withStyle(style = SpanStyle(color = Color.Red)) {
                            append(line)
                        }
                        else -> append(line)
                    }
                    append("\n")
                }
            }
            Text(
                text = annotatedString,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }
    }
}
