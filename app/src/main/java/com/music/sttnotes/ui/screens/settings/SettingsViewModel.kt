package com.music.sttnotes.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.sttnotes.data.api.ApiConfig
import com.music.sttnotes.data.api.LlmProvider
import com.music.sttnotes.data.api.SttProvider
import com.music.sttnotes.data.api.UsageService
import com.music.sttnotes.data.i18n.AppLanguage
import com.music.sttnotes.data.i18n.Strings
import com.music.sttnotes.data.stt.SttLanguage
import com.music.sttnotes.data.stt.SttPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sttProvider: SttProvider = SttProvider.LOCAL,
    val llmProvider: LlmProvider = LlmProvider.NONE,
    val groqApiKey: String = "",
    val openaiApiKey: String = "",
    val xaiApiKey: String = "",
    val anthropicApiKey: String = "",
    val openaiAdminKey: String = "",
    val anthropicAdminKey: String = "",
    val llmSystemPrompt: String = ApiConfig.DEFAULT_SYSTEM_PROMPT,
    val sttLanguage: SttLanguage = SttLanguage.FRENCH,
    val chatFontSize: Float = ApiConfig.DEFAULT_CHAT_FONT_SIZE,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val shareEnabled: Boolean = false,
    val shareApiToken: String = "",
    val shareExpirationDays: Int = ApiConfig.DEFAULT_SHARE_EXPIRATION_DAYS,
    val volumeButtonScrollEnabled: Boolean = false,
    val volumeButtonScrollDistance: Float = 0.8f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiConfig: ApiConfig,
    private val sttPreferences: SttPreferences,
    private val usageService: UsageService,
    private val modelDownloadService: com.music.sttnotes.data.stt.ModelDownloadService,
    private val uiPreferences: com.music.sttnotes.data.ui.UiPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Usage tracking state
    // Usage stats per provider
    private val _openaiUsage7Days = MutableStateFlow(0.0)
    val openaiUsage7Days: StateFlow<Double> = _openaiUsage7Days.asStateFlow()

    private val _openaiUsage30Days = MutableStateFlow(0.0)
    val openaiUsage30Days: StateFlow<Double> = _openaiUsage30Days.asStateFlow()

    private val _openaiInputTokens7Days = MutableStateFlow(0L)
    val openaiInputTokens7Days: StateFlow<Long> = _openaiInputTokens7Days.asStateFlow()

    private val _openaiOutputTokens7Days = MutableStateFlow(0L)
    val openaiOutputTokens7Days: StateFlow<Long> = _openaiOutputTokens7Days.asStateFlow()

    private val _openaiInputTokens30Days = MutableStateFlow(0L)
    val openaiInputTokens30Days: StateFlow<Long> = _openaiInputTokens30Days.asStateFlow()

    private val _openaiOutputTokens30Days = MutableStateFlow(0L)
    val openaiOutputTokens30Days: StateFlow<Long> = _openaiOutputTokens30Days.asStateFlow()

    private val _anthropicUsage7Days = MutableStateFlow(0.0)
    val anthropicUsage7Days: StateFlow<Double> = _anthropicUsage7Days.asStateFlow()

    private val _anthropicUsage30Days = MutableStateFlow(0.0)
    val anthropicUsage30Days: StateFlow<Double> = _anthropicUsage30Days.asStateFlow()

    private val _anthropicInputTokens7Days = MutableStateFlow(0L)
    val anthropicInputTokens7Days: StateFlow<Long> = _anthropicInputTokens7Days.asStateFlow()

    private val _anthropicOutputTokens7Days = MutableStateFlow(0L)
    val anthropicOutputTokens7Days: StateFlow<Long> = _anthropicOutputTokens7Days.asStateFlow()

    private val _anthropicInputTokens30Days = MutableStateFlow(0L)
    val anthropicInputTokens30Days: StateFlow<Long> = _anthropicInputTokens30Days.asStateFlow()

    private val _anthropicOutputTokens30Days = MutableStateFlow(0L)
    val anthropicOutputTokens30Days: StateFlow<Long> = _anthropicOutputTokens30Days.asStateFlow()

    private val _isLoadingUsage = MutableStateFlow(false)
    val isLoadingUsage: StateFlow<Boolean> = _isLoadingUsage.asStateFlow()

    // Expose model download state
    val downloadState = modelDownloadService.downloadState

    init {
        viewModelScope.launch {
            combine(
                combine(
                    apiConfig.sttProvider,
                    apiConfig.llmProvider,
                    apiConfig.groqApiKey,
                    apiConfig.openaiApiKey,
                    apiConfig.xaiApiKey
                ) { stt, llm, groq, openai, xai ->
                    arrayOf(stt, llm, groq, openai, xai)
                },
                combine(
                    apiConfig.anthropicApiKey,
                    apiConfig.openaiAdminKey,
                    apiConfig.anthropicAdminKey,
                    apiConfig.llmSystemPrompt,
                    sttPreferences.selectedLanguage
                ) { anthropic, openaiAdmin, anthropicAdmin, prompt, lang ->
                    arrayOf(anthropic, openaiAdmin, anthropicAdmin, prompt, lang)
                },
                combine(
                    apiConfig.chatFontSize,
                    apiConfig.appLanguage,
                    apiConfig.shareEnabled,
                    apiConfig.shareApiToken,
                    apiConfig.shareExpirationDays
                ) { fontSize, appLang, shareEnabled, shareToken, shareDays ->
                    arrayOf(fontSize, appLang, shareEnabled, shareToken, shareDays)
                },
                combine(
                    uiPreferences.volumeButtonScrollEnabled,
                    uiPreferences.volumeButtonScrollDistance
                ) { volScrollEnabled, volScrollDistance ->
                    arrayOf(volScrollEnabled, volScrollDistance)
                }
            ) { first, second, third, fourth ->
                SettingsUiState(
                    sttProvider = first[0] as SttProvider,
                    llmProvider = first[1] as LlmProvider,
                    groqApiKey = (first[2] as? String) ?: "",
                    openaiApiKey = (first[3] as? String) ?: "",
                    xaiApiKey = (first[4] as? String) ?: "",
                    anthropicApiKey = (second[0] as? String) ?: "",
                    openaiAdminKey = (second[1] as? String) ?: "",
                    anthropicAdminKey = (second[2] as? String) ?: "",
                    llmSystemPrompt = second[3] as String,
                    sttLanguage = second[4] as SttLanguage,
                    chatFontSize = third[0] as Float,
                    appLanguage = third[1] as AppLanguage,
                    shareEnabled = third[2] as Boolean,
                    shareApiToken = (third[3] as? String) ?: "",
                    shareExpirationDays = third[4] as Int,
                    volumeButtonScrollEnabled = fourth[0] as Boolean,
                    volumeButtonScrollDistance = fourth[1] as Float
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

    fun setAnthropicApiKey(key: String) {
        viewModelScope.launch { apiConfig.setAnthropicApiKey(key) }
    }

    fun setOpenaiAdminKey(key: String) {
        viewModelScope.launch { apiConfig.setOpenaiAdminKey(key) }
    }

    fun setAnthropicAdminKey(key: String) {
        viewModelScope.launch { apiConfig.setAnthropicAdminKey(key) }
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

    fun setShareEnabled(enabled: Boolean) {
        viewModelScope.launch { apiConfig.setShareEnabled(enabled) }
    }

    fun setShareApiToken(token: String) {
        viewModelScope.launch { apiConfig.setShareApiToken(token) }
    }

    fun setShareExpirationDays(days: Int) {
        viewModelScope.launch { apiConfig.setShareExpirationDays(days) }
    }

    fun setVolumeButtonScrollEnabled(enabled: Boolean) {
        viewModelScope.launch { uiPreferences.setVolumeButtonScrollEnabled(enabled) }
    }

    fun setVolumeButtonScrollDistance(distance: Float) {
        viewModelScope.launch { uiPreferences.setVolumeButtonScrollDistance(distance) }
    }

    /**
     * Refresh usage statistics for both OpenAI and Anthropic
     */
    fun refreshUsageStats() {
        Log.d("SettingsViewModel", "refreshUsageStats() called")
        viewModelScope.launch {
            _isLoadingUsage.value = true
            Log.d("SettingsViewModel", "Loading started")

            try {
                // Fetch OpenAI usage if admin key is configured
                val openaiAdminKey = _uiState.value.openaiAdminKey
                Log.d("SettingsViewModel", "OpenAI admin key present: ${openaiAdminKey.isNotBlank()}")
                if (openaiAdminKey.isNotBlank()) {
                    Log.d("SettingsViewModel", "Fetching OpenAI 7 days...")
                    val openai7 = usageService.fetchOpenAIUsage(openaiAdminKey, 7)
                    Log.d("SettingsViewModel", "Fetching OpenAI 30 days...")
                    val openai30 = usageService.fetchOpenAIUsage(openaiAdminKey, 30)

                    openai7.onSuccess { stats ->
                        Log.d("SettingsViewModel", "OpenAI 7d: cost=${stats.cost}, input=${stats.inputTokens}, output=${stats.outputTokens}")
                        _openaiUsage7Days.value = stats.cost
                        _openaiInputTokens7Days.value = stats.inputTokens
                        _openaiOutputTokens7Days.value = stats.outputTokens
                    }.onFailure { e ->
                        Log.e("SettingsViewModel", "OpenAI 7d failed", e)
                    }
                    openai30.onSuccess { stats ->
                        Log.d("SettingsViewModel", "OpenAI 30d: cost=${stats.cost}, input=${stats.inputTokens}, output=${stats.outputTokens}")
                        _openaiUsage30Days.value = stats.cost
                        _openaiInputTokens30Days.value = stats.inputTokens
                        _openaiOutputTokens30Days.value = stats.outputTokens
                    }.onFailure { e ->
                        Log.e("SettingsViewModel", "OpenAI 30d failed", e)
                    }
                }

                // Fetch Anthropic usage if admin key is configured
                val anthropicAdminKey = _uiState.value.anthropicAdminKey
                Log.d("SettingsViewModel", "Anthropic admin key present: ${anthropicAdminKey.isNotBlank()}")
                if (anthropicAdminKey.isNotBlank()) {
                    Log.d("SettingsViewModel", "Fetching Anthropic 7 days...")
                    val anthropic7 = usageService.fetchAnthropicUsage(anthropicAdminKey, 7)
                    Log.d("SettingsViewModel", "Fetching Anthropic 30 days...")
                    val anthropic30 = usageService.fetchAnthropicUsage(anthropicAdminKey, 30)

                    anthropic7.onSuccess { stats ->
                        Log.d("SettingsViewModel", "Anthropic 7d: cost=${stats.cost}, input=${stats.inputTokens}, output=${stats.outputTokens}")
                        _anthropicUsage7Days.value = stats.cost
                        _anthropicInputTokens7Days.value = stats.inputTokens
                        _anthropicOutputTokens7Days.value = stats.outputTokens
                    }.onFailure { e ->
                        Log.e("SettingsViewModel", "Anthropic 7d failed", e)
                    }
                    anthropic30.onSuccess { stats ->
                        Log.d("SettingsViewModel", "Anthropic 30d: cost=${stats.cost}, input=${stats.inputTokens}, output=${stats.outputTokens}")
                        _anthropicUsage30Days.value = stats.cost
                        _anthropicInputTokens30Days.value = stats.inputTokens
                        _anthropicOutputTokens30Days.value = stats.outputTokens
                    }.onFailure { e ->
                        Log.e("SettingsViewModel", "Anthropic 30d failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to fetch usage", e)
            } finally {
                _isLoadingUsage.value = false
                Log.d("SettingsViewModel", "Loading finished")
            }
        }
    }

    init {
        // Auto-refresh usage when admin keys change
        viewModelScope.launch {
            _uiState.collectLatest { state ->
                if (state.openaiAdminKey.isNotBlank() || state.anthropicAdminKey.isNotBlank()) {
                    refreshUsageStats()
                }
            }
        }
    }

    // Whisper model download methods
    fun downloadModel(model: com.music.sttnotes.data.stt.WhisperModel) {
        viewModelScope.launch {
            modelDownloadService.downloadModel(model)
        }
    }

    fun deleteModel(model: com.music.sttnotes.data.stt.WhisperModel) {
        viewModelScope.launch {
            modelDownloadService.deleteModel(model)
        }
    }

    fun isModelDownloaded(model: com.music.sttnotes.data.stt.WhisperModel): Boolean {
        return modelDownloadService.isModelDownloaded(model)
    }

    fun selectModel(model: com.music.sttnotes.data.stt.WhisperModel) {
        viewModelScope.launch {
            apiConfig.setSelectedWhisperModel(model.name)
        }
    }
}
