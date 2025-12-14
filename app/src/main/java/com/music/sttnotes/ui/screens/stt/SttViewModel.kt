package com.music.sttnotes.ui.screens.stt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.stt.AudioRecorder
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttManager
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SttViewModel @Inject constructor(
    private val sttManager: SttManager,
    private val audioRecorder: AudioRecorder,
    private val sttPreferences: SttPreferences,
    private val apiConfig: ApiConfig
) : ViewModel() {

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    val state: StateFlow<SttState> = _state

    private val _selectedLanguage = MutableStateFlow(SttLanguage.FRENCH)
    val selectedLanguage: StateFlow<SttLanguage> = _selectedLanguage

    private var recordingJob: Job? = null
    private var durationJob: Job? = null
    private var recordingDuration = 0

    init {
        viewModelScope.launch {
            sttPreferences.selectedLanguage.collect {
                _selectedLanguage.value = it
            }
        }
        // No automatic Whisper initialization - SttManager handles it lazily
    }

    fun hasAudioPermission(): Boolean = audioRecorder.hasPermission()

    fun startRecording() {
        if (_state.value !is SttState.Idle && _state.value !is SttState.Result) return

        recordingDuration = 0
        _state.value = SttState.Recording(0)

        recordingJob = viewModelScope.launch {
            audioRecorder.startRecording()
        }

        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                recordingDuration++
                if (_state.value is SttState.Recording) {
                    _state.value = SttState.Recording(recordingDuration)
                }

                // Max 30s recording
                if (recordingDuration >= 30) {
                    stopRecording()
                    break
                }
            }
        }
    }

    fun stopRecording() {
        durationJob?.cancel()
        recordingJob?.cancel()

        val audioData = audioRecorder.stopRecording()

        if (audioData.isEmpty()) {
            _state.value = SttState.Error("No audio recorded")
            return
        }

        // Calculate debug info
        val duration = audioData.size.toFloat() / AudioRecorder.SAMPLE_RATE
        val maxAmp = audioData.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        val avgAmp = audioData.map { kotlin.math.abs(it.toInt()) }.average().toInt()
        val debugInfo = "Samples: ${audioData.size} (${String.format("%.1f", duration)}s)\n" +
                "Amplitude: max=$maxAmp, avg=$avgAmp\n" +
                "Bytes: ${audioData.size * 2}"

        _state.value = SttState.Processing(debugInfo)

        viewModelScope.launch {
            val language = sttPreferences.selectedLanguage.first()
            val startTime = System.currentTimeMillis()

            sttManager.transcribe(audioData, language.code).fold(
                onSuccess = { text ->
                    val elapsed = System.currentTimeMillis() - startTime
                    val resultDebug = "$debugInfo\nTranscribe: ${elapsed}ms"

                    if (text.isBlank()) {
                        _state.value = SttState.Result("No speech detected", resultDebug)
                    } else {
                        _state.value = SttState.Result(text, resultDebug)
                    }
                },
                onFailure = { error ->
                    _state.value = SttState.Error(error.message ?: "Transcription failed")
                }
            )
        }
    }

    fun cancelRecording() {
        durationJob?.cancel()
        recordingJob?.cancel()
        audioRecorder.cancelRecording()
        _state.value = SttState.Idle
    }

    fun setLanguage(language: SttLanguage) {
        viewModelScope.launch {
            sttPreferences.setLanguage(language)
        }
    }

    fun resetToIdle() {
        _state.value = SttState.Idle
    }
}
