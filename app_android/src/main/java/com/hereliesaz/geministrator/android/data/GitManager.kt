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

    fun init(): Result<Unit> = runCatching {
        if (!repository.directory.exists()) {
            Git.init().setDirectory(projectCacheDir).call()
        }
    }

    fun stageFile(filePath: String): Result<Unit> = runCatching {
        git.add().addFilepattern(filePath).call()
    }

    fun commit(message: String): Result<String> = runCatching {
        val revCommit = git.commit().setMessage(message).call()
        revCommit.fullMessage
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