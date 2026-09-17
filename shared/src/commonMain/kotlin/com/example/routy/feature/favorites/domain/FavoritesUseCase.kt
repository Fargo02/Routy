package com.example.routy.feature.favorites.domain

import com.example.routy.core.preferences.domain.PreferencesRepository

class FavoritesUseCase(
    private val repository: PreferencesRepository,
) {
    val state = repository.state

    suspend fun route(id: String) = repository.toggleRoute(id)

    suspend fun stop(id: String) = repository.toggleStop(id)

    suspend fun tracking(vehicleId: String?, stopId: String?, routeId: String?) = repository.setTracking(vehicleId, stopId, routeId)
}
