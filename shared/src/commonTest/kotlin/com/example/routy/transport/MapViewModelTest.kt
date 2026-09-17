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
}
