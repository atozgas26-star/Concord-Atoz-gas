package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "concord_session_prefs")

class SessionPreferences(private val context: Context) {

    private val keyLoggedInUserId = longPreferencesKey("logged_in_local_user_id")

    suspend fun getLoggedInUserId(): Long? {
        return context.sessionDataStore.data
            .map { prefs -> prefs[keyLoggedInUserId] }
            .first()
    }

    suspend fun saveLoggedInUserId(userId: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyLoggedInUserId] = userId
        }
    }

    suspend fun clearLoggedInUserId() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyLoggedInUserId)
        }
    }
}
