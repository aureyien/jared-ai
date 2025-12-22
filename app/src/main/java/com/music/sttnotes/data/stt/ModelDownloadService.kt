package com.music.sttnotes.data.stt

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    data object NotStarted : DownloadState()
    data class Downloading(val progress: Float, val downloadedMB: Long, val totalMB: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

enum class WhisperModel(
    val filename: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val displayName: String
) {
    TINY(
        filename = "ggml-tiny-q5_1.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
        sizeBytes = 31_000_000, // ~31MB
        displayName = "Tiny (31MB, fast)"
    ),
    BASE(
        filename = "ggml-base-q5_1.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
        sizeBytes = 58_000_000, // ~58MB
        displayName = "Base (58MB, balanced)"
    ),
    SMALL(
        filename = "ggml-small-q8_0.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q8_0.bin",
        sizeBytes = 252_000_000, // ~252MB
        displayName = "Small (252MB, accurate)"
    ),
    MEDIUM(
        filename = "ggml-medium-q5_0.bin",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin",
        sizeBytes = 514_000_000, // ~514MB
        displayName = "Medium (514MB, very accurate)"
    )
}

@Singleton
class ModelDownloadService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val okHttpClient = OkHttpClient()
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.NotStarted)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "whisper_models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelFile(model: WhisperModel): File {
        return File(getModelsDir(), model.filename)
    }

    fun isModelDownloaded(model: WhisperModel): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.length() > 0
    }

    suspend fun downloadModel(model: WhisperModel): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download of ${model.displayName}")
            _downloadState.value = DownloadState.Downloading(0f, 0, model.sizeBytes / 1_000_000)

            val outputFile = getModelFile(model)

            // If file already exists and has correct size, skip download
            if (outputFile.exists() && outputFile.length() == model.sizeBytes) {
                Log.d(TAG, "Model already downloaded: ${model.filename}")
                _downloadState.value = DownloadState.Completed
                return@withContext Result.success(outputFile)
            }

            // Delete partial download if exists
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val request = Request.Builder()
                .url(model.downloadUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("HTTP ${response.code}")
                return@withContext Result.failure(
                    Exception("Download failed: HTTP ${response.code}")
                )
            }

            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("Empty response")
                return@withContext Result.failure(Exception("Empty response body"))
            }

            val contentLength = body.contentLength()
            Log.d(TAG, "Content length: $contentLength bytes")

            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            FileOutputStream(outputFile).use { output ->
                body.byteStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (contentLength > 0) {
                            (downloadedBytes.toFloat() / contentLength)
                        } else {
                            0f
                        }

                        val downloadedMB = downloadedBytes / 1_000_000
                        val totalMB = contentLength / 1_000_000

                        _downloadState.value = DownloadState.Downloading(
                            progress = progress,
                            downloadedMB = downloadedMB,
                            totalMB = totalMB
                        )

                        // Log progress every 50MB
                        if (downloadedMB % 50 == 0L && downloadedBytes > 0) {
                            Log.d(TAG, "Downloaded: $downloadedMB MB / $totalMB MB (${(progress * 100).toInt()}%)")
                        }
                    }
                }
            }

            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            _downloadState.value = DownloadState.Completed

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    suspend fun deleteModel(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = getModelFile(model)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted model: ${model.filename}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model", e)
            Result.failure(e)
        }
    }

    fun getDownloadedModels(): List<WhisperModel> {
        return WhisperModel.entries.filter { isModelDownloaded(it) }
    }

    companion object {
        private const val TAG = "ModelDownloadService"
    }
}
