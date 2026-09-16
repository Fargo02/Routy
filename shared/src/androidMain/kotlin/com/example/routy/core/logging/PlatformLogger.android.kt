package com.example.routy.core.logging

import android.util.Log
import com.example.routy.core.transport.domain.AppError

class PlatformLogger : AppLogger {
    override fun log(
        level: LogLevel,
        event: LogEvent,
        error: AppError?,
    ) {
        val priority =
            when (level) {
                LogLevel.Debug -> Log.DEBUG
                LogLevel.Info -> Log.INFO
                LogLevel.Warning -> Log.WARN
                LogLevel.Error -> Log.ERROR
            }
        Log.println(priority, "Routy", "${event.name}:${error?.name.orEmpty()}")
    }

    override fun diagnostic(event: LogEvent, details: String) {
        Log.d("Routy", "${event.name}:$details")
    }
}
