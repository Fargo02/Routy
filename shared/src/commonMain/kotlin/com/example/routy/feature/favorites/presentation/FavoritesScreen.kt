package com.example.routy.feature.favorites.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.*
import com.example.routy.core.localization.*
import com.example.routy.core.mvi.CollectEffects
import com.example.routy.core.navigation.Destination
import com.example.routy.feature.favorites.presentation.state.*

@Composable
fun FavoritesScreen(
    model: FavoritesViewModel,
    navigate: (Destination) -> Unit,
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    CollectEffects(model.effects) {
        when (it) {
            is FavoritesEffect.Navigate -> navigate(it.destination)
        }
    }
    ScreenScaffold { screenPadding ->
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding =
                PaddingValues(
                    start = 20.dp,
                    top = screenPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = screenPadding.calculateBottomPadding() + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatusPanel(state.network) { model.actionHandler(FavoritesAction.Retry) } }
            if (state.routes.isEmpty() && state.stops.isEmpty() && state.network.network != null) item { EmptyPanel(TextKey.NoFavorites) }
            items(
                state.routes,
                key = { "route:${it.id}" },
            ) { route ->
                FavoriteSwipeToDismiss(
                    onDismiss = { model.actionHandler(FavoritesAction.RemoveRoute(route.id)) },
                ) {
                    RouteCard(route, { model.actionHandler(FavoritesAction.SelectRoute(route.id)) })
                }
            }
            items(
                state.stops,
                key = { "stop:${it.id}" },
            ) { stop ->
                FavoriteSwipeToDismiss(
                    onDismiss = { model.actionHandler(FavoritesAction.RemoveStop(stop.id)) },
                ) {
                    StopCard(stop, { model.actionHandler(FavoritesAction.SelectStop(stop.id)) })
                }
            }
        }
    }
}

@Composable
private fun FavoriteSwipeToDismiss(
    onDismiss: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val strings = LocalStrings.current
    var removalConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) removalConfirmationVisible = true
                false
            },
            positionalThreshold = { distance -> distance * 0.7f },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isDismissInProgress = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
            if (isDismissInProgress) {
                val alignment =
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment,
                ) {
                    Text(strings[TextKey.Remove], color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = content,
    )
    if (removalConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { removalConfirmationVisible = false },
            title = { Text(strings[TextKey.RemoveFavoriteTitle]) },
            text = { Text(strings[TextKey.RemoveFavoriteBody]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removalConfirmationVisible = false
                        onDismiss()
                    },
                ) { Text(strings[TextKey.Remove]) }
            },
            dismissButton = {
                TextButton(onClick = { removalConfirmationVisible = false }) {
                    Text(strings[TextKey.Close])
                }
            },
        )
    }
}
