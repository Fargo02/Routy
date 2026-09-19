package com.example.routy.feature.map.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.localization.TextKey
import com.example.routy.core.navigation.BottomNavigationItem
import com.example.routy.core.navigation.navigateIfResumed
import kotlinx.serialization.Serializable

@Serializable
data object Map : BottomNavigationItem {
    override val icon = Glyph.Map
    override val title = TextKey.Map
}

fun NavController.navigateToMapScreen() =
    navigateIfResumed(Map) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

fun NavGraphBuilder.mapScreen() {
    composable<Map>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) { }
}
