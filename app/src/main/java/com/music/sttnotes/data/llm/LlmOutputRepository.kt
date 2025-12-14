package com.music.sttnotes.data.llm

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmOutputRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LlmOutputRepository"
        private const val ROOT_DIR = "llm_outputs"
    }

    private val rootDir: File
        get() = File(context.filesDir, ROOT_DIR).also { it.mkdirs() }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Save LLM output to a markdown file
     */
    suspend fun saveLlmOutput(
        folder: String,
        content: String,
        rawTranscription: String,
        customFilename: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val folderDir = File(rootDir, sanitizePath(folder)).also { it.mkdirs() }

            val filename = customFilename
                ?: "${dateFormat.format(Date())}.md"

            val file = File(folderDir, sanitizeFilename(filename))

            // Build markdown content with metadata
            val mdContent = buildString {
                appendLine("---")
                appendLine("created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("folder: $folder")
                appendLine("---")
                appendLine()
                appendLine(content)
                appendLine()
                appendLine("---")
                appendLine()
                appendLine("## Transcription originale")
                appendLine()
                appendLine("> ${rawTranscription.replace("\n", "\n> ")}")
            }

            file.writeText(mdContent)
            Log.d(TAG, "Saved LLM output to: ${file.absolutePath}")

            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save LLM output", e)
            Result.failure(e)
        }
    }

    /**
     * List all folders
     */
    fun listFolders(): List<String> {
        return rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * List files in a folder
     */
    fun listFiles(folder: String): List<File> {
        val folderDir = File(rootDir, sanitizePath(folder))
        return folderDir.listFiles()
            ?.filter { it.isFile && it.extension == "md" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Read file content
     */
    suspend fun readFile(folder: String, filename: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete file
     */
    suspend fun deleteFile(folder: String, filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete folder and all its contents
     */
    suspend fun deleteFolder(folder: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderDir = File(rootDir, sanitizePath(folder))
            folderDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export file (get absolute path for sharing)
     * Note: filename should already be the actual filename, no sanitization needed
     */
    fun getFilePath(folder: String, filename: String): File {
        return File(File(rootDir, sanitizePath(folder)), filename)
    }

    private fun sanitizePath(path: String): String {
        return path.replace(Regex("[^a-zA-Z0-9_\\-/]"), "_")
    }

    private fun sanitizeFilename(filename: String): String {
        val name = filename.replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
        return if (name.endsWith(".md")) name else "$name.md"
    }
}
