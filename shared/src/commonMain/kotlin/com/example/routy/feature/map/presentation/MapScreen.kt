package com.example.routy.feature.map.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.PlatformBackHandler
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.LocalRoutyPalette
import com.example.routy.core.designsystem.RouteCard
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.designsystem.ScreenScaffold
import com.example.routy.core.designsystem.SearchEmptyState
import com.example.routy.core.designsystem.SearchField
import com.example.routy.core.designsystem.StatusPanel
import com.example.routy.core.designsystem.StopCard
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.location.rememberSystemSettingsLauncher
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import routy.shared.generated.resources.Res
import routy.shared.generated.resources.bus

/** Style provider is a replaceable presentation configuration, never a domain dependency. */
data class MapStyleConfig(
    val light: String = "https://tiles.openfreemap.org/styles/positron",
    val dark: String = "https://tiles.openfreemap.org/styles/dark",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    model: MapViewModel,
    navigate: (Destination) -> Unit,
    style: MapStyleConfig = MapStyleConfig(),
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val vehicles by model.vehicles.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    val routes =
        state.network.network
            ?.routes
            .orEmpty()
            .let { routes -> routes.filter { it.id in state.favoriteRouteIds } + routes.filterNot { it.id in state.favoriteRouteIds } }
    val scope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchFieldQuery by rememberSaveable { mutableStateOf("") }
    var locationEnabled by rememberSaveable { mutableStateOf(false) }
    var focusLocation by remember { mutableStateOf(false) }
    var vehicleDetailsVisible by rememberSaveable { mutableStateOf(false) }
    var routeInfoVisible by rememberSaveable { mutableStateOf(false) }
    var selectedVehicle by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStop by rememberSaveable { mutableStateOf<String?>(null) }

    PlatformBackHandler(enabled = state.selectedRouteIds.isNotEmpty()) {
        model.actionHandler(MapAction.ClearSelectedRoutes)
    }

    var mapError by remember { mutableStateOf(false) }
    val busDirectionPainter = painterResource(Res.drawable.bus)
    val location = rememberLocationState(enabled = locationEnabled)
    val systemSettings = rememberSystemSettingsLauncher()
    var locationPrompt by remember { mutableStateOf(false) }
    val vehicleJson by produceState(EMPTY_GEOJSON, vehicles.vehicles) {
        value = withContext(Dispatchers.Default) { vehiclesGeoJson(vehicles.vehicles) }
    }
    val selectedStopJson =
        remember(
            state.stops,
            selectedStop,
        ) { stopsGeoJson(state.stops.filter { it.id == selectedStop }) }
    val selectedVehicleJson =
        remember(vehicles.vehicles, selectedVehicle) {
            vehiclesGeoJson(
                vehicles.vehicles.filter {
                    it.id ==
                        selectedVehicle
                },
            )
        }
    val dark = MaterialTheme.colorScheme.background.red < 0.3f
    val mapState =
        rememberMapState(
            baseStyle = BaseStyle.Uri(if (dark) style.dark else style.light),
            initialCameraPosition =
                CameraPosition(
                    target =
                        Position(
                            longitude = 41.6367,
                            latitude = 41.6461,
                        ),
                    zoom = 13.0,
                ),
        ) {
            val stopSource = rememberGeoJsonSource(GeoJsonData.JsonString(state.stopGeoJson))
            val vehicleSource = rememberGeoJsonSource(GeoJsonData.JsonString(vehicleJson))
            val selectionSource = rememberGeoJsonSource(GeoJsonData.JsonString(selectedStopJson))
            val vehicleSelectionSource =
                rememberGeoJsonSource(GeoJsonData.JsonString(selectedVehicleJson))
            state.geometries.forEachIndexed { index, geometry ->
                val routeSource =
                    rememberGeoJsonSource(GeoJsonData.JsonString(routeGeoJson(listOf(geometry))))
                val routeColor = palette.routeColors[index % palette.routeColors.size]
                LineLayer(
                    "route-outline-${geometry.routeId}",
                    routeSource,
                    color = const(palette.routeOutline),
                    width = const(8.dp),
                )
                LineLayer(
                    "route-${geometry.routeId}",
                    routeSource,
                    color = const(routeColor),
                    width = const(5.dp),
                )
            }
            CircleLayer(
                "stops",
                stopSource,
                color = const(Color.White),
                radius = const(3.dp),
                strokeColor = const(palette.route),
                strokeWidth = const(1.5.dp),
                hitPadding = 18.dp,
                onClick = { features ->
                    features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content?.let { id ->
                        selectedStop = id
                        model.actionHandler(MapAction.SelectStop(id))
                    }
                    ClickResult.Consume
                },
            )
            CircleLayer(
                "selected-stop",
                selectionSource,
                color = const(palette.selected),
                radius = const(9.dp),
                strokeColor = const(MaterialTheme.colorScheme.onSurface),
                strokeWidth = const(3.dp),
            )
            SymbolLayer(
                "vehicles",
                vehicleSource,
                iconImage = image(busDirectionPainter),
                iconSize = const(0.08f),
                iconRotate = feature["heading"].cast(),
                iconAllowOverlap = const(true),
                onClick = { features ->
                    features
                        .firstOrNull()
                        ?.properties
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content
                        ?.let { model.actionHandler(MapAction.SelectVehicle(it)) }
                    ClickResult.Consume
                },
            )
            CircleLayer(
                "selected-vehicle",
                vehicleSelectionSource,
                color = const(palette.selected),
                radius = const(10.dp),
                strokeColor = const(MaterialTheme.colorScheme.onSurface),
                strokeWidth = const(3.dp),
            )
            if (locationEnabled) LocationPuck(idPrefix = "user", locationState = location)
        }

    CollectEffects(model.effects) {
        when (it) {
            is MapEffect.Navigate -> navigate(it.destination)
            is MapEffect.ShowVehicle -> {
                selectedVehicle = it.id
                vehicleDetailsVisible = true
            }

            is MapEffect.ShowStopOnMap -> selectedStop = it.id

            MapEffect.RequestLocation -> {
                locationEnabled = true
                focusLocation = true
                val permission = location.permission
                if (permission is LocationPermission.NotGranted && (permission.shouldShowRationale || permission.canRequest == false)) {
                    locationPrompt = true
                } else {
                    location.requestPermission()
                    location.retry()
                }
            }
        }
    }
    LaunchedEffect(mapState) {
        mapState.events.collect {
            if (it is MapEvent.StyleLoadFailed) mapError = true
        }
    }
    LaunchedEffect(state.search.isOpen) {
        if (state.search.isOpen) {
            searchFieldQuery = state.search.query
            yield()
            searchFocusRequester.requestFocus()
            keyboard?.show()
            searchSheetState.expand()
        } else {
            keyboard?.hide()
        }
    }
    LaunchedEffect(state.selectedRouteIds) {
        selectedVehicle = null
        vehicleDetailsVisible = false
        routeInfoVisible = false
    }
    LaunchedEffect(selectedStop) {
        state.stops.firstOrNull { it.id == selectedStop }?.let {
            mapState.animateCameraPosition(
                mapState.cameraPosition.copy(
                    target =
                        Position(
                            it.position.longitude,
                            it.position.latitude,
                        ),
                ),
            )
        }
    }
    LaunchedEffect(focusLocation, location.lastLocation) {
        if (focusLocation) {
            location.lastLocation?.let {
                mapState.animateCameraPosition(CameraPosition(target = it.position, zoom = 15.0))
                focusLocation = false
            }
        }
    }
    ScreenScaffold(hasTopBarOverlay = false) { screenPadding ->
        Box(Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = strings[TextKey.Map] },
                state = mapState,
                overlay = {},
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = screenPadding.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { model.actionHandler(MapAction.OpenSearch) },
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
                if (state.network.network == null ||
                    state.network.error != null
                ) {
                    StatusPanel(state.network) { model.actionHandler(MapAction.Retry) }
                }
                if (mapError) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(strings[TextKey.MapUnavailable], Modifier.padding(16.dp)) }
                }
            }
            LazyRow(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = screenPadding.calculateTopPadding() + 84.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    routes,
                    key = { it.id },
                ) { route ->
                    FilterChip(
                        selected = route.id in state.selectedRouteIds,
                        onClick = { model.actionHandler(MapAction.SelectRoute(route.id)) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (route.id in state.favoriteRouteIds) {
                                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                                        RoutyIcon(Glyph.Star)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(route.name.resolve(strings.language, route.id))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.selectedRouteIds.isNotEmpty()) {
                    SmallFloatingActionButton(
                        { routeInfoVisible = true },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        RoutyIcon(Glyph.Routes, strings[TextKey.Live])
                    }
                }
                SmallFloatingActionButton({
                    model.actionHandler(MapAction.OpenStops)
                }, containerColor = MaterialTheme.colorScheme.surface) {
                    RoutyIcon(
                        Glyph.Stop,
                        strings[TextKey.Nearby],
                    )
                }
                SmallFloatingActionButton({
                    model.actionHandler(MapAction.MyLocation)
                }, containerColor = MaterialTheme.colorScheme.surface) {
                    RoutyIcon(
                        Glyph.Location,
                        strings[TextKey.MyLocation],
                    )
                }
            }
        }
    }
    if (routeInfoVisible) {
        ModalBottomSheet(
            onDismissRequest = { routeInfoVisible = false },
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.network.network
                    ?.routes
                    ?.firstOrNull { it.id == state.routeId }
                    ?.let { route ->
                        Text(
                            route.name.resolve(strings.language, route.id),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                Text(strings[TextKey.Live], style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        vehicles.isLoading -> strings[TextKey.Loading]
                        vehicles.isStale -> strings[TextKey.Stale]
                        vehicles.vehicles.isEmpty() -> strings[TextKey.NoBuses]
                        else -> "${vehicles.vehicles.size} ${strings[TextKey.Vehicle]}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (vehicles.vehicles.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vehicles.vehicles, key = { it.id }) { vehicle ->
                            AssistChip(
                                onClick = {
                                    routeInfoVisible = false
                                    model.actionHandler(MapAction.SelectVehicle(vehicle.id))
                                },
                                label = { Text("${strings[TextKey.Vehicle]} ${vehicle.id}") },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = {
                        routeInfoVisible = false
                        model.actionHandler(MapAction.OpenRouteDetails)
                    },
                ) { Text(strings[TextKey.Details]) }
            }
        }
    }
    if (state.search.isOpen) {
        val hasCurrentSearchResults = state.search.query == searchFieldQuery
        ModalBottomSheet(
            onDismissRequest = { model.actionHandler(MapAction.CloseSearch) },
            sheetState = searchSheetState,
            containerColor = palette.searchSheet,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
        ) {
            LazyColumn(
                Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(top = 8.dp, bottom = searchBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SearchField(
                        value = searchFieldQuery,
                        placeholder = strings[TextKey.Search],
                        onChange = {
                            searchFieldQuery = it
                            model.actionHandler(MapAction.SearchQueryChanged(it))
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                            .focusRequester(searchFocusRequester),
                        onClear = {
                            searchFieldQuery = ""
                            model.actionHandler(MapAction.ClearSearch)
                        },
                    )
                }
                if (searchFieldQuery.isBlank()) {
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
                                FilterChip(
                                    selected = route.id in state.selectedRouteIds,
                                    onClick = {
                                        model.actionHandler(
                                            MapAction.SelectSearchRoute(
                                                route.id,
                                            ),
                                        )
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (route.id in state.favoriteRouteIds) {
                                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                                                    RoutyIcon(
                                                        Glyph.Star,
                                                        strings[TextKey.Favorites],
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(route.name.resolve(strings.language, route.id))
                                        }
                                    },
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
                                onClick = { model.actionHandler(MapAction.SelectSearchRoute(route.id)) },
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
                if (hasCurrentSearchResults) items(
                    state.search.stops,
                    key = { "stop:${it.id}" }) { stop ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        StopCard(
                            stop = stop,
                            subtitle =
                                stop.services
                                    .map { it.routeId }
                                    .distinct()
                                    .joinToString(" · "),
                            onClick = {
                                model.actionHandler(MapAction.SelectSearchStop(stop.id))
                            },
                        )
                    }
                }
                if (hasCurrentSearchResults && searchFieldQuery.isNotBlank() && state.search.routes.isEmpty() && state.search.stops.isEmpty()) {
                    item { SearchEmptyState(Modifier.padding(horizontal = 20.dp)) }
                }
            }
        }
    }
    if (locationPrompt) {
        AlertDialog(
            onDismissRequest = { locationPrompt = false },
            title = { Text(strings[TextKey.MyLocation]) },
            text = { Text(strings[TextKey.LocationHelp]) },
            confirmButton = {
                TextButton({
                    locationPrompt = false
                    if ((location.permission as? LocationPermission.NotGranted)?.canRequest == false) {
                        systemSettings.openApplicationSettings()
                    } else {
                        location.requestPermission()
                    }
                }) { Text(strings[TextKey.MyLocation]) }
            },
            dismissButton = {
                TextButton({
                    locationPrompt = false
                }) { Text(strings[TextKey.Close]) }
            },
        )
    }
    if (vehicleDetailsVisible) {
        selectedVehicle?.let { id ->
            val vehicle = vehicles.vehicles.firstOrNull { it.id == id }
            AlertDialog(
                onDismissRequest = { vehicleDetailsVisible = false },
                title = { Text("${strings[TextKey.Vehicle]} $id") },
                text = { Text(strings[if (vehicles.isStale || vehicle == null) TextKey.Stale else TextKey.Live]) },
                confirmButton = {
                    TextButton({
                        vehicle?.let {
                            scope.launch {
                                mapState.animateCameraPosition(
                                    CameraPosition(
                                        target =
                                            Position(
                                                it.position.longitude,
                                                it.position.latitude,
                                            ),
                                        zoom = 16.0,
                                    ),
                                )
                            }
                        }
                        selectedVehicle =
                            null
                    }) { Text(strings[TextKey.ShowMap]) }
                },
                dismissButton = {
                    TextButton({
                        vehicleDetailsVisible = false
                    }) { Text(strings[TextKey.Close]) }
                },
            )
        }
    }
}
