package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AutoLogoutMode
import com.example.data.model.LowStockAlertState
import com.example.data.model.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "concord_session_prefs")

class SessionPreferences(private val context: Context) {

    private val keyLoggedInUserId = longPreferencesKey("logged_in_local_user_id")
    private val keyAutoLoginEnabled = booleanPreferencesKey("auto_login_enabled")
    private val keyAutoLogoutMode = stringPreferencesKey("auto_logout_mode")
    private val keyThemePreset = stringPreferencesKey("theme_preset")
    private val keySoundEnabled = booleanPreferencesKey("sound_enabled")
    private val keyAutoDriveBackupEnabled = booleanPreferencesKey("auto_drive_backup_enabled")
    private val keyLastDriveBackupAtMs = longPreferencesKey("last_drive_backup_at_ms")
    private val keyLastDriveFileId = stringPreferencesKey("last_drive_file_id")
    private val keyLowStockThreshold = intPreferencesKey("low_stock_threshold")

    val autoLoginEnabledFlow: Flow<Boolean> = context.sessionDataStore.data
        .map { prefs -> prefs[keyAutoLoginEnabled] ?: true }

    val autoLogoutModeFlow: Flow<AutoLogoutMode> = context.sessionDataStore.data
        .map { prefs ->
            val raw = prefs[keyAutoLogoutMode]
            AutoLogoutMode.entries.firstOrNull { it.name == raw } ?: AutoLogoutMode.OFF
        }

    val themePresetFlow: Flow<ThemePreset> = context.sessionDataStore.data
        .map { prefs ->
            val raw = prefs[keyThemePreset]
            ThemePreset.entries.firstOrNull { it.name == raw } ?: ThemePreset.EMERALD_CORPORATE
        }

    val soundEnabledFlow: Flow<Boolean> = context.sessionDataStore.data
        .map { prefs -> prefs[keySoundEnabled] ?: true }

    val autoDriveBackupEnabledFlow: Flow<Boolean> = context.sessionDataStore.data
        .map { prefs -> prefs[keyAutoDriveBackupEnabled] ?: false }

    val lastDriveBackupAtMsFlow: Flow<Long> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLastDriveBackupAtMs] ?: 0L }

    val lastDriveFileIdFlow: Flow<String> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLastDriveFileId] ?: "" }

    val lowStockThresholdFlow: Flow<Int> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLowStockThreshold] ?: LowStockAlertState.DEFAULT_LOW_STOCK_THRESHOLD }

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

    suspend fun setAutoLoginEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAutoLoginEnabled] = enabled
        }
    }

    suspend fun setAutoLogoutMode(mode: AutoLogoutMode) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAutoLogoutMode] = mode.name
        }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyThemePreset] = preset.name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[keySoundEnabled] = enabled
        }
    }

    suspend fun setAutoDriveBackupEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAutoDriveBackupEnabled] = enabled
        }
    }

    suspend fun saveDriveBackupReceipt(timestampMs: Long, fileId: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyLastDriveBackupAtMs] = timestampMs
            prefs[keyLastDriveFileId] = fileId
        }
    }

    suspend fun setLowStockThreshold(threshold: Int) {
        val safeValue = threshold.coerceIn(0, 999)
        context.sessionDataStore.edit { prefs ->
            prefs[keyLowStockThreshold] = safeValue
        }
    }
}
