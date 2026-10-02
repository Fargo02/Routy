package com.example.routy.feature.favorites.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey

@Composable
internal fun FavoriteSwipeToDismiss(
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
        RemoveFavoriteDialog(
            onConfirm = {
                removalConfirmationVisible = false
                onDismiss()
            },
            onDismiss = { removalConfirmationVisible = false },
        )
    }
}
