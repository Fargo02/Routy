package com.example.routy.feature.map.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.example.routy.core.transport.domain.BusStop
import com.example.routy.feature.map.presentation.stopsGeoJson
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

@Composable
@MaplibreComposable
internal fun StopLayers(
    stops: List<BusStop>,
    selectedStopId: String?,
    favoriteStopIds: Set<String>,
    strokeColor: Color,
    favoriteStopPainter: Painter,
    onStopClick: (String) -> Unit,
) {
    val stopsJson = remember(stops, favoriteStopIds) { stopsGeoJson(stops, favoriteStopIds) }
    val selectedStopJson = remember(stops, selectedStopId) { stopsGeoJson(stops.filter { it.id == selectedStopId }) }
    val favoriteStopsJson =
        remember(stops, favoriteStopIds) { stopsGeoJson(stops.filter { it.id in favoriteStopIds }) }
    val stopSource = rememberGeoJsonSource(GeoJsonData.JsonString(stopsJson))
    val selectionSource = rememberGeoJsonSource(GeoJsonData.JsonString(selectedStopJson))
    val favoriteStopSource = rememberGeoJsonSource(GeoJsonData.JsonString(favoriteStopsJson))
    CircleLayer(
        "stops",
        stopSource,
        color = const(Color.White),
        radius = const(3.dp),
        strokeColor = const(strokeColor),
        strokeWidth = const(1.5.dp),
        hitPadding = 18.dp,
        onClick = { features ->
            features
                .firstOrNull()
                ?.properties
                ?.get("id")
                ?.jsonPrimitive
                ?.content
                ?.let(onStopClick)
            ClickResult.Consume
        },
    )
    CircleLayer(
        "selected-stop",
        selectionSource,
        color = const(Color.White),
        radius = const(8.dp),
        strokeColor = const(strokeColor),
        strokeWidth = const(2.5.dp),
    )
    if (favoriteStopIds.isNotEmpty()) {
        SymbolLayer(
            "favorite-stops",
            favoriteStopSource,
            iconImage = image(favoriteStopPainter),
            iconSize = const(0.5f),
            iconAllowOverlap = const(true),
            onClick = { features ->
                features
                    .firstOrNull()
                    ?.properties
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.content
                    ?.let(onStopClick)
                ClickResult.Consume
            },
        )
    }
}
