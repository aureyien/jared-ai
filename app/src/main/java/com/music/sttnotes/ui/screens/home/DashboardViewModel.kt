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

data class DashboardState(
    val notesCount: Int = 0,
    val lastNote: Note? = null,
    val conversationsCount: Int = 0,
    val lastConversation: ChatConversation? = null,
    val kbItemsCount: Int = 0,
    val lastKbFolder: String? = null,
    val lastKbFile: String? = null,
    val isLlmConfigured: Boolean = false,
    val isLoading: Boolean = true
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
    }

    fun refresh() {
        loadData()
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

            // Get KB data
            val folders = llmOutputRepository.listFolders()
            var totalFiles = 0
            var lastFolder: String? = null
            var lastFile: String? = null

            folders.forEach { folder ->
                val files = llmOutputRepository.listFiles(folder)
                totalFiles += files.size
                if (lastFolder == null && files.isNotEmpty()) {
                    lastFolder = folder
                    lastFile = files.firstOrNull()?.name
                }
            }

            // Check LLM configuration
            val llmProvider = apiConfig.llmProvider.first()
            val isLlmConfigured = when (llmProvider) {
                LlmProvider.GROQ -> apiConfig.groqApiKey.first()?.isNotEmpty() == true
                LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()?.isNotEmpty() == true
                LlmProvider.XAI -> apiConfig.xaiApiKey.first()?.isNotEmpty() == true
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
                isLlmConfigured = isLlmConfigured,
                isLoading = false
            )
        }
    }
}
