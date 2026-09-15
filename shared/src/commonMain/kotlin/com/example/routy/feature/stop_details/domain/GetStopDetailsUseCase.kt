package com.example.routy.feature.stop_details.domain

import com.example.routy.core.transport.domain.*

data class StopDetails(
    val stop: BusStop,
    val routes: List<Route>,
)

class GetStopDetailsUseCase {
    operator fun invoke(
        network: TransportNetwork,
        stopId: String,
    ): StopDetails? {
        val stop = network.stops.firstOrNull { it.id == stopId } ?: return null
        val routeIds = stop.services.map { it.routeId }.toSet()
        return StopDetails(stop, network.routes.filter { it.id in routeIds })
    }
}
