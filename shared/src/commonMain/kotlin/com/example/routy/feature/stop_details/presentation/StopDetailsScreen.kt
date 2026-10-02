package com.example.routy.feature.stop_details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.EmptyPanel
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.RouteCard
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.designsystem.StatusPanel
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.ObserveAsEvents
import com.example.routy.feature.stop_details.domain.minutesUntilNextScheduledDeparture
import com.example.routy.feature.stop_details.domain.nearestScheduledDeparture
import com.example.routy.feature.stop_details.presentation.state.StopDetailsAction
import com.example.routy.feature.stop_details.presentation.state.StopDetailsEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailsScreen(
    model: StopDetailsViewModel,
    showRouteOnMap: (String) -> Unit,
    message: suspend (String) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    ObserveAsEvents(model.effects, key1 = strings) {
        when (it) {
            is StopDetailsEffect.ShowRouteOnMap -> showRouteOnMap(it.id)
            is StopDetailsEffect.Error -> message(strings.error(it.error))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatusPanel(state.network) { model.actionHandler(StopDetailsAction.Retry) } }
        val details = state.details
        if (details != null) {
            item {
                Text(
                    details.stop.name.resolve(strings.language, details.stop.id),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(16.dp))
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
                }.sortedBy { (route, _) ->
                    val times =
                        details.stop.services
                            .filter { it.routeId == route.id }
                            .flatMap { it.times }
                    minutesUntilNextScheduledDeparture(times) ?: Int.MAX_VALUE
                }.forEach { (route, nextDeparture) ->
                    item {
                        RouteCard(
                            route = route,
                            onClick = { model.actionHandler(StopDetailsAction.SelectRoute(route.id)) },
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
