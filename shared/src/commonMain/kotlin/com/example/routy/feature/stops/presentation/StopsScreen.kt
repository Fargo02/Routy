package com.example.routy.feature.stops.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination

@Composable
fun StopsScreen(
    model: StopsViewModel,
    navigate: (Destination) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is StopsEffect.Navigate -> navigate(it.destination)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SearchField(state.query, strings[TextKey.SearchStops]) { model.accept(StopsIntent.Search(it)) } }
        item { StatusPanel(state.network) { model.accept(StopsIntent.Retry) } }
        if (state.items.isEmpty() && state.network.network != null) item { EmptyPanel() }
        items(state.items, key = { it.id }) { item -> StopCard(item, { model.accept(StopsIntent.Select(item.id)) }) }
    }
}
