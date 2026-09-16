package com.example.routy.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.localization.TextKey
import com.example.routy.feature.favorites.navigation.Favorites
import com.example.routy.feature.favorites.navigation.navigateToFavoritesScreen
import com.example.routy.feature.map.navigation.Map
import com.example.routy.feature.map.navigation.navigateToMapScreen
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.route_details.navigation.navigateToRouteDetailsScreen
import com.example.routy.feature.settings.navigation.Settings
import com.example.routy.feature.settings.navigation.navigateToSettingsScreen
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stop_details.navigation.navigateToStopDetailsScreen
import com.example.routy.feature.stops.navigation.Stops
import com.example.routy.feature.stops.navigation.navigateToStopsScreen

interface Destination

interface BottomNavigationItem : Destination {
    val icon: Glyph
    val title: TextKey
}

inline fun <reified T : Any> NavController.navigateIfResumed(
    route: T,
    noinline builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route, builder)
    }
}

fun NavController.navigateTo(destination: Destination) {
    when (destination) {
        Map -> navigateToMapScreen()
        Stops -> navigateToStopsScreen()
        Favorites -> navigateToFavoritesScreen()
        Settings -> navigateToSettingsScreen()
        is RouteDetails -> navigateToRouteDetailsScreen(destination.routeId)
        is StopDetails -> navigateToStopDetailsScreen(destination.stopId)
        else -> error("Unsupported destination: $destination")
    }
}
