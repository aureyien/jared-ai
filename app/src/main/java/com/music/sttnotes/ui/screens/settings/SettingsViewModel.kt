package com.music.sttnotes.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.i18n.AppLanguage
import com.music.sttnotes.data.i18n.Strings
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
    val sttLanguage: SttLanguage = SttLanguage.FRENCH,
    val chatFontSize: Float = ApiConfig.DEFAULT_CHAT_FONT_SIZE,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH
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
                apiConfig.xaiApiKey
            ) { stt, llm, groq, openai, xai ->
                arrayOf(stt, llm, groq, openai, xai)
            }.combine(
                combine(
                    apiConfig.llmSystemPrompt,
                    sttPreferences.selectedLanguage,
                    apiConfig.chatFontSize,
                    apiConfig.appLanguage
                ) { prompt, lang, fontSize, appLang ->
                    arrayOf(prompt, lang, fontSize, appLang)
                }
            ) { first, second ->
                SettingsUiState(
                    sttProvider = first[0] as SttProvider,
                    llmProvider = first[1] as LlmProvider,
                    groqApiKey = (first[2] as? String) ?: "",
                    openaiApiKey = (first[3] as? String) ?: "",
                    xaiApiKey = (first[4] as? String) ?: "",
                    llmSystemPrompt = second[0] as String,
                    sttLanguage = second[1] as SttLanguage,
                    chatFontSize = second[2] as Float,
                    appLanguage = second[3] as AppLanguage
                )
            }.collect { state ->
                _uiState.value = state
                // Update the global Strings object when language changes
                Strings.setLanguage(state.appLanguage)
            }
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

    fun resetLlmSystemPrompt() {
        viewModelScope.launch { apiConfig.setLlmSystemPrompt(ApiConfig.DEFAULT_SYSTEM_PROMPT) }
    }

    fun setSttLanguage(language: SttLanguage) {
        viewModelScope.launch { sttPreferences.setLanguage(language) }
    }

    fun setChatFontSize(size: Float) {
        viewModelScope.launch { apiConfig.setChatFontSize(size) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { apiConfig.setAppLanguage(language) }
    }
}
