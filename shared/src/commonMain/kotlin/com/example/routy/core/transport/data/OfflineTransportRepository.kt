package com.example.routy.core.transport.data

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
) : TransportRepository {
    private val mutableState = MutableStateFlow(NetworkState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var cacheLoaded = false

    override suspend fun refresh() = mutex.withLock {
        withContext(dispatcher) {
            if (!cacheLoaded) {
                try {
                    local.read()?.let { cache ->
                        mutableState.value = NetworkState(parser.network(cache.payload), updatedAtMillis = cache.updatedAtMillis)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Corrupt/evicted cache is recoverable through the network.
                }
                cacheLoaded = true
            }
            mutableState.update { it.copy(isRefreshing = true) }
            try {
                val payload = remote.database()
                val network = parser.network(payload)
                val now = clock.nowMillis()
                val storageError = try {
                    local.write(CachedDatabase(payload, now))
                    null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    AppError.StorageUnavailable
                }
                mutableState.value = NetworkState(network, isStale = false, updatedAtMillis = now, error = storageError)
            } catch (error: Exception) {
                val mapped = mapTransportError(error)
                mutableState.update { it.copy(isStale = true, error = mapped) }
            } finally {
                mutableState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    override fun observeNetwork(): Flow<NetworkState> = channelFlow {
        launch { state.collect { send(it) } }
        while (isActive) {
            refresh()
            delay(config.databaseRefreshMillis)
        }
    }
}
