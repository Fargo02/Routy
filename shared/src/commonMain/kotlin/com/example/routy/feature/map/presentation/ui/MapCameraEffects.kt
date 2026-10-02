package com.example.routy.feature.map.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.map.presentation.state.MapCamera
import com.example.routy.feature.map.presentation.state.MapState
import kotlinx.coroutines.delay
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationState
import org.maplibre.spatialk.geojson.Position
import org.maplibre.compose.map.MapState as MaplibreMapState

@Composable
internal fun MapCameraEffects(
    mapState: MaplibreMapState,
    state: MapState,
    location: LocationState,
    focusLocation: Boolean,
    onLocationFocused: () -> Unit,
    onAction: (MapAction) -> Unit,
) {
    var initialLocationCheckFinished by remember { mutableStateOf(false) }
    DisposableEffect(mapState) {
        onDispose {
            mapState.cameraPosition.let { position ->
                onAction(
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
            onAction(MapAction.LocationAvailable)
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
            onAction(MapAction.InitialCameraPlaced)
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
        onAction(MapAction.InitialCameraPlaced)
    }
    LaunchedEffect(focusLocation, location.lastLocation) {
        if (focusLocation) {
            location.lastLocation?.let {
                mapState.animateCameraPosition(CameraPosition(target = it.position, zoom = 15.0))
                onLocationFocused()
            }
        }
    }
}
