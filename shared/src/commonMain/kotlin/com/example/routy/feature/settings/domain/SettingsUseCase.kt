package com.example.routy.feature.settings.domain

import com.example.routy.core.preferences.domain.*
import com.example.routy.core.transport.domain.Language

class SettingsUseCase(
    private val repository: PreferencesRepository,
) {
    val state = repository.state

    suspend fun load() = repository.load()

    suspend fun language(value: Language) = repository.setLanguage(value)

    suspend fun appearance(value: Appearance) = repository.setAppearance(value)

    suspend fun colorTheme(value: ColorTheme) = repository.setColorTheme(value)
}
