package com.music.sttnotes.data.stt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stt_preferences")

@Singleton
class SttPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("selected_language")

    val selectedLanguage: Flow<SttLanguage> = context.dataStore.data.map { preferences ->
        val code = preferences[languageKey] ?: SttLanguage.FRENCH.code
        SttLanguage.fromCode(code)
    }

    suspend fun setLanguage(language: SttLanguage) {
        context.dataStore.edit { preferences ->
            preferences[languageKey] = language.code
        }
    }
}
