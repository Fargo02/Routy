package com.example.routy.core.logging

import com.example.routy.core.transport.domain.AppError

enum class LogLevel { Debug, Info, Warning, Error }

enum class LogEvent { CacheLoaded, CacheReadFailed, CacheWriteFailed, DatabaseRefreshFailed, VehicleRefreshFailed }

/** Structured records deliberately cannot contain payloads, identifiers, or coordinates. */
fun interface AppLogger {
    fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    )
}

object SilentLogger : AppLogger {
    override fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    ) = Unit
}
