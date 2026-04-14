package com.example.d_linkmobilymanagement.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.d_linkmobilymanagement.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {

    companion object {
        private val AUTO_REFRESH_ENABLED = booleanPreferencesKey("auto_refresh_enabled")
        private val REFRESH_INTERVAL_SECONDS = intPreferencesKey("refresh_interval_seconds")
    }

    val appSettings: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            autoRefreshEnabled = prefs[AUTO_REFRESH_ENABLED] ?: true,
            refreshIntervalSeconds = prefs[REFRESH_INTERVAL_SECONDS] ?: 2
        )
    }

    suspend fun updateAutoRefreshEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[AUTO_REFRESH_ENABLED] = enabled
        }
    }

    suspend fun updateRefreshInterval(seconds: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[REFRESH_INTERVAL_SECONDS] = seconds
        }
    }
}
