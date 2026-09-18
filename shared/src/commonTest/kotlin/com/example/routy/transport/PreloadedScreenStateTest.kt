package com.example.routy.transport

import com.example.routy.core.preferences.domain.*
import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.favorites.presentation.FavoritesViewModel
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.presentation.RouteDetailsViewModel
import com.example.routy.feature.stop_details.domain.GetStopDetailsUseCase
import com.example.routy.feature.stop_details.presentation.StopDetailsViewModel
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import com.example.routy.feature.stops.presentation.StopsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PreloadedScreenStateTest {
    private val network = TransportParser().network(TransportParserTest.fixture)
    private val transport =
        ObserveTransportUseCase(
            object : TransportRepository {
                override val state = MutableStateFlow(NetworkState(network, isStale = false))

                override suspend fun refresh() = Unit

                override fun observeNetwork() = state
            },
        )
    private val favorites =
        FavoritesUseCase(
            object : PreferencesRepository {
                override val state = MutableStateFlow(Preferences(routeIds = setOf("r"), stopIds = setOf("s")))

                override suspend fun load() = Outcome.Success(Unit)

                override suspend fun setLanguage(language: Language) = Outcome.Success(Unit)

                override suspend fun setAppearance(appearance: Appearance) = Outcome.Success(Unit)

                override suspend fun setColorTheme(colorTheme: ColorTheme) = Outcome.Success(Unit)

                override suspend fun toggleRoute(id: String) = Outcome.Success(Unit)

                override suspend fun toggleStop(id: String) = Outcome.Success(Unit)

                override suspend fun setTracking(
                    vehicleId: String?,
                    stopId: String?,
                    routeId: String?,
                ) = Outcome.Success(Unit)
            },
        )
    private val vehicles =
        ObserveRouteVehiclesUseCase(
            object : VehicleRepository {
                override fun observeVehicles(routeId: String): Flow<VehicleState> = flowOf(VehicleState(isLoading = false))
            },
        )

    @Test fun screensOpenOnTheLoadedNetworkInsteadOfTheLoadingPlaceholder() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val saved = FavoritesViewModel(transport, favorites).uiState.value
                assertEquals(listOf("r"), saved.routes.map { it.id })
                assertEquals(listOf("s"), saved.stops.map { it.id })
                assertNotNull(saved.network.network)

                val stops = StopsViewModel(transport, SearchStopsUseCase()).uiState.value
                assertEquals(listOf("s"), stops.items.map { it.id })
                assertNotNull(stops.network.network)

                val route = RouteDetailsViewModel("r", transport, GetRouteDetailsUseCase(), favorites, vehicles).uiState.value
                assertNotNull(route.details)
                assertTrue(route.favorite)

                val stop = StopDetailsViewModel("s", transport, GetStopDetailsUseCase(), favorites).uiState.value
                assertNotNull(stop.details)
                assertTrue(stop.favorite)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
