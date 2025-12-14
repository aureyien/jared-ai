package com.music.sttnotes.ui.screens.knowledgebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.llm.LlmOutputRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class FilePreview(
    val file: File,
    val preview: String,
    val createdAt: Long
)

data class FolderWithFiles(
    val name: String,
    val files: List<FilePreview>,
    val isExpanded: Boolean = false
)

@HiltViewModel
class KnowledgeBaseViewModel @Inject constructor(
    private val llmOutputRepository: LlmOutputRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<FolderWithFiles>>(emptyList())
    val folders: StateFlow<List<FolderWithFiles>> = _folders

    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent: StateFlow<String?> = _fileContent

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredFolders = MutableStateFlow<List<FolderWithFiles>>(emptyList())
    val filteredFolders: StateFlow<List<FolderWithFiles>> = _filteredFolders

    init {
        loadFolders()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterFolders()
    }

    private fun filterFolders() {
        val query = _searchQuery.value.lowercase().trim()
        if (query.isEmpty()) {
            _filteredFolders.value = _folders.value
        } else {
            _filteredFolders.value = _folders.value.mapNotNull { folder ->
                val matchingFiles = folder.files.filter { file ->
                    file.file.nameWithoutExtension.lowercase().contains(query) ||
                    file.preview.lowercase().contains(query)
                }
                if (matchingFiles.isNotEmpty() || folder.name.lowercase().contains(query)) {
                    folder.copy(
                        files = if (folder.name.lowercase().contains(query)) folder.files else matchingFiles,
                        isExpanded = true // Auto-expand folders with matches
                    )
                } else {
                    null
                }
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            val folderNames = llmOutputRepository.listFolders()
            _folders.value = folderNames.map { name ->
                FolderWithFiles(
                    name = name,
                    files = loadFilePreviews(name),
                    isExpanded = false
                )
            }
            filterFolders() // Apply current search filter
            _isLoading.value = false
        }
    }

    private suspend fun loadFilePreviews(folder: String): List<FilePreview> = withContext(Dispatchers.IO) {
        llmOutputRepository.listFiles(folder).map { file ->
            val content = try {
                file.readText()
            } catch (e: Exception) {
                ""
            }
            FilePreview(
                file = file,
                preview = extractPreview(content),
                createdAt = file.lastModified()
            )
        }
    }

    private fun extractPreview(content: String): String {
        val lines = content.lines()
        // Skip YAML frontmatter (between --- markers)
        val startIndex = if (lines.firstOrNull()?.trim() == "---") {
            val endFrontmatter = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (endFrontmatter >= 0) endFrontmatter + 2 else 0
        } else 0

        return lines.drop(startIndex)
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith(">") }
            .joinToString(" ")
            .take(120)
            .trim()
            .let { if (it.length >= 120) "$it..." else it }
    }

    fun toggleFolder(folderName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.name == folderName) folder.copy(isExpanded = !folder.isExpanded)
            else folder
        }
        filterFolders() // Update filtered list to reflect expansion state
    }

    fun loadFileContent(folder: String, filename: String) {
        viewModelScope.launch {
            llmOutputRepository.readFile(folder, filename).onSuccess { content ->
                _fileContent.value = content
            }
        }
    }

    fun deleteFile(folder: String, filename: String) {
        viewModelScope.launch {
            llmOutputRepository.deleteFile(folder, filename)
            loadFolders()
        }
    }

    /**
     * Rename a file and return the new filename
     */
    suspend fun renameFile(folder: String, oldFilename: String, newFilename: String): Result<String> {
        val result = llmOutputRepository.renameFile(folder, oldFilename, newFilename)
        if (result.isSuccess) {
            loadFolders()
        }
        return result
    }

    fun deleteFolder(folder: String) {
        viewModelScope.launch {
            llmOutputRepository.deleteFolder(folder)
            loadFolders()
        }
    }

    fun clearFileContent() {
        _fileContent.value = null
    }

    fun getFile(folder: String, filename: String): File {
        return llmOutputRepository.getFilePath(folder, filename)
    }

    fun getFileContent(folder: String, filename: String): String? {
        return try {
            llmOutputRepository.getFilePath(folder, filename).readText()
        } catch (e: Exception) {
            null
        }
    }
}
