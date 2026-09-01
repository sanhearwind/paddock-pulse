package com.f1pulse.app.domain.usecase

import com.f1pulse.app.data.repository.CircuitRepository
import com.f1pulse.app.domain.model.CircuitMeta
import javax.inject.Inject

class GetCircuitUseCase @Inject constructor(
    private val circuitRepository: CircuitRepository,
) {
    suspend operator fun invoke(circuitId: String): CircuitMeta? =
        circuitRepository.getCircuitMeta(circuitId)
}
