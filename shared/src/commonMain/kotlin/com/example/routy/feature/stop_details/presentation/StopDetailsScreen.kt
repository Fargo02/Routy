package com.example.routy.feature.stop_details.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.PlatformBackHandler
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.stop_details.presentation.state.*
import com.example.routy.feature.stop_details.domain.nearestScheduledDeparture
import com.example.routy.feature.stop_details.domain.minutesUntilNextScheduledDeparture
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailsScreen(
    model: StopDetailsViewModel,
    navigate: (Destination) -> Unit,
    message: suspend (String) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var selectedRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    CollectEffects(model.effects) {
        when (it) {
            is StopDetailsEffect.Navigate -> navigate(it.destination)
            is StopDetailsEffect.Error -> message(strings.error(it.error))
        }
    }
    val routeDetails =
        selectedRouteId?.let { routeId ->
            remember(state.network.network, routeId) {
                state.network.network?.let { GetRouteDetailsUseCase()(it, routeId) }
            }
        }
    PlatformBackHandler(enabled = selectedRouteId != null) { selectedRouteId = null }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusPanel(state.network) { model.actionHandler(StopDetailsAction.Retry) } }
        val details = state.details
        if (routeDetails != null) {
            item {
                TextButton(onClick = { selectedRouteId = null }) {
                    RoutyIcon(Glyph.Back, strings[TextKey.Back])
                    Spacer(Modifier.width(8.dp))
                    Text(strings[TextKey.Back])
                }
                Text(
                    routeDetails.route.name.resolve(strings.language, routeDetails.route.id),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
            routeDetails.groups.forEach { (group, stops) ->
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
                        onClick = { selectedRouteId = null },
                        subtitle =
                            routeStop.service.times
                                .take(4)
                                .joinToString(" • ")
                                .ifEmpty { strings[TextKey.NoSchedule] },
                    )
                }
            }
        } else if (details != null) {
            item {
                Text(details.stop.name.resolve(strings.language, details.stop.id), style = MaterialTheme.typography.headlineSmall)
                FilledTonalButton({ model.actionHandler(StopDetailsAction.ToggleFavorite) }) {
                    RoutyIcon(if (state.favorite) Glyph.StarFilled else Glyph.Star)
                    Spacer(Modifier.width(8.dp))
                    Text(strings[if (state.favorite) TextKey.Saved else TextKey.Save])
                }
            }
            item {
                Text(strings[TextKey.Schedule], style = MaterialTheme.typography.titleLarge)
                Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall)
            }
            details.routes
                .map { route ->
                    val times =
                        details.stop.services
                            .filter { it.routeId == route.id }
                            .flatMap { it.times }
                    route to nearestScheduledDeparture(times)
                }
                .sortedBy { (route, _) ->
                    val times =
                        details.stop.services
                            .filter { it.routeId == route.id }
                            .flatMap { it.times }
                    minutesUntilNextScheduledDeparture(times) ?: Int.MAX_VALUE
                }
                .forEach { (route, nextDeparture) ->
                    item {
                        RouteCard(
                            route = route,
                            onClick = { selectedRouteId = route.id },
                            subtitle = strings[TextKey.Departure],
                            trailingLabel = nextDeparture?.toString(),
                        )
                    }
                }
        } else if (state.network.network != null) {
            item { EmptyPanel() }
        }
    }
}
