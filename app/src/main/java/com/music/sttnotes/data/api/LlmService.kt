package com.music.sttnotes.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatChoice(
    val message: ChatMessage? = null,
    val delta: ChatDelta? = null
)

@Serializable
data class ChatDelta(
    val content: String? = null
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>
)

// Anthropic-specific data classes
@Serializable
data class AnthropicChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int,
    val stream: Boolean = false,
    val system: String? = null  // System prompt is separate field
)

// Anthropic prompt caching data classes
@Serializable
data class CacheControl(
    val type: String = "ephemeral"
)

@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String,
    val cache_control: CacheControl? = null
)

@Serializable
data class AnthropicMessageWithCache(
    val role: String,
    val content: List<AnthropicContentBlock>
)

@Serializable
data class AnthropicChatRequestWithCache(
    val model: String,
    val messages: List<AnthropicMessageWithCache>,
    val max_tokens: Int,
    val stream: Boolean = false,
    val system: List<AnthropicContentBlock>? = null  // System as array for cache_control
)

@Serializable
data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null
)

@Serializable
data class AnthropicContentBlockDelta(
    val type: String,
    val index: Int? = null,
    val delta: AnthropicDelta? = null
)

@Singleton
class LlmService @Inject constructor() {
    companion object {
        private const val TAG = "LlmService"

        // Model recommendations (Dec 2025)
        const val GROQ_MODEL = "llama-3.3-70b-versatile"  // Fast, free tier
        const val OPENAI_MODEL = "gpt-5-mini"             // $0.25/1M input, $2/1M output
        const val XAI_MODEL = "grok-4-1-fast-reasoning"   // $0.20/1M input, $0.50/1M output
        const val ANTHROPIC_MODEL = "claude-haiku-4-5"    // $1/1M input, $5/1M output
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Process text with LLM (non-streaming, for backward compatibility)
     */
    suspend fun processWithLlm(
        text: String,
        systemPrompt: String,
        provider: LlmProvider,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        when (provider) {
            LlmProvider.GROQ -> callLlmApi(
                baseUrl = ApiConfig.GROQ_BASE_URL,
                model = GROQ_MODEL,
                text = text,
                systemPrompt = systemPrompt,
                apiKey = apiKey
            )
            LlmProvider.OPENAI -> callLlmApi(
                baseUrl = ApiConfig.OPENAI_BASE_URL,
                model = OPENAI_MODEL,
                text = text,
                systemPrompt = systemPrompt,
                apiKey = apiKey
            )
            LlmProvider.XAI -> callLlmApi(
                baseUrl = ApiConfig.XAI_BASE_URL,
                model = XAI_MODEL,
                text = text,
                systemPrompt = systemPrompt,
                apiKey = apiKey
            )
            LlmProvider.ANTHROPIC -> callAnthropicApi(
                text = text,
                systemPrompt = systemPrompt,
                apiKey = apiKey
            )
            LlmProvider.NONE -> Result.success(text)
        }
    }

    /**
     * Process text with LLM using streaming (SSE)
     * Emits text chunks as they arrive
     * Now accepts full message history for conversation context/memory
     */
    fun processWithLlmStreaming(
        messages: List<ChatMessage>,
        systemPrompt: String,
        provider: LlmProvider,
        apiKey: String
    ): Flow<String> = flow {
        val (baseUrl, model) = when (provider) {
            LlmProvider.GROQ -> ApiConfig.GROQ_BASE_URL to GROQ_MODEL
            LlmProvider.OPENAI -> ApiConfig.OPENAI_BASE_URL to OPENAI_MODEL
            LlmProvider.XAI -> ApiConfig.XAI_BASE_URL to XAI_MODEL
            LlmProvider.ANTHROPIC -> ApiConfig.ANTHROPIC_BASE_URL to ANTHROPIC_MODEL
            LlmProvider.NONE -> {
                // Return last user message content if available
                messages.lastOrNull { it.role == "user" }?.content?.let { emit(it) }
                return@flow
            }
        }

        // Build request based on provider (Anthropic has different format)
        val (requestBody, endpoint, headers) = if (provider == LlmProvider.ANTHROPIC) {
            // Anthropic format: system prompt separate, different headers
            // Use prompt caching for long system prompts (> 4000 chars)
            val usePromptCaching = systemPrompt.length > 4000

            val anthropicRequestBody = if (usePromptCaching) {
                // Convert messages to cached format
                val cachedMessages = messages.takeLast(50).map { msg ->
                    AnthropicMessageWithCache(
                        role = msg.role,
                        content = listOf(AnthropicContentBlock(type = "text", text = msg.content))
                    )
                }

                val cachedRequest = AnthropicChatRequestWithCache(
                    model = model,
                    messages = cachedMessages,
                    max_tokens = 4096,
                    stream = true,
                    system = listOf(
                        AnthropicContentBlock(
                            type = "text",
                            text = systemPrompt,
                            cache_control = CacheControl(type = "ephemeral")
                        )
                    )
                )
                Log.d(TAG, "Using Anthropic prompt caching for system prompt (${systemPrompt.length} chars)")
                json.encodeToString(cachedRequest)
            } else {
                // Standard request without caching
                val anthropicRequest = AnthropicChatRequest(
                    model = model,
                    messages = messages.takeLast(50),
                    max_tokens = 4096,
                    stream = true,
                    system = systemPrompt
                )
                json.encodeToString(anthropicRequest)
            }

            Triple(
                anthropicRequestBody,
                "${baseUrl}messages",
                mapOf(
                    "x-api-key" to apiKey,
                    "anthropic-version" to "2023-06-01",
                    "content-type" to "application/json"
                )
            )
        } else {
            // OpenAI-compatible format
            val allMessages = listOf(ChatMessage(role = "system", content = systemPrompt)) +
                messages.takeLast(50)
            val standardRequest = ChatRequest(
                model = model,
                messages = allMessages,
                stream = true
            )
            Triple(
                json.encodeToString(standardRequest),
                "${baseUrl}chat/completions",
                mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json",
                    "Accept" to "text/event-stream"
                )
            )
        }

        Log.d(TAG, "Streaming request: $requestBody")

        val requestBuilder = Request.Builder().url(endpoint)
        headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        requestBuilder.post(requestBody.toRequestBody("application/json".toMediaType()))
        val request = requestBuilder.build()

        Log.d(TAG, "Starting streaming LLM call ($model)...")

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            Log.e(TAG, "LLM API error ${response.code}: $errorBody")
            throw IOException("API error ${response.code}: $errorBody")
        }

        val reader = response.body?.source()?.inputStream()?.bufferedReader()
            ?: throw IOException("Empty response body")

        try {
            // Read line by line WITHOUT buffering all lines first
            // This is critical for real-time streaming
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue

                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()

                    // End of stream
                    if (data == "[DONE]") {
                        Log.d(TAG, "Stream completed")
                        break
                    }

                    // Skip empty data
                    if (data.isEmpty()) continue

                    // Parse the JSON chunk based on provider
                    try {
                        if (provider == LlmProvider.ANTHROPIC) {
                            // Parse Anthropic's event-based format
                            val event = json.decodeFromString<AnthropicContentBlockDelta>(data)
                            if (event.type == "content_block_delta") {
                                val content = event.delta?.text
                                if (!content.isNullOrEmpty()) {
                                    emit(content)
                                }
                            }
                        } else {
                            // Parse OpenAI-compatible format
                            val chunk = json.decodeFromString<ChatResponse>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse chunk: $data", e)
                    }
                }
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun callAnthropicApi(
        text: String,
        systemPrompt: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = AnthropicChatRequest(
                model = ANTHROPIC_MODEL,
                messages = listOf(ChatMessage(role = "user", content = text)),
                max_tokens = 4096,
                stream = false,
                system = systemPrompt
            )

            val jsonBody = json.encodeToString(requestBody)

            val request = Request.Builder()
                .url("${ApiConfig.ANTHROPIC_BASE_URL}messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(IOException("API error ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))

                // Parse Anthropic response format
                val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
                val content = jsonResponse["content"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: return@withContext Result.failure(IOException("No response content"))

                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Anthropic API call failed", e)
            Result.failure(e)
        }
    }

    private suspend fun callLlmApi(
        baseUrl: String,
        model: String,
        text: String,
        systemPrompt: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = text)
                ),
                stream = false
            )

            val jsonBody = json.encodeToString(requestBody)
            Log.d(TAG, "JSON body: $jsonBody")

            val request = Request.Builder()
                .url("${baseUrl}chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "Calling LLM API ($model) with ${text.length} chars...")
            val startTime = System.currentTimeMillis()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "LLM API error ${response.code}: $errorBody")
                    return@withContext Result.failure(IOException("API error ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))

                val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
                val result = chatResponse.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(IOException("No response content"))

                Log.d(TAG, "LLM response in ${elapsed}ms: \"${result.take(50)}${if (result.length > 50) "..." else ""}\"")
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM call failed", e)
            Result.failure(e)
        }
    }
}
