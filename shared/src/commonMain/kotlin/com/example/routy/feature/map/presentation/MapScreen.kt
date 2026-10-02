package com.example.routy.feature.map.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.PlatformBackHandler
import com.example.routy.core.designsystem.LocalRoutyPalette
import com.example.routy.core.designsystem.ScreenScaffold
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.ObserveAsEvents
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.Vehicle
import com.example.routy.core.transport.domain.VehicleState
import com.example.routy.core.transport.domain.hasConnectivityIssue
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapEffect
import com.example.routy.feature.map.presentation.ui.LocationPromptDialog
import com.example.routy.feature.map.presentation.ui.MapActionButtons
import com.example.routy.feature.map.presentation.ui.MapCameraEffects
import com.example.routy.feature.map.presentation.ui.MapSearchSheet
import com.example.routy.feature.map.presentation.ui.MapTopPanel
import com.example.routy.feature.map.presentation.ui.MapZoomControls
import com.example.routy.feature.map.presentation.ui.RouteInfoSheet
import com.example.routy.feature.map.presentation.ui.RouteLayers
import com.example.routy.feature.map.presentation.ui.StopLayers
import com.example.routy.feature.map.presentation.ui.VehicleDetailsDialog
import com.example.routy.feature.map.presentation.ui.VehicleLayers
import com.example.routy.feature.map.presentation.ui.rememberMapBaseStyle
import com.example.routy.feature.map.presentation.ui.rememberMapMarkerPainters
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.location.rememberSystemSettingsLauncher
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.spatialk.geojson.Position

/** Style provider is a replaceable presentation configuration, never a domain dependency. */
data class MapStyleConfig(
    val light: String = "https://tiles.openfreemap.org/styles/positron",
    val dark: String = "https://tiles.openfreemap.org/styles/dark",
)

@Composable
fun MapScreen(
    model: MapViewModel,
    navigate: (Destination) -> Unit,
    style: MapStyleConfig = MapStyleConfig(),
    isActive: Boolean = true,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val camera by model.camera.collectAsStateWithLifecycle()
    val mapStyleJson by model.mapStyleJson.collectAsStateWithLifecycle()
    val vehicles by
        model.vehicles.collectAsStateWithLifecycle(
            initialValue = VehicleState(isLoading = false),
            lifecycle = LocalLifecycleOwner.current.lifecycle,
        )
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    val scope = rememberCoroutineScope()
    var focusLocation by remember { mutableStateOf(false) }
    var locationPrompt by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf(false) }
    val location = rememberLocationState(enabled = state.isLocationEnabled)
    val systemSettings = rememberSystemSettingsLauncher()
    val painters = rememberMapMarkerPainters()
    val animatedVehicles = rememberAnimatedVehicles(vehicles)
    val vehicleRouteCount = remember(vehicles.vehicles) { vehicles.vehicles.distinctBy(Vehicle::routeId).size }
    val routeLabels =
        remember(state.network.network?.routes, strings.language) {
            state.network.network
                ?.routes
                ?.associate { route -> route.id to route.name.resolve(strings.language, route.id) }
                .orEmpty()
        }
    val baseStyle =
        rememberMapBaseStyle(style, mapStyleJson) { model.actionHandler(MapAction.LoadMapStyle(it)) }
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
                    target = Position(longitude = camera.longitude, latitude = camera.latitude),
                    zoom = camera.zoom,
                ),
        ) {
            RouteLayers(
                geometries = state.geometries,
                routeColor = { routeId ->
                    palette.routeColors[state.routeColorIndex(routeId).coerceAtLeast(0) % palette.routeColors.size]
                },
                outlineColor = palette.routeOutline,
            )
            StopLayers(
                stops = state.mapStops,
                selectedStopId = state.selectedStopId,
                favoriteStopIds = state.favoriteStopIds,
                strokeColor = palette.route,
                favoriteStopPainter = painters.favoriteStop,
                onStopClick = { model.actionHandler(MapAction.SelectStop(it)) },
            )
            VehicleLayers(
                vehicles = animatedVehicles,
                routeColorIndex = state::routeColorIndex,
                routeLabels = routeLabels,
                routeBusPainters = painters.routeBuses,
                busDirectionPainter = painters.bus,
                onAttached = model::logVehicleLayerAttached,
                onDetached = model::logVehicleLayerDetached,
                onVehicleClick = { model.actionHandler(MapAction.SelectVehicle(it)) },
            )
            if (state.isLocationEnabled) LocationPuck(idPrefix = "user", locationState = location)
        }

    PlatformBackHandler(enabled = isActive && state.selectedRouteIds.isNotEmpty()) {
        model.actionHandler(MapAction.ClearSelectedRoutes)
    }
    SideEffect {
        model.logVehicleLayersComposed(
            routeCount = vehicleRouteCount,
            vehicleCount = vehicles.vehicles.size,
            selectedRouteCount = state.selectedRouteIds.size,
        )
    }
    ObserveAsEvents(model.effects) {
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
    MapCameraEffects(
        mapState = mapState,
        state = state,
        location = location,
        focusLocation = focusLocation,
        onLocationFocused = { focusLocation = false },
        onAction = model::actionHandler,
    )

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
            MapTopPanel(
                state = state,
                isMapUnavailable = mapError,
                onAction = model::actionHandler,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = screenPadding.calculateTopPadding() + 16.dp),
            )
            MapZoomControls(
                onZoomIn = {
                    scope.launch {
                        mapState.animateCameraPosition(
                            mapState.cameraPosition.copy(zoom = (mapState.cameraPosition.zoom + 1.0).coerceAtMost(20.0)),
                        )
                    }
                },
                onZoomOut = {
                    scope.launch {
                        mapState.animateCameraPosition(
                            mapState.cameraPosition.copy(zoom = (mapState.cameraPosition.zoom - 1.0).coerceAtLeast(1.0)),
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            )
            MapActionButtons(
                hasNetworkIssue = state.network.hasConnectivityIssue || vehicles.isStale,
                hasSelectedRoutes = state.selectedRouteIds.isNotEmpty(),
                onAction = model::actionHandler,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 156.dp),
            )
        }
    }

    state.routeInfo?.let { routeInfo ->
        RouteInfoSheet(
            state = state,
            routeInfo = routeInfo,
            vehicles = vehicles,
            onAction = model::actionHandler,
        )
    }
    if (state.search.isOpen) {
        MapSearchSheet(
            state = state,
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
