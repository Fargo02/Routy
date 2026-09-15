package com.example.routy.core.transport.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class DbResponse(val data: DbDto)

@Serializable
internal data class DbDto(
    val busStops: Map<String, StopDto>,
    val routesNames: Map<String, JsonElement> = emptyMap(),
    val routeCoordinatesGrouped: Map<String, List<PointDto>> = emptyMap(),
    val routeStatusInfo: JsonElement? = null,
)

@Serializable
internal data class StopDto(
    @SerialName("BusStopIdGeoGps") val id: String,
    @SerialName("BusStopNumber") val number: Int? = null,
    @SerialName("BusStopNameGeoGps") val original: String? = null,
    @SerialName("BusStopNameKA") val georgian: String? = null,
    @SerialName("BusStopNameEN") val english: String? = null,
    @SerialName("BusStopLatitude") val latitude: Double,
    @SerialName("BusStopLongitude") val longitude: Double,
    val routes: Map<String, ServiceDto> = emptyMap(),
)

@Serializable
internal data class ServiceDto(
    @SerialName("Status") val status: Int? = null,
    @SerialName("Order") val order: Int? = null,
    val times: List<String> = emptyList(),
)

@Serializable
internal data class PointDto(val lat: Double, val lon: Double)

@Serializable
internal data class VehiclesResponse(val data: List<VehicleDto>)

@Serializable
internal data class VehicleDto(
    @SerialName("Lat") val latitude: Double,
    @SerialName("Lon") val longitude: Double,
    @SerialName("Name") val name: String,
    @SerialName("Status") val status: JsonElement? = null,
)
