package com.example.routy.feature.map.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.routy.core.designsystem.NetworkIssueBadge
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.transport.domain.Vehicle

@Composable
internal fun VehicleDetailsDialog(
    vehicleId: String,
    vehicle: Vehicle?,
    isStale: Boolean,
    onShowOnMap: (Vehicle) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("${strings[TextKey.Vehicle]} $vehicleId") },
        text = {
            if (isStale || vehicle == null) {
                NetworkIssueBadge()
            } else {
                Text(strings[TextKey.Live])
            }
        },
        confirmButton = {
            TextButton({
                vehicle?.let(onShowOnMap)
                onDismiss()
            }) { Text(strings[TextKey.ShowMap]) }
        },
        dismissButton = {
            TextButton(onDismiss) { Text(strings[TextKey.Close]) }
        },
    )
}
