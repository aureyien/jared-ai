package com.music.sttnotes.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TranscriptionResponse(
    val text: String
)

@Singleton
class CloudSttService @Inject constructor() {
    companion object {
        private const val TAG = "CloudSttService"
        private const val SAMPLE_RATE = 16000
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribe audio using Groq Whisper API
     * @param audioData PCM 16-bit mono 16kHz audio
     * @param language ISO language code (e.g., "fr", "en")
     * @param apiKey Groq API key
     */
    suspend fun transcribeWithGroq(
        audioData: ShortArray,
        language: String = "fr",
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Convert PCM to WAV format
            val wavBytes = pcmToWav(audioData)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-large-v3-turbo")
                .addFormDataPart("language", language)
                .addFormDataPart("response_format", "json")
                .addFormDataPart(
                    "file",
                    "audio.wav",
                    wavBytes.toRequestBody("audio/wav".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("${ApiConfig.GROQ_BASE_URL}audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val audioDuration = audioData.size.toFloat() / SAMPLE_RATE
            Log.d(TAG, "Sending ${wavBytes.size} bytes (${audioDuration}s) to Groq Whisper API...")
            val startTime = System.currentTimeMillis()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Groq API error ${response.code}: $errorBody")
                    return@withContext Result.failure(IOException("API error ${response.code}: $errorBody"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))

                val transcription = json.decodeFromString<TranscriptionResponse>(responseBody)
                Log.d(TAG, "Transcription completed in ${elapsed}ms: \"${transcription.text.take(50)}${if (transcription.text.length > 50) "..." else ""}\"")

                Result.success(transcription.text.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(e)
        }
    }

    /**
     * Convert PCM 16-bit mono audio to WAV format
     */
    private fun pcmToWav(pcmData: ShortArray): ByteArray {
        val byteRate = SAMPLE_RATE * 2  // 16-bit mono
        val dataSize = pcmData.size * 2
        val fileSize = 36 + dataSize

        val output = ByteArrayOutputStream()

        // RIFF header
        output.write("RIFF".toByteArray())
        output.write(intToBytes(fileSize, 4))
        output.write("WAVE".toByteArray())

        // fmt chunk
        output.write("fmt ".toByteArray())
        output.write(intToBytes(16, 4))        // Chunk size
        output.write(intToBytes(1, 2))         // Audio format (PCM)
        output.write(intToBytes(1, 2))         // Num channels (mono)
        output.write(intToBytes(SAMPLE_RATE, 4))  // Sample rate
        output.write(intToBytes(byteRate, 4))  // Byte rate
        output.write(intToBytes(2, 2))         // Block align
        output.write(intToBytes(16, 2))        // Bits per sample

        // data chunk
        output.write("data".toByteArray())
        output.write(intToBytes(dataSize, 4))

        // PCM data (little-endian)
        for (sample in pcmData) {
            output.write(sample.toInt() and 0xFF)
            output.write((sample.toInt() shr 8) and 0xFF)
        }

        return output.toByteArray()
    }

    private fun intToBytes(value: Int, numBytes: Int): ByteArray {
        val bytes = ByteArray(numBytes)
        for (i in 0 until numBytes) {
            bytes[i] = ((value shr (8 * i)) and 0xFF).toByte()
        }
        return bytes
    }
}
