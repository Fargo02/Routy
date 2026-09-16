package com.example.routy.feature.routes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.routes.presentation.state.*

@Composable
fun RoutesScreen(
    model: RoutesViewModel,
    navigate: (Destination) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is RoutesEffect.Navigate -> navigate(it.destination)
        }
    }
    ScreenScaffold { screenPadding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    top = screenPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = screenPadding.calculateBottomPadding() + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SearchField(state.query, strings[TextKey.Search]) { model.actionHandler(RoutesAction.Search(it)) } }
            item { StatusPanel(state.network) { model.actionHandler(RoutesAction.Retry) } }
            if (state.items.isEmpty() && state.stops.isEmpty() && state.network.network != null) item { EmptyPanel() }
            items(
                state.items,
                key = { "route:${it.id}" },
            ) { item -> RouteCard(item, { model.actionHandler(RoutesAction.Select(item.id)) }) }
            items(
                state.stops,
                key = { "stop:${it.id}" },
            ) { stop -> StopCard(stop, { model.actionHandler(RoutesAction.SelectStop(stop.id)) }) }
        }
    }
}
