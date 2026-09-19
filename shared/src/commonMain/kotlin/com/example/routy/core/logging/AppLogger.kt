package com.example.routy.core.logging

import com.example.routy.core.transport.domain.AppError

enum class LogLevel { Debug, Info, Warning, Error }

enum class LogEvent {
    CacheLoaded,
    CacheReadFailed,
    CacheWriteFailed,
    DatabaseRefreshFailed,
    VehicleRefreshFailed,
    FavoriteStopsChanged,
    MapFavoriteStopsUpdated,
    MapVehiclePollingStarted,
    MapVehiclePollingStopped,
    MapVehicleStateCombined,
    MapVehicleLayersComposed,
    MapVehicleLayerAttached,
    MapVehicleLayerDetached,
    StopNamesTranslated,
    StopNamesTranslationFailed,
}

/** Structured records deliberately cannot contain payloads, identifiers, or coordinates. */
fun interface AppLogger {
    fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    )

    /** Diagnostic metadata must not contain names, identifiers, or coordinates. */
    fun diagnostic(
        event: LogEvent,
        details: String,
    ) = log(LogLevel.Debug, event, null)
}

object SilentLogger : AppLogger {
    override fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    ) = Unit
}
