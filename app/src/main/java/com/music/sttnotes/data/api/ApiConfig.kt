package com.music.sttnotes.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music.sttnotes.data.i18n.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiDataStore by preferencesDataStore(name = "api_config")

enum class SttProvider { LOCAL, GROQ, OPENAI }
enum class LlmProvider { GROQ, OPENAI, XAI, NONE }

fun LlmProvider.displayName(): String = when (this) {
    LlmProvider.GROQ -> "Groq"
    LlmProvider.OPENAI -> "GPT"
    LlmProvider.XAI -> "Grok"
    LlmProvider.NONE -> "None"
}

@Singleton
class ApiConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val XAI_API_KEY = stringPreferencesKey("xai_api_key")
        private val STT_PROVIDER = stringPreferencesKey("stt_provider")
        private val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        private val LLM_SYSTEM_PROMPT = stringPreferencesKey("llm_system_prompt")
        private val CHAT_FONT_SIZE = floatPreferencesKey("chat_font_size")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")

        const val DEFAULT_CHAT_FONT_SIZE = 14f
        const val MIN_CHAT_FONT_SIZE = 10f
        const val MAX_CHAT_FONT_SIZE = 20f

        const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
        const val OPENAI_BASE_URL = "https://api.openai.com/v1/"
        const val XAI_BASE_URL = "https://api.x.ai/v1/"

        val DEFAULT_SYSTEM_PROMPT = """
            Tu es un assistant intelligent et serviable.
            - Réponds de manière concise et utile aux questions
            - Utilise le format markdown pour structurer tes réponses (listes, titres, code, etc.)
            - Réponds en français sauf si l'utilisateur écrit dans une autre langue
        """.trimIndent()
    }

    val groqApiKey: Flow<String?> = context.apiDataStore.data.map { it[GROQ_API_KEY] }
    val openaiApiKey: Flow<String?> = context.apiDataStore.data.map { it[OPENAI_API_KEY] }
    val xaiApiKey: Flow<String?> = context.apiDataStore.data.map { it[XAI_API_KEY] }
    val sttProvider: Flow<SttProvider> = context.apiDataStore.data.map {
        try {
            SttProvider.valueOf(it[STT_PROVIDER] ?: SttProvider.LOCAL.name)
        } catch (e: IllegalArgumentException) {
            SttProvider.LOCAL
        }
    }
    val llmProvider: Flow<LlmProvider> = context.apiDataStore.data.map {
        try {
            LlmProvider.valueOf(it[LLM_PROVIDER] ?: LlmProvider.NONE.name)
        } catch (e: IllegalArgumentException) {
            LlmProvider.NONE
        }
    }
    val llmSystemPrompt: Flow<String> = context.apiDataStore.data.map {
        it[LLM_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
    }
    val chatFontSize: Flow<Float> = context.apiDataStore.data.map {
        it[CHAT_FONT_SIZE] ?: DEFAULT_CHAT_FONT_SIZE
    }
    val appLanguage: Flow<AppLanguage> = context.apiDataStore.data.map {
        try {
            AppLanguage.valueOf(it[APP_LANGUAGE] ?: AppLanguage.ENGLISH.name)
        } catch (e: IllegalArgumentException) {
            AppLanguage.ENGLISH
        }
    }

    suspend fun setGroqApiKey(key: String) {
        context.apiDataStore.edit { it[GROQ_API_KEY] = key }
    }

    suspend fun setOpenaiApiKey(key: String) {
        context.apiDataStore.edit { it[OPENAI_API_KEY] = key }
    }

    suspend fun setXaiApiKey(key: String) {
        context.apiDataStore.edit { it[XAI_API_KEY] = key }
    }

    suspend fun setSttProvider(provider: SttProvider) {
        context.apiDataStore.edit { it[STT_PROVIDER] = provider.name }
    }

    suspend fun setLlmProvider(provider: LlmProvider) {
        context.apiDataStore.edit { it[LLM_PROVIDER] = provider.name }
    }

    suspend fun setLlmSystemPrompt(prompt: String) {
        context.apiDataStore.edit { it[LLM_SYSTEM_PROMPT] = prompt }
    }

    suspend fun setChatFontSize(size: Float) {
        context.apiDataStore.edit { it[CHAT_FONT_SIZE] = size.coerceIn(MIN_CHAT_FONT_SIZE, MAX_CHAT_FONT_SIZE) }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.apiDataStore.edit { it[APP_LANGUAGE] = language.name }
    }
}
