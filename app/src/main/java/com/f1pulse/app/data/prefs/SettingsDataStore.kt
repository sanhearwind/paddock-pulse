package com.f1pulse.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.f1pulse.app.core.time.TimeZoneMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class Settings(
    val timeZoneMode: TimeZoneMode = TimeZoneMode.DEVICE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val spoilerShield: Boolean = true,
    val widgetRefreshMinutes: Int = 30,
)

private val Context.dataStore by preferencesDataStore(name = "f1_pulse_prefs")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val TIME_ZONE_MODE = stringPreferencesKey("time_zone_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SPOILER_SHIELD = booleanPreferencesKey("spoiler_shield")
        val WIDGET_REFRESH_MINUTES = intPreferencesKey("widget_refresh_minutes")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            timeZoneMode = p[Keys.TIME_ZONE_MODE]
                ?.let { runCatching { TimeZoneMode.valueOf(it) }.getOrNull() }
                ?: TimeZoneMode.DEVICE,
            themeMode = p[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            spoilerShield = p[Keys.SPOILER_SHIELD] ?: false,
            widgetRefreshMinutes = p[Keys.WIDGET_REFRESH_MINUTES] ?: 30,
        )
    }

    suspend fun setTimeZoneMode(mode: TimeZoneMode) =
        context.dataStore.edit { it[Keys.TIME_ZONE_MODE] = mode.name }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setSpoilerShield(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SPOILER_SHIELD] = enabled }

    suspend fun setWidgetRefreshMinutes(min: Int) =
        context.dataStore.edit { it[Keys.WIDGET_REFRESH_MINUTES] = min.coerceIn(15, 240) }
}
