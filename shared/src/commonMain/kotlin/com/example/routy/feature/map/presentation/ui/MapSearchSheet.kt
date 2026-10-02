package com.example.routy.feature.map.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.RouteCard
import com.example.routy.core.designsystem.SearchEmptyState
import com.example.routy.core.designsystem.SearchField
import com.example.routy.core.designsystem.StopCard
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapState
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapSearchSheet(
    state: MapState,
    onAction: (MapAction) -> Unit,
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf(state.search.query) }
    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
        keyboard?.show()
        sheetState.expand()
    }
    DisposableEffect(Unit) {
        onDispose { keyboard?.hide() }
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    val hasCurrentSearchResults = state.search.query == query
    ModalBottomSheet(
        onDismissRequest = { onAction(MapAction.CloseSearch) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
    ) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchField(
                    value = query,
                    placeholder = strings[TextKey.Search],
                    onChange = {
                        query = it
                        onAction(MapAction.SearchQueryChanged(it))
                    },
                    modifier =
                        Modifier
                            .padding(horizontal = 20.dp)
                            .focusRequester(focusRequester),
                    onClear = {
                        query = ""
                        onAction(MapAction.ClearSearch)
                    },
                )
            }
            if (query.isBlank()) {
                item {
                    Text(
                        strings[TextKey.QuickSelect],
                        Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier.fillParentMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.search.quickRoutes, key = { it.id }) { route ->
                            val selected = route.id in state.selectedRouteIds
                            MapRouteChip(
                                route = route,
                                colorIndex = state.routeColorIndex(route.id),
                                selected = selected,
                                enabled = selected || state.selectedRouteIds.size < 7,
                                favorite = route.id in state.favoriteRouteIds,
                                onClick = { onAction(MapAction.SelectSearchRoute(route.id)) },
                            )
                        }
                    }
                }
            }
            if (hasCurrentSearchResults && state.search.routes.isNotEmpty()) {
                item {
                    Text(
                        strings[TextKey.Routes],
                        Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.search.routes, key = { "route:${it.id}" }) { route ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        RouteCard(
                            route = route,
                            stopCount = state.search.routeStopCounts[route.id],
                            onClick = { onAction(MapAction.SelectSearchRoute(route.id)) },
                        )
                    }
                }
            }
            if (hasCurrentSearchResults && state.search.stops.isNotEmpty()) {
                item {
                    Text(
                        strings[TextKey.Stops],
                        Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (hasCurrentSearchResults) {
                items(
                    state.search.stops,
                    key = { "stop:${it.id}" },
                ) { stop ->
                    val routeNames =
                        remember(stop, state.network.network, strings.language) {
                            val routesById =
                                state.network.network
                                    ?.routes
                                    .orEmpty()
                                    .associateBy { it.id }
                            stop.services
                                .map { it.routeId }
                                .distinct()
                                .mapNotNull { routesById[it] }
                                .joinToString(" • ") { it.name.resolve(strings.language, it.id) }
                                .ifBlank { null }
                        }
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        StopCard(
                            stop = stop,
                            subtitle = routeNames,
                            onClick = { onAction(MapAction.SelectSearchStop(stop.id)) },
                        )
                    }
                }
            }
            if (hasCurrentSearchResults &&
                query.isNotBlank() &&
                state.search.routes.isEmpty() &&
                state.search.stops.isEmpty()
            ) {
                item { SearchEmptyState(Modifier.padding(horizontal = 20.dp)) }
            }
        }
    }
}
