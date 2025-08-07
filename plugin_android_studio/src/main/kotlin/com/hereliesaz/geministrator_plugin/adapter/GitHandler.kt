package com.hereliesaz.geministrator_plugin.adapter

import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class GitHandler(private val project: Project, private val logger: ILogger) {

    private fun getRepository(): GitRepository? {
        val repoManager = GitUtil.getRepositoryManager(project)
        val repo = repoManager.repositories.firstOrNull()
        if (repo == null) {
            logger.error("No Git repository found for this project.")
        }
        return repo
    }

    private suspend fun runCommand(handler: GitLineHandler): ExecutionResult =
        withContext(Dispatchers.IO) {
            val result = Git.getInstance().runCommand(handler)
            if (result.exitCode == 0) {
                ExecutionResult(
                    true,
                    result.outputAsJoinedString.ifBlank { "Command executed successfully." })
            } else {
                ExecutionResult(false, result.errorOutputAsJoinedString)
            }
        }

    suspend fun getCurrentBranch(): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val branchName = repo.currentBranch?.name ?: "detached HEAD"
        return ExecutionResult(true, branchName, branchName)
    }

    suspend fun createAndSwitchToBranch(branchName: String): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val handler = GitLineHandler(project, repo.root, GitCommand.CHECKOUT)
        handler.addParameters("-b", branchName)
        return runCommand(handler)
    }

    suspend fun switchToBranch(branchName: String): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val handler = GitLineHandler(project, repo.root, GitCommand.CHECKOUT)
        handler.addParameters(branchName)
        return runCommand(handler)
    }

    suspend fun mergeBranch(branchName: String): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val handler = GitLineHandler(project, repo.root, GitCommand.MERGE)
        handler.addParameters(branchName)
        return runCommand(handler)
    }

    suspend fun deleteBranch(branchName: String): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val handler = GitLineHandler(project, repo.root, GitCommand.BRANCH)
        handler.addParameters("-D", branchName)
        return runCommand(handler)
    }

    suspend fun stageFiles(filePaths: List<String>): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val changeListManager = ChangeListManager.getInstance(project)
            val projectBase = VfsUtil.findFileByIoFile(File(project.basePath!!), true)
                ?: return@withContext ExecutionResult(false, "Could not find project root.")

            val filesToStage = filePaths.mapNotNull { projectBase.findFileByRelativePath(it) }
            val changes = filesToStage.mapNotNull { changeListManager.getChange(it) }

            changeListManager.addChangesToDefaultTracker(changes)

            ExecutionResult(true, "Staged ${filesToStage.size} files.")
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to stage files: ${e.message}")
        }
    }


    suspend fun commit(message: String): ExecutionResult {
        val repo = getRepository() ?: return ExecutionResult(false, "Git repository not found.")
        val handler = GitLineHandler(project, repo.root, GitCommand.COMMIT)
        handler.addParameters("-m", message)
        return runCommand(handler)
    }
}