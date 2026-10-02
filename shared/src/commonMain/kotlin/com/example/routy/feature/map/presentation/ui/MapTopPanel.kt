package com.example.routy.feature.map.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapState

@Composable
internal fun MapTopPanel(
    state: MapState,
    isMapUnavailable: Boolean,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val routes =
        state.network.network
            ?.routes
            .orEmpty()
            .let { routes -> routes.filter { it.id in state.favoriteRouteIds } + routes.filterNot { it.id in state.favoriteRouteIds } }
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = { onAction(MapAction.OpenSearch) },
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RoutyIcon(Glyph.Search)
                Text(
                    strings[TextKey.Search],
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(routes, key = { it.id }) { route ->
                val selected = route.id in state.selectedRouteIds
                MapRouteChip(
                    route = route,
                    colorIndex = state.routeColorIndex(route.id),
                    selected = selected,
                    enabled = selected || state.selectedRouteIds.size < 7,
                    favorite = route.id in state.favoriteRouteIds,
                    onClick = { onAction(MapAction.SelectRoute(route.id)) },
                    showBorder = false,
                )
            }
        }
        if (isMapUnavailable) {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
            ) { Text(strings[TextKey.MapUnavailable], Modifier.padding(16.dp)) }
        }
    }
}
