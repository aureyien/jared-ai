package com.music.sttnotes.data.stt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private val audioData = mutableListOf<Short>()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return@withContext Result.failure(Exception("Invalid buffer size: $bufferSize"))
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord initialization failed"))
            }

            audioData.clear()
            audioRecord?.startRecording()
            _isRecording.value = true

            Log.d(TAG, "Recording started")

            val buffer = ShortArray(bufferSize)
            var maxAmplitude = 0

            try {
                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        for (i in 0 until read) {
                            audioData.add(buffer[i])
                            val amplitude = kotlin.math.abs(buffer[i].toInt())
                            if (amplitude > maxAmplitude) maxAmplitude = amplitude
                        }

                        // Log every second
                        val seconds = audioData.size / SAMPLE_RATE
                        if (audioData.size % SAMPLE_RATE < buffer.size) {
                            Log.d(TAG, "Recording: ${seconds}s, max amplitude: $maxAmplitude (32767 = max)")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Recording coroutine cancelled")
                _isRecording.value = false
                throw e
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Recording failed", e)
            _isRecording.value = false
            Result.failure(e)
        }
    }

    fun stopRecording(): ShortArray {
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val result = audioData.toShortArray()
        val duration = result.size.toFloat() / SAMPLE_RATE
        Log.d(TAG, "Recording stopped, ${result.size} samples (${duration}s)")

        return result
    }

    fun cancelRecording() {
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        audioData.clear()
        Log.d(TAG, "Recording cancelled")
    }

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
    }
}
