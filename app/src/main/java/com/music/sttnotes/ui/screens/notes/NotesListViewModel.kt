package com.music.sttnotes.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.LlmService
import com.music.sttnotes.data.llm.LlmOutputRepository
import com.music.sttnotes.data.notes.Note
import com.music.sttnotes.data.notes.NotesRepository
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val llmService: LlmService,
    private val apiConfig: ApiConfig,
    private val llmOutputRepository: LlmOutputRepository,
    private val sttPreferences: SttPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    // Selected tag filters (multi-select)
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters

    // Tag visibility state
    private val _showTagFilter = MutableStateFlow(false)
    val showTagFilter: StateFlow<Boolean> = _showTagFilter

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    val allTags: StateFlow<Set<String>> = notesRepository.allTags

    val notes: StateFlow<List<Note>> = notesRepository.notes

    val archivedNotes: StateFlow<List<Note>> = notesRepository.archivedNotes

    val filteredNotes: StateFlow<List<Note>> = combine(
        notesRepository.notes,
        _searchQuery,
        _selectedTagFilters
    ) { notes, query, tagFilters ->
        when {
            tagFilters.isNotEmpty() -> notes.filter { note -> note.tags.any { it in tagFilters } }
            query.isNotBlank() -> notesRepository.search(query)
            else -> notes
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            notesRepository.initialize()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _selectedTag.value = null
        _selectedTagFilters.value = emptySet()
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
        _searchQuery.value = ""
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

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
        }
    }

    fun archiveNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.archiveNote(noteId)
        }
    }

    fun unarchiveNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.unarchiveNote(noteId)
        }
    }

    fun deleteAllArchived() {
        viewModelScope.launch {
            notesRepository.deleteAllArchived()
        }
    }

    fun addTagToNote(noteId: String, tag: String) {
        viewModelScope.launch {
            notesRepository.addTagToNote(noteId, tag.trim().lowercase().take(20))
        }
    }

    fun removeTagFromNote(noteId: String, tag: String) {
        viewModelScope.launch {
            notesRepository.removeTagFromNote(noteId, tag)
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            notesRepository.deleteTag(tag)
            // Clear filter if the deleted tag was selected
            if (_selectedTag.value == tag) {
                _selectedTag.value = null
            }
            // Remove from filter if it was selected
            if (tag in _selectedTagFilters.value) {
                _selectedTagFilters.value = _selectedTagFilters.value - tag
            }
        }
    }

    fun toggleNoteFavorite(noteId: String) {
        viewModelScope.launch {
            notesRepository.toggleNoteFavorite(noteId)
        }
    }

    // Summary generation state
    private val _summaryInProgress = MutableStateFlow<String?>(null)
    val summaryInProgress: StateFlow<String?> = _summaryInProgress

    private val _generatedSummary = MutableStateFlow<Pair<String, String>?>(null)
    val generatedSummary: StateFlow<Pair<String, String>?> = _generatedSummary

    fun generateSummary(noteId: String) {
        viewModelScope.launch {
            val note = notes.value.find { it.id == noteId } ?: return@launch

            if (note.content.isBlank()) return@launch

            _summaryInProgress.value = noteId

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
                SttLanguage.FRENCH -> """Tu es un assistant de résumé. Crée un résumé complet de la note suivante en utilisant le formatage markdown.

Structure ton résumé comme suit :
## Vue d'ensemble
Un bref aperçu en 1-2 phrases du sujet de la note.

## Points clés
- Points principaux couvrant les sujets principaux
- Inclure les détails importants, décisions ou conclusions
- Capturer les actions à entreprendre ou les prochaines étapes mentionnées

## Détails
Développer les aspects les plus importants de la note avec le contexte pertinent.

Utilise un formatage markdown approprié (titres, puces, gras pour l'emphase). Sois complet mais concis. Concentre-toi sur l'extraction d'informations exploitables et d'informations clés."""

                SttLanguage.ENGLISH -> """You are a summarization assistant. Create a comprehensive summary of the following note using markdown formatting.

Structure your summary as follows:
## Overview
A brief 1-2 sentence overview of the note's topic.

## Key Points
- Bullet points covering the main topics
- Include important details, decisions, or conclusions
- Capture any action items or next steps mentioned

## Details
Expand on the most important aspects of the note with relevant context.

Use proper markdown formatting (headers, bullet points, bold for emphasis). Be thorough but concise. Focus on extracting actionable insights and key information."""
            }

            llmService.processWithLlm(
                text = note.content,
                systemPrompt = systemPrompt,
                provider = provider,
                apiKey = apiKey
            ).fold(
                onSuccess = { summary ->
                    _generatedSummary.value = noteId to summary
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

    fun saveSummaryToKb(noteId: String, summary: String, folderName: String, filename: String) {
        viewModelScope.launch {
            val note = notes.value.find { it.id == noteId }
            val content = buildString {
                appendLine("# ${note?.title ?: "Note Summary"}")
                appendLine()
                appendLine(summary)
                appendLine()
                appendLine("---")
                appendLine("*Generated from note*")
            }
            llmOutputRepository.writeFile(folderName, filename, content)
        }
    }
}
