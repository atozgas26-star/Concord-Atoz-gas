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
import com.example.audio.StartupSoundStyle
import com.example.data.model.AutoLogoutMode
import com.example.data.model.LowStockAlertState
import com.example.data.model.ThemePreset
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AdminCredentialsState(
    val username: String = "",
    val passwordHash: String = "",
    val recoveryEmail: String = ""
) {
    val isConfigured: Boolean
        get() = username.isNotBlank() && passwordHash.isNotBlank()
}

data class BiometricUnlockState(
    val armed: Boolean = false,
    val userId: Long = 0L,
    val role: UserRole? = null,
    val displayName: String = "",
    val passwordRequiredAfterLogout: Boolean = false
) {
    val isReadyForBiometric: Boolean
        get() = armed && !passwordRequiredAfterLogout && role != null
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "concord_session_prefs")

class SessionPreferences(private val context: Context) {

    private val keyLoggedInUserId = longPreferencesKey("logged_in_local_user_id")
    private val keyAutoLoginEnabled = booleanPreferencesKey("auto_login_enabled")
    private val keyAutoLogoutMode = stringPreferencesKey("auto_logout_mode")
    private val keyThemePreset = stringPreferencesKey("theme_preset")
    private val keySoundEnabled = booleanPreferencesKey("sound_enabled")
    private val keyStartupSoundStyle = stringPreferencesKey("startup_sound_style")
    private val keyAutoDriveBackupEnabled = booleanPreferencesKey("auto_drive_backup_enabled")
    private val keyLastDriveBackupAtMs = longPreferencesKey("last_drive_backup_at_ms")
    private val keyLastDriveFileId = stringPreferencesKey("last_drive_file_id")
    private val keyLowStockThreshold = intPreferencesKey("low_stock_threshold")
    private val keyDarkModeEnabled = booleanPreferencesKey("dark_mode_enabled")
    private val keyAdminUsername = stringPreferencesKey("admin_login_username")
    private val keyAdminPasswordHash = stringPreferencesKey("admin_password_hash")
    private val keyAdminRecoveryEmail = stringPreferencesKey("admin_recovery_email")
    private val keyOwnerViewPasswordHash = stringPreferencesKey("owner_view_password_hash")
    private val keyLoggedInRole = stringPreferencesKey("logged_in_user_role")
    private val keyBiometricArmed = booleanPreferencesKey("biometric_unlock_armed")
    private val keyBiometricUserId = longPreferencesKey("biometric_unlock_user_id")
    private val keyBiometricRole = stringPreferencesKey("biometric_unlock_role")
    private val keyBiometricDisplayName = stringPreferencesKey("biometric_unlock_display_name")
    private val keyBiometricRequiresPasswordAfterLogout = booleanPreferencesKey("biometric_requires_password_after_logout")

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

    val startupSoundStyleFlow: Flow<StartupSoundStyle> = context.sessionDataStore.data
        .map { prefs ->
            val raw = prefs[keyStartupSoundStyle]
            StartupSoundStyle.entries.firstOrNull { it.name == raw } ?: StartupSoundStyle.ROYAL_FANFARE
        }

    val autoDriveBackupEnabledFlow: Flow<Boolean> = context.sessionDataStore.data
        .map { prefs -> prefs[keyAutoDriveBackupEnabled] ?: false }

    val lastDriveBackupAtMsFlow: Flow<Long> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLastDriveBackupAtMs] ?: 0L }

    val lastDriveFileIdFlow: Flow<String> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLastDriveFileId] ?: "" }

    val lowStockThresholdFlow: Flow<Int> = context.sessionDataStore.data
        .map { prefs -> prefs[keyLowStockThreshold] ?: LowStockAlertState.DEFAULT_LOW_STOCK_THRESHOLD }

    val darkModeEnabledFlow: Flow<Boolean> = context.sessionDataStore.data
        .map { prefs -> prefs[keyDarkModeEnabled] ?: false }

    val adminCredentialsFlow: Flow<AdminCredentialsState> = context.sessionDataStore.data
        .map { prefs ->
            AdminCredentialsState(
                username = prefs[keyAdminUsername].orEmpty(),
                passwordHash = prefs[keyAdminPasswordHash].orEmpty(),
                recoveryEmail = prefs[keyAdminRecoveryEmail].orEmpty()
            )
        }

    val ownerViewPasswordHashFlow: Flow<String> = context.sessionDataStore.data
        .map { prefs -> prefs[keyOwnerViewPasswordHash].orEmpty() }

    val ownerViewConfiguredFlow: Flow<Boolean> = ownerViewPasswordHashFlow
        .map { it.isNotBlank() }

    val biometricUnlockStateFlow: Flow<BiometricUnlockState> = context.sessionDataStore.data
        .map { prefs ->
            val rawRole = prefs[keyBiometricRole]
            BiometricUnlockState(
                armed = prefs[keyBiometricArmed] ?: false,
                userId = prefs[keyBiometricUserId] ?: 0L,
                role = UserRole.entries.firstOrNull { it.name == rawRole },
                displayName = prefs[keyBiometricDisplayName].orEmpty(),
                passwordRequiredAfterLogout = prefs[keyBiometricRequiresPasswordAfterLogout] ?: false
            )
        }

    suspend fun getLoggedInUserId(): Long? {
        return context.sessionDataStore.data
            .map { prefs -> prefs[keyLoggedInUserId] }
            .first()
    }

    suspend fun getLoggedInRole(): UserRole? {
        val raw = context.sessionDataStore.data
            .map { prefs -> prefs[keyLoggedInRole] }
            .first()
        return UserRole.entries.firstOrNull { it.name == raw }
    }

    suspend fun saveLoggedInUserId(userId: Long, role: UserRole? = null) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyLoggedInUserId] = userId
            if (role != null) {
                prefs[keyLoggedInRole] = role.name
            } else {
                prefs.remove(keyLoggedInRole)
            }
        }
    }

    suspend fun clearLoggedInUserId() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyLoggedInUserId)
            prefs.remove(keyLoggedInRole)
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

    suspend fun setStartupSoundStyle(style: StartupSoundStyle) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyStartupSoundStyle] = style.name
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

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyDarkModeEnabled] = enabled
        }
    }

    suspend fun saveAdminCredentials(
        username: String,
        passwordHash: String,
        recoveryEmail: String
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAdminUsername] = username.trim()
            prefs[keyAdminPasswordHash] = passwordHash
            prefs[keyAdminRecoveryEmail] = recoveryEmail.trim().lowercase()
        }
    }

    suspend fun updateAdminPasswordHash(passwordHash: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAdminPasswordHash] = passwordHash
        }
    }

    suspend fun updateAdminUsername(username: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAdminUsername] = username.trim()
        }
    }

    suspend fun updateAdminRecoveryEmail(recoveryEmail: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAdminRecoveryEmail] = recoveryEmail.trim().lowercase()
        }
    }

    suspend fun clearAdminCredentials() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyAdminUsername)
            prefs.remove(keyAdminPasswordHash)
            prefs.remove(keyAdminRecoveryEmail)
        }
    }

    suspend fun saveOwnerViewPasswordHash(passwordHash: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyOwnerViewPasswordHash] = passwordHash
        }
    }

    suspend fun clearOwnerViewPasswordHash() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyOwnerViewPasswordHash)
        }
    }

    suspend fun armBiometricUnlock(
        userId: Long,
        role: UserRole,
        displayName: String
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyBiometricArmed] = true
            prefs[keyBiometricUserId] = userId
            prefs[keyBiometricRole] = role.name
            prefs[keyBiometricDisplayName] = displayName.trim().take(100)
            prefs[keyBiometricRequiresPasswordAfterLogout] = false
        }
    }

    suspend fun disarmBiometricUnlockOnLogout() {
        context.sessionDataStore.edit { prefs ->
            prefs[keyBiometricArmed] = false
            prefs[keyBiometricRequiresPasswordAfterLogout] = true
            prefs.remove(keyLoggedInUserId)
            prefs.remove(keyLoggedInRole)
        }
    }

    suspend fun clearBiometricUnlockState() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyBiometricArmed)
            prefs.remove(keyBiometricUserId)
            prefs.remove(keyBiometricRole)
            prefs.remove(keyBiometricDisplayName)
            prefs.remove(keyBiometricRequiresPasswordAfterLogout)
        }
    }
}
