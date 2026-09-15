package com.example.routy.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.settings.domain.SettingsUseCase
import kotlinx.coroutines.launch

sealed interface SettingsIntent {
    data class SetLanguage(
        val language: Language,
    ) : SettingsIntent

    data class SetAppearance(
        val appearance: Appearance,
    ) : SettingsIntent
}

sealed interface SettingsEffect {
    data class Error(
        val error: AppError,
    ) : SettingsEffect
}

class SettingsViewModel(
    private val settings: SettingsUseCase,
) : MviViewModel<SettingsIntent, SettingsEffect>() {
    val state = settings.state

    init {
        viewModelScope.launch { report(settings.load()) }
    }

    override fun accept(intent: SettingsIntent) {
        viewModelScope.launch {
            report(
                when (intent) {
                    is SettingsIntent.SetLanguage -> settings.language(intent.language)
                    is SettingsIntent.SetAppearance -> settings.appearance(intent.appearance)
                },
            )
        }
    }

    private fun report(result: Outcome<Unit>) {
        if (result is Outcome.Failure) effect(SettingsEffect.Error(result.error))
    }
}
