package com.music.sttnotes.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.data.chat.ChatHistoryRepository
import com.music.sttnotes.data.llm.LlmOutputRepository
import com.music.sttnotes.data.notes.Note
import com.music.sttnotes.data.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Search result types
sealed class GlobalSearchResult {
    data class NoteResult(val note: Note) : GlobalSearchResult()
    data class ConversationResult(val conversation: ChatConversation) : GlobalSearchResult()
    data class KbResult(val folder: String, val filename: String, val preview: String) : GlobalSearchResult()
}

// Favorite filter types
enum class FavoriteFilter {
    ALL, NOTES, KB, CHAT
}

sealed class FavoriteItem {
    data class NoteItem(val note: Note) : FavoriteItem()
    data class ConversationItem(val conversation: ChatConversation) : FavoriteItem()
    data class KbItem(val folder: String, val filename: String) : FavoriteItem()
}

data class KbPreviewItem(
    val folder: String,
    val filename: String,
    val preview: String,
    val lastModified: Long
)

data class DashboardState(
    val notesCount: Int = 0,
    val lastNote: Note? = null,
    val conversationsCount: Int = 0,
    val lastConversation: ChatConversation? = null,
    val kbItemsCount: Int = 0,
    val lastKbFolder: String? = null,
    val lastKbFile: String? = null,
    val lastKbItems: List<KbPreviewItem> = emptyList(),
    val isLlmConfigured: Boolean = false,
    val isLoading: Boolean = true,
    // Search state
    val searchQuery: String = "",
    val searchResults: List<GlobalSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    // Favorites state
    val favoriteItems: List<FavoriteItem> = emptyList(),
    val favoriteFilter: FavoriteFilter = FavoriteFilter.ALL
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val llmOutputRepository: LlmOutputRepository,
    private val apiConfig: ApiConfig
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        loadData()
        observeFavoritesChanges()
    }

    fun refresh() {
        loadData()
    }

    /**
     * Observe changes in repositories to auto-refresh favorites
     */
    private fun observeFavoritesChanges() {
        viewModelScope.launch {
            // Watch for changes in notes, conversations, and KB files
            kotlinx.coroutines.flow.combine(
                notesRepository.notes,
                chatHistoryRepository.conversations,
                llmOutputRepository.kbChangeCounter
            ) { _, _, _ ->
                // Trigger on any change
            }.collect {
                // Refresh favorites when data changes
                refreshFavorites()
            }
        }
    }

    /**
     * Refresh only favorites without full reload
     */
    private fun refreshFavorites() {
        viewModelScope.launch {
            val favoriteNotes = notesRepository.getFavoriteNotes()
            val favoriteConversations = chatHistoryRepository.getFavoriteConversations()
            val favoriteKbFiles = llmOutputRepository.listFavoriteFiles()

            val favoriteItems = mutableListOf<FavoriteItem>()
            favoriteNotes.forEach { favoriteItems.add(FavoriteItem.NoteItem(it)) }
            favoriteConversations.forEach { favoriteItems.add(FavoriteItem.ConversationItem(it)) }
            favoriteKbFiles.forEach { (folder, filename) ->
                favoriteItems.add(FavoriteItem.KbItem(folder, filename))
            }

            _state.value = _state.value.copy(favoriteItems = favoriteItems)
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
        } else {
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            val results = mutableListOf<GlobalSearchResult>()
            val queryLower = query.lowercase()

            // Search in notes
            val notes = notesRepository.notes.value
            notes.filter { note ->
                note.title.lowercase().contains(queryLower) ||
                note.content.lowercase().contains(queryLower) ||
                note.tags.any { it.lowercase().contains(queryLower) }
            }.take(5).forEach { note ->
                results.add(GlobalSearchResult.NoteResult(note))
            }

            // Search in conversations
            val conversations = chatHistoryRepository.conversations.value
            conversations.filter { conv ->
                conv.title.lowercase().contains(queryLower) ||
                conv.messages.any { msg ->
                    msg.content.lowercase().contains(queryLower)
                }
            }.take(5).forEach { conv ->
                results.add(GlobalSearchResult.ConversationResult(conv))
            }

            // Search in KB files
            val folders = llmOutputRepository.listFolders()
            var kbCount = 0
            for (folder in folders) {
                if (kbCount >= 5) break
                val files = llmOutputRepository.listFiles(folder)
                for (file in files) {
                    if (kbCount >= 5) break
                    val filename = file.name.removeSuffix(".md")
                    val content = llmOutputRepository.readFile(folder, file.name).getOrNull() ?: ""
                    if (filename.lowercase().contains(queryLower) ||
                        folder.lowercase().contains(queryLower) ||
                        content.lowercase().contains(queryLower)) {
                        val preview = content.take(100).replace("\n", " ")
                        results.add(GlobalSearchResult.KbResult(folder, file.name, preview))
                        kbCount++
                    }
                }
            }

            _state.value = _state.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun setFavoriteFilter(filter: FavoriteFilter) {
        _state.value = _state.value.copy(favoriteFilter = filter)
    }

    fun getFilteredFavorites(): List<FavoriteItem> {
        val items = _state.value.favoriteItems
        return when (_state.value.favoriteFilter) {
            FavoriteFilter.ALL -> items
            FavoriteFilter.NOTES -> items.filterIsInstance<FavoriteItem.NoteItem>()
            FavoriteFilter.KB -> items.filterIsInstance<FavoriteItem.KbItem>()
            FavoriteFilter.CHAT -> items.filterIsInstance<FavoriteItem.ConversationItem>()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Initialize repositories
            notesRepository.initialize()
            chatHistoryRepository.initialize()

            // Get notes data
            val notes = notesRepository.notes.value
            val notesCount = notes.size
            val lastNote = notes.firstOrNull()

            // Get conversations data
            val conversations = chatHistoryRepository.conversations.value
            val conversationsCount = conversations.size
            val lastConversation = conversations.firstOrNull()

            // Get KB data - find most recently modified files across all folders
            val folders = llmOutputRepository.listFolders()
            var totalFiles = 0
            var lastFolder: String? = null
            var lastFile: String? = null
            var lastModifiedTime: Long = 0

            // Collect all KB files with their metadata for sorting
            val allKbItems = mutableListOf<KbPreviewItem>()

            folders.forEach { folder ->
                val files = llmOutputRepository.listFiles(folder)
                totalFiles += files.size
                // Check if this folder has a more recent file
                files.firstOrNull()?.let { file ->
                    if (file.lastModified() > lastModifiedTime) {
                        lastModifiedTime = file.lastModified()
                        lastFolder = folder
                        lastFile = file.name
                    }
                }

                // Collect all files with previews
                files.forEach { file ->
                    val content = llmOutputRepository.readFile(folder, file.name).getOrNull() ?: ""
                    // Extract only the LLM answer (skip frontmatter and original transcription)
                    // Regular file structure: ---\nmetadata\n---\n\n## Original transcription\n...\n---\n\nLLM ANSWER
                    // Merged file structure: ---\nmetadata\n---\n\n## From: file1\n\n## Original transcription\n...\n---\n\nAnswer1\n\n---\n\n## From: file2...

                    val answerContent = if (content.contains("---")) {
                        val parts = content.split("---")
                        if (parts.size >= 3) {
                            // Skip parts[0] (empty) and parts[1] (frontmatter)
                            // Join the rest and extract content after "## Original transcription" section
                            val bodyParts = parts.drop(2)
                            val fullBody = bodyParts.joinToString("---")

                            // Find last occurrence of "## Original transcription" block and take content after its separator
                            val transcriptionPattern = """## Original transcription.*?---""".toRegex(RegexOption.DOT_MATCHES_ALL)
                            transcriptionPattern.replace(fullBody, "").trim()
                        } else {
                            content
                        }
                    } else {
                        content
                    }

                    val preview = answerContent.take(200).replace("\n", " ")
                    allKbItems.add(KbPreviewItem(
                        folder = folder,
                        filename = file.name,
                        preview = preview,
                        lastModified = file.lastModified()
                    ))
                }
            }

            // Get the last 3 most recently modified KB items
            val lastKbItems = allKbItems
                .sortedByDescending { it.lastModified }
                .take(3)

            // Load favorite items
            val favoriteNotes = notesRepository.getFavoriteNotes()
            val favoriteConversations = chatHistoryRepository.getFavoriteConversations()
            val favoriteKbFiles = llmOutputRepository.listFavoriteFiles()

            val favoriteItems = mutableListOf<FavoriteItem>()
            favoriteNotes.forEach { favoriteItems.add(FavoriteItem.NoteItem(it)) }
            favoriteConversations.forEach { favoriteItems.add(FavoriteItem.ConversationItem(it)) }
            favoriteKbFiles.forEach { (folder, filename) ->
                favoriteItems.add(FavoriteItem.KbItem(folder, filename))
            }

            // Check LLM configuration
            val llmProvider = apiConfig.llmProvider.first()
            val isLlmConfigured = when (llmProvider) {
                LlmProvider.GROQ -> apiConfig.groqApiKey.first()?.isNotEmpty() == true
                LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()?.isNotEmpty() == true
                LlmProvider.XAI -> apiConfig.xaiApiKey.first()?.isNotEmpty() == true
                LlmProvider.ANTHROPIC -> apiConfig.anthropicApiKey.first()?.isNotEmpty() == true
                LlmProvider.NONE -> false
            }

            _state.value = DashboardState(
                notesCount = notesCount,
                lastNote = lastNote,
                conversationsCount = conversationsCount,
                lastConversation = lastConversation,
                kbItemsCount = totalFiles,
                lastKbFolder = lastFolder,
                lastKbFile = lastFile,
                lastKbItems = lastKbItems,
                isLlmConfigured = isLlmConfigured,
                isLoading = false,
                favoriteItems = favoriteItems,
                favoriteFilter = FavoriteFilter.ALL
            )
        }
    }

    // Toggle favorite methods
    fun toggleNoteFavorite(noteId: String) {
        viewModelScope.launch {
            notesRepository.toggleNoteFavorite(noteId)
        }
    }

    fun toggleConversationFavorite(conversationId: String) {
        viewModelScope.launch {
            chatHistoryRepository.toggleConversationFavorite(conversationId)
        }
    }

    fun toggleFileFavorite(folder: String, filename: String) {
        viewModelScope.launch {
            llmOutputRepository.toggleFileFavorite(folder, filename)
        }
    }
}
