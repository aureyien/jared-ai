package com.music.sttnotes.data.chat

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data models for chat conversation persistence.
 */

@Serializable
data class ChatConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val messages: List<ChatMessageEntity> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get a preview of the last assistant message (for list display)
     */
    fun getLastResponsePreview(maxLength: Int = 60): String {
        val lastAssistant = messages.lastOrNull { it.role == "assistant" }
        return lastAssistant?.content?.take(maxLength)?.let {
            if (it.length >= maxLength) "$it..." else it
        } ?: ""
    }
}

@Serializable
data class ChatMessageEntity(
    val id: String = UUID.randomUUID().toString(),
    val role: String,  // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCloud: Boolean = false,
    val processingTimeMs: Long? = null,
    val savedToFile: String? = null  // Path if saved as .md
)

@Serializable
data class ChatHistoryData(
    val conversations: List<ChatConversation> = emptyList(),
    val allTags: Set<String> = emptySet(),
    val version: Int = 1
)
