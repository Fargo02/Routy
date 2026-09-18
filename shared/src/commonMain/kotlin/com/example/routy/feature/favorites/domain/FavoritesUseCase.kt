package com.example.routy.feature.favorites.domain

import com.example.routy.core.preferences.domain.PreferencesRepository

class FavoritesUseCase(
    private val repository: PreferencesRepository,
) {
    val state = repository.state

    suspend fun route(id: String) = repository.toggleRoute(id)

    suspend fun stop(id: String) = repository.toggleStop(id)
}
