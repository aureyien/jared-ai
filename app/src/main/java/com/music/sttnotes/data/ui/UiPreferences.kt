package com.music.sttnotes.data.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_preferences")

@Singleton
class UiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val kbIsListViewKey = booleanPreferencesKey("kb_is_list_view")
    private val kbPreviewFontSizeKey = floatPreferencesKey("kb_preview_font_size")
    private val volumeButtonScrollEnabledKey = booleanPreferencesKey("volume_button_scroll_enabled")
    private val volumeButtonScrollDistanceKey = floatPreferencesKey("volume_button_scroll_distance")

    val kbIsListView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[kbIsListViewKey] ?: true // Default to list view
    }

    val kbPreviewFontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[kbPreviewFontSizeKey] ?: 9f // Default 9sp
    }

    val volumeButtonScrollEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[volumeButtonScrollEnabledKey] ?: false // Default disabled
    }

    val volumeButtonScrollDistance: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[volumeButtonScrollDistanceKey] ?: 0.8f // Default 80% of viewport
    }

    suspend fun setKbIsListView(isListView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[kbIsListViewKey] = isListView
        }
    }

    suspend fun setKbPreviewFontSize(fontSize: Float) {
        context.dataStore.edit { preferences ->
            preferences[kbPreviewFontSizeKey] = fontSize
        }
    }

    suspend fun setVolumeButtonScrollEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[volumeButtonScrollEnabledKey] = enabled
        }
    }

    suspend fun setVolumeButtonScrollDistance(distance: Float) {
        context.dataStore.edit { preferences ->
            preferences[volumeButtonScrollDistanceKey] = distance.coerceIn(0.3f, 1.0f)
        }
    }
}
