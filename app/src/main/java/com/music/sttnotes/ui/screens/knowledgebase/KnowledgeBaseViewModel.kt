package com.music.sttnotes.ui.screens.knowledgebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.llm.FrontmatterParser
import com.music.sttnotes.data.llm.KbFileMeta
import com.music.sttnotes.data.llm.LlmOutputRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class FilePreview(
    val file: File,
    val preview: String,
    val createdAt: Long,
    val tags: List<String> = emptyList()
)

data class FolderWithFiles(
    val name: String,
    val files: List<FilePreview>,
    val isExpanded: Boolean = false
)

@HiltViewModel
class KnowledgeBaseViewModel @Inject constructor(
    private val llmOutputRepository: LlmOutputRepository,
    private val apiConfig: ApiConfig
) : ViewModel() {

    private val _folders = MutableStateFlow<List<FolderWithFiles>>(emptyList())

    private val _isLlmConfigured = MutableStateFlow(false)
    val isLlmConfigured: StateFlow<Boolean> = _isLlmConfigured
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

    // Tag filter for KB list
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters

    // Selection mode for merge feature
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet()) // filenames in current folder
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles

    // Favorite status for current file
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private var currentMeta: KbFileMeta? = null
    private var currentFolder: String? = null
    private var currentFilename: String? = null

    init {
        loadFolders()
        loadAllTags()
        checkLlmConfiguration()
    }

    private fun checkLlmConfiguration() {
        viewModelScope.launch {
            val llmProvider = apiConfig.llmProvider.first()
            _isLlmConfigured.value = when (llmProvider) {
                LlmProvider.GROQ -> apiConfig.groqApiKey.first()?.isNotEmpty() == true
                LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()?.isNotEmpty() == true
                LlmProvider.XAI -> apiConfig.xaiApiKey.first()?.isNotEmpty() == true
                LlmProvider.ANTHROPIC -> apiConfig.anthropicApiKey.first()?.isNotEmpty() == true
                LlmProvider.NONE -> false
            }
        }
    }

    fun toggleTagFilter(tag: String) {
        _selectedTagFilters.value = if (_selectedTagFilters.value.contains(tag)) {
            _selectedTagFilters.value - tag
        } else {
            _selectedTagFilters.value + tag
        }
        filterFolders()
    }

    fun clearTagFilters() {
        _selectedTagFilters.value = emptySet()
        filterFolders()
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
        val tagFilter = _selectedTagFilters.value

        if (query.isEmpty() && tagFilter.isEmpty()) {
            _filteredFolders.value = _folders.value
        } else {
            _filteredFolders.value = _folders.value.mapNotNull { folder ->
                val matchingFiles = folder.files.filter { file ->
                    val matchesQuery = query.isEmpty() ||
                        file.file.nameWithoutExtension.lowercase().contains(query) ||
                        file.preview.lowercase().contains(query)
                    val matchesTags = tagFilter.isEmpty() ||
                        file.tags.any { it in tagFilter }
                    matchesQuery && matchesTags
                }
                if (matchingFiles.isNotEmpty() || (folder.name.lowercase().contains(query) && tagFilter.isEmpty())) {
                    folder.copy(
                        files = if (folder.name.lowercase().contains(query) && tagFilter.isEmpty()) folder.files.filter { file ->
                            tagFilter.isEmpty() || file.tags.any { it in tagFilter }
                        } else matchingFiles,
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
            // Preserve expansion state of existing folders
            val previousExpansionState = _folders.value.associate { it.name to it.isExpanded }
            val folderNames = llmOutputRepository.listFolders()
            _folders.value = folderNames.map { name ->
                FolderWithFiles(
                    name = name,
                    files = loadFilePreviews(name),
                    isExpanded = previousExpansionState[name] ?: false
                )
            }
            filterFolders() // Apply current search filter
            // Refresh tags from all files
            _allTags.value = llmOutputRepository.getAllTags()
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
            // Parse tags from frontmatter
            val (meta, _) = FrontmatterParser.parse(content)
            FilePreview(
                file = file,
                preview = extractPreview(content),
                createdAt = file.lastModified(),
                tags = meta.tags
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
                _isFavorite.value = meta.favorite
                // Only store the body content, not the frontmatter
                _fileContent.value = content
            }.onFailure {
                // Fallback to simple read - parse to extract body without frontmatter
                llmOutputRepository.readFile(folder, filename).onSuccess { rawContent ->
                    val (meta, bodyContent) = FrontmatterParser.parse(rawContent)
                    _fileContent.value = bodyContent
                    currentMeta = meta
                    _fileTags.value = meta.tags
                    _isFavorite.value = meta.favorite
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

    /**
     * Rename a folder and return the new folder name
     */
    suspend fun renameFolder(oldName: String, newName: String): Result<String> {
        val result = llmOutputRepository.renameFolder(oldName, newName)
        if (result.isSuccess) {
            loadFolders()
        }
        return result
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
            // Content is now pure body (no frontmatter), use currentMeta for metadata
            val meta = currentMeta?.copy(tags = _fileTags.value) ?: KbFileMeta(tags = _fileTags.value)

            llmOutputRepository.writeFileWithMeta(folder, filename, meta, content).onSuccess {
                _fileContent.value = content
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

    fun addTagToFile(folder: String, filename: String, tag: String) {
        viewModelScope.launch {
            llmOutputRepository.addTagToFile(folder, filename, tag.trim().lowercase().take(20)).onSuccess {
                loadAllTags()
                loadFolders()
            }
        }
    }

    fun removeTagFromFile(folder: String, filename: String, tag: String) {
        viewModelScope.launch {
            llmOutputRepository.removeTagFromFile(folder, filename, tag).onSuccess {
                loadAllTags()
                loadFolders()
            }
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            llmOutputRepository.deleteTag(tag).onSuccess { filesModified ->
                loadAllTags() // Refresh all tags
                loadFolders() // Refresh folders to show updated file tags
                // Remove from filter if it was selected
                if (tag in _selectedTagFilters.value) {
                    _selectedTagFilters.value = _selectedTagFilters.value - tag
                }
            }
        }
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

    fun isFileFavorite(folder: String, filename: String): Boolean {
        return llmOutputRepository.getFileFavoriteStatus(folder, filename)
    }

    fun toggleFileFavorite(folder: String, filename: String) {
        viewModelScope.launch {
            llmOutputRepository.toggleFileFavorite(folder, filename)
            // Update local state immediately
            _isFavorite.value = !_isFavorite.value
        }
    }

    // Selection mode functions for merge feature
    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        if (!_selectionMode.value) {
            _selectedFiles.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedFiles.value = emptySet()
    }

    fun toggleFileSelection(filename: String) {
        _selectedFiles.value = if (filename in _selectedFiles.value) {
            _selectedFiles.value - filename
        } else {
            _selectedFiles.value + filename
        }
    }

    fun canMergeSelection(): Boolean {
        return _selectedFiles.value.size >= 2
    }

    fun mergeSelectedFiles(folder: String, newFilename: String) {
        viewModelScope.launch {
            val filenames = _selectedFiles.value.toList()
            if (filenames.size < 2) return@launch

            llmOutputRepository.mergeFiles(folder, filenames, newFilename).onSuccess {
                exitSelectionMode()
                loadFolders()
            }
        }
    }
}
