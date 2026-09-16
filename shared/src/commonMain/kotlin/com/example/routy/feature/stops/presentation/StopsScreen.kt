package com.example.routy.feature.stops.presentation

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
import com.example.routy.feature.stops.presentation.state.*

@Composable
fun StopsScreen(
    model: StopsViewModel,
    navigate: (Destination) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is StopsEffect.Navigate -> navigate(it.destination)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SearchField(state.query, strings[TextKey.SearchStops]) { model.actionHandler(StopsAction.Search(it)) } }
        item { StatusPanel(state.network) { model.actionHandler(StopsAction.Retry) } }
        if (state.items.isEmpty() && state.network.network != null) item { EmptyPanel() }
        items(state.items, key = { it.id }) { item -> StopCard(item, { model.actionHandler(StopsAction.Select(item.id)) }) }
    }
}
