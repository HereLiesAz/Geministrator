package com.hereliesaz.geministrator.android.data

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class GitManager(private val projectCacheDir: File) {

    private val repository: Repository by lazy {
        val gitDir = File(projectCacheDir, ".git")
        FileRepositoryBuilder()
            .setGitDir(gitDir)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
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
        [cite_start]status.added.forEach { statusStringBuilder.append("ADDED: $it\n") } [cite: 398]
        [cite_start]status.modified.forEach { statusStringBuilder.append("MODIFIED: $it\n") } [cite: 398]
        [cite_start]status.removed.forEach { statusStringBuilder.append("REMOVED: $it\n") } [cite: 398]
        [cite_start]status.untracked.forEach { statusStringBuilder.append("UNTRACKED: $it\n") } [cite: 398]
        statusStringBuilder.toString().ifEmpty { "No changes." [cite_start]} [cite: 399]
    }
}