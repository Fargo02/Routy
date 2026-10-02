package com.example.routy.feature.map.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.feature.map.presentation.vehiclesGeoJson
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

@Composable
@MaplibreComposable
internal fun VehicleLayers(
    vehicles: State<List<Vehicle>>,
    routeColorIndex: (String) -> Int,
    routeLabels: Map<String, String>,
    routeBusPainters: List<Painter>,
    busDirectionPainter: Painter,
    onAttached: (Int) -> Unit,
    onDetached: () -> Unit,
    onVehicleClick: (String) -> Unit,
) {
    val vehiclesByRoute = vehicles.value.groupBy { it.routeId }
    vehiclesByRoute.entries.sortedBy { it.key }.forEach { (routeId, routeVehicles) ->
        key(routeId) {
            DisposableEffect(routeId) {
                onAttached(routeVehicles.size)
                onDispose { onDetached() }
            }
            val routeVehicleSource =
                rememberGeoJsonSource(GeoJsonData.JsonString(vehiclesGeoJson(routeVehicles)))
            val selectedRouteIndex = routeColorIndex(routeId)
            val routeLabel = routeLabels[routeId] ?: routeId
            SymbolLayer(
                "vehicles-$routeId",
                routeVehicleSource,
                iconImage = image(routeBusPainters.getOrNull(selectedRouteIndex) ?: busDirectionPainter),
                iconSize = const(if (selectedRouteIndex >= 0) 0.4f else 0.32f),
                iconRotate = feature["heading"].cast(),
                iconAllowOverlap = const(true),
                textField = format(span(routeLabel)),
                textColor = const(Color(0xFF0F172A)),
                textHaloColor = const(Color.White),
                textHaloWidth = const(1.dp),
                textFont = const(listOf("Noto Sans Bold")),
                textSize = const(12.sp),
                textAllowOverlap = const(true),
                onClick = { features ->
                    features
                        .firstOrNull()
                        ?.properties
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content
                        ?.let(onVehicleClick)
                    ClickResult.Consume
                },
            )
        }
    }
}
