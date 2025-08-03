package com.hereliesaz.GeminiOrchestrator.plugin

import com.hereliesaz.GeminiOrchestrator.adapter.as.AndroidStudioAdapter
import com.hereliesaz.GeminiOrchestrator.adapter.as.PluginConfigStorage
import com.hereliesaz.GeminiOrchestrator.core.Orchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class OrchestratorToolWindowFactory : ToolWindowFactory {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val orchestratorPanel = OrchestratorPanel(project)
        val content = ContentFactory.getInstance().createContent(orchestratorPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    inner class OrchestratorPanel(private val project: Project) : JPanel(BorderLayout()) {
        private val promptArea = JBTextArea(5, 50).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        private val runButton = JButton("Run Workflow")
        private val outputArea = JTextArea().apply {
            isEditable = false
            font = font.deriveFont(12f)
        }
        private val configStorage = PluginConfigStorage()
        private val logger = ProgressLogger(outputArea)
        private val reviewCheckbox = JBCheckBox("Require final review before commit", configStorage.loadPreCommitReview())
        private val concurrencyLabel = JLabel("Max Parallel Tasks:")
        private val concurrencySpinner = JSpinner(SpinnerNumberModel(configStorage.loadConcurrencyLimit(), 1, 16, 1))
        private val tokenLimitLabel = JLabel("Token Limit:")
        private val tokenLimitSpinner = JSpinner(SpinnerNumberModel(configStorage.loadTokenLimit(), 10000, 2000000, 10000))
        private val monitorLabel = JLabel("Active Task Monitor")
        private val monitorArea = JTextArea(10, 50).apply {
            isEditable = false
            font = font.deriveFont(12f)
        }

        init {
            val settingsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            settingsPanel.add(reviewCheckbox)
            settingsPanel.add(Box.createHorizontalStrut(10))
            settingsPanel.add(concurrencyLabel)
            settingsPanel.add(concurrencySpinner)
            settingsPanel.add(Box.createHorizontalStrut(10))
            settingsPanel.add(tokenLimitLabel)
            settingsPanel.add(tokenLimitSpinner)

            val buttonPanel = JPanel(BorderLayout())
            buttonPanel.add(runButton, BorderLayout.CENTER)
            buttonPanel.add(settingsPanel, BorderLayout.SOUTH)

            val topPanel = JPanel(BorderLayout())
            topPanel.add(JBScrollPane(promptArea), BorderLayout.CENTER)
            topPanel.add(buttonPanel, BorderLayout.SOUTH)

            val mainPanel = JPanel(BorderLayout())
            mainPanel.add(topPanel, BorderLayout.NORTH)
            mainPanel.add(JBScrollPane(outputArea), BorderLayout.CENTER)

            val monitorPanel = JPanel(BorderLayout())
            monitorPanel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
            monitorPanel.add(monitorLabel, BorderLayout.NORTH)
            monitorPanel.add(JBScrollPane(monitorArea), BorderLayout.CENTER)

            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, monitorPanel)
            splitPane.resizeWeight = 0.7

            add(splitPane, BorderLayout.CENTER)

            runButton.addActionListener {
                coroutineScope.launch { runWorkflow(promptArea.text) }
            }
            reviewCheckbox.addActionListener { configStorage.savePreCommitReview(reviewCheckbox.isSelected) }
            concurrencySpinner.addChangeListener { configStorage.saveConcurrencyLimit(concurrencySpinner.value as Int) }
            tokenLimitSpinner.addChangeListener { configStorage.saveTokenLimit(tokenLimitSpinner.value as Int) }

            if (configStorage.loadApiKey().isNullOrBlank()) {
                showOnboardingWizard()
            }
        }

        private fun showOnboardingWizard() {
            SwingUtilities.invokeLater {
                Messages.showMessageDialog(project, "Welcome to Gemini Orchestrator!\nThis tool requires a Gemini API Key to function.\nPlease enter your key to continue.", "Welcome", Messages.getInformationIcon())
                coroutineScope.launch { getAndValidateApiKey() }
            }
        }

        private suspend fun runWorkflow(prompt: String) {
            if (prompt.isBlank()) return
            withContext(Dispatchers.Main) {
                outputArea.text = ""
                monitorArea.text = ""
                runButton.isEnabled = false
            }
            val combinedLogger = object : com.hereliesaz.GeminiOrchestrator.core.council.ILogger {
                override fun log(message: String) {
                    logger.log(message)
                    if (message.contains("[STARTING]") || message.contains("[FINISHED]")) {
                        SwingUtilities.invokeLater { monitorArea.append("$message\n") }
                    }
                }
            }
            combinedLogger.log("▶️ Initializing workflow...")
            val apiKey = getAndValidateApiKey()
            if (apiKey == null) {
                combinedLogger.log("❌ Workflow cancelled. API key is required.")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
                return
            }
            val adapter = AndroidStudioAdapter(project, combinedLogger)
            val orchestrator = Orchestrator(adapter, apiKey, combinedLogger, configStorage)
            try {
                orchestrator.run(prompt, project.basePath ?: ".")
            } catch (e: Exception) {
                combinedLogger.log("---")
                combinedLogger.log("❌ A critical error occurred: ${e.message}")
            } finally {
                combinedLogger.log("--- Workflow Finished ---")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
            }
        }

        private suspend fun getAndValidateApiKey(): String? {
            var apiKey = configStorage.loadApiKey()
            while (true) {
                if (!apiKey.isNullOrBlank()) {
                    val service = com.hereliesaz.GeminiOrchestrator.core.GeminiService(apiKey, logger, configStorage, "", "")
                    if (service.validateApiKey()) {
                        return apiKey
                    }
                    logger.log("⚠️ Your saved API key is no longer valid. Please enter a new one.")
                }
                apiKey = withContext(Dispatchers.Main) {
                    Messages.showInputDialog(project, "Please enter your Gemini API Key:", "API Key Required", Messages.getQuestionIcon())
                }
                if (apiKey.isNullOrBlank()) return null
                val service = com.hereliesaz.GeminiOrchestrator.core.GeminiService(apiKey, logger, configStorage, "", "")
                if (service.validateApiKey()) {
                    configStorage.saveApiKey(apiKey)
                    logger.log("✅ API Key is valid and has been saved.")
                    return apiKey
                } else {
                    withContext(Dispatchers.Main) {
                        Messages.showErrorDialog(project, "The key you entered is invalid. Please try again.", "Invalid API Key")
                    }
                }
            }
        }
    }
}