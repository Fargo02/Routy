package com.example.routy.feature.map.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.routy.core.transport.domain.RouteGeometry
import com.example.routy.feature.map.presentation.routeGeoJson
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

@Composable
@MaplibreComposable
internal fun RouteLayers(
    geometries: List<RouteGeometry>,
    routeColor: (String) -> Color,
    outlineColor: Color,
) {
    // Route lines must be ready before MapLibre redraws its tiles after a zoom gesture.
    val sourceOptions = remember { GeoJsonOptions(synchronousUpdate = true) }
    geometries.forEach { geometry ->
        val source =
            rememberGeoJsonSource(
                GeoJsonData.JsonString(routeGeoJson(listOf(geometry))),
                options = sourceOptions,
            )
        LineLayer(
            "route-outline-${geometry.routeId}",
            source,
            color = const(outlineColor),
            width = const(5.dp),
        )
        LineLayer(
            "route-${geometry.routeId}",
            source,
            color = const(routeColor(geometry.routeId)),
            width = const(3.dp),
        )
    }
}
