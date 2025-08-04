package com.hereliesaz.geministrator_plugin.plugin

import com.hereliesaz.geministrator.common.ILogger
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JTextArea

class ProgressLogger(private val outputArea: JTextArea) : ILogger {
    private fun log(message: String) {
        ApplicationManager.getApplication().invokeLater {
            outputArea.append("$message\n")
            outputArea.caretPosition = outputArea.document.length
        }
    }

    override fun info(message: String) {
        log("[INFO] $message")
    }

    override fun error(message: String, e: Throwable?) {
        val fullMessage = if (e != null) "$message: ${e.message}" else message
        log("[ERROR] $fullMessage")
    }

    override fun interactive(message: String) {
        log("[USER] $message")
    }

    override fun prompt(message: String): String? {
        // Cannot prompt from a non-interactive logger in the UI
        // This could be implemented with a dialog box if needed
        interactive(message)
        return null
    }
}