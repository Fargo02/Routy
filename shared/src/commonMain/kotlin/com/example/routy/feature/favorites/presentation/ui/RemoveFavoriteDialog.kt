package com.example.routy.feature.favorites.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey

@Composable
internal fun RemoveFavoriteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(strings[TextKey.RemoveFavoriteTitle]) },
        text = { Text(strings[TextKey.RemoveFavoriteBody]) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings[TextKey.Remove]) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings[TextKey.Close]) } },
    )
}
