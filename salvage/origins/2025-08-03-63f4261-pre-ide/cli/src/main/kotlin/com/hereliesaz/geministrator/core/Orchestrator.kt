package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.core.config.ConfigStorage
import com.hereliesaz.geministrator.core.council.Antagonist
import com.hereliesaz.geministrator.core.council.Architect
import com.hereliesaz.geministrator.core.council.Designer
import com.hereliesaz.geministrator.core.council.Researcher
import com.hereliesaz.geministrator.core.council.TechSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class SessionState(val masterPlan: MasterPlan, val completedBranches: List<String>, val mainBranch: String)

@Serializable
data class MasterPlan(val sub_tasks: List<SubTask>)

@Serializable
data class SubTask(val description: String, val responsible_component: String)

@Serializable
data class WorkflowPlan(val reasoning: String, val steps: List<WorkflowStep>)

@Serializable
data class WorkflowStep(val command_type: String, val parameters: JsonObject)

class Orchestrator(
    private val adapter: ExecutionAdapter,
    private val apiKey: String,
    private val logger: ILogger,
    private val config: ConfigStorage,
) {
    private val ai: GeminiService
    private val architect: Architect
    private val researcher: Researcher
    private val designer: Designer
    private val antagonist: Antagonist
    private val techSupport: TechSupport
    private val jsonParser = Json { isLenient = true; ignoreUnknownKeys = true }
    private val MAX_RETRY_ATTEMPTS = 2
    private val JOURNAL_FILE = ".orchestrator/journal.log"
    private val SESSION_FILE = ".orchestrator/session.json"

    init {
        val strategicModel = config.loadModelName("strategic", "gemini-pro")
        val flashModel = config.loadModelName("flash", "gemini-1.5-flash-latest")
        ai = GeminiService(apiKey, logger, config, strategicModel, flashModel)
        architect = Architect(logger, ai, adapter)
        researcher = Researcher(logger, ai, adapter)
        designer = Designer(logger, adapter)
        antagonist = Antagonist(logger, ai)
        techSupport = TechSupport(logger, ai)
    }

    fun run(prompt: String, projectRoot: String) = runBlocking(Dispatchers.Default) {
        val sessionState = loadSessionState()
        if (sessionState != null) {
            val decision = adapter.execute(
                AbstractCommand.RequestUserDecision(
                    "An incomplete workflow was found. Do you want to resume it?",
                    listOf("Resume", "Start New")
                )
            )
            if (decision.data == "Resume") {
                logger.log("Resuming previous workflow...")
                executeMasterPlan(sessionState.masterPlan, projectRoot, sessionState.mainBranch, sessionState.completedBranches)
                return@runBlocking
            }
        }

        ai.clearSession()
        logger.log("Orchestrator received complex prompt: '$prompt'")
        val mainBranch = adapter.execute(AbstractCommand.GetCurrentBranch).output.trim()
        val masterPlanJson = deconstructPromptIntoSubTasks(prompt)
        if (masterPlanJson.startsWith("Error:")) {
            logger.log("CRITICAL: Could not deconstruct prompt into sub-tasks. AI Failure: $masterPlanJson")
            return@runBlocking
        }
        val masterPlan = jsonParser.decodeFromString<MasterPlan>(masterPlanJson)
        saveSessionState(SessionState(masterPlan, emptyList(), mainBranch))
        executeMasterPlan(masterPlan, projectRoot, mainBranch, emptyList())
    }

    private suspend fun executeMasterPlan(masterPlan: MasterPlan, projectRoot: String, mainBranch: String, completedBranches: List<String>) = coroutineScope {
        val integrationBranch = "integration/orchestrator-${System.currentTimeMillis()}"
        val concurrencyLimit = config.loadConcurrencyLimit()
        val semaphore = Semaphore(concurrencyLimit)
        logger.log("Orchestrator has deconstructed the prompt into ${masterPlan.sub_tasks.size} sub-tasks.")
        logger.log("Concurrency limit set to $concurrencyLimit simultaneous tasks.")

        val tasksToRun = masterPlan.sub_tasks.withIndex().filter { (index, _) ->
            !completedBranches.any { it.contains("task-$index") }
        }

        val managerJobs = tasksToRun.map { (index, subTask) ->
            async {
                semaphore.withPermit {
                    val taskBranch = "feature/orchestrator-task-$index"
                    logger.log("---")
                    logger.log("[STARTING] Manager for '${subTask.description}' on branch '$taskBranch'")
                    val result = handleTask(subTask.description, projectRoot, mutableListOf(), 0, taskBranch)
                    logger.log("[FINISHED] Manager for '${subTask.description}' with result: ${if (result != null) "SUCCESS" else "FAILURE"}")
                    result
                }
            }
        }

        val newSuccessfulBranches = managerJobs.awaitAll().filterNotNull()
        val allSuccessfulBranches = (completedBranches + newSuccessfulBranches).distinct()
        saveSessionState(SessionState(masterPlan, allSuccessfulBranches, mainBranch))

        if (allSuccessfulBranches.size == masterPlan.sub_tasks.size) {
            logger.log("All sub-tasks completed. Beginning final integration.")
            adapter.execute(AbstractCommand.CreateAndSwitchToBranch(integrationBranch))
            var mergeSuccess = true
            var mergeResult: ExecutionResult? = null
            for (branch in allSuccessfulBranches) {
                mergeResult = adapter.execute(AbstractCommand.MergeBranch(branch))
                if (!mergeResult.isSuccess) {
                    logger.log("CRITICAL: Merge conflict detected when merging '$branch'. Halting integration.")
                    mergeSuccess = false
                    break
                }
            }

            if (mergeSuccess) {
                val commitMessage = "feat: ${masterPlan.sub_tasks.joinToString(", ") { it.description }}"
                if (config.loadPreCommitReview()) {
                    val reviewResult = adapter.execute(AbstractCommand.RequestCommitReview(commitMessage))
                    if (reviewResult.data != "APPROVE") {
                        logger.log("User rejected the final commit. Rolling back.")
                        cleanup(integrationBranch, allSuccessfulBranches, mainBranch)
                        return@coroutineScope
                    }
                }
                adapter.execute(AbstractCommand.SwitchToBranch(mainBranch))
                adapter.execute(AbstractCommand.MergeBranch(integrationBranch))
                designer.updateChangelog(commitMessage)
                cleanup(integrationBranch, allSuccessfulBranches, mainBranch)
            } else {
                val analysis = techSupport.analyzeMergeConflict(mergeResult?.output ?: "Unknown error")
                logger.log("--- TECH SUPPORT ANALYSIS ---\n$analysis")
                val userDecision = adapter.execute(
                    AbstractCommand.RequestUserDecision(
                        "A merge conflict occurred. What should we do?",
                        listOf("Abandon", "Attempt AI Fix")
                    )
                )
                if (userDecision.data == "Attempt AI Fix") {
                    // Call handleTask without creating a new branch
                    handleTask("Resolve merge conflict based on Tech Support analysis", projectRoot, mutableListOf(analysis), 0, integrationBranch, createBranch = false)
                } else {
                    cleanup(integrationBranch, allSuccessfulBranches, mainBranch)
                }
            }
        } else {
            logger.log("One or more sub-tasks failed. Commit has been aborted.")
            cleanup(null, allSuccessfulBranches, mainBranch)
        }
    }

    private fun handleTask(prompt: String, projectRoot: String, context: MutableList<String>, attempt: Int, branch: String, createBranch: Boolean = true): String? {
        logJournal("START_TASK", mapOf("prompt" to prompt, "branch" to branch, "attempt" to attempt))

        if (createBranch) {
            adapter.execute(AbstractCommand.CreateAndSwitchToBranch(branch))
        }

        if (attempt > MAX_RETRY_ATTEMPTS) {
            logger.log("Maximum retry attempts reached for task '$prompt'. Halting this sub-task.")
            designer.recordHistoricalLesson("Sub-task '$prompt' failed after $MAX_RETRY_ATTEMPTS self-correction attempts.")
            return null
        }

        if (attempt == 0 && createBranch) { // Only create spec for new feature tasks
            val specWorkflow = designer.createSpecification(prompt)
            Manager(adapter, logger).executeWorkflow(specWorkflow, prompt)
        }

        val bestPractices = researcher.findBestPracticesFor(prompt)
        val projectContext = architect.getProjectContextFor(prompt, projectRoot)
        val allContext = context + listOf(bestPractices, projectContext)
        val planJson = generatePlanWithAI(prompt, *allContext.toTypedArray())

        if (planJson.startsWith("Error:")) {
            logger.log("Failed to generate a plan for '$prompt'. AI Failure: $planJson. Halting task.")
            return null
        }

        val workflowPlan = jsonParser.decodeFromString<WorkflowPlan>(planJson)
        logger.log("AI Reasoning: ${workflowPlan.reasoning}")

        val workflow = convertPlanToWorkflow(workflowPlan)
        if (workflow.isEmpty() && !workflowPlan.reasoning.contains("No changes needed")) {
            logger.log("Generated an empty or invalid workflow. Halting task.")
            return null
        }

        if (workflow.size == 1 && workflow.first() is AbstractCommand.RequestClarification) {
            val clarificationCommand = workflow.first() as AbstractCommand.RequestClarification
            val result = adapter.execute(clarificationCommand)
            val userAnswer = result.data as? String ?: "No response."
            context.add("User Clarification: $userAnswer")
            return handleTask(prompt, projectRoot, context, attempt, branch, createBranch)
        }

        val objection = antagonist.reviewPlan(planJson)
        if (objection != null) {
            logger.log("--- INITIATING SELF-CORRECTION (Attempt ${attempt + 1}) ---")
            context.add("The previous plan was rejected by the Antagonist. Reason: $objection")
            context.add("Please generate a new, valid plan that addresses this specific objection.")
            return handleTask(prompt, projectRoot, context, attempt + 1, branch, createBranch)
        }

        val manager = Manager(adapter, logger)
        val status = manager.executeWorkflow(workflow, prompt)

        return when (status) {
            is WorkflowStatus.Success -> {
                adapter.execute(AbstractCommand.Commit("WIP: ${status.commitMessage}"))
                logJournal("TASK_SUCCESS", mapOf("branch" to branch))
                branch
            }
            is WorkflowStatus.TestsFailed -> {
                logger.log("--- INITIATING SELF-CORRECTION (Attempt ${attempt + 1}) ---")
                context.add("The previous attempt failed with this test error:\n${status.testOutput}")
                context.add("These steps were successful before the failure:\n${status.successfulSteps.joinToString("\n")}")
                handleTask(prompt, projectRoot, context, attempt + 1, branch, createBranch)
            }
            is WorkflowStatus.Failure -> {
                logJournal("TASK_FAILURE", mapOf("branch" to branch, "reason" to status.reason))
                null
            }
        }
    }

    private fun deconstructPromptIntoSubTasks(userPrompt: String): String {
        val prompt = """
            You are an expert project manager. Deconstruct the following high-level user request into a series of smaller, parallelizable sub-tasks.
            You MUST respond with ONLY a single, valid JSON object of the format:
            { "sub_tasks": [ { "description": "...", "responsible_component": "..." } ] }

            User Request: "$userPrompt"
        """.trimIndent()
        return ai.executeStrategicPrompt(prompt)
    }

    private fun cleanup(integrationBranch: String?, featureBranches: List<String>, mainBranch: String) {
        logger.log("Cleaning up temporary branches...")
        adapter.execute(AbstractCommand.SwitchToBranch(mainBranch))
        featureBranches.forEach { adapter.execute(AbstractCommand.DeleteBranch(it)) }
        integrationBranch?.let { adapter.execute(AbstractCommand.DeleteBranch(it)) }
        adapter.execute(AbstractCommand.DeleteFile(SESSION_FILE))
        adapter.execute(AbstractCommand.DeleteFile(JOURNAL_FILE))
        logger.log("Cleanup complete.")
    }

    private fun logJournal(action: String, data: Map<String, Any>) {
        val dataJsonObject = JsonObject(data.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString()) // Fallback for other types
            }
        })
        val entry =
            "{\"timestamp\": ${java.lang.System.currentTimeMillis()}, \"action\": \"$action\", \"data\": ${
                jsonParser.encodeToString(
                    JsonObject.serializer(),
                    dataJsonObject
                )
            }}\n"
        adapter.execute(AbstractCommand.LogJournalEntry(entry))
    }

    private fun saveSessionState(state: SessionState) {
        val json = Json.encodeToString(SessionState.serializer(), state)
        adapter.execute(AbstractCommand.WriteFile(SESSION_FILE, json))
    }

    private fun loadSessionState(): SessionState? {
        val result = adapter.execute(AbstractCommand.ReadFile(SESSION_FILE))
        return if (result.isSuccess && result.data is String && (result.data.isNotBlank())) {
            try { Json.decodeFromString(SessionState.serializer(), result.data) }
            catch (e: Exception) { null }
        } else null
    }

    private fun generatePlanWithAI(userPrompt: String, vararg context: String): String {
        logger.log("Orchestrator: Generating workflow plan with AI...")
        val systemPrompt = """
            You are an expert software development orchestrator. You will be given a user's request and rich context, including the full content of relevant files.
            Your task is to create a precise, step-by-step workflow plan to accomplish the request.
            Your plan must be in a single, valid JSON object.

            IMPORTANT:
            - Your response MUST be only the JSON object, with no other text, comments, or markdown.
            - When modifying a file, your `WRITE_FILE` command must contain the *entire* new content of the file.
            - For `RUN_SHELL`, the `command` parameter MUST be a JSON array of strings (e.g., ["./gradlew", "build"]). Do not use shell operators like && or |; create separate steps.
            - For `STAGE_FILES`, the `paths` parameter MUST be a JSON array of file path strings.
            - If you modify code, you MUST include a `RUN_TESTS` step before the `STAGE_FILES` step.
            - If the provided context is insufficient, your ONLY step should be a `REQUEST_CLARIFICATION` command.
            - If no changes are needed, provide an empty `steps` array and explain why in the `reasoning`.

            CONTEXT PROVIDED:
            ${context.joinToString("\n---\n")}

            Based on the user's request and all the provided file content and context, create the JSON workflow plan.
            User Request: "$userPrompt"
        """.trimIndent()
        return ai.executeStrategicPrompt(systemPrompt)
    }

    private fun convertPlanToWorkflow(plan: WorkflowPlan): List<AbstractCommand> {
        return plan.steps.mapNotNull { step ->
            when (step.command_type) {
                "WRITE_FILE" -> {
                    val path =
                        step.parameters["path"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val content =
                        step.parameters["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    AbstractCommand.WriteFile(path, content)
                }

                "RUN_SHELL" -> {
                    val command =
                        step.parameters["command"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
                            ?: return@mapNotNull null
                    val workDir = step.parameters["workingDir"]?.jsonPrimitive?.content ?: "."
                    AbstractCommand.RunShellCommand(command, workDir)
                }

                "RUN_TESTS" -> {
                    val module = step.parameters["module"]?.jsonPrimitive?.content
                    val testName = step.parameters["testName"]?.jsonPrimitive?.content
                    AbstractCommand.RunTests(module, testName)
                }

                "DISPLAY_MESSAGE" -> {
                    val message =
                        step.parameters["message"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    AbstractCommand.DisplayMessage(message)
                }

                "STAGE_FILES" -> {
                    val paths =
                        step.parameters["paths"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
                            ?: return@mapNotNull null
                    AbstractCommand.StageFiles(paths)
                }

                "REQUEST_CLARIFICATION" -> {
                    val question = step.parameters["question"]?.jsonPrimitive?.content
                        ?: return@mapNotNull null
                    AbstractCommand.RequestClarification(question)
                }
                else -> null
            }
        }
    }
}