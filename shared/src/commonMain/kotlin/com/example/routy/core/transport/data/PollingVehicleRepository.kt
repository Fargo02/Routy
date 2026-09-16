package com.example.routy.core.transport.data

import com.example.routy.core.logging.*
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
                    when (val result = transportOperation(dispatcher) { parser.vehicles(remote.vehicles(routeId), routeId) }) {
                        is Outcome.Success -> VehicleState(result.value, isLoading = false, updatedAtMillis = clock.nowMillis())
                        is Outcome.Failure -> {
                            logger.log(LogLevel.Warning, LogEvent.VehicleRefreshFailed, result.error)
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
}
