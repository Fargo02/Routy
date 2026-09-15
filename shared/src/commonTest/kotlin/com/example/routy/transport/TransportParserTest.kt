package com.example.routy.transport

import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.data.mapTransportError
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.CancellationException
import kotlin.test.*

class TransportParserTest {
    private val parser = TransportParser()

    @Test fun parsesDocumentedCasingAndPreservesUnicode() {
        val network = parser.network(fixture)
        assertEquals(GeoPoint(41.63, 41.64), network.stops.single().position)
        assertEquals(
            "ბათუმი",
            network.stops
                .single()
                .name
                .resolve(Language.Georgian, "fallback"),
        )
        assertEquals(
            "2A",
            network.routes
                .single()
                .name
                .resolve(Language.English, "fallback"),
        )
        assertEquals(GeoPoint(41.62, 41.61), network.geometries["r"]!!.points.first())
        assertEquals(
            listOf(ScheduleTime(7, 13), ScheduleTime(8, 0)),
            network.stops
                .single()
                .services
                .single()
                .times,
        )
        assertEquals(
            77,
            network.stops
                .single()
                .services
                .single()
                .sourceGroup,
        )
    }

    @Test fun parsesLiveCasingWithoutInterpretingStatus() {
        val vehicles = parser.vehicles("""{"data":[{"Lat":41.6,"Lon":41.7,"Name":"bus","Status":{"future":true}}]}""", "r")
        assertEquals(GeoPoint(41.6, 41.7), vehicles.single().position)
    }

    @Test fun rejectsMalformedPayloadRatherThanReplacingCache() {
        for (payload in listOf("{}", "not json", fixture.replace("41.63", "141.63"), fixture.replace("07:13", "25:90"))) {
            assertFails { parser.network(payload) }
        }
    }

    @Test fun unknownRouteMetadataDoesNotBreakStopDatabase() {
        val network = parser.network(fixture.replace("{\"RouteNameEN\":\"2A\",\"RouteSortOrder\":25}", "[1,2]"))
        assertEquals(
            "r",
            network.routes
                .single()
                .name
                .resolve(Language.English, "r"),
        )
    }

    @Test fun fallbackSkipsBlankNames() {
        assertEquals("ბათუმი", LocalizedName(" ", "ბათუმი", "original").resolve(Language.English, "s"))
        assertEquals("s", LocalizedName(null, null, null).resolve(Language.Georgian, "s"))
        assertNull(ScheduleTime.parse("7:13"))
    }

    @Test fun cancellationIsNeverConvertedToFailure() {
        assertFailsWith<CancellationException> { mapTransportError(CancellationException()) }
        assertEquals(AppError.InvalidData, mapTransportError(IllegalArgumentException()))
    }

    companion object {
        val fixture =
            """
            {"data":{
              "busStops":{"s":{
                "BusStopIdGeoGps":"s","BusStopNameKA":"ბათუმი",
                "BusStopLatitude":41.63,"BusStopLongitude":41.64,
                "routes":{"r":{"Status":77,"Order":2,"times":["08:00","07:13"]}}
              }},
              "routesNames":{"r":{"RouteNameEN":"2A","RouteSortOrder":25}},
              "routeCoordinatesGrouped":{"r":[{"lat":41.62,"lon":41.61},{"lat":41.64,"lon":41.65}]},
              "unknown":true
            }}
            """.trimIndent()
    }
}
