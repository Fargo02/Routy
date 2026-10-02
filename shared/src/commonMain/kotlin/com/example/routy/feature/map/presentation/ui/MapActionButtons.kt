package com.example.routy.feature.map.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.NetworkIssueBadge
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.feature.map.presentation.state.MapAction

@Composable
internal fun MapActionButtons(
    hasNetworkIssue: Boolean,
    hasSelectedRoutes: Boolean,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (hasNetworkIssue) {
            NetworkIssueBadge(onClick = { onAction(MapAction.Retry) })
        }
        if (hasSelectedRoutes) {
            SmallFloatingActionButton(
                { onAction(MapAction.OpenRouteInfo) },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                RoutyIcon(Glyph.Routes, strings[TextKey.Live])
            }
        }
        SmallFloatingActionButton(
            { onAction(MapAction.MyLocation) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            RoutyIcon(Glyph.Location, strings[TextKey.MyLocation])
        }
    }
}
