package com.music.sttnotes.data.notes

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val notesFile: File
        get() = File(context.filesDir, "notes.json")

    private val mutex = Mutex()
    private val indexBuilder = SearchIndexBuilder()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    val archivedNotes: StateFlow<List<Note>> = _archivedNotes

    private val _allTags = MutableStateFlow<Set<String>>(emptySet())
    val allTags: StateFlow<Set<String>> = _allTags

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (notesFile.exists()) {
                    val content = notesFile.readText()
                    val data = json.decodeFromString<NotesData>(content)
                    val allNotes = data.notes.sortedByDescending { it.updatedAt }
                    _notes.value = allNotes.filter { !it.isArchived }
                    _archivedNotes.value = allNotes.filter { it.isArchived }
                    rebuildIndex(allNotes.filter { !it.isArchived })
                    updateAllTags(allNotes.filter { !it.isArchived })
                    Log.d(TAG, "Loaded ${_notes.value.size} notes, ${_archivedNotes.value.size} archived")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notes", e)
                _notes.value = emptyList()
                _archivedNotes.value = emptyList()
            }
        }
    }

    suspend fun saveNote(note: Note): Note = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
            val currentNotes = _notes.value.toMutableList()
            val existingIndex = currentNotes.indexOfFirst { it.id == note.id }

            if (existingIndex >= 0) {
                indexBuilder.removeNote(note.id)
                currentNotes[existingIndex] = updatedNote
            } else {
                currentNotes.add(0, updatedNote)
            }

            indexBuilder.indexNote(updatedNote)
            _notes.value = currentNotes.sortedByDescending { it.updatedAt }
            updateAllTags(currentNotes)
            persistNotes(currentNotes)
            updatedNote
        }
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentNotes = _notes.value.filter { it.id != noteId }
            val currentArchived = _archivedNotes.value.filter { it.id != noteId }
            indexBuilder.removeNote(noteId)
            _notes.value = currentNotes
            _archivedNotes.value = currentArchived
            updateAllTags(currentNotes)
            persistNotes(currentNotes + currentArchived)
        }
    }

    suspend fun archiveNote(noteId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val noteToArchive = _notes.value.find { it.id == noteId } ?: return@withLock
            val archivedNote = noteToArchive.copy(isArchived = true, updatedAt = System.currentTimeMillis())

            val currentNotes = _notes.value.filter { it.id != noteId }
            val currentArchived = _archivedNotes.value + archivedNote

            indexBuilder.removeNote(noteId)
            _notes.value = currentNotes
            _archivedNotes.value = currentArchived.sortedByDescending { it.updatedAt }
            updateAllTags(currentNotes)
            persistNotes(currentNotes + currentArchived)
        }
    }

    suspend fun unarchiveNote(noteId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val noteToRestore = _archivedNotes.value.find { it.id == noteId } ?: return@withLock
            val restoredNote = noteToRestore.copy(isArchived = false, updatedAt = System.currentTimeMillis())

            val currentNotes = (_notes.value + restoredNote).sortedByDescending { it.updatedAt }
            val currentArchived = _archivedNotes.value.filter { it.id != noteId }

            indexBuilder.indexNote(restoredNote)
            _notes.value = currentNotes
            _archivedNotes.value = currentArchived
            updateAllTags(currentNotes)
            persistNotes(currentNotes + currentArchived)
        }
    }

    suspend fun deleteAllArchived() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentNotes = _notes.value
            _archivedNotes.value = emptyList()
            persistNotes(currentNotes)
        }
    }

    fun getNote(noteId: String): Note? {
        return _notes.value.find { it.id == noteId }
            ?: _archivedNotes.value.find { it.id == noteId }
    }

    fun search(query: String): List<Note> {
        if (query.isBlank()) return _notes.value
        val matchingIds = indexBuilder.search(query)
        return _notes.value.filter { it.id in matchingIds }
    }

    fun searchByTag(tag: String): List<Note> {
        val matchingIds = indexBuilder.searchByTag(tag)
        return _notes.value.filter { it.id in matchingIds }
    }

    private fun rebuildIndex(notes: List<Note>) {
        notes.forEach { indexBuilder.indexNote(it) }
    }

    private fun updateAllTags(notes: List<Note>) {
        _allTags.value = notes.flatMap { it.tags }.toSet()
    }

    private suspend fun persistNotes(notes: List<Note>) {
        try {
            val data = NotesData(notes = notes)
            notesFile.writeText(json.encodeToString(NotesData.serializer(), data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist notes", e)
        }
    }

    companion object {
        private const val TAG = "NotesRepository"
    }
}
