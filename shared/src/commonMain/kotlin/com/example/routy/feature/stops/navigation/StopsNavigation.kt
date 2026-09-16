package com.example.routy.feature.stops.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.routy.core.navigation.Destination
import com.example.routy.core.navigation.navigateIfResumed
import com.example.routy.core.transport.domain.ObserveTransportUseCase
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import com.example.routy.feature.stops.presentation.StopsScreen
import com.example.routy.feature.stops.presentation.StopsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Stops : Destination

fun NavController.navigateToStopsScreen() = navigateIfResumed(Stops) { launchSingleTop = true }

fun NavGraphBuilder.stopsScreen(
    transport: ObserveTransportUseCase,
    navigate: (Destination) -> Unit,
) {
    composable<Stops>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        StopsScreen(
            viewModel { StopsViewModel(transport, SearchStopsUseCase()) },
            navigate,
        )
    }
}
