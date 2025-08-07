package com.hereliesaz.geministrator_plugin.plugin

import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
import com.hereliesaz.geministrator.core.config.ConfigStorage
import com.hereliesaz.geministrator_plugin.adapter.AndroidStudioAdapter
import com.hereliesaz.geministrator_plugin.adapter.PluginConfigStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import jdk.internal.joptsimple.internal.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.File
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
        private val reviewCheckbox = JBCheckBox("Require final review before commit")
        private val concurrencyLabel = JLabel("Max Parallel Tasks:")
        private val concurrencySpinner = JSpinner(SpinnerNumberModel(2, 1, 16, 1))
        private val tokenLimitLabel = JLabel("Token Limit:")
        private val tokenLimitSpinner = JSpinner(SpinnerNumberModel(500000, 10000, 2000000, 10000))

        init {
            loadInitialSettings()

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

            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, JScrollPane(JTextArea("")))
            splitPane.resizeWeight = 0.7

            add(splitPane, BorderLayout.CENTER)

            runButton.addActionListener {
                coroutineScope.launch { runWorkflow(promptArea.text) }
            }
            reviewCheckbox.addActionListener {
                coroutineScope.launch {
                    configStorage.savePreCommitReview(
                        reviewCheckbox.isSelected
                    )
                }
            }
            concurrencySpinner.addChangeListener {
                coroutineScope.launch {
                    configStorage.saveConcurrencyLimit(
                        concurrencySpinner.value as Int
                    )
                }
            }
            tokenLimitSpinner.addChangeListener {
                coroutineScope.launch {
                    configStorage.saveTokenLimit(
                        tokenLimitSpinner.value as Int
                    )
                }
            }

            coroutineScope.launch {
                if (configStorage.loadApiKey().isNullOrBlank()) {
                    showOnboardingWizard()
                }
            }
        }

        private fun loadInitialSettings() {
            coroutineScope.launch {
                reviewCheckbox.isSelected = configStorage.loadPreCommitReview()
                concurrencySpinner.value = configStorage.loadConcurrencyLimit()
                tokenLimitSpinner.value = configStorage.loadTokenLimit()
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

            val adapter = AndroidStudioAdapter(project, logger)
            // The config directory is not directly used by the plugin adapter, but PromptManager needs a valid path.
            // We can point it to a temp or config location within the project or user home.
            val configDir = File(System.getProperty("user.home"), ".gemini-orchestrator")
            val promptManager = PromptManager(configDir)

            val geminiService = createGeminiService(configStorage, logger, adapter)

            if (geminiService == null) {
                logger.error("Could not create Gemini Service. Check authentication.")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
                return
            }

            val orchestrator =
                Orchestrator(adapter, logger, configStorage, promptManager, geminiService)

            try {
                orchestrator.run(prompt, project.basePath ?: ".", "IDE Plugin Task", null)
            } catch (e: Exception) {
                logger.error("--- A critical error occurred ---", e)
            } finally {
                logger.info("--- Workflow Finished ---")
                withContext(Dispatchers.Main) { runButton.isEnabled = true }
            }
        }

        private suspend fun createGeminiService(
            config: ConfigStorage,
            logger: ProgressLogger,
            adapter: AndroidStudioAdapter,
        ): GeminiService? {
            val apiKey = getAndValidateApiKey() ?: return null
            val strategicModel = config.loadModelName("strategic", "gemini-1.5-pro-latest")
            val flashModel = config.loadModelName("flash", "gemini-1.5-flash-latest")
            val configDir = File(System.getProperty("user.home"), ".gemini-orchestrator")
            val promptManager = PromptManager(configDir)

            val service = GeminiService(
                authMethod = "apikey",
                apiKey = apiKey,
                logger = logger,
                config = config,
                strategicModelName = strategicModel,
                flashModelName = flashModel,
                promptManager = promptManager,
                adapter = adapter
            )
            service.initialize()
            return service
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