package com.example.routy.core.transport.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class NetworkState(
    val network: TransportNetwork? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = true,
    val updatedAtMillis: Long? = null,
    val error: AppError? = null,
)

data class VehicleState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true,
    val isStale: Boolean = false,
    val updatedAtMillis: Long? = null,
    val error: AppError? = null,
)

interface TransportRepository {
    val state: StateFlow<NetworkState>
    suspend fun refresh()
    /** One foreground owner collects this to refresh the database periodically. */
    fun observeNetwork(): Flow<NetworkState>
}

interface VehicleRepository {
    /** A cold stream: cancellation of its visible screen stops requests. */
    fun observeVehicles(routeId: String): Flow<VehicleState>
}

fun interface EpochClock { fun nowMillis(): Long }
