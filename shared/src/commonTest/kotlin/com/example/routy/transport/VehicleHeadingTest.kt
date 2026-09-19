package com.example.routy.transport

import com.example.routy.core.transport.domain.GeoPoint
import com.example.routy.core.transport.domain.RouteGeometry
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.feature.map.presentation.alignHeadingsToRoutes
import com.example.routy.feature.map.presentation.routeHeading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VehicleHeadingTest {
    private val eastbound =
        listOf(
            GeoPoint(latitude = 41.6400, longitude = 41.6000),
            GeoPoint(latitude = 41.6400, longitude = 41.7000),
            GeoPoint(latitude = 41.6800, longitude = 41.7000),
        )

    private fun bus(
        position: GeoPoint,
        headingDegrees: Float? = null,
        routeId: String = "7",
    ) = Vehicle(id = "v", routeId = routeId, position = position, headingDegrees = headingDegrees)

    @Test fun turnsTheBusAlongTheSegmentItDrivesOn() {
        val heading = routeHeading(GeoPoint(latitude = 41.6401, longitude = 41.6500), eastbound, 74f)
        assertEquals(90f, heading!!, 1e-3f)
    }

    @Test fun followsTheSegmentBackwardsWhenTheBusDrivesAgainstIt() {
        val heading = routeHeading(GeoPoint(latitude = 41.6401, longitude = 41.6500), eastbound, 260f)
        assertEquals(270f, heading!!, 1e-3f)
    }

    @Test fun picksTheNearestSegmentOfTheRoute() {
        val heading = routeHeading(GeoPoint(latitude = 41.6600, longitude = 41.7001), eastbound, 5f)
        assertEquals(0f, heading!!, 1e-3f)
    }

    @Test fun usesTheRouteDirectionForABusThatHasNotMovedYet() {
        val heading = routeHeading(GeoPoint(latitude = 41.6401, longitude = 41.6500), eastbound, null)
        assertEquals(90f, heading!!, 1e-3f)
    }

    @Test fun keepsTheReportedHeadingOfABusStandingAwayFromTheRoute() {
        assertEquals(33f, routeHeading(GeoPoint(latitude = 41.6200, longitude = 41.6500), eastbound, 33f))
        assertNull(routeHeading(GeoPoint(latitude = 41.6200, longitude = 41.6500), eastbound, null))
    }

    @Test fun leavesBusesOfRoutesWithoutGeometryAlone() {
        val vehicle = bus(GeoPoint(latitude = 41.6401, longitude = 41.6500), headingDegrees = 74f)
        assertEquals(listOf(vehicle), alignHeadingsToRoutes(listOf(vehicle), emptyMap()))
    }

    @Test fun alignsEveryBusOfTheRoute() {
        val aligned =
            alignHeadingsToRoutes(
                listOf(
                    bus(GeoPoint(latitude = 41.6401, longitude = 41.6500), headingDegrees = 74f),
                    bus(GeoPoint(latitude = 41.6399, longitude = 41.6800), headingDegrees = 260f),
                ),
                mapOf("7" to RouteGeometry("7", eastbound)),
            )
        assertEquals(listOf(90f, 270f), aligned.map { it.headingDegrees })
    }
}
