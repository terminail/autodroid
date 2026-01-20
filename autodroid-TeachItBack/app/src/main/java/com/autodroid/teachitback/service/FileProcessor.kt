package com.autodroid.teachitback.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class FileProcessor(private val context: Context) {

    suspend fun extractTextFromPdf(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                content.append(line).append("\n")
            }

            reader.close()
            inputStream?.close()

            content.toString()
        } catch (e: Exception) {
            throw Exception("Failed to read PDF: ${e.message}")
        }
    }

    suspend fun extractTextFromTextFile(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                content.append(line).append("\n")
            }

            reader.close()
            inputStream?.close()

            content.toString()
        } catch (e: Exception) {
            throw Exception("Failed to read text file: ${e.message}")
        }
    }

    suspend fun extractTextFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri) ?: "text/plain"

        when {
            mimeType.startsWith("text/") || mimeType == "application/json" -> {
                extractTextFromTextFile(uri)
            }
            mimeType == "application/pdf" -> {
                extractTextFromPdf(uri)
            }
            else -> {
                throw Exception("Unsupported file type: $mimeType")
            }
        }
    }
}
