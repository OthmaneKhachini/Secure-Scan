package com.example.securescan.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Utility object for file-related operations, specifically for handling file uploads.
 */
object FileUtils {
    
    /**
     * Represents a file and its corresponding multipart body part for API submission.
     */
    data class FilePayload(val part: MultipartBody.Part, val file: File)

    /**
     * Processes a Uri into a FilePayload containing the physical file and its upload-ready part.
     *
     * @param context Application context.
     * @param uri The Uri of the selected file.
     * @param partName The name of the form field for the multipart request.
     * @return FilePayload or null if processing fails.
     */
    fun getFilePayload(context: Context, uri: Uri, partName: String): FilePayload? {
        val file = getFileFromUri(context, uri) ?: return null
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(partName, file.name, requestFile)
        return FilePayload(part, file)
    }

    /**
     * Copies a file from a Uri into the application's internal cache directory.
     */
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "temp_file_${System.currentTimeMillis()}"
        val tempFile = File(context.cacheDir, fileName)
        
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(4096)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            return tempFile
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Extracts the file name from a Uri.
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name ?: "temp_file_${System.currentTimeMillis()}"
    }
}