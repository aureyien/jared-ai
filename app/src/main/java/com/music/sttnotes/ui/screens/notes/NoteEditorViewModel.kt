package com.music.sttnotes.ui.screens.notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedrejeb.richeditor.model.RichTextState
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

    val richTextState = RichTextState().apply {
        config.listIndent = 16  // Reduce from default 38 for tighter list indentation
    }

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

    fun loadNote(noteId: String?) {
        if (noteId != null) {
            notesRepository.getNote(noteId)?.let { existingNote ->
                _note.value = existingNote
                richTextState.setMarkdown(existingNote.content)
                isNewNote = false
            }
        }
    }

    fun updateTitle(title: String) {
        _note.value = _note.value.copy(title = title)
    }

    fun updateContent(markdown: String) {
        _note.value = _note.value.copy(content = markdown)
    }

    fun togglePreviewMode() {
        if (!_isPreviewMode.value) {
            // Sync content before preview
            _note.value = _note.value.copy(content = richTextState.toMarkdown())
        }
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
                        // Insert transcribed text at current cursor position
                        val trimmedText = text.trim()
                        richTextState.addTextAfterSelection(trimmedText)
                        // Update note content
                        _note.value = _note.value.copy(content = richTextState.toMarkdown())
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
            val content = richTextState.toMarkdown()
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
            // Save first to persist any changes
            val content = richTextState.toMarkdown()
            val noteToArchive = if (content.isNotBlank() || !isNewNote) {
                val currentNote = _note.value.copy(content = content)
                notesRepository.saveNote(currentNote)
                currentNote
            } else {
                _note.value
            }
            // Then archive (wait for save to complete)
            notesRepository.archiveNote(noteToArchive.id)
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
