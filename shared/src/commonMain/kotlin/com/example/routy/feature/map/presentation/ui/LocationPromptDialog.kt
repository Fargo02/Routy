package com.example.routy.feature.map.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey

@Composable
internal fun LocationPromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(strings[TextKey.MyLocation]) },
        text = { Text(strings[TextKey.LocationHelp]) },
        confirmButton = {
            TextButton(onConfirm) { Text(strings[TextKey.MyLocation]) }
        },
        dismissButton = {
            TextButton(onDismiss) { Text(strings[TextKey.Close]) }
        },
    )
}
