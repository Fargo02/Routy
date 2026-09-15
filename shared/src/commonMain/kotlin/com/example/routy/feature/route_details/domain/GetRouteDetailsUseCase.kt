package com.example.routy.feature.route_details.domain

import com.example.routy.core.transport.domain.*

data class RouteStop(
    val stop: BusStop,
    val service: StopService,
)

data class RouteDetails(
    val route: Route,
    val groups: Map<Int?, List<RouteStop>>,
    val geometry: RouteGeometry?,
)

class GetRouteDetailsUseCase {
    operator fun invoke(
        network: TransportNetwork,
        routeId: String,
    ): RouteDetails? {
        val route = network.routes.firstOrNull { it.id == routeId } ?: return null
        val groups =
            network.stops
                .flatMap { stop ->
                    stop.services.filter { it.routeId == routeId }.map { RouteStop(stop, it) }
                }.groupBy { it.service.sourceGroup }
                .mapValues { (_, stops) ->
                    stops.sortedWith(compareBy<RouteStop> { it.service.order ?: Int.MAX_VALUE }.thenBy { it.stop.id })
                }
        return RouteDetails(route, groups, network.geometries[routeId])
    }
}
