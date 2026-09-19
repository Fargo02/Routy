package com.example.routy.feature.map.presentation

import com.example.routy.core.transport.domain.GeoPoint
import com.example.routy.core.transport.domain.RouteGeometry
import com.example.routy.core.transport.domain.Vehicle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos

private const val MaxSnapDegrees = 0.0012
private const val DegreesPerRadian = 180.0 / PI

fun alignHeadingsToRoutes(
    vehicles: List<Vehicle>,
    geometries: Map<String, RouteGeometry>,
): List<Vehicle> =
    vehicles.map { vehicle ->
        val points = geometries[vehicle.routeId]?.points ?: return@map vehicle
        val heading = routeHeading(vehicle.position, points, vehicle.headingDegrees)
        if (heading == vehicle.headingDegrees) vehicle else vehicle.copy(headingDegrees = heading)
    }

fun routeHeading(
    position: GeoPoint,
    points: List<GeoPoint>,
    observedDegrees: Float?,
): Float? {
    val longitudeScale = cos(position.latitude / DegreesPerRadian)
    var nearestDistance = MaxSnapDegrees * MaxSnapDegrees
    var forward: Double? = null
    for (index in 0 until points.lastIndex) {
        val from = points[index]
        val to = points[index + 1]
        val segmentX = (to.longitude - from.longitude) * longitudeScale
        val segmentY = to.latitude - from.latitude
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared == 0.0) continue
        val offsetX = (position.longitude - from.longitude) * longitudeScale
        val offsetY = position.latitude - from.latitude
        val progress = ((offsetX * segmentX + offsetY * segmentY) / lengthSquared).coerceIn(0.0, 1.0)
        val awayX = offsetX - segmentX * progress
        val awayY = offsetY - segmentY * progress
        val distance = awayX * awayX + awayY * awayY
        if (distance >= nearestDistance) continue
        nearestDistance = distance
        forward = (atan2(segmentX, segmentY) * DegreesPerRadian + 360.0) % 360.0
    }
    val along = forward ?: return observedDegrees
    val observed = observedDegrees?.toDouble() ?: return along.toFloat()
    val backward = (along + 180.0) % 360.0
    return (if (angleBetween(along, observed) <= angleBetween(backward, observed)) along else backward).toFloat()
}

private fun angleBetween(
    first: Double,
    second: Double,
): Double {
    val difference = abs(first - second) % 360.0
    return if (difference > 180.0) 360.0 - difference else difference
}
