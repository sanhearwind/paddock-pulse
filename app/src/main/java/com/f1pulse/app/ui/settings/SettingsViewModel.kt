package com.f1pulse.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.data.prefs.Settings
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.prefs.ThemeMode
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.data.repository.DriversRepository
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.data.repository.StandingsRepository
import android.content.Context
import com.f1pulse.app.widget.WidgetInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val scheduleRepository: ScheduleRepository,
    private val standingsRepository: StandingsRepository,
    private val driversRepository: DriversRepository,
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsDataStore.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshFailed = MutableStateFlow(false)
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsDataStore.setThemeMode(mode)
    }

    fun setTimeZoneMode(mode: TimeZoneMode) = viewModelScope.launch {
        settingsDataStore.setTimeZoneMode(mode)
    }

    fun setSpoilerShield(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setSpoilerShield(enabled)
    }

    fun setWidgetRefreshMinutes(minutes: Int) = viewModelScope.launch {
        val clamped = minutes.coerceIn(15, 240)
        settingsDataStore.setWidgetRefreshMinutes(clamped)
        WidgetInitializer.schedule(context, clamped)
    }

    fun dismissRefreshError() {
        _refreshFailed.value = false
    }

    fun refreshNow() = viewModelScope.launch {
        _refreshFailed.value = false
        if (_refreshing.value) return@launch
        _refreshing.value = true
        try {
            val season = Year.now().value
            val results = listOf(
                scheduleRepository.refreshSchedule(season, force = true),
                standingsRepository.refreshStandings(season, force = true),
                driversRepository.refreshDrivers(season, force = true),
            )
            _refreshFailed.value = results.any { it is Resource.Error }
        } finally {
            _refreshing.value = false
        }
    }
}
