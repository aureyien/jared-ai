package com.music.sttnotes.data.stt

import android.util.Log
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.CloudSttService
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.LlmService
import com.music.sttnotes.data.api.SttProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified STT manager that handles both local (whisper.cpp) and cloud (Groq) transcription.
 * Local Whisper is initialized lazily only when needed to save battery.
 */
@Singleton
class SttManager @Inject constructor(
    private val whisperManager: WhisperManager,
    private val cloudSttService: CloudSttService,
    private val llmService: LlmService,
    private val apiConfig: ApiConfig
) {
    companion object {
        private const val TAG = "SttManager"
    }

    /**
     * Initialize local Whisper only if needed (lazy loading)
     */
    private suspend fun ensureLocalWhisperReady(): Boolean {
        if (whisperManager.state.value is WhisperState.Ready) {
            return true
        }
        if (whisperManager.state.value is WhisperState.NotInitialized) {
            Log.d(TAG, "Initializing local Whisper on demand...")
            val result = whisperManager.initialize()
            return result.isSuccess
        }
        return false
    }

    /**
     * Transcribe audio using configured provider.
     * Local Whisper is only initialized if provider is LOCAL or as fallback.
     */
    suspend fun transcribe(
        audioData: ShortArray,
        language: String = "fr"
    ): Result<String> {
        val provider = apiConfig.sttProvider.first()
        Log.d(TAG, "Transcribing with provider: $provider")

        return when (provider) {
            SttProvider.LOCAL -> {
                if (!ensureLocalWhisperReady()) {
                    return Result.failure(Exception("Failed to initialize local Whisper"))
                }
                whisperManager.transcribe(audioData, language)
            }
            SttProvider.GROQ -> {
                val apiKey = apiConfig.groqApiKey.first()
                if (apiKey.isNullOrBlank()) {
                    Log.e(TAG, "Groq API key not configured, falling back to local")
                    if (!ensureLocalWhisperReady()) {
                        return Result.failure(Exception("Groq not configured and local Whisper failed"))
                    }
                    whisperManager.transcribe(audioData, language)
                } else {
                    cloudSttService.transcribeWithGroq(audioData, language, apiKey)
                }
            }
            SttProvider.OPENAI -> {
                // OpenAI Whisper API could be added later - for now fall back to local
                Log.w(TAG, "OpenAI STT not yet implemented, using local")
                if (!ensureLocalWhisperReady()) {
                    return Result.failure(Exception("OpenAI not implemented and local Whisper failed"))
                }
                whisperManager.transcribe(audioData, language)
            }
        }
    }

    /**
     * Process transcription with LLM (if configured)
     */
    suspend fun processWithLlm(text: String): Result<String> {
        val provider = apiConfig.llmProvider.first()

        if (provider == LlmProvider.NONE) {
            return Result.success(text)
        }

        val apiKey = when (provider) {
            LlmProvider.GROQ -> apiConfig.groqApiKey.first()
            LlmProvider.OPENAI -> apiConfig.openaiApiKey.first()
            LlmProvider.XAI -> apiConfig.xaiApiKey.first()
            LlmProvider.NONE -> null
        }

        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "LLM API key not configured, skipping processing")
            return Result.success(text)
        }

        val systemPrompt = apiConfig.llmSystemPrompt.first()
        return llmService.processWithLlm(text, systemPrompt, provider, apiKey)
    }

    /**
     * Full pipeline: transcribe + optional LLM processing
     */
    suspend fun transcribeAndProcess(
        audioData: ShortArray,
        language: String = "fr"
    ): Result<TranscriptionResult> {
        // Step 1: Transcribe
        val transcriptionResult = transcribe(audioData, language)
        if (transcriptionResult.isFailure) {
            return Result.failure(transcriptionResult.exceptionOrNull()!!)
        }

        val rawText = transcriptionResult.getOrThrow()

        // Step 2: LLM processing (if enabled)
        val llmResult = processWithLlm(rawText)
        val llmProvider = apiConfig.llmProvider.first()

        return Result.success(TranscriptionResult(
            rawTranscription = rawText,
            processedText = llmResult.getOrNull() ?: rawText,
            wasProcessedByLlm = llmResult.isSuccess && llmProvider != LlmProvider.NONE
        ))
    }

    /**
     * Get current STT provider
     */
    suspend fun getCurrentProvider(): SttProvider = apiConfig.sttProvider.first()
}

data class TranscriptionResult(
    val rawTranscription: String,
    val processedText: String,
    val wasProcessedByLlm: Boolean
)
