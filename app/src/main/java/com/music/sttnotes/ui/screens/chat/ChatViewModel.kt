package com.music.sttnotes.ui.screens.chat

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.ChatMessage
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.LlmService
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.chat.ChatConversation
import com.music.sttnotes.data.chat.ChatHistoryRepository
import com.music.sttnotes.data.chat.ChatMessageEntity
import com.music.sttnotes.data.llm.LlmOutputRepository
import com.music.sttnotes.data.stt.AudioRecorder
import com.music.sttnotes.data.stt.SttManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.Immutable
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@Immutable
data class UiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCloud: Boolean = false,
    val processingTimeMs: Long? = null,
    val savedToFile: String? = null
)

sealed class ChatState {
    data object Idle : ChatState()
    data object Recording : ChatState()
    data object Transcribing : ChatState()
    data object SendingToLlm : ChatState()
    data class Error(val message: String) : ChatState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sttManager: SttManager,
    private val audioRecorder: AudioRecorder,
    private val llmService: LlmService,
    private val apiConfig: ApiConfig,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val llmOutputRepository: LlmOutputRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get conversationId from navigation - "new" means new conversation
    private val navConversationId: String? = savedStateHandle.get<String>("conversationId")
        ?.takeIf { it != "new" }

    private val _currentConversationId = MutableStateFlow<String?>(navConversationId)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<UiChatMessage>>(emptyList())
    val messages: StateFlow<List<UiChatMessage>> = _messages

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    // Always ready since SttManager handles provider selection
    private val _whisperReady = MutableStateFlow(true)
    val whisperReady: StateFlow<Boolean> = _whisperReady

    // Existing folders for save dialog
    private val _existingFolders = MutableStateFlow<List<String>>(emptyList())
    val existingFolders: StateFlow<List<String>> = _existingFolders

    // Conversation title
    private val _conversationTitle = MutableStateFlow("")
    val conversationTitle: StateFlow<String> = _conversationTitle

    // Current LLM provider (can be changed from UI)
    private val _currentLlmProvider = MutableStateFlow(LlmProvider.OPENAI)
    val currentLlmProvider: StateFlow<LlmProvider> = _currentLlmProvider

    // Available LLM providers (only those with API keys configured)
    private val _availableLlmProviders = MutableStateFlow<List<LlmProvider>>(emptyList())
    val availableLlmProviders: StateFlow<List<LlmProvider>> = _availableLlmProviders

    // Chat font size from settings
    private val _chatFontSize = MutableStateFlow(ApiConfig.DEFAULT_CHAT_FONT_SIZE)
    val chatFontSize: StateFlow<Float> = _chatFontSize

    // Ephemeral mode - conversation not saved to history
    private val _isEphemeral = MutableStateFlow(false)
    val isEphemeral: StateFlow<Boolean> = _isEphemeral

    fun toggleEphemeral() {
        _isEphemeral.value = !_isEphemeral.value
    }

    init {
        viewModelScope.launch {
            // Initialize repository
            chatHistoryRepository.initialize()

            // Load LLM provider from settings
            _currentLlmProvider.value = apiConfig.llmProvider.first()

            // Collect chat font size changes
            apiConfig.chatFontSize.collect { size ->
                _chatFontSize.value = size
            }
        }

        viewModelScope.launch {

            // Load available providers (with API keys configured)
            refreshAvailableProviders()

            // Load existing conversation if navigated with ID
            if (navConversationId != null) {
                loadConversation(navConversationId)
            }

            // Load existing folders
            refreshFolders()
        }
    }

    private suspend fun refreshAvailableProviders() {
        val available = mutableListOf<LlmProvider>()
        // Only GPT, Grok, and Claude for LLM analysis (Groq is STT only)
        if (!apiConfig.openaiApiKey.first().isNullOrBlank()) available.add(LlmProvider.OPENAI)
        if (!apiConfig.xaiApiKey.first().isNullOrBlank()) available.add(LlmProvider.XAI)
        if (!apiConfig.anthropicApiKey.first().isNullOrBlank()) available.add(LlmProvider.ANTHROPIC)
        _availableLlmProviders.value = available
    }

    fun setLlmProvider(provider: LlmProvider) {
        _currentLlmProvider.value = provider
        viewModelScope.launch {
            apiConfig.setLlmProvider(provider)
        }
    }

    private fun loadConversation(conversationId: String) {
        val conversation = chatHistoryRepository.getConversation(conversationId)
        if (conversation != null) {
            _currentConversationId.value = conversationId
            _conversationTitle.value = conversation.title
            _messages.value = conversation.messages.map { entity ->
                UiChatMessage(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isCloud = entity.isCloud,
                    processingTimeMs = entity.processingTimeMs,
                    savedToFile = entity.savedToFile
                )
            }
        }
    }

    fun refreshFolders() {
        _existingFolders.value = llmOutputRepository.listFolders()
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            // Ensure we have a conversation
            ensureConversation(text)

            // Add user message
            val userMessage = UiChatMessage(role = "user", content = text)
            addMessage(userMessage)
            persistMessage(userMessage)
            _inputText.value = ""

            // Send to LLM
            sendToLlm(text)
        }
    }

    private suspend fun ensureConversation(firstMessage: String) {
        // Skip creating conversation in ephemeral mode
        if (_isEphemeral.value) return
        if (_currentConversationId.value == null) {
            val conversation = chatHistoryRepository.createConversation(firstMessage)
            _currentConversationId.value = conversation.id
            _conversationTitle.value = conversation.title
        }
    }

    private suspend fun persistMessage(uiMessage: UiChatMessage) {
        // Skip persisting messages in ephemeral mode
        if (_isEphemeral.value) return
        _currentConversationId.value?.let { convId ->
            val entity = ChatMessageEntity(
                id = uiMessage.id,
                role = uiMessage.role,
                content = uiMessage.content,
                timestamp = uiMessage.timestamp,
                isCloud = uiMessage.isCloud,
                processingTimeMs = uiMessage.processingTimeMs,
                savedToFile = uiMessage.savedToFile
            )
            chatHistoryRepository.addMessage(convId, entity)
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            _chatState.value = ChatState.Recording
            audioRecorder.startRecording()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val audioData = audioRecorder.stopRecording()
            if (audioData != null && audioData.isNotEmpty()) {
                _chatState.value = ChatState.Transcribing

                // Yield to allow UI to update (important for e-ink displays)
                kotlinx.coroutines.yield()

                val startTime = System.currentTimeMillis()
                val isCloud = apiConfig.sttProvider.first() != SttProvider.LOCAL

                val result = sttManager.transcribe(audioData, "fr")
                val elapsed = System.currentTimeMillis() - startTime

                result.fold(
                    onSuccess = { transcription ->
                        if (transcription.isNotBlank()) {
                            // Ensure conversation exists
                            ensureConversation(transcription)

                            // Add user message with transcription + cloud indicator
                            val userMessage = UiChatMessage(
                                role = "user",
                                content = transcription,
                                isCloud = isCloud,
                                processingTimeMs = elapsed
                            )
                            addMessage(userMessage)
                            persistMessage(userMessage)

                            // Yield to allow UI to update with new message (important for e-ink)
                            kotlinx.coroutines.yield()

                            // Send to LLM
                            sendToLlm(transcription)
                        } else {
                            _chatState.value = ChatState.Error("Empty transcription")
                        }
                    },
                    onFailure = { error ->
                        _chatState.value = ChatState.Error(error.message ?: "Transcription error")
                    }
                )
            } else {
                _chatState.value = ChatState.Idle
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.stopRecording()
        _chatState.value = ChatState.Idle
    }

    private suspend fun sendToLlm(userMessage: String) {
        _chatState.value = ChatState.SendingToLlm

        // Yield to allow UI to update (important for e-ink displays)
        kotlinx.coroutines.yield()

        val llmProvider = _currentLlmProvider.value
        if (llmProvider == LlmProvider.NONE) {
            _chatState.value = ChatState.Error("LLM not configured. Go to Settings.")
            return
        }

        val apiKey = when (llmProvider) {
            LlmProvider.GROQ -> apiConfig.groqApiKey.first()
            LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()
            LlmProvider.XAI -> apiConfig.xaiApiKey.first()
            LlmProvider.ANTHROPIC -> apiConfig.anthropicApiKey.first()
            LlmProvider.NONE -> null
        }

        if (apiKey.isNullOrBlank()) {
            _chatState.value = ChatState.Error("API key missing. Configure it in Settings.")
            return
        }

        val systemPrompt = apiConfig.llmSystemPrompt.first()
        val startTime = System.currentTimeMillis()

        // Message ID for streaming - message will be created on first chunk
        val messageId = java.util.UUID.randomUUID().toString()
        var messageCreated = false

        // Build message history for conversation context/memory
        val history = _messages.value
            .filter { it.content.isNotBlank() }
            .map { ChatMessage(role = it.role, content = it.content) }

        Log.d(TAG, "Sending ${history.size} messages to LLM for context")

        try {
            val contentBuilder = StringBuilder()

            llmService.processWithLlmStreaming(
                messages = history,
                systemPrompt = systemPrompt,
                provider = llmProvider,
                apiKey = apiKey
            ).collect { chunk ->
                contentBuilder.append(chunk)

                // Create message on first chunk, then update
                if (!messageCreated) {
                    val streamingMessage = UiChatMessage(
                        id = messageId,
                        role = "assistant",
                        content = contentBuilder.toString(),
                        isCloud = true
                    )
                    addMessage(streamingMessage)
                    messageCreated = true
                } else {
                    updateMessageContent(messageId, contentBuilder.toString())
                }
            }

            // Final update with processing time
            val elapsed = System.currentTimeMillis() - startTime
            updateMessageWithTime(messageId, contentBuilder.toString(), elapsed)

            // Persist the final assistant message
            val finalMessage = _messages.value.find { it.id == messageId }
            if (finalMessage != null) {
                persistMessage(finalMessage)
            }

            _chatState.value = ChatState.Idle

        } catch (e: Exception) {
            // Remove the empty streaming message on error
            removeMessage(messageId)
            _chatState.value = ChatState.Error(e.message ?: "LLM error")
        }
    }

    private fun updateMessageContent(messageId: String, newContent: String) {
        Log.d(TAG, "Streaming chunk received: ${newContent.takeLast(20)}")
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(content = newContent) else msg
        }
    }

    private fun updateMessageWithTime(messageId: String, content: String, processingTimeMs: Long) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(content = content, processingTimeMs = processingTimeMs) else msg
        }
    }

    private fun removeMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }

    private fun addMessage(message: UiChatMessage) {
        _messages.value = _messages.value + message
    }

    /**
     * Save a specific assistant response to a markdown file
     */
    fun saveResponseToFile(messageId: String, filename: String, folder: String) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            if (message != null && message.role == "assistant") {
                // Find the preceding user message for context
                val messageIndex = _messages.value.indexOfFirst { it.id == messageId }
                val userMessage = if (messageIndex > 0) {
                    _messages.value.getOrNull(messageIndex - 1)?.content ?: ""
                } else ""

                val result = llmOutputRepository.saveLlmOutput(
                    folder = folder,
                    content = message.content,
                    rawTranscription = userMessage,
                    customFilename = filename
                )

                result.fold(
                    onSuccess = { file ->
                        // Update message to mark as saved
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == messageId) msg.copy(savedToFile = file.absolutePath) else msg
                        }
                        // Update in repository
                        _currentConversationId.value?.let { convId ->
                            chatHistoryRepository.updateMessageSavedPath(convId, messageId, file.absolutePath)
                        }
                        // Refresh folders list
                        refreshFolders()
                    },
                    onFailure = {
                        _chatState.value = ChatState.Error("Save error: ${it.message}")
                    }
                )
            }
        }
    }

    fun dismissError() {
        _chatState.value = ChatState.Idle
    }

    fun clearChat() {
        _messages.value = emptyList()
        _currentConversationId.value = null
        _conversationTitle.value = ""
    }

    fun restoreMessages(messages: List<UiChatMessage>) {
        _messages.value = messages
    }

    fun renameConversation(newTitle: String) {
        val convId = _currentConversationId.value ?: return
        viewModelScope.launch {
            chatHistoryRepository.updateConversationTitle(convId, newTitle)
            _conversationTitle.value = newTitle.trim()
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
