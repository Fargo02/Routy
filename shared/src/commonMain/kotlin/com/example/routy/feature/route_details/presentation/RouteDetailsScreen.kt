package com.example.routy.feature.route_details.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.route_details.presentation.state.*

@Composable
fun RouteDetailsScreen(
    model: RouteDetailsViewModel,
    navigate: (Destination) -> Unit,
    showMap: (String) -> Unit,
    message: suspend (String) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val vehicles by model.vehicles.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is RouteDetailsEffect.Navigate -> navigate(it.destination)
            is RouteDetailsEffect.ShowMap -> showMap(it.routeId)
            is RouteDetailsEffect.Error -> message(strings.error(it.error))
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusPanel(state.network) { model.actionHandler(RouteDetailsAction.Retry) } }
        val details = state.details
        if (details != null) {
            item {
                Text(details.route.name.resolve(strings.language, details.route.id), style = MaterialTheme.typography.headlineLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button({ model.actionHandler(RouteDetailsAction.ShowMap) }) { Text(strings[TextKey.ShowMap]) }
                    FilledTonalButton({ model.actionHandler(RouteDetailsAction.ToggleFavorite) }) {
                        RoutyIcon(Glyph.Star)
                        Spacer(Modifier.width(8.dp))
                        Text(strings[if (state.favorite) TextKey.Saved else TextKey.Save])
                    }
                }
            }
            item {
                Text(strings[TextKey.Live], style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        vehicles.isLoading -> strings[TextKey.Loading]
                        vehicles.isStale -> strings[TextKey.Stale]
                        vehicles.vehicles.isEmpty() -> strings[TextKey.NoBuses]
                        else -> vehicles.vehicles.joinToString(" • ") { it.id }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
            details.groups.forEach { (group, stops) ->
                item {
                    Text(
                        if (group ==
                            null
                        ) {
                            strings[TextKey.Unspecified]
                        } else {
                            "${strings[TextKey.Group]} $group"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(stops, key = { "$group:${it.stop.id}" }) { item ->
                    StopCard(
                        item.stop,
                        { model.actionHandler(RouteDetailsAction.SelectStop(item.stop.id)) },
                        item.service.times
                            .take(4)
                            .joinToString(" • ")
                            .ifEmpty { strings[TextKey.NoSchedule] },
                    )
                }
            }
        } else if (state.network.network != null) {
            item { EmptyPanel() }
        }
    }
}
