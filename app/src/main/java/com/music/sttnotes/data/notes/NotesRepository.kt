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

                    // Migration: if allTags empty but notes have tags, extract them
                    if (data.allTags.isEmpty() && data.notes.any { it.tags.isNotEmpty() }) {
                        _allTags.value = data.notes.flatMap { it.tags }.toSet()
                        persistNotes(allNotes.filter { !it.isArchived })
                        Log.d(TAG, "Migrated ${_allTags.value.size} tags from notes")
                    } else {
                        _allTags.value = data.allTags
                    }

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

    suspend fun addTagToNote(noteId: String, tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _notes.value.toMutableList()
            val index = current.indexOfFirst { it.id == noteId }

            if (index >= 0) {
                val note = current[index]
                val updatedTags = (note.tags + tag).distinct()
                current[index] = note.copy(tags = updatedTags)
                _notes.value = current.sortedByDescending { it.updatedAt }
                _allTags.value = _allTags.value + tag
                persistNotes(current)
            }
        }
    }

    suspend fun removeTagFromNote(noteId: String, tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _notes.value.toMutableList()
            val index = current.indexOfFirst { it.id == noteId }

            if (index >= 0) {
                val note = current[index]
                current[index] = note.copy(tags = note.tags - tag)
                _notes.value = current.sortedByDescending { it.updatedAt }
                persistNotes(current)
            }
        }
    }

    suspend fun deleteTag(tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Remove from all notes
            val current = _notes.value.map { note ->
                if (tag in note.tags) note.copy(tags = note.tags - tag)
                else note
            }
            _notes.value = current.sortedByDescending { it.updatedAt }

            // Remove from allTags set
            _allTags.value = _allTags.value - tag

            persistNotes(current)
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
            val noteToArchive = _notes.value.find { it.id == noteId }
            if (noteToArchive == null) {
                // Note already archived or doesn't exist
                Log.w(TAG, "Cannot archive note $noteId - not found in active notes")
                return@withLock
            }
            val archivedNote = noteToArchive.copy(isArchived = true, updatedAt = System.currentTimeMillis())

            val currentNotes = _notes.value.filter { it.id != noteId }
            val currentArchived = _archivedNotes.value + archivedNote

            indexBuilder.removeNote(noteId)
            _notes.value = currentNotes
            _archivedNotes.value = currentArchived.sortedByDescending { it.updatedAt }
            updateAllTags(currentNotes)
            persistNotes(currentNotes + currentArchived)
            Log.d(TAG, "Archived note $noteId")
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

    suspend fun toggleNoteFavorite(noteId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentNotes = _notes.value.toMutableList()
            val index = currentNotes.indexOfFirst { it.id == noteId }
            if (index >= 0) {
                val updated = currentNotes[index].copy(isFavorite = !currentNotes[index].isFavorite)
                currentNotes[index] = updated
                _notes.value = currentNotes.sortedByDescending { it.updatedAt }
                persistNotes(currentNotes + _archivedNotes.value)
                Log.d(TAG, "Toggled favorite for note $noteId to ${updated.isFavorite}")
            }
        }
    }

    fun getFavoriteNotes(): List<Note> {
        return _notes.value.filter { it.isFavorite }
    }

    private fun rebuildIndex(notes: List<Note>) {
        notes.forEach { indexBuilder.indexNote(it) }
    }

    private fun updateAllTags(notes: List<Note>) {
        _allTags.value = notes.flatMap { it.tags }.toSet()
    }

    private suspend fun persistNotes(notes: List<Note>) {
        try {
            val data = NotesData(
                notes = notes,
                allTags = _allTags.value
            )
            notesFile.writeText(json.encodeToString(NotesData.serializer(), data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist notes", e)
        }
    }

    companion object {
        private const val TAG = "NotesRepository"
    }
}
