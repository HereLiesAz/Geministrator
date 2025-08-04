package com.hereliesaz.geministrator.android.data

import androidx.room.jarjarred.org.antlr.v4.tool.Rule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `init creates a git repository`() {
        val gitManager = GitManager(tempFolder.root)
        val result = gitManager.init()
        assertTrue(result.isSuccess)
        assertTrue(File(tempFolder.root, ".git").exists())
    }

    @Test
    fun `getStatus shows untracked file`() {
        val gitManager = GitManager(tempFolder.root)
        gitManager.init()

        // Create a new file
        File(tempFolder.root, "test.txt").writeText("hello world")

        val statusResult = gitManager.getStatus()
        assertTrue(statusResult.isSuccess)
        assertTrue(statusResult.getOrThrow().contains("UNTRACKED: test.txt"))
    }

    @Test
    fun `stageFile and commit work correctly`() {
        val gitManager = GitManager(tempFolder.root)
        gitManager.init()

        val testFile = File(tempFolder.root, "test.txt")
        testFile.writeText("hello world")

        // Stage the file
        val stageResult = gitManager.stageFile("test.txt")
        assertTrue(stageResult.isSuccess)

        // Check status, should now be "ADDED"
        val statusAfterStage = gitManager.getStatus().getOrThrow()
        assertTrue(statusAfterStage.contains("ADDED: test.txt"))

        // Commit the file
        val commitResult = gitManager.commit("Initial commit")
        assertTrue(commitResult.isSuccess)

        // Check status, should now be clean
        val statusAfterCommit = gitManager.getStatus().getOrThrow()
        assertTrue(statusAfterCommit.contains("No changes."))
    }
}