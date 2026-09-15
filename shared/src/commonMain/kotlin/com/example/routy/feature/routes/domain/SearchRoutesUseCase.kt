package com.example.routy.feature.routes.domain

import com.example.routy.core.transport.domain.Route

class SearchRoutesUseCase {
    operator fun invoke(
        routes: List<Route>,
        query: String,
    ): List<Route> {
        val term = query.trim()
        return if (term.isEmpty()) routes else routes.filter { it.name.matches(term) }
    }
}
