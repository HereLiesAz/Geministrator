package com.hereliesaz.geministrator.android.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun SideBySideDiffView(diff: String) {
    val lines = diff.lines()
    val addedLines = lines.filter { it.startsWith("+") }
    val removedLines = lines.filter { it.startsWith("-") }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            removedLines.forEach { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray.copy(alpha = 0.2f))
                        .padding(4.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            addedLines.forEach { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Green,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray.copy(alpha = 0.2f))
                        .padding(4.dp)
                )
            }
        }
    }
}
