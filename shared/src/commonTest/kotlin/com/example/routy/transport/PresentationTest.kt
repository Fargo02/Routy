@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.transport

import androidx.lifecycle.ViewModelStore
import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.routes.presentation.*
import com.example.routy.feature.routes.presentation.state.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class PresentationTest {
    @Test fun loadingContentSearchAndNavigationTransitions() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val source = MutableStateFlow(NetworkState())
                val repository =
                    object : TransportRepository {
                        override val state = source.asStateFlow()

                        override suspend fun refresh() {
                            source.update { it.copy(isRefreshing = true) }
                        }

                        override fun observeNetwork() = state
                    }
                val model = RoutesViewModel(ObserveTransportUseCase(repository), SearchRoutesUseCase())
                store.put("routes", model)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
                // Search transforms run on Default; await their observable result, not wall-clock sleeps.
                source.value = NetworkState(TransportParser().network(TransportParserTest.fixture), isStale = false)
                assertEquals(
                    1,
                    model.uiState
                        .first { it.items.isNotEmpty() }
                        .items.size,
                )
                model.actionHandler(RoutesAction.Search("missing"))
                assertTrue(
                    model.uiState
                        .first { it.query == "missing" }
                        .items
                        .isEmpty(),
                )
                val effect = async { model.effects.first() }
                model.actionHandler(RoutesAction.Select("r"))
                assertEquals(RoutesEffect.Navigate(RouteDetails("r")), effect.await())
                source.value = NetworkState(error = AppError.NoInternet)
                assertEquals(
                    AppError.NoInternet,
                    model.uiState
                        .first { it.network.error != null }
                        .network.error,
                )
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
