package com.f1pulse.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.domain.usecase.GetRaceScheduleUseCase
import com.f1pulse.app.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Year
import javax.inject.Inject

data class CalendarUiData(
    val upcoming: List<Race>,
    val past: List<Race>,
    val timeZoneMode: TimeZoneMode,
    val isOffline: Boolean,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getRaceScheduleUseCase: GetRaceScheduleUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val settingsDataStore: SettingsDataStore,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = Year.now().value
    val uiState: StateFlow<UiState<CalendarUiData>> = combine(
        getRaceScheduleUseCase(season),
        settingsDataStore.flow,
        connectivityMonitor.observe(),
    ) { races, settings, isOnline ->
        UiState.Success(
            CalendarUiData(
                upcoming = races.filter { !it.isFinished },
                past = races.filter { it.isFinished }.sortedByDescending { it.round },
                timeZoneMode = settings.timeZoneMode,
                isOffline = !isOnline,
            ),
            isOffline = !isOnline,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        withContext(io) { scheduleRepository.refreshSchedule(season) }
    }
}
