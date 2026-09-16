package com.example.routy.feature.routes.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.localization.TextKey
import com.example.routy.core.navigation.BottomNavigationItem
import com.example.routy.core.navigation.Destination
import com.example.routy.core.navigation.navigateIfResumed
import com.example.routy.core.transport.domain.ObserveTransportUseCase
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.routes.presentation.RoutesScreen
import com.example.routy.feature.routes.presentation.RoutesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Routes : BottomNavigationItem {
    override val icon = Glyph.Routes
    override val title = TextKey.Routes
}

fun NavController.navigateToRoutesScreen() =
    navigateIfResumed(Routes) {
        popUpTo(graph.id)
        launchSingleTop = true
    }

fun NavGraphBuilder.routesScreen(
    transport: ObserveTransportUseCase,
    navigate: (Destination) -> Unit,
) {
    composable<Routes>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        RoutesScreen(
            viewModel { RoutesViewModel(transport, SearchRoutesUseCase()) },
            navigate,
        )
    }
}
