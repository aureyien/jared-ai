package com.music.sttnotes.data.llm

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // StateFlow to notify changes in KB files (for reactive UI updates)
    private val _kbChangeCounter = MutableStateFlow(0)
    val kbChangeCounter: StateFlow<Int> = _kbChangeCounter

    /**
     * Emit a change event to trigger observers (Dashboard favorites refresh)
     */
    private fun notifyChange() {
        _kbChangeCounter.value += 1
    }

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
            // Transcription originale goes first (after frontmatter), then separator, then LLM response
            val mdContent = buildString {
                appendLine("---")
                appendLine("created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("folder: $folder")
                appendLine("favorite: false")
                appendLine("---")
                appendLine()
                appendLine("## Original transcription")
                appendLine()
                appendLine("> ${rawTranscription.replace("\n", "\n> ")}")
                appendLine()
                appendLine("---")
                appendLine()
                appendLine(content)
            }

            file.writeText(mdContent)
            Log.d(TAG, "Saved LLM output to: ${file.absolutePath}")
            notifyChange()

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
     * Write/update file content
     */
    suspend fun writeFile(folder: String, filename: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            file.writeText(content)
            Log.d(TAG, "Updated file: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file", e)
            Result.failure(e)
        }
    }

    /**
     * Read file content and parse metadata
     */
    suspend fun readFileWithMeta(folder: String, filename: String): Result<Pair<KbFileMeta, String>> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            val content = file.readText()
            Result.success(FrontmatterParser.parse(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Write file with metadata
     */
    suspend fun writeFileWithMeta(folder: String, filename: String, meta: KbFileMeta, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            file.writeText(FrontmatterParser.combine(meta, content))
            Log.d(TAG, "Updated file with meta: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file", e)
            Result.failure(e)
        }
    }

    /**
     * Get all unique tags across all files
     */
    suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        val tags = mutableSetOf<String>()
        listFolders().forEach { folder ->
            listFiles(folder).forEach { file ->
                try {
                    val (meta, _) = FrontmatterParser.parse(file.readText())
                    tags.addAll(meta.tags)
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        tags.toList().sorted()
    }

    /**
     * Add a tag to a specific file
     */
    suspend fun addTagToFile(folder: String, filename: String, tag: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = getFilePath(folder, filename)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            val content = file.readText()
            val (meta, body) = FrontmatterParser.parse(content)

            val updatedTags = (meta.tags + tag.trim().lowercase()).distinct()
            val updatedMeta = meta.copy(tags = updatedTags)
            val updatedContent = FrontmatterParser.combine(updatedMeta, body)
            file.writeText(updatedContent)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a tag from a specific file
     */
    suspend fun removeTagFromFile(folder: String, filename: String, tag: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = getFilePath(folder, filename)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            val content = file.readText()
            val (meta, body) = FrontmatterParser.parse(content)

            val updatedMeta = meta.copy(tags = meta.tags - tag)
            val updatedContent = FrontmatterParser.combine(updatedMeta, body)
            file.writeText(updatedContent)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a tag from all files
     */
    suspend fun deleteTag(tagToDelete: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var filesModified = 0
            listFolders().forEach { folder ->
                listFiles(folder).forEach { file ->
                    try {
                        val content = file.readText()
                        val (meta, body) = FrontmatterParser.parse(content)

                        if (tagToDelete in meta.tags) {
                            val updatedMeta = meta.copy(tags = meta.tags - tagToDelete)
                            val updatedContent = FrontmatterParser.combine(updatedMeta, body)
                            file.writeText(updatedContent)
                            filesModified++
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
            }
            Result.success(filesModified)
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
            notifyChange()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rename file
     * @return Result with the new filename (sanitized) on success
     */
    suspend fun renameFile(folder: String, oldFilename: String, newFilename: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val folderDir = File(rootDir, sanitizePath(folder))
            val oldFile = File(folderDir, sanitizeFilename(oldFilename))
            val sanitizedNewName = sanitizeFilename(newFilename)
            val newFile = File(folderDir, sanitizedNewName)

            if (newFile.exists() && oldFile.canonicalPath != newFile.canonicalPath) {
                return@withContext Result.failure(Exception("A file with this name already exists"))
            }

            if (oldFile.renameTo(newFile)) {
                Log.d(TAG, "Renamed file from $oldFilename to $sanitizedNewName")
                notifyChange()
                Result.success(sanitizedNewName)
            } else {
                Result.failure(Exception("Rename failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename file", e)
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
            notifyChange()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rename folder
     * @return Result with the new folder name (sanitized) on success
     */
    suspend fun renameFolder(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val oldFolder = File(rootDir, sanitizePath(oldName))
            val sanitizedNewName = sanitizePath(newName)
            val newFolder = File(rootDir, sanitizedNewName)

            if (newFolder.exists() && oldFolder.canonicalPath != newFolder.canonicalPath) {
                return@withContext Result.failure(Exception("A folder with this name already exists"))
            }

            if (oldFolder.renameTo(newFolder)) {
                Log.d(TAG, "Renamed folder from $oldName to $sanitizedNewName")
                notifyChange()
                Result.success(sanitizedNewName)
            } else {
                Result.failure(Exception("Rename failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename folder", e)
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

    /**
     * Toggle favorite status for a KB file
     */
    suspend fun toggleFileFavorite(folder: String, filename: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            val content = file.readText()
            val (meta, body) = FrontmatterParser.parse(content)
            val newFavoriteState = !meta.favorite

            // Update metadata with new favorite state
            val updatedMeta = meta.copy(favorite = newFavoriteState)
            val updatedContent = FrontmatterParser.combine(updatedMeta, body)

            file.writeText(updatedContent)
            Log.d(TAG, "Toggled favorite for $filename to $newFavoriteState")
            notifyChange()
            Result.success(newFavoriteState)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle favorite", e)
            Result.failure(e)
        }
    }

    /**
     * Get favorite status for a KB file
     * Optimized: reads only first 15 lines (frontmatter) instead of entire file
     */
    fun getFileFavoriteStatus(folder: String, filename: String): Boolean {
        return try {
            val file = File(File(rootDir, sanitizePath(folder)), sanitizeFilename(filename))
            if (!file.exists()) return false

            // Read only first 15 lines (frontmatter is always at top)
            val lines = file.bufferedReader().use { reader ->
                (1..15).mapNotNull { reader.readLine() }
            }

            // Check if frontmatter exists
            if (lines.isEmpty() || lines.first().trim() != "---") return false

            // Find closing --- and favorite field
            var foundFavorite = false
            for (line in lines.drop(1)) {
                if (line.trim() == "---") break  // End of frontmatter
                if (line.startsWith("favorite:")) {
                    foundFavorite = line.substringAfter(":").trim().toBoolean()
                    break
                }
            }
            foundFavorite
        } catch (e: Exception) {
            false
        }
    }

    /**
     * List all favorite KB files
     * Returns list of (folder, filename) pairs
     */
    fun listFavoriteFiles(): List<Pair<String, String>> {
        val favorites = mutableListOf<Pair<String, String>>()
        listFolders().forEach { folder ->
            listFiles(folder).forEach { file ->
                if (getFileFavoriteStatus(folder, file.name)) {
                    favorites.add(folder to file.name)
                }
            }
        }
        return favorites
    }

    private fun sanitizePath(path: String): String {
        // Allow alphanumeric (including accented), spaces, underscores, hyphens, and forward slashes
        // Remove only dangerous characters: null bytes, path separators (except /), special chars
        return path.replace(Regex("[\\x00-\\x1F<>:\"|?*\\\\]"), "_")
    }

    private fun sanitizeFilename(filename: String): String {
        // Allow alphanumeric (including accented), spaces, underscores, hyphens, periods
        // Remove only dangerous characters: null bytes, path separators, special chars
        val name = filename.replace(Regex("[\\x00-\\x1F<>:\"|?*\\\\/]"), "_")
        return if (name.endsWith(".md")) name else "$name.md"
    }
}
