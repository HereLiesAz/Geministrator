package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.config.ConfigStorage

class OrchestratorTest {

    private lateinit var adapter: ExecutionAdapter
    private lateinit var logger: ILogger
    private lateinit var config: ConfigStorage
    private lateinit var promptManager: PromptManager
    private lateinit var ai: GeminiService
    private lateinit var orchestrator: Orchestrator

    @BeforeEach
    fun setUp() {
        adapter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        config = mockk(relaxed = true)
        promptManager = mockk(relaxed = true)
        ai = mockk(relaxed = true)

        // Default mock behaviors
        every { config.loadPreCommitReview() } returns false
        every { config.loadConcurrencyLimit() } returns 1
        every { adapter.execute(AbstractCommand.GetCurrentBranch) } returns ExecutionResult(true, "main")
        every { promptManager.getPrompt(any(), any()) } returns "mocked prompt"

        orchestrator = Orchestrator(adapter, logger, config, promptManager, ai)
    }

    @Test
    fun `run completes a simple workflow successfully`() = runBlocking {
        // Arrange
        val masterPlanJson = """
            {
                "sub_tasks": [
                    {
                        "description": "Implement feature X",
                        "responsible_component": "Backend",
                        "depends_on": []
                    }
                ]
            }
        """.trimIndent()

        val workflowPlanJson = """
            {
                "reasoning": "This is a simple plan to write a file.",
                "steps": [
                    {
                        "command_type": "WRITE_FILE",
                        "parameters": {
                            "path": "src/main/FeatureX.kt",
                            "content": "class FeatureX"
                        }
                    }
                ]
            }
        """.trimIndent()

        every { ai.executeStrategicPrompt(any()) } returns masterPlanJson andThen workflowPlanJson
        every { ai.executeFlashPrompt(any()) } returns """{ "needs_web_research": false, "needs_project_context": false }"""
        every { adapter.execute(AbstractCommand.ReadFile(".orchestrator/session.json")) } returns ExecutionResult(false, "")


        // Act
        orchestrator.run("Implement feature X", "/fake/path", "Test Project", null)

        // Assert
        verify { logger.info("Orchestrator has deconstructed the prompt into 1 sub-tasks.") }
        verify { adapter.execute(AbstractCommand.CreateAndSwitchToBranch("feature/orchestrator-task-0")) }
        verify { adapter.execute(AbstractCommand.WriteFile("src/main/FeatureX.kt", "class FeatureX")) }
        verify { adapter.execute(AbstractCommand.Commit(any())) }
        verify { logger.info("All sub-tasks completed. Beginning final integration.") }
        verify { adapter.execute(AbstractCommand.MergeBranch("feature/orchestrator-task-0")) }
        verify { logger.info("Cleaning up temporary branches...") }
    }
}