package com.music.sttnotes.data.stt

import android.content.Context
import android.util.Log
import com.music.sttnotes.data.api.ApiConfig
import com.whispercpp.whisper.WhisperContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class WhisperState {
    data object NotInitialized : WhisperState()
    data class Loading(val progress: Float) : WhisperState()
    data object Ready : WhisperState()
    data class Error(val message: String) : WhisperState()
}

@Singleton
class WhisperManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadService: ModelDownloadService,
    private val apiConfig: ApiConfig
) {
    private var whisperContext: WhisperContext? = null

    private val _state = MutableStateFlow<WhisperState>(WhisperState.NotInitialized)
    val state: StateFlow<WhisperState> = _state

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription

    suspend fun initialize(selectedModel: WhisperModel? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = WhisperState.Loading(0.1f)

            // Determine which model to use
            val model = selectedModel ?: run {
                val savedModelName = apiConfig.selectedWhisperModel.first()
                savedModelName?.let {
                    try {
                        WhisperModel.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        WhisperModel.SMALL
                    }
                } ?: WhisperModel.SMALL
            }

            // Try to load from downloaded models first
            val downloadedFile = modelDownloadService.getModelFile(model)
            if (downloadedFile.exists()) {
                Log.d(TAG, "Loading model from file: ${downloadedFile.absolutePath}")
                whisperContext = WhisperContext.createContextFromFile(downloadedFile.absolutePath)
                Log.d(TAG, "Model loaded from downloaded file: ${model.displayName}")
            } else {
                // Fallback: Try to load from bundled assets
                Log.d(TAG, "Attempting to load model from assets: models/${model.filename}")
                try {
                    whisperContext = WhisperContext.createContextFromAsset(context.assets, "models/${model.filename}")
                    Log.d(TAG, "Model loaded from bundled assets: ${model.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Model not found in assets or downloaded", e)
                    _state.value = WhisperState.Error("Model not downloaded. Please download a model in Settings.")
                    return@withContext Result.failure(Exception("Model not available. Please download a model in Settings."))
                }
            }

            _state.value = WhisperState.Loading(0.5f)

            // Warm-up: run a short inference to initialize internal caches
            Log.d(TAG, "Running warm-up inference...")
            try {
                val warmupStart = System.currentTimeMillis()
                val silentAudio = FloatArray(SAMPLE_RATE * 3) { 0f }
                whisperContext?.transcribeDataWithLang(
                    data = silentAudio,
                    language = "fr",
                    translate = false,
                    printTimestamp = false
                )
                val warmupTime = System.currentTimeMillis() - warmupStart
                Log.d(TAG, "Warm-up completed in ${warmupTime}ms")
            } catch (e: Exception) {
                Log.w(TAG, "Warm-up failed (non-critical): ${e.message}")
            }

            _state.value = WhisperState.Ready
            Log.d(TAG, "WhisperContext initialized successfully from assets")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            _state.value = WhisperState.Error(e.message ?: "Initialization failed")
            Result.failure(e)
        }
    }

    suspend fun transcribe(
        audioData: ShortArray,
        language: String = "fr"
    ): Result<String> = withContext(Dispatchers.IO) {
        val ctx = whisperContext ?: return@withContext Result.failure(Exception("Not initialized"))

        try {
            _transcription.value = ""
            val floatData = shortArrayToFloatArray(audioData)

            val startTime = System.currentTimeMillis()
            val result = ctx.transcribeDataWithLang(
                data = floatData,
                language = language,
                translate = false,
                printTimestamp = false
            )

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Transcription completed in ${elapsed}ms")

            _transcription.value = result.trim()
            Result.success(result.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(e)
        }
    }

    private fun shortArrayToFloatArray(shortArray: ShortArray): FloatArray {
        return FloatArray(shortArray.size) { i ->
            shortArray[i].toFloat() / 32768.0f
        }
    }

    fun release() {
        whisperContext?.let {
            kotlinx.coroutines.runBlocking {
                it.release()
            }
        }
        whisperContext = null
        _state.value = WhisperState.NotInitialized
    }

    companion object {
        private const val TAG = "WhisperManager"
        const val SAMPLE_RATE = 16000
        private const val MODEL_FILENAME = "ggml-small-q8_0.bin"
    }
}
