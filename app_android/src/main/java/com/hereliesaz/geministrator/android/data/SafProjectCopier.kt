package com.hereliesaz.geministrator.android.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

object SafProjectCopier {

    /**
     * Copies a directory from a SAF Uri to a local cache directory.
     * This is necessary for libraries like JGit that require direct File API access.
     */
    fun copyProjectToCache(context: Context, projectUri: Uri): File {
        val cacheDir = File(context.cacheDir, "project_copy_${System.currentTimeMillis()}")
        cacheDir.mkdirs()

        val rootDocument = DocumentFile.fromTreeUri(context, projectUri)
        rootDocument?.let { doc ->
            copyDirectory(context, doc, cacheDir)
        }
        return cacheDir
    }

    private fun copyDirectory(context: Context, sourceDir: DocumentFile, destinationDir: File) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        sourceDir.listFiles().forEach { sourceFile ->
            val destinationFile = File(destinationDir, sourceFile.name ?: "unknown_file")
            if (sourceFile.isDirectory) {
                copyDirectory(context, sourceFile, destinationFile)
            } else {
                copyFile(context, sourceFile, destinationFile)
            }
        }
    }

    private fun copyFile(context: Context, sourceFile: DocumentFile, destinationFile: File) {
        try {
            context.contentResolver.openInputStream(sourceFile.uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            // In a real implementation, propagate this error to the UI
            e.printStackTrace()
        }
    }

    /**
     * Syncs changes from the local cache back to the original SAF Uri.
     * (This is a simplified example; a real implementation needs robust diffing.)
     */
    fun syncCacheToSaf(context: Context, cacheDir: File, projectUri: Uri) {
        val projectRoot = DocumentFile.fromTreeUri(context, projectUri) ?: return
        val cacheFiles = cacheDir.walk().filter { it.isFile }.toList()

        cacheFiles.forEach { cacheFile ->
            val relativePath = cacheFile.relativeTo(cacheDir).path
            // This is a naive implementation. A real one would check for existing files
            // and handle updates and deletes.
            writeFileToSaf(context, projectRoot, relativePath, cacheFile.readText())
        }
    }

    private fun writeFileToSaf(context: Context, root: DocumentFile, path: String, content: String) {
        val parts = path.split(File.separator)
        var currentDir = root
        // Traverse or create directories
        parts.dropLast(1).forEach { dirName ->
            val nextDir = currentDir.findFile(dirName) ?: currentDir.createDirectory(dirName)
            currentDir = nextDir ?: return // Stop if directory creation fails
        }

        // Create or find the file
        val fileName = parts.last()
        val file = currentDir.findFile(fileName) ?: currentDir.createFile("text/plain", fileName)

        // Write content to the file
        file?.let {
            try {
                context.contentResolver.openOutputStream(it.uri)?.use { stream ->
                    stream.bufferedWriter().write(content)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}