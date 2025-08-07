package com.hereliesaz.geministrator.android.data

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.UUID

data class GitStatus(
    val added: Set<String> = emptySet(),
    val modified: Set<String> = emptySet(),
    val removed: Set<String> = emptySet(),
    val untracked: Set<String> = emptySet()
)

/**
 * Manages Git operations for a given project cache directory.
 * @param projectCacheDir The directory where the project is cached.
 */
class GitManager(private val projectCacheDir: File) {

    private val repository: Repository by lazy {
        val gitDir = File(projectCacheDir, ".git")
        FileRepositoryBuilder()
            .setGitDir(gitDir)
            .readEnvironment()
            .findGitDir()
            .build()
    }

    private val git: Git by lazy {
        Git(repository)
    }

    /**
     * Initializes a new Git repository in the project cache directory if one doesn't already exist.
     * @return A [Result] indicating success or failure.
     */
    fun init(): Result<Unit> {
        return try {
            if (!repository.directory.exists()) {
                Git.init().setDirectory(projectCacheDir).call()
            }
            Result.success(Unit)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    /**
     * Stages a file for commit.
     * @param filePath The path of the file to stage.
     * @return A [Result] indicating success or failure.
     */
    fun stageFile(filePath: String): Result<Unit> {
        return try {
            git.add().addFilepattern(filePath).call()
            Result.success(Unit)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    /**
     * Commits the staged files.
     * @param message The commit message.
     * @return A [Result] containing the full commit message on success, or an exception on failure.
     */
    fun commit(message: String): Result<String> {
        return try {
            val revCommit = git.commit().setMessage(message).call()
            Result.success(revCommit.fullMessage)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    /**
     * Gets the diff of a file.
     * @param filePath The path of the file.
     * @return A [Result] containing the diff on success, or an exception on failure.
     */
    fun getDiff(filePath: String): Result<String> {
        return try {
            val diffStream = java.io.ByteArrayOutputStream()
            git.diff().setOutputStream(diffStream).addPath(filePath).call()
            Result.success(diffStream.toString())
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    /**
     * Gets the status of the repository.
     * @return A [Result] containing the [GitStatus] on success, or an exception on failure.
     */
    fun getStatus(): Result<GitStatus> {
        return try {
            val status = git.status().call()
            val gitStatus = GitStatus(
                added = status.added,
                modified = status.modified,
                removed = status.removed,
                untracked = status.untracked
            )
            Result.success(gitStatus)
        } catch (e: org.eclipse.jgit.api.errors.NoWorkTreeException) {
            Result.failure(e)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Clones a repository from a URL.
         * @param url The URL of the repository to clone.
         * @param context The application context.
         * @return A [Result] containing the path to the cloned repository on success, or an exception on failure.
         */
        fun cloneRepository(url: String, context: Context): Result<File> {
            return try {
                val repoName = url.substringAfterLast('/').substringBeforeLast('.')
                val destination =
                    File(context.cacheDir, "cloned_repos/${repoName}_${UUID.randomUUID()}")
                destination.mkdirs()

                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination)
                    .call()

                Result.success(destination)
            } catch (e: org.eclipse.jgit.api.errors.InvalidRemoteException) {
                Result.failure(e)
            } catch (e: org.eclipse.jgit.api.errors.TransportException) {
                Result.failure(e)
            } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
                Result.failure(e)
            }
        }
    }
}