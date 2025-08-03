package com.hereliesaz.geministrator_plugin.plugin
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JTextArea

class ProgressLogger(private val outputArea: JTextArea) : com.hereliesaz.geminiorchestrator.core.council.ILogger {
    override fun log(message: String) {
        ApplicationManager.getApplication().invokeLater {
            outputArea.append("$message\n")
            outputArea.caretPosition = outputArea.document.length
        }
    }
}