package com.example.routy.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.localization.*
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.transport.domain.Language

@Composable
fun SettingsScreen(model: SettingsViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(strings[TextKey.Language], style = MaterialTheme.typography.titleLarge)
        Language.entries.forEach { language ->
            FilterChip(state.language == language, { model.accept(SettingsIntent.SetLanguage(language)) }, label = {
                Text(
                    strings[
                        if (language ==
                            Language.English
                        ) {
                            TextKey.English
                        } else {
                            TextKey.Georgian
                        },
                    ],
                )
            })
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
                { model.accept(SettingsIntent.SetAppearance(appearance)) },
                label = { Text(strings[key]) },
            )
        }
        HorizontalDivider()
        Text(strings[TextKey.LocationHelp])
        Text(strings[TextKey.About], color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
