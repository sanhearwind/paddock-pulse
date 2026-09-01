package com.f1pulse.app.ui.driverdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.data.repository.DriversRepository
import com.f1pulse.app.domain.model.Driver
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.usecase.GetDriverDetailUseCase
import com.f1pulse.app.domain.usecase.GetDriverStandingsUseCase
import com.f1pulse.app.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DriverDetailUiData(
    val driver: Driver,
    val standing: DriverStanding?,
    val isOffline: Boolean,
)

@HiltViewModel
class DriverDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDriverDetailUseCase: GetDriverDetailUseCase,
    private val getDriverStandingsUseCase: GetDriverStandingsUseCase,
    private val driversRepository: DriversRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = savedStateHandle.get<Int>("season")
        ?: java.time.Year.now().value
    private val driverId: String = savedStateHandle["driverId"] ?: ""

    val uiState: StateFlow<UiState<DriverDetailUiData>> = combine(
        getDriverDetailUseCase(season, driverId),
        getDriverStandingsUseCase(season),
        connectivityMonitor.observe(),
    ) { driver, standings, isOnline ->
        if (driver == null) {
            UiState.Error("Driver not found")
        } else {
            UiState.Success(
                DriverDetailUiData(
                    driver = driver,
                    standing = standings.firstOrNull { it.driver.driverId == driverId },
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
        withContext(io) { driversRepository.refreshDrivers(season) }
    }
}
