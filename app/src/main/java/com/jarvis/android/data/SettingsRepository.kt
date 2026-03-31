package com.jarvis.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val TELEGRAM_ID = longPreferencesKey("telegram_id")
        val TOKEN_EXPIRES_AT = stringPreferencesKey("token_expires_at")
        val PORCUPINE_API_KEY = stringPreferencesKey("porcupine_api_key")
    }

    val authToken: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] ?: ""
        }

    val telegramId: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TELEGRAM_ID] ?: 0L
        }

    val tokenExpiresAt: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TOKEN_EXPIRES_AT] ?: ""
        }

    val porcupineApiKey: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PORCUPINE_API_KEY] ?: ""
        }

    val isAuthenticated: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val token = preferences[PreferencesKeys.AUTH_TOKEN] ?: ""
            token.isNotBlank()
        }

    val hasValidSettings: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val token = preferences[PreferencesKeys.AUTH_TOKEN] ?: ""
            val apiKey = preferences[PreferencesKeys.PORCUPINE_API_KEY] ?: ""
            token.isNotBlank() && apiKey.isNotBlank()
        }

    suspend fun getAuthToken(): String {
        return authToken.first()
    }

    suspend fun saveAuthData(token: String, telegramId: Long, expiresAt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
            preferences[PreferencesKeys.TELEGRAM_ID] = telegramId
            preferences[PreferencesKeys.TOKEN_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }
    }

    suspend fun savePorcupineApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PORCUPINE_API_KEY] = apiKey
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
            preferences.remove(PreferencesKeys.TELEGRAM_ID)
            preferences.remove(PreferencesKeys.TOKEN_EXPIRES_AT)
        }
    }
}
