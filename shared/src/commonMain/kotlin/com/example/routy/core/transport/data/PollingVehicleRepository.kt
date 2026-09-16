package com.example.routy.core.transport.data

import com.example.routy.core.logging.AppLogger
import com.example.routy.core.logging.LogEvent
import com.example.routy.core.logging.LogLevel
import com.example.routy.core.logging.SilentLogger
import com.example.routy.core.transport.domain.EpochClock
import com.example.routy.core.transport.domain.GeoPoint
import com.example.routy.core.transport.domain.Outcome
import com.example.routy.core.transport.domain.VehicleRepository
import com.example.routy.core.transport.domain.VehicleState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PollingVehicleRepository(
    private val remote: TransportRemoteDataSource,
    private val parser: TransportParser,
    private val clock: EpochClock,
    private val config: TransportConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: AppLogger = SilentLogger,
) : VehicleRepository {
    override fun observeVehicles(routeId: String): Flow<VehicleState> =
        flow {
            require(routeId.isNotBlank())
            var previous = VehicleState()
            emit(previous)
            while (currentCoroutineContext().isActive) {
                previous =
                    when (val result = transportOperation(dispatcher) {
                        parser.vehicles(
                            remote.vehicles(routeId),
                            routeId
                        )
                    }) {
                        is Outcome.Success ->
                            VehicleState(
                                result.value.map { vehicle ->
                                    val prior =
                                        previous.vehicles.firstOrNull { it.id == vehicle.id }
                                    vehicle.copy(
                                        headingDegrees =
                                            prior?.let {
                                                headingBetween(
                                                    it.position,
                                                    vehicle.position
                                                )
                                            }
                                                ?: prior?.headingDegrees,
                                    )
                                },
                                isLoading = false,
                                updatedAtMillis = clock.nowMillis(),
                            )

                        is Outcome.Failure -> {
                            logger.log(
                                LogLevel.Warning,
                                LogEvent.VehicleRefreshFailed,
                                result.error
                            )
                            val age = previous.updatedAtMillis?.let { clock.nowMillis() - it }
                            previous.copy(
                                vehicles = if (age != null && age in 0..config.vehicleRetentionMillis) previous.vehicles else emptyList(),
                                isLoading = false,
                                isStale = true,
                                error = result.error,
                            )
                        }
                    }
                emit(previous)
                delay(config.vehicleRefreshMillis)
            }
        }

    private fun headingBetween(
        from: GeoPoint,
        to: GeoPoint,
    ): Float? {
        val latitudeDelta = to.latitude - from.latitude
        val longitudeDelta = to.longitude - from.longitude
        if (latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta < 1e-12) return null
        val fromLatitude = from.latitude * PI / 180.0
        val toLatitude = to.latitude * PI / 180.0
        val longitudeDifference = longitudeDelta * PI / 180.0
        val y = sin(longitudeDifference) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(
            longitudeDifference
        )
        return ((atan2(y, x) * 180.0 / PI + 360.0) % 360.0).toFloat()
    }
}
