package com.example.routy.feature.stop_details.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.example.routy.core.navigation.Destination
import com.example.routy.core.navigation.navigateIfResumed
import com.example.routy.core.transport.domain.ObserveTransportUseCase
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.stop_details.domain.GetStopDetailsUseCase
import com.example.routy.feature.stop_details.presentation.StopDetailsScreen
import com.example.routy.feature.stop_details.presentation.StopDetailsViewModel
import kotlinx.serialization.Serializable

@Serializable
data class StopDetails(
    val stopId: String,
) : Destination

fun NavController.navigateToStopDetailsScreen(stopId: String) = navigateIfResumed(StopDetails(stopId))

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.stopDetailsScreen(
    transport: ObserveTransportUseCase,
    favorites: FavoritesUseCase,
    navigate: (Destination) -> Unit,
    onDismiss: () -> Unit,
    message: suspend (String) -> Unit,
) {
    dialog<StopDetails> { entry ->
        val route = entry.toRoute<StopDetails>()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
            sheetState =
                rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                ),
        ) {
            StopDetailsScreen(
                viewModel(key = "stop:${route.stopId}") {
                    StopDetailsViewModel(route.stopId, transport, GetStopDetailsUseCase(), favorites)
                },
                navigate,
                message,
            )
        }
    }
}
