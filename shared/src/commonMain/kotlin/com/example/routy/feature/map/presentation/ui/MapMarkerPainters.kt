package com.example.routy.feature.map.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import routy.shared.generated.resources.Res
import routy.shared.generated.resources.azure_bus
import routy.shared.generated.resources.blue_bus
import routy.shared.generated.resources.bus
import routy.shared.generated.resources.dark_blue_bus
import routy.shared.generated.resources.favorite_stop
import routy.shared.generated.resources.green_bus
import routy.shared.generated.resources.orage_bus
import routy.shared.generated.resources.pink_bus
import routy.shared.generated.resources.red_bus

internal class MapMarkerPainters(
    val bus: Painter,
    val favoriteStop: Painter,
    val routeBuses: List<Painter>,
)

@Composable
internal fun rememberMapMarkerPainters() =
    MapMarkerPainters(
        bus = painterResource(Res.drawable.bus),
        favoriteStop = painterResource(Res.drawable.favorite_stop),
        routeBuses =
            listOf(
                painterResource(Res.drawable.blue_bus),
                painterResource(Res.drawable.red_bus),
                painterResource(Res.drawable.green_bus),
                painterResource(Res.drawable.orage_bus),
                painterResource(Res.drawable.azure_bus),
                painterResource(Res.drawable.pink_bus),
                painterResource(Res.drawable.dark_blue_bus),
            ),
    )
