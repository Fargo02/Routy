package com.example.routy.feature.favorites.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.LocalRoutyPalette
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.transport.domain.BusStop

internal const val FAVORITE_CARD_ALPHA = 0.55f

internal data class UpcomingBus(
    val routeName: String,
    val minutes: Int,
    val routeColorIndex: Int,
)

@Composable
internal fun FavoriteStopCard(
    stop: BusStop,
    upcomingBuses: List<UpcomingBus>,
    fallbackSubtitle: String,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = FAVORITE_CARD_ALPHA)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoutyIcon(Glyph.Stop, modifier = Modifier.size(28.dp))
                Text(
                    stop.name.resolve(strings.language, stop.id),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                RoutyIcon(Glyph.Chevron, strings[TextKey.Details])
            }
            if (upcomingBuses.isEmpty()) {
                Text(
                    fallbackSubtitle,
                    modifier = Modifier.padding(start = 42.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(start = 42.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    upcomingBuses.forEach { bus ->
                        val color = palette.routeColors[bus.routeColorIndex % palette.routeColors.size]
                        Surface(
                            color = color.copy(alpha = 0.13f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                "${bus.routeName} · ${strings.minutesShort(bus.minutes)}",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
