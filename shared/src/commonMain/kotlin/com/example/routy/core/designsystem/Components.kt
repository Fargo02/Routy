package com.example.routy.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.routy.core.localization.*
import com.example.routy.core.transport.domain.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// Small, shared vector vocabulary; no platform icon fonts or bitmap dependencies.
enum class Glyph { Map, Routes, Star, StarFilled, Search, Settings, Back, Stop, Location, Close, Chevron, NoSignal }

@Composable
fun RoutyIcon(
    glyph: Glyph,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    Canvas(modifier.size(24.dp).then(if (label == null) Modifier else Modifier.semantics { contentDescription = label })) {
        val u = size.width / 24

        fun line(
            x: Float,
            y: Float,
            a: Float,
            b: Float,
        ) = drawLine(color, Offset(x * u, y * u), Offset(a * u, b * u), 2 * u)
        when (glyph) {
            Glyph.Search -> {
                drawCircle(color, 7 * u, Offset(10 * u, 10 * u), style = Stroke(2 * u))
                line(15f, 15f, 22f, 22f)
            }
            Glyph.Back -> {
                line(4f, 12f, 21f, 12f)
                line(4f, 12f, 11f, 5f)
                line(4f, 12f, 11f, 19f)
            }
            Glyph.Close -> {
                line(5f, 5f, 19f, 19f)
                line(5f, 19f, 19f, 5f)
            }
            Glyph.Chevron -> {
                line(9f, 5f, 16f, 12f)
                line(16f, 12f, 9f, 19f)
            }
            Glyph.Location -> {
                drawCircle(color, 7 * u, style = Stroke(2 * u))
                drawCircle(color, 2 * u)
                line(12f, 0f, 12f, 4f)
                line(12f, 20f, 12f, 24f)
                line(0f, 12f, 4f, 12f)
                line(20f, 12f, 24f, 12f)
            }
            Glyph.Stop -> {
                fun bar(
                    x: Float,
                    y: Float,
                    a: Float,
                    b: Float,
                    w: Float,
                ) = drawLine(color, Offset(x * u, y * u), Offset(a * u, b * u), w * u, StrokeCap.Round)

                val board = Stroke(1.9f * u, cap = StrokeCap.Round)
                val detail = Stroke(1.2f * u, cap = StrokeCap.Round)
                drawRoundRect(
                    color,
                    Offset(4 * u, 1.2f * u),
                    Size(16 * u, 14.4f * u),
                    androidx.compose.ui.geometry
                        .CornerRadius(4 * u),
                    style = board,
                )
                bar(12f, 15.6f, 12f, 22.3f, 1.9f)
                bar(8.4f, 22.3f, 15.6f, 22.3f, 1.9f)
                drawRoundRect(
                    color,
                    Offset(7.6f * u, 3.3f * u),
                    Size(8.8f * u, 9f * u),
                    androidx.compose.ui.geometry
                        .CornerRadius(2.4f * u),
                    style = detail,
                )
                bar(9.9f, 5.2f, 14.1f, 5.2f, 1.1f)
                drawRoundRect(
                    color,
                    Offset(9.1f * u, 6.6f * u),
                    Size(5.8f * u, 2.8f * u),
                    androidx.compose.ui.geometry
                        .CornerRadius(0.7f * u),
                    style = detail,
                )
                drawCircle(color, 0.75f * u, Offset(9.8f * u, 10.8f * u))
                drawCircle(color, 0.75f * u, Offset(14.2f * u, 10.8f * u))
                bar(9.7f, 12.9f, 9.7f, 13.7f, 1.1f)
                bar(14.3f, 12.9f, 14.3f, 13.7f, 1.1f)
                bar(6.9f, 6.1f, 6.9f, 7.5f, 1.1f)
                bar(17.1f, 6.1f, 17.1f, 7.5f, 1.1f)
            }
            Glyph.Routes -> {
                drawRoundRect(
                    color,
                    Offset(4 * u, 3 * u),
                    Size(16 * u, 17 * u),
                    androidx.compose.ui.geometry
                        .CornerRadius(3 * u),
                    style =
                        Stroke(
                            2 * u,
                        ),
                )
                line(4f, 12f, 20f, 12f)
                drawCircle(color, 1.5f * u, Offset(8 * u, 16 * u))
                drawCircle(color, 1.5f * u, Offset(16 * u, 16 * u))
                line(7f, 20f, 7f, 23f)
                line(17f, 20f, 17f, 23f)
            }
            Glyph.Map -> {
                val p =
                    Path().apply {
                        moveTo(2 * u, 5 * u)
                        lineTo(8 * u, 2 * u)
                        lineTo(16 * u, 5 * u)
                        lineTo(22 * u, 2 * u)
                        lineTo(22 * u, 19 * u)
                        lineTo(
                            16 * u,
                            22 * u,
                        )
                        lineTo(8 * u, 19 * u)
                        lineTo(2 * u, 22 * u)
                        close()
                    }
                ;drawPath(p, color, style = Stroke(1.6f * u))
                line(8f, 2f, 8f, 19f)
                line(16f, 5f, 16f, 22f)
            }
            Glyph.Star -> {
                val p = Path()
                for (i in 0..9) {
                    val a =
                        (i * 36 - 90) * kotlin.math.PI / 180
                    val r =
                        if (i % 2 ==
                            0
                        ) {
                            10.0
                        } else {
                            4.5
                        }
                    ;val x =
                        (12 + kotlin.math.cos(a) * r).toFloat() * u
                    val y =
                        (12 + kotlin.math.sin(a) * r).toFloat() * u
                    if (i ==
                        0
                    ) {
                        p.moveTo(x, y)
                    } else {
                        p.lineTo(x, y)
                    }
                }
                p.close()
                drawPath(p, color, style = Stroke(1.8f * u))
            }
            Glyph.StarFilled -> {
                val p = Path()
                for (i in 0..9) {
                    val a = (i * 36 - 90) * kotlin.math.PI / 180
                    val r = if (i % 2 == 0) 10.0 else 4.5
                    val x = (12 + kotlin.math.cos(a) * r).toFloat() * u
                    val y = (12 + kotlin.math.sin(a) * r).toFloat() * u
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, color)
            }
            Glyph.NoSignal -> {
                for (r in listOf(4f, 8f, 12f)) {
                    drawArc(
                        color,
                        205f,
                        130f,
                        false,
                        Offset((12 - r) * u, (19 - r) * u),
                        Size(2 * r * u, 2 * r * u),
                        style = Stroke(2 * u),
                    )
                }
                drawCircle(color, 1.6f * u, Offset(12 * u, 19 * u))
                line(4f, 20f, 20f, 4f)
            }
            Glyph.Settings -> {
                drawCircle(color, 8 * u, style = Stroke(2 * u))
                drawCircle(color, 3 * u, style = Stroke(2 * u))
                for (i in 0..7) {
                    val a =
                        i * kotlin.math.PI / 4
                    line(
                        (12 + kotlin.math.cos(a) * 8).toFloat(),
                        (12 + kotlin.math.sin(a) * 8).toFloat(),
                        (12 + kotlin.math.cos(a) * 11).toFloat(),
                        (
                            12 +
                                kotlin.math.sin(a) * 11
                        ).toFloat(),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    OutlinedTextField(
        value,
        onChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { RoutyIcon(Glyph.Search) },
        trailingIcon =
            if (value.isNotEmpty() && onClear != null) {
                { IconButton(onClear) { RoutyIcon(Glyph.Close, strings[TextKey.Close]) } }
            } else {
                null
            },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}

@Composable
fun RouteCard(
    route: Route,
    onClick: () -> Unit,
    stopCount: Int? = null,
    subtitle: String? = null,
    trailingLabel: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val strings = LocalStrings.current
    Card(
        onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(
                    route.name.resolve(strings.language, route.id),
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(route.name.resolve(strings.language, route.id), style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle ?: stopCount?.let(strings::stopsCount) ?: strings[TextKey.Details],
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                trailingLabel?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                RoutyIcon(Glyph.Chevron, strings[TextKey.Details])
            }
        }
    }
}

@Composable
fun StopCard(
    stop: BusStop,
    onClick: () -> Unit,
    subtitle: String? = null,
    isFavorite: Boolean = false,
) {
    val strings = LocalStrings.current
    Card(onClick, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoutyIcon(Glyph.Stop, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(stop.name.resolve(strings.language, stop.id), style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle ?: listOfNotNull(stop.number?.toString(), strings[TextKey.Stops]).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isFavorite) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                    RoutyIcon(Glyph.StarFilled, strings[TextKey.Saved])
                }
            }
            RoutyIcon(Glyph.Chevron, strings[TextKey.Details])
        }
    }
}

@Composable
fun SearchEmptyState(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(strings[TextKey.SearchEmptyTitle], style = MaterialTheme.typography.titleMedium)
        Text(
            strings[TextKey.SearchEmptyBody],
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun NetworkIssueBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = strings[TextKey.Offline]
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onError) {
            RoutyIcon(Glyph.NoSignal, modifier = Modifier.size(size * 0.58f))
        }
    }
}

@Composable
fun StatusPanel(
    state: NetworkState,
    retry: () -> Unit,
) {
    val strings = LocalStrings.current
    val error = state.error
    when {
        state.network == null && error == null -> LoadingIndicator()
        error != null ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (error.isConnectivity) NetworkIssueBadge()
                    Column(Modifier.weight(1f)) {
                        Text(strings.error(error), style = MaterialTheme.typography.bodyMedium)
                        TextButton(retry) { Text(strings[TextKey.Retry]) }
                    }
                }
            }
        state.isRefreshing ->
            LinearProgressIndicator(
                Modifier.fillMaxWidth().semantics { contentDescription = strings[TextKey.Refreshing] },
            )
    }
}

@Composable
fun LoadingIndicator() {
    val strings = LocalStrings.current
    LinearProgressIndicator(
        Modifier.fillMaxWidth().semantics { contentDescription = strings[TextKey.Loading] },
    )
}

@Composable
fun EmptyPanel(key: TextKey = TextKey.NoResults) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings[TextKey.Empty], style = MaterialTheme.typography.headlineSmall)
        Text(strings[key], color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ScreenScaffold(
    hasTopBarOverlay: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { topBar?.invoke() },
    ) { systemPadding ->
        content(
            PaddingValues(
                top = systemPadding.calculateTopPadding() + if (hasTopBarOverlay && topBar == null) 64.dp else 0.dp,
                bottom = systemPadding.calculateBottomPadding() + 112.dp,
            ),
        )
    }
}

@Composable
fun ScreenBackdrop(
    light: DrawableResource,
    dark: DrawableResource,
    alignment: Alignment = Alignment.BottomCenter,
) {
    val surface = MaterialTheme.colorScheme.surface
    Image(
        painter = painterResource(if (LocalRoutyPalette.current.dark) dark else light),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = alignment,
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to surface.copy(alpha = 0.78f),
                0.5f to surface.copy(alpha = 0.52f),
                1f to surface.copy(alpha = 0.12f),
            ),
        ),
    )
}
