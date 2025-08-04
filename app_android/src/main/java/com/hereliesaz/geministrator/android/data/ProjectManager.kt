package com.hereliesaz.geministrator.android.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.BufferedReader
import java.io.InputStreamReader

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val PROJECT_URI_KEY = stringPreferencesKey("project_folder_uri")

class ProjectManager(private val context: Context) {

    /**
     * Launches the system's folder picker for the user to select a project directory.
     */
    fun openProjectFolderPicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launcher.launch(intent)
    }

    /**
     * Saves the URI of the folder the user selected, granting persistent access.
     */
    suspend fun onProjectFolderSelected(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.dataStore.edit { settings ->
            settings[PROJECT_URI_KEY] = uri.toString()
        }
    }

    /**
     * Retrieves the saved project folder URI, if one exists.
     */
    suspend fun getProjectFolderUri(): Uri? {
        val uriString = context.dataStore.data.map { it[PROJECT_URI_KEY] }.firstOrNull()
        return uriString?.let { Uri.parse(it) }
    }

    fun writeFile(projectUri: Uri, filePath: String, content: String) {
        val parent = DocumentFile.fromTreeUri(context, projectUri)
        val file = parent?.findFile(filePath) ?: parent?.createFile("text/plain", filePath)
        file?.let {
            context.contentResolver.openOutputStream(it.uri)?.use { stream ->
                stream.bufferedWriter().use { writer -> writer.write(content) }
            }
        }
    }

    fun readFile(projectUri: Uri, filePath: String): String? {
        val parent = DocumentFile.fromTreeUri(context, projectUri)
        val file = parent?.findFile(filePath)
        return file?.let {
            context.contentResolver.openInputStream(it.uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.readText()
                }
            }
        }
    }
}