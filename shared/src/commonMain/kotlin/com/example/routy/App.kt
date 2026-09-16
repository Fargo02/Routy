package com.example.routy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routy.core.designsystem.*
import com.example.routy.core.di.AppGraph
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.favorites.presentation.*
import com.example.routy.feature.map.presentation.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.presentation.*
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.routes.presentation.*
import com.example.routy.feature.settings.presentation.*
import com.example.routy.feature.stop_details.domain.GetStopDetailsUseCase
import com.example.routy.feature.stop_details.presentation.*
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import com.example.routy.feature.stops.presentation.*
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(graph: AppGraph) {
    val settings: SettingsViewModel = viewModel { SettingsViewModel(graph.settings) }
    val preferences by settings.state.collectAsStateWithLifecycle()
    val strings = remember(preferences.language) { Strings(preferences.language) }
    val snackbar = remember { SnackbarHostState() }
    var stackJson by rememberSaveable { mutableStateOf(Json.encodeToString<List<Destination>>(listOf(Destination.Map))) }
    val stack = remember(stackJson) { Json.decodeFromString<List<Destination>>(stackJson) }

    fun navigate(destination: Destination) {
        stackJson = Json.encodeToString(stack + destination)
    }

    fun back() {
        if (stack.size > 1) stackJson = Json.encodeToString(stack.dropLast(1))
    }
    val current = stack.last()
    val underlying = if (current is Destination.StopDetails) stack.dropLast(1).lastOrNull() ?: Destination.Map else current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, graph) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { graph.transport.foreground().collect { } }
    }
    PlatformBackHandler(stack.size > 1) { back() }
    val map: MapViewModel = viewModel { MapViewModel(graph.transport, graph.vehicles, GetRouteDetailsUseCase()) }
    var routeId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(routeId) { if (routeId != null) map.accept(MapIntent.SelectRoute(routeId)) }
    CompositionLocalProvider(LocalStrings provides strings) {
        RoutyTheme(preferences.appearance) {
            CollectEffects(settings.effects) {
                when (it) {
                    is SettingsEffect.Error -> snackbar.showSnackbar(strings.error(it.error))
                }
            }
            Scaffold(
                topBar = {
                    if (underlying != Destination.Map) {
                        TopAppBar(
                            title = {
                                Text(
                                    strings[
                                        when (underlying) {
                                            Destination.Routes -> TextKey.Routes
                                            Destination.Stops -> TextKey.Stops
                                            Destination.Favorites -> TextKey.Favorites
                                            Destination.Settings -> TextKey.Settings
                                            is Destination.RouteDetails -> TextKey.Routes
                                            else -> TextKey.AppName
                                        },
                                    ],
                                )
                            },
                            navigationIcon = {
                                if (stack.size >
                                    1
                                ) {
                                    IconButton({ back() }) { RoutyIcon(Glyph.Back, strings[TextKey.Back]) }
                                }
                            },
                            actions = {
                                IconButton(
                                    { navigate(Destination.Settings) },
                                ) { RoutyIcon(Glyph.Settings, strings[TextKey.Settings]) }
                            },
                        )
                    }
                },
                bottomBar = {
                    NavigationBar {
                        listOf(
                            Triple(Destination.Map, TextKey.Map, Glyph.Map),
                            Triple(Destination.Routes, TextKey.Routes, Glyph.Routes),
                            Triple(Destination.Favorites, TextKey.Favorites, Glyph.Star),
                        ).forEach { (destination, text, glyph) ->
                            NavigationBarItem(current == destination, {
                                stackJson =
                                    Json.encodeToString<List<Destination>>(listOf(destination))
                            }, icon = { RoutyIcon(glyph) }, label = { Text(strings[text]) })
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (underlying) {
                        Destination.Map -> MapScreen(map, ::navigate)
                        Destination.Routes ->
                            RoutesScreen(
                                viewModel { RoutesViewModel(graph.transport, SearchRoutesUseCase()) },
                                ::navigate,
                            )
                        Destination.Stops -> StopsScreen(viewModel { StopsViewModel(graph.transport, SearchStopsUseCase()) }, ::navigate)
                        Destination.Favorites ->
                            FavoritesScreen(
                                viewModel { FavoritesViewModel(graph.transport, graph.favorites) },
                                ::navigate,
                            )
                        Destination.Settings -> SettingsScreen(settings)
                        is Destination.RouteDetails ->
                            RouteDetailsScreen(
                                viewModel(key = "route:${underlying.routeId}") {
                                    RouteDetailsViewModel(underlying.routeId, graph.transport, GetRouteDetailsUseCase(), graph.favorites)
                                },
                                ::navigate,
                                { id ->
                                    routeId = id
                                    map.accept(MapIntent.SelectRoute(id))
                                    stackJson =
                                        Json.encodeToString<List<Destination>>(listOf(Destination.Map))
                                },
                                { snackbar.showSnackbar(it) },
                            )
                        is Destination.StopDetails -> Unit
                    }
                }
            }
            if (current is Destination.StopDetails) {
                ModalBottomSheet(onDismissRequest = { back() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                    StopDetailsScreen(
                        viewModel(key = "stop:${current.stopId}") {
                            StopDetailsViewModel(current.stopId, graph.transport, GetStopDetailsUseCase(), graph.favorites)
                        },
                        { destination -> stackJson = Json.encodeToString(stack.dropLast(1) + destination) },
                        { snackbar.showSnackbar(it) },
                    )
                }
            }
        }
    }
}

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
