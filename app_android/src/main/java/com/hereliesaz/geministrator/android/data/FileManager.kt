package com.hereliesaz.geministrator.android.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

class FileManager(private val context: Context) {

    /**
     * Launches the system file picker to let the user choose a location to save a file.
     */
    fun saveFile(launcher: ActivityResultLauncher<Intent>, fileName: String, content: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain" // Or any other MIME type
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        // We'll pass the content through a temporary store or another mechanism
        // For now, the ViewModel will hold it.
        launcher.launch(intent)
    }

    /**
     * Writes the given content to the Uri provided by the file picker.
     */
    fun writeFileContent(uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use {
                    it.write(content)
                }
            }
        } catch (e: Exception) {
            // Handle exceptions like IOException
            e.printStackTrace()
        }
    }
}