package com.f1pulse.app.ui.racedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.time.CurrentSession
import com.f1pulse.app.core.time.SessionSelector
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.core.time.TimeZoneResolver
import com.f1pulse.app.core.time.minuteTicker
import com.f1pulse.app.data.prefs.Settings
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.repository.ResultsRepository
import com.f1pulse.app.data.repository.SessionDetailRepository
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.domain.model.RaceResult
import com.f1pulse.app.domain.usecase.GetRaceDetailUseCase
import com.f1pulse.app.domain.usecase.GetRaceResultsUseCase
import com.f1pulse.app.domain.usecase.GetRoundSessionResultsUseCase
import com.f1pulse.app.domain.usecase.ObserveWeatherUseCase
import com.f1pulse.app.domain.usecase.SessionWithResults
import com.f1pulse.app.domain.model.Weather
import com.f1pulse.app.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class RaceDetailUiData(
    val race: Race,
    val raceResults: List<RaceResult>,
    val qualiResults: List<RaceResult>,
    val sprintResults: List<RaceResult>,
    /** The session to highlight right now, with its phase. Null before any session exists. */
    val currentSession: CurrentSession<SessionWithResults>?,
    /** Every classified session of the weekend, chronological — drives the per-session tabs. */
    val sessions: List<SessionWithResults>,
    val weather: Weather?,
    val zone: ZoneId,
    val timeZoneMode: TimeZoneMode,
    val spoilerShield: Boolean,
    val isOffline: Boolean,
)

@HiltViewModel
class RaceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRaceDetailUseCase: GetRaceDetailUseCase,
    private val getRaceResultsUseCase: GetRaceResultsUseCase,
    private val getRoundSessionResultsUseCase: GetRoundSessionResultsUseCase,
    private val observeWeatherUseCase: ObserveWeatherUseCase,
    private val resultsRepository: ResultsRepository,
    private val sessionDetailRepository: SessionDetailRepository,
    private val settingsDataStore: SettingsDataStore,
    private val timeZoneResolver: TimeZoneResolver,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = savedStateHandle.get<Int>("season")
        ?: java.time.Year.now().value
    private val round: Int = savedStateHandle.get<Int>("round") ?: 1

    private val sessionsFlow: Flow<List<SessionWithResults>> =
        getRoundSessionResultsUseCase(season, round)

    /**
     * The session the page should surface, re-evaluated every minute so the weekend rolls
     * forward on its own: a running session while it runs, then the session that just
     * finished — results and all — until the next one starts.
     */
    private val currentSessionFlow: Flow<CurrentSession<SessionWithResults>?> = combine(
        sessionsFlow,
        minuteTicker(),
    ) { sessions, nowMillis ->
        SessionSelector.current(sessions, Instant.ofEpochMilli(nowMillis)) { it.window }
    }

    /**
     * Weather follows the current session rather than always the race.
     *
     * This flow and [refresh] must agree on which session key they use — reading one key
     * while writing another is what previously left the weather card permanently empty.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val weatherFlow: Flow<Weather?> = currentSessionFlow
        .map { it?.session?.detail?.sessionKey }
        .distinctUntilChanged()
        .flatMapLatest { key -> if (key != null) observeWeatherUseCase(key) else flowOf(null) }

    private val officialResultsFlow: Flow<Triple<List<RaceResult>, List<RaceResult>, List<RaceResult>>> =
        combine(
            getRaceResultsUseCase(season, round, "RACE"),
            getRaceResultsUseCase(season, round, "QUALIFYING"),
            getRaceResultsUseCase(season, round, "SPRINT"),
        ) { race, quali, sprint -> Triple(race, quali, sprint) }

    private val prefsFlow: Flow<Pair<Settings, Boolean>> =
        combine(settingsDataStore.flow, connectivityMonitor.observe()) { settings, online ->
            settings to online
        }

    private val weekendFlow: Flow<Pair<CurrentSession<SessionWithResults>?, List<SessionWithResults>>> =
        combine(currentSessionFlow, sessionsFlow) { current, all -> current to all }

    val uiState: StateFlow<UiState<RaceDetailUiData>> = combine(
        getRaceDetailUseCase(season, round),
        officialResultsFlow,
        weekendFlow,
        weatherFlow,
        prefsFlow,
    ) { race, official, weekend, weather, prefs ->
        if (race == null) {
            UiState.Error("Race not found")
        } else {
            val (raceResults, qualiResults, sprintResults) = official
            val (current, sessions) = weekend
            val (settings, isOnline) = prefs
            val zone = timeZoneResolver.resolve(settings.timeZoneMode, race.circuit.circuitId)
            UiState.Success(
                RaceDetailUiData(
                    race = race,
                    raceResults = raceResults.sortedBy { it.position },
                    qualiResults = qualiResults.sortedBy { it.position },
                    sprintResults = sprintResults.sortedBy { it.position },
                    currentSession = current,
                    sessions = sessions,
                    weather = weather,
                    zone = zone,
                    timeZoneMode = settings.timeZoneMode,
                    spoilerShield = settings.spoilerShield,
                    isOffline = !isOnline,
                ),
                isOffline = !isOnline,
            )
        }
    }.flowOn(io).stateIn(
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
                // Official Jolpica classifications (race / qualifying / sprint).
                resultsRepository.refreshResults(season, round)

                val now = Instant.now()
                val sessions = sessionsFlow.first()

                // Only sessions that have finished can have a published result, and OpenF1 holds
                // them back until ~30min after the end. Requesting the rest would burn the free
                // tier's 30 requests/minute budget on guaranteed 404s.
                sessions.filter { !now.isBefore(it.window.end) }
                    .forEach { sessionDetailRepository.refreshSessionResult(it.detail.sessionKey) }

                // Weather for whatever session the UI is showing — same key the UI observes.
                SessionSelector.current(sessions, now) { it.window }
                    ?.takeIf { !now.isBefore(it.session.window.start) }
                    ?.let { sessionDetailRepository.refreshWeather(it.session.detail.sessionKey) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Repositories already fold failures into Resource.Error; cached data stays on screen.
        }
    }
}
