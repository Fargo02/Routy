@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.transport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.map.presentation.*
import com.example.routy.feature.map.presentation.state.MapAction
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class MapViewModelTest {
    @Test fun restoredRouteAndVisibleSubscribersOwnPolling() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val transport =
                    object : TransportRepository {
                        override val state = MutableStateFlow(NetworkState(TransportParser().network(TransportParserTest.fixture)))

                        override suspend fun refresh() = Unit

                        override fun observeNetwork() = state
                    }
                var active = 0
                var subscriptions = 0
                val vehicles =
                    object : VehicleRepository {
                        override fun observeVehicles(routeId: String): Flow<VehicleState> =
                            flow {
                                active++
                                subscriptions++
                                try {
                                    emit(VehicleState(isLoading = false))
                                    awaitCancellation()
                                } finally {
                                    active--
                                }
                            }
                    }
                val handle = SavedStateHandle(mapOf("routeId" to "r"))
                val model =
                    MapViewModel(
                        ObserveTransportUseCase(transport),
                        ObserveRouteVehiclesUseCase(vehicles),
                        GetRouteDetailsUseCase(),
                        handle,
                    )
                store.put("map", model)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
                val static = model.uiState.first { it.routeId == "r" }
                assertEquals("r", static.geometries.singleOrNull()?.routeId)
                assertEquals(0, active)
                val visible = launch { model.vehicles.collect() }
                runCurrent()
                assertEquals(1, active)
                assertSame(static, model.uiState.value)
                model.actionHandler(MapAction.SelectRoute("another"))
                runCurrent()
                assertEquals(listOf("r", "another"), handle.get<List<String>>("routeIds"))
                assertEquals(2, active)
                assertEquals(3, subscriptions)
                visible.cancelAndJoin()
                runCurrent()
                assertEquals(0, active)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
