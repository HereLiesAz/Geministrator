package com.hereliesaz.geministrator.data

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class GitManager(private val projectCacheDir: File) {

    private val git: Git by lazy {
        val gitDir = File(projectCacheDir, ".git")
        // Ensure the repository directory exists before trying to build or wrap it.
        if (!gitDir.exists()) {
            Git.init().setDirectory(projectCacheDir).call()
        }
        val repo = FileRepositoryBuilder().setGitDir(gitDir).build()
        Git(repo)
    }

    private val repository: Repository by lazy {
        git.repository
    }

    fun stageFile(filePath: String): Result<Unit> = runCatching {
        git.add().addFilepattern(filePath).call()
    }

    fun stageFiles(filePaths: List<String>): Result<Unit> = runCatching {
        val addCommand = git.add()
        filePaths.forEach { addCommand.addFilepattern(it) }
        addCommand.call()
    }


    fun commit(message: String): Result<String> = runCatching {
        val revCommit = git.commit().setMessage(message).call()
        revCommit.fullMessage
    }

    fun getDiff(filePath: String): Result<String> = runCatching {
        val head = repository.resolve("HEAD^{tree}")
            ?: return@runCatching "No HEAD commit found to compare against."

        ByteArrayOutputStream().use { out ->
            DiffFormatter(out).use { formatter ->
                formatter.setRepository(repository)
                val oldTreeParser = CanonicalTreeParser().apply {
                    reset(repository.newObjectReader(), head)
                }
                val newTreeParser = org.eclipse.jgit.treewalk.FileTreeIterator(repository)

                val diffEntries = formatter.scan(oldTreeParser, newTreeParser)
                val fileEntry =
                    diffEntries.firstOrNull { it.newPath == filePath || it.oldPath == filePath }

                if (fileEntry != null) {
                    formatter.format(fileEntry)
                    out.toString("UTF-8")
                } else {
                    "No changes found for file: $filePath"
                }
            }
        }
    }

    fun getCurrentBranch(): Result<String> = runCatching {
        repository.fullBranch
    }

    fun createAndSwitchToBranch(branchName: String): Result<Unit> = runCatching {
        git.checkout().setCreateBranch(true).setName(branchName).call()
    }

    fun switchToBranch(branchName: String): Result<Unit> = runCatching {
        git.checkout().setName(branchName).call()
    }

    fun mergeBranch(branchName: String): Result<String> = runCatching {
        val featureBranch = repository.resolve(branchName)

        val mergeResult = git.merge()
            .include(featureBranch)
            .setCommit(true)
            .setMessage("Merge branch '$branchName'")
            .call()

        if (mergeResult.mergeStatus.isSuccessful) {
            "Merged '$branchName' successfully."
        } else {
            // Throw a concrete exception instead of an abstract one.
            throw RuntimeException("Merge conflict for branch '$branchName'. Conflicts: ${mergeResult.conflicts}")
        }
    }

    fun deleteBranch(branchName: String): Result<List<String>> = runCatching {
        git.branchDelete().setBranchNames(branchName).setForce(true).call()
    }


    fun getStatus(): Result<String> = runCatching {
        val status = git.status().call()
        val statusStringBuilder = StringBuilder()
        status.added.forEach { statusStringBuilder.append("ADDED: $it\n") }
        status.modified.forEach { statusStringBuilder.append("MODIFIED: $it\n") }
        status.removed.forEach { statusStringBuilder.append("REMOVED: $it\n") }
        status.untracked.forEach { statusStringBuilder.append("UNTRACKED: $it\n") }
        statusStringBuilder.toString().ifEmpty { "No changes." }
    }

    companion object {
        fun cloneRepository(url: String, context: Context): Result<File> = runCatching {
            val repoName = url.substringAfterLast('/').substringBeforeLast('.')
            val destination = File(context.cacheDir, "cloned_repos/${repoName}_${UUID.randomUUID()}")
            destination.mkdirs()

            Git.cloneRepository()
                .setURI(url)
                .setDirectory(destination)
                .call()

            destination
        }
    }
}