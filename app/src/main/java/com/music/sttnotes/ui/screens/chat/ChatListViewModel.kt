package com.music.sttnotes.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.data.chat.ChatHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    val conversations: StateFlow<List<ChatConversation>> = chatHistoryRepository.conversations

    // All tags (persisted even when not assigned to any conversation)
    val allTags: StateFlow<Set<String>> = chatHistoryRepository.allTags

    // Tags currently used in conversations (for filtering UI)
    val usedTags: StateFlow<List<String>> = conversations.map { convs ->
        convs.flatMap { it.tags }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected tag filters
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters

    // Tag visibility state (persisted across navigation)
    private val _showTagFilter = MutableStateFlow(false)
    val showTagFilter: StateFlow<Boolean> = _showTagFilter

    init {
        viewModelScope.launch {
            chatHistoryRepository.initialize()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            chatHistoryRepository.initialize()
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatHistoryRepository.deleteConversation(id)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatHistoryRepository.updateConversationTitle(id, newTitle)
        }
    }

    // Tag filter toggle
    fun toggleTagFilter(tag: String) {
        _selectedTagFilters.value = if (tag in _selectedTagFilters.value) {
            _selectedTagFilters.value - tag
        } else {
            _selectedTagFilters.value + tag
        }
    }

    // Clear tag filters
    fun clearTagFilters() {
        _selectedTagFilters.value = emptySet()
    }

    // Toggle tag visibility
    fun toggleShowTagFilter() {
        _showTagFilter.value = !_showTagFilter.value
    }

    // Add tag to conversation
    fun addTagToConversation(conversationId: String, tag: String) {
        viewModelScope.launch {
            chatHistoryRepository.addTagToConversation(conversationId, tag.trim().lowercase())
        }
    }

    // Remove tag from conversation
    fun removeTagFromConversation(conversationId: String, tag: String) {
        viewModelScope.launch {
            chatHistoryRepository.removeTagFromConversation(conversationId, tag)
        }
    }
}
