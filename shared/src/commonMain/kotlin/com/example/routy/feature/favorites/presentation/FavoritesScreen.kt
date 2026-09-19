package com.example.routy.feature.favorites.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.feature.favorites.presentation.state.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.stop_details.domain.GetStopDetailsUseCase
import com.example.routy.feature.stop_details.domain.minutesUntilNextScheduledDeparture
import com.example.routy.feature.stop_details.domain.nearestScheduledDeparture
import com.example.routy.feature.stop_details.domain.scheduledFrequencyMinutes
import routy.shared.generated.resources.Res
import routy.shared.generated.resources.favorites_backdrop

private const val RouteSheetPrefix = "route:"
private const val StopSheetPrefix = "stop:"

private data class UpcomingBus(
    val routeName: String,
    val minutes: Int,
    val routeColorIndex: Int,
)

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
    var removalConfirmation by rememberSaveable { mutableStateOf<String?>(null) }
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScreenScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings[TextKey.Favorites],
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { screenPadding ->
        Box(Modifier.fillMaxSize()) {
            ScreenBackdrop(Res.drawable.favorites_backdrop)
            LazyColumn(
                Modifier.fillMaxSize(),
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
                if (state.routes.isEmpty() &&
                    state.stops.isEmpty() &&
                    state.network.network != null
                ) {
                    item { EmptyPanel(TextKey.NoFavorites) }
                }
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
                    val upcomingBuses =
                        remember(stop, state.network.network, strings.language) {
                            val routes =
                                state.network.network
                                    ?.routes
                                    .orEmpty()
                            val routesById = routes.associateBy { it.id }
                            stop.services
                                .groupBy { it.routeId }
                                .mapNotNull { (routeId, services) ->
                                    val minutes =
                                        minutesUntilNextScheduledDeparture(services.flatMap { it.times })
                                            ?: return@mapNotNull null
                                    val route = routesById[routeId] ?: return@mapNotNull null
                                    UpcomingBus(
                                        routeName = route.name.resolve(strings.language, route.id),
                                        minutes = minutes,
                                        routeColorIndex = routes.indexOf(route).coerceAtLeast(0),
                                    )
                                }.sortedBy(UpcomingBus::minutes)
                                .take(3)
                        }
                    FavoriteSwipeToDismiss(
                        onDismiss = { model.actionHandler(FavoritesAction.RemoveStop(stop.id)) },
                    ) {
                        FavoriteStopCard(
                            stop = stop,
                            upcomingBuses = upcomingBuses,
                            onClick = {
                                sheetContent = "$StopSheetPrefix${stop.id}"
                            },
                            fallbackSubtitle =
                                strings.vehiclesCount(
                                    stop.services
                                        .map { it.routeId }
                                        .distinct()
                                        .size,
                                ),
                        )
                    }
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        sheetContent = null
                                        showRouteOnMap(routeId)
                                    },
                                ) { Text(strings[TextKey.ShowMap]) }
                                if (state.routes.any { it.id == routeId }) {
                                    FilledTonalButton(
                                        onClick = { removalConfirmation = "$RouteSheetPrefix$routeId" },
                                    ) { Text(strings[TextKey.Remove]) }
                                }
                            }
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
                                        sheetContent = null
                                        showStopOnMap(item.stop.id)
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
                                        onClick = { removalConfirmation = "$StopSheetPrefix$stopId" },
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
                            }.sortedBy { (route, _) ->
                                val times =
                                    details.stop.services
                                        .filter { it.routeId == route.id }
                                        .flatMap { it.times }
                                minutesUntilNextScheduledDeparture(times) ?: Int.MAX_VALUE
                            }.forEach { (route, nextDeparture) ->
                                item {
                                    RouteCard(
                                        route,
                                        onClick = {
                                            sheetContent = null
                                            showRouteOnMap(route.id)
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
    removalConfirmation?.let { target ->
        RemoveFavoriteDialog(
            onConfirm = {
                removalConfirmation = null
                sheetContent = null
                val action =
                    if (target.startsWith(RouteSheetPrefix)) {
                        FavoritesAction.RemoveRoute(target.removePrefix(RouteSheetPrefix))
                    } else {
                        FavoritesAction.RemoveStop(target.removePrefix(StopSheetPrefix))
                    }
                model.actionHandler(action)
            },
            onDismiss = { removalConfirmation = null },
        )
    }
}

@Composable
private fun RemoveFavoriteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(strings[TextKey.RemoveFavoriteTitle]) },
        text = { Text(strings[TextKey.RemoveFavoriteBody]) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings[TextKey.Remove]) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings[TextKey.Close]) } },
    )
}

@Composable
private fun FavoriteStopCard(
    stop: com.example.routy.core.transport.domain.BusStop,
    upcomingBuses: List<UpcomingBus>,
    fallbackSubtitle: String,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    RoutyIcon(Glyph.Stop)
                }
                Text(
                    stop.name.resolve(strings.language, stop.id),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                RoutyIcon(Glyph.Chevron, strings[TextKey.Details])
            }
            if (upcomingBuses.isEmpty()) {
                Text(
                    fallbackSubtitle,
                    modifier = Modifier.padding(start = 38.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(start = 38.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    upcomingBuses.forEach { bus ->
                        val color = palette.routeColors[bus.routeColorIndex % palette.routeColors.size]
                        Surface(
                            color = color.copy(alpha = 0.13f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                "${bus.routeName} · ${strings.minutesShort(bus.minutes)}",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
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
        RemoveFavoriteDialog(
            onConfirm = {
                removalConfirmationVisible = false
                onDismiss()
            },
            onDismiss = { removalConfirmationVisible = false },
        )
    }
}
