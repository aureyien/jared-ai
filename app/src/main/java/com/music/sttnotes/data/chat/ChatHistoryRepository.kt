package com.music.sttnotes.data.chat

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
class ChatHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val historyFile: File
        get() = File(context.filesDir, "chat_history.json")

    private val mutex = Mutex()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations

    private val _allTags = MutableStateFlow<Set<String>>(emptySet())
    val allTags: StateFlow<Set<String>> = _allTags

    private val _archivedConversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val archivedConversations: StateFlow<List<ChatConversation>> = _archivedConversations

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (historyFile.exists()) {
                    val content = historyFile.readText()
                    val data = json.decodeFromString<ChatHistoryData>(content)
                    val allConversations = data.conversations.sortedByDescending { it.updatedAt }
                    _conversations.value = allConversations.filter { !it.isArchived }
                    _archivedConversations.value = allConversations.filter { it.isArchived }

                    // Migration: if allTags is empty but conversations have tags, populate from conversations
                    if (data.allTags.isEmpty() && data.conversations.any { it.tags.isNotEmpty() }) {
                        val migratedTags = data.conversations.flatMap { it.tags }.toSet()
                        _allTags.value = migratedTags
                        Log.d(TAG, "Migrated ${migratedTags.size} tags from conversations")
                        // Persist the migrated tags
                        persistConversations(data.conversations)
                    } else {
                        _allTags.value = data.allTags
                    }

                    Log.d(TAG, "Loaded ${_conversations.value.size} conversations, ${_archivedConversations.value.size} archived, ${_allTags.value.size} tags")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations", e)
                _conversations.value = emptyList()
                _archivedConversations.value = emptyList()
                _allTags.value = emptySet()
            }
        }
    }

    suspend fun createConversation(firstMessage: String? = null): ChatConversation = withContext(Dispatchers.IO) {
        mutex.withLock {
            val title = firstMessage?.take(50)?.trim() ?: "New conversation"
            val conversation = ChatConversation(title = title)
            val current = _conversations.value.toMutableList()
            current.add(0, conversation)
            _conversations.value = current
            persistConversations(current)
            Log.d(TAG, "Created conversation: ${conversation.id}")
            conversation
        }
    }

    suspend fun updateConversation(conversation: ChatConversation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = conversation.copy(updatedAt = System.currentTimeMillis())
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == conversation.id }

            if (index >= 0) {
                current[index] = updated
            } else {
                current.add(0, updated)
            }

            _conversations.value = current.sortedByDescending { it.updatedAt }
            persistConversations(current)
        }
    }

    suspend fun addMessage(conversationId: String, message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == conversationId }

            if (index >= 0) {
                val conv = current[index]
                val newMessages = conv.messages + message

                current[index] = conv.copy(
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )

                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
                Log.d(TAG, "Added message to conversation $conversationId")
            }
        }
    }

    suspend fun updateMessageSavedPath(conversationId: String, messageId: String, savedPath: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val convIndex = current.indexOfFirst { it.id == conversationId }

            if (convIndex >= 0) {
                val conv = current[convIndex]
                val updatedMessages = conv.messages.map { msg ->
                    if (msg.id == messageId) msg.copy(savedToFile = savedPath) else msg
                }
                current[convIndex] = conv.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
            }
        }
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Remove from both active and archived lists
            val currentActive = _conversations.value.filter { it.id != id }
            val currentArchived = _archivedConversations.value.filter { it.id != id }

            _conversations.value = currentActive
            _archivedConversations.value = currentArchived

            persistConversations(currentActive + currentArchived)
            Log.d(TAG, "Deleted conversation: $id")
        }
    }

    suspend fun updateConversationTitle(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }

            if (index >= 0) {
                current[index] = current[index].copy(
                    title = newTitle.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
            }
        }
    }

    suspend fun updateConversationTags(id: String, tags: List<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }

            if (index >= 0) {
                current[index] = current[index].copy(tags = tags)
                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
            }
        }
    }

    suspend fun addTagToConversation(id: String, tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }

            if (index >= 0) {
                val conv = current[index]
                if (tag !in conv.tags) {
                    current[index] = conv.copy(tags = conv.tags + tag)
                    _conversations.value = current.sortedByDescending { it.updatedAt }

                    // Add tag to allTags set to persist it even when removed from all conversations
                    _allTags.value = _allTags.value + tag

                    persistConversations(current)
                }
            }
        }
    }

    suspend fun removeTagFromConversation(id: String, tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }

            if (index >= 0) {
                val conv = current[index]
                current[index] = conv.copy(tags = conv.tags - tag)
                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
            }
        }
    }

    suspend fun deleteTag(tag: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Remove tag from all conversations
            val current = _conversations.value.map { conv ->
                if (tag in conv.tags) {
                    conv.copy(tags = conv.tags - tag)
                } else {
                    conv
                }
            }
            _conversations.value = current.sortedByDescending { it.updatedAt }

            // Remove from allTags set
            _allTags.value = _allTags.value - tag

            persistConversations(current)
        }
    }

    fun getConversation(id: String): ChatConversation? {
        return _conversations.value.find { it.id == id }
            ?: _archivedConversations.value.find { it.id == id }
    }

    suspend fun toggleConversationFavorite(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _conversations.value.toMutableList()
            val index = current.indexOfFirst { it.id == conversationId }
            if (index >= 0) {
                val updated = current[index].copy(isFavorite = !current[index].isFavorite)
                current[index] = updated
                _conversations.value = current.sortedByDescending { it.updatedAt }
                persistConversations(current)
                Log.d(TAG, "Toggled favorite for conversation $conversationId to ${updated.isFavorite}")
            }
        }
    }

    suspend fun archiveConversation(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val conversationToArchive = _conversations.value.find { it.id == conversationId }
            if (conversationToArchive == null) {
                Log.w(TAG, "Cannot archive conversation $conversationId - not found in active conversations")
                return@withLock
            }

            val archivedConversation = conversationToArchive.copy(
                isArchived = true,
                isFavorite = false,  // Automatically unfavorite when archiving
                updatedAt = System.currentTimeMillis()
            )

            // Remove from active conversations
            val currentActive = _conversations.value.filter { it.id != conversationId }
            // Add to archived conversations
            val currentArchived = _archivedConversations.value + archivedConversation

            _conversations.value = currentActive.sortedByDescending { it.updatedAt }
            _archivedConversations.value = currentArchived.sortedByDescending { it.updatedAt }

            persistConversations(currentActive + currentArchived)
            Log.d(TAG, "Archived conversation $conversationId (was favorite: ${conversationToArchive.isFavorite})")
        }
    }

    suspend fun unarchiveConversation(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val conversationToRestore = _archivedConversations.value.find { it.id == conversationId }
                ?: return@withLock

            val restoredConversation = conversationToRestore.copy(
                isArchived = false,
                updatedAt = System.currentTimeMillis()
            )

            // Add back to active conversations
            val currentActive = _conversations.value + restoredConversation
            // Remove from archived conversations
            val currentArchived = _archivedConversations.value.filter { it.id != conversationId }

            _conversations.value = currentActive.sortedByDescending { it.updatedAt }
            _archivedConversations.value = currentArchived

            persistConversations(currentActive + currentArchived)
            Log.d(TAG, "Unarchived conversation $conversationId")
        }
    }

    suspend fun permanentlyDeleteArchivedConversation(conversationId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentArchived = _archivedConversations.value.filter { it.id != conversationId }
            val currentActive = _conversations.value

            _archivedConversations.value = currentArchived

            persistConversations(currentActive + currentArchived)
            Log.d(TAG, "Permanently deleted archived conversation: $conversationId")
        }
    }

    fun getArchivedConversations(): List<ChatConversation> {
        return _archivedConversations.value
    }

    fun getFavoriteConversations(): List<ChatConversation> {
        return _conversations.value.filter { it.isFavorite }
    }

    private suspend fun persistConversations(conversations: List<ChatConversation>) {
        try {
            val data = ChatHistoryData(
                conversations = conversations,
                allTags = _allTags.value
            )
            historyFile.writeText(json.encodeToString(ChatHistoryData.serializer(), data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist conversations", e)
        }
    }

    companion object {
        private const val TAG = "ChatHistoryRepository"
    }
}
