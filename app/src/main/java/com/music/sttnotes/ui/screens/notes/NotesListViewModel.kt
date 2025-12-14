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

    val allTags: StateFlow<Set<String>> = notesRepository.allTags

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

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
        }
    }
}
