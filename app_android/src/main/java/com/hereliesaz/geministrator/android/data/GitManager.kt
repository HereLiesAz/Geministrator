package com.hereliesaz.geministrator.android.data

import android.content.Context
import android.net.Uri
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class GitManager(private val context: Context, private val projectFolderUri: Uri) {

    private val repository: Repository by lazy {
        // JGit needs a File path, but SAF gives us a Uri.
        // A common approach is to copy to a cache or use a proxy.
        // For simplicity, we'll assume a direct path is available for now,
        // but this part will require a more complex SAF-to-File implementation.
        val projectDir = File(projectFolderUri.path!!) // Simplified for now
        val gitDir = File(projectDir, ".git")
        FileRepositoryBuilder().setGitDir(gitDir).build()
    }

    private val git: Git by lazy {
        Git(repository)
    }

    fun init() {
        if (!File(repository.directory.parent, ".git").exists()) {
            Git.init().setDirectory(File(repository.directory.parent)).call()
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
}