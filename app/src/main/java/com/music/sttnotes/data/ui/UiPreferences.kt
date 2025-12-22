package com.music.sttnotes.data.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    val kbIsListView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[kbIsListViewKey] ?: true // Default to list view
    }

    suspend fun setKbIsListView(isListView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[kbIsListViewKey] = isListView
        }
    }
}
