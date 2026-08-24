package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileHelper {
    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
