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
            .filter { route -> route.id.contains(term, ignoreCase = true) || route.name.matches(term) }
            .sortedBy { route ->
                val names = listOf(route.name.english, route.name.georgian, route.name.original)
                when {
                    route.id.equals(term, ignoreCase = true) || names.any { it.equals(term, ignoreCase = true) } -> 0
                    route.id.startsWith(term, ignoreCase = true) || names.any { it?.startsWith(term, ignoreCase = true) == true } -> 1
                    else -> 2
                }
            }
    }
}
