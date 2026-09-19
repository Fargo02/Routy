package com.example.routy.core.logging

import com.example.routy.core.transport.domain.AppError
import platform.Foundation.NSLog

class PlatformLogger : AppLogger {
    override fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    ) {
        NSLog("Routy ${level.name} ${event.name}:${error?.name.orEmpty()}")
    }

    override fun diagnostic(
        event: LogEvent,
        details: String,
    ) {
        NSLog("Routy ${event.name}:$details")
    }
}
