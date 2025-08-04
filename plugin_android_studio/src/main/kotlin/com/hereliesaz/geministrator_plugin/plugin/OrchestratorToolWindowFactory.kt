package com.hereliesaz.geministrator_plugin.plugin

import com.hereliesaz.geministrator.adapter.CliAdapter
import com.hereliesaz.geministrator.adapter.CliConfigStorage
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
import com.hereliesaz.geministrator_plugin.adapter.AndroidStudioAdapter
import com.hereliesaz.geministrator_plugin.adapter.PluginConfigStorage
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
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities

class OrchestratorToolWindowFactory : ToolWindowFactory {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val orchestratorPanel = OrchestratorPanel(project) // [cite: 873]
        val content = ContentFactory.getInstance().createContent(orchestratorPanel, "", false) // [cite: 873]
        toolWindow.contentManager.addContent(content) // [cite: 873]
    }

    inner class OrchestratorPanel(private val project: Project) : JPanel(BorderLayout()) {
        private val promptArea = JBTextArea(5, 50).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        private val runButton = JButton("Run Workflow") // [cite: 874]
        private val outputArea = JTextArea().apply {
            isEditable = false
            font = font.deriveFont(12f)
        }
        private val configStorage = PluginConfigStorage()
        private val logger = ProgressLogger(outputArea)
        private val reviewCheckbox =
            JBCheckBox("Require final review before commit", configStorage.loadPreCommitReview())
        private val concurrencyLabel = JLabel("Max Parallel Tasks:") // [cite: 875]
        private val concurrencySpinner =
            JSpinner(SpinnerNumberModel(configStorage.loadConcurrencyLimit(), 1, 16, 1)) // [cite: 875]
        private val tokenLimitLabel = JLabel("Token Limit:") // [cite: 875]
        private val tokenLimitSpinner =
            JSpinner(SpinnerNumberModel(configStorage.loadTokenLimit(), 10000, 2000000, 10000)) // [cite: 875]

        init {
            val settingsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            settingsPanel.add(reviewCheckbox)
            settingsPanel.add(Box.createHorizontalStrut(10))
            settingsPanel.add(concurrencyLabel)
            settingsPanel.add(concurrencySpinner)
            settingsPanel.add(Box.createHorizontalStrut(10)) // [cite: 877]
            settingsPanel.add(tokenLimitLabel)
            settingsPanel.add(tokenLimitSpinner)

            val buttonPanel = JPanel(BorderLayout())
            buttonPanel.add(runButton, BorderLayout.CENTER)
            buttonPanel.add(settingsPanel, BorderLayout.SOUTH)

            val topPanel = JPanel(BorderLayout())
            topPanel.add(JBScrollPane(promptArea), BorderLayout.CENTER)
            topPanel.add(buttonPanel, BorderLayout.SOUTH) // [cite: 878]

            val mainPanel = JPanel(BorderLayout())
            mainPanel.add(topPanel, BorderLayout.NORTH)
            mainPanel.add(JBScrollPane(outputArea), BorderLayout.CENTER)

            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, JScrollPane(JTextArea("")))
            splitPane.resizeWeight = 0.7

            add(splitPane, BorderLayout.CENTER)

            runButton.addActionListener {
                coroutineScope.launch { runWorkflow(promptArea.text) }
            }
            reviewCheckbox.addActionListener { configStorage.savePreCommitReview(reviewCheckbox.isSelected) } // [cite: 880]
            concurrencySpinner.addChangeListener { configStorage.saveConcurrencyLimit(concurrencySpinner.value as Int) } // [cite: 880]
            tokenLimitSpinner.addChangeListener { configStorage.saveTokenLimit(tokenLimitSpinner.value as Int) } // [cite: 880]

            if (configStorage.loadApiKey().isNullOrBlank()) {
                showOnboardingWizard()
            }
        }

        private fun showOnboardingWizard() {
            SwingUtilities.invokeLater {
                Messages.showMessageDialog(
                    project,
                    "Welcome to Geministrator!\nThis tool requires a Gemini API Key to function.\nPlease enter your key to continue.",
                    "Welcome",
                    Messages.getInformationIcon()
                )
                coroutineScope.launch { getAndValidateApiKey() }
            }
        }

        private suspend fun runWorkflow(prompt: String) {
            if (prompt.isBlank()) return
            withContext(Dispatchers.Main) {
                outputArea.text = ""
                runButton.isEnabled = false
            }

            // The adapter for the plugin UI
            val adapter = AndroidStudioAdapter(project, logger)

            // Since the CLI module contains the core logic, we need its config and prompt manager
            val cliConfig = CliConfigStorage()
            val promptManager = PromptManager(cliConfig.getConfigDirectory())

            // Create the GeminiService with all its required arguments
            val geminiService = createGeminiService(cliConfig, logger, CliAdapter(cliConfig, logger))

            if (geminiService == null) {
                logger.error("Could not create Gemini Service. Check authentication.")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
                return
            }

            // Create the orchestrator with all its required arguments
            val orchestrator = Orchestrator(adapter, logger, cliConfig, promptManager, geminiService)

            try {
                // For now, projectType is hardcoded. This could be a dropdown in the UI later.
                orchestrator.run(prompt, project.basePath ?: ".", "IDE Plugin Task", null)
            } catch (e: Exception) {
                logger.error("--- A critical error occurred ---", e)
            } finally {
                logger.info("--- Workflow Finished ---")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
            }
        }

        private suspend fun getAndValidateApiKey(): String? {
            var apiKey = configStorage.loadApiKey()
            while (true) {
                if (!apiKey.isNullOrBlank()) {
                    val serviceForValidation = GeminiService("apikey", apiKey, logger, configStorage, "", "", null, null)
                    if (serviceForValidation.validateApiKey(apiKey)) {
                        return apiKey
                    }
                    logger.error("Your saved API key is no longer valid. Please enter a new one.")
                }
                apiKey = withContext(Dispatchers.Main) {
                    Messages.showInputDialog(
                        project,
                        "Please enter your Gemini API Key:",
                        "API Key Required",
                        Messages.getQuestionIcon()
                    )
                }
                if (apiKey.isNullOrBlank()) return null
                configStorage.saveApiKey(apiKey) // Save the key to be validated
            }
        }
    }
}