package com.music.sttnotes.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sttProvider: SttProvider = SttProvider.LOCAL,
    val llmProvider: LlmProvider = LlmProvider.NONE,
    val groqApiKey: String = "",
    val openaiApiKey: String = "",
    val xaiApiKey: String = "",
    val llmSystemPrompt: String = ApiConfig.DEFAULT_SYSTEM_PROMPT,
    val sttLanguage: SttLanguage = SttLanguage.FRENCH
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiConfig: ApiConfig,
    private val sttPreferences: SttPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                apiConfig.sttProvider,
                apiConfig.llmProvider,
                apiConfig.groqApiKey,
                apiConfig.openaiApiKey,
                apiConfig.xaiApiKey,
                apiConfig.llmSystemPrompt,
                sttPreferences.selectedLanguage
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    sttProvider = values[0] as SttProvider,
                    llmProvider = values[1] as LlmProvider,
                    groqApiKey = (values[2] as? String) ?: "",
                    openaiApiKey = (values[3] as? String) ?: "",
                    xaiApiKey = (values[4] as? String) ?: "",
                    llmSystemPrompt = values[5] as String,
                    sttLanguage = values[6] as SttLanguage
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setSttProvider(provider: SttProvider) {
        viewModelScope.launch { apiConfig.setSttProvider(provider) }
    }

    fun setLlmProvider(provider: LlmProvider) {
        viewModelScope.launch { apiConfig.setLlmProvider(provider) }
    }

    fun setGroqApiKey(key: String) {
        viewModelScope.launch { apiConfig.setGroqApiKey(key) }
    }

    fun setOpenaiApiKey(key: String) {
        viewModelScope.launch { apiConfig.setOpenaiApiKey(key) }
    }

    fun setXaiApiKey(key: String) {
        viewModelScope.launch { apiConfig.setXaiApiKey(key) }
    }

    fun setLlmSystemPrompt(prompt: String) {
        viewModelScope.launch { apiConfig.setLlmSystemPrompt(prompt) }
    }

    fun setSttLanguage(language: SttLanguage) {
        viewModelScope.launch { sttPreferences.setLanguage(language) }
    }
}
