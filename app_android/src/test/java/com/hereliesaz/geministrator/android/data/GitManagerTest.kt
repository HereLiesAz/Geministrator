package com.hereliesaz.geministrator.android.data

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class GitManagerTest {

    private lateinit var tempDir: File
    private lateinit var gitManager: GitManager

    @Before
    fun setup() {
        tempDir = File.createTempFile("test", "")
        tempDir.delete()
        tempDir.mkdir()
        gitManager = GitManager(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `init creates a git repository`() {
        val result = gitManager.init()
        assert(result.isSuccess)
        assert(File(tempDir, ".git").exists())
    }
}
