package com.example.routy.feature.map.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.PlatformBackHandler
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.LocalRoutyPalette
import com.example.routy.core.designsystem.NetworkIssueBadge
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.designsystem.ScreenScaffold
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.core.transport.domain.VehicleState
import com.example.routy.core.transport.domain.hasConnectivityIssue
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapCamera
import com.example.routy.feature.map.presentation.state.MapEffect
import com.example.routy.feature.map.presentation.ui.LocationPromptDialog
import com.example.routy.feature.map.presentation.ui.MapRouteChip
import com.example.routy.feature.map.presentation.ui.MapSearchSheet
import com.example.routy.feature.map.presentation.ui.MapZoomButton
import com.example.routy.feature.map.presentation.ui.RouteInfoSheet
import com.example.routy.feature.map.presentation.ui.VehicleDetailsDialog
import com.example.routy.feature.map.presentation.ui.VehicleLayers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
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
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import routy.shared.generated.resources.Res
import routy.shared.generated.resources.azure_bus
import routy.shared.generated.resources.blue_bus
import routy.shared.generated.resources.bus
import routy.shared.generated.resources.dark_blue_bus
import routy.shared.generated.resources.favorite_stop
import routy.shared.generated.resources.green_bus
import routy.shared.generated.resources.orage_bus
import routy.shared.generated.resources.pink_bus
import routy.shared.generated.resources.red_bus

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
    isActive: Boolean = true,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val camera by model.camera.collectAsStateWithLifecycle()
    val vehicles by
        model.vehicles.collectAsStateWithLifecycle(
            initialValue = VehicleState(isLoading = false),
            lifecycle = LocalLifecycleOwner.current.lifecycle,
        )
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
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val routeInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchFieldQuery by rememberSaveable { mutableStateOf("") }
    var focusLocation by remember { mutableStateOf(false) }
    var initialLocationCheckFinished by remember { mutableStateOf(false) }
    var locationPrompt by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf(false) }
    val mapStops =
        remember(state.stops, state.network.network, state.selectedStopId) {
            val shown =
                state.selectedStopId
                    ?.takeIf { id -> state.stops.none { it.id == id } }
                    ?.let { id ->
                        state.network.network
                            ?.stops
                            ?.firstOrNull { it.id == id }
                    }
            if (shown == null) state.stops else state.stops + shown
        }

    PlatformBackHandler(enabled = isActive && state.selectedRouteIds.isNotEmpty()) {
        model.actionHandler(MapAction.ClearSelectedRoutes)
    }

    val busDirectionPainter = painterResource(Res.drawable.bus)
    val favoriteStopPainter = painterResource(Res.drawable.favorite_stop)
    val routeBusPainters =
        listOf(
            painterResource(Res.drawable.blue_bus),
            painterResource(Res.drawable.red_bus),
            painterResource(Res.drawable.green_bus),
            painterResource(Res.drawable.orage_bus),
            painterResource(Res.drawable.azure_bus),
            painterResource(Res.drawable.pink_bus),
            painterResource(Res.drawable.dark_blue_bus),
        )
    val location = rememberLocationState(enabled = state.isLocationEnabled)
    val systemSettings = rememberSystemSettingsLauncher()
    val animatedVehicles = rememberAnimatedVehicles(vehicles)
    val vehicleRouteCount = remember(vehicles.vehicles) { vehicles.vehicles.distinctBy(Vehicle::routeId).size }
    SideEffect {
        model.logVehicleLayersComposed(
            routeCount = vehicleRouteCount,
            vehicleCount = vehicles.vehicles.size,
            selectedRouteCount = state.selectedRouteIds.size,
        )
    }
    val routeLabels =
        remember(state.network.network?.routes, strings.language) {
            state.network.network
                ?.routes
                ?.associate { route -> route.id to route.name.resolve(strings.language, route.id) }
                .orEmpty()
        }
    val selectedStopJson =
        remember(mapStops, state.selectedStopId) { stopsGeoJson(mapStops.filter { it.id == state.selectedStopId }) }
    val mapStopsGeoJson =
        remember(mapStops, state.favoriteStopIds) { stopsGeoJson(mapStops, state.favoriteStopIds) }
    val favoriteStopsGeoJson =
        remember(mapStops, state.favoriteStopIds) {
            stopsGeoJson(mapStops.filter { stop -> stop.id in state.favoriteStopIds })
        }
    val dark = MaterialTheme.colorScheme.background.red < 0.3f
    val styleUri = if (dark) style.dark else style.light
    val mapStyleJson by model.mapStyleJson.collectAsStateWithLifecycle()
    LaunchedEffect(dark, styleUri) { model.actionHandler(MapAction.LoadMapStyle(styleUri.takeIf { dark })) }
    val baseStyle =
        remember(styleUri, mapStyleJson) { mapStyleJson?.let(BaseStyle::Json) ?: BaseStyle.Uri(styleUri) }
    // Route lines must be ready before MapLibre redraws its tiles after a zoom gesture.
    val routeSourceOptions =
        remember {
            GeoJsonOptions(
                synchronousUpdate = true,
            )
        }
    val mapInteractions =
        remember {
            MapInteractions {
                camera {
                    rotate { enabled = false }
                }
            }
        }
    val mapState =
        rememberMapState(
            baseStyle = baseStyle,
            initialCameraPosition =
                CameraPosition(
                    target =
                        Position(
                            longitude = camera.longitude,
                            latitude = camera.latitude,
                        ),
                    zoom = camera.zoom,
                ),
        ) {
            val stopSource = rememberGeoJsonSource(GeoJsonData.JsonString(mapStopsGeoJson))
            val favoriteStopSource = rememberGeoJsonSource(GeoJsonData.JsonString(favoriteStopsGeoJson))
            val selectionSource = rememberGeoJsonSource(GeoJsonData.JsonString(selectedStopJson))
            state.geometries.forEach { geometry ->
                val routeSource =
                    rememberGeoJsonSource(
                        GeoJsonData.JsonString(routeGeoJson(listOf(geometry))),
                        options = routeSourceOptions,
                    )
                val routeColor =
                    palette.routeColors[state.routeColorIndex(geometry.routeId).coerceAtLeast(0) % palette.routeColors.size]
                LineLayer(
                    "route-outline-${geometry.routeId}",
                    routeSource,
                    color = const(palette.routeOutline),
                    width = const(5.dp),
                )
                LineLayer(
                    "route-${geometry.routeId}",
                    routeSource,
                    color = const(routeColor),
                    width = const(3.dp),
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
                        model.actionHandler(MapAction.SelectStop(id))
                    }
                    ClickResult.Consume
                },
            )
            CircleLayer(
                "selected-stop",
                selectionSource,
                color = const(Color.White),
                radius = const(8.dp),
                strokeColor = const(palette.route),
                strokeWidth = const(2.5.dp),
            )
            if (state.favoriteStopIds.isNotEmpty()) {
                SymbolLayer(
                    "favorite-stops",
                    favoriteStopSource,
                    iconImage = image(favoriteStopPainter),
                    iconSize = const(0.5f),
                    iconAllowOverlap = const(true),
                    onClick = { features ->
                        features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content?.let { id ->
                            model.actionHandler(MapAction.SelectStop(id))
                        }
                        ClickResult.Consume
                    },
                )
            }
            VehicleLayers(
                vehicles = animatedVehicles,
                routeColorIndex = state::routeColorIndex,
                routeLabels = routeLabels,
                routeBusPainters = routeBusPainters,
                busDirectionPainter = busDirectionPainter,
                onAttached = model::logVehicleLayerAttached,
                onDetached = model::logVehicleLayerDetached,
                onVehicleClick = { model.actionHandler(MapAction.SelectVehicle(it)) },
            )
            if (state.isLocationEnabled) LocationPuck(idPrefix = "user", locationState = location)
        }

    CollectEffects(model.effects) {
        when (it) {
            is MapEffect.Navigate -> navigate(it.destination)

            MapEffect.RequestLocation -> {
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
    DisposableEffect(mapState) {
        onDispose {
            mapState.cameraPosition.let { position ->
                model.actionHandler(
                    MapAction.SaveCamera(
                        MapCamera(
                            longitude = position.target.longitude,
                            latitude = position.target.latitude,
                            zoom = position.zoom,
                        ),
                    ),
                )
            }
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
    LaunchedEffect(state.selectedStopId) {
        state.network.network?.stops?.firstOrNull { it.id == state.selectedStopId }?.let {
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
    LaunchedEffect(location.permission) {
        if (location.permission !is LocationPermission.NotGranted) {
            model.actionHandler(MapAction.LocationAvailable)
            delay(1_500)
        }
        initialLocationCheckFinished = true
    }
    LaunchedEffect(
        location.lastLocation,
        state.network.network,
        state.favoriteStopIds,
        state.isInitialCameraPlaced,
        initialLocationCheckFinished,
    ) {
        if (state.isInitialCameraPlaced) return@LaunchedEffect
        location.lastLocation?.let { userLocation ->
            mapState.animateCameraPosition(CameraPosition(target = userLocation.position, zoom = 14.0))
            model.actionHandler(MapAction.InitialCameraPlaced)
            return@LaunchedEffect
        }
        if (!initialLocationCheckFinished) return@LaunchedEffect
        val initialStop =
            state.network.network
                ?.stops
                ?.firstOrNull { it.id in state.favoriteStopIds }
                ?: return@LaunchedEffect
        mapState.animateCameraPosition(
            mapState.cameraPosition.copy(
                target = Position(initialStop.position.longitude, initialStop.position.latitude),
                zoom = 14.0,
            ),
        )
        model.actionHandler(MapAction.InitialCameraPlaced)
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
                interactions = mapInteractions,
                overlay = {},
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = screenPadding.calculateTopPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { model.actionHandler(MapAction.OpenSearch) },
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
                    items(
                        routes,
                        key = { it.id },
                    ) { route ->
                        val selected = route.id in state.selectedRouteIds
                        MapRouteChip(
                            route = route,
                            colorIndex = state.routeColorIndex(route.id),
                            selected = selected,
                            enabled = selected || state.selectedRouteIds.size < 7,
                            favorite = route.id in state.favoriteRouteIds,
                            onClick = { model.actionHandler(MapAction.SelectRoute(route.id)) },
                            showBorder = false,
                        )
                    }
                }
                if (mapError) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(strings[TextKey.MapUnavailable], Modifier.padding(16.dp)) }
                }
            }
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MapZoomButton(
                    label = "Приблизить карту",
                    symbol = "+",
                    onClick = {
                        scope.launch {
                            mapState.animateCameraPosition(
                                mapState.cameraPosition.copy(
                                    zoom = (mapState.cameraPosition.zoom + 1.0).coerceAtMost(20.0),
                                ),
                            )
                        }
                    },
                )
                MapZoomButton(
                    label = "Отдалить карту",
                    symbol = "−",
                    onClick = {
                        scope.launch {
                            mapState.animateCameraPosition(
                                mapState.cameraPosition.copy(
                                    zoom = (mapState.cameraPosition.zoom - 1.0).coerceAtLeast(1.0),
                                ),
                            )
                        }
                    },
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 156.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.network.hasConnectivityIssue || vehicles.isStale) {
                    NetworkIssueBadge(onClick = { model.actionHandler(MapAction.Retry) })
                }
                if (state.selectedRouteIds.isNotEmpty()) {
                    SmallFloatingActionButton(
                        { model.actionHandler(MapAction.OpenRouteInfo) },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        RoutyIcon(Glyph.Routes, strings[TextKey.Live])
                    }
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
    state.routeInfo?.let { routeInfo ->
        RouteInfoSheet(
            state = state,
            routeInfo = routeInfo,
            vehicles = vehicles,
            sheetState = routeInfoSheetState,
            onAction = model::actionHandler,
        )
    }
    if (state.search.isOpen) {
        MapSearchSheet(
            state = state,
            query = searchFieldQuery,
            onQueryChange = {
                searchFieldQuery = it
                model.actionHandler(MapAction.SearchQueryChanged(it))
            },
            onClear = {
                searchFieldQuery = ""
                model.actionHandler(MapAction.ClearSearch)
            },
            sheetState = searchSheetState,
            focusRequester = searchFocusRequester,
            onAction = model::actionHandler,
        )
    }
    if (locationPrompt) {
        LocationPromptDialog(
            onConfirm = {
                locationPrompt = false
                if ((location.permission as? LocationPermission.NotGranted)?.canRequest == false) {
                    systemSettings.openApplicationSettings()
                } else {
                    location.requestPermission()
                }
            },
            onDismiss = { locationPrompt = false },
        )
    }
    state.selectedVehicleId?.let { id ->
        VehicleDetailsDialog(
            vehicleId = id,
            vehicle = vehicles.vehicles.firstOrNull { it.id == id },
            isStale = vehicles.isStale,
            onShowOnMap = { vehicle ->
                scope.launch {
                    mapState.animateCameraPosition(
                        CameraPosition(target = Position(vehicle.position.longitude, vehicle.position.latitude), zoom = 16.0),
                    )
                }
            },
            onDismiss = { model.actionHandler(MapAction.DismissVehicle) },
        )
    }
}
