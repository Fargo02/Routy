package com.example.routy.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.routy.core.designsystem.ScreenBackdrop
import com.example.routy.core.designsystem.ScreenScaffold
import com.example.routy.core.localization.LocalStrings
import com.example.routy.core.localization.TextKey
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.preferences.domain.ColorTheme
import com.example.routy.core.transport.domain.Language
import com.example.routy.feature.settings.presentation.state.SettingsAction
import com.example.routy.feature.settings.presentation.ui.SettingOption
import com.example.routy.feature.settings.presentation.ui.SettingRow
import com.example.routy.feature.settings.presentation.ui.appearanceKey
import com.example.routy.feature.settings.presentation.ui.colorKey
import com.example.routy.feature.settings.presentation.ui.languageKey
import kotlinx.coroutines.launch
import routy.shared.generated.resources.Res
import routy.shared.generated.resources.settings_backdrop
import routy.shared.generated.resources.settings_backdrop_dark

private enum class SettingsSheet { Language, ColorTheme, Appearance }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: SettingsViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch {
            sheetState.hide()
            sheet = null
        }
    }
    ScreenScaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    strings[TextKey.Settings],
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }) { padding ->
        Box(Modifier.fillMaxSize()) {
            ScreenBackdrop(
                light = Res.drawable.settings_backdrop,
                dark = Res.drawable.settings_backdrop_dark,
                alignment = Alignment.BottomEnd,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 24.dp,
                    ),
            ) {
                Column {
                    SettingRow(
                        strings[TextKey.Language],
                        strings[languageKey(state.language)],
                    ) { sheet = SettingsSheet.Language }
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        strings[TextKey.ColorTheme],
                        strings[colorKey(state.colorTheme)],
                    ) { sheet = SettingsSheet.ColorTheme }
                    HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        strings[TextKey.Appearance],
                        strings[appearanceKey(state.appearance)],
                    ) { sheet = SettingsSheet.Appearance }
                }
            }
        }
    }
    sheet?.let { active ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    strings[
                        when (active) {
                            SettingsSheet.Language -> TextKey.Language
                            SettingsSheet.ColorTheme -> TextKey.ColorTheme
                            SettingsSheet.Appearance -> TextKey.Appearance
                        },
                    ],
                    Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                when (active) {
                    SettingsSheet.Language ->
                        Language.entries.forEach { value ->
                            SettingOption(
                                strings[
                                    languageKey(
                                        value,
                                    ),
                                ],
                                state.language == value,
                            ) {
                                model.actionHandler(SettingsAction.SetLanguage(value))
                                closeSheet()
                            }
                        }

                    SettingsSheet.ColorTheme ->
                        ColorTheme.entries.forEach { value ->
                            SettingOption(
                                strings[
                                    colorKey(
                                        value,
                                    ),
                                ],
                                state.colorTheme == value,
                            ) {
                                model.actionHandler(SettingsAction.SetColorTheme(value))
                                closeSheet()
                            }
                        }

                    SettingsSheet.Appearance ->
                        Appearance.entries.forEach { value ->
                            SettingOption(
                                strings[
                                    appearanceKey(
                                        value,
                                    ),
                                ],
                                state.appearance == value,
                            ) {
                                model.actionHandler(SettingsAction.SetAppearance(value))
                                closeSheet()
                            }
                        }
                }
            }
        }
    }
}
