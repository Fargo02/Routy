package com.example.routy.feature.stop_details.domain

import com.example.routy.core.transport.domain.ScheduleTime

/** Returns the most common interval between scheduled departures at one stop. */
fun scheduledFrequencyMinutes(times: List<ScheduleTime>): Int? {
    val minutes = times.map { it.hour * 60 + it.minute }.distinct().sorted()
    if (minutes.size < 2) return null

    return minutes
        .zipWithNext { current, next -> next - current }
        .filter { it > 0 }
        .groupingBy { it }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenByDescending { -it.key })
        ?.key
}
