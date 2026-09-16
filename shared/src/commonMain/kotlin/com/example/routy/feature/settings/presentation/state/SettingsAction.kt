package com.example.routy.feature.settings.presentation.state

import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.preferences.domain.ColorTheme
import com.example.routy.core.transport.domain.Language

sealed interface SettingsAction {
    data class SetLanguage(
        val language: Language,
    ) : SettingsAction

    data class SetAppearance(
        val appearance: Appearance,
    ) : SettingsAction

    data class SetColorTheme(
        val colorTheme: ColorTheme,
    ) : SettingsAction
}
