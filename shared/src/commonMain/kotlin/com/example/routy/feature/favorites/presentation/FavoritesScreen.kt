package com.example.routy.feature.favorites.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.favorites.presentation.state.*

@Composable
fun FavoritesScreen(
    model: FavoritesViewModel,
    navigate: (Destination) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    CollectEffects(model.effects) {
        when (it) {
            is FavoritesEffect.Navigate -> navigate(it.destination)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusPanel(state.network) { model.actionHandler(FavoritesAction.Retry) } }
        if (state.routes.isEmpty() && state.stops.isEmpty() && state.network.network != null) item { EmptyPanel(TextKey.NoFavorites) }
        items(
            state.routes,
            key = { "route:${it.id}" },
        ) { route -> RouteCard(route, { model.actionHandler(FavoritesAction.SelectRoute(route.id)) }) }
        items(
            state.stops,
            key = { "stop:${it.id}" },
        ) { stop -> StopCard(stop, { model.actionHandler(FavoritesAction.SelectStop(stop.id)) }) }
    }
}
