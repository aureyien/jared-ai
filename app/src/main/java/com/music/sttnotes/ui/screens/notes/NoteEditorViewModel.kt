package com.music.sttnotes.ui.screens.notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.notes.Note
import com.music.sttnotes.data.notes.NotesRepository
import com.music.sttnotes.data.stt.AudioRecorder
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttManager
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Initializing : RecordingState()
    data class Recording(val duration: Int) : RecordingState()
    data object Processing : RecordingState()
    data class Error(val message: String) : RecordingState()
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val sttManager: SttManager,
    private val audioRecorder: AudioRecorder,
    private val sttPreferences: SttPreferences
) : ViewModel() {

    private val _note = MutableStateFlow(Note())
    val note: StateFlow<Note> = _note

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _selectedLanguage = MutableStateFlow(SttLanguage.FRENCH)
    val selectedLanguage: StateFlow<SttLanguage> = _selectedLanguage

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode

    private val _tagInput = MutableStateFlow("")
    val tagInput: StateFlow<String> = _tagInput

    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived

    // Expose all available tags from repository
    val allTags: StateFlow<Set<String>> = notesRepository.allTags

    // Plain markdown content state
    private val _markdownContent = MutableStateFlow("")
    val markdownContent: StateFlow<String> = _markdownContent

    // Store cursor position for text insertion
    private var cursorPosition: Int = 0

    private var recordingJob: Job? = null
    private var durationJob: Job? = null
    private var recordingDuration = 0

    init {
        viewModelScope.launch {
            sttPreferences.selectedLanguage.collect {
                _selectedLanguage.value = it
            }
        }
    }

    // Track if this is a new note (for empty note check on save)
    private var isNewNote = true

    // Track the currently loaded note ID to avoid reloading the same note
    private var loadedNoteId: String? = null

    fun loadNote(noteId: String?) {
        // Only reload if the noteId has changed
        if (loadedNoteId == noteId) {
            return
        }

        loadedNoteId = noteId

        // Reset state when loading a note (fixes issue when navigating back and creating new note)
        if (noteId == null) {
            // New note - reset to defaults
            _note.value = Note()
            _markdownContent.value = ""
            isNewNote = true
            _isPreviewMode.value = false  // Edit mode for new notes
            _tagInput.value = ""
            _isArchived.value = false
        } else {
            // Existing note - load from repository
            notesRepository.getNote(noteId)?.let { existingNote ->
                _note.value = existingNote
                _markdownContent.value = existingNote.content
                isNewNote = false
                _isPreviewMode.value = true  // Preview mode for existing notes
                _tagInput.value = ""
                _isArchived.value = false
            }
        }
    }

    fun updateTitle(title: String) {
        _note.value = _note.value.copy(title = title)
    }

    fun updateContent(markdown: String) {
        _markdownContent.value = markdown
    }

    fun updateCursorPosition(position: Int) {
        cursorPosition = position
    }

    fun togglePreviewMode() {
        // Always sync content from markdown state
        _note.value = _note.value.copy(content = _markdownContent.value)
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun updateTagInput(input: String) {
        _tagInput.value = input
    }

    fun addTag() {
        val tag = _tagInput.value.trim()
        if (tag.isNotEmpty() && tag !in _note.value.tags) {
            _note.value = _note.value.copy(tags = _note.value.tags + tag)
            _tagInput.value = ""
        }
    }

    fun removeTag(tag: String) {
        _note.value = _note.value.copy(tags = _note.value.tags - tag)
    }

    fun setLanguage(language: SttLanguage) {
        viewModelScope.launch {
            sttPreferences.setLanguage(language)
        }
    }

    fun hasAudioPermission(): Boolean = audioRecorder.hasPermission()

    fun startRecording() {
        if (_recordingState.value !is RecordingState.Idle) return
        // SttManager handles provider selection (Groq cloud or local whisper)
        doStartRecording()
    }

    private fun doStartRecording() {
        recordingDuration = 0
        _recordingState.value = RecordingState.Recording(0)

        recordingJob = viewModelScope.launch {
            audioRecorder.startRecording()
        }

        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                recordingDuration++
                if (_recordingState.value is RecordingState.Recording) {
                    _recordingState.value = RecordingState.Recording(recordingDuration)
                }
                if (recordingDuration >= 30) {
                    stopRecording()
                    break
                }
            }
        }
    }

    fun stopRecording() {
        durationJob?.cancel()
        recordingJob?.cancel()

        val audioData = audioRecorder.stopRecording()
        if (audioData.isEmpty()) {
            _recordingState.value = RecordingState.Idle
            return
        }

        _recordingState.value = RecordingState.Processing

        viewModelScope.launch {
            val language = sttPreferences.selectedLanguage.first()
            sttManager.transcribe(audioData, language.code).fold(
                onSuccess = { text ->
                    if (text.isNotBlank()) {
                        // Insert transcribed text at cursor position
                        val trimmedText = text.trim()
                        val currentContent = _markdownContent.value
                        val insertPosition = cursorPosition.coerceIn(0, currentContent.length)

                        // Insert text at cursor position with space/newline before if needed
                        val before = currentContent.substring(0, insertPosition)
                        val after = currentContent.substring(insertPosition)
                        val separator = when {
                            before.isEmpty() -> "" // Start of document
                            before.endsWith("\n") -> "" // Already at line start
                            else -> " " // Add space before
                        }

                        val newContent = before + separator + trimmedText + after
                        Log.d(TAG, "Transcription success: '$trimmedText', inserting at position $insertPosition (${currentContent.length} -> ${newContent.length} chars)")
                        _markdownContent.value = newContent
                    } else {
                        Log.d(TAG, "Transcription returned blank text")
                    }
                    _recordingState.value = RecordingState.Idle
                },
                onFailure = { error ->
                    Log.e(TAG, "Transcription failed", error)
                    _recordingState.value = RecordingState.Error(error.message ?: "Transcription failed")
                }
            )
        }
    }

    fun cancelRecording() {
        durationJob?.cancel()
        recordingJob?.cancel()
        audioRecorder.cancelRecording()
        _recordingState.value = RecordingState.Idle
    }

    fun dismissError() {
        _recordingState.value = RecordingState.Idle
    }

    fun saveNote() {
        viewModelScope.launch {
            // Always use markdown content state
            val content = _markdownContent.value
            // Don't save empty notes (new notes with no content)
            if (content.isBlank() && isNewNote) {
                return@launch
            }
            val currentNote = _note.value.copy(content = content)
            notesRepository.saveNote(currentNote)
        }
    }

    fun archiveNote() {
        viewModelScope.launch {
            Log.d(TAG, "archiveNote() called for note id: ${_note.value.id}")
            // Save first to persist any changes
            // Always use markdown content state
            val content = _markdownContent.value
            val noteToArchive = if (content.isNotBlank() || !isNewNote) {
                val currentNote = _note.value.copy(content = content)
                notesRepository.saveNote(currentNote)
                Log.d(TAG, "Note saved before archiving")
                currentNote
            } else {
                _note.value
            }
            // Then archive (wait for save to complete)
            notesRepository.archiveNote(noteToArchive.id)
            Log.d(TAG, "Note archived, setting _isArchived to true")
            // Signal that archiving is complete
            _isArchived.value = true
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            notesRepository.toggleNoteFavorite(_note.value.id)
            // Update local state
            _note.value = _note.value.copy(isFavorite = !_note.value.isFavorite)
        }
    }

    companion object {
        private const val TAG = "NoteEditorViewModel"
    }
}
