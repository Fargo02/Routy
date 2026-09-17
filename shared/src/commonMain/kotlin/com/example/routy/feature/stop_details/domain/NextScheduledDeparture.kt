package com.example.routy.feature.stop_details.domain

import com.example.routy.core.transport.domain.ScheduleTime
import kotlin.time.Clock

private const val MinutesPerDay = 24 * 60
private const val BatumiUtcOffsetMinutes = 4 * 60

fun nearestScheduledDeparture(
    times: List<ScheduleTime>,
    currentMinutes: Int = batumiMinutesNow(),
): ScheduleTime? =
    times.minByOrNull { time ->
        (time.hour * 60 + time.minute - currentMinutes + MinutesPerDay) % MinutesPerDay
    }

fun minutesUntilNextScheduledDeparture(
    times: List<ScheduleTime>,
    currentMinutes: Int = batumiMinutesNow(),
): Int? =
    nearestScheduledDeparture(times, currentMinutes)?.let { time ->
        (time.hour * 60 + time.minute - currentMinutes + MinutesPerDay) % MinutesPerDay
    }

private fun batumiMinutesNow(): Int =
    ((Clock.System.now().toEpochMilliseconds() / 60_000 + BatumiUtcOffsetMinutes) % MinutesPerDay).toInt()
