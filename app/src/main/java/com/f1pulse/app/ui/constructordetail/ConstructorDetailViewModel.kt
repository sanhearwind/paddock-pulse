package com.f1pulse.app.ui.constructordetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.data.repository.ConstructorsRepository
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ConstructorDetailUiData(
    val standing: ConstructorStanding,
    val drivers: List<DriverStanding>,
    val isOffline: Boolean,
)

@HiltViewModel
class ConstructorDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConstructorStandingsUseCase: GetConstructorStandingsUseCase,
    private val getDriverStandingsUseCase: GetDriverStandingsUseCase,
    private val constructorsRepository: ConstructorsRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    @com.f1pulse.app.core.dispatchers.Dispatcher(com.f1pulse.app.core.dispatchers.DispatcherType.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val season: Int = savedStateHandle.get<Int>("season")
        ?: java.time.Year.now().value
    private val constructorId: String = savedStateHandle["constructorId"] ?: ""

    val uiState: StateFlow<UiState<ConstructorDetailUiData>> = combine(
        getConstructorStandingsUseCase(season),
        getDriverStandingsUseCase(season),
        connectivityMonitor.observe(),
    ) { constructorStandings, driverStandings, isOnline ->
        val standing = constructorStandings.firstOrNull { it.team.constructorId == constructorId }
        if (standing == null) {
            UiState.Error("Constructor not found")
        } else {
            val teamDrivers = driverStandings
                .filter { it.team?.constructorId == constructorId }
                .sortedByDescending { it.points }
            UiState.Success(
                ConstructorDetailUiData(
                    standing = standing,
                    drivers = teamDrivers,
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
        withContext(io) { constructorsRepository.refreshConstructors(season) }
    }
}
