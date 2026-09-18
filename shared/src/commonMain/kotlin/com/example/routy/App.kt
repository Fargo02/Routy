package com.example.routy

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.routy.core.navigation.Destination
import com.example.routy.core.navigation.navigateTo
import com.example.routy.feature.favorites.navigation.Favorites
import com.example.routy.feature.favorites.navigation.favoritesScreen
import com.example.routy.feature.map.navigation.Map
import com.example.routy.feature.map.navigation.mapScreen
import com.example.routy.feature.map.navigation.navigateToMapScreen
import com.example.routy.feature.map.presentation.MapScreen
import com.example.routy.feature.map.presentation.MapViewModel
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.route_details.navigation.routeDetailsScreen
import com.example.routy.feature.settings.navigation.Settings
import com.example.routy.feature.settings.navigation.settingsScreen
import com.example.routy.feature.settings.presentation.SettingsViewModel
import com.example.routy.feature.settings.presentation.state.SettingsEffect
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stop_details.navigation.stopDetailsScreen
import com.example.routy.feature.stops.navigation.Stops
import com.example.routy.feature.stops.navigation.stopsScreen

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
    val map: MapViewModel =
        viewModel {
            MapViewModel(
                graph.transport,
                graph.vehicles,
                GetRouteDetailsUseCase(),
                graph.favorites,
                graph.mapStyle,
                savedState = createSavedStateHandle(),
                logger = graph.logger,
            )
        }

    LaunchedEffect(owner, graph) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { graph.transport.foreground().collect { } }
    }
    PlatformBackHandler(navController.previousBackStackEntry != null) { navController.popBackStack() }

    CompositionLocalProvider(LocalStrings provides strings) {
        RoutyTheme(preferences.appearance, preferences.colorTheme) {
            CollectEffects(settings.effects) {
                when (it) {
                    is SettingsEffect.Error -> snackbar.showSnackbar(strings.error(it.error))
                }
            }
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }) {
                Box(Modifier.fillMaxSize()) {
                    MapScreen(
                        model = map,
                        navigate = navController::navigateTo,
                        isActive = visibleDestination.hasRouteType<Map>(),
                    )
                    NavHost(navController = navController, startDestination = Map) {
                        mapScreen()
                        stopsScreen(graph.transport, navController::navigateTo)
                        favoritesScreen(
                            graph.transport,
                            graph.favorites,
                            showStopOnMap = { id ->
                                map.actionHandler(MapAction.ShowStopOnMap(id))
                                navController.navigateToMapScreen()
                            },
                            showRouteOnMap = { id ->
                                map.actionHandler(MapAction.SelectRoute(id))
                                navController.navigateToMapScreen()
                            },
                        )
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
                            showRouteOnMap = { id ->
                                map.actionHandler(MapAction.ShowRoute(id))
                                navController.popBackStack(Map, inclusive = false)
                            },
                            onDismiss = {
                                map.actionHandler(MapAction.ClearSelectedStop)
                                navController.popBackStack()
                            },
                            message = { snackbar.showSnackbar(it) },
                        )
                    }
                    FloatingBottomNavigation(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        items = listOf(Map, Favorites, Settings),
                        selected = visibleDestination,
                        onNavigate = { destination ->
                            val returningToMap =
                                destination == Map &&
                                    (visibleDestination.hasRouteType<Favorites>() || visibleDestination.hasRouteType<Settings>())
                            if (returningToMap) navController.popBackStack() else navController.navigateTo(destination)
                        },
                    )
                }
            }
        }
    }
}

private fun NavDestination?.matches(item: BottomNavigationItem): Boolean =
    when (item) {
        Map -> hasRouteType<Map>()
        Favorites -> hasRouteType<Favorites>()
        Settings -> hasRouteType<Settings>()
        else -> false
    }

@Composable
private fun FloatingBottomNavigation(
    modifier: Modifier = Modifier,
    items: List<BottomNavigationItem>,
    selected: NavDestination?,
    onNavigate: (Destination) -> Unit,
) {
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(40.dp),
            shadowElevation = 12.dp,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 8.dp)) {
                val selectedIndex = items.indexOfFirst { selected.matches(it) }.coerceAtLeast(0)
                val itemSlotWidth = 64.dp
                val gap = (maxWidth - itemSlotWidth * items.size) / (items.size + 1)
                val targetOffset = gap + (itemSlotWidth + gap) * selectedIndex + 6.dp
                val selectionOffset by
                    animateDpAsState(
                        targetValue = targetOffset,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        label = "bottom-nav-selection",
                )
                Surface(
                    modifier = Modifier.offset(x = selectionOffset, y = 12.dp).size(52.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                ) {}
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEach { item ->
                        val isSelected = selected.matches(item)
                        Box(Modifier.width(itemSlotWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Surface(
                                onClick = { onNavigate(item) },
                                modifier = Modifier.size(52.dp).semantics { contentDescription = item.title.name },
                                color = Color.Transparent,
                                contentColor =
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            ) {
                                Box(contentAlignment = Alignment.Center) { RoutyIcon(item.icon) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun NavDestination?.title(): TextKey =
    when {
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
