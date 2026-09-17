package com.example.routy.feature.favorites.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.feature.favorites.presentation.state.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.stop_details.domain.GetStopDetailsUseCase
import com.example.routy.feature.stop_details.domain.nearestScheduledDeparture
import com.example.routy.feature.stop_details.domain.scheduledFrequencyMinutes
import com.example.routy.feature.stop_details.domain.minutesUntilNextScheduledDeparture
import kotlinx.coroutines.launch

private const val RouteSheetPrefix = "route:"
private const val StopSheetPrefix = "stop:"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    model: FavoritesViewModel,
    showStopOnMap: (String) -> Unit,
    showRouteOnMap: (String) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var sheetContent by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRouteId = sheetContent?.removePrefix(RouteSheetPrefix)?.takeIf { sheetContent?.startsWith(RouteSheetPrefix) == true }
    val selectedStopId = sheetContent?.removePrefix(StopSheetPrefix)?.takeIf { sheetContent?.startsWith(StopSheetPrefix) == true }
    var stopRemovalConfirmationId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScreenScaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings[TextKey.Favorites], style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            )
        },
    ) { screenPadding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    top = screenPadding.calculateTopPadding(),
                    end = 20.dp,
                    bottom = screenPadding.calculateBottomPadding() + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().heightIn(min = 12.dp)) {
                    StatusPanel(state.network) { model.actionHandler(FavoritesAction.Retry) }
                }
            }
            if (state.routes.isEmpty() && state.stops.isEmpty() && state.network.network != null) item { EmptyPanel(TextKey.NoFavorites) }
            items(
                state.routes,
                key = { "route:${it.id}" },
            ) { route ->
                FavoriteSwipeToDismiss(
                    onDismiss = { model.actionHandler(FavoritesAction.RemoveRoute(route.id)) },
                ) {
                    val frequency =
                        state.network.network
                            ?.let { GetRouteDetailsUseCase()(it, route.id) }
                            ?.groups
                            ?.values
                            ?.asSequence()
                            ?.flatten()
                            ?.mapNotNull { scheduledFrequencyMinutes(it.service.times) }
                            ?.firstOrNull()
                    RouteCard(
                        route = route,
                        onClick = {
                            sheetContent = "$RouteSheetPrefix${route.id}"
                        },
                        subtitle = frequency?.let(strings::runsEvery),
                    )
                }
            }
            items(
                state.stops,
                key = { "stop:${it.id}" },
            ) { stop ->
                FavoriteSwipeToDismiss(
                    onDismiss = { model.actionHandler(FavoritesAction.RemoveStop(stop.id)) },
                ) {
                    StopCard(
                        stop,
                        onClick = {
                            sheetContent = "$StopSheetPrefix${stop.id}"
                        },
                        subtitle = strings.vehiclesCount(stop.services.map { it.routeId }.distinct().size),
                    )
                }
            }
        }
    }
    if (sheetContent != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetContent = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = detailsSheetState,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
        ) {
            selectedRouteId?.let { routeId ->
        val details =
            remember(state.network.network, routeId) {
                state.network.network?.let { GetRouteDetailsUseCase()(it, routeId) }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (details != null) {
                    item {
                        Text(
                            details.route.name.resolve(strings.language, details.route.id),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            sheetContent = null
                            showRouteOnMap(routeId)
                        }) { Text(strings[TextKey.ShowMap]) }
                    }
                    item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
                    details.groups.forEach { (group, stops) ->
                        item {
                            Text(
                                stops.lastOrNull()?.stop?.let { stop -> stop.name.resolve(strings.language, stop.id) }
                                    ?: strings[TextKey.Unspecified],
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(stops, key = { "$group:${it.stop.id}" }) { item ->
                            StopCard(
                                item.stop,
                                onClick = {
                                    sheetContent = "$StopSheetPrefix${item.stop.id}"
                                },
                                subtitle =
                                    item.service.times
                                        .take(4)
                                        .joinToString(" • ")
                                        .ifEmpty { strings[TextKey.NoSchedule] },
                            )
                        }
                    }
                } else {
                    item { EmptyPanel() }
                }
            }
            }
            selectedStopId?.let { stopId ->
        val details =
            remember(state.network.network, stopId) {
                state.network.network?.let { GetStopDetailsUseCase()(it, stopId) }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (details != null) {
                    item {
                        Text(
                            details.stop.name.resolve(strings.language, details.stop.id),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    sheetContent = null
                                    showStopOnMap(stopId)
                                },
                            ) { Text(strings[TextKey.ShowMap]) }
                            if (state.stops.any { it.id == stopId }) {
                                FilledTonalButton(
                                    onClick = {
                                        sheetContent = null
                                        stopRemovalConfirmationId = stopId
                                    },
                                ) { Text(strings[TextKey.Remove]) }
                            }
                        }
                    }
                    item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
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
                                route,
                                onClick = {
                                    sheetContent = "$RouteSheetPrefix${route.id}"
                                },
                                subtitle = strings[TextKey.Departure],
                                trailingLabel = nextDeparture?.toString(),
                            )
                        }
                    }
                } else {
                    item { EmptyPanel() }
                }
            }
        }
    }
    }
    stopRemovalConfirmationId?.let { stopId ->
        AlertDialog(
            onDismissRequest = { stopRemovalConfirmationId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings[TextKey.RemoveFavoriteTitle]) },
            text = { Text(strings[TextKey.RemoveFavoriteBody]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        stopRemovalConfirmationId = null
                        model.actionHandler(FavoritesAction.RemoveStop(stopId))
                    },
                ) { Text(strings[TextKey.Remove]) }
            },
            dismissButton = {
                TextButton(onClick = { stopRemovalConfirmationId = null }) {
                    Text(strings[TextKey.Close])
                }
            },
        )
    }
}

@Composable
private fun FavoriteSwipeToDismiss(
    onDismiss: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val strings = LocalStrings.current
    var removalConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) removalConfirmationVisible = true
                false
            },
            positionalThreshold = { distance -> distance * 0.7f },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isDismissInProgress = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
            if (isDismissInProgress) {
                val alignment =
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment,
                ) {
                    Text(strings[TextKey.Remove], color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = content,
    )
    if (removalConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { removalConfirmationVisible = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings[TextKey.RemoveFavoriteTitle]) },
            text = { Text(strings[TextKey.RemoveFavoriteBody]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removalConfirmationVisible = false
                        onDismiss()
                    },
                ) { Text(strings[TextKey.Remove]) }
            },
            dismissButton = {
                TextButton(onClick = { removalConfirmationVisible = false }) {
                    Text(strings[TextKey.Close])
                }
            },
        )
    }
}
