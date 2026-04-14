package com.example.d_linkmobilymanagement.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.d_linkmobilymanagement.data.model.AppLanguageSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "language_prefs")

class AppLanguageStore(private val context: Context) {

    private object Keys {
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val HAS_USER_CHOSEN = booleanPreferencesKey("has_user_chosen")
    }

    val languageSettings: Flow<AppLanguageSettings> =
        context.languageDataStore.data.map { prefs ->
            AppLanguageSettings(
                selectedLanguageTag = prefs[Keys.SELECTED_LANGUAGE],
                hasUserChosenLanguage = prefs[Keys.HAS_USER_CHOSEN] ?: false
            )
        }

    suspend fun saveLanguage(languageTag: String, hasUserChosen: Boolean) {
        context.languageDataStore.edit { prefs ->
            prefs[Keys.SELECTED_LANGUAGE] = languageTag
            prefs[Keys.HAS_USER_CHOSEN] = hasUserChosen
        }
    }
}
