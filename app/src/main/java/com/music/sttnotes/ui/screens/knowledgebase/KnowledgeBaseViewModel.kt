package com.music.sttnotes.ui.screens.knowledgebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.llm.FrontmatterParser
import com.music.sttnotes.data.llm.KbFileMeta
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

    // Edit mode state
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _editedContent = MutableStateFlow<String?>(null)
    val editedContent: StateFlow<String?> = _editedContent

    // Tags state
    private val _fileTags = MutableStateFlow<List<String>>(emptyList())
    val fileTags: StateFlow<List<String>> = _fileTags

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags

    private val _tagInput = MutableStateFlow("")
    val tagInput: StateFlow<String> = _tagInput

    private var currentMeta: KbFileMeta? = null

    init {
        loadFolders()
        loadAllTags()
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            _allTags.value = llmOutputRepository.getAllTags()
        }
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
            llmOutputRepository.readFileWithMeta(folder, filename).onSuccess { (meta, content) ->
                currentMeta = meta
                _fileTags.value = meta.tags
                _fileContent.value = FrontmatterParser.combine(meta, content)
            }.onFailure {
                // Fallback to simple read
                llmOutputRepository.readFile(folder, filename).onSuccess { content ->
                    _fileContent.value = content
                    currentMeta = null
                    _fileTags.value = emptyList()
                }
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
        _isEditMode.value = false
        _editedContent.value = null
        _fileTags.value = emptyList()
        _tagInput.value = ""
        currentMeta = null
    }

    // Edit mode functions
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun updateEditedContent(content: String) {
        _editedContent.value = content
    }

    fun saveFileContent(folder: String, filename: String, content: String) {
        viewModelScope.launch {
            // Parse content to separate frontmatter from body
            val (existingMeta, bodyContent) = FrontmatterParser.parse(content)
            // Use existing meta if available, or create new one with current tags
            val meta = currentMeta?.copy(tags = _fileTags.value) ?: existingMeta.copy(tags = _fileTags.value)

            llmOutputRepository.writeFileWithMeta(folder, filename, meta, bodyContent).onSuccess {
                _fileContent.value = FrontmatterParser.combine(meta, bodyContent)
                _editedContent.value = null
                currentMeta = meta
                loadAllTags() // Refresh all tags after save
            }
        }
    }

    // Tag management functions
    fun updateTagInput(input: String) {
        _tagInput.value = input
    }

    fun addTag(tag: String) {
        val trimmedTag = tag.trim()
        if (trimmedTag.isNotEmpty() && !_fileTags.value.contains(trimmedTag)) {
            _fileTags.value = _fileTags.value + trimmedTag
            _tagInput.value = ""
        }
    }

    fun removeTag(tag: String) {
        _fileTags.value = _fileTags.value.filter { it != tag }
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
