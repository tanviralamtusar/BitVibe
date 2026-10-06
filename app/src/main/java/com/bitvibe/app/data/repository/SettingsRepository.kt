package com.bitvibe.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")

    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: 0 // Default to 0 (System)
        }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { settings ->
            settings[THEME_MODE_KEY] = mode
        }
    }

    private val AUTO_UPDATE_KEY = booleanPreferencesKey("auto_update")
    private val SKIPPED_UPDATE_BUILD_KEY = intPreferencesKey("skipped_update_build")

    /** Download and install new releases automatically (on by default). */
    val autoUpdate: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_UPDATE_KEY] ?: true }

    suspend fun setAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { settings -> settings[AUTO_UPDATE_KEY] = enabled }
    }

    /** A build the user chose "Later" for; it isn't offered again automatically. */
    val skippedUpdateBuild: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[SKIPPED_UPDATE_BUILD_KEY] ?: 0 }

    suspend fun setSkippedUpdateBuild(build: Int) {
        context.dataStore.edit { settings -> settings[SKIPPED_UPDATE_BUILD_KEY] = build }
    }
    
    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        const val THEME_BLACK = 3
    }
}
