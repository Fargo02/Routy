package com.example.routy.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.ScreenScaffold
import com.example.routy.core.localization.*
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.preferences.domain.ColorTheme
import com.example.routy.core.transport.domain.Language
import com.example.routy.feature.settings.presentation.state.SettingsAction

@Composable
fun SettingsScreen(model: SettingsViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    ScreenScaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings[TextKey.Settings], style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            )
        },
    ) { screenPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    top = screenPadding.calculateTopPadding() + 24.dp,
                    end = 24.dp,
                    bottom = screenPadding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(strings[TextKey.Language], style = MaterialTheme.typography.titleLarge)
            Language.entries.forEach { language ->
                FilterChip(state.language == language, { model.actionHandler(SettingsAction.SetLanguage(language)) }, label = {
                    Text(
                        strings[
                            when (language) {
                                Language.English -> TextKey.English
                                Language.Georgian -> TextKey.Georgian
                                Language.Russian -> TextKey.Russian
                            },
                        ],
                    )
                })
            }
            HorizontalDivider()
            Text(strings[TextKey.ColorTheme], style = MaterialTheme.typography.titleLarge)
            ColorTheme.entries.forEach { colorTheme ->
                FilterChip(
                    selected = state.colorTheme == colorTheme,
                    onClick = { model.actionHandler(SettingsAction.SetColorTheme(colorTheme)) },
                    label = {
                        Text(
                            strings[
                                when (colorTheme) {
                                    ColorTheme.Ocean -> TextKey.Ocean
                                    ColorTheme.Mint -> TextKey.Mint
                                    ColorTheme.Mono -> TextKey.Mono
                                },
                            ],
                        )
                    },
                )
            }
            HorizontalDivider()
            Text(strings[TextKey.Appearance], style = MaterialTheme.typography.titleLarge)
            Appearance.entries.forEach { appearance ->
                val key =
                    when (appearance) {
                        Appearance.System -> TextKey.System
                        Appearance.Light -> TextKey.Light
                        Appearance.Dark -> TextKey.Dark
                    }
                FilterChip(
                    state.appearance == appearance,
                    { model.actionHandler(SettingsAction.SetAppearance(appearance)) },
                    label = { Text(strings[key]) },
                )
            }
            HorizontalDivider()
            Text(strings[TextKey.LocationHelp])
            Text(strings[TextKey.About], color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
