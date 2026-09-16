package com.example.routy.feature.settings.presentation.state

import com.example.routy.core.transport.domain.AppError

sealed interface SettingsEffect {
    data class Error(
        val error: AppError,
    ) : SettingsEffect
}
