package com.music.sttnotes.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.data.chat.ChatHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    val conversations: StateFlow<List<ChatConversation>> = chatHistoryRepository.conversations

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
}
