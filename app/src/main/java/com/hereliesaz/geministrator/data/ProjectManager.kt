package com.hereliesaz.geministrator.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectManager(private val application: Application) {

    fun openProjectFolderPicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launcher.launch(intent)
    }

    suspend fun onProjectFolderSelected(uri: Uri) {
        val contentResolver = application.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        // Save the URI for future access
        // For example, in SharedPreferences or a local database
    }

    suspend fun getProjectFolderUri(): Uri? {
        // Retrieve the saved URI
        // For example, from SharedPreferences
        return null
    }

    suspend fun writeFile(uri: Uri, filePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val projectRoot = DocumentFile.fromTreeUri(application, uri)
            val file = projectRoot?.findFile(filePath) ?: projectRoot?.createFile("*/*", filePath)
            file?.let {
                application.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            }
            Unit
        }
    }
}
