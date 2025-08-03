package com.hereliesaz.GeminiOrchestrator.plugin

import com.hereliesaz.GeminiOrchestrator.core.council.ILogger
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JTextArea

class ProgressLogger(private val outputArea: JTextArea) : ILogger {
    override fun log(message: String) {
        ApplicationManager.getApplication().invokeLater {
            outputArea.append("$message\n")
            outputArea.caretPosition = outputArea.document.length
        }
    }
}