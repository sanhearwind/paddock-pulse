package com.f1pulse.app.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.data.repository.StandingsRepository
import com.f1pulse.app.domain.model.ConstructorStanding
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.usecase.GetConstructorStandingsUseCase
import com.f1pulse.app.domain.usecase.GetDriverStandingsUseCase
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

enum class StandingsTab { DRIVERS, CONSTRUCTORS }

data class StandingsUiData(
    val drivers: List<DriverStanding>,
    val constructors: List<ConstructorStanding>,
    val isOffline: Boolean,
)

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val getDriverStandingsUseCase: GetDriverStandingsUseCase,
    private val getConstructorStandingsUseCase: GetConstructorStandingsUseCase,
    private val standingsRepository: StandingsRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = Year.now().value

    val uiState: StateFlow<UiState<StandingsUiData>> = combine(
        getDriverStandingsUseCase(season),
        getConstructorStandingsUseCase(season),
        connectivityMonitor.observe(),
    ) { drivers, constructors, isOnline ->
        UiState.Success(
            StandingsUiData(
                drivers = drivers.sortedBy { it.position },
                constructors = constructors.sortedBy { it.position },
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
        withContext(io) { standingsRepository.refreshStandings(season) }
    }
}
