package com.music.sttnotes.data.notes

import kotlinx.serialization.Serializable

@Serializable
data class SearchIndex(
    val wordToNoteIds: Map<String, Set<String>> = emptyMap(),
    val tagToNoteIds: Map<String, Set<String>> = emptyMap()
)

class SearchIndexBuilder {
    private val wordIndex = mutableMapOf<String, MutableSet<String>>()
    private val tagIndex = mutableMapOf<String, MutableSet<String>>()

    fun indexNote(note: Note) {
        // Index title and content words
        val words = tokenize(note.title) + tokenize(note.content)
        words.forEach { word ->
            wordIndex.getOrPut(word) { mutableSetOf() }.add(note.id)
        }
        // Index tags
        note.tags.forEach { tag ->
            tagIndex.getOrPut(tag.lowercase()) { mutableSetOf() }.add(note.id)
        }
    }

    fun removeNote(noteId: String) {
        wordIndex.values.forEach { it.remove(noteId) }
        tagIndex.values.forEach { it.remove(noteId) }
        // Clean up empty entries
        wordIndex.entries.removeAll { it.value.isEmpty() }
        tagIndex.entries.removeAll { it.value.isEmpty() }
    }

    fun build(): SearchIndex = SearchIndex(
        wordToNoteIds = wordIndex.mapValues { it.value.toSet() },
        tagToNoteIds = tagIndex.mapValues { it.value.toSet() }
    )

    fun search(query: String): Set<String> {
        val queryWords = tokenize(query)
        if (queryWords.isEmpty()) return emptySet()

        // Find notes containing ALL query words (AND search)
        val matchingSets = queryWords.map { word ->
            wordIndex.filterKeys { it.contains(word) }
                .values.flatten().toSet()
        }

        return if (matchingSets.isEmpty()) {
            emptySet()
        } else {
            matchingSets.reduce { acc, set -> acc.intersect(set) }
        }
    }

    fun searchByTag(tag: String): Set<String> {
        return tagIndex[tag.lowercase()] ?: emptySet()
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9àâäéèêëïîôùûüç\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }
    }
}
