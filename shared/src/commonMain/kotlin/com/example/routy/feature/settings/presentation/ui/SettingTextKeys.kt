package com.example.routy.feature.settings.presentation.ui

import com.example.routy.core.localization.TextKey
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.preferences.domain.ColorTheme
import com.example.routy.core.transport.domain.Language

internal fun languageKey(value: Language) =
    when (value) {
        Language.English -> TextKey.English
        Language.Georgian -> TextKey.Georgian
        Language.Russian -> TextKey.Russian
    }

internal fun colorKey(value: ColorTheme) =
    when (value) {
        ColorTheme.Ocean -> TextKey.Ocean
        ColorTheme.Mint -> TextKey.Mint
        ColorTheme.Mono -> TextKey.Mono
    }

internal fun appearanceKey(value: Appearance) =
    when (value) {
        Appearance.System -> TextKey.System
        Appearance.Light -> TextKey.Light
        Appearance.Dark -> TextKey.Dark
    }
