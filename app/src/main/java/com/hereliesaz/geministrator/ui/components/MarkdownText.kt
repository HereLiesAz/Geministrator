package com.hereliesaz.geministrator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.regex.Pattern

@Composable
fun MarkdownText(text: String) {
    val codeBlockPattern = Pattern.compile("```(.*?)```", Pattern.DOTALL)
    val parts = remember(text) {
        val matcher = codeBlockPattern.matcher(text)
        val result = mutableListOf<Pair<String, Boolean>>()
        var lastEnd = 0
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.add(Pair(text.substring(lastEnd, matcher.start()), false))
            }
            result.add(Pair(matcher.group(1) ?: "", true))
            lastEnd = matcher.end()
        }
        if (lastEnd < text.length) {
            result.add(Pair(text.substring(lastEnd), false))
        }
        result
    }

    Column {
        parts.forEach { (content, isCode) ->
            if (isCode) {
                CodeBlock(code = content.trim())
            } else {
                Text(
                    text = content.trim(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}