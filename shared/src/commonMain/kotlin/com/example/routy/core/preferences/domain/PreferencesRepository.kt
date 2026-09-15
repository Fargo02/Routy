package com.example.routy.core.preferences.domain

import com.example.routy.core.transport.domain.Language
import com.example.routy.core.transport.domain.Outcome
import kotlinx.coroutines.flow.StateFlow

enum class Appearance { System, Light, Dark }

data class Preferences(
    val language: Language = Language.English,
    val appearance: Appearance = Appearance.System,
    val routeIds: Set<String> = emptySet(),
    val stopIds: Set<String> = emptySet(),
)

interface PreferencesRepository {
    val state: StateFlow<Preferences>

    suspend fun load(): Outcome<Unit>

    suspend fun setLanguage(language: Language): Outcome<Unit>

    suspend fun setAppearance(appearance: Appearance): Outcome<Unit>

    suspend fun toggleRoute(id: String): Outcome<Unit>

    suspend fun toggleStop(id: String): Outcome<Unit>
}
