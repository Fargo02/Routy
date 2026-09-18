@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.transport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.routy.core.preferences.domain.*
import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
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
                val favorites =
                    FavoritesUseCase(
                        object : PreferencesRepository {
                            override val state = MutableStateFlow(Preferences())

                            override suspend fun load() = Outcome.Success(Unit)

                            override suspend fun setLanguage(language: Language) = Outcome.Success(Unit)

                            override suspend fun setAppearance(appearance: Appearance) = Outcome.Success(Unit)

                            override suspend fun setColorTheme(colorTheme: ColorTheme) = Outcome.Success(Unit)

                            override suspend fun toggleRoute(id: String) = Outcome.Success(Unit)

                            override suspend fun toggleStop(id: String) = Outcome.Success(Unit)

                            override suspend fun setTracking(vehicleId: String?, stopId: String?, routeId: String?) = Outcome.Success(Unit)
                        },
                    )
                val model =
                    MapViewModel(
                        ObserveTransportUseCase(transport),
                        ObserveRouteVehiclesUseCase(vehicles),
                        GetRouteDetailsUseCase(),
                        favorites,
                        savedState = handle,
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
                model.actionHandler(MapAction.OpenSearch)
                assertTrue(
                    model.uiState
                        .first { it.search.isOpen }
                        .search
                        .quickRoutes
                        .isNotEmpty(),
                )
                model.actionHandler(MapAction.SearchQueryChanged("another"))
                assertEquals(
                    "another",
                    model.uiState
                        .first { it.search.query == "another" }
                        .search
                        .query,
                )
                model.actionHandler(MapAction.SelectSearchRoute("another"))
                runCurrent()
                assertEquals(listOf("r", "another"), handle.get<List<String>>("routeIds"))
                assertFalse(model.uiState.value.search.isOpen)
                assertEquals(2, active)
                assertEquals(2, subscriptions)
                visible.cancelAndJoin()
                runCurrent()
                assertEquals(0, active)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test fun showingRouteFromStopDetailsKeepsItSelectedWhenRepeated() =
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
                val vehicles =
                    object : VehicleRepository {
                        override fun observeVehicles(routeId: String): Flow<VehicleState> = flowOf(VehicleState(isLoading = false))
                    }
                val handle = SavedStateHandle()
                val model =
                    MapViewModel(
                        ObserveTransportUseCase(transport),
                        ObserveRouteVehiclesUseCase(vehicles),
                        GetRouteDetailsUseCase(),
                        FavoritesUseCase(StubPreferences()),
                        savedState = handle,
                    )
                store.put("map", model)
                val collector = launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
                model.actionHandler(MapAction.ShowRoute("r"))
                assertEquals(listOf("r"), model.uiState.first { it.selectedRouteIds.isNotEmpty() }.selectedRouteIds)
                model.actionHandler(MapAction.ShowRoute("r"))
                runCurrent()
                assertEquals(listOf("r"), model.uiState.value.selectedRouteIds)
                collector.cancelAndJoin()
                runCurrent()
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    @Test fun deselectingRouteKeepsColorsOfRoutesThatStaySelected() =
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
                val vehicles =
                    object : VehicleRepository {
                        override fun observeVehicles(routeId: String): Flow<VehicleState> = flowOf(VehicleState(isLoading = false))
                    }
                val handle = SavedStateHandle()
                val model =
                    MapViewModel(
                        ObserveTransportUseCase(transport),
                        ObserveRouteVehiclesUseCase(vehicles),
                        GetRouteDetailsUseCase(),
                        FavoritesUseCase(StubPreferences()),
                        savedState = handle,
                    )
                store.put("map", model)
                val collector = launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
                listOf("r", "another", "third").forEach { model.actionHandler(MapAction.SelectRoute(it)) }
                assertEquals(
                    mapOf("r" to 0, "another" to 1, "third" to 2),
                    model.uiState.first { it.selectedRouteIds.size == 3 }.routeColorIndices,
                )
                model.actionHandler(MapAction.SelectRoute("r"))
                assertEquals(
                    mapOf("another" to 1, "third" to 2),
                    model.uiState.first { "r" !in it.selectedRouteIds }.routeColorIndices,
                )
                model.actionHandler(MapAction.SelectRoute("fourth"))
                val refilled = model.uiState.first { "fourth" in it.selectedRouteIds }
                assertEquals(mapOf("another" to 1, "third" to 2, "fourth" to 0), refilled.routeColorIndices)
                assertEquals(-1, refilled.routeColorIndex("r"))
                model.actionHandler(MapAction.ClearSelectedRoutes)
                assertEquals(emptyMap(), model.uiState.first { it.selectedRouteIds.isEmpty() }.routeColorIndices)
                collector.cancelAndJoin()
                runCurrent()
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}

private class StubPreferences : PreferencesRepository {
    override val state = MutableStateFlow(Preferences())

    override suspend fun load() = Outcome.Success(Unit)

    override suspend fun setLanguage(language: Language) = Outcome.Success(Unit)

    override suspend fun setAppearance(appearance: Appearance) = Outcome.Success(Unit)

    override suspend fun setColorTheme(colorTheme: ColorTheme) = Outcome.Success(Unit)

    override suspend fun toggleRoute(id: String) = Outcome.Success(Unit)

    override suspend fun toggleStop(id: String) = Outcome.Success(Unit)

    override suspend fun setTracking(vehicleId: String?, stopId: String?, routeId: String?) = Outcome.Success(Unit)
}
