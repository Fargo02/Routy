package com.example.routy.feature.routes.domain

import com.example.routy.core.transport.domain.Route

class SearchRoutesUseCase {
    operator fun invoke(
        routes: List<Route>,
        query: String,
    ): List<Route> {
        val term = query.trim()
        if (term.isEmpty()) return routes
        return routes
            .filter { route ->
                val names = listOf(route.name.english, route.name.georgian, route.name.original)
                names.any { it?.contains(term, ignoreCase = true) == true } ||
                    (names.all { it.isNullOrBlank() } && route.id.contains(term, ignoreCase = true))
            }.sortedBy { route ->
                val names = listOf(route.name.english, route.name.georgian, route.name.original)
                when {
                    names.any { it.equals(term, ignoreCase = true) } ||
                        (names.all { it.isNullOrBlank() } && route.id.equals(term, ignoreCase = true)) -> 0
                    names.any { it?.startsWith(term, ignoreCase = true) == true } ||
                        (names.all { it.isNullOrBlank() } && route.id.startsWith(term, ignoreCase = true)) -> 1
                    else -> 2
                }
            }
    }
}
