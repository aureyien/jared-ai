package com.music.sttnotes.data.notes

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",  // Markdown content
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false
)

@Serializable
data class NotesData(
    val notes: List<Note> = emptyList(),
    val allTags: Set<String> = emptySet(),
    val version: Int = 1
)
