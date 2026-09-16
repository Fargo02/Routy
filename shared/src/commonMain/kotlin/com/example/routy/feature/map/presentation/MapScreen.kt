package com.example.routy.feature.map.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.map.presentation.state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.*
import org.maplibre.compose.location.*
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.*
import org.maplibre.compose.style.BaseStyle
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
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val vehicles by model.vehicles.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    val scope = rememberCoroutineScope()
    var locationEnabled by rememberSaveable { mutableStateOf(false) }
    var focusLocation by remember { mutableStateOf(false) }
    var vehicleDetailsVisible by rememberSaveable { mutableStateOf(false) }
    var selectedVehicle by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStop by rememberSaveable { mutableStateOf<String?>(null) }

    var mapError by remember { mutableStateOf(false) }
    var lastFittedRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val location = rememberLocationState(enabled = locationEnabled)
    val systemSettings = rememberSystemSettingsLauncher()
    var locationPrompt by remember { mutableStateOf(false) }
    val vehicleJson by produceState(EMPTY_GEOJSON, vehicles.vehicles) {
        value = withContext(Dispatchers.Default) { vehiclesGeoJson(vehicles.vehicles) }
    }
    val selectedStopJson = remember(state.stops, selectedStop) { stopsGeoJson(state.stops.filter { it.id == selectedStop }) }
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
            initialCameraPosition = CameraPosition(target = Position(longitude = 41.6367, latitude = 41.6461), zoom = 13.0),
        ) {
            val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(state.routeGeoJson))
            val stopSource = rememberGeoJsonSource(GeoJsonData.JsonString(state.stopGeoJson))
            val vehicleSource = rememberGeoJsonSource(GeoJsonData.JsonString(vehicleJson))
            val selectionSource = rememberGeoJsonSource(GeoJsonData.JsonString(selectedStopJson))
            val vehicleSelectionSource = rememberGeoJsonSource(GeoJsonData.JsonString(selectedVehicleJson))
            LineLayer("route-outline", routeSource, color = const(palette.routeOutline), width = const(8.dp))
            LineLayer("route", routeSource, color = const(palette.route), width = const(5.dp))
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
            CircleLayer(
                "vehicles",
                vehicleSource,
                color = const(MaterialTheme.colorScheme.onSurface),
                radius = const(9.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp),
                hitPadding = 18.dp,
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

    suspend fun fitSelectedRoute() {
        state.geometry?.points?.takeIf { it.isNotEmpty() }?.let { points ->
            mapState.animateCameraToBounds(
                org.maplibre.spatialk.geojson.BoundingBox(
                    west = points.minOf { it.longitude },
                    south = points.minOf { it.latitude },
                    east = points.maxOf { it.longitude },
                    north = points.maxOf { it.latitude },
                ),
                padding = PaddingValues(70.dp),
            )
        }
    }
    CollectEffects(model.effects) {
        when (it) {
            is MapEffect.Navigate -> navigate(it.destination)
            is MapEffect.ShowVehicle -> {
                selectedVehicle = it.id
                vehicleDetailsVisible = true
            }
            MapEffect.FitRoute -> fitSelectedRoute()
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
    LaunchedEffect(mapState) { mapState.events.collect { if (it is MapEvent.StyleLoadFailed) mapError = true } }
    LaunchedEffect(state.routeId, state.geometry?.routeId) {
        selectedVehicle = null
        vehicleDetailsVisible = false
        if (state.routeId == null) lastFittedRoute = null
        if (state.geometry != null && state.routeId != lastFittedRoute) {
            fitSelectedRoute()
            lastFittedRoute = state.routeId
        }
    }
    LaunchedEffect(selectedStop) {
        state.stops.firstOrNull { it.id == selectedStop }?.let {
            mapState.animateCameraPosition(mapState.cameraPosition.copy(target = Position(it.position.longitude, it.position.latitude)))
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
                modifier = Modifier.fillMaxSize().semantics { contentDescription = strings[TextKey.Map] },
                state = mapState,
                overlay = {},
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = screenPadding.calculateTopPadding() + 16.dp, end = 16.dp),
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
                        Text(strings[TextKey.Search], Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            state.routeId == null,
                            { model.actionHandler(MapAction.SelectRoute(null)) },
                            label = { Text(strings[TextKey.AllRoutes]) },
                        )
                    }
                    items(
                        state.network.network
                            ?.routes
                            .orEmpty(),
                        key = { it.id },
                    ) { route ->
                        FilterChip(state.routeId == route.id, {
                            model.actionHandler(MapAction.SelectRoute(route.id))
                        }, label = {
                            Text(route.name.resolve(strings.language, route.id))
                        }, colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface))
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
                if (state.routeId != null) {
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings[TextKey.Live], style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    vehicles.isLoading -> strings[TextKey.Loading]
                                    vehicles.isStale -> strings[TextKey.Stale]
                                    vehicles.vehicles.isEmpty() -> strings[TextKey.NoBuses]
                                    else -> "${vehicles.vehicles.size} ${strings[TextKey.Vehicle]}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(vehicles.vehicles, key = { it.id }) { vehicle ->
                                    AssistChip(
                                        { model.actionHandler(MapAction.SelectVehicle(vehicle.id)) },
                                        label = { Text("${strings[TextKey.Vehicle]} ${vehicle.id}") },
                                    )
                                }
                            }
                            TextButton({ model.actionHandler(MapAction.OpenRouteDetails) }) { Text(strings[TextKey.Details]) }
                        }
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.geometry !=
                    null
                ) {
                    SmallFloatingActionButton(
                        { model.actionHandler(MapAction.FitRoute) },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        RoutyIcon(Glyph.Routes, strings[TextKey.FitRoute])
                    }
                }
                SmallFloatingActionButton({
                    model.actionHandler(MapAction.OpenStops)
                }, containerColor = MaterialTheme.colorScheme.surface) { RoutyIcon(Glyph.Stop, strings[TextKey.Nearby]) }
                SmallFloatingActionButton({
                    model.actionHandler(MapAction.MyLocation)
                }, containerColor = MaterialTheme.colorScheme.surface) { RoutyIcon(Glyph.Location, strings[TextKey.MyLocation]) }
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
            dismissButton = { TextButton({ locationPrompt = false }) { Text(strings[TextKey.Close]) } },
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
                                    CameraPosition(target = Position(it.position.longitude, it.position.latitude), zoom = 16.0),
                                )
                            }
                        }
                        selectedVehicle =
                            null
                    }) { Text(strings[TextKey.ShowMap]) }
                },
                dismissButton = { TextButton({ vehicleDetailsVisible = false }) { Text(strings[TextKey.Close]) } },
            )
        }
    }
}
