package com.f1pulse.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.domain.usecase.GetRaceScheduleUseCase
import com.f1pulse.app.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HistoryUiData(
    val selectedSeason: Int,
    val races: List<Race>,
    val timeZoneMode: TimeZoneMode,
    val isOffline: Boolean,
    val isRefreshing: Boolean,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getRaceScheduleUseCase: GetRaceScheduleUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val settingsDataStore: SettingsDataStore,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val currentYear = HistorySeasonPolicy.currentYear()
    private val initialSeason = HistorySeasonPolicy.defaultSeason(currentYear)
    private val _selectedSeason = MutableStateFlow(initialSeason)
    val selectedSeason: StateFlow<Int> = _selectedSeason
    val availableSeasons: List<Int> = HistorySeasonPolicy.availableSeasons(currentYear)
    private val refreshStates = MutableStateFlow<Map<Int, Resource<Unit>>>(
        mapOf(initialSeason to Resource.Loading),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val racesFlow: Flow<List<Race>> = _selectedSeason
        .flatMapLatest(getRaceScheduleUseCase::invoke)

    val uiState: StateFlow<UiState<HistoryUiData>> = combine(
        _selectedSeason,
        racesFlow,
        settingsDataStore.flow,
        connectivityMonitor.observe(),
        refreshStates,
    ) { season, races, settings, isOnline, states ->
        val refreshState = states[season]
        val completedRaces = races
            .filter { it.isFinished }
            .sortedByDescending { it.round }

        when {
            completedRaces.isEmpty() && refreshState is Resource.Loading -> UiState.Loading
            completedRaces.isEmpty() && refreshState is Resource.Error -> {
                UiState.Error(refreshState.message)
            }
            else -> UiState.Success(
                HistoryUiData(
                    selectedSeason = season,
                    races = completedRaces,
                    timeZoneMode = settings.timeZoneMode,
                    isOffline = !isOnline,
                    isRefreshing = refreshState is Resource.Loading,
                ),
                isOffline = !isOnline,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading,
    )

    init {
        refreshSeason(initialSeason)
    }

    fun selectSeason(season: Int) {
        val bounded = HistorySeasonPolicy.clamp(season, currentYear)
        if (_selectedSeason.value == bounded) return
        refreshStates.value = refreshStates.value + (bounded to Resource.Loading)
        _selectedSeason.value = bounded
        refreshSeason(bounded)
    }

    fun refresh() {
        refreshSeason(_selectedSeason.value, force = true)
    }

    private fun refreshSeason(season: Int, force: Boolean = false) {
        refreshStates.value = refreshStates.value + (season to Resource.Loading)
        viewModelScope.launch {
            val result = try {
                withContext(io) { scheduleRepository.refreshSchedule(season, force) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Resource.Error(t.message ?: "Failed to load historical season")
            }
            refreshStates.value = refreshStates.value + (season to result)
        }
    }
}
