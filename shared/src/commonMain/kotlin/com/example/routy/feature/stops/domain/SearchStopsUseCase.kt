package com.example.routy.feature.stops.domain

import com.example.routy.core.transport.domain.BusStop

class SearchStopsUseCase {
    operator fun invoke(stops: List<BusStop>, query: String): List<BusStop> {
        val term = query.trim()
        return if (term.isEmpty()) stops else stops.filter {
            it.name.matches(term) || it.number?.toString()?.contains(term) == true
        }
    }
}
