package com.example.routy.feature.map.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.EmptyPanel
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.NetworkIssueBadge
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.designsystem.StopCard
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.transport.domain.VehicleState
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapRouteInfoState
import com.example.routy.feature.map.presentation.state.MapState
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.stop_details.domain.scheduledFrequencyMinutes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RouteInfoSheet(
    state: MapState,
    routeInfo: MapRouteInfoState,
    vehicles: VehicleState,
    sheetState: SheetState,
    onAction: (MapAction) -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var swipeDistance by remember { mutableStateOf(0f) }
    val routeIds = state.selectedRouteIds
    val activeRouteId = routeInfo.routeId
    val activeRouteIndex = routeIds.indexOf(activeRouteId)
    val activeVehicles = vehicles.vehicles.filter { it.routeId == activeRouteId }
    val routeSchedule =
        remember(state.network.network, activeRouteId) {
            state.network.network?.let { GetRouteDetailsUseCase()(it, activeRouteId) }
        }
    val routeFrequency =
        routeSchedule
            ?.groups
            ?.values
            ?.asSequence()
            ?.flatten()
            ?.mapNotNull { scheduledFrequencyMinutes(it.service.times) }
            ?.firstOrNull()

    fun showRoute(id: String?) {
        id?.let { onAction(MapAction.ShowRouteInfo(it)) }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(MapAction.CloseRouteInfo) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
    ) {
        if (routeInfo.isScheduleVisible) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(onClick = { onAction(MapAction.CloseRouteSchedule) }) {
                        RoutyIcon(Glyph.Back, strings[TextKey.Back])
                        Spacer(Modifier.width(8.dp))
                        Text(strings[TextKey.Back])
                    }
                    routeSchedule?.let { details ->
                        Text(
                            details.route.name.resolve(strings.language, details.route.id),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
                routeSchedule?.groups?.forEach { (group, stops) ->
                    item {
                        Text(
                            stops.lastOrNull()?.stop?.let { stop -> stop.name.resolve(strings.language, stop.id) }
                                ?: strings[TextKey.Unspecified],
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(stops, key = { "$group:${it.stop.id}" }) { routeStop ->
                        StopCard(
                            stop = routeStop.stop,
                            onClick = { onAction(MapAction.ShowStopOnMap(routeStop.stop.id)) },
                            subtitle =
                                routeStop.service.times
                                    .take(4)
                                    .joinToString(" • ")
                                    .ifEmpty { strings[TextKey.NoSchedule] },
                        )
                    }
                } ?: item { EmptyPanel() }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(routeIds, activeRouteId) {
                        detectHorizontalDragGestures(
                            onDragStart = { swipeDistance = 0f },
                            onHorizontalDrag = { _, dragAmount -> swipeDistance += dragAmount },
                            onDragEnd = {
                                when {
                                    swipeDistance <= -48f -> showRoute(routeIds.getOrNull(activeRouteIndex + 1))
                                    swipeDistance >= 48f -> showRoute(routeIds.getOrNull(activeRouteIndex - 1))
                                }
                            },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedContent(
                    targetState = activeRouteId,
                    transitionSpec = {
                        val forward = routeIds.indexOf(targetState) > routeIds.indexOf(initialState)
                        if (forward) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "route-info-transition",
                ) { displayedRouteId ->
                    val displayedIndex = routeIds.indexOf(displayedRouteId)
                    state.network.network
                        ?.routes
                        ?.firstOrNull { it.id == displayedRouteId }
                        ?.let { route ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (routeIds.size > 1) 12.dp else 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (routeIds.size > 1) {
                                        IconButton(
                                            onClick = { showRoute(routeIds.getOrNull(displayedIndex - 1)) },
                                            enabled = displayedIndex > 0,
                                        ) { RoutyIcon(Glyph.Chevron, modifier = Modifier.graphicsLayer(rotationZ = 180f)) }
                                    }
                                    Text(
                                        route.name.resolve(strings.language, route.id),
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    if (routeIds.size > 1) {
                                        IconButton(
                                            onClick = { showRoute(routeIds.getOrNull(displayedIndex + 1)) },
                                            enabled = displayedIndex in 0 until routeIds.lastIndex,
                                        ) { RoutyIcon(Glyph.Chevron) }
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { onAction(MapAction.ToggleRouteFavorite(route.id)) },
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                ) {
                                    RoutyIcon(
                                        if (route.id in state.favoriteRouteIds) Glyph.StarFilled else Glyph.Star,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings[if (route.id in state.favoriteRouteIds) TextKey.Saved else TextKey.Save])
                                }
                            }
                        }
                }
                routeFrequency?.let { frequency ->
                    Text(
                        strings.runsEvery(frequency),
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings[TextKey.Live], style = MaterialTheme.typography.titleMedium)
                    if (vehicles.isStale) NetworkIssueBadge(size = 28.dp)
                }
                Text(
                    when {
                        vehicles.isLoading -> strings[TextKey.Loading]
                        activeVehicles.isEmpty() -> strings[TextKey.NoBuses]
                        else -> strings.vehiclesCount(activeVehicles.size)
                    },
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (activeVehicles.isNotEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(activeVehicles, key = { it.id }) { vehicle ->
                            AssistChip(
                                onClick = { onAction(MapAction.SelectVehicle(vehicle.id)) },
                                label = { Text("${strings[TextKey.Vehicle]} ${vehicle.id}") },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = {
                        onAction(MapAction.OpenRouteSchedule)
                        scope.launch { sheetState.expand() }
                    },
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
                ) { Text(strings[TextKey.Details]) }
            }
        }
    }
}
