package com.example.routy.feature.map.presentation.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.routy.core.designsystem.Glyph
import com.example.routy.core.designsystem.LocalRoutyPalette
import com.example.routy.core.designsystem.RoutyIcon
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.transport.domain.Route

@Composable
internal fun MapRouteChip(
    route: Route,
    colorIndex: Int,
    selected: Boolean,
    enabled: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    showBorder: Boolean = true,
) {
    val strings = LocalStrings.current
    val palette = LocalRoutyPalette.current
    val routeColor = palette.routeColors[colorIndex.mod(palette.routeColors.size)]
    val selectedContentColor = if (routeColor.luminance() > 0.45f) Color(0xFF0F172A) else Color.White
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (favorite) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            if (selected) selectedContentColor else MaterialTheme.colorScheme.tertiary,
                    ) {
                        RoutyIcon(Glyph.StarFilled, strings[TextKey.Favorites])
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(route.name.resolve(strings.language, route.id))
            }
        },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedContainerColor = routeColor,
                selectedLabelColor = selectedContentColor,
                selectedLeadingIconColor = selectedContentColor,
            ),
        border = if (showBorder) FilterChipDefaults.filterChipBorder(enabled = enabled, selected = selected) else null,
    )
}
