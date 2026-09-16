package com.example.routy.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.settings.domain.SettingsUseCase
import com.example.routy.feature.settings.presentation.state.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsUseCase,
) : ViewModel() {
    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<SettingsState> = settings.state

    init {
        viewModelScope.launch { report(settings.load()) }
    }

    fun actionHandler(action: SettingsAction) {
        viewModelScope.launch {
            report(
                when (action) {
                    is SettingsAction.SetLanguage -> settings.language(action.language)
                    is SettingsAction.SetAppearance -> settings.appearance(action.appearance)
                    is SettingsAction.SetColorTheme -> settings.colorTheme(action.colorTheme)
                },
            )
        }
    }

    private fun report(result: Outcome<Unit>) {
        if (result is Outcome.Failure) sendEffect(SettingsEffect.Error(result.error))
    }

    private fun sendEffect(effect: SettingsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
