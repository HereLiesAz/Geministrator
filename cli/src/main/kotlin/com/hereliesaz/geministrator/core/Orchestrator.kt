package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager
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
import kotlin.system.exitProcess

@Serializable
data class SessionState(
    val masterPlan: MasterPlan,
    val completedBranches: List<String>,
    val completedTaskIndices: Set<Int>, // Explicitly track completed task indices
    val mainBranch: String,
)

@Serializable
data class MasterPlan(val sub_tasks: List<SubTask>)

@Serializable
data class SubTask(
    val description: String,
    val responsible_component: String,
    val depends_on: List<Int> = emptyList(),
)

@Serializable
data class WorkflowPlan(val reasoning: String, val steps: List<WorkflowStep>)

@Serializable
data class WorkflowStep(val command_type: String, val parameters: JsonObject)

@Serializable
private data class TriageResult(
    val needs_web_research: Boolean = false,
    val needs_project_context: Boolean = false,
)


class Orchestrator(
    private val adapter: ExecutionAdapter,
    private val logger: ILogger,
    private val config: ConfigStorage,
    private val promptManager: PromptManager,
    private val ai: GeminiService,
) {
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
        architect = Architect(logger, ai, adapter, promptManager)
        researcher = Researcher(logger, ai, adapter, promptManager)
        designer = Designer(logger, adapter)
        antagonist = Antagonist(logger, ai, promptManager)
        techSupport = TechSupport(logger, ai, promptManager)
    }

    fun run(prompt: String, projectRoot: String, projectType: String, specFileContent: String?) =
        runBlocking(Dispatchers.Default) {
            val sessionState = loadSessionState()
            if (sessionState != null) {
                val decision = adapter.execute(
                    AbstractCommand.RequestUserDecision(
                        "An incomplete workflow was found. Do you want to resume it?",
                        listOf("Resume", "Start New")
                    )
                )
                if (decision.data == "Resume") {
                    logger.info("Resuming previous workflow...")
                    executeMasterPlan(
                        sessionState.masterPlan,
                        projectRoot,
                        sessionState.mainBranch,
                        sessionState.completedBranches.toMutableList(),
                        sessionState.completedTaskIndices.toMutableSet()
                    )
                    return@runBlocking
                }
            }

            ai.clearSession()
            logger.info("Orchestrator received complex prompt: '$prompt'")
            val mainBranch = adapter.execute(AbstractCommand.GetCurrentBranch).output.trim()
            val masterPlanJson = deconstructPromptIntoSubTasks(prompt, projectType, specFileContent)
            if (masterPlanJson.startsWith("Error:")) {
                logger.error("CRITICAL: Could not deconstruct prompt into sub-tasks. AI Failure: $masterPlanJson")
                return@runBlocking
            }
            val masterPlan = jsonParser.decodeFromString<MasterPlan>(masterPlanJson)
            saveSessionState(SessionState(masterPlan, emptyList(), emptySet(), mainBranch))
            executeMasterPlan(masterPlan, projectRoot, mainBranch, mutableListOf(), mutableSetOf())
        }

    private suspend fun executeMasterPlan(
        masterPlan: MasterPlan,
        projectRoot: String,
        mainBranch: String,
        completedBranches: MutableList<String>,
        completedIndices: MutableSet<Int>,
    ) = coroutineScope {
        val integrationBranch = "integration/orchestrator-${System.currentTimeMillis()}"
        val concurrencyLimit = config.loadConcurrencyLimit()
        val semaphore = Semaphore(concurrencyLimit)
        logger.info("Orchestrator has deconstructed the prompt into ${masterPlan.sub_tasks.size} sub-tasks.")
        logger.info("Concurrency limit set to $concurrencyLimit simultaneous tasks.")

        val tasks = masterPlan.sub_tasks.withIndex().toList()
        val runningTasks = mutableSetOf<Int>()

        while (completedIndices.size < tasks.size) {
            val tasksReadyToRun = tasks.filter { (index, subTask) ->
                index !in completedIndices &&
                        index !in runningTasks &&
                        subTask.depends_on.all { it in completedIndices }
            }

            if (tasksReadyToRun.isEmpty() && runningTasks.isEmpty() && completedIndices.size < tasks.size) {
                logger.error("EXECUTION HALTED: Circular dependency or failed tasks detected. Cannot proceed.")
                saveSessionState(
                    SessionState(
                        masterPlan,
                        completedBranches,
                        completedIndices,
                        mainBranch
                    )
                )
                return@coroutineScope
            }

            val jobs = tasksReadyToRun.map { (index, subTask) ->
                runningTasks.add(index)
                async {
                    val taskResult = semaphore.withPermit {
                        val taskBranch = "feature/orchestrator-task-$index"
                        logger.info("---")
                        logger.info("[STARTING] Manager for '${subTask.description}' on branch '$taskBranch'")
                        handleTask(subTask.description, projectRoot, mutableListOf(), 0, taskBranch)
                    }
                    if (taskResult != null) {
                        completedBranches.add(taskResult)
                        completedIndices.add(index)
                        saveSessionState(
                            SessionState(
                                masterPlan,
                                completedBranches,
                                completedIndices,
                                mainBranch
                            )
                        )
                    }
                    runningTasks.remove(index)
                    taskResult != null
                }
            }

            val results = jobs.awaitAll()
            if (results.any { !it }) {
                logger.error("One or more tasks failed in the current wave. Halting further execution.")
                saveSessionState(
                    SessionState(
                        masterPlan,
                        completedBranches,
                        completedIndices,
                        mainBranch
                    )
                )
                return@coroutineScope
            }
        }

        logger.info("All sub-tasks completed. Beginning final integration.")
        adapter.execute(AbstractCommand.CreateAndSwitchToBranch(integrationBranch))
        var mergeSuccess = true
        var mergeResult: ExecutionResult? = null
        for (branch in completedBranches) {
            mergeResult = adapter.execute(AbstractCommand.MergeBranch(branch))
            if (!mergeResult.isSuccess) {
                logger.error("CRITICAL: Merge conflict detected when merging '$branch'. Halting integration.")
                mergeSuccess = false
                break
            }
        }

        if (mergeSuccess) {
            val commitMessage =
                "feat: ${masterPlan.sub_tasks.joinToString(", ") { it.description }}"
            if (config.loadPreCommitReview()) {
                val reviewResult =
                    adapter.execute(AbstractCommand.RequestCommitReview(commitMessage))
                if (reviewResult.data != "APPROVE") {
                    logger.info("User rejected the final commit. Rolling back.")
                    cleanup(integrationBranch, completedBranches, mainBranch)
                    return@coroutineScope
                }
            }
            adapter.execute(AbstractCommand.SwitchToBranch(mainBranch))
            adapter.execute(AbstractCommand.MergeBranch(integrationBranch))
            designer.updateChangelog(commitMessage)
            cleanup(integrationBranch, completedBranches, mainBranch)
        } else {
            val analysis = techSupport.analyzeMergeConflict(mergeResult?.output ?: "Unknown error")
            logger.info("--- TECH SUPPORT ANALYSIS ---\n$analysis")
            val userDecision = adapter.execute(
                AbstractCommand.RequestUserDecision(
                    "A merge conflict occurred. What should we do?",
                    listOf("Abandon", "Attempt AI Fix")
                )
            )
            if (userDecision.data == "Attempt AI Fix") {
                handleTask(
                    "Resolve merge conflict based on Tech Support analysis",
                    projectRoot,
                    mutableListOf(analysis),
                    0,
                    integrationBranch,
                    createBranch = false
                )
            } else {
                cleanup(integrationBranch, completedBranches, mainBranch)
            }
        }
    }

    private fun handleTask(prompt: String, projectRoot: String, context: MutableList<String>, attempt: Int, branch: String, createBranch: Boolean = true): String? {
        logJournal("START_TASK", mapOf("prompt" to prompt, "branch" to branch, "attempt" to attempt))

        if (createBranch) {
            adapter.execute(AbstractCommand.CreateAndSwitchToBranch(branch))
        }

        if (attempt > MAX_RETRY_ATTEMPTS) {
            logger.error("Maximum retry attempts reached for task '$prompt'. Halting this sub-task.")
            designer.recordHistoricalLesson("Sub-task '$prompt' failed after $MAX_RETRY_ATTEMPTS self-correction attempts.")
            return null
        }

        if (attempt == 0 && createBranch) {
            val specWorkflow = designer.createSpecification(prompt)
            Manager(adapter, logger).executeWorkflow(specWorkflow, prompt)
        }

        logger.info("Triaging task to determine necessary agents...")
        val triagePrompt =
            promptManager.getPrompt("orchestrator.triageTask", mapOf("task" to prompt))
        val triageJson = ai.executeFlashPrompt(triagePrompt)
        val triageResult = try {
            jsonParser.decodeFromString<TriageResult>(triageJson)
        } catch (e: Exception) {
            logger.error("Could not parse triage result, defaulting to full context.", e)
            TriageResult(needs_web_research = true, needs_project_context = true)
        }
        logger.info("  -> Triage result: Web research needed: ${triageResult.needs_web_research}, Project context needed: ${triageResult.needs_project_context}")


        val bestPractices = if (triageResult.needs_web_research) {
            researcher.findBestPracticesFor(prompt)
        } else {
            ""
        }

        val projectContext = if (triageResult.needs_project_context) {
            architect.getProjectContextFor(prompt, projectRoot)
        } else {
            ""
        }

        val allContext = context + listOf(bestPractices, projectContext).filter { it.isNotBlank() }
        val planJson = generatePlanWithAI(prompt, *allContext.toTypedArray())

        if (planJson.startsWith("Error:")) {
            logger.error("Failed to generate a plan for '$prompt'. AI Failure: $planJson. Halting task.")
            return null
        }

        val workflowPlan = jsonParser.decodeFromString<WorkflowPlan>(planJson)
        logger.info("AI Reasoning: ${workflowPlan.reasoning}")

        val workflow = convertPlanToWorkflow(workflowPlan)
        if (workflow.isEmpty() && !workflowPlan.reasoning.contains("No changes needed")) {
            logger.error("Generated an empty or invalid workflow. Halting task.")
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
            logger.info("--- INITIATING SELF-CORRECTION (Attempt ${attempt + 1}) ---")
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
                logger.info("--- INITIATING SELF-CORRECTION (Attempt ${attempt + 1}) ---")
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

    private fun deconstructPromptIntoSubTasks(
        userPrompt: String,
        projectType: String,
        specFileContent: String?,
    ): String {
        val specContent = if (specFileContent != null) {
            "PROJECT SPECIFICATION:\n$specFileContent"
        } else {
            ""
        }
        val prompt = promptManager.getPrompt(
            "orchestrator.deconstructPrompt", mapOf(
                "userPrompt" to userPrompt,
                "projectType" to projectType,
                "specFileContent" to specContent
            )
        )
        return ai.executeStrategicPrompt(prompt)
    }

    private fun cleanup(integrationBranch: String?, featureBranches: List<String>, mainBranch: String) {
        logger.info("Cleaning up temporary branches...")
        adapter.execute(AbstractCommand.SwitchToBranch(mainBranch))
        featureBranches.forEach { adapter.execute(AbstractCommand.DeleteBranch(it)) }
        integrationBranch?.let { adapter.execute(AbstractCommand.DeleteBranch(it)) }
        adapter.execute(AbstractCommand.DeleteFile(SESSION_FILE))
        adapter.execute(AbstractCommand.DeleteFile(JOURNAL_FILE))
        logger.info("Cleanup complete.")
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
        val json = jsonParser.encodeToString(SessionState.serializer(), state)
        adapter.execute(AbstractCommand.WriteFile(SESSION_FILE, json))
    }

    private fun loadSessionState(): SessionState? {
        val result = adapter.execute(AbstractCommand.ReadFile(SESSION_FILE))
        return if (result.isSuccess && result.data is String && (result.data.isNotBlank())) {
            try {
                jsonParser.decodeFromString(SessionState.serializer(), result.data)
            } catch (e: Exception) {
                logger.error("Could not parse session file, starting new session.", e)
                null
            }
        } else null
    }

    private fun generatePlanWithAI(userPrompt: String, vararg context: String): String {
        logger.info("Orchestrator: Generating workflow plan with AI...")
        val systemPrompt = promptManager.getPrompt(
            "orchestrator.generatePlan",
            mapOf("context" to context.joinToString("\n---\n"), "userPrompt" to userPrompt)
        )
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
                "PAUSE_AND_EXIT" -> {
                    val message = step.parameters["checkInMessage"]?.jsonPrimitive?.content
                        ?: "Execution paused."
                    AbstractCommand.PauseAndExit(message)
                }
                else -> null
            }
        }
    }
}