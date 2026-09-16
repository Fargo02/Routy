package com.example.routy.feature.route_details.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.routy.core.navigation.Destination
import com.example.routy.core.navigation.navigateIfResumed
import com.example.routy.core.transport.domain.ObserveRouteVehiclesUseCase
import com.example.routy.core.transport.domain.ObserveTransportUseCase
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.presentation.RouteDetailsScreen
import com.example.routy.feature.route_details.presentation.RouteDetailsViewModel
import kotlinx.serialization.Serializable

@Serializable
data class RouteDetails(
    val routeId: String,
) : Destination

fun NavController.navigateToRouteDetailsScreen(routeId: String) = navigateIfResumed(RouteDetails(routeId))

fun NavGraphBuilder.routeDetailsScreen(
    transport: ObserveTransportUseCase,
    favorites: FavoritesUseCase,
    vehicles: ObserveRouteVehiclesUseCase,
    navigate: (Destination) -> Unit,
    showMap: (String) -> Unit,
    message: suspend (String) -> Unit,
) {
    composable<RouteDetails>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) { entry ->
        val route = entry.toRoute<RouteDetails>()
        RouteDetailsScreen(
            viewModel(key = "route:${route.routeId}") {
                RouteDetailsViewModel(
                    route.routeId,
                    transport,
                    GetRouteDetailsUseCase(),
                    favorites,
                    vehicles,
                )
            },
            navigate,
            showMap,
            message,
        )
    }
}
