package com.example.routy.core.transport.data

import com.example.routy.core.logging.*
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineTransportRepository(
    private val remote: TransportRemoteDataSource,
    private val local: TransportLocalDataSource,
    private val parser: TransportParser,
    private val clock: EpochClock,
    private val config: TransportConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: AppLogger = SilentLogger,
) : TransportRepository {
    private val mutableState = MutableStateFlow(NetworkState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var cacheLoaded = false

    override suspend fun refresh() =
        mutex.withLock {
            withContext(dispatcher) {
                if (!cacheLoaded) {
                    try {
                        local.read()?.let { cache ->
                            mutableState.value = NetworkState(parser.network(cache.payload), updatedAtMillis = cache.updatedAtMillis)
                            logger.log(LogLevel.Info, LogEvent.CacheLoaded, null)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        logger.log(LogLevel.Warning, LogEvent.CacheReadFailed, AppError.StorageUnavailable)
                    }
                    cacheLoaded = true
                }
                mutableState.update { it.copy(isRefreshing = true) }
                try {
                    val payload = remote.database()
                    val network = parser.network(payload)
                    val now = clock.nowMillis()
                    val storageError =
                        try {
                            local.write(CachedDatabase(payload, now))
                            null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            logger.log(LogLevel.Warning, LogEvent.CacheWriteFailed, AppError.StorageUnavailable)
                            AppError.StorageUnavailable
                        }
                    mutableState.value = NetworkState(network, isStale = false, updatedAtMillis = now, error = storageError)
                } catch (error: Exception) {
                    val mapped = mapTransportError(error)
                    logger.log(LogLevel.Warning, LogEvent.DatabaseRefreshFailed, mapped)
                    mutableState.update { it.copy(isStale = true, error = mapped) }
                } finally {
                    mutableState.update { it.copy(isRefreshing = false) }
                }
            }
        }

    override fun observeNetwork(): Flow<NetworkState> =
        channelFlow {
            launch { state.collect { send(it) } }
            while (isActive) {
                refresh()
                delay(if (state.value.hasConnectivityIssue) config.databaseRetryMillis else config.databaseRefreshMillis)
            }
        }
}
