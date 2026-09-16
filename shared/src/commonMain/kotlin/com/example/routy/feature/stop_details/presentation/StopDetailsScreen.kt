package com.example.routy.feature.stop_details.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.stop_details.presentation.state.*

@Composable
fun StopDetailsScreen(
    model: StopDetailsViewModel,
    navigate: (Destination) -> Unit,
    message: suspend (String) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is StopDetailsEffect.Navigate -> navigate(it.destination)
            is StopDetailsEffect.Error -> message(strings.error(it.error))
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusPanel(state.network) { model.actionHandler(StopDetailsAction.Retry) } }
        val details = state.details
        if (details != null) {
            item {
                Text(details.stop.name.resolve(strings.language, details.stop.id), style = MaterialTheme.typography.headlineSmall)
                FilledTonalButton({ model.actionHandler(StopDetailsAction.ToggleFavorite) }) {
                    RoutyIcon(Glyph.Star)
                    Spacer(Modifier.width(8.dp))
                    Text(strings[if (state.favorite) TextKey.Saved else TextKey.Save])
                }
            }
            item {
                Text(strings[TextKey.Schedule], style = MaterialTheme.typography.titleLarge)
                Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall)
            }
            details.routes.forEach { route ->
                item { RouteCard(route, { model.actionHandler(StopDetailsAction.SelectRoute(route.id)) }) }
                details.stop.services.filter { it.routeId == route.id }.forEach { service ->
                    item {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Text(
                                service.times.joinToString("   ").ifEmpty {
                                    strings[TextKey.NoSchedule]
                                },
                                Modifier.fillMaxWidth().padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        } else if (state.network.network != null) {
            item { EmptyPanel() }
        }
    }
}
