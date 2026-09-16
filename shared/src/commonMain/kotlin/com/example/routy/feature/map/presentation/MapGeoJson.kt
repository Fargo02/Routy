package com.example.routy.feature.map.presentation

import com.example.routy.core.transport.domain.*
import kotlinx.serialization.json.*

const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private fun point(
    id: String,
    position: GeoPoint,
) = buildJsonObject {
    put("type", "Feature")
    put("id", id)
    putJsonObject("properties") { put("id", id) }
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

fun stopsGeoJson(stops: List<BusStop>): String = collection(stops.map { point(it.id, it.position) })

fun vehiclesGeoJson(vehicles: List<Vehicle>): String = collection(vehicles.map { point(it.id, it.position) })

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
