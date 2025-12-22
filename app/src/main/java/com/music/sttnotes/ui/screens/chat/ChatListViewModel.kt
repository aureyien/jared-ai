package com.music.sttnotes.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.LlmService
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.data.chat.ChatHistoryRepository
import com.music.sttnotes.data.llm.LlmOutputRepository
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
    private val llmService: LlmService,
    private val apiConfig: ApiConfig,
    private val llmOutputRepository: LlmOutputRepository,
    private val sttPreferences: SttPreferences
) : ViewModel() {

    val conversations: StateFlow<List<ChatConversation>> = chatHistoryRepository.conversations
    val archivedConversations: StateFlow<List<ChatConversation>> = chatHistoryRepository.archivedConversations

    // Existing KB folders
    private val _existingFolders = MutableStateFlow<List<String>>(emptyList())
    val existingFolders: StateFlow<List<String>> = _existingFolders

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
            _existingFolders.value = llmOutputRepository.listFolders()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            chatHistoryRepository.initialize()
            _existingFolders.value = llmOutputRepository.listFolders()
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
            chatHistoryRepository.addTagToConversation(conversationId, tag.trim().lowercase().take(20))
        }
    }

    // Remove tag from conversation
    fun removeTagFromConversation(conversationId: String, tag: String) {
        viewModelScope.launch {
            chatHistoryRepository.removeTagFromConversation(conversationId, tag)
        }
    }

    // Delete tag from all conversations
    fun deleteTag(tag: String) {
        viewModelScope.launch {
            chatHistoryRepository.deleteTag(tag)
            // Remove from filter if it was selected
            if (tag in _selectedTagFilters.value) {
                _selectedTagFilters.value = _selectedTagFilters.value - tag
            }
        }
    }

    fun toggleConversationFavorite(conversationId: String) {
        viewModelScope.launch {
            chatHistoryRepository.toggleConversationFavorite(conversationId)
        }
    }

    fun archiveConversation(id: String) {
        viewModelScope.launch {
            chatHistoryRepository.archiveConversation(id)
        }
    }

    fun unarchiveConversation(id: String) {
        viewModelScope.launch {
            chatHistoryRepository.unarchiveConversation(id)
        }
    }

    fun permanentlyDeleteArchivedConversation(id: String) {
        viewModelScope.launch {
            chatHistoryRepository.permanentlyDeleteArchivedConversation(id)
        }
    }

    // Summary generation state
    private val _summaryInProgress = MutableStateFlow<String?>(null)
    val summaryInProgress: StateFlow<String?> = _summaryInProgress

    private val _generatedSummary = MutableStateFlow<Pair<String, String>?>(null)
    val generatedSummary: StateFlow<Pair<String, String>?> = _generatedSummary

    fun generateSummary(conversationId: String) {
        viewModelScope.launch {
            val conversation = conversations.value.find { it.id == conversationId } ?: return@launch

            val conversationText = conversation.messages.joinToString("\n") { msg ->
                "${if (msg.role == "user") "User" else "Assistant"}: ${msg.content}"
            }

            if (conversationText.isBlank()) return@launch

            _summaryInProgress.value = conversationId

            val provider = apiConfig.llmProvider.first()
            val apiKey = when (provider) {
                LlmProvider.GROQ -> apiConfig.groqApiKey.first()
                LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()
                LlmProvider.XAI -> apiConfig.xaiApiKey.first()
                LlmProvider.ANTHROPIC -> apiConfig.anthropicApiKey.first()
                LlmProvider.NONE -> null
            }

            if (apiKey.isNullOrEmpty()) {
                _summaryInProgress.value = null
                return@launch
            }

            val sttLanguage = sttPreferences.selectedLanguage.first()

            val systemPrompt = when (sttLanguage) {
                SttLanguage.FRENCH -> """Tu es un assistant de résumé. Crée un résumé complet de la conversation suivante en utilisant le formatage markdown.

Structure ton résumé comme suit :
## Vue d'ensemble
Un bref aperçu en 1-2 phrases du sujet de la conversation.

## Points clés
- Points principaux couvrant les sujets principaux discutés
- Inclure les détails importants, décisions ou conclusions
- Capturer les actions à entreprendre ou les prochaines étapes mentionnées

## Détails
Développer les aspects les plus importants de la conversation avec le contexte pertinent.

Utilise un formatage markdown approprié (titres, puces, gras pour l'emphase). Sois complet mais concis. Concentre-toi sur l'extraction d'informations exploitables et d'informations clés."""

                SttLanguage.ENGLISH -> """You are a summarization assistant. Create a comprehensive summary of the following conversation using markdown formatting.

Structure your summary as follows:
## Overview
A brief 1-2 sentence overview of the conversation topic.

## Key Points
- Bullet points covering the main topics discussed
- Include important details, decisions, or conclusions
- Capture any action items or next steps mentioned

## Details
Expand on the most important aspects of the conversation with relevant context.

Use proper markdown formatting (headers, bullet points, bold for emphasis). Be thorough but concise. Focus on extracting actionable insights and key information."""
            }

            llmService.processWithLlm(
                text = conversationText,
                systemPrompt = systemPrompt,
                provider = provider,
                apiKey = apiKey
            ).fold(
                onSuccess = { summary ->
                    _generatedSummary.value = conversationId to summary
                },
                onFailure = {
                    // Silent failure
                }
            )
            _summaryInProgress.value = null
        }
    }

    fun clearSummary() {
        _generatedSummary.value = null
    }

    fun clearSummaryProgress() {
        _summaryInProgress.value = null
    }

    fun saveSummaryToKb(conversationId: String, summary: String, folderName: String, filename: String) {
        viewModelScope.launch {
            val conversation = conversations.value.find { it.id == conversationId }
            val content = buildString {
                appendLine("# ${conversation?.title ?: "Conversation Summary"}")
                appendLine()
                appendLine(summary)
                appendLine()
                appendLine("---")
                appendLine("*Generated from chat conversation*")
            }
            llmOutputRepository.writeFile(folderName, filename, content)
        }
    }
}
