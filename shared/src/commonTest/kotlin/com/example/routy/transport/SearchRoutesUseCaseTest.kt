package com.example.routy.transport

import com.example.routy.core.transport.domain.LocalizedName
import com.example.routy.core.transport.domain.Route
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRoutesUseCaseTest {
    @Test
    fun exactRouteNumberIsRankedBeforePartialMatches() {
        val results =
            SearchRoutesUseCase()(
                listOf(route("12"), route("2A"), route("2"), route("20")),
                "2",
            )

        assertEquals(listOf("2", "2A", "20", "12"), results.map { it.id })
    }

    @Test
    fun technicalRouteIdsDoNotMatchWhenTheRouteHasAName() {
        val results =
            SearchRoutesUseCase()(
                listOf(route(id = "5ed6077c340f60873ff9e1be", number = "7"), route(id = "34", number = "34")),
                "34",
            )

        assertEquals(listOf("34"), results.map { it.id })
    }

    private fun route(number: String) = route(id = number, number = number)

    private fun route(
        id: String,
        number: String,
    ) = Route(
        id = id,
        name = LocalizedName(english = number, georgian = null, original = null),
        sortOrder = null,
    )
}
