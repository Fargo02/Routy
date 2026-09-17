package com.example.routy.feature.map.presentation

import com.example.routy.core.transport.domain.*
import kotlinx.serialization.json.*

const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private fun point(
    id: String,
    position: GeoPoint,
    favorite: Boolean = false,
) = buildJsonObject {
    put("type", "Feature")
    put("id", id)
    putJsonObject("properties") {
        put("id", id)
        put("favorite", favorite)
    }
    putJsonObject("geometry") {
        put("type", "Point")
        putJsonArray("coordinates") {
            add(position.longitude)
            add(position.latitude)
        }
    }
}

private fun collection(features: List<JsonObject>) =
    buildJsonObject {
        put("type", "FeatureCollection")
        put("features", JsonArray(features))
    }.toString()

fun stopsGeoJson(
    stops: List<BusStop>,
    favoriteStopIds: Set<String> = emptySet(),
): String = collection(stops.map { point(it.id, it.position, it.id in favoriteStopIds) })

fun vehiclesGeoJson(vehicles: List<Vehicle>): String =
    collection(
        vehicles.map { vehicle ->
            buildJsonObject {
                put("type", "Feature")
                put("id", vehicle.id)
                putJsonObject("properties") {
                    put("id", vehicle.id)
                    put("routeLabel", vehicle.routeId)
                    put("heading", vehicle.headingDegrees ?: 0f)
                }
                putJsonObject("geometry") {
                    put("type", "Point")
                    putJsonArray("coordinates") {
                        add(vehicle.position.longitude)
                        add(vehicle.position.latitude)
                    }
                }
            }
        },
    )

fun routeGeoJson(geometries: List<RouteGeometry>): String =
    collection(
        geometries
            .filter { it.points.size >= 2 }
            .map { geometry ->
                buildJsonObject {
                    put("type", "Feature")
                    put("id", geometry.routeId)
                    putJsonObject("properties") { }
                    putJsonObject("geometry") {
                        put("type", "LineString")
                        put(
                            "coordinates",
                            JsonArray(geometry.points.map { JsonArray(listOf(JsonPrimitive(it.longitude), JsonPrimitive(it.latitude))) }),
                        )
                    }
                }
            },
    )

/** The part of a route between a live bus position and the selected stop. */
fun trackingRouteGeoJson(
    vehicle: Vehicle,
    stop: BusStop,
    geometry: RouteGeometry?,
): String {
    val routePoints = geometry?.points.orEmpty()
    val segment =
        if (routePoints.size < 2) {
            emptyList()
        } else {
            val busIndex = routePoints.indices.minBy { index -> squaredDistance(routePoints[index], vehicle.position) }
            val stopIndex = routePoints.indices.minBy { index -> squaredDistance(routePoints[index], stop.position) }
            routePoints.subList(minOf(busIndex, stopIndex), maxOf(busIndex, stopIndex) + 1).let {
                if (busIndex <= stopIndex) it else it.reversed()
            }
        }
    return routeGeoJson(listOf(RouteGeometry("tracking", listOf(vehicle.position) + segment + stop.position)))
}

private fun squaredDistance(first: GeoPoint, second: GeoPoint): Double =
    (first.latitude - second.latitude) * (first.latitude - second.latitude) +
        (first.longitude - second.longitude) * (first.longitude - second.longitude)
