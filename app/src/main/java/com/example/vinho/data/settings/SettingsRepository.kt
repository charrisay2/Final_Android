package com.example.vinho.data.settings

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

/** Cấu hình nhắc nhở hằng ngày (lưu bằng DataStore, không cần mạng). */
data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
) {
    /** "20:00" */
    val label: String get() = "%02d:%02d".format(hour, minute)
}

interface SettingsRepository {
    val reminder: Flow<ReminderSettings>

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTime(hour: Int, minute: Int)
}

// DataStore phải là 1 instance duy nhất cho cả app -> khai báo ở top-level, không tạo trong class
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    override val reminder: Flow<ReminderSettings> =
        context.settingsDataStore.data.map { prefs ->
            ReminderSettings(
                enabled = prefs[Keys.REMINDER_ENABLED] ?: false,
                hour = prefs[Keys.REMINDER_HOUR] ?: 20,
                minute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            )
        }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
