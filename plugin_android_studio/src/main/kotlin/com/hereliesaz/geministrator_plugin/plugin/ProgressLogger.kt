package com.hereliesaz.geministrator_plugin.plugin

import com.hereliesaz.geministrator.common.ILogger
import com.intellij.openapi.application.ApplicationManager
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import javax.swing.JEditorPane

class ProgressLogger(private val outputArea: JEditorPane) : ILogger {
    private val options = MutableDataSet()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    private fun log(message: String) {
        ApplicationManager.getApplication().invokeLater {
            val html = renderer.render(parser.parse(message))
            outputArea.text = outputArea.text.replace("</body>", "$html</body>")
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