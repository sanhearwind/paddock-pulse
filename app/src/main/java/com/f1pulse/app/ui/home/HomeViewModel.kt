package com.f1pulse.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.time.RaceTiming
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.core.time.TimeZoneResolver
import com.f1pulse.app.data.prefs.Settings
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.repository.ResultsRepository
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.data.repository.StandingsRepository
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.domain.model.RaceResult
import com.f1pulse.app.domain.usecase.GetDriverStandingsUseCase
import com.f1pulse.app.domain.usecase.GetNextRaceUseCase
import com.f1pulse.app.domain.usecase.GetRaceResultsUseCase
import com.f1pulse.app.domain.usecase.GetRaceScheduleUseCase
import com.f1pulse.app.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiData(
    val season: Int,
    val nextRace: Race?,
    val lastRace: Race?,
    val podium: List<DriverStanding>,
    val lastResultTop3: List<RaceResult>,
    val zone: ZoneId,
    val timeZoneMode: TimeZoneMode,
    val isOffline: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNextRaceUseCase: GetNextRaceUseCase,
    private val getRaceScheduleUseCase: GetRaceScheduleUseCase,
    private val getDriverStandingsUseCase: GetDriverStandingsUseCase,
    private val getRaceResultsUseCase: GetRaceResultsUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val standingsRepository: StandingsRepository,
    private val resultsRepository: ResultsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val timeZoneResolver: TimeZoneResolver,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = Year.now().value

    private val lastRoundFlow: Flow<Int?> = getRaceScheduleUseCase(season).map { list ->
        list.filter { it.isFinished }.maxByOrNull { it.round }?.round
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val lastResultsFlow: Flow<List<RaceResult>> = lastRoundFlow.flatMapLatest { round ->
        if (round != null) getRaceResultsUseCase(season, round, "RACE") else flowOf(emptyList())
    }

    /** Combine 6 sources into a single UI state. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<HomeUiData>> = combine(
        getNextRaceUseCase(season),
        getRaceScheduleUseCase(season),
        getDriverStandingsUseCase(season),
        lastResultsFlow,
        settingsDataStore.flow,
        connectivityMonitor.observe(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val nextRace = values[0] as Race?
        @Suppress("UNCHECKED_CAST")
        val schedule = values[1] as List<Race>
        @Suppress("UNCHECKED_CAST")
        val standings = values[2] as List<DriverStanding>
        @Suppress("UNCHECKED_CAST")
        val lastResults = values[3] as List<RaceResult>
        val settings = values[4] as Settings
        val isOnline = values[5] as Boolean

        val lastRace = schedule.filter { it.isFinished }.maxByOrNull { it.round }
        val circuitId = nextRace?.circuit?.circuitId ?: lastRace?.circuit?.circuitId
        val zone = timeZoneResolver.resolve(settings.timeZoneMode, circuitId)
        val podium = standings.sortedBy { it.position }.take(3)
        val lastTop3 = lastResults.sortedBy { it.position }.take(3)

        val success: UiState<HomeUiData> = UiState.Success(
            HomeUiData(
                season = season,
                nextRace = nextRace,
                lastRace = lastRace,
                podium = podium,
                lastResultTop3 = lastTop3,
                zone = zone,
                timeZoneMode = settings.timeZoneMode,
                isOffline = !isOnline,
            ),
            isOffline = !isOnline,
        )
        success
    }
        .catch { emit(UiState.Error(it.message ?: "加载失败")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        try {
            withContext(io) {
                // 1. Refresh the calendar + standings.
                scheduleRepository.refreshSchedule(season)
                standingsRepository.refreshStandings(season)

                // 2. Refresh results for the most recent finished race (if any).
                val now = TimeFormatter.now().toEpochMilli()
                val schedule = scheduleRepository.observeSchedule(season).first()
                val lastFinished = schedule.lastOrNull { RaceTiming.isFinished(it.raceDateUtc, now) }
                if (lastFinished != null) {
                    resultsRepository.refreshResults(season, lastFinished.round)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Swallow: combine's .catch surfaces errors to UiState.Error
        }
    }
}
