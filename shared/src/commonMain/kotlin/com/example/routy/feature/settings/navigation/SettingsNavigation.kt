package com.example.routy.feature.settings.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.localization.TextKey
import com.example.routy.core.navigation.BottomNavigationItem
import com.example.routy.core.navigation.navigateIfResumed
import com.example.routy.feature.settings.presentation.SettingsScreen
import com.example.routy.feature.settings.presentation.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Settings : BottomNavigationItem {
    override val icon = Glyph.Settings
    override val title = TextKey.Settings
}

fun NavController.navigateToSettingsScreen() =
    navigateIfResumed(Settings) {
        popUpTo(graph.id)
        launchSingleTop = true
    }

fun NavGraphBuilder.settingsScreen(model: SettingsViewModel) {
    composable<Settings>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        SettingsScreen(model)
    }
}
