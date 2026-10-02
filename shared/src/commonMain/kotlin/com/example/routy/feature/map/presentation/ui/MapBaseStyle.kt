package com.example.routy.feature.map.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.routy.feature.map.presentation.MapStyleConfig
import org.maplibre.compose.style.BaseStyle

@Composable
internal fun rememberMapBaseStyle(
    style: MapStyleConfig,
    styleJson: String?,
    onLoadStyle: (String?) -> Unit,
): BaseStyle {
    val dark = MaterialTheme.colorScheme.background.red < 0.3f
    val styleUri = if (dark) style.dark else style.light
    LaunchedEffect(dark, styleUri) { onLoadStyle(styleUri.takeIf { dark }) }
    return remember(styleUri, styleJson) { styleJson?.let(BaseStyle::Json) ?: BaseStyle.Uri(styleUri) }
}
