package com.example.routy.feature.favorites.navigation

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
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.favorites.presentation.FavoritesScreen
import com.example.routy.feature.favorites.presentation.FavoritesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Favorites : BottomNavigationItem {
    override val icon = Glyph.Star
    override val title = TextKey.Favorites
}

fun NavController.navigateToFavoritesScreen() =
    navigateIfResumed(Favorites) {
        popUpTo(graph.id)
        launchSingleTop = true
    }

fun NavGraphBuilder.favoritesScreen(
    transport: ObserveTransportUseCase,
    favorites: FavoritesUseCase,
    navigate: (Destination) -> Unit,
) {
    composable<Favorites>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        FavoritesScreen(
            viewModel { FavoritesViewModel(transport, favorites) },
            navigate,
        )
    }
}
