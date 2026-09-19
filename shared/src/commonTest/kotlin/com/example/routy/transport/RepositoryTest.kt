@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.transport

import com.example.routy.core.transport.data.*
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class RepositoryTest {
    private class Remote(
        var failure: Boolean = false,
        var payload: String = TransportParserTest.fixture,
    ) : TransportRemoteDataSource {
        var calls = 0

        override suspend fun database(): String {
            calls++
            if (failure) error("offline")
            return payload
        }

        override suspend fun vehicles(routeId: String): String {
            calls++
            if (failure) error("offline")
            return """{"data":[{"Lat":41.6,"Lon":41.6,"Name":"bus"}]}"""
        }
    }

    private class Local(
        var cache: CachedDatabase? = null,
        var writeFails: Boolean = false,
    ) : TransportLocalDataSource {
        override suspend fun read() = cache

        override suspend fun write(database: CachedDatabase) {
            if (writeFails) error("disk full")
            cache = database
        }
    }

    @Test fun cacheAndNetworkMatrix() =
        runTest {
            for (cached in listOf(false, true)) {
                for (failure in listOf(false, true)) {
                    val local = Local(if (cached) CachedDatabase(TransportParserTest.fixture, 1) else null)
                    val repository =
                        OfflineTransportRepository(
                            Remote(failure),
                            local,
                            TransportParser(),
                            EpochClock {
                                10
                            },
                            TransportConfig(),
                            StandardTestDispatcher(testScheduler),
                        )
                    repository.refresh()
                    assertEquals(cached || !failure, repository.state.value.network != null)
                    assertEquals(failure, repository.state.value.isStale)
                    assertFalse(repository.state.value.isRefreshing)
                    if (!failure) assertEquals(10, local.cache!!.updatedAtMillis)
                }
            }
        }

    @Test fun malformedNetworkPreservesPersistentCache() =
        runTest {
            val cache = CachedDatabase(TransportParserTest.fixture, 1)
            val local = Local(cache)
            val repository =
                OfflineTransportRepository(
                    Remote(payload = "{}"),
                    local,
                    TransportParser(),
                    EpochClock {
                        10
                    },
                    TransportConfig(),
                    StandardTestDispatcher(testScheduler),
                )
            repository.refresh()
            assertEquals(cache, local.cache)
            assertEquals(AppError.InvalidData, repository.state.value.error)
            assertNotNull(repository.state.value.network)
        }

    @Test fun cachedContentIsEmittedBeforeNetworkCompletes() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val remote =
                object : TransportRemoteDataSource {
                    override suspend fun database(): String {
                        gate.await()
                        return TransportParserTest.fixture
                    }

                    override suspend fun vehicles(routeId: String) = ""
                }
            val repository =
                OfflineTransportRepository(
                    remote,
                    Local(CachedDatabase(TransportParserTest.fixture, 1)),
                    TransportParser(),
                    EpochClock {
                        10
                    },
                    TransportConfig(),
                    StandardTestDispatcher(testScheduler),
                )
            val job = launch { repository.refresh() }
            runCurrent()
            assertNotNull(repository.state.value.network)
            assertTrue(repository.state.value.isRefreshing)
            assertTrue(repository.state.value.isStale)
            assertFalse(repository.state.value.hasConnectivityIssue)
            gate.complete(Unit)
            job.join()
        }

    @Test fun connectivityIssueFollowsReachabilityNotStorage() =
        runTest {
            val remote = Remote()
            val local = Local(writeFails = true)
            val repository =
                OfflineTransportRepository(
                    remote,
                    local,
                    TransportParser(),
                    EpochClock { 10 },
                    TransportConfig(),
                    StandardTestDispatcher(testScheduler),
                )
            repository.refresh()
            assertEquals(AppError.StorageUnavailable, repository.state.value.error)
            assertFalse(repository.state.value.hasConnectivityIssue)

            remote.failure = true
            repository.refresh()
            assertTrue(repository.state.value.hasConnectivityIssue)
        }

    @Test fun vehicleRetentionExpiresAndPollingCancels() =
        runTest {
            val remote = Remote()
            val repository =
                PollingVehicleRepository(
                    remote,
                    TransportParser(),
                    EpochClock {
                        testScheduler.currentTime
                    },
                    TransportConfig(),
                    StandardTestDispatcher(testScheduler),
                )
            val states = mutableListOf<VehicleState>()
            val job = launch { repository.observeVehicles("r").toList(states) }
            runCurrent()
            assertEquals(1, states.last().vehicles.size)
            remote.failure = true
            advanceTimeBy(5_001)
            assertTrue(states.last().isStale)
            assertEquals(1, states.last().vehicles.size)
            advanceTimeBy(30_000)
            assertTrue(states.last().vehicles.isEmpty())
            job.cancelAndJoin()
            val calls = remote.calls
            advanceTimeBy(20_000)
            assertEquals(calls, remote.calls)
        }

    @Test fun localSearchAndOrderingUseSourceGroups() {
        val network = TransportParser().network(TransportParserTest.fixture)
        assertEquals(1, SearchRoutesUseCase()(network.routes, " 2a ").size)
        assertTrue(SearchRoutesUseCase()(network.routes, "missing").isEmpty())
        assertEquals(1, SearchStopsUseCase()(network.stops, "ბათ").size)
        val stop = network.stops.single()
        val earlier = stop.copy(id = "earlier", services = listOf(stop.services.single().copy(order = 1)))
        val another = stop.copy(id = "another", services = listOf(stop.services.single().copy(order = 0, sourceGroup = 88)))
        val details = GetRouteDetailsUseCase()(network.copy(stops = listOf(stop, earlier, another)), "r")!!
        assertEquals(listOf("earlier", "s"), details.groups[77]!!.map { it.stop.id })
        assertEquals(listOf("another"), details.groups[88]!!.map { it.stop.id })
    }
}
