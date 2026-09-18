package com.example.routy.feature.map.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.routy.PlatformBackHandler
import com.example.routy.notifyBusApproaching
import com.example.routy.requestBusNotificationPermission
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.EmptyPanel
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
import com.example.routy.feature.map.presentation.state.MapCamera
import com.example.routy.feature.map.presentation.state.MapEffect
import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.VehicleState
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.stop_details.domain.scheduledFrequencyMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.span
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
import routy.shared.generated.resources.tracking_stop

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
    val searchBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val routeInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchFieldQuery by rememberSaveable { mutableStateOf("") }
    var locationEnabled by rememberSaveable { mutableStateOf(false) }
    var focusLocation by remember { mutableStateOf(false) }
    var initialStopFocused by rememberSaveable { mutableStateOf(false) }
    var initialLocationCheckFinished by rememberSaveable { mutableStateOf(false) }
    var vehicleDetailsVisible by rememberSaveable { mutableStateOf(false) }
    var routeInfoVisible by rememberSaveable { mutableStateOf(false) }
    var routeInfoRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeScheduleVisible by rememberSaveable { mutableStateOf(false) }
    var routeInfoSwipeDistance by remember { mutableStateOf(0f) }
    var selectedVehicle by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStop by rememberSaveable { mutableStateOf<String?>(null) }
    var trackingVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    var trackingStopId by rememberSaveable { mutableStateOf<String?>(null) }
    var trackingSetupVisible by rememberSaveable { mutableStateOf(false) }
    var stopPickerVisible by rememberSaveable { mutableStateOf(false) }
    var pickingTrackingStop by rememberSaveable { mutableStateOf(false) }
    var stopPickerQuery by rememberSaveable { mutableStateOf("") }
    var busReachedTrackingStop by rememberSaveable { mutableStateOf(false) }
    val trackingStop = state.network.network?.stops?.firstOrNull { it.id == trackingStopId }
    val isTrackingActive =
        trackingVehicleId != null &&
            trackingStopId != null &&
            trackingVehicleId == state.trackedVehicleId &&
            trackingStopId == state.trackedStopId
    val trackingVehicle = vehicles.vehicles.firstOrNull { it.id == trackingVehicleId }
    val trackingRouteJson =
        remember(isTrackingActive, trackingVehicle, trackingStop, state.geometries) {
            if (isTrackingActive && trackingVehicle != null && trackingStop != null) {
                trackingRouteGeoJson(
                    trackingVehicle,
                    trackingStop,
                    state.trackingGeometry?.takeIf { it.routeId == trackingVehicle.routeId },
                )
            } else {
                EMPTY_GEOJSON
            }
        }
    val mapStops = if (pickingTrackingStop) state.network.network?.stops.orEmpty() else state.stops

    fun selectMapStop(id: String) {
        selectedStop = id
        if (pickingTrackingStop) {
            trackingStopId = id
            busReachedTrackingStop = false
            pickingTrackingStop = false
            trackingSetupVisible = true
        } else {
            model.actionHandler(MapAction.SelectStop(id))
        }
    }

    PlatformBackHandler(enabled = isActive && state.selectedRouteIds.isNotEmpty()) {
        model.actionHandler(MapAction.ClearSelectedRoutes)
    }

    var mapError by remember { mutableStateOf(false) }
    val busDirectionPainter = painterResource(Res.drawable.bus)
    val favoriteStopPainter = painterResource(Res.drawable.favorite_stop)
    val trackingStopPainter = painterResource(Res.drawable.tracking_stop)
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
    val location = rememberLocationState(enabled = locationEnabled)
    val systemSettings = rememberSystemSettingsLauncher()
    var locationPrompt by remember { mutableStateOf(false) }
    val vehiclesByRoute = remember(vehicles.vehicles) { vehicles.vehicles.groupBy { it.routeId } }
    SideEffect {
        model.logVehicleLayersComposed(
            routeCount = vehiclesByRoute.size,
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
        remember(
            mapStops,
            selectedStop,
            trackingStopId,
            isTrackingActive,
        ) {
            if (isTrackingActive && selectedStop == trackingStopId) {
                EMPTY_GEOJSON
            } else {
                stopsGeoJson(mapStops.filter { it.id == selectedStop })
            }
        }
    val trackingStopJson =
        remember(trackingStop, isTrackingActive) {
            if (isTrackingActive && trackingStop != null) stopsGeoJson(listOf(trackingStop)) else EMPTY_GEOJSON
        }
    val mapStopsGeoJson =
        remember(mapStops, state.favoriteStopIds) { stopsGeoJson(mapStops, state.favoriteStopIds) }
    val favoriteStopsGeoJson =
        remember(mapStops, state.favoriteStopIds, trackingStopId, isTrackingActive) {
            stopsGeoJson(
                mapStops.filter { stop ->
                    stop.id in state.favoriteStopIds && !(isTrackingActive && stop.id == trackingStopId)
                },
            )
        }
    val dark = MaterialTheme.colorScheme.background.red < 0.3f
    // Route lines must be ready before MapLibre redraws its tiles after a zoom gesture.
    val routeSourceOptions =
        remember {
            GeoJsonOptions(
                synchronousUpdate = true,
            )
        }
    val mapState =
        rememberMapState(
            baseStyle = BaseStyle.Uri(if (dark) style.dark else style.light),
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
            val trackingStopSource = rememberGeoJsonSource(GeoJsonData.JsonString(trackingStopJson))
            val trackingRouteSource =
                rememberGeoJsonSource(
                    GeoJsonData.JsonString(trackingRouteJson),
                    options = routeSourceOptions,
                )
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
            LineLayer(
                "tracking-route",
                trackingRouteSource,
                color = const(palette.selected),
                width = const(6.dp),
            )
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
                        selectMapStop(id)
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
                            selectMapStop(id)
                        }
                        ClickResult.Consume
                    },
                )
            }
            SymbolLayer(
                "tracking-stop",
                trackingStopSource,
                iconImage = image(trackingStopPainter),
                iconSize = const(0.4f),
                iconAllowOverlap = const(true),
            )
            vehiclesByRoute.entries.sortedBy { it.key }.forEach { (routeId, routeVehicles) ->
                key(routeId) {
                    DisposableEffect(routeId) {
                        model.logVehicleLayerAttached(routeVehicles.size)
                        onDispose { model.logVehicleLayerDetached() }
                    }
                    val routeVehicleSource =
                        rememberGeoJsonSource(GeoJsonData.JsonString(vehiclesGeoJson(routeVehicles)))
                    val selectedRouteIndex = state.routeColorIndex(routeId)
                    val routeLabel = routeLabels[routeId] ?: routeId
                    SymbolLayer(
                        "vehicles-$routeId",
                        routeVehicleSource,
                        iconImage = image(routeBusPainters.getOrNull(selectedRouteIndex) ?: busDirectionPainter),
                        iconSize = const(if (selectedRouteIndex >= 0) 0.1f else 0.08f),
                        iconRotate = feature["heading"].cast(),
                        iconAllowOverlap = const(true),
                        textField = format(span(routeLabel)),
                        textColor = const(Color(0xFF0F172A)),
                        textHaloColor = const(Color.White),
                        textHaloWidth = const(1.dp),
                        textFont = const(listOf("Noto Sans Bold")),
                        textSize = const(12.sp),
                        textAllowOverlap = const(true),
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
                }
            }
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

            MapEffect.ClearStopSelection -> selectedStop = null

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
    LaunchedEffect(state.selectedRouteIds) {
        selectedVehicle = null
        vehicleDetailsVisible = false
        routeInfoVisible = false
        routeScheduleVisible = false
    }
    LaunchedEffect(state.trackedVehicleId, state.trackedStopId) {
        if (trackingVehicleId != state.trackedVehicleId || trackingStopId != state.trackedStopId) {
            trackingVehicleId = state.trackedVehicleId
            trackingStopId = state.trackedStopId
            selectedStop = state.trackedStopId
            busReachedTrackingStop = false
        }
    }
    LaunchedEffect(isTrackingActive, trackingVehicle, state.trackedRouteId) {
        if (isTrackingActive && state.trackedRouteId == null && trackingVehicle != null) {
            // Migrates tracking records created before route persistence was introduced.
            model.actionHandler(MapAction.SetTracking(trackingVehicleId, trackingStopId, trackingVehicle.routeId))
        }
    }
    LaunchedEffect(vehicles.vehicles, trackingVehicleId, trackingStop, isTrackingActive) {
        if (!isTrackingActive) return@LaunchedEffect
        val bus = vehicles.vehicles.firstOrNull { it.id == trackingVehicleId } ?: return@LaunchedEffect
        val stop = trackingStop ?: return@LaunchedEffect
        val distance = distanceMeters(bus.position.latitude, bus.position.longitude, stop.position.latitude, stop.position.longitude)
        if (!busReachedTrackingStop && distance <= 250.0) {
            notifyBusApproaching(
                strings[TextKey.BusApproaching],
                "${strings[TextKey.Vehicle]} ${bus.id} ${strings[TextKey.BusApproachingBody]} ${stop.name.resolve(strings.language, stop.id)}",
            )
            busReachedTrackingStop = true
        } else if (busReachedTrackingStop && distance > 350.0) {
            // The bus has left the stop's area after the arrival alert: this trip is complete.
            trackingVehicleId = null
            trackingStopId = null
            selectedStop = null
            busReachedTrackingStop = false
            model.actionHandler(MapAction.SetTracking(null, null, null))
        }
    }
    LaunchedEffect(selectedStop) {
        state.network.network?.stops?.firstOrNull { it.id == selectedStop }?.let {
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
            locationEnabled = true
            delay(1_500)
        }
        initialLocationCheckFinished = true
    }
    LaunchedEffect(location.lastLocation, state.network.network, state.trackedStopId, state.favoriteStopIds, initialStopFocused, initialLocationCheckFinished) {
        if (initialStopFocused) return@LaunchedEffect
        location.lastLocation?.let { userLocation ->
            mapState.animateCameraPosition(CameraPosition(target = userLocation.position, zoom = 14.0))
            initialStopFocused = true
            return@LaunchedEffect
        }
        if (!initialLocationCheckFinished) return@LaunchedEffect
        val initialStop =
            state.network.network
                ?.stops
                ?.firstOrNull { it.id == state.trackedStopId }
                ?: state.network.network
                    ?.stops
                    ?.firstOrNull { it.id in state.favoriteStopIds }
                ?: return@LaunchedEffect
        mapState.animateCameraPosition(
            mapState.cameraPosition.copy(
                target = Position(initialStop.position.longitude, initialStop.position.latitude),
                zoom = 14.0,
            ),
        )
        initialStopFocused = true
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
                    val selected = route.id in state.selectedRouteIds
                    val routeColor = palette.routeColors[state.routeColorIndex(route.id).mod(palette.routeColors.size)]
                    val selectedContentColor = if (routeColor.luminance() > 0.45f) Color(0xFF0F172A) else Color.White
                    FilterChip(
                        selected = selected,
                        enabled = selected || state.selectedRouteIds.size < 7,
                        onClick = { model.actionHandler(MapAction.SelectRoute(route.id)) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (route.id in state.favoriteRouteIds) {
                                    CompositionLocalProvider(
                                        LocalContentColor provides
                                            if (selected) selectedContentColor else MaterialTheme.colorScheme.tertiary,
                                    ) {
                                        RoutyIcon(Glyph.StarFilled)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(route.name.resolve(strings.language, route.id))
                            }
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = routeColor,
                                selectedLabelColor = selectedContentColor,
                                selectedLeadingIconColor = selectedContentColor,
                            ),
                        border = null,
                    )
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
                    .padding(end = 16.dp, bottom = if (isTrackingActive && trackingStop != null) 264.dp else 156.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.selectedRouteIds.isNotEmpty()) {
                    SmallFloatingActionButton(
                        {
                            routeInfoRouteId = state.routeId
                            routeInfoVisible = true
                        },
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
            if (isTrackingActive && trackingStop != null) {
                Surface(
                    onClick = { trackingSetupVisible = true },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 132.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RoutyIcon(Glyph.Routes)
                        Column(Modifier.weight(1f)) {
                            Text(strings[TextKey.Tracking], style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${strings[TextKey.Vehicle]} $trackingVehicleId → ${trackingStop.name.resolve(strings.language, trackingStop.id)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        RoutyIcon(Glyph.Chevron, strings[TextKey.Details])
                    }
                }
            }
        }
    }
    if (routeInfoVisible) {
        val routeIds = state.selectedRouteIds
        val activeRouteId = routeInfoRouteId?.takeIf { it in routeIds } ?: routeIds.lastOrNull()
        val activeRouteIndex = routeIds.indexOf(activeRouteId)
        val activeVehicles = vehicles.vehicles.filter { it.routeId == activeRouteId }
        val routeSchedule =
            remember(state.network.network, activeRouteId) {
                activeRouteId?.let { id -> state.network.network?.let { GetRouteDetailsUseCase()(it, id) } }
            }
        val routeFrequency =
            routeSchedule
                ?.groups
                ?.values
                ?.asSequence()
                ?.flatten()
                ?.mapNotNull { scheduledFrequencyMinutes(it.service.times) }
                ?.firstOrNull()
        ModalBottomSheet(
            onDismissRequest = {
                routeInfoVisible = false
                routeScheduleVisible = false
            },
            sheetState = routeInfoSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
        ) {
            if (routeScheduleVisible) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TextButton(onClick = { routeScheduleVisible = false }) {
                            RoutyIcon(Glyph.Back, strings[TextKey.Back])
                            Spacer(Modifier.width(8.dp))
                            Text(strings[TextKey.Back])
                        }
                        routeSchedule?.let { details ->
                            Text(
                                details.route.name.resolve(strings.language, details.route.id),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                    item { Text(strings[TextKey.ScheduleNote], style = MaterialTheme.typography.bodySmall) }
                    routeSchedule?.groups?.forEach { (group, stops) ->
                        item {
                            Text(
                                stops.lastOrNull()?.stop?.let { stop -> stop.name.resolve(strings.language, stop.id) }
                                    ?: strings[TextKey.Unspecified],
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(stops, key = { "$group:${it.stop.id}" }) { routeStop ->
                            StopCard(
                                stop = routeStop.stop,
                                onClick = {
                                    routeScheduleVisible = false
                                    routeInfoVisible = false
                                    model.actionHandler(MapAction.ShowStopOnMap(routeStop.stop.id))
                                },
                                subtitle =
                                    routeStop.service.times
                                        .take(4)
                                        .joinToString(" • ")
                                        .ifEmpty { strings[TextKey.NoSchedule] },
                            )
                        }
                    } ?: item { EmptyPanel() }
                }
            } else Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(routeIds, activeRouteId) {
                        detectHorizontalDragGestures(
                            onDragStart = { routeInfoSwipeDistance = 0f },
                            onHorizontalDrag = { _, dragAmount -> routeInfoSwipeDistance += dragAmount },
                            onDragEnd = {
                                routeInfoRouteId =
                                    when {
                                        routeInfoSwipeDistance <= -48f -> routeIds.getOrNull(activeRouteIndex + 1)
                                        routeInfoSwipeDistance >= 48f -> routeIds.getOrNull(activeRouteIndex - 1)
                                        else -> routeInfoRouteId
                                    }
                            },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedContent(
                    targetState = activeRouteId,
                    transitionSpec = {
                        val forward = routeIds.indexOf(targetState) > routeIds.indexOf(initialState)
                        if (forward) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "route-info-transition",
                ) { displayedRouteId ->
                    val displayedIndex = routeIds.indexOf(displayedRouteId)
                    state.network.network
                        ?.routes
                        ?.firstOrNull { it.id == displayedRouteId }
                        ?.let { route ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (routeIds.size > 1) 12.dp else 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (routeIds.size > 1) {
                                        IconButton(
                                            onClick = { routeInfoRouteId = routeIds.getOrNull(displayedIndex - 1) },
                                            enabled = displayedIndex > 0,
                                        ) { RoutyIcon(Glyph.Chevron, modifier = Modifier.graphicsLayer(rotationZ = 180f)) }
                                    }
                                    Text(
                                        route.name.resolve(strings.language, route.id),
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    if (routeIds.size > 1) {
                                        IconButton(
                                            onClick = { routeInfoRouteId = routeIds.getOrNull(displayedIndex + 1) },
                                            enabled = displayedIndex in 0 until routeIds.lastIndex,
                                        ) { RoutyIcon(Glyph.Chevron) }
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { model.actionHandler(MapAction.ToggleRouteFavorite(route.id)) },
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                ) {
                                    RoutyIcon(
                                        if (route.id in state.favoriteRouteIds) Glyph.StarFilled else Glyph.Star,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings[if (route.id in state.favoriteRouteIds) TextKey.Saved else TextKey.Save])
                                }
                            }
                        }
                }
                routeFrequency?.let { frequency ->
                    Text(
                        strings.runsEvery(frequency),
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    strings[TextKey.Live],
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when {
                        vehicles.isLoading -> strings[TextKey.Loading]
                        vehicles.isStale -> strings[TextKey.Stale]
                        activeVehicles.isEmpty() -> strings[TextKey.NoBuses]
                        else -> strings.vehiclesCount(activeVehicles.size)
                    },
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (activeVehicles.isNotEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(activeVehicles, key = { it.id }) { vehicle ->
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
                        routeScheduleVisible = true
                        scope.launch { routeInfoSheetState.expand() }
                    },
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
                ) { Text(strings[TextKey.Details]) }
            }
        }
    }
    if (state.search.isOpen) {
        val hasCurrentSearchResults = state.search.query == searchFieldQuery
        ModalBottomSheet(
            onDismissRequest = { model.actionHandler(MapAction.CloseSearch) },
            sheetState = searchSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
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
                                val selected = route.id in state.selectedRouteIds
                                val routeColor = palette.routeColors[state.routeColorIndex(route.id).mod(palette.routeColors.size)]
                                val selectedContentColor = if (routeColor.luminance() > 0.45f) Color(0xFF0F172A) else Color.White
                                FilterChip(
                                    selected = selected,
                                    enabled = selected || state.selectedRouteIds.size < 7,
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
                                                CompositionLocalProvider(
                                                    LocalContentColor provides
                                                        if (selected) selectedContentColor else MaterialTheme.colorScheme.tertiary,
                                                ) {
                                                    RoutyIcon(
                                                        Glyph.StarFilled,
                                                        strings[TextKey.Favorites],
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(route.name.resolve(strings.language, route.id))
                                        }
                                    },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            selectedContainerColor = routeColor,
                                            selectedLabelColor = selectedContentColor,
                                            selectedLeadingIconColor = selectedContentColor,
                                        ),
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
            containerColor = MaterialTheme.colorScheme.surface,
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
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("${strings[TextKey.Vehicle]} $id") },
                text = { Text(strings[if (vehicles.isStale || vehicle == null) TextKey.Stale else TextKey.Live]) },
                confirmButton = {
                    TextButton({
                        trackingVehicleId = id
                        trackingStopId = null
                        busReachedTrackingStop = false
                        // A new tracking flow must not inherit a pending map-stop selection.
                        selectedStop = null
                        pickingTrackingStop = false
                        stopPickerVisible = false
                        vehicleDetailsVisible = false
                        trackingSetupVisible = true
                    }) { Text(strings[TextKey.TrackBus]) }
                },
                dismissButton = {
                    TextButton({
                        vehicle?.let {
                            scope.launch {
                                mapState.animateCameraPosition(CameraPosition(target = Position(it.position.longitude, it.position.latitude), zoom = 16.0))
                            }
                        }
                        vehicleDetailsVisible = false
                    }) { Text(strings[TextKey.ShowMap]) }
                },
            )
        }
    }
    if (trackingSetupVisible) {
        AlertDialog(
            onDismissRequest = { trackingSetupVisible = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings[TextKey.TrackBus]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings[TextKey.TrackBusBody])
                    FilledTonalButton(onClick = { stopPickerVisible = true }, modifier = Modifier.fillMaxWidth()) {
                        RoutyIcon(Glyph.Stop)
                        Spacer(Modifier.width(8.dp))
                        Text(trackingStop?.let { it.name.resolve(strings.language, it.id) } ?: strings[TextKey.ChooseStop])
                    }
                    TextButton(onClick = {
                        trackingSetupVisible = false
                        pickingTrackingStop = true
                    }) { Text(strings[TextKey.ChooseStopOnMap]) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        requestBusNotificationPermission()
                        model.actionHandler(MapAction.SetTracking(trackingVehicleId, trackingStopId, trackingVehicle?.routeId))
                        selectedStop = trackingStopId
                        trackingSetupVisible = false
                    },
                    enabled = trackingStop != null,
                ) { Text(strings[TextKey.StartTracking]) }
            },
            dismissButton = {
                if (trackingStopId != null) TextButton(onClick = {
                    trackingVehicleId = null
                    trackingStopId = null
                    selectedStop = null
                    busReachedTrackingStop = false
                    model.actionHandler(MapAction.SetTracking(null, null, null))
                    trackingSetupVisible = false
                }) { Text(strings[TextKey.StopTracking]) }
            },
        )
    }
    if (stopPickerVisible) {
        val allStops = state.network.network?.stops.orEmpty()
        val orderedStops = allStops.filter { it.id in state.favoriteStopIds } + allStops.filterNot { it.id in state.favoriteStopIds }
        val matchingStops = orderedStops.filter { stop -> stopPickerQuery.isBlank() || stop.name.matches(stopPickerQuery) || stop.number?.toString()?.contains(stopPickerQuery) == true }
        ModalBottomSheet(
            onDismissRequest = { stopPickerVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            LazyColumn(
                Modifier.fillMaxWidth().height(560.dp).imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(strings[TextKey.ChooseStop], style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    SearchField(
                        value = stopPickerQuery,
                        placeholder = strings[TextKey.SearchStops],
                        onChange = { stopPickerQuery = it },
                        onClear = { stopPickerQuery = "" },
                    )
                }
                if (stopPickerQuery.isBlank() && state.favoriteStopIds.isNotEmpty()) item {
                    Text(strings[TextKey.FavoriteStops], style = MaterialTheme.typography.titleMedium)
                }
                items(matchingStops, key = { it.id }) { stop ->
                    StopCard(
                        stop = stop,
                        subtitle = stop.services.map { it.routeId }.distinct().joinToString(" · "),
                        isFavorite = stop.id in state.favoriteStopIds,
                        onClick = {
                            trackingStopId = stop.id
                            selectedStop = stop.id
                            busReachedTrackingStop = false
                            pickingTrackingStop = false
                            stopPickerVisible = false
                        },
                    )
                }
                if (matchingStops.isEmpty()) item { SearchEmptyState() }
            }
        }
    }
}

private fun Double.toRadians(): Double = this * PI / 180

private fun distanceMeters(latitudeA: Double, longitudeA: Double, latitudeB: Double, longitudeB: Double): Double {
    val latitudeDelta = (latitudeB - latitudeA).toRadians()
    val longitudeDelta = (longitudeB - longitudeA).toRadians()
    val haversine = sin(latitudeDelta / 2).let { it * it } + cos(latitudeA.toRadians()) * cos(latitudeB.toRadians()) * sin(longitudeDelta / 2).let { it * it }
    return 6_371_000.0 * 2 * asin(sqrt(haversine))
}

@Composable
private fun MapZoomButton(
    label: String,
    symbol: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
