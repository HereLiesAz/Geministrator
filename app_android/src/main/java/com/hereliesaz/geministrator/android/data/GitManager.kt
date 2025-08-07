package com.hereliesaz.geministrator.android.data

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.UUID

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

    fun stageFile(filePath: String): Result<Unit> {
        return try {
            git.add().addFilepattern(filePath).call()
            Result.success(Unit)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    fun commit(message: String): Result<String> {
        return try {
            val revCommit = git.commit().setMessage(message).call()
            Result.success(revCommit.fullMessage)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    fun getStatus(): Result<String> {
        return try {
            val status = git.status().call()
            val statusStringBuilder = StringBuilder()
            status.added.forEach { statusStringBuilder.append("ADDED: $it\n") }
            status.modified.forEach { statusStringBuilder.append("MODIFIED: $it\n") }
            status.removed.forEach { statusStringBuilder.append("REMOVED: $it\n") }
            status.untracked.forEach { statusStringBuilder.append("UNTRACKED: $it\n") }
            Result.success(statusStringBuilder.toString().ifEmpty { "No changes." })
        } catch (e: org.eclipse.jgit.api.errors.NoWorkTreeException) {
            Result.failure(e)
        } catch (e: org.eclipse.jgit.api.errors.GitAPIException) {
            Result.failure(e)
        }
    }

    companion object {
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