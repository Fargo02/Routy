package com.example.routy.core.transport.domain

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

enum class Language { English, Georgian, Russian }

data class LocalizedName(
    val english: String?,
    val georgian: String?,
    val original: String?,
) {
    fun resolve(
        language: Language,
        fallback: String,
    ): String =
        listOf(if (language == Language.Georgian) georgian else english, english, georgian, original)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim() ?: fallback

    fun matches(query: String): Boolean =
        listOf(english, georgian, original)
            .any { it?.contains(query, ignoreCase = true) == true }
}

data class ScheduleTime(
    val hour: Int,
    val minute: Int,
) : Comparable<ScheduleTime> {
    init {
        require(hour in 0..23 && minute in 0..59)
    }

    override fun compareTo(other: ScheduleTime): Int = (hour * 60 + minute).compareTo(other.hour * 60 + other.minute)

    override fun toString(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    companion object {
        fun parse(value: String): ScheduleTime? {
            if (!Regex("[0-9]{2}:[0-9]{2}").matches(value)) return null
            val hour = value.take(2).toInt()
            val minute = value.takeLast(2).toInt()
            return if (hour in 0..23 && minute in 0..59) ScheduleTime(hour, minute) else null
        }
    }
}

/** Status is an opaque source grouping, never a named travel direction. */
data class StopService(
    val routeId: String,
    val sourceGroup: Int?,
    val order: Int?,
    val times: List<ScheduleTime>,
)

data class BusStop(
    val id: String,
    val number: Int?,
    val name: LocalizedName,
    val position: GeoPoint,
    val services: List<StopService>,
)

data class Route(
    val id: String,
    val name: LocalizedName,
    val sortOrder: Int?,
)

data class RouteGeometry(
    val routeId: String,
    val points: List<GeoPoint>,
)

data class Vehicle(
    val id: String,
    val routeId: String,
    val position: GeoPoint,
)

data class TransportNetwork(
    val routes: List<Route>,
    val stops: List<BusStop>,
    val geometries: Map<String, RouteGeometry>,
)

enum class AppError { NoInternet, Timeout, ServerUnavailable, InvalidData, StorageUnavailable, Unknown }

sealed interface Outcome<out T> {
    data class Success<T>(
        val value: T,
    ) : Outcome<T>

    data class Failure(
        val error: AppError,
    ) : Outcome<Nothing>
}
