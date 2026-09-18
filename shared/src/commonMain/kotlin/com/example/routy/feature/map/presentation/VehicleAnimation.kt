package com.example.routy.feature.map.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.example.routy.core.transport.domain.GeoPoint
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.core.transport.domain.VehicleState

private const val DefaultStepMillis = 5_000L
private const val MinStepMillis = 700L
private const val EmitIntervalMillis = 32L
private const val MaxAnimatedDegrees = 0.02
private const val NanosPerMilli = 1_000_000L

data class VehicleSegment(
    val from: GeoPoint,
    val to: GeoPoint,
    val startNanos: Long,
    val durationNanos: Long,
) {
    val endNanos: Long get() = startNanos + durationNanos
}

@Composable
fun rememberAnimatedVehicles(state: VehicleState): State<List<Vehicle>> {
    val animated = remember { mutableStateOf(state.vehicles) }
    val segments = remember { mutableStateOf(emptyMap<String, VehicleSegment>()) }
    LaunchedEffect(state.vehicles) {
        var frameNanos = withFrameNanos { it }
        val current = segmentsAt(segments.value, state.vehicles, frameNanos)
        segments.value = current
        animated.value = vehiclesAt(current, state.vehicles, frameNanos)
        var emittedNanos = frameNanos
        while (current.values.any { frameNanos < it.endNanos }) {
            frameNanos = withFrameNanos { it }
            if (frameNanos - emittedNanos < EmitIntervalMillis * NanosPerMilli) continue
            emittedNanos = frameNanos
            animated.value = vehiclesAt(current, state.vehicles, frameNanos)
        }
        animated.value = state.vehicles
    }
    return animated
}

fun segmentsAt(
    previous: Map<String, VehicleSegment>,
    vehicles: List<Vehicle>,
    nowNanos: Long,
): Map<String, VehicleSegment> =
    vehicles.associate { vehicle ->
        val existing = previous[vehicle.id]
        vehicle.id to
            when {
                existing == null || !isAnimatable(existing.to, vehicle.position) ->
                    VehicleSegment(vehicle.position, vehicle.position, nowNanos, 0L)
                existing.to == vehicle.position -> existing
                else ->
                    VehicleSegment(
                        from = positionAt(existing, nowNanos),
                        to = vehicle.position,
                        startNanos = nowNanos,
                        durationNanos = stepNanos(nowNanos - existing.startNanos),
                    )
            }
    }

fun vehiclesAt(
    segments: Map<String, VehicleSegment>,
    vehicles: List<Vehicle>,
    nowNanos: Long,
): List<Vehicle> =
    vehicles.map { vehicle ->
        val segment = segments[vehicle.id]
        if (segment == null) vehicle else vehicle.copy(position = positionAt(segment, nowNanos))
    }

fun positionAt(
    segment: VehicleSegment,
    nowNanos: Long,
): GeoPoint {
    if (segment.durationNanos <= 0L) return segment.to
    val progress = ((nowNanos - segment.startNanos).toDouble() / segment.durationNanos).coerceIn(0.0, 1.0)
    return GeoPoint(
        latitude = segment.from.latitude + (segment.to.latitude - segment.from.latitude) * progress,
        longitude = segment.from.longitude + (segment.to.longitude - segment.from.longitude) * progress,
    )
}

private fun isAnimatable(
    from: GeoPoint,
    to: GeoPoint,
): Boolean {
    val latitudeDelta = to.latitude - from.latitude
    val longitudeDelta = to.longitude - from.longitude
    return latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta <= MaxAnimatedDegrees * MaxAnimatedDegrees
}

private fun stepNanos(sinceLastMoveNanos: Long): Long =
    sinceLastMoveNanos.coerceIn(MinStepMillis * NanosPerMilli, DefaultStepMillis * NanosPerMilli)
