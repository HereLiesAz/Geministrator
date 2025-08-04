package com.hereliesaz.geministrator.android.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val PROJECT_URI_KEY = stringPreferencesKey("project_folder_uri")

class ProjectManager(private val context: Context) {

    private val TAG = "ProjectManager"

    fun openProjectFolderPicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        launcher.launch(intent)
    }

    suspend fun onProjectFolderSelected(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.dataStore.edit { settings ->
                settings[PROJECT_URI_KEY] = uri.toString()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to persist URI permission", e)
        }
    }

    suspend fun getProjectFolderUri(): Uri? {
        return try {
            val uriString = context.dataStore.data.map { it[PROJECT_URI_KEY] }.firstOrNull()
            uriString?.let { Uri.parse(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve project folder URI", e)
            null
        }
    }

    fun writeFile(projectUri: Uri, filePath: String, content: String): Result<Unit> = runCatching {
        try {
            val parent = DocumentFile.fromTreeUri(context, projectUri)
            val file = parent?.findFile(filePath) ?: parent?.createFile("text/plain", filePath)
            file?.let {
                context.contentResolver.openOutputStream(it.uri)?.use { stream ->
                    stream.bufferedWriter().use { writer -> writer.write(content) }
                }
            } ?: throw FileNotFoundException("Could not find or create file at path: $filePath")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing file: $filePath", e)
            throw e // Re-throw to be caught by runCatching
        }
    }

    fun readFile(projectUri: Uri, filePath: String): Result<String> = runCatching {
        try {
            val parent = DocumentFile.fromTreeUri(context, projectUri)
            val file = parent?.findFile(filePath)
            file?.let {
                context.contentResolver.openInputStream(it.uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
            } ?: throw FileNotFoundException("File not found: $filePath")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading file: $filePath", e)
            throw e // Re-throw to be caught by runCatching
        }
    }
}