package com.example.routy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.designsystem.RoutyTheme
import com.example.routy.core.di.AppGraph
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.Strings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.BottomNavigationItem
import com.example.routy.core.navigation.navigateTo
import com.example.routy.feature.favorites.navigation.Favorites
import com.example.routy.feature.favorites.navigation.favoritesScreen
import com.example.routy.feature.map.navigation.Map
import com.example.routy.feature.map.navigation.mapScreen
import com.example.routy.feature.map.navigation.navigateToMapScreen
import com.example.routy.feature.map.presentation.MapViewModel
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.route_details.navigation.routeDetailsScreen
import com.example.routy.feature.routes.navigation.Routes
import com.example.routy.feature.routes.navigation.routesScreen
import com.example.routy.feature.settings.navigation.Settings
import com.example.routy.feature.settings.navigation.settingsScreen
import com.example.routy.feature.settings.presentation.SettingsViewModel
import com.example.routy.feature.settings.presentation.state.SettingsEffect
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stop_details.navigation.stopDetailsScreen
import com.example.routy.feature.stops.navigation.Stops
import com.example.routy.feature.stops.navigation.stopsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(graph: AppGraph) {
    val settings: SettingsViewModel = viewModel { SettingsViewModel(graph.settings) }
    val preferences by settings.uiState.collectAsStateWithLifecycle()
    val strings = remember(preferences.language) { Strings(preferences.language) }
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination
    val visibleDestination =
        if (currentDestination.hasRouteType<StopDetails>()) {
            navController.previousBackStackEntry?.destination
        } else {
            currentDestination
        }
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val map: MapViewModel =
        viewModel {
            MapViewModel(graph.transport, graph.vehicles, GetRouteDetailsUseCase(), createSavedStateHandle())
        }

    LaunchedEffect(owner, graph) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { graph.transport.foreground().collect { } }
    }
    PlatformBackHandler(navController.previousBackStackEntry != null) { navController.popBackStack() }

    CompositionLocalProvider(LocalStrings provides strings) {
        RoutyTheme(preferences.appearance) {
            CollectEffects(settings.effects) {
                when (it) {
                    is SettingsEffect.Error -> snackbar.showSnackbar(strings.error(it.error))
                }
            }
            Scaffold(
                topBar = {
                    if (!visibleDestination.hasRouteType<Map>()) {
                        TopAppBar(
                            title = { Text(strings[visibleDestination.title()]) },
                            navigationIcon = {
                                if (navController.previousBackStackEntry != null) {
                                    IconButton({ navController.popBackStack() }) {
                                        RoutyIcon(Glyph.Back, strings[TextKey.Back])
                                    }
                                }
                            },
                            actions = {
                                IconButton({ navController.navigateTo(Settings) }) {
                                    RoutyIcon(Glyph.Settings, strings[TextKey.Settings])
                                }
                            },
                        )
                    }
                },
                bottomBar = {
                    NavigationBar {
                        listOf<BottomNavigationItem>(Map, Routes, Favorites).forEach { item ->
                            NavigationBarItem(
                                selected = currentDestination.matches(item),
                                onClick = { navController.navigateTo(item) },
                                icon = { RoutyIcon(item.icon) },
                                label = { Text(strings[item.title]) },
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    NavHost(navController = navController, startDestination = Map) {
                        mapScreen(map, navController::navigateTo)
                        routesScreen(graph.transport, navController::navigateTo)
                        stopsScreen(graph.transport, navController::navigateTo)
                        favoritesScreen(graph.transport, graph.favorites, navController::navigateTo)
                        settingsScreen(settings)
                        routeDetailsScreen(
                            transport = graph.transport,
                            favorites = graph.favorites,
                            vehicles = graph.vehicles,
                            navigate = navController::navigateTo,
                            showMap = { id ->
                                map.actionHandler(MapAction.SelectRoute(id))
                                navController.navigateToMapScreen()
                            },
                            message = { snackbar.showSnackbar(it) },
                        )
                        stopDetailsScreen(
                            transport = graph.transport,
                            favorites = graph.favorites,
                            navigate = { destination ->
                                scope.launch {
                                    navController.popBackStack()
                                    navController.navigateTo(destination)
                                }
                            },
                            onDismiss = { navController.popBackStack() },
                            message = { snackbar.showSnackbar(it) },
                        )
                    }
                }
            }
        }
    }
}

private fun NavDestination?.matches(item: BottomNavigationItem): Boolean =
    when (item) {
        Map -> hasRouteType<Map>()
        Routes -> hasRouteType<Routes>()
        Favorites -> hasRouteType<Favorites>()
        else -> false
    }

private fun NavDestination?.title(): TextKey =
    when {
        hasRouteType<Routes>() -> TextKey.Routes
        hasRouteType<Stops>() -> TextKey.Stops
        hasRouteType<Favorites>() -> TextKey.Favorites
        hasRouteType<Settings>() -> TextKey.Settings
        hasRouteType<RouteDetails>() -> TextKey.Routes
        else -> TextKey.AppName
    }

private inline fun <reified T : Any> NavDestination?.hasRouteType(): Boolean =
    this?.route?.startsWith(T::class.qualifiedName.orEmpty()) == true

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
