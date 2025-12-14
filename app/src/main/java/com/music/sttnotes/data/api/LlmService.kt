package com.music.sttnotes.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

@Singleton
class LlmService @Inject constructor() {
    companion object {
        private const val TAG = "LlmService"

        // Model recommendations (Dec 2025)
        const val GROQ_MODEL = "llama-3.3-70b-versatile"  // Fast, free tier
        const val OPENAI_MODEL = "gpt-5-mini"             // $0.25/1M input, $2/1M output
        const val XAI_MODEL = "grok-4-1-fast-reasoning"   // $0.20/1M input, $0.50/1M output
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
            LlmProvider.NONE -> {
                // Return last user message content if available
                messages.lastOrNull { it.role == "user" }?.content?.let { emit(it) }
                return@flow
            }
        }

        // Build request with system prompt + full conversation history
        val allMessages = listOf(ChatMessage(role = "system", content = systemPrompt)) +
            messages.takeLast(50)  // Limit to last 50 messages to avoid token overflow

        val requestBody = ChatRequest(
            model = model,
            messages = allMessages,
            stream = true
        )

        val jsonBody = json.encodeToString(requestBody)
        Log.d(TAG, "Streaming request: $jsonBody")

        val request = Request.Builder()
            .url("${baseUrl}chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

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

                    // Parse the JSON chunk
                    try {
                        val chunk = json.decodeFromString<ChatResponse>(data)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
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
