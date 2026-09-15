package com.example.routy.core.transport.domain

class ObserveTransportUseCase(
    private val repository: TransportRepository,
) {
    val state = repository.state

    fun foreground() = repository.observeNetwork()

    suspend fun refresh() = repository.refresh()
}

class ObserveRouteVehiclesUseCase(
    private val repository: VehicleRepository,
) {
    operator fun invoke(routeId: String) = repository.observeVehicles(routeId)
}
