package com.example.routy.transport

import com.example.routy.core.transport.domain.GeoPoint
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.feature.map.presentation.segmentsAt
import com.example.routy.feature.map.presentation.vehiclesAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class VehicleAnimationTest {
    private val second = 1_000_000_000L
    private val start = GeoPoint(latitude = 41.6461, longitude = 41.6367)
    private val next = GeoPoint(latitude = 41.6481, longitude = 41.6387)

    private fun bus(
        position: GeoPoint,
        id: String = "v",
        routeId: String = "r",
    ) = Vehicle(id = id, routeId = routeId, position = position, headingDegrees = 90f)

    private fun reported(
        position: GeoPoint,
        atNanos: Long,
        previousPosition: GeoPoint = start,
    ) = segmentsAt(segmentsAt(emptyMap(), listOf(bus(previousPosition)), 0L), listOf(bus(position)), atNanos)

    @Test fun movesVehicleAlongTheSegmentBetweenTwoUpdates() {
        val segments = reported(next, 5 * second)
        val half = vehiclesAt(segments, listOf(bus(next)), 7 * second + second / 2).single()
        assertEquals(41.6471, half.position.latitude, 1e-9)
        assertEquals(41.6377, half.position.longitude, 1e-9)
        assertEquals(90f, half.headingDegrees)
    }

    @Test fun holdsTheVehicleAtBothEndsOfTheSegment() {
        val segments = reported(next, 5 * second)
        assertEquals(start, vehiclesAt(segments, listOf(bus(next)), 5 * second).single().position)
        assertEquals(next, vehiclesAt(segments, listOf(bus(next)), 20 * second).single().position)
    }

    @Test fun keepsRunningSegmentsOfVehiclesThatDidNotMove() {
        val segments = reported(next, 5 * second)
        val updated = segmentsAt(segments, listOf(bus(next)), 6 * second)
        assertSame(segments.getValue("v"), updated.getValue("v"))
    }

    @Test fun showsVehiclesWithoutAPreviousPositionWhereTheyReallyAre() {
        val segments = segmentsAt(emptyMap(), listOf(bus(next)), 5 * second)
        assertEquals(next, vehiclesAt(segments, listOf(bus(next)), 5 * second).single().position)
    }

    @Test fun snapsInsteadOfFlyingAcrossTheMapOnAJump() {
        val jumped = GeoPoint(latitude = 42.0, longitude = 42.0)
        val segments = reported(jumped, 5 * second)
        assertEquals(jumped, vehiclesAt(segments, listOf(bus(jumped)), 5 * second).single().position)
    }

    @Test fun animatesEachVehicleOverItsOwnUpdateInterval() {
        val slowStart = GeoPoint(latitude = 41.7461, longitude = 41.7367)
        val slowNext = GeoPoint(latitude = 41.7481, longitude = 41.7387)
        val first = segmentsAt(emptyMap(), listOf(bus(start), bus(slowStart, id = "w", routeId = "s")), 0L)
        val moved = segmentsAt(first, listOf(bus(next), bus(slowStart, id = "w", routeId = "s")), 2 * second)
        val later = segmentsAt(moved, listOf(bus(next), bus(slowNext, id = "w", routeId = "s")), 6 * second)
        assertEquals(2 * second, later.getValue("v").durationNanos)
        assertEquals(5 * second, later.getValue("w").durationNanos)
    }
}
