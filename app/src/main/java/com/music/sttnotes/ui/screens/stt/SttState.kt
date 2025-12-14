package com.music.sttnotes.ui.screens.stt

sealed class SttState {
    data object CheckingModel : SttState()
    data class DownloadingModel(val progress: Float) : SttState()
    data class ModelError(val message: String) : SttState()
    data object Idle : SttState()
    data class Recording(val duration: Int) : SttState()
    data class Processing(val debugInfo: String = "") : SttState()
    data class Result(val text: String, val debugInfo: String = "") : SttState()
    data class Error(val message: String) : SttState()
}
