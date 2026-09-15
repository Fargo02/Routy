package com.example.routy.core.transport.data

import com.example.routy.core.transport.domain.*
import kotlinx.serialization.json.*

class TransportParser(private val json: Json = Json { ignoreUnknownKeys = true }) {
    fun network(payload: String): TransportNetwork {
        val dto = json.decodeFromString<DbResponse>(payload).data
        val stops = dto.busStops.map { (key, stop) ->
            require(key.isNotBlank() && key == stop.id)
            BusStop(key, stop.number, LocalizedName(stop.english, stop.georgian, stop.original),
                GeoPoint(stop.latitude, stop.longitude), stop.routes.map { (routeId, service) ->
                    require(routeId.isNotBlank())
                    StopService(routeId, service.status, service.order, service.times.map {
                        requireNotNull(ScheduleTime.parse(it)) { "Invalid schedule time" }
                    }.distinct().sorted())
                })
        }
        val ids = dto.routesNames.keys + dto.routeCoordinatesGrouped.keys + stops.flatMap { it.services.map(StopService::routeId) }
        val routes = ids.map { id ->
            require(id.isNotBlank())
            val metadata = dto.routesNames[id] as? JsonObject
            fun name(key: String) = (metadata?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
            Route(id, LocalizedName(name("RouteNameEN"), name("RouteNameKA"), name("RouteNameGeoGps")),
                (metadata?.get("RouteSortOrder") as? JsonPrimitive)?.intOrNull)
        }.sortedWith(compareBy<Route> { it.sortOrder ?: Int.MAX_VALUE }.thenBy { it.id })
        val geometry = dto.routeCoordinatesGrouped.mapValues { (id, points) ->
            RouteGeometry(id, points.map { GeoPoint(it.lat, it.lon) })
        }
        return TransportNetwork(routes, stops, geometry)
    }

    fun vehicles(payload: String, routeId: String): List<Vehicle> =
        json.decodeFromString<VehiclesResponse>(payload).data.map {
            require(it.name.isNotBlank())
            Vehicle(it.name, routeId, GeoPoint(it.latitude, it.longitude))
        }.also { require(it.map(Vehicle::id).distinct().size == it.size) }
}
