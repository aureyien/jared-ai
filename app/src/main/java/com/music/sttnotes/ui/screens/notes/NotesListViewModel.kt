package com.music.sttnotes.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.notes.Note
import com.music.sttnotes.data.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    val allTags: StateFlow<Set<String>> = notesRepository.allTags

    val notes: StateFlow<List<Note>> = notesRepository.notes

    val archivedNotes: StateFlow<List<Note>> = notesRepository.archivedNotes

    val filteredNotes: StateFlow<List<Note>> = combine(
        notesRepository.notes,
        _searchQuery,
        _selectedTag
    ) { notes, query, tag ->
        when {
            tag != null -> notesRepository.searchByTag(tag)
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
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
        _searchQuery.value = ""
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
        }
    }
}
